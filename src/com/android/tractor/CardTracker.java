package com.android.tractor;

import java.util.Vector;

/**
 * A class that keeps track of all the remaining cards out there
 * in a deck, answers questions like how good is this card property
 * w.r.t. the current deck.
 * @author anzhu
 *
 */
public class CardTracker {
	int[] num_cards;
	int trumpSuit;
	int trumpNumber;
	int numDecks;
	// TODO: we probably need a real time card property comparator here. i.e., based on
	// remaining cards, calculate the probability of the current property, and do some ranking.
	
	public CardTracker(int suit, int number, int num_decks) {
		trumpSuit = suit;
		trumpNumber = number;
		numDecks = num_decks;
		num_cards = new int[Card.CARDS_PER_DECK];
		for (int i = 0; i <  Card.CARDS_PER_DECK; ++i)
			num_cards[i] = num_decks;
	}
	
	public void DeleteCards(Card[] cards) {
		for (Card c : cards) {
			num_cards[c.GetIndex()]--;
		}
	}
	
	/**
	 * Based on the existing_played_cards, return the probability of a particular property p,
	 * i.e., high chance of winning in the same suit.  We'll factor in the number of players
	 * some what.  The idea is to calculate for cards higher than p that's remaining, what are
	 * the chance of something bigger property will form and beat p.
	 * 
	 * @param p
	 * @param num_possible_players  Number of players that could possibly have this property.
	 * For instance, maybe p is a pair, and we know certain players don't have pairs.
	 * @param total_players Total number of players involved that can beat property p (usually it
	 * would be numPlayers - 1).
	 * @return the winning probability of p.
	 */
	public double CurrentPropertyProbability(SingletonCardProperty p, int total_players, boolean fixed_targeting_player) {
		double probability = 1;
		int suit = p.suit;
		int highest_number_for_suit = GetHighestNumberForSuit(suit);
		int lowest_number_for_suit = p.leading_number;
		if (suit == Card.SUIT_NO_TRUMP)
			lowest_number_for_suit = Math.max(lowest_number_for_suit, SingletonCardProperty.MAJOR_TRUMP_NUMBER);
		for (int i = highest_number_for_suit; i > lowest_number_for_suit; i = GetNextLowerNumber(i, trumpNumber)) {
			SingletonCardProperty pp = new SingletonCardProperty(trumpSuit, trumpNumber);
			pp.Copy(p);
			pp.leading_number = i;
			// pp should always be valid, for NO_TRUMP it's tricky, but since p is valid to start with,
			// so must pp.
			int ending_number = i - pp.num_sequences + 1;
			// Normally we should check for ending_number > 1, but it's gonna be bigger than p.leading_number
			// so we are fine here.
			if (i > trumpNumber && ending_number <= trumpNumber)
				ending_number--;
			Vector<Integer> pp_num_cards = new Vector<Integer>();
			if (i < SingletonCardProperty.MINOR_TRUMP_NUMBER ||
					ending_number > SingletonCardProperty.MINOR_TRUMP_NUMBER) {
				// Simple case.
				int number = i;
				for (int j = 0; j < pp.num_sequences; ++j) {
					pp_num_cards.add(num_cards[SingletonCardProperty.ConvertToCardIndex(number, suit, trumpNumber)]);
					number = GetNextLowerNumber(number, trumpNumber);
				}
				probability *= (1 - pp.Probability(total_players, pp_num_cards, fixed_targeting_player));
				if (probability == 0.0) return 0;
			} else {
				// We have to involve MINOR_TRUMP_NUMBER
				for (int k = 0; k < 4; ++k) {
					pp_num_cards.clear();
					if (k == suit)
						continue;
					int number = i;
					int local_suit = suit;
					for (int j = 0; j < pp.num_sequences; ++j) {
						if (number == SingletonCardProperty.MINOR_TRUMP_NUMBER)
							local_suit = k;
						else
							local_suit = suit;
						pp_num_cards.add(num_cards[SingletonCardProperty.ConvertToCardIndex(number, local_suit, trumpNumber)]);
						number = GetNextLowerNumber(number, trumpNumber);
					}
					probability *= (1 - pp.Probability(total_players, pp_num_cards, fixed_targeting_player));
					if (probability == 0.0) return 0;
				}
			}
		}
		return probability;
	}
	
