package com.android.tractor;

import java.util.Comparator;

public class SingletonCardPropertyComparator implements
		Comparator<SingletonCardProperty> {
	// Used to determind the type of a SingletonCardPropertyComparator.
	public static final int DE_LEADING_NUMBER = 1;
	public static final int IN_LEADING_NUMBER = 2;
	public static final int IN_TYPE_IN_LEADING_NUMBER = 3;
	public static final int DE_TYPE_DE_LEADING_NUMBER = 4;
	public static final int DE_TYPE_IN_LEADING_NUMBER = 5;
	public static final int SAME_TYPE_TOGETHER = 6;
	// Here we use suit and static probability to estimate importance.
	// Important is first decided by suit, trump suit is the most important.  Within the same suit, it is ordered
	// by the importance of SingletonCardProperty, and then leading number.
	public static final int DE_POINTS_IN_IMPORTANCE = 7;
	public static final int IN_POINTS_IN_IMPORTANCE = 8;
	public static final int IN_IMPORTANCE_IN_POINTS = 9;
	public static final int IN_POINTS_DE_LEADING_NUMBER = 10;
	public static final int DE_TYPE_DE_IDENTICAL_CARDS = 11;

	
	private int mode;
	private int numDecks;
	private int numPlayers;
	private int trumpSuit;
	
	public SingletonCardPropertyComparator(int m, int decks, int players, int suit) {
		mode = m;
		numDecks = decks;
		numPlayers = players;
		trumpSuit = suit;
	}
	
	public SingletonCardPropertyComparator(int m) {
		assert(m == SAME_TYPE_TOGETHER);
		mode = m;
		numDecks = 0;
		numPlayers = 0;
		trumpSuit = Card.SUIT_UNDEFINED;
	}
		
	/**
	 * A true type comparator.  If the two types are are the same, return zero.
	 * Otherwise, return in decreasing order of importance.
	 * 
	 * @param object1
	 * @param object2
	 * @return
	 */
	private int compareType(SingletonCardProperty object1,
			SingletonCardProperty object2) {
		if (object1.num_identical_cards == object2.num_identical_cards &&
				object1.num_sequences == object2.num_sequences) {
			return 0;
		}
		// Complete dominance of one property.
		if (object1.num_identical_cards >= object2.num_identical_cards &&
				object1.num_sequences >= object2.num_sequences)
			return -1;
		if (object1.num_identical_cards <= object2.num_identical_cards &&
				object1.num_sequences <= object2.num_sequences)
			return 1;

		// Now deal with not directly computable cases. We are going to do
		// a computation on the possibility of each property, and the ones
		// with lower probability (higher number of instances) is more important (rare),
		// very democratic.
		// For rare cases of numDecks really huge, we might have a tie of 1, which is 
		// actually important for us to report the tie zero!
		double prob1 = object1.Probability(numPlayers, numDecks);
		double prob2 = object2.Probability(numPlayers, numDecks);
		if (prob1 < prob2)
			return -1;
		if (prob2 < prob1)
			return 1;
		return 0;
	}
	
	public int compare(SingletonCardProperty object1,
				SingletonCardProperty object2) {
			if (mode == SAME_TYPE_TOGETHER) {
				// Sort on num_sequences first, and then sort on num_identical_cards, and
				// then sort on leading_number.
				if (object1.num_sequences != object2.num_sequences)
					return object1.num_sequences - object2.num_sequences;
				if (object1.num_identical_cards != object2.num_identical_cards)
					return object1.num_identical_cards - object2.num_identical_cards;
				return object1.leading_number - object2.leading_number;
			}
			// Now get ordering that do the type first.
			int result = compareType(object1, object2);
			int leading_number = object2.leading_number - object1.leading_number;
			if (mode == DE_LEADING_NUMBER) {
				if (leading_number != 0)
					return leading_number;
				return -result;  // in case of tie, we'll return them in increasing order.
			}
			if (mode == IN_LEADING_NUMBER) {
				if (leading_number != 0)
					return -leading_number;
				return -result;  // in case of tie, we'll return them in increasing order.
			}
			
			if (mode == DE_TYPE_DE_LEADING_NUMBER) {
				if (result != 0)
					return result;
				return leading_number;
			}
			if (mode == DE_TYPE_IN_LEADING_NUMBER) {
				if (result != 0)
					return result;
				return -leading_number;
			}
			if (mode == IN_TYPE_IN_LEADING_NUMBER) {
				if (result != 0)
					return -result;
				return -leading_number;
			}
			if (mode == DE_TYPE_DE_IDENTICAL_CARDS) {
				if (result != 0)
					return result;
				return object2.num_identical_cards - object1.num_identical_cards;
			}
			int suit = 0;
			if (object1.suit == trumpSuit) {
				if (object2.suit != trumpSuit)
					suit = 1;
			} else if (object2.suit == trumpSuit) {
				suit = -1;
			}
			int average_point = 0;
			double average_points_1 = (double) object1.TotalPoints() / object1.num_cards;
			double average_points_2 = (double) object2.TotalPoints() / object2.num_cards;
			if (average_points_1 < average_points_2)
				average_point = -1;
			else if (average_points_1 > average_points_2)
				average_point = 1;
			if (mode == DE_POINTS_IN_IMPORTANCE) {
				if (average_point != 0) {
					return -average_point;
				} else {
					if (suit != 0)
						return suit;
					if (result != 0)
						return -result;
					return -leading_number;
				}
			}
			if (mode == IN_POINTS_IN_IMPORTANCE) {
				if (average_point != 0) {
					return average_point;
				} else {
					if (suit != 0)
						return suit;
					if (result != 0)
						return -result;
					return -leading_number;
				}
			}
			if (mode == IN_IMPORTANCE_IN_POINTS) {
				if (suit != 0)
					return suit;
				if (result != 0)
					return -result;
				if (average_point != 0) {
					return average_point;
				} else {
					return -leading_number;
				}
			}
			if (mode == IN_POINTS_DE_LEADING_NUMBER) {
				if (average_point != 0) {
					return average_point;
				} else {
					return leading_number;
				}
			}
			return 0;
	}
}