package com.android.tractor;

import java.util.Arrays;
import java.util.Vector;

public class SingletonCardPropertyProbability {
	/* We are having a 4-dimensional array of probability table, keyed by
	 * 1. Number of players: max value is Tractor.MAX_PLAYERS
	 * 2. Number of decks: max value is Tractor.MAX_DECKS
	 * 3. Number of identical cards: max value is Tractor.MAX_DECKS
	 * 4. Number of sequences: max value is SingletonCardProperty.BIG_JOKER
	 */
	static double[][][][] type_probabilities = new double[Tractor.MAX_PLAYERS][Tractor.MAX_DECKS][Tractor.MAX_DECKS][SingletonCardProperty.BIG_JOKER];
	static {
		// Arrays.fill apparently only works on one dimensional arrays, sigh.
		// We *could* just use the default value 0, but I'm paranoid, so...
		for (int i = 0; i < Tractor.MAX_PLAYERS; ++i)
			for (int j = 0; j < Tractor.MAX_DECKS; ++j)
				for (int k = 0; k < Tractor.MAX_DECKS; ++k)
					for (int l = 0; l < SingletonCardProperty.BIG_JOKER; ++l)
						type_probabilities[i][j][k][l] = -1;
	}
	
	static double Probability(int total_players, int num_decks, int num_identical_cards, int num_sequences) {
		// First check to see if the numbers are within range.  Sometimes when we do testing, we test on
		// numbers not necessarily follow constants in Tractor.java.
		boolean numbers_in_range = true;
		if (total_players > Tractor.MAX_PLAYERS || num_decks > Tractor.MAX_DECKS ||
				num_identical_cards > Tractor.MAX_DECKS || num_sequences > SingletonCardProperty.BIG_JOKER)
			numbers_in_range = false;
		double prob = -1;
		if (numbers_in_range)
			prob = type_probabilities[total_players - 1][num_decks - 1][num_identical_cards - 1][num_sequences - 1];
		if (prob != -1)  // If we have already calculated for this combination, return
			return prob;
		// Do the actual calculation.
		Vector<Integer> num_cards = new Vector<Integer>();
		for (int i = 0; i < num_sequences; ++i)
			num_cards.add(num_decks);
		// prob = ProbabilityExact(total_players, num_cards, false, num_identical_cards, num_sequences);
		prob = ProbabilityApproximate(total_players, num_cards, false, num_identical_cards, num_sequences);
		if (numbers_in_range)
			type_probabilities[total_players - 1][num_decks - 1][num_identical_cards - 1][num_sequences - 1] = prob;
		return prob;
	}
	
	// Zhu An's first approximation.	
	public static double ProbabilityApproximate(int total_players, Vector<Integer> num_cards, boolean fixed_targeting_player,
			int num_identical_cards, int num_sequences) {
		// First some short cut.  If we don't have even enough cards to make up the property,
		// zero chance.
		for (Integer i : num_cards)
			if (i < num_identical_cards)
				return 0;
		// Now an approximate calculation.  First the simple case, no consecutive pairs.
		// N cards, M players, what's the probability
		// one of the player has at least K such cards?  Do the calculation of no player has
		// K or more cards.  Look at the choice of each card, which player does it go to.
		// Look at K of the card choices all together. We
		// can assume that all N choose K cases are independent events.  Of each K cards, 
		// the probability of them not all go to a single player i is: 1 - (1/M)^(K - 1).
		// 
		// Now factor in the num_sequences, say S.  Now we are looking at:
		// N cards, S*K cards should be selected, K cards per number, we can easily calculate
		// that as product of each of the selections (as the exponent).
		// Now for each of S*K cards, the probability of them not all go to one player is:
		// q = 1 - (1/M)^(SK - 1) = (M^(SK - 1) - 1) / M^(SK-1) = (P - 1) / P
		// The overall probability is then: prob = 1 - q^exponent.
		// Got the idea from this wiki:
		// http://en.wikipedia.org/wiki/Birthday_paradox#Approximation_of_number_of_people
		double SK_minus_1 = num_identical_cards * num_sequences - 1;
		if (fixed_targeting_player)  // the cards have to avoid a particular player.
			SK_minus_1++;
		double P = Math.pow(total_players, SK_minus_1);
		double exponent = 1;
		for (int i = 0; i < num_sequences; ++i) {
			exponent *= NChooseM(num_cards.get(i), num_identical_cards);
		}
		double prob = 1 -  Math.pow((P - 1) / P , exponent);
		return prob;
	}
	
	// An exact solution.
	// 强!
	public static double ProbabilityExact(int total_players, Vector<Integer> num_cards, int num_identical_cards, int num_sequences) {
	    // calc p = Prob(ANY has the required (k,num_seq) tuple)
		double p = 0;
		int sign = 1;
outer:
		for (int i=1; /* until ik>n_j */ ; i++) {
		    // Inner loop calcs:
		    // pp = Prob(a GIVEN i players all have the required (k,num_seq) tuple)
			double pp = 1;
			int ik = i*num_identical_cards;
			for (int j=0; j<num_sequences; j++) {
				int n = num_cards.get(j);
				if (ik > n) break outer;
				// MQC: this is still not quite right...
				pp *= NPermuteM(n, ik)
				/ Math.pow(NPermuteM(num_identical_cards, num_identical_cards), i)
				/ Math.pow(total_players, ik);
			}
			// compute p by inclusion-exclusion principle
			p += NChooseM(total_players, i) * pp * sign;
			sign = -sign;
		}
		return p;
	}
	
	/**
	 * Returns the combinatorial number of all possible ways of choosing
	 * m objects out of the n objects.
	 * 
	 * @param n total number of objects to choose from
	 * @param m number of objects to choose
	 * @return total number of choices
	 */
	static public int NChooseM(int n, int m) {
		if (n < m)
			return 0;
		int total = 1;
		if (m > n -m )
			m = n - m;
		for (int i = n; i > n - m; i--) {
			total *= i;
		}
		for (int i = 2; i <= m; i++) {
			total /= m;
		}
		return total;
	}
	
	static private int NPermuteM(int n, int m) {
		int total = 1;
		for (int i = n; i > n - m; i--) {
			total *= i;
		}
		return total;
	}
}
