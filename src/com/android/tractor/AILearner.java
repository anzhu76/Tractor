package com.android.tractor;

import java.util.Vector;

public class AILearner {

	protected boolean isLearningEnabled = true;
	
	public void enableLearning(boolean b) {
		isLearningEnabled = b;
	}
	
	public double evaluateAfterState(Vector<Integer> afterstate) {
		//System.out.println("eval");
		return -1;
	}
	
	public void gameOver() {
		//System.out.println("gameover");
	}

	public void observeAfterStateReward(Vector<Integer> afterstate,
			double reward) {
		// TODO Auto-generated method stub
		return;
	}
	
	
}
