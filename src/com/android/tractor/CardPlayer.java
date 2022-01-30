/*
 * Copyright (C) 2008 An Zhu, Qicheng Ma.
 * 
 * Base class for game player.  Here we assume that we are only implementing
 * the push events, i.e., something is forced upon a player.  If a player needs
 * to take actions, such as declare a suit, swap tractor cards, or play a card.
 * Such actions are initiated from the players via Handler and Message to upper
 * class, such as CardGame (for all players) and GameController (for HumanPlayer,
 * the one that interacts with real user).
 */
package com.android.tractor;

import java.util.Arrays;
import java.util.Vector;

import com.maqicheng.games.GameAction;
import com.maqicheng.games.GameParam;
import com.maqicheng.games.GamePlayer;
import com.maqicheng.games.GameServerInterface;
import com.maqicheng.games.GameState;
import com.maqicheng.games.GameStateUpdate;

// TODO: MQC: this is to become TractorGamePlayer
public class CardPlayer extends GamePlayer implements GenericCardPlayer {

	private static final String debugTAG = "CardPlayer";
	private static final String kTractorGameType = TractorGame.class.getSimpleName();

	protected TractorGameState state;
	protected Vector<Integer> declarable_suits;

	// We need one up and one down channel so that
	// the downstream one can block waiting for updates, while the upstream
	// will still be immediately available (sometimes needed to incite updates)
	protected GameServerInterface gameServerUpstream;
	protected GameServerInterface gameServerDownstream;

	public static long kDelayBetweenUpdates = 100;
	public static long kGetUpdatesTimeOut = 30000;

	protected AIDealAnalyzer analyzer;

	public CardPlayer(GameParam param) {
		super(param);
		state = new TractorGameState(param);
		analyzer = new AIDealAnalyzer();
		// TODO: something is not right here.  If I don't do it here.  When loading the game, we'll
		// get a null pointer exception on line 68 below, which will indicate that onNewDeal is never
		// called, why?
		declarable_suits = new Vector<Integer>();
		declarable_suits.clear();
	}

	public CardPlayer(GameParam param,
			GameServerInterface downServer,
			GameServerInterface upServer) {
		this(param);
		gameServerUpstream = upServer;
		gameServerDownstream = downServer;
		analyzer = new AIDealAnalyzer();
	}

	/*
	 * Dealing a card.
	 */
	public void onDealACard(Card card) {
		// updating game state has already been done
		int suit = card.GetSuit();
		if (!declarable_suits.contains(suit) && state.IsDeclarableSuit(state.playerId, suit)) {
			declarable_suits.add(suit);
		}
	}

	public Card[] MyCards() {
		return state.playerCards[state.playerId];
	}