	//public int LargestPointCardsRemaining
	
	/**
	 * Return all points remaining still in the suit, a minimum leading_number is provided
	 * @param suit
	 * @param leading_number
	 * @return
	 */
	public int TotalPointsRemainingInSuit(int suit, int leading_number) {
		int max_leading_number = GetHighestNumberForSuit(suit);
		int total_points = 0;
		for (int i = max_leading_number; i > leading_number; --i) {
			if (i != SingletonCardProperty.MINOR_TRUMP_NUMBER) {
				int index = SingletonCardProperty.ConvertToCardIndex(i, suit, trumpNumber);
				total_points += num_cards[index] * Card.PointsBasedOnIndex(index);
			} else if (trumpNumber == SingletonCardProperty.FIVE ||
					trumpNumber == SingletonCardProperty.TEN ||
					trumpNumber == SingletonCardProperty.KING) {
				// Sigh, no through all of them
				for (int j = 0; j < 4; ++j) {
					if (j == trumpSuit)
						continue;
					int index = SingletonCardProperty.ConvertToCardIndex(i, j, trumpNumber);
					total_points += num_cards[index] * Card.PointsBasedOnIndex(index);
				}
			}
		}
		return total_points;
	}
	
	public int TotalCardsInSuit(int suit) {
		if (suit != trumpSuit && trumpSuit != Card.SUIT_NO_TRUMP)
			return (Card.CARDS_PER_SUIT - 1) * numDecks;
		if (suit != trumpSuit && trumpSuit == Card.SUIT_NO_TRUMP)
			return Card.CARDS_PER_SUIT * numDecks;
		if (suit == trumpSuit && trumpSuit != Card.SUIT_NO_TRUMP)
			return (Card.CARDS_PER_SUIT + 5) * numDecks;
		if (suit == trumpSuit && trumpSuit == Card.SUIT_NO_TRUMP)
			return 6 * numDecks;
		return 0;
	}
	
	public int RemainingCardsInSuit(int suit) {
		int highest_number_for_suit = GetHighestNumberForSuit(suit);
		int lowest_number_for_suit = GetLowestNumberForSuit(suit);
		int total_cards = 0;
		for (int i = highest_number_for_suit; i >= lowest_number_for_suit; i = GetNextLowerNumber(i, trumpNumber)) {
			if (i != SingletonCardProperty.MINOR_TRUMP_NUMBER) {
				total_cards += num_cards[SingletonCardProperty.ConvertToCardIndex(i, suit, trumpNumber)];
			} else {
				for (int k = 0; k < 4; ++k) {
					if (k == suit)
						continue;
					total_cards += num_cards[SingletonCardProperty.ConvertToCardIndex(i, k, trumpNumber)];	
				}
			}
		}
		return total_cards;
	}
	
	private int GetHighestNumberForSuit(int suit) {
		int highest_number_in_suit = SingletonCardProperty.ACE;
		if (trumpNumber == SingletonCardProperty.ACE)
			highest_number_in_suit = SingletonCardProperty.KING;
		// we usually only have 4 suit per deal.  So usually SUIT_NO_TRUMP is undefined,
		// unless it turns out to be trump_suit.
		if (suit == Card.SUIT_NO_TRUMP)
			highest_number_in_suit = SingletonCardProperty.UNDEFINED;
		if (suit == trumpSuit)
			highest_number_in_suit = SingletonCardProperty.BIG_JOKER;
		return highest_number_in_suit;
	}
	
	public int GetLowestNumberForSuit(int suit) {
		int lowest_number_in_suit = SingletonCardProperty.TWO;
		if (trumpNumber == SingletonCardProperty.TWO)
			lowest_number_in_suit = SingletonCardProperty.THREE;
		if (suit == Card.SUIT_NO_TRUMP)
			lowest_number_in_suit = SingletonCardProperty.UNDEFINED;
		if (suit == trumpSuit && trumpSuit == Card.SUIT_NO_TRUMP)
			lowest_number_in_suit = SingletonCardProperty.MINOR_TRUMP_NUMBER;
		return lowest_number_in_suit;
	}
	
	public static int GetNextLowerNumber(int number, int trump_number) {
		number--;
		if (number == trump_number)
			number--;
		return number;  // number could become UNDEFINED
	}
}
