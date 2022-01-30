package com.android.tractor;

import java.util.Arrays;

import com.maqicheng.games.GameAction;

public class TractorGameAction extends GameAction {

	private static final long serialVersionUID = 7036823856491566140L;
	
	public static final int ILLEGAL_ACTION = -1;
	public static final int DECLARE_TRUMP_SUIT_ACTION = 0;
	public static final int SWAP_TRACTOR_CARD_ACTION = 1;
	public static final int PLAY_CARD_ACTION = 2;
	public static final int NEW_DEAL_ACTION = 3;
	public static final int READY_FOR_NEXT_CARD_ACTION = 4;
	public static final int ABANDON_CURRENT_GAME = 5;

	private String getActionName() {
		switch(actionCode) {
		case ILLEGAL_ACTION:
			return "ILLEGAL_ACTION";
		case DECLARE_TRUMP_SUIT_ACTION:
			return "DECL_TRUMP";
		case SWAP_TRACTOR_CARD_ACTION:
			return "SWAP_TRTOR";
		case PLAY_CARD_ACTION:
			return "PLAY_CARD";
		case NEW_DEAL_ACTION:
			return "NEW_DEAL";
		case READY_FOR_NEXT_CARD_ACTION:
			return "READY";
		case ABANDON_CURRENT_GAME:
			return "ABANDON_GAME";
		default:
			return "UNKNWON_ACT";
		}
	}

	
	// These are the public data
	public int actionCode;
	public Card[] cards = null;
	public int suit = Card.SUIT_UNDEFINED;
	// MQC: do NOT use a generic Object here any more, be explicit about
	// what is stored here, useful for later custom serialization.

	
	public String toString() {
		String txt = "{ pid=" + playerId + " act=" + getActionName() + "\t cards=" +
		Arrays.toString(cards) + " suit=" + suit + " }";
		return txt;
	}
	
	public TractorGameAction(int playerId, int actionCode, Card[] cards, int suit) {
		super();
		this.playerId = playerId;
		this.actionCode = actionCode;
		this.cards = cards;
		this.suit = suit;
		
	}

}
