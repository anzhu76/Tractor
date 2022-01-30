package com.android.tractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

// Learns a model from R^n -> R as y ~ w^t x

public class AILinearLearner extends AILearner {

	private static final double EPSILON = 1e-8;
	private static final int MAX_ITER = 10000;
	private static double learningRate = 1e-7;
	private static double discountRate = 0.5;

	int degree = 2;
	int batchSize = 200;
	double C = .1; // L2 regularization
	
	Vector<Integer> last_sa = null;
	double last_reward;
	
	List<Vector<Integer>> afterstate_history = new LinkedList<Vector<Integer>>();
	List<Double> reward_history = new ArrayList<Double>();
	
	
	double[] weights = null;
	int dim;
	
	public AILinearLearner() {
	}
	
	public AILinearLearner(int degree) {
		this.degree = degree;
	}

	Vector<Integer> featureMap(Vector<Integer> afterstate) {
		//System.out.println(afterstate);
		int n = afterstate.size();
		afterstate.add(1);
		Vector<Integer> mapped = new Vector<Integer>(afterstate);
		if (degree == 1) return mapped;
		for (int i=0; i<n; i++)
			for (int j=i+1; j<n; j++)
				mapped.add(afterstate.get(i) * afterstate.get(j));
		if (degree == 2) return mapped;
		for (int i=0; i<n; i++)
			for (int j=i+1; j<n; j++)
				for (int k=j+1; k<n; k++)
					mapped.add(afterstate.get(i) * afterstate.get(j) * afterstate.get(k));
		return mapped;
	}
	
	private Vector<Integer> featureCross(Vector<Integer> as1,
			Vector<Integer> as2) {
		Vector<Integer> cross = new Vector<Integer>(as1.size() * as2.size());
		for (Integer i : as1) 
			for (Integer j : as2)
				cross.add(i*j);
		return cross;
	}

	@Override
	public synchronized double evaluateAfterState(Vector<Integer> afterstate) {
		if (afterstate == null) {
			// the terminal state
			return 0;
		}
		return evaluateInternal(featureMap(afterstate));
	}

	public double evaluateInternal(Vector<Integer> afterstate) {
		if (afterstate == null) {
			// the terminal state
			return 0;
		}
		double val = 0;
		for (int i=0; i<dim; i++) {
			val += weights[i] * afterstate.get(i);
		}
		return val;
	}

	@Override
	public synchronized void gameOver() {
		if (!isLearningEnabled) return;
		afterstate_history.add(null);
		reward_history.add(0.0);
		BatchLearn();
	}

	@Override
	public synchronized void observeAfterStateReward(Vector<Integer> afterstate, double reward) {
		if (!isLearningEnabled) return;
		afterstate = featureMap(afterstate);
		if (weights == null) {
			// init weights, assume all subsequent afterstates will have the same dimension
			dim = afterstate.size();
			weights = new double[dim];
			// TODO: randomize?
		}
		afterstate_history.add(afterstate);
		reward_history.add(reward);
		//BatchLearn();
		
	}
	
	private double[][] A;
	private double[] b;

