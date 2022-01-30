package com.android.tractor.test;

import java.util.Collections;
import java.util.Vector;

import com.android.tractor.Card;
import com.android.tractor.CardProperty;
import com.android.tractor.CardTracker;
import com.android.tractor.SingletonCardProperty;

import junit.framework.TestCase;

public class CardTrackerTest extends TestCase {
	// tolerance for float point error
	private static final double EPSILON = 1e-9;

	public void testCurrentPropretyProbability() {
		int trumpSuit = Card.SUIT_HEART;
		int trumpNumber = Card.NUMBER_TEN;
		int numDecks = 2;
		int num_other_players = 3;

		CardTracker deck = new CardTracker(trumpSuit, trumpNumber, numDecks);
		// First test, AKKJJ, A KK should both be good.  JJ, QQ can beat it, and there are 1/3 of the chance
		// that the rest of the three players have a pair QQ.
		Vector<Integer> num_array = new Vector<Integer>();
		num_array.add(1);
		num_array.add(2);
		num_array.add(0);
		num_array.add(2);
		Card[] my_hand = CardPropertyTest.CreateCards(num_array, Card.SUIT_DIAMOND, Card.NUMBER_ACE);
		deck.DeleteCards(my_hand);
		CardProperty property = new CardProperty(my_hand, trumpSuit, trumpNumber);
		for (SingletonCardProperty p: property.properties) {
			double probability = deck.CurrentPropertyProbability(p, num_other_players, false);
			switch (p.leading_number) {
			case Card.NUMBER_ACE:
				assertEquals(probability, 1.0);
				break;
			case Card.NUMBER_KING:
				assertEquals(probability, 1.0);
				break;
			case Card.NUMBER_JACK:
				assertEquals((double) 2/3, probability, EPSILON);
				break;
			default:
				assertFalse(true);
			}
		}
		// Now I'm curious, what about say K5544? :).  I know about K, but 5544?
		num_array.clear();
		num_array.add(1);
		for (int i = 0; i < 7; i++)
			num_array.add(0);
		for (int i = 0; i < 2; i++)
			num_array.add(2);
		my_hand = CardPropertyTest.CreateCards(num_array, Card.SUIT_CLUB, Card.NUMBER_KING);
		deck.DeleteCards(my_hand);
		property = new CardProperty(my_hand, trumpSuit, trumpNumber);
		for (SingletonCardProperty p: property.properties) {
			double probability = deck.CurrentPropertyProbability(p, num_other_players, false);
			switch (p.leading_number) {
			case Card.NUMBER_KING:
				assertEquals(0.0, probability);
				break;
			case Card.NUMBER_FIVE:
				assertEquals(probability, 0.8280335219957866, EPSILON);  // Not bad.
				break;
			default:
				assertFalse(true);
			}
		}
		// Now how about adding a few more cards? say a 9 and a 7.  Now only QQJJ can beat the current
		// hand.  T is the trump number.  It used to be (QQJJ, JJ99, 9988, 8877, 7766).
		num_array.clear();
		num_array.add(1);
		num_array.add(0);
		num_array.add(1);
		deck.DeleteCards(CardPropertyTest.CreateCards(num_array, Card.SUIT_CLUB, Card.NUMBER_NINE));
		for (SingletonCardProperty p: property.properties) {
			double probability = deck.CurrentPropertyProbability(p, num_other_players, false);
			switch (p.leading_number) {
			case Card.NUMBER_KING:
				assertTrue(probability == 0.0);
				break;
			case Card.NUMBER_FIVE:
				assertEquals(probability, 0.9629629629629629, EPSILON);  // Much better.
				break;
			default:
				assertFalse(true);
			}
		}
		// Now deal with trump suit, sigh, the MINOR_TRUMP_NUMBER.  In this case, we'll have the following probabilities:
		// BJ-BJ-SJ-SJ SJ-SJ-3-3 3-3-3-3 3-3-3-3, 3-3-A-A  3-3-A-A (we expired a pair of MINOR 3's, and we didn't expire A).
		num_array.clear();
		num_array.add(1);
		deck.DeleteCards(CardPropertyTest.CreateCards(num_array, Card.SUIT_DIAMOND, Card.NUMBER_TEN));
		SingletonCardProperty new_property = new SingletonCardProperty(new Card(Card.SUIT_HEART, Card.NUMBER_ACE),
				trumpSuit, trumpNumber);
		new_property.num_identical_cards = 2;
		new_property.num_sequences = 2;
		new_property.num_cards = 4;
		new_property.is_consecutive = true;
		double probability = deck.CurrentPropertyProbability(new_property, num_other_players, false);
		assertEquals(probability, 0.7973656137737204, EPSILON);
		
		// Here is the probability of a consecutive pair, without any other card's support.
		deck = new CardTracker(trumpSuit, trumpNumber, 3);
		int[] num_cards = {2,2};
		my_hand = CardPropertyTest.CreateCards(num_cards, Card.SUIT_DIAMOND, Card.NUMBER_THREE);
		deck.DeleteCards(my_hand);
		property = new CardProperty(my_hand, trumpSuit, trumpNumber);
		for (SingletonCardProperty p: property.properties) {
			probability = deck.CurrentPropertyProbability(p, 5, false);
			switch (p.leading_number) {
			case Card.NUMBER_THREE:
				assertEquals(probability, 0.521729561129189, EPSILON);
				break;
			default:
				assertFalse(true);
			}
		}
		
		//  Now compare triples with 2-2-2's, for a 4 player game.
		int[] num_cards_2 = {3, 0, 0, 0, 0, 0, 2, 2, 2};
		my_hand = CardPropertyTest.CreateCards(num_cards_2, trumpSuit, Card.NUMBER_GUARANTEE);
		deck.DeleteCards(my_hand);
		property = new CardProperty(my_hand, trumpSuit, trumpNumber);
		for (SingletonCardProperty p: property.properties) {
			probability = deck.CurrentPropertyProbability(p, 3, false);
			switch (p.leading_number) {
			case SingletonCardProperty.SMALL_JOKER:
				assertEquals(probability, 0.8888888888888888, EPSILON);
				break;
			case Card.NUMBER_NINE:
				assertEquals(probability, 0.41035899217694916, EPSILON);
				break;
			default:
				assertFalse(true);
			}
		}
		// Now compare in the non-trump suit.  So with the help of lots of MINOR_TRUMP_NUMBER, consecutive
		// one have much lower probability in trumps.
		int[] num_cards_3 = {3, 0, 0, 0, 0, 0, 2, 2, 2};
		my_hand = CardPropertyTest.CreateCards(num_cards_3, Card.SUIT_SPADE, Card.NUMBER_KING);
		deck.DeleteCards(my_hand);
		property = new CardProperty(my_hand, trumpSuit, trumpNumber);
		for (SingletonCardProperty p: property.properties) {
			probability = deck.CurrentPropertyProbability(p, 3, false);
			switch (p.leading_number) {
			case Card.NUMBER_KING:
				assertEquals(probability, 0.8888888888888888, EPSILON);
				break;
			case Card.NUMBER_SEVEN:
				assertEquals(probability, 0.8003703444039424, EPSILON);
				break;
			default:
				assertFalse(true);
			}
		}
		
		deck = new CardTracker(trumpSuit, trumpNumber, 3);
		int[] num_cards_4 = {1, 2};
		my_hand = CardPropertyTest.CreateCards(num_cards_4, Card.SUIT_HEART, Card.NUMBER_NO_GUARANTEE);
		deck.DeleteCards(my_hand);
		property = new CardProperty(my_hand, trumpSuit, trumpNumber);
		for (SingletonCardProperty p: property.properties) {
			probability = deck.CurrentPropertyProbability(p, 3, false);
			switch (p.leading_number) {
			case SingletonCardProperty.BIG_JOKER:
				assertTrue(probability == 1.0);
				break;
			case SingletonCardProperty.SMALL_JOKER:
				assertEquals(probability, 0.6666666666666666, EPSILON);
				break;
			default:
				assertFalse(true);
			}
		}
	}
}
