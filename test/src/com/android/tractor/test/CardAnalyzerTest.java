package com.android.tractor.test;

import java.util.Collections;
import java.util.Vector;

import com.android.tractor.AIDealAnalyzer;
import com.android.tractor.Card;
import com.android.tractor.CardAnalyzer;
import com.android.tractor.CardProperty;
import com.android.tractor.SingletonCardProperty;
import com.android.tractor.SingletonCardPropertyComparator;
import com.android.tractor.TractorMessageCenter;

import junit.framework.TestCase;

public class CardAnalyzerTest extends TestCase {
	int trump_suit = Card.SUIT_HEART;
	int trump_number = Card.NUMBER_THREE;
	int decks = 5;
	int players = 4;
	CardAnalyzer analyzer = new CardAnalyzer(trump_suit,trump_number,decks, players);
	SingletonCardPropertyComparator comparator= new SingletonCardPropertyComparator(
			SingletonCardPropertyComparator.DE_TYPE_DE_LEADING_NUMBER, decks, players, trump_suit);
	
	/**
	 * Check that the card has the specified number and suit w.r.t. the Card class
	 * 
	 * @param card
	 * @param number
	 * @param suit
	 */
	static public void CheckCard(Card card, int number, int suit) {
		assertTrue(card.GetNumber() == number);
		assertTrue(card.GetSuit() == suit);
	}

	public void testFindAllForcedProperties() {
		// Test that we return the combination with higher probability.
		Card[] lead_play = CardPropertyTest.CreateNoneTrumpCards(3, 3, 2, 0, 0, 0, Card.SUIT_CLUB);
		CardProperty lead_property = new CardProperty(lead_play, trump_suit, trump_number);
		Card[] follow_play = CardPropertyTest.CreateNoneTrumpCards(2, 2, 3, 0, 0, 3, Card.SUIT_CLUB);
		Vector<SingletonCardProperty> missing_properties = new Vector<SingletonCardProperty>();
		Vector<SingletonCardProperty> forced_properties = analyzer.FindAllForcedProperties(lead_property.properties, follow_play, missing_properties);
		assertTrue(forced_properties.size() == 3);
		Collections.sort(forced_properties, comparator);
		CardPropertyTest.CheckCardProperty(forced_properties.get(0), Card.NUMBER_SIX, 3, 1, 3);
		CardPropertyTest.CheckCardProperty(forced_properties.get(1), Card.NUMBER_TWO, 3, 1, 3);
		CardPropertyTest.CheckCardProperty(forced_properties.get(2), Card.NUMBER_EIGHT, 2, 1, 2);
		assertTrue(follow_play.length == 10);
		assertTrue(missing_properties.size() == 1);
		CardPropertyTest.CheckCardProperty(missing_properties.get(0), -1, 3, 2, 6);
		
		// Test that we should not force more dominate properties, though pairs must be played.
		lead_play = CardPropertyTest.CreateNoneTrumpCards(2, 2, 0, 0, 0, 0, Card.SUIT_CLUB);
		lead_property = new CardProperty(lead_play, trump_suit, trump_number);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(2, 0, 2, 2, 2, 0, Card.SUIT_CLUB);
		missing_properties.clear();
		forced_properties = analyzer.FindAllForcedProperties(lead_property.properties, follow_play, missing_properties);
		assertTrue(forced_properties.size() == 2);
		Collections.sort(forced_properties, comparator);
		CardPropertyTest.CheckCardProperty(forced_properties.get(0), Card.NUMBER_EIGHT, 2, 1, 2);
		CardPropertyTest.CheckCardProperty(forced_properties.get(1), Card.NUMBER_SIX, 2, 1, 2);
		assertTrue(follow_play.length == 8);
		assertTrue(missing_properties.size() == 1);
		CardPropertyTest.CheckCardProperty(missing_properties.get(0), -1, 2, 2, 4);
		
		// Test that we honor property before points.
		lead_play = CardPropertyTest.CreateNoneTrumpCards(2, 0, 0, 0, 0, 0, Card.SUIT_CLUB);
		lead_property = new CardProperty(lead_play, trump_suit, trump_number);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(0, 2, 0, 1, 0, 1, Card.SUIT_CLUB);
		missing_properties.clear();
		forced_properties = analyzer.FindAllForcedProperties(lead_property.properties, follow_play, missing_properties);
		assertTrue(forced_properties.size() == 1);
		
		// Test that we match the properties with the smallest type first, i.e., if we have to match a triple, and
		// we have a 2*1, and 2*2, we should match up 2*1, instead of 2*2.
		lead_play = CardPropertyTest.CreateNoneTrumpCards(3, 2, 2, 0, 0, 0, Card.SUIT_CLUB);
		lead_property = new CardProperty(lead_play, trump_suit, trump_number);
		int[] num_cards = {2, 2, 0, 2, 0, 2, 0, 0, 2};
		follow_play = CardPropertyTest.CreateCards(num_cards, Card.SUIT_CLUB, Card.NUMBER_TEN);
		missing_properties.clear();
		forced_properties = analyzer.FindAllForcedProperties(lead_property.properties, follow_play, missing_properties);
		assertTrue(forced_properties.size() == 2);
		Collections.sort(forced_properties, comparator);
		CardPropertyTest.CheckCardProperty(forced_properties.get(0), Card.NUMBER_TEN, 2, 2, 4);
		CardPropertyTest.CheckCardProperty(forced_properties.get(1), Card.NUMBER_TWO, 2, 1, 2);
		
		// Test that we match up correctly with pairs with points when want_points is set to true.
		lead_play = CardPropertyTest.CreateNoneTrumpCards(2, 0, 0, 0, 0, 0, Card.SUIT_CLUB);
		lead_property = new CardProperty(lead_play, trump_suit, trump_number);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 2, 0, 2, Card.SUIT_CLUB);
		missing_properties.clear();
		forced_properties = analyzer.FindAllForcedProperties(lead_property.properties, follow_play, missing_properties);
		assertTrue(forced_properties.size() == 1);
		Collections.sort(forced_properties, comparator);
		CardPropertyTest.CheckCardProperty(forced_properties.get(0), Card.NUMBER_TWO, 2, 1, 2);
		