	/*
	 * based on the tractor cards being passed in, the player needs to select a
	 * set of cards to discard.
	 */
	public void onDealTractorCards(Card[] init_tractor_cards) {
		InitializeCardsForAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.android.tractor.GenericCardPlayer#YourTurnToLead()
	 */
	public void onYourTurnToLead() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.android.tractor.GenericCardPlayer#YourTurnToFollow()
	 */
	public void onYourTurnToFollow() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.android.tractor.GenericCardPlayer#DealerSwappedTractorCards(int)
	 */
	public void onDealerSwappedTractorCards(int dealer_id) {
		// We should have all cards in our hand now.  Do cleaning and sorting.
		// This function is called everytime after the tractor cards swapped and
		// all players receive such a call.
		// NOTE: MyCards() SHOULD REMAIN SORTED THROUGHOUT THE REST OF THE DEAL!
		ReplaceCurrentCards(Card.GetRidOfNullCards(MyCards()));
		Arrays.sort(MyCards(), new PerDealCardComparator(state.trump_suit, state.trump_number));
		if (dealer_id != state.playerId)  // Dealer's analyzer's parameter is set during onDealTractorCards.
			InitializeCardsForAll();
		analyzer.DeleteTractorCardsFromMyHand(state.tractor_cards);
	}

	public void onNewDeal(int trump_number) {
		/* moved to GaemState.NewDeal 
		state.playerNumCards[state.playerId] = 0;
		state.trump_number = trump_number;
		state.trump_suit = Card.SUIT_UNDEFINED;
		state.suit_declarer_id_ = -1;
		state.current_round_played_players = 0;
		state.current_round_num_cards = 0;
		ClearHand();
		*/
		declarable_suits = new Vector<Integer>();
		declarable_suits.clear();
	}

	public void NewGame(int cards_per_player, int num_tractor_cards) {
		// These should all been taken care of in the constructor of state.
		//state.playerNumCards[state.playerId] = 0;
		//state.num_tractor_cards = num_tractor_cards;
		//state.max_card_per_player = cards_per_player + num_tractor_cards;
		//state.current_round_played_players = 0;
		//state.current_round_num_cards = 0;
		//state.playerCards[state.playerId] = new Card[state.max_card_per_player];
		//ClearHand();
	}

	private void ReplaceCurrentCards(Card[] cards) {
		state.playerCards[state.playerId] = cards;
		state.playerNumCards[state.playerId] = MyCards().length;
	}

	public void AssignPlayerId(int id) {
		// TODO Auto-generated method stub
		playerId = id;
	}

	public int GetPlayerId() {
		// TODO Auto-generated method stub
		return playerId;
	}
	
	public void onDeclareTrumpSuit(int player_id, int suit, int num_declared_suit_cards) {
		/* update should be done in state already
		state.declared_trump_suit = suit;
		state.num_declared_suit_cards = num_declared_suit_cards;
		state.suit_declarer_id_ = player_id;
		*/
		// update declarable_suits
		Vector<Integer> new_declarable_suits = new Vector<Integer>();
		for (int i : declarable_suits) {
			if (state.IsDeclarableSuit(state.playerId, i))
				new_declarable_suits.add(i);
		}
		declarable_suits = new_declarable_suits;
	}

	public void SetTrumpNumber(int number) {
		state.trump_number = number;
	}

	public void SetTrumpSuit(int suit) {
		state.trump_suit = suit;
	}

	public void onOthersPlayedCards(TractorGameStateUpdate update) {
		analyzer.PlayerPlayedCards(update.cards, update.otherPlayerId, state.current_round_leading_play);
	}

	protected void PlayerAction(int action_code, Card[] cards) {
		TractorGameAction act = NewAction(action_code, cards);
		sendActionToServer(act);
	}
	
	protected void PlayerAction(int action_code, int suit) {
		TractorGameAction act = NewAction(action_code, suit);
		sendActionToServer(act);
	}

	
	protected GameStateUpdate[] sendActionToServer(GameAction act) {
		return gameServerUpstream.playerAction(
				kTractorGameType,
				state.game_id, act);
	}

	public void onLastHandResult() {
		analyzer.LastHandResult();
	}
	
	public void onAbandonGame() {
	}
	
	public void onSetTrumpSuitAndDealer() {
	}

	// MQC: ideally this is the central point for making action decisions
	public synchronized GameAction makeAction() {
		// TODO: now only says "ready". do something else afterward.
		/*
		if (state.playerNumCards[playerId] < state.cards_per_player) {
		    return NewAction(TractorGameAction.READY_FOR_NEXT_DEAL_ACTION, null);
		}
		*/
		return null;
	}

	// short-hand for making a GameAction object with my playerId
	protected TractorGameAction NewAction(int action_code, Card[] cards) {
		return new TractorGameAction(playerId, action_code, cards, Card.SUIT_UNDEFINED);
	}
	
	protected TractorGameAction NewAction(int action_code, int suit) {
		return new TractorGameAction(playerId, action_code, null, suit);
	}


	/*
	 * 
	 * The "main" method for a player, does the [action, update] loop
	 * 
	 */
	public void startPlayLoop(GameAction initialAction) {
		GameStateUpdate[] ups = new GameStateUpdate[0];
		if (initialAction != null) {
			ups = sendActionToServer(initialAction);
		}
		while (!state.hasEnded()) {
			while (ups == null || ups.length == 0) {
				/*
				try {
					Thread.sleep(kDelayBetweenUpdates);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				ups = gameServerDownstream.getGameStateUpdate(state.game_id, playerId, kGetUpdatesTimeOut);
			}
			if (ups.length > 0) {
				if (Util.logging_on) Util.v(debugTAG + " got ups", Arrays.toString(ups));
				updateGameState(ups);
				// GameAction act = makeAction();
				// if (act != null) ups = sendActionToServer(act);
				ups = null;
			}
		}
	}
	
	public void startPlayLoopInNewThread(final GameAction initialAction) {
		new Thread(
				new Runnable() {
					public void run() {
						startPlayLoop(initialAction);
					}
				}, 
		"playLoop_" + playerId).start();
	}
	
	public void startPlayLoopInNewThreadDefault() {
		startPlayLoopInNewThread(NewAction(TractorGameAction.NEW_DEAL_ACTION, null));
	}
	
	@Override
	public GameState getState() {
		// MQC: important! this is needed to return my state (which is a 
		// TractorGameState) instead of the base class's generic state.
		return state;
	}
	
	@Override
	public synchronized void updateGameState(GameStateUpdate update) {
		TractorGameStateUpdate up = (TractorGameStateUpdate) update;
		
		super.updateGameState(update);
		
		// In addition to changing the game state, what additional stuff
		// the human player need to do in response to the state update
		// e.g. drawing UI for the change
		switch (up.updateCode) {
		case TractorGameStateUpdate.NEW_DEAL:
			// nothing, update already done
			onNewDeal(up.number);
			break;
		case TractorGameStateUpdate.DEAL_A_CARD:
			onDealACard(up.cards[0]);
			break;
		case TractorGameStateUpdate.DECLARE_TRUMP_SUIT:
			onDeclareTrumpSuit(up.otherPlayerId, up.suit, up.number);
			break;
		case TractorGameStateUpdate.SET_TRUMP_SUIT_AND_DEALER:
			onSetTrumpSuitAndDealer();
			break;
		case TractorGameStateUpdate.DEAL_TRACTOR_CARDS:
			onDealTractorCards(up.cards);
			break;
		case TractorGameStateUpdate.SWAPPED_TRACTOR_CARDS:
			onDealerSwappedTractorCards(up.otherPlayerId);
			break;
		case TractorGameStateUpdate.TURN_TO_LEAD:
			onYourTurnToLead();
			break;
		case TractorGameStateUpdate.TURN_TO_FOLLOW:
			onYourTurnToFollow();
			break;
		case TractorGameStateUpdate.PLAYER_PLAYED_CARDS:
			onOthersPlayedCards(up);
			break;
		case TractorGameStateUpdate.LAST_HAND_RESULT:
			onLastHandResult();
			break;
		case TractorGameStateUpdate.ILLEGAL_ACTION:
			onNotifyIllegalAction(up);
			break;
		case TractorGameStateUpdate.ABANDON_GAME:
			onAbandonGame();
			break;
		default:
			Util.e("CardPlayer.update", "Unimplemented updateCode " + up.updateCode);
		}
		finishGameState(up);
	}

	
	public void onNotifyIllegalAction(TractorGameStateUpdate update) {
		// TODO Auto-generated method stub
		
	}
	
	protected void InitializeCardsForAll() {
		analyzer.SetParameters(state.trump_suit, state.trump_number, state.numPlayers, state.num_decks, state.playerId, state);
		analyzer.AddCardsToMyHand(MyCards());
	}

	/* Below are technically AIPlayer function, but HumanPlayer also shares this suggestion mechanism */
	
	protected Card[] AISuggestCardsToFollow() {
		return analyzer.SuggestCardsToFollow(state.playerId, state.per_round_played_cards.get(state.current_round_leader_id),
				state.playerCards[state.playerId],
				state.current_round_winning_property,
				state.isOnSameTeam(state.playerId, state.current_round_winning_id),
				state.numPlayers - state.current_round_played_players,
				state.dealerGroup);
	}

	protected Card[] AISuggestLeadingCards() {
		return analyzer.SugguestLeadingCards(state.playerId, MyCards(), state.dealerGroup);
	}

	protected Card[] AISuggestTractorCards(Card[] initial_tractor_cards) {
		return analyzer.SuggestTractorCards(state.playerId, initial_tractor_cards.length).toArray(new Card[0]);
	}

} 
