/*
 * Copyright (C) 2008 An Zhu, Qicheng Ma.
 * 
 * Android independent.
 * 
 * This is an implementation of the human player that is actually
 * playing the game on the phone.  It holds local information to the player.
 * The local information here is for display purpose though, which might be a bit
 * behind the local information stored in the TractorGameState, which is also
 * owned by the player.
 */
package com.android.tractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import com.maqicheng.games.GameParam;
import com.maqicheng.games.GameServerInterface;


public class HumanPlayer extends CardPlayer {
	
	/*
	 * This is shared with GameController, or whatever display mechanism is there.
	 * Every time we are ready to
	 * call the GameController to display something, we acquire the lock,
	 * i.e., make sure that previous display has finished.  And then once
	 * we grab the lock, we are making sure that the intended display will
	 * indeed happen.  This will make sure that the internal state of HumanPlayer
	 * is not modified when display is happening (i.e., we are not
	 * dealt another card, etc.).
	 */
	private Semaphore game_lock_;
	
	// Made this a generic interface, so we can develope different GUI client.
	private IHumanPlayerController game_controller_;
	
	private int[] display_suit_mapping_;
	
	// Even though this is stored in TractorGameState, there is no way that we
	// can delay the displaying of certain cards.  For this reason, we'll need
	// to store the played cards history ourselves, for display purposes only.
	private List<Card[]> per_round_played_cards_;
	int current_round_played_players_;
	int current_round_winning_id_;
	int current_round_num_cards_;
	protected int per_deal_defender_group_score_;
	boolean current_deal_ended_;
	String error_message;
	Card[] error_cards;
	// For smoothness during dealing, keep a local copy of cards, so to the user, we
	// always see cards appearing one by one
	Vector<Card> local_cards_for_dealing_mode_only;
	// Suit declaration also need to be localized.
	int suit_declarer_id;
	int num_declared_suit_cards;
	int declared_trump_suit;
	
	// communicate to GameController if we want to pre-select some cards, e.g. forced to play
	protected  Card[] pre_select_cards_;
	
	public HumanPlayer(GameParam param, IHumanPlayerController game_controller, GameServerInterface downServer,
			GameServerInterface upServer) {
		super(param, downServer, upServer);
		game_controller_ = game_controller;
		state.trump_suit = Card.SUIT_UNDEFINED;
		game_lock_ = game_controller.getGameControllerLock();
		display_suit_mapping_ = new int[GameController.NUM_DISPLAY_COL];
		current_deal_ended_ = false;
		local_cards_for_dealing_mode_only = new Vector<Card>();
		suit_declarer_id = -1;
		num_declared_suit_cards = -1;
		declared_trump_suit = Card.SUIT_UNDEFINED;
		InitDisplaySuitMapping();
	}
	