		// Test that we are returning missing properties properly.
		lead_play = CardPropertyTest.CreateNoneTrumpCards(3, 3, 0, 0, 0, 0, Card.SUIT_CLUB);
		lead_property = new CardProperty(lead_play, trump_suit, trump_number);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(3, 0, 1, 1, 1, 1, Card.SUIT_CLUB);
		missing_properties.clear();
		forced_properties = analyzer.FindAllForcedProperties(lead_property.properties, follow_play, missing_properties);
		assertTrue(missing_properties.size() == 1);
		CardPropertyTest.CheckCardProperty(missing_properties.get(0), -1, 2, 1, 2);
		
		// More complicated missing property case:
		lead_play = CardPropertyTest.CreateNoneTrumpCards(3, 3, 3, 0, 0, 0, Card.SUIT_CLUB);
		lead_property = new CardProperty(lead_play, trump_suit, trump_number);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(2, 2, 1, 1, 2, 1, Card.SUIT_CLUB);
		missing_properties.clear();
		forced_properties = analyzer.FindAllForcedProperties(lead_property.properties, follow_play, missing_properties);
		assertTrue(missing_properties.size() == 2);
		Collections.sort(missing_properties, comparator);
		CardPropertyTest.CheckCardProperty(missing_properties.get(0), -1, 3, 1, 3);
		CardPropertyTest.CheckCardProperty(missing_properties.get(1), -1, 2, 3, 6);
		
		// no missing property case.
		lead_play = CardPropertyTest.CreateNoneTrumpCards(3, 3, 0, 0, 0, 0, Card.SUIT_CLUB);
		lead_property = new CardProperty(lead_play, trump_suit, trump_number);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(3, 3, 1, 1, 2, 1, Card.SUIT_CLUB);
		missing_properties.clear();
		forced_properties = analyzer.FindAllForcedProperties(lead_property.properties, follow_play, missing_properties);
		assertTrue(missing_properties.size() == 0);
		
		lead_play = CardPropertyTest.CreateNoneTrumpCards(4, 4, 0, 0, 0, 0, Card.SUIT_CLUB);
		lead_property = new CardProperty(lead_play, trump_suit, trump_number);
		int[] follow_play_nums = {2, 0, 2, 0, 2, 0, 2, 0, 2};
		follow_play = CardPropertyTest.CreateCards(follow_play_nums, Card.SUIT_CLUB, Card.NUMBER_ACE);
		missing_properties.clear();
		forced_properties = analyzer.FindAllForcedProperties(lead_property.properties, follow_play, missing_properties);
		assertTrue(forced_properties.size() == 4);
		
