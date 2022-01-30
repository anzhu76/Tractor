package com.android.tractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class AIStatefulLearner extends AILearner {

	private Map<String, Double> state_count = new HashMap<String, Double>();
	private Map<String, Double> state_sum = new HashMap<String, Double>();
	
	private static final double alpha = 0.01;
	double discountRate = 0.5 * 0;
	
	String last_state_key = null;
	
	@Override
	public synchronized double evaluateAfterState(Vector<Integer> afterstate) {
		return evaluateAfterStateInternal(MakeKey(afterstate));
	}

	private double evaluateAfterStateInternal(String key) {
		if (key==null) return 0;
		Double in = GetCount(state_count, key);
		if (in == null) {
			return 0;
		} else {
			return GetCount(state_sum, key) / in; 
		}
	}

	private Double GetCount(Map<String, Double> map, String key) {
		return map.get(key);
	}

	private String MakeKey(Vector<Integer> afterstate) {
		if (afterstate == null ) return null;
		return afterstate.toString();
	}

	@Override
	public void gameOver() {
		observeAfterStateReward(null, 0);
	}

	@Override
	public synchronized void observeAfterStateReward(Vector<Integer> afterstate,
			double reward) {
		if (!isLearningEnabled) return;
		String key2 = MakeKey(afterstate);
		if (last_state_key != null) SARSA(last_state_key, reward, key2);
		last_state_key = key2;
	}

	private void AddCount(Map<String, Double> map, String key,
			double num) {
		Double val = map.get(key);
		if (val == null) {
			val = num;
		} else {
			val = (1.0-alpha) * val + num;
		}
		map.put(key, val);
	}
	
	private void SARSA(String key1, double reward, String key2) {
		Double true_val = reward + discountRate * evaluateAfterStateInternal(key2);
		AddCount(state_count, key1, 1.0);
		AddCount(state_sum, key1, true_val);

	}

}