	private void AcquireLock() {
		try {
			game_lock_.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void ReleaseLock() {
		game_lock_.release();
	}
	
	@Override
	public void onDealACard(Card card) {
		AcquireLock();
		super.onDealACard(card);
		local_cards_for_dealing_mode_only.add(card);
		SetGameControllerMode(GameController.DEALING_MODE);
		// TractorGameAction.READY_FOR_NEXT_DEAL_ACTION will be called by GameController upon
		// exiting the display thread.
	}
	
	public int[] CurrentDisplaySuitMapping() {
		return display_suit_mapping_;
	}
	
	public int GetTrumpNumber() {
		return state.trump_number;
	}

	// The following two functions, CurrentRoundHistory() and CurrentDisplayHand()
	// are used to display cards to GameController.
	public List<Card[]> CurrentRoundHistory() {
		for (Card[] cards : per_round_played_cards_) {
			if (cards != null) {
				Arrays.sort(cards, new PerDealCardComparator(state.trump_suit, state.trump_number));
			}
		}
		return per_round_played_cards_;
	}
	public Card[][] CurrentDisplayHand(int mode) {	
		Card[] current_clean_hand = Card.GetRidOfNullCards(MyCards());
		if (mode == GameController.DEALING_MODE) {
			current_clean_hand = local_cards_for_dealing_mode_only.toArray(new Card[0]);
		}
		// Log.d("CurrentDisplayHand " + player_id_, "Before: " + Arrays.toString(current_clean_hand));
		int num_cards = current_clean_hand.length;
		Arrays.sort(current_clean_hand, new PerDealCardComparator(state.trump_suit, state.trump_number));
		// Log.d("CurrentDisplayHand " + player_id_, "After: " + Arrays.toString(current_clean_hand));
		InitDisplaySuitMapping();
		if (state.trump_suit != Card.SUIT_NO_TRUMP
				&& state.trump_suit != Card.SUIT_UNDEFINED) {
			int temp_suit = display_suit_mapping_[1];
			display_suit_mapping_[1] = state.trump_suit;
			display_suit_mapping_[GameController.NUM_DISPLAY_COL - 1 - state.trump_suit] = temp_suit;
		}
		
    	Card[][] display_hand = new Card[GameController.NUM_DISPLAY_COL][num_cards];
    	int second_index = 0;
    	int first_index = 0;
    	int current_suit = Card.SUIT_NO_TRUMP;
    	for (int i = 0; i < num_cards;++i) {
    		int hand_suit = current_clean_hand[i].GetSuit();
    		if (current_clean_hand[i].GetNumber() == state.trump_number) {
    			hand_suit = Card.SUIT_NO_TRUMP;
    		}
    		if (hand_suit != current_suit) {
    			first_index = -1;
    			while (display_suit_mapping_[++first_index] != hand_suit);
    			second_index = 0;
    			current_suit = hand_suit;
    		}
    		
    		display_hand[first_index][second_index++] = current_clean_hand[i];
    	}
    	return display_hand;
	}
	
	private void InitDisplaySuitMapping() {
		for (int i = 0; i < GameController.NUM_DISPLAY_COL; ++i) {
			display_suit_mapping_[i] = GameController.NUM_DISPLAY_COL - 1 - i;
		}
	}
	
	/**
	 * Get the player id that wins the current round.  -1 means that the round
	 * is not over yet.
	 * 
	 * @return player_id or -1.
	 */
	public int getWinnerOfCurrentRound() {
		if (current_round_played_players_ == 0)
			return current_round_winning_id_;
		return -1;
	}
	
	
	private int GetDefenderGroupScore() {
		int total_score = 0;
		for (int i = 0; i < state.numPlayers; ++i) {
			if (!state.dealerGroup.contains(i))
				total_score += state.currentDealScores[i];
		}
		return total_score;
	}
	

	@Override
	public void onOthersPlayedCards(TractorGameStateUpdate update) {
		AcquireLock();
		super.onOthersPlayedCards(update);
		
		current_round_winning_id_ = update.suit;
		current_round_played_players_ = (current_round_played_players_ + 1) % state.numPlayers;;
		current_round_num_cards_ = update.cards.length;
		// add to per round history
		if (current_round_played_players_ == 1) {
			// we started a new round.  Clear per_round_history.
			ClearPerRoundHistory();
		}
		if (current_round_played_players_ == 0) {
			// We finished the current round, update defender group scoring
			per_deal_defender_group_score_ = GetDefenderGroupScore();
		}
		per_round_played_cards_.set(update.otherPlayerId, update.cards);
		// redraw
		SetGameControllerMode(GameController.WAITING_MODE);
	}

	@Override
	public void onDealTractorCards(Card[] initial_tractor_cards) {
		AcquireLock();
		super.onDealTractorCards(initial_tractor_cards);
		this.pre_select_cards_ = AISuggestTractorCards(initial_tractor_cards);
		SetGameControllerMode(GameController.TRACTOR_CARD_SWAPPING_MODE);
	}
	
	@Override
	public void onDealerSwappedTractorCards(int dealer_id) {
		AcquireLock();
		super.onDealerSwappedTractorCards(dealer_id);
		// Really, there is nothing to be display here.
		ReleaseLock();
	}

	@Override
	public void onNewDeal(int trump_number) {
		super.onNewDeal(trump_number);
		// Now is the time to initiate per round history stuff
		current_round_played_players_ = 0;
		current_round_num_cards_ = 0;
		per_round_played_cards_ = new ArrayList<Card[]>();
		for (int i = 0; i < state.numPlayers; ++i) {
			per_round_played_cards_.add(null);
		}
		current_deal_ended_ = false;
		local_cards_for_dealing_mode_only.clear();
		per_deal_defender_group_score_ = 0;
		suit_declarer_id = -1;
		num_declared_suit_cards = -1;
		declared_trump_suit = Card.SUIT_UNDEFINED;
	}

	private void ClearPerRoundHistory() {
		for (int i = 0; i < per_round_played_cards_.size(); ++i) {
			per_round_played_cards_.set(i, null);
		}
	}
	
	@Override
	public void onDeclareTrumpSuit(int player_id, int suit,
								 int num_declared_suit_cards) {
		AcquireLock();
		super.onDeclareTrumpSuit(player_id, suit, num_declared_suit_cards);
		this.suit_declarer_id = player_id;
		this.num_declared_suit_cards = num_declared_suit_cards;
		declared_trump_suit = suit;
		SetGameControllerMode(GameController.DEALING_MODE_DISPLAY_ONLY);
	}

	private void SetGameControllerMode(int mode) {
		game_controller_.SetGameControllerMode(mode, state.game_id);
	}

	@Override
	public void onYourTurnToFollow() {
		AcquireLock();
		super.onYourTurnToFollow();
		// This is exactly what the AIPlayer would do. 
		// TODO: select forced play card only.
		Card[]  play_cards = AISuggestCardsToFollow();
		Util.d("pre-select ", Arrays.toString(play_cards));
		this.pre_select_cards_ = play_cards;
		
		SetGameControllerMode(GameController.FOLLOWING_MODE);
	}

	@Override
	public void onYourTurnToLead() {
		AcquireLock();
		super.onYourTurnToLead();
		pre_select_cards_ = AISuggestLeadingCards();
 		SetGameControllerMode(GameController.LEADING_MODE);
	}
	
	@Override
	public void onLastHandResult() {
		AcquireLock();
		super.onLastHandResult();
		current_deal_ended_ = true;
		per_deal_defender_group_score_ = GetDefenderGroupScore();  // Maybe tractor card points are added.
		SetGameControllerMode(GameController.ENDING_DEAL_MODE);
	}
	
	private boolean displayErrorMessage() {
		if (error_message.length() == 0)
			return false;
		return true;
	}
	
	@Override
	public void onNotifyIllegalAction(TractorGameStateUpdate update) {
		super.onNotifyIllegalAction(update);
		error_message = TractorMessageCenter.generateMessage(update.errorMessage[0],
				update.errorMessage[1], state.trump_suit, state.playerId, update.cardProperty);
		if (!displayErrorMessage())  // Some error is not worth display, such as can no long declare a suit.
			return;
		AcquireLock();
		error_cards = null;
		if (update.errorMessage[0] == TractorMessageCenter.ILLEGAL_THROW)
			error_cards = update.cards;
		if (error_cards != null) {
			Arrays.sort(error_cards, new PerDealCardComparator(state.trump_suit, state.trump_number));
		}
		SetGameControllerMode(GameController.ERROR_MESSAGE_MODE);
	}
	
	@Override
	public void onAbandonGame() {
		// This is probably the one action where it doens't necessarily require us to grab the lock,
		// because we are about to exit the game.  But to be safe, maybe we should?  Also, it makes
		// display/message handling more coherent.
		AcquireLock();
		super.onAbandonGame();
		SetGameControllerMode(GameController.ABANDON_MODE);
	}
	
	@Override
	public void onSetTrumpSuitAndDealer() {
		AcquireLock();
		super.onSetTrumpSuitAndDealer();
		SetGameControllerMode(GameController.WAITING_FOR_PLAY_START_MODE);
	}
}
