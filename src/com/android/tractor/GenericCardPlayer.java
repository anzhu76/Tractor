package com.android.tractor;

import android.os.Handler;
import android.os.Message;

@Deprecated
public interface GenericCardPlayer {

	/*
	 * Dealing a card.
	 */
	public abstract void onDealACard(Card card);

	/*
	 * based on played_cards being passed in, play a corresponding hand
	 */
	public abstract void onOthersPlayedCards(TractorGameStateUpdate update);

	/*
	 * based on the tractor cards being passed in, the player needs to
	 * select a set of cards to discard.
	 */
	public abstract void onDealTractorCards(Card[] initial_tractor_cards);

	/*
	 * We have a new deal, the game setting is unchanged compared to before.
	 */
	public abstract void onNewDeal(int trump_number);

	/*
	 * We are starting a new game with possibly new game settings.
	 */
	public abstract void NewGame(int cards_per_player, int num_tractor_cards);

	/*
	 * This is not finalized, but I figure that we probably needs to
	 * know the hands.
	 */
	public abstract Card[] MyCards();

	public abstract void SetTrumpNumber(int number);

	public abstract void SetTrumpSuit(int suit);

	public abstract void onDeclareTrumpSuit(int player_id, int suit, int num_declared_suit_cards);

	public abstract void AssignPlayerId(int id);

	public abstract int GetPlayerId();

	public abstract void onYourTurnToLead();

	public abstract void onYourTurnToFollow();

	public abstract void onDealerSwappedTractorCards(int dealer_id);
	
	public abstract void onNotifyIllegalAction(TractorGameStateUpdate update);
	
	// TODO: this API should be changed for friends version, later...
	public abstract void onLastHandResult();

}