	protected synchronized void BatchLearn() {
		if (reward_history.size() <= batchSize || 
				afterstate_history.get(afterstate_history.size()-1) !=null) return;

		if (reward_history.size() != afterstate_history.size()) {
			Util.f("AILearner", "rewards and afterstates don't match!!!");
		}
		System.out.println("Start Learning");

		if (false){
			BatchSARSA();
			return;
		}
		
		AccumulateRewards();
		
		// conjugate gradient on obj(w) = w'Aw - 2b'w + c
		// where A = sum_i x_i x_i'
		//       b = sum_i y_i x_i

		double old_weight;
		double new_weight;
		if (A == null) {
			A = new double[dim][dim];
			b = new double[dim];
			old_weight = 0;
			new_weight = 1;
		} else {
			old_weight = 1;
			new_weight = 1;
		}
		
		double[][] Anew = new double[dim][dim];
		double[] bnew = new double[dim];

		
		for (int i=0; i<afterstate_history.size()-1; i++) {
			Vector<Integer> sa1 = afterstate_history.get(i);
			if (sa1==null) continue;
			Vector<Integer> sa2 = afterstate_history.get(i+1);
			double reward = reward_history.get(i);
			for(int x=0; x<dim; x++) {
				bnew[x] += reward * sa1.get(x);
				Anew[x][x] += C;  // l2 regularization
				for (int y=0; y<dim; y++) {
					Anew[x][y] += sa1.get(x) * sa1.get(y);
					/*
					Anew[x][y] -= discountRate * sa1.get(x) * sa2.get(y);
					Anew[x][y] -= discountRate * sa1.get(y) * sa2.get(x);
					Anew[x][y] += discountRate * discountRate * sa2.get(x) * sa2.get(y);
					*/
				}
			}
		}

		// Exponentially weight each batch
		for(int x=0; x<dim; x++) {
			b[x] = b[x] * old_weight + bnew[x] * new_weight;
			for (int y=0; y<dim; y++) {
				A[x][y] = A[x][y] * old_weight + Anew[x][y] * new_weight;
			}
		}

		//for(int x=0; x<dim; x++)
		//	System.out.println(Arrays.toString(A[x]));

		
		double[] s = new double[dim];
		double[] r = new double[dim];
		double rtr = 0;
		double stAs = 0;
		double rtr_old = 0;
		
		int iter;
		for (iter=0; iter<MAX_ITER; iter++) {
			//double[] b = new double[dim];
			if (iter % 50 == 0) {
				// recompute r from scratch
				r = new double[dim];
				/*
				for (int i=0; i<afterstate_history.size()-1; i++) {
					Vector<Integer> sa1 = afterstate_history.get(i);
					Vector<Integer> sa2 = afterstate_history.get(i+1);
					double reward = reward_history.get(i);
					double v1 = evaluateInternal(sa1);
					double v2 = evaluateInternal(sa2);
					double y = reward + discountRate * v2;
					double delta = y - v1;
					for (int j=0; j<dim; j++) {
						r[j] += delta * (sa1.get(j) - discountRate * sa2.get(j));
						//b[j] += y * sa1.get(j);
					}
				}
				*/
				for (int j=0; j<dim; j++) {
					r[j] = b[j];
					for (int k=0; k<dim; k++) {
						r[j] -= A[j][k] * weights[k];
					}
				}
				
			} else {
				// r has already been updated
			}

			rtr_old = rtr;
			rtr = 0;
			for (int j=0; j<dim; j++) {
				rtr += r[j] * r[j];
			}
			if (iter==0) {
				s = r.clone();  // s0 = r0	
			} else {
				for (int j=0; j<dim; j++) {
				    s[j]  = r[j] + s[j] * rtr / rtr_old; 
				}
			}
			stAs = 0;
			for (int j=0; j<dim; j++) {
				for (int k=0; k<dim; k++) {
					stAs += s[j] * s[k] * A[j][k];
				}
			}
			double alpha = rtr / stAs;

			// r_k+1 = r_k - \alpha A s_k
			for (int j=0; j<dim; j++) {
				for (int k=0; k<dim; k++) {
					r[j] -= alpha * A[j][k] * s[k];
				}
			}
			
			double l2 = 0;
			for (int j=0; j<dim; j++) {
				weights[j] += alpha * s[j];
				l2 += alpha * alpha * s[j] * s[j];
			}
			l2 = Math.sqrt(l2);
			

			double err = 0;
			/*
			for (int i=0; i<afterstate_history.size()-1; i++) {
				Vector<Integer> sa1 = afterstate_history.get(i);
				double reward = reward_history.get(i);
				double v1 = evaluateInternal(sa1);
				double y = reward;
				double delta = y - v1;
				err += delta * delta;
			}
			err /= afterstate_history.size();
			*/

			
			//System.out.println("inc=" + Arrays.toString(inc));
			//System.out.printf("iter=%d\tl2=%.7f\terr=%.2f\tw=%s\n", iter, l2, err, Arrays.toString(weights));
			if (iter % 50 == 0) System.out.print(".");
			

			if (l2 < EPSILON) break;
		}
		
		System.out.println("Done learning.");
		System.out.printf("iter=%d\tw = %s\n", iter, Arrays.toString(weights));
		afterstate_history.clear();
		reward_history.clear();
	}
	
	private void AccumulateRewards() {
		double sum_reward = 0;
		//System.out.println(reward_history.toString());
		for (int i=afterstate_history.size()-2; i>=0; i--) {
			if (reward_history.get(i) != null)
				reward_history.set(i, reward_history.get(i) + 
						discountRate * reward_history.get(i+1));
		}
		//System.out.println(reward_history.toString());
		
	}

	private void BatchSARSA() {
		for (int iter =0; iter<1; iter++) {
			for (int i=0; i<afterstate_history.size()-1; i++) {
				Vector<Integer> sa1 = afterstate_history.get(i);
				if (sa1==null) continue;
				Vector<Integer> sa2 = afterstate_history.get(i+1);
				double reward = reward_history.get(i);
				LearnSARSA(sa1, reward, sa2);
			}
		}
		System.out.printf("w=%s\n", Arrays.toString(weights));
		afterstate_history.clear();
		reward_history.clear();
	}

	private void LearnSARSA(Vector<Integer> sa1, double reward, Vector<Integer> sa2) {
		double v2 = evaluateInternal(sa2);
		//v2 = 0; // only learns immediate reward
		double v1 = evaluateInternal(sa1);
		double delta = reward + discountRate * v2 - v1;
		double l2 = 0;
		for (int i=0; i<dim; i++) {
			double di = learningRate * delta * sa1.get(i);
			weights[i] += di;
			l2 += di*di;
		}
		//System.out.println("err=" + delta + "\tl2=" + l2 + "\tw=" + Arrays.toString(weights));
	}

}
