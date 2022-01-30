package com.android.tractor.test;

import java.util.Arrays;
import java.util.Vector;

import com.android.tractor.Card;
import com.android.tractor.CardAnalyzer;
import com.android.tractor.CardOrganizer;
import com.android.tractor.SingletonCardProperty;

import junit.framework.TestCase;

public class CardOrganizerTest extends TestCase {
	int trumpSuit = Card.SUIT_HEART;
	int trumpNumber = Card.NUMBER_TEN;
	int numPlayers = 4;
	int numDecks = 6;
	CardAnalyzer analyzer = new CardAnalyzer(trumpSuit, trumpNumber, numDecks, numPlayers);

	
	public void testAddCards() {
		CardOrganizer organizer = new CardOrganizer(trumpSuit, trumpNumber, analyzer);
		int[] num_cards = {1, 0, 2, 2};
		Card[] card_part1 = CardPropertyTest.CreateCards(num_cards, Card.SUIT_SPADE, Card.NUMBER_QUEEN);
		int[] num_cards2 = {1, 0, 2, 2, 1};
		Card[] card_part2 = CardPropertyTest.CreateCards(num_cards2, Card.SUIT_DIAMOND, Card.NUMBER_SEVEN);
		Card[] all_cards = new Card[card_part1.length + card_part2.length];
		System.arraycopy(card_part1, 0, all_cards, 0, card_part1.length);
		System.arraycopy(card_part2, 0, all_cards, card_part1.length, card_part2.length);
		Card.Shuffle(all_cards);
		organizer.AddCards(all_cards, null);
		Card[] card_suit1 = organizer.GetSuit(Card.SUIT_SPADE).toArray(new Card[0]);
		assertTrue(card_suit1.length == 3);
		CardAnalyzerTest.CheckCard(card_suit1[0], Card.NUMBER_QUEEN, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(card_suit1[1], Card.NUMBER_NINE, Card.SUIT_SPADE);  // We added T of spade, that's trump, shouldn't appear in spade.
		CardAnalyzerTest.CheckCard(card_suit1[2], Card.NUMBER_NINE, Card.SUIT_SPADE);
		card_suit1 = organizer.GetSuit(Card.SUIT_HEART).toArray(new Card[0]);  // trump suit
		assertTrue(card_suit1.length == 2);
		CardAnalyzerTest.CheckCard(card_suit1[0], Card.NUMBER_TEN, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(card_suit1[1], Card.NUMBER_TEN, Card.SUIT_SPADE);
		card_suit1 = organizer.GetSuit(Card.SUIT_DIAMOND).toArray(new Card[0]);  // trump suit
		assertTrue(card_suit1.length == 6);
		CardAnalyzerTest.CheckCard(card_suit1[0], Card.NUMBER_SEVEN, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(card_suit1[1], Card.NUMBER_FIVE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(card_suit1[2], Card.NUMBER_FIVE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(card_suit1[3], Card.NUMBER_FOUR, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(card_suit1[4], Card.NUMBER_FOUR, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(card_suit1[5], Card.NUMBER_THREE, Card.SUIT_DIAMOND);
		int[] num_cards3 = {2, 2, 3, 1};
		organizer.AddCards(CardPropertyTest.CreateCards(num_cards3, Card.SUIT_CLUB, Card.NUMBER_JACK), null);
		card_suit1 = organizer.GetSuit(Card.SUIT_HEART).toArray(new Card[0]);  // trump suit
		assertTrue(card_suit1.length == 4);
		CardAnalyzerTest.CheckCard(card_suit1[0], Card.NUMBER_TEN, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(card_suit1[1], Card.NUMBER_TEN, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(card_suit1[2], Card.NUMBER_TEN, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(card_suit1[3], Card.NUMBER_TEN, Card.SUIT_CLUB);
		int[] num_cards4 = {1, 0, 1, 0, 0, 0, 1};
		organizer.AddCards(CardPropertyTest.CreateCards(num_cards4, Card.SUIT_HEART, Card.NUMBER_NO_GUARANTEE), null);
		card_suit1 = organizer.GetSuit(Card.SUIT_HEART).toArray(new Card[0]);  // trump suit
		assertTrue(card_suit1.length == 7);
		CardAnalyzerTest.CheckCard(card_suit1[0], Card.NUMBER_NO_GUARANTEE, Card.SUIT_NO_TRUMP);
		CardAnalyzerTest.CheckCard(card_suit1[1], Card.NUMBER_TEN, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(card_suit1[2], Card.NUMBER_TEN, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(card_suit1[3], Card.NUMBER_TEN, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(card_suit1[4], Card.NUMBER_TEN, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(card_suit1[5], Card.NUMBER_TEN, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(card_suit1[6], Card.NUMBER_ACE, Card.SUIT_HEART);
		card_suit1 = organizer.GetSuit(Card.SUIT_NO_TRUMP).toArray(new Card[0]);
		assertTrue(card_suit1.length == 0);
	}

	public void testDeleteCards() {
		CardOrganizer organizer = new CardOrganizer(trumpSuit, trumpNumber, analyzer);
		int[] num_cards = {1, 0, 2, 2};
		Card[] card_part1 = CardPropertyTest.CreateCards(num_cards, Card.SUIT_SPADE, Card.NUMBER_QUEEN);
		organizer.AddCards(card_part1, null);
		int[] num_cards2 = {1, 0, 2, 2, 1};
		card_part1 = CardPropertyTest.CreateCards(num_cards2, Card.SUIT_DIAMOND, Card.NUMBER_SEVEN);
		organizer.AddCards(card_part1, null);
		int[] num_cards3 = {1, 0, 1, 0, 1};
		card_part1 = CardPropertyTest.CreateCards(num_cards3, Card.SUIT_DIAMOND, Card.NUMBER_SEVEN);
		organizer.DeleteCards(card_part1);
		Card[] card_suit1 = organizer.GetSuit(Card.SUIT_DIAMOND).toArray(new Card[0]); 
		assertTrue(card_suit1.length == 3);
		CardAnalyzerTest.CheckCard(card_suit1[0], Card.NUMBER_FIVE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(card_suit1[1], Card.NUMBER_FOUR, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(card_suit1[2], Card.NUMBER_FOUR, Card.SUIT_DIAMOND);
	}
	
	public void testIsPropertyAvailableInSuit() {
		CardOrganizer organizer = new CardOrganizer(trumpSuit, trumpNumber, analyzer);
		int[] num_play_cards = {2};
		int[] lead_cards = {3};
		organizer.AddCards(CardPropertyTest.CreateCards(num_play_cards, Card.SUIT_DIAMOND, Card.NUMBER_ACE),
				CardPropertyTest.CreateCards(lead_cards, Card.SUIT_DIAMOND, Card.NUMBER_EIGHT));
		SingletonCardProperty p = SingletonCardProperty.CreatePropertyOfType(3, 1);
		Vector<SingletonCardProperty> possible_properties = organizer.IsPropertyAvailableInSuit(p, Card.SUIT_DIAMOND);
		assertTrue(possible_properties.size() == 1);
		CardPropertyTest.CheckCardProperty(possible_properties.get(0), -1, 4, 1, 4);
		p = SingletonCardProperty.CreatePropertyOfType(3, 2);
		possible_properties = organizer.IsPropertyAvailableInSuit(p, Card.SUIT_DIAMOND);
		assertTrue(possible_properties.size() == 1);
		CardPropertyTest.CheckCardProperty(possible_properties.get(0), -1, 4, 2, 8);
		int[] num_play_cards_2 = {2, 2};
		int[] lead_cards_2 = {4};
		organizer.AddCards(CardPropertyTest.CreateCards(num_play_cards_2, Card.SUIT_DIAMOND, Card.NUMBER_ACE),
				CardPropertyTest.CreateCards(lead_cards_2, Card.SUIT_DIAMOND, Card.NUMBER_EIGHT));
		possible_properties = organizer.IsPropertyAvailableInSuit(p, Card.SUIT_DIAMOND);
		assertTrue(possible_properties.size() == 1);
		CardPropertyTest.CheckCardProperty(possible_properties.get(0), -1, 5, 2, 10);
		p = SingletonCardProperty.CreatePropertyOfType(2, 1);
		possible_properties = organizer.IsPropertyAvailableInSuit(p, Card.SUIT_DIAMOND);
		assertTrue(possible_properties.size() == 1);
		CardPropertyTest.CheckCardProperty(possible_properties.get(0), -1, 2, 1, 2);
		int[] num_play_cards_3 = {3, 2};
		int[] lead_cards_3 = {5};
		organizer.AddCards(CardPropertyTest.CreateCards(num_play_cards_3, Card.SUIT_DIAMOND, Card.NUMBER_ACE),
				CardPropertyTest.CreateCards(lead_cards_3, Card.SUIT_DIAMOND, Card.NUMBER_EIGHT));
		possible_properties = organizer.IsPropertyAvailableInSuit(p, Card.SUIT_DIAMOND);
		assertTrue(possible_properties.size() == 1);
		CardPropertyTest.CheckCardProperty(possible_properties.get(0), -1, 2, 1, 2);
		p = SingletonCardProperty.CreatePropertyOfType(3, 2);
		possible_properties = organizer.IsPropertyAvailableInSuit(p, Card.SUIT_DIAMOND);
		assertTrue(possible_properties.size() == 1);
		CardPropertyTest.CheckCardProperty(possible_properties.get(0), -1, 5, 2, 10);
		p = SingletonCardProperty.CreatePropertyOfType(1, 1);
		possible_properties = organizer.IsPropertyAvailableInSuit(p, Card.SUIT_DIAMOND);
		assertTrue(possible_properties.size() == 1);
	}
}
