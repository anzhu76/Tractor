package com.android.tractor;

import java.util.Vector;

public class FollowCardSet {
	public Card[] follow_cards;
	public int point_attribute;
	public int lead_magnitude_attribute;
	public int play_attribute;
	
	public FollowCardSet(int point, int lead_magnitude, int play) {
		point_attribute = point;
		lead_magnitude_attribute = lead_magnitude;
		play_attribute = play;
		follow_cards = null;
	}
	
	static public Vector<FollowCardSet> AllPossibleSets() {
		Vector<FollowCardSet> sets = new Vector<FollowCardSet>();
		sets.add(new FollowCardSet(MAX_POINTS, NO_MAGNITUDE_POSSIBLE, FORCED_FOLLOW));
		sets.add(new FollowCardSet(MIN_POINTS, NO_MAGNITUDE_POSSIBLE, FORCED_FOLLOW));
		sets.add(new FollowCardSet(MAX_POINTS, NO_MAGNITUDE_POSSIBLE, GARBAGE_FOLLOW));
		sets.add(new FollowCardSet(MIN_POINTS, NO_MAGNITUDE_POSSIBLE, GARBAGE_FOLLOW));
		sets.add(new FollowCardSet(MAX_POINTS, NO_MAGNITUDE_POSSIBLE, NO_LEAD_FOLLOW));
		sets.add(new FollowCardSet(MIN_POINTS, NO_MAGNITUDE_POSSIBLE, NO_LEAD_FOLLOW));
		
		sets.add(new FollowCardSet(MAX_POINTS, MAX_POSSIBLE, LEAD_FOLLOW));
		sets.add(new FollowCardSet(MIN_POINTS, MAX_POSSIBLE, LEAD_FOLLOW));
		sets.add(new FollowCardSet(MAX_POINTS, MIN_POSSIBLE, LEAD_FOLLOW));
		sets.add(new FollowCardSet(MIN_POINTS, MIN_POSSIBLE, LEAD_FOLLOW));
		sets.add(new FollowCardSet(IMPOSSIBLE_POINT_ATTRIBUTE, NO_POINT_MAX_POSSIBLE, LEAD_FOLLOW));
		sets.add(new FollowCardSet(MAX_POINTS, MAX_POSSIBLE, TRUMP_FOLLOW));
		sets.add(new FollowCardSet(MIN_POINTS, MAX_POSSIBLE, TRUMP_FOLLOW));
		sets.add(new FollowCardSet(MAX_POINTS, MIN_POSSIBLE, TRUMP_FOLLOW));
		sets.add(new FollowCardSet(MIN_POINTS, MIN_POSSIBLE, TRUMP_FOLLOW));
		sets.add(new FollowCardSet(IMPOSSIBLE_POINT_ATTRIBUTE, NO_POINT_MAX_POSSIBLE, TRUMP_FOLLOW));
		return sets;
	}
	
	static public Vector<FollowCardSet> AllPossibleNonLeadingMaxPointSets() {
		Vector<FollowCardSet> sets = new Vector<FollowCardSet>();
		sets.add(new FollowCardSet(MAX_POINTS, NO_MAGNITUDE_POSSIBLE, FORCED_FOLLOW));
		sets.add(new FollowCardSet(MAX_POINTS, NO_MAGNITUDE_POSSIBLE, GARBAGE_FOLLOW));
		sets.add(new FollowCardSet(MAX_POINTS, NO_MAGNITUDE_POSSIBLE, NO_LEAD_FOLLOW));
		return sets;
	}
	
	static public Vector<FollowCardSet> AllPossibleNonLeadingMinPointSets() {
		Vector<FollowCardSet> sets = new Vector<FollowCardSet>();
		sets.add(new FollowCardSet(MIN_POINTS, NO_MAGNITUDE_POSSIBLE, FORCED_FOLLOW));
		sets.add(new FollowCardSet(MIN_POINTS, NO_MAGNITUDE_POSSIBLE, GARBAGE_FOLLOW));
		sets.add(new FollowCardSet(MIN_POINTS, NO_MAGNITUDE_POSSIBLE, NO_LEAD_FOLLOW));
		return sets;
	}

	// Constants for point_attribute
	public static int IMPOSSIBLE_POINT_ATTRIBUTE = -1;
	public static int MAX_POINTS = 0;
	public static int MIN_POINTS = 1;
	
	// Constants for lead magnitude attribute
	public static int NO_MAGNITUDE_POSSIBLE = -1;
	public static int MAX_POSSIBLE = 0;
	public static int MIN_POSSIBLE = 1;
	public static int NO_POINT_MAX_POSSIBLE = 2;  // If we have MAX_POSSIBLE AND MAX_POINT, then MAX_POSSIBLE takes precedence.  This sort
	                                              // of takes care of both magnitude and points.
	
	// When following a play, the play_attribute can only come from one of the following five sets
	// 
	// When the same suit cards is less or equal to the lead_play cards
	// 1. forced follow
	
	// When we are void of the suit in the lead_play cards
	// 2. garbage follow
	// 3. trump (takes the lead in this round) - need lead_magnitude_attribute

	// When we have more cards in the suit compared to the lead_play
	// 4 no lead follow
	// 5 lead (takes the lead in this round) - need lead_magnitude_attribute
	public static int FORCED_FOLLOW = 0;
	public static int GARBAGE_FOLLOW = 1;
	public static int TRUMP_FOLLOW = 2;
	public static int NO_LEAD_FOLLOW = 3;
	public static int LEAD_FOLLOW = 4;
}
