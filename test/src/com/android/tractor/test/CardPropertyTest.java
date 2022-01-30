package com.android.tractor.test;

import java.util.Collections;
import java.util.Vector;

import com.android.tractor.Card;
import com.android.tractor.CardProperty;
import com.android.tractor.SingletonCardProperty;
import com.android.tractor.SingletonCardPropertyComparator;

import junit.framework.TestCase;

public class CardPropertyTest extends TestCase {
	int trump_suit = Card.SUIT_HEART;
	int trump_number = Card.NUMBER_THREE;
	int decks = 5;
	int players = 4;
	CardProperty dummy_property = new CardProperty(null, trump_suit, trump_number);
	SingletonCardPropertyComparator comparator =
		new SingletonCardPropertyComparator(SingletonCardPropertyComparator.DE_TYPE_IN_LEADING_NUMBER,
				decks, players, trump_suit);

	
	/**
	 * Good for generating general non-trump cards.  For trump cards, try CreateTrumpCards ba.
	 * 
	 * @param card_nums
	 * @param suit
	 * @param starting_number
	 * @return
	 */
	static public Card[] CreateCards(int[] card_nums, int suit, int starting_number) {
		return CreateVectorCards(card_nums, suit, starting_number).toArray(new Card[0]);
	}
	
	/**
	 * Good for generating general non-trump cards.  For trump cards, try CreateTrumpCards ba.
	 * 
	 * @param card_nums
	 * @param suit
	 * @param starting_number
	 * @return
	 */
	static public Card[] CreateCards(Vector<Integer> card_nums, int suit, int starting_number) {
		int[] card_nums_array = new int[card_nums.size()];
		for (int i = 0; i < card_nums_array.length; ++i) {
			card_nums_array[i] = card_nums.get(i);
		}
		return CreateCards(card_nums_array, suit, starting_number);
	}

	/**
	 * Good for generating general non-trump cards.  For trump cards, try CreateTrumpCards ba.
	 * 
	 * @param card_nums
	 * @param suit
	 * @param starting_number
	 * @return
	 */
	static public Vector<Card> CreateVectorCards(int[] card_nums, int suit, int starting_number) {
		Vector<Card> cards = new Vector<Card>();
		for (int i : card_nums) {
			for (int j = 0; j < i; j++)
				cards.add(new Card(suit, starting_number));
			starting_number--;
		}
		Collections.shuffle(cards);
		return cards;
	}

	/**
	 * starts with EIGHT, skips THREE, which is the trump number.  Can be of any suit
	 * 
	 * @param num_1
	 * @param num_2
	 * @param num_3
	 * @param num_4
	 * @param num_5
	 * @param num_6
	 * @param suit
	 * @return
	 */
	static public  Card[] CreateNoneTrumpCards(int num_1, int num_2, int num_3, int num_4, int num_5,
			int num_6, int suit) {
		// Note: also tests skips trump number.
		Vector<Card> cards = new Vector<Card>();
		for (int i = 0; i < num_1; i++)
			cards.add(new Card(suit, Card.NUMBER_EIGHT));
		for (int i = 0; i < num_2; i++)
			cards.add(new Card(suit, Card.NUMBER_SEVEN));
		for (int i = 0; i < num_3; i++)
			cards.add(new Card(suit, Card.NUMBER_SIX));
		for (int i = 0; i < num_4; i++)
			cards.add(new Card(suit, Card.NUMBER_FIVE));
		for (int i = 0; i < num_5; i++)
			cards.add(new Card(suit, Card.NUMBER_FOUR));
		for (int i = 0; i < num_6; i++)
			cards.add(new Card(suit, Card.NUMBER_TWO));
		Card[] card_array = cards.toArray(new Card[0]);
		Card.Shuffle(card_array);
		return card_array;
	}
	
	/**
	 * Starts with the small joker, and then 3 of heart, 3 of spade, 3 of club, 3 of diamond, finishes
	 * with ace of heart.
	 * 
	 * @param num_1
	 * @param num_2
	 * @param num_3
	 * @param num_4
	 * @param num_5
	 * @param num_6
	 * @return
	 */
	static public Card[] CreateTrumpCards(int num_1, int num_2, int num_3, int num_4, int num_5,
			int num_6) {
		Vector<Card> cards = new Vector<Card>();
		for (int i = 0; i < num_1; i++)
			cards.add(new Card(Card.SUIT_NO_TRUMP, Card.NUMBER_GUARANTEE));
		for (int i = 0; i < num_2; i++)
			cards.add(new Card(Card.SUIT_HEART, Card.NUMBER_THREE));
		for (int i = 0; i < num_3; i++)
			cards.add(new Card(Card.SUIT_SPADE, Card.NUMBER_THREE));
		for (int i = 0; i < num_4; i++)
			cards.add(new Card(Card.SUIT_CLUB, Card.NUMBER_THREE));
		for (int i = 0; i < num_5; i++)
			cards.add(new Card(Card.SUIT_DIAMOND, Card.NUMBER_THREE));
		for (int i = 0; i < num_6; i++)
			cards.add(new Card(Card.SUIT_HEART, Card.NUMBER_ACE));
		Card[] card_array = cards.toArray(new Card[0]);
		Card.Shuffle(card_array);
		return card_array;
	}
	