		int trumpNumber = Card.NUMBER_KING;
		int trumpSuit = Card.SUIT_SPADE;
		int numDecks = 20;
		int numPlayers = 6;
		lead_play = CardPropertyTest.CreateCards(new int[]{5,5}, Card.SUIT_DIAMOND, Card.NUMBER_TEN);
		lead_property = new CardProperty(lead_play, trumpSuit, trumpNumber);
		Card[] current_hand = CardPropertyTest.CreateCards(new int[]{2, 0, 2, 0, 2, 1, 1, 3, 3, 3, 3},  Card.SUIT_DIAMOND, Card.NUMBER_ACE);
		CardAnalyzer new_analyzer = new CardAnalyzer(trumpSuit,trumpNumber, numDecks, numPlayers);
		Vector<SingletonCardProperty> properties = (Vector<SingletonCardProperty>) lead_property.properties.clone();
		forced_properties = new_analyzer.FindAllForcedProperties(properties, current_hand, missing_properties);
		properties = (Vector<SingletonCardProperty>) lead_property.properties.clone();
		Vector<SingletonCardProperty> ppp = new_analyzer.FindAllForcedProperties(properties, current_hand, missing_properties);
		Card[] return_hand = new Card[10];
		Vector<SingletonCardProperty> pppp = (Vector<SingletonCardProperty>) ppp.clone();
		pppp.remove(0);
		new_analyzer.FindBestCovering(ppp.get(0), pppp, current_hand, return_hand, CardAnalyzer.VALIDATE_FOLLOW_NO_POINT_MODE);
		new_analyzer.FindBestCovering(ppp.get(0), pppp, current_hand, return_hand, CardAnalyzer.VALIDATE_FOLLOW_POINT_MODE);
		assertTrue(ppp.size() == 4);
	}
	
	public void testFindLegalLeadingCards() {
		// An illegal throw, a pair with higher leading number.
		Card[] lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 1, 0, 1, 3, 0, Card.SUIT_CLUB);
		Card[] hand1 = CardPropertyTest.CreateNoneTrumpCards(2, 0, 0, 1, 1, 0, Card.SUIT_CLUB);
		Card[] hand2 = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 0, 1, 0, Card.SUIT_CLUB);
		Vector<Card[]> hands = new Vector<Card[]>();
		hands.add(hand1);
		hands.add(hand2);
		Card[] legal_play = analyzer.FindLegalLeadingCards(lead_play, hands);
		assertTrue(legal_play.length == 1);
		CheckCard(legal_play[0], Card.NUMBER_SEVEN, Card.SUIT_CLUB);
		
		// Legal throw.
		hands.clear();
		hands.add(hand2);
		legal_play = analyzer.FindLegalLeadingCards(lead_play, hands);
		assertTrue(legal_play == lead_play);
		
		// trump suit throw, and others don't have trump suit.
		lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 1, 1, 0, Card.SUIT_HEART);
		hands.clear();
		hands.add(hand1);
		hands.add(hand2);
		legal_play = analyzer.FindLegalLeadingCards(lead_play, hands);
		assertTrue(legal_play == lead_play);
	}
	
	public void testIsWinningPlay() {
		Card[] lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 2, 2, 1, Card.SUIT_CLUB);
		Card[] follow_play = CardPropertyTest.CreateNoneTrumpCards(3, 2, 0, 0, 0, 0, Card.SUIT_HEART);
		SingletonCardProperty p = analyzer.GetWinningPropertyForPlay(lead_play);
		SingletonCardProperty pp = analyzer.IsWinningPlay(lead_play, follow_play, p);
		assertTrue(pp != null);
		CardPropertyTest.CheckCardProperty(pp, Card.NUMBER_EIGHT, 2, 2, 4);
		
		lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 2, 2, 0, Card.SUIT_CLUB);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(4, 0, 0, 0, 0, 0, Card.SUIT_CLUB);
		p = analyzer.GetWinningPropertyForPlay(lead_play);
		pp = analyzer.IsWinningPlay(lead_play, follow_play, p);
		assertTrue(pp == null);
		
		lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 2, 2, 0, Card.SUIT_CLUB);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(2, 2, 0, 0, 0, 0, Card.SUIT_CLUB);
		p = analyzer.GetWinningPropertyForPlay(lead_play);
		pp = analyzer.IsWinningPlay(lead_play, follow_play, p);
		assertTrue(pp != null);
		CardPropertyTest.CheckCardProperty(pp, Card.NUMBER_EIGHT, 2, 2, 4);
		
		lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 2, 0, 2, 0, 2, Card.SUIT_CLUB);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(2, 2, 2, 0, 0, 0, Card.SUIT_HEART);
		p = analyzer.GetWinningPropertyForPlay(lead_play);
		pp = analyzer.IsWinningPlay(lead_play, follow_play, p);
		assertTrue(pp != null);
		CardPropertyTest.CheckCardProperty(pp, Card.NUMBER_EIGHT, 2, 1, 2);
		
		lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 2, 0, 0, 0, 3, Card.SUIT_CLUB);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(5, 0, 0, 0, 0, 0, Card.SUIT_HEART);
		p = analyzer.GetWinningPropertyForPlay(lead_play);
		pp = analyzer.IsWinningPlay(lead_play, follow_play, p);
		assertTrue(pp != null);
		CardPropertyTest.CheckCardProperty(pp, Card.NUMBER_EIGHT, 3, 1, 3);
	}
	
	public void testIsFollowPlayFollowProperty() {
		Card[] lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 2, 2, 0, Card.SUIT_CLUB);
		Card[] follow_play = CardPropertyTest.CreateNoneTrumpCards(2, 1, 1, 0, 0, 0, Card.SUIT_CLUB);
		Card[] current_hand = CardPropertyTest.CreateNoneTrumpCards(3, 1, 2, 0, 2, 2, Card.SUIT_CLUB);
		int[] error_message = new int[2];
		Vector<SingletonCardProperty> properties = new Vector<SingletonCardProperty>();
		assertFalse(analyzer.IsFollowPlayFollowProperty(lead_play, follow_play, current_hand, error_message, properties));
		assertTrue(error_message[0] == TractorMessageCenter.FOLLOW_PROPERTY);
		assertTrue(TractorMessageCenter.generateMessage(error_message[0], 0, 0, 0, properties).contentEquals("Must follow cards with type(s):\nConsecutive 2 sequences of pairs"));
		follow_play = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 0, 2, 2, Card.SUIT_CLUB);
		assertTrue(analyzer.IsFollowPlayFollowProperty(lead_play, follow_play, current_hand, error_message, properties));
		follow_play = CardPropertyTest.CreateNoneTrumpCards(3, 1, 0, 0, 0, 0, Card.SUIT_CLUB);
		current_hand = CardPropertyTest.CreateNoneTrumpCards(3, 1, 1, 0, 0, 2, Card.SUIT_CLUB);
		assertTrue(analyzer.IsFollowPlayFollowProperty(lead_play, follow_play, current_hand, error_message, properties));
		
		
		lead_play = CardPropertyTest.CreateNoneTrumpCards(3, 3, 0, 0, 2, 1, Card.SUIT_CLUB);
		current_hand = CardPropertyTest.CreateNoneTrumpCards(3, 0, 3, 2, 2, 3, Card.SUIT_CLUB);
		follow_play = CardPropertyTest.CreateNoneTrumpCards(1, 1, 0, 2, 2, 3, Card.SUIT_CLUB);
		assertFalse(analyzer.IsFollowPlayFollowProperty(lead_play, follow_play, current_hand, error_message, properties));
		assertTrue(error_message[0] == TractorMessageCenter.FOLLOW_PROPERTY);
		assertTrue(TractorMessageCenter.generateMessage(error_message[0], 0, 0, 0, properties).contentEquals("Must follow cards with type(s):\nA triple\nA triple\nA pair"));
		follow_play = CardPropertyTest.CreateNoneTrumpCards(3, 0, 3, 1, 1, 1, Card.SUIT_CLUB);
		assertFalse(analyzer.IsFollowPlayFollowProperty(lead_play, follow_play, current_hand, error_message, properties));
		assertTrue(error_message[0] == TractorMessageCenter.FOLLOW_PROPERTY);
		assertTrue(TractorMessageCenter.generateMessage(error_message[0], 0, 0, 0, properties).contentEquals("Must follow cards with type(s):\nA triple\nA triple\nA pair"));
		follow_play = CardPropertyTest.CreateNoneTrumpCards(3, 0, 3, 0, 0, 3, Card.SUIT_CLUB);
		assertTrue(analyzer.IsFollowPlayFollowProperty(lead_play, follow_play, current_hand, error_message, properties));
		follow_play = CardPropertyTest.CreateNoneTrumpCards(3, 0, 3, 2, 0, 1, Card.SUIT_CLUB);
		assertTrue(analyzer.IsFollowPlayFollowProperty(lead_play, follow_play, current_hand, error_message, properties));
	}

	public void testFindMaxPropertiesIndex() {
		// Just a sanity check that two triples is more important than 2*2 pairs
		Vector<SingletonCardProperty> properties_1 = new Vector<SingletonCardProperty>();
		Vector<SingletonCardProperty> properties_2 = new Vector<SingletonCardProperty>();
		properties_1.add(SingletonCardProperty.CreatePropertyOfType(2, 2));
		properties_2.add(SingletonCardProperty.CreatePropertyOfType(3, 1));
		properties_2.add(SingletonCardProperty.CreatePropertyOfType(3, 1));
		Vector<Vector<SingletonCardProperty> > vector = new Vector<Vector<SingletonCardProperty> >();
		vector.add(properties_1);
		vector.add(properties_2);
		assertTrue(analyzer.FindMaxPropertiesIndex(vector) == 0);
		assertTrue(vector.get(0).size() == 2);
		CardPropertyTest.CheckCardProperty(vector.get(0).get(0), -1, 3, 1, 3);
		CardPropertyTest.CheckCardProperty(vector.get(0).get(1), -1, 3, 1, 3);
	}
	

	public void testGetWinningPropertyForPlay() {
		CardPropertyTest.CheckCardProperty(analyzer.GetWinningPropertyForPlay(CardPropertyTest.CreateNoneTrumpCards(2, 2, 0, 0, 2, 2, Card.SUIT_SPADE)),
				SingletonCardProperty.EIGHT, 2, 2, 4);
		
		CardPropertyTest.CheckCardProperty(analyzer.GetWinningPropertyForPlay(CardPropertyTest.CreateNoneTrumpCards(2, 4, 3, 1, 4, 4, Card.SUIT_SPADE)),
				SingletonCardProperty.FOUR, 4, 2, 8);
		
		Card[] trump_part1 = CardPropertyTest.CreateTrumpCards(0, 2, 2, 0, 0, 0);
		Card[] trump_part2 = CardPropertyTest.CreateNoneTrumpCards(2, 2, 0, 0, 0, 0, Card.SUIT_HEART);
		Card[] trump = new Card[trump_part1.length + trump_part2.length];
		System.arraycopy(trump_part1, 0, trump, 0, trump_part1.length);
		System.arraycopy(trump_part2, 0, trump, trump_part1.length, trump_part2.length);
		CardPropertyTest.CheckCardProperty(analyzer.GetWinningPropertyForPlay(trump),
				SingletonCardProperty.MAJOR_TRUMP_NUMBER, 2, 2, 4);
	}
	
	public void testSelectCardTargetingPoints() {
		Card[]  trump_cards = CardPropertyTest.CreateCards(new int[]{2, 2, 1 ,2}, trump_suit, Card.NUMBER_NO_GUARANTEE);
		Vector<Card> cards = analyzer.SelectCardTargetingPoints(trump_cards, 4, 0, false);
		assertTrue(cards.size() == 4);
		Collections.sort(cards, analyzer.card_comparator);
		CheckCard(cards.get(0), Card.NUMBER_GUARANTEE, Card.SUIT_NO_TRUMP);
		CheckCard(cards.get(1), Card.NUMBER_ACE, Card.SUIT_HEART);
		CheckCard(cards.get(2), Card.NUMBER_KING, Card.SUIT_HEART);
		CheckCard(cards.get(3), Card.NUMBER_KING, Card.SUIT_HEART);
		
		cards = analyzer.SelectCardTargetingPoints(trump_cards, 2, 0, false);
		assertTrue(cards.size() == 2);
		Collections.sort(cards, analyzer.card_comparator);
		CheckCard(cards.get(0), Card.NUMBER_ACE, Card.SUIT_HEART);
		CheckCard(cards.get(1), Card.NUMBER_KING, Card.SUIT_HEART);
		
		cards = analyzer.SelectCardTargetingPoints(trump_cards, 6, 0, false);
		assertTrue(cards.size() == 6);
		Collections.sort(cards, analyzer.card_comparator);
		CheckCard(cards.get(0), Card.NUMBER_NO_GUARANTEE, Card.SUIT_NO_TRUMP);
		CheckCard(cards.get(1), Card.NUMBER_GUARANTEE, Card.SUIT_NO_TRUMP);
		CheckCard(cards.get(2), Card.NUMBER_GUARANTEE, Card.SUIT_NO_TRUMP);
		CheckCard(cards.get(3), Card.NUMBER_ACE, Card.SUIT_HEART);
		CheckCard(cards.get(4), Card.NUMBER_KING, Card.SUIT_HEART);
		CheckCard(cards.get(5), Card.NUMBER_KING, Card.SUIT_HEART);
	}
}
