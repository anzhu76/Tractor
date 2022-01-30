package com.android.tractor;

import java.util.Comparator;

public class PerDealCardComparator implements Comparator<AbstractCard>{
	private int trump_suit;
	private int trump_number;
	
	/**
	 * A comparator that will order cards in decreasing order according to the
	 * trump suit and number passed in.
	 * 
	 * @param suit trump suit this deal
	 * @param number trump number this deal
	 */
	public PerDealCardComparator(int suit, int number) {
		trump_suit = suit;
		trump_number = number;
	}
 
	public int compare(AbstractCard first_card, AbstractCard second_card) {
		if (first_card == null) return 1;
		if (second_card == null) return -1;
		// First take care of identical cards.  Identical cards should be identical ba, not sure
		// why earlier I need the tie breaker, maybe for display purposes.
		if (first_card.GetIndex() == second_card.GetIndex())
			return 0;
		// Now the jokers
		if (first_card.GetSuit() == Card.SUIT_NO_TRUMP ||
			second_card.GetSuit() == Card.SUIT_NO_TRUMP)
			return second_card.GetIndex() - first_card.GetIndex();
		// Now the trump numbers.  The trump number of the
		// trump suit is larger than the rest of of the suit.
		// For sorting purposes we break the tie according to
		// suit order.
		if (first_card.GetNumber() == trump_number) {
			if (second_card.GetNumber() == trump_number) {
				if (first_card.GetSuit() == trump_suit)
					return -1;
				if (second_card.GetSuit() == trump_suit)
					return 1;
				return second_card.GetSuit() - first_card.GetSuit();
			}
			return -1;
		}
		if (second_card.GetNumber() == trump_number)
			return 1;
		// Now the trump suit.
		if (first_card.GetSuit() == trump_suit) {
			if (second_card.GetSuit() == trump_suit)
				return second_card.GetIndex() - first_card.GetIndex();
			return -1;
		}
		if (second_card.GetSuit() == trump_suit)
			return 1;
		// Now the rest, we'll simply order w.r.t. reverse card index
		return second_card.GetIndex() - first_card.GetIndex();
	}

}
