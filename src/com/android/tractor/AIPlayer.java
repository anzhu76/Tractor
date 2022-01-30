package com.android.tractor;

import java.util.Arrays;

import com.maqicheng.games.GameParam;
import com.maqicheng.games.GameServerInterface;

public class AIPlayer extends CardPlayer {
	static final boolean declare_suit = true;
	int dealsPlayed;
	int numDeals;
	Object endGameSignal = new Object(); // used to signal end game
	
	public void setPlayNumDeals(int num_deals) {
		if (num_deals > 0)
			numDeals = num_deals;
	}

	public AIPlayer(GameParam param, GameServerInterface downServer, GameServerInterface upServer) {
		super(param, downServer, upServer);
		dealsPlayed = 0;
		numDeals = -1;
	}
	
	@Override
	public void onDealACard(Card card) {
		super.onDealACard(card);
		// Just try to declare the suit of the card dealt in (aka. Fen Qing)
		// TODO: make AIDealAnalyzer take over the suit declaration as well?
		int suit_to_declare = analyzer.SuggestSuitToDeclare(MyCards(), declarable_suits,
				state.cards_per_player, state.num_decks, state.numPlayers,
				state.trump_number, state.declared_trump_suit);
		if (declare_suit && suit_to_declare != Card.SUIT_UNDEFINED) {
			// AIDealAnalyzer.SuggestSuitToDeclare(MyCards(), declarable_suits,
			//		state.cards_per_player, state.num_decks, state.numPlayers,
			//		state.trump_number, state.declared_trump_suit, AIMode);
			// Util.w("Declare Suit on deal", Integer.toString(state.playerId) + " " + Card.SuitToString(suit_to_declare));
			PlayerAction(TractorGameAction.DECLARE_TRUMP_SUIT_ACTION, suit_to_declare);
		}
		PlayerAction(TractorGameAction.READY_FOR_NEXT_CARD_ACTION, null);
	}
	
	@Override
	public void onNewDeal(int trump_number) {
		super.onNewDeal(trump_number);
		if (numDeals > 0)
			dealsPlayed++;
	}
	
	@Override
	public void onDealerSwappedTractorCards(int dealer_id) {
		super.onDealerSwappedTractorCards(dealer_id);
	}
	
	@Override
	public void onOthersPlayedCards(TractorGameStateUpdate update) {
		super.onOthersPlayedCards(update);
	}
	
	@Override
	public void onDealTractorCards(Card[] initial_tractor_cards) {
		super.onDealTractorCards(initial_tractor_cards);
		
		Card[] play_cards = AISuggestTractorCards(initial_tractor_cards);
		PlayerAction(TractorGameAction.SWAP_TRACTOR_CARD_ACTION, play_cards);
	}

	@Override
	public void onYourTurnToFollow() {
		super.onYourTurnToFollow();
		Util.d("AIPlayer" + Integer.toString(state.playerId) + " winning property: ", Arrays.toString(state.current_round_winning_property.ToCards()));
		Card[]  play_cards = AISuggestCardsToFollow();
		Util.d("AIPlayer" + Integer.toString(state.playerId) + " to Follow: ", Arrays.toString(MyCards()));
		Util.d("AIPlayer" + Integer.toString(state.playerId) + " to Play: ", Arrays.toString(play_cards));
		// CleanHand(play_cards); // don't CleanHand myself, will wait for an echo by PlayerPlaysCards
		PlayerAction(TractorGameAction.PLAY_CARD_ACTION, play_cards);
	}

	@Override
	public void onYourTurnToLead() {
		super.onYourTurnToLead();
		if (state.playerNumCards[state.playerId]==0) return; // endgame.
		// CleanHand(play_cards); // don't CleanHand myself, will wait for an echo by PlayerPlaysCards
		Util.d("AIPlayer" + Integer.toString(state.playerId) + " to lead: ", Arrays.toString(MyCards()));
		PlayerAction(TractorGameAction.PLAY_CARD_ACTION, AISuggestLeadingCards());	
	
		/*  Test illegal throw error message.
		Card[] cards = new Card[4];
		System.arraycopy(MyCards(), 0, cards, 0, 4);
		PlayerAction(TractorGameAction.PLAY_CARD_ACTION, cards);
		*/
	}

	@Override
	public void onLastHandResult() {
		super.onLastHandResult();
		if (numDeals > 0 && dealsPlayed >= numDeals)  {
			// end of the game.
			PlayerAction(TractorGameAction.ABANDON_CURRENT_GAME, null);
			synchronized (endGameSignal) {
				endGameSignal.notifyAll(); // in case someone is waiting on me to finish
			}
		}
		PlayerAction(TractorGameAction.NEW_DEAL_ACTION, null);
	}
	
	@Override
	public void onDeclareTrumpSuit(int player_id, int suit, int num_declared_suit_cards) {
		super.onDeclareTrumpSuit(player_id, suit, num_declared_suit_cards);
		//  Maybe declare a new suit right away?  If we haven't just done so.
		// Here is a natural situation that could occur: Player decided to declare a suit, in response to
		// somebody else's declaration.  While that action is being carried out, a card is dealt to that player
		// who just declared.  The player decided that yes, we should declare once again, since we haven't received
		// the update yet about the earlier declaration.  That way, the second declaration will become invalid.
		// Due to this fact, we'll turn off the declaration in this routine.
		/*
		if (player_id == state.playerId)
			return;
		int suit_to_declare = analyzer.SuggestSuitToDeclare(MyCards(), declarable_suits,
				state.cards_per_player, state.num_decks, state.numPlayers,
				state.trump_number, state.declared_trump_suit);
		if (suit_to_declare != Card.SUIT_UNDEFINED) {
			// AIDealAnalyzer.SuggestSuitToDeclare(MyCards(), declarable_suits,
			//		state.cards_per_player, state.num_decks, state.numPlayers,
			//		state.trump_number, state.declared_trump_suit, AIMode);
			// Util.w("Declare Suit on declare", Integer.toString(state.playerId) + " " + Card.SuitToString(suit_to_declare));
			PlayerAction(TractorGameAction.DECLARE_TRUMP_SUIT_ACTION, suit_to_declare);
		}
		*/
	}

	public void waitUntilGameEnded() {
		synchronized (endGameSignal) {
			while (dealsPlayed < numDeals) {
				try {
					endGameSignal.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