	/**
	 * Check that the specific property p is the right type.
	 * 
	 * @param p
	 * @param leading_number
	 * @param num_identical_cards
	 * @param num_sequences
	 * @param num_cards
	 */
	static public void CheckCardProperty(SingletonCardProperty p, int leading_number,
			int num_identical_cards, int num_sequences, int num_cards) {
		if (leading_number >= 0)
			assertTrue(p.leading_number == leading_number);
		assertTrue(p.num_identical_cards == num_identical_cards);
		assertTrue(p.num_sequences == num_sequences);
		assertTrue(p.num_cards == num_cards);
	}

	public void testCardProperty() {
		Card[] trump_cards = CreateTrumpCards(2, 2, 1, 2, 2, 2);
		CardProperty property = new CardProperty(trump_cards, trump_suit, trump_number);
		assertFalse(property.is_mixed_suit);
		assertTrue(property.is_trump_suit);
		// First Property
		assertTrue(property.properties.size() == 3);
		Collections.sort(property.properties, comparator);
		CheckCardProperty(property.properties.get(0), SingletonCardProperty.SMALL_JOKER, 2, 4, 8);
		CheckCardProperty(property.properties.get(1), SingletonCardProperty.MINOR_TRUMP_NUMBER, 2, 1, 2);
		CheckCardProperty(property.properties.get(2), SingletonCardProperty.MINOR_TRUMP_NUMBER, 1, 1, 1);
		// Secondary Property
		assertTrue(property.secondary_properties.size() == 1);
		Collections.sort(property.secondary_properties, comparator);
		CheckCardProperty(property.secondary_properties.get(0), SingletonCardProperty.MINOR_TRUMP_NUMBER, 2, 2, 4);
	
		// Now something really crazy, tons of secondary properties.
		property = new CardProperty(CreateTrumpCards(2, 4, 1, 3, 5, 3), trump_suit, trump_number);
		assertFalse(property.is_mixed_suit);
		assertTrue(property.is_trump_suit);
		assertTrue(property.properties.size() == 5);
		Collections.sort(property.properties, comparator);
		CheckCardProperty(property.properties.get(0), SingletonCardProperty.MINOR_TRUMP_NUMBER, 5, 1, 5);
		CheckCardProperty(property.properties.get(1), SingletonCardProperty.MAJOR_TRUMP_NUMBER, 4, 1, 4);
		CheckCardProperty(property.properties.get(2), SingletonCardProperty.MINOR_TRUMP_NUMBER, 3, 2, 6);
		CheckCardProperty(property.properties.get(3), SingletonCardProperty.SMALL_JOKER, 2, 1, 2);
		CheckCardProperty(property.properties.get(4), SingletonCardProperty.MINOR_TRUMP_NUMBER, 1, 1, 1);
		// Note, if we don't delete duplicate secondary_properties, we actually generate the first
		// secondary property twice, because of the multiple minor trump number.
		assertTrue(property.secondary_properties.size() == 4);
		Collections.sort(property.secondary_properties, comparator);
		CheckCardProperty(property.secondary_properties.get(0), SingletonCardProperty.MAJOR_TRUMP_NUMBER, 4, 2, 8);
		CheckCardProperty(property.secondary_properties.get(1), SingletonCardProperty.MAJOR_TRUMP_NUMBER, 3, 3, 9);
		CheckCardProperty(property.secondary_properties.get(2), SingletonCardProperty.MINOR_TRUMP_NUMBER, 3, 2, 6);
		CheckCardProperty(property.secondary_properties.get(3), SingletonCardProperty.SMALL_JOKER, 2, 4, 8);
		
		// Something that doesn't involve trump suit, much, much cleaner.
		Card[] none_trump_cards = CreateNoneTrumpCards(2, 4, 1, 3, 5, 3, Card.SUIT_CLUB);
		property = new CardProperty(none_trump_cards, trump_suit, trump_number);
		assertFalse(property.is_mixed_suit);
		assertFalse(property.is_trump_suit);
		assertTrue(property.properties.size() == 6);
		Collections.sort(property.properties, comparator);
		CheckCardProperty(property.properties.get(0), SingletonCardProperty.FOUR, 5, 1, 5);
		CheckCardProperty(property.properties.get(1), SingletonCardProperty.SEVEN, 4, 1, 4);
		CheckCardProperty(property.properties.get(2), SingletonCardProperty.TWO, 3, 1, 3);
		CheckCardProperty(property.properties.get(3), SingletonCardProperty.FIVE, 3, 1, 3);
		CheckCardProperty(property.properties.get(4), SingletonCardProperty.EIGHT, 2, 1, 2);
		CheckCardProperty(property.properties.get(5), SingletonCardProperty.SIX, 1, 1, 1);
		assertTrue(property.secondary_properties.size() == 3);
		Collections.sort(property.secondary_properties, comparator);
		CheckCardProperty(property.secondary_properties.get(0), SingletonCardProperty.FIVE, 3, 3, 9);
		CheckCardProperty(property.secondary_properties.get(1), SingletonCardProperty.FOUR, 3, 2, 6);
		CheckCardProperty(property.secondary_properties.get(2), SingletonCardProperty.EIGHT, 2, 2, 4);
		
		// Mixed Suit.  Eliminate same secondary property doesn't go across suits.  Simply,
		// we combine all properties from all suits together.
		Card[] none_trump_cards_2 = CreateNoneTrumpCards(2, 4, 1, 3, 5, 3, Card.SUIT_SPADE);
		Card[] mixed_cards = new Card[trump_cards.length + none_trump_cards.length + none_trump_cards_2.length];
		System.arraycopy(trump_cards, 0 , mixed_cards, 0 , trump_cards.length);
		System.arraycopy(none_trump_cards, 0, mixed_cards, trump_cards.length, none_trump_cards.length);
		System.arraycopy(none_trump_cards_2, 0 , mixed_cards, trump_cards.length + none_trump_cards.length, none_trump_cards_2.length);
		Card.Shuffle(mixed_cards);
		property = new CardProperty(mixed_cards, trump_suit, trump_number);
		assertTrue(property.is_mixed_suit);
		assertFalse(property.is_trump_suit);
		assertTrue(property.properties.size() == 15);
		Collections.sort(property.properties, comparator);
		for (int i = 0; i < 2; ++i)
			CheckCardProperty(property.properties.get(i), SingletonCardProperty.FOUR, 5, 1, 5);
		for (int i = 2; i < 4; ++i)
			CheckCardProperty(property.properties.get(i), SingletonCardProperty.SEVEN, 4, 1, 4);
		CheckCardProperty(property.properties.get(4), SingletonCardProperty.SMALL_JOKER, 2, 4, 8);	
		for (int i = 5; i < 7; ++i)
			CheckCardProperty(property.properties.get(i), SingletonCardProperty.TWO, 3, 1, 3);
		for (int i = 7; i < 9; ++i)
			CheckCardProperty(property.properties.get(i), SingletonCardProperty.FIVE, 3, 1, 3);
		for (int i = 9; i < 11; ++i)
			CheckCardProperty(property.properties.get(i), SingletonCardProperty.EIGHT, 2, 1, 2);
		CheckCardProperty(property.properties.get(11), SingletonCardProperty.MINOR_TRUMP_NUMBER, 2, 1, 2);
		for (int i = 12; i < 14; ++i)
			CheckCardProperty(property.properties.get(i), SingletonCardProperty.SIX, 1, 1, 1);
		CheckCardProperty(property.properties.get(14), SingletonCardProperty.MINOR_TRUMP_NUMBER, 1, 1, 1);
		assertTrue(property.secondary_properties.size() == 7);
		Collections.sort(property.secondary_properties, comparator);
		for (int i = 0; i < 2; ++i)
			CheckCardProperty(property.secondary_properties.get(i), SingletonCardProperty.FIVE, 3, 3, 9);
		for (int i = 2; i < 4; ++i)
			CheckCardProperty(property.secondary_properties.get(i), SingletonCardProperty.FOUR, 3, 2, 6);
		for (int i = 4; i < 6; ++i)
			CheckCardProperty(property.secondary_properties.get(i), SingletonCardProperty.EIGHT, 2, 2, 4);
		CheckCardProperty(property.secondary_properties.get(6), SingletonCardProperty.MINOR_TRUMP_NUMBER, 2, 2, 4);
	}

}
