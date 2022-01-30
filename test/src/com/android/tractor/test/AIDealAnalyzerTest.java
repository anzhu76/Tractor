package com.android.tractor.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import com.android.tractor.AIDealAnalyzer;
import com.android.tractor.Card;
import com.android.tractor.FollowCardSet;
import com.android.tractor.SingletonCardProperty;

import junit.framework.TestCase;

public class AIDealAnalyzerTest extends TestCase {
	int trumpSuit = Card.SUIT_HEART;
	int trumpNumber = Card.NUMBER_TWO;
	int numDecks = 2;
	int numPlayers = 4;
	int myId = 1;
	Vector<Integer> dealerGroup =  new Vector<Integer>();
	
	public void testSuggestTractorCards() {
		AIDealAnalyzer analyzer = new AIDealAnalyzer(trumpSuit, trumpNumber, numPlayers, numDecks, myId);
		// Create hand
		int[] suit_spade = {1, 1, 2};
		int[] suit_club = {1, 2, 0, 1, 0 ,2, 1};
		int[] suit_diamond = {2, 0, 0, 0, 1, 1, 0, 1};
		int[] suit_heart = {1, 1, 1, 1, 1, 1, 1, 1};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_spade, Card.SUIT_SPADE, Card.NUMBER_ACE));
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_heart, Card.SUIT_HEART, Card.NUMBER_ACE));
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club, Card.SUIT_CLUB, Card.NUMBER_ACE));
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_diamond, Card.SUIT_DIAMOND, Card.NUMBER_ACE));
		Vector<Card> tractor_cards = analyzer.SuggestTractorCards(myId, 6);
		Collections.sort(tractor_cards);
		CardAnalyzerTest.CheckCard(tractor_cards.get(0), Card.NUMBER_SEVEN, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(1), Card.NUMBER_NINE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(2), Card.NUMBER_EIGHT, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(tractor_cards.get(3), Card.NUMBER_NINE, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(tractor_cards.get(4), Card.NUMBER_NINE, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(tractor_cards.get(5), Card.NUMBER_JACK, Card.SUIT_CLUB);
		// What if we want more tractor cards?
		tractor_cards = analyzer.SuggestTractorCards(myId, 9);
		Collections.sort(tractor_cards);
		CardAnalyzerTest.CheckCard(tractor_cards.get(0), Card.NUMBER_SEVEN, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(1), Card.NUMBER_NINE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(2), Card.NUMBER_TEN, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(3), Card.NUMBER_EIGHT, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(tractor_cards.get(4), Card.NUMBER_NINE, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(tractor_cards.get(5), Card.NUMBER_NINE, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(tractor_cards.get(6), Card.NUMBER_JACK, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(tractor_cards.get(7), Card.NUMBER_KING, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(tractor_cards.get(8), Card.NUMBER_ACE, Card.SUIT_SPADE);
		
		// Add a few more trump cards to make AI bury more points.
		int[] suit_heart_2 = {1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_heart_2, Card.SUIT_HEART, Card.NUMBER_ACE));
		tractor_cards = analyzer.SuggestTractorCards(myId, 6);
		Collections.sort(tractor_cards);
		CardAnalyzerTest.CheckCard(tractor_cards.get(0), Card.NUMBER_SEVEN, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(1), Card.NUMBER_NINE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(2), Card.NUMBER_TEN, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(3), Card.NUMBER_EIGHT, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(tractor_cards.get(4), Card.NUMBER_JACK, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(tractor_cards.get(5), Card.NUMBER_KING, Card.SUIT_SPADE);
		
		// Now a new setup.  Too many points for any suit
		analyzer = new AIDealAnalyzer(trumpSuit, trumpNumber, numPlayers, numDecks, myId);
		int[] suit_diamond_2 = {1, 0, 0, 1};
		int[] suit_club_2 = {2, 0, 0, 2};
		int[] suit_spade_2 = {1, 0, 0, 2};
		// no trumps!
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_spade_2, Card.SUIT_SPADE, Card.NUMBER_KING));
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_diamond_2, Card.SUIT_DIAMOND, Card.NUMBER_KING));
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club_2, Card.SUIT_CLUB, Card.NUMBER_KING));
		tractor_cards = analyzer.SuggestTractorCards(myId, 4);
		Collections.sort(tractor_cards);
		CardAnalyzerTest.CheckCard(tractor_cards.get(0), Card.NUMBER_TEN, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(1), Card.NUMBER_KING, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(tractor_cards.get(2), Card.NUMBER_TEN, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(tractor_cards.get(3), Card.NUMBER_KING, Card.SUIT_SPADE);
	}

	public void testSugguestLeadingCardsRuleBased() {
		AIDealAnalyzer analyzer = new AIDealAnalyzer(trumpSuit, trumpNumber, numPlayers, numDecks, myId);
		dealerGroup.add(0);
		dealerGroup.add(2);
		// Create hand of interest
		int[] suit_club = {1, 2, 0, 1, 0 ,2, 1};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club, Card.SUIT_CLUB, Card.NUMBER_ACE));
		Card[] lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 3);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_KING, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(lead_cards[1], Card.NUMBER_KING, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(lead_cards[2], Card.NUMBER_ACE, Card.SUIT_CLUB);
		Card[] play_cards = new Card[2];
		play_cards[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_KING);
		play_cards[1] = new Card(Card.SUIT_CLUB, Card.NUMBER_KING);
		analyzer.PlayerPlayedCards(play_cards, myId, play_cards);
		Card[] follow_cards = new Card[2];
		follow_cards[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_THREE);
		follow_cards[1] = new Card(Card.SUIT_CLUB, Card.NUMBER_FOUR);
		// myId = 0
		analyzer.PlayerPlayedCards(follow_cards, (myId + 1) % numPlayers, play_cards);
		analyzer.PlayerPlayedCards(follow_cards, (myId + 2) % numPlayers, play_cards);
		follow_cards[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_FIVE);
		follow_cards[1] = new Card(Card.SUIT_CLUB, Card.NUMBER_SIX);
		analyzer.PlayerPlayedCards(follow_cards, (myId + 3) % numPlayers, play_cards);
		// Need to be able to recognize that since nobody followed with pairs, pairs are good.
		lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 3);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_NINE, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(lead_cards[1], Card.NUMBER_NINE, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(lead_cards[2], Card.NUMBER_ACE, Card.SUIT_CLUB);
		
		// Now test triple vs. pair:
		analyzer = new AIDealAnalyzer(trumpSuit, trumpNumber, numPlayers, 3, myId);
		// Create hand of interest
		int[] suit_club_1 = {1, 2, 0, 2, 0 ,2, 1};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club_1, Card.SUIT_CLUB, Card.NUMBER_ACE));
		lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 1);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_ACE, Card.SUIT_CLUB);
		play_cards[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_KING);;
		play_cards[1] = new Card(Card.SUIT_CLUB, Card.NUMBER_KING);;
		analyzer.PlayerPlayedCards(play_cards, myId, play_cards);
		follow_cards[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_THREE);
		follow_cards[1] = new Card(Card.SUIT_CLUB, Card.NUMBER_FOUR);
		// myId = 0
		analyzer.PlayerPlayedCards(follow_cards, (myId + 1) % numPlayers, play_cards);
		analyzer.PlayerPlayedCards(follow_cards, (myId + 2) % numPlayers, play_cards);
		analyzer.PlayerPlayedCards(follow_cards, (myId + 3) % numPlayers, play_cards);
		// Need to be able to recognize that even though nobody played pairs, pairs are not good.
		// And so we can't play the two small pairs all together.  Actually, the simpler function
		// at the moment doesn't consider triples, it just considers pairs.
		lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 1);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_ACE, Card.SUIT_CLUB);
		
		// Now test that we can property recognize that others might trump us if we have played too many cards.
		// Now test triple vs. pair:
		analyzer = new AIDealAnalyzer(trumpSuit, trumpNumber, numPlayers, numDecks, myId);
		int[] suit_club_2 = {1, 2, 0, 2, 0 ,2, 1};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club_2, Card.SUIT_CLUB, Card.NUMBER_ACE));
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club_2, Card.SUIT_SPADE, Card.NUMBER_ACE));
		lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 3);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_KING, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(lead_cards[1], Card.NUMBER_KING, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(lead_cards[2], Card.NUMBER_ACE, Card.SUIT_CLUB);
		analyzer.PlayerPlayedCards(lead_cards, myId, lead_cards);
		play_cards = new Card[3];
		play_cards[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_QUEEN);
		play_cards[1] = new Card(Card.SUIT_SPADE, Card.NUMBER_EIGHT);
		play_cards[2] = new Card(Card.SUIT_SPADE, Card.NUMBER_SEVEN);
		analyzer.PlayerPlayedCards(play_cards, (myId + 1) % numPlayers, lead_cards);
		lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 3);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_KING, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(lead_cards[1], Card.NUMBER_KING, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(lead_cards[2], Card.NUMBER_ACE, Card.SUIT_SPADE);
		
		// Lead play with single cards should also be covered
		analyzer = new AIDealAnalyzer(trumpSuit, trumpNumber, numPlayers, numDecks, myId);
		int[] suit_club_3 = {1, 2, 1, 1, 1 ,1, 1};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club_3, Card.SUIT_CLUB, Card.NUMBER_ACE));
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club_2, Card.SUIT_SPADE, Card.NUMBER_ACE));
		lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 3);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_KING, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(lead_cards[1], Card.NUMBER_KING, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(lead_cards[2], Card.NUMBER_ACE, Card.SUIT_CLUB);
		analyzer.PlayerPlayedCards(lead_cards, myId, lead_cards);
		play_cards = new Card[3];
		play_cards[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_QUEEN);
		play_cards[1] = new Card(Card.SUIT_SPADE, Card.NUMBER_EIGHT);
		play_cards[2] = new Card(Card.SUIT_SPADE, Card.NUMBER_SEVEN);
		analyzer.PlayerPlayedCards(play_cards, (myId + 1) % numPlayers, lead_cards);
		play_cards[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_JACK);
		play_cards[1] = new Card(Card.SUIT_CLUB, Card.NUMBER_ACE);
		play_cards[2] = new Card(Card.SUIT_SPADE, Card.NUMBER_FOUR);
		analyzer.PlayerPlayedCards(play_cards, (myId + 2) % numPlayers, lead_cards);
		lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 3);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_KING, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(lead_cards[1], Card.NUMBER_KING, Card.SUIT_SPADE);
		CardAnalyzerTest.CheckCard(lead_cards[2], Card.NUMBER_ACE, Card.SUIT_SPADE);
		
		// Suggest void suit
		analyzer = new AIDealAnalyzer(trumpSuit, trumpNumber, numPlayers, numDecks, myId);
		int[] suit_club_4 = {1, 1, 0, 2, 0 ,1, 1};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club_4, Card.SUIT_CLUB, Card.NUMBER_ACE));
		lead_cards = new Card[1];
		lead_cards[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_ACE);
		analyzer.PlayerPlayedCards(lead_cards, myId, lead_cards);
		play_cards = new Card[1];
		play_cards[0] = new Card(Card.SUIT_SPADE, Card.NUMBER_FIVE);
		analyzer.PlayerPlayedCards(play_cards, (myId + 2) % numPlayers, lead_cards);
		// Now make 2 don't have a trump pair.  But since we are going to play a single card, this is OK.
		analyzer.PlayerPlayedCards(CardPropertyTest.CreateNoneTrumpCards(0, 1, 1, 0, 0, 0, trumpSuit), 2, CardPropertyTest.CreateNoneTrumpCards(2, 0, 0, 0, 0, 0, trumpSuit));
		lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 1);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_KING, Card.SUIT_CLUB);
		
		// don't suggest a suit has high chance of being trumped
		analyzer = new AIDealAnalyzer(trumpSuit, trumpNumber, numPlayers, numDecks, myId);
		int[] suit_club_5 = {2, 2, 2, 2, 2 ,2, 2, 2, 2, 2, 1};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_club_5, Card.SUIT_CLUB, Card.NUMBER_ACE));
		int[] suit_diamond = {2};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_diamond, Card.SUIT_DIAMOND, Card.NUMBER_KING));
		int[] suit_trump = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
		analyzer.AddCardsToMyHand(CardPropertyTest.CreateCards(suit_trump, trumpSuit, Card.NUMBER_ACE));
		int[] lead_club = {2, 2, 2, 2, 2 ,2, 2, 2, 2, 0, 1};
		lead_cards = CardPropertyTest.CreateCards(lead_club, Card.SUIT_CLUB, Card.NUMBER_ACE);
		analyzer.PlayerPlayedCards(lead_cards, myId, lead_cards);
		lead_cards = analyzer.SugguestLeadingCardsRuleBased(myId, dealerGroup);
		assertTrue(lead_cards.length == 2);
		Arrays.sort(lead_cards);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_KING, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(lead_cards[0], Card.NUMBER_KING, Card.SUIT_DIAMOND);
	}
	
	public void testSelectCardTargetingPoints() {
		AIDealAnalyzer analyzer = new AIDealAnalyzer(trumpSuit, trumpNumber, numPlayers, numDecks, myId);
		int[] suit_club = {2, 2, 1, 1};
		Vector<Card> selected_cards =
			analyzer.SelectCardTargetingPoints(CardPropertyTest.CreateVectorCards(suit_club, Card.SUIT_CLUB, Card.NUMBER_TEN), 1, Integer.MAX_VALUE, true);
		assertTrue(selected_cards.size() == 1);
		Collections.sort(selected_cards);
		CardAnalyzerTest.CheckCard(selected_cards.get(0), Card.NUMBER_TEN, Card.SUIT_CLUB);
		
		int[] suit_club_2 = {2, 2};
		selected_cards =
			analyzer.SelectCardTargetingPoints(CardPropertyTest.CreateVectorCards(suit_club_2, Card.SUIT_CLUB, Card.NUMBER_ACE), 1, 0, true);
		assertTrue(selected_cards.size() == 1);
		Collections.sort(selected_cards);
		CardAnalyzerTest.CheckCard(selected_cards.get(0), Card.NUMBER_ACE, Card.SUIT_CLUB);
		selected_cards =
			analyzer.SelectCardTargetingPoints(CardPropertyTest.CreateVectorCards(suit_club_2, Card.SUIT_CLUB, Card.NUMBER_ACE), 3, 0, true);
		// We wouldn't be breaking up the properties when respecting points limit.
		assertTrue(selected_cards.size() == 0);
		selected_cards =
			analyzer.SelectCardTargetingPoints(CardPropertyTest.CreateVectorCards(suit_club_2, Card.SUIT_CLUB, Card.NUMBER_ACE), 3, 0, false);
		assertTrue(selected_cards.size() == 3);
		Collections.sort(selected_cards);
		CardAnalyzerTest.CheckCard(selected_cards.get(0), Card.NUMBER_KING, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(selected_cards.get(1), Card.NUMBER_ACE, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(selected_cards.get(2), Card.NUMBER_ACE, Card.SUIT_CLUB);
	}
	
	
	public void testCompleteFollowCardSet() {
		AIDealAnalyzer analyzer = new AIDealAnalyzer(Card.SUIT_HEART, Card.NUMBER_THREE, numPlayers, numDecks, myId);
		
		Card[] lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 2, 2, 0, Card.SUIT_CLUB);
		Card[] hand_part1 = CardPropertyTest.CreateTrumpCards(1, 3, 1, 1, 2, 0);
		Card[] hand_part2 = CardPropertyTest.CreateNoneTrumpCards(2, 0, 4, 0, 2, 0, Card.SUIT_CLUB);
		Card[] hand_part3 = CardPropertyTest.CreateNoneTrumpCards(2, 0, 0, 0, 2, 0, Card.SUIT_SPADE);
		Card[] hand = new Card[hand_part1.length + hand_part3.length];
		System.arraycopy(hand_part1, 0 , hand, 0, hand_part1.length);
		System.arraycopy(hand_part3, 0, hand, hand_part1.length, hand_part3.length);
		FollowCardSet set = new FollowCardSet(FollowCardSet.MAX_POINTS, FollowCardSet.MAX_POSSIBLE, FollowCardSet.TRUMP_FOLLOW);
		SingletonCardProperty winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 4);
		CardPropertyTest.CheckCardProperty(winning_property, SingletonCardProperty.MAJOR_TRUMP_NUMBER, 2, 2, 4);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_THREE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_THREE, Card.SUIT_DIAMOND);
		
		System.arraycopy(hand_part2, 0 , hand, 0, hand_part2.length);
		set.play_attribute = FollowCardSet.NO_LEAD_FOLLOW;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 4);
		assertTrue(winning_property == null);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_EIGHT, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_EIGHT, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_FOUR, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_FOUR, Card.SUIT_CLUB);
		
		lead_play = CardPropertyTest.CreateNoneTrumpCards(0, 0, 0, 2, 0, 2, Card.SUIT_CLUB);
		hand_part3 = CardPropertyTest.CreateNoneTrumpCards(2, 0, 0, 2, 0, 0, Card.SUIT_HEART);
		System.arraycopy(hand_part1, 0 , hand, 0, hand_part1.length);
		System.arraycopy(hand_part3, 0, hand, hand_part1.length, hand_part3.length);
		set.play_attribute = FollowCardSet.TRUMP_FOLLOW;
		set.lead_magnitude_attribute = FollowCardSet.MAX_POSSIBLE;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 4);
		CardPropertyTest.CheckCardProperty(winning_property, SingletonCardProperty.MAJOR_TRUMP_NUMBER, 2, 1, 2);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_FIVE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_FIVE, Card.SUIT_HEART);
		set.point_attribute = FollowCardSet.MIN_POINTS;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 4);
		CardPropertyTest.CheckCardProperty(winning_property, SingletonCardProperty.MAJOR_TRUMP_NUMBER, 2, 1, 2);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_EIGHT, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_EIGHT, Card.SUIT_HEART);
		set.lead_magnitude_attribute = FollowCardSet.MIN_POSSIBLE;
		set.point_attribute = FollowCardSet.MAX_POINTS;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 4);
		CardPropertyTest.CheckCardProperty(winning_property, SingletonCardProperty.FIVE, 2, 1, 2);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_EIGHT, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_EIGHT, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_FIVE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_FIVE, Card.SUIT_HEART);
		set.point_attribute = FollowCardSet.MIN_POINTS;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 4);
		CardPropertyTest.CheckCardProperty(winning_property, SingletonCardProperty.EIGHT, 2, 1, 2);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_THREE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_THREE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_EIGHT, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_EIGHT, Card.SUIT_HEART);
		
		lead_play = CardPropertyTest.CreateNoneTrumpCards(3, 0, 0, 3, 0, 2, Card.SUIT_CLUB);
		hand_part1 = CardPropertyTest.CreateTrumpCards(1, 3, 1, 1, 3, 3);
		System.arraycopy(hand_part1, 0 , hand, 0, hand_part1.length);
		set.play_attribute = FollowCardSet.TRUMP_FOLLOW;
		set.lead_magnitude_attribute = FollowCardSet.MAX_POSSIBLE;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 8);
		CardPropertyTest.CheckCardProperty(winning_property, SingletonCardProperty.MAJOR_TRUMP_NUMBER, 3, 1, 3);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_THREE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(set.follow_cards[4], Card.NUMBER_THREE, Card.SUIT_DIAMOND);
		CardAnalyzerTest.CheckCard(set.follow_cards[5], Card.NUMBER_ACE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[6], Card.NUMBER_ACE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[7], Card.NUMBER_ACE, Card.SUIT_HEART);
		
		lead_play = CardPropertyTest.CreateNoneTrumpCards(2, 0, 0, 2, 0, 1, Card.SUIT_CLUB);
		hand_part1 = CardPropertyTest.CreateTrumpCards(1, 3, 1, 1, 3, 3);
		System.arraycopy(hand_part1, 0 , hand, 0, hand_part1.length);
		set.play_attribute = FollowCardSet.TRUMP_FOLLOW;
		set.lead_magnitude_attribute = FollowCardSet.MAX_POSSIBLE;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 5);
		CardPropertyTest.CheckCardProperty(winning_property, SingletonCardProperty.MAJOR_TRUMP_NUMBER, 2, 1, 2);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_THREE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_ACE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_ACE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[4], Card.NUMBER_ACE, Card.SUIT_HEART);
		
		// Check that even though we are avoiding points, we should not take QQJJ to match the 2*2 first.  Because then TTKK
		// can't trump the rest.
		lead_play = CardPropertyTest.CreateNoneTrumpCards(2, 2, 0, 2, 2, 0, Card.SUIT_CLUB);
		int[] num_cards = { 2, 2, 2, 2, 1};
		hand = CardPropertyTest.CreateCards(num_cards, Card.SUIT_HEART, Card.NUMBER_KING);
		set.play_attribute = FollowCardSet.TRUMP_FOLLOW;
		set.lead_magnitude_attribute = FollowCardSet.MIN_POSSIBLE;
		set.point_attribute = FollowCardSet.MIN_POINTS;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 8);
		CardPropertyTest.CheckCardProperty(winning_property, SingletonCardProperty.JACK, 2, 2, 4);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_KING, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_KING, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_QUEEN, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_QUEEN, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[4], Card.NUMBER_JACK, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[5], Card.NUMBER_JACK, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[6], Card.NUMBER_TEN, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[7], Card.NUMBER_TEN, Card.SUIT_HEART);

		int[] num_lead_cards = {2};
		lead_play = CardPropertyTest.CreateCards(num_lead_cards, Card.SUIT_CLUB, Card.NUMBER_ACE);
		int[] num_hand_cards = {2, 0, 0, 0, 2};
		hand = CardPropertyTest.CreateCards(num_hand_cards, Card.SUIT_CLUB, Card.NUMBER_TEN);
		set.play_attribute = FollowCardSet.NO_LEAD_FOLLOW;
		set.point_attribute = FollowCardSet.MAX_POINTS;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 2);
		assertTrue(winning_property == null);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_TEN, Card.SUIT_CLUB);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_TEN, Card.SUIT_CLUB);
		
		analyzer = new AIDealAnalyzer(Card.SUIT_HEART, Card.NUMBER_FIVE, 6, 3, myId);
		lead_play = CardPropertyTest.CreateCards(new int[]{2, 2}, Card.SUIT_HEART, Card.NUMBER_NINE);
		hand = CardPropertyTest.CreateCards(new int[] { 2, 2, 0, 2}, Card.SUIT_HEART, Card.NUMBER_ACE);
		Card[] real_hand = new Card[hand.length + 2];
		real_hand[0] = new Card(Card.SUIT_CLUB, Card.NUMBER_FIVE);
		real_hand[1] = new Card(Card.SUIT_CLUB, Card.NUMBER_FIVE);
		System.arraycopy(hand, 0, real_hand, 2, hand.length);
		set.play_attribute = FollowCardSet.NO_LEAD_FOLLOW;
		set.point_attribute = FollowCardSet.MIN_POINTS;
		winning_property = analyzer.CompleteFollowCardSet(lead_play, real_hand, analyzer.GetWinningPropertyForPlay(lead_play), set);
		assertTrue(set.follow_cards.length == 4);
		assertTrue(winning_property == null);
		CardAnalyzerTest.CheckCard(set.follow_cards[0], Card.NUMBER_ACE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[1], Card.NUMBER_ACE, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[2], Card.NUMBER_JACK, Card.SUIT_HEART);
		CardAnalyzerTest.CheckCard(set.follow_cards[3], Card.NUMBER_JACK, Card.SUIT_HEART);
	}
	
	public void testSuggestCardsToFollowRuleBased() {
		int trump_suit = Card.SUIT_HEART;
		int trump_number = Card.NUMBER_THREE;
		int num_players = 6;
		int num_decks = 4;
		int my_id = 1;
		AIDealAnalyzer analyzer = new AIDealAnalyzer(trump_suit, trump_number, num_players, num_decks, my_id);
		int[] my_trump = {3, 1, 1, 1};
		Card[] trump_cards = CardPropertyTest.CreateCards(my_trump, trump_suit, Card.NUMBER_NO_GUARANTEE);
		analyzer.AddCardsToMyHand(trump_cards);
		Card[] lead_play = new Card[1];
		lead_play[0] = new Card(Card.SUIT_NO_TRUMP, Card.NUMBER_GUARANTEE);
		SingletonCardProperty current_winning_property = new SingletonCardProperty(lead_play[0], trump_suit, trump_number);
		Vector<Integer> dealer_group = new Vector<Integer>();
		dealer_group.add(0);
		dealer_group.add(2);
		dealer_group.add(4);
		Card[] follow_play = analyzer.SuggestCardsToFollowRuleBased(my_id, lead_play, trump_cards, current_winning_property, true, 2, dealer_group);
		assertTrue(follow_play.length == 1);
		// We should not be playing KING of heart!  Ideally, not big joker, either, but maybe this is for later.
		CardAnalyzerTest.CheckCard(follow_play[0], Card.NUMBER_NO_GUARANTEE, Card.SUIT_NO_TRUMP);
		
		analyzer = new AIDealAnalyzer(trump_suit, trump_number, num_players, num_decks, my_id);
		int[] my_trump_2 = {2, 2, 1, 1};
		trump_cards = CardPropertyTest.CreateCards(my_trump_2, trump_suit, Card.NUMBER_NO_GUARANTEE);
		analyzer.AddCardsToMyHand(trump_cards);
		follow_play = analyzer.SuggestCardsToFollowRuleBased(my_id, lead_play, trump_cards, current_winning_property, false, 2, dealer_group);
		assertTrue(follow_play.length == 1);
		// Should be playing the big joker!
		CardAnalyzerTest.CheckCard(follow_play[0], Card.NUMBER_NO_GUARANTEE, Card.SUIT_NO_TRUMP);
		
		analyzer = new AIDealAnalyzer(Card.SUIT_DIAMOND, Card.NUMBER_THREE, 4, 2, my_id);
		trump_cards = CardPropertyTest.CreateTrumpCards(1, 1, 2, 0, 1, 0);
		Card[] more_trumps = CardPropertyTest.CreateCards(new int[]{2}, Card.SUIT_DIAMOND, Card.NUMBER_KING);
		Card[] total_trump = new Card[trump_cards.length + more_trumps.length];
		System.arraycopy(more_trumps, 0, total_trump, 0, more_trumps.length);
		System.arraycopy(trump_cards, 0, total_trump, more_trumps.length, trump_cards.length);
		analyzer.AddCardsToMyHand(total_trump);
		current_winning_property = new SingletonCardProperty(lead_play[0], Card.SUIT_DIAMOND, Card.NUMBER_THREE);
		follow_play = analyzer.SuggestCardsToFollowRuleBased(my_id, lead_play, total_trump, current_winning_property, false, 1, dealer_group);
		assertTrue(follow_play.length == 1);
		CardAnalyzerTest.CheckCard(follow_play[0], Card.NUMBER_THREE, Card.SUIT_HEART);
		
		analyzer = new AIDealAnalyzer(trump_suit, trump_number, num_players, num_decks, my_id);
		trump_cards = CardPropertyTest.CreateCards(my_trump, trump_suit, Card.NUMBER_GUARANTEE);
		analyzer.AddCardsToMyHand(trump_cards);
		lead_play[0] = new Card(Card.SUIT_HEART, Card.NUMBER_ACE);
		current_winning_property = new SingletonCardProperty(lead_play[0], trump_suit, trump_number);
		follow_play = analyzer.SuggestCardsToFollowRuleBased(my_id, lead_play, trump_cards, current_winning_property, true, 2, dealer_group);
		assertTrue(follow_play.length == 1);
		// We should not be playing small joker.  It's not winning, and doesn't achieve anything useful.
		CardAnalyzerTest.CheckCard(follow_play[0], Card.NUMBER_QUEEN, Card.SUIT_HEART);
	}
}
