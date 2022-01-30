package com.android.tractor;

import com.maqicheng.games.GameParam;

public class TractorGameParam extends GameParam {

	private static final long serialVersionUID = 6704813577828425041L;
	
	public int num_decks = 2;
	public int[] initial_scores = null;
	public int[] dealer_group = null;
	public int dealer_index = -1;
	
	// AZ: why are we returning a TractorGameParam here?  Shouldn't these be void?  Or since the variables are public.
	// Just set them directly?
	public TractorGameParam setNumDecks(int n) {
		num_decks = n;
		return this;
	}
	
	public TractorGameParam setInitialScores(int[] scores) {
		if (scores == null) {
			initial_scores = null;
			return this;
		}
		numPlayers = scores.length;  // Number of players should equal the number of scores.
		initial_scores = new int[scores.length];
		System.arraycopy(scores, 0, initial_scores, 0, scores.length);
		return this;
	}

	public TractorGameParam setDealerGroup(int[] group) {
		if (group == null) {
			dealer_group = null;
			return this;
		}
		dealer_group = new int[group.length];
		System.arraycopy(group, 0, dealer_group, 0, group.length);
		return this;
	}
	
	public TractorGameParam setDealer(int dealer) {
		dealer_index = dealer;
		return this;
	}
}
