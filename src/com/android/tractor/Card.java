package com.android.tractor;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;

public class Card implements Externalizable, AbstractCard, Comparable<Card> {

	public static final int CARDS_PER_DECK = 54;
	public static final int BLANK_CARD = -1;
	public static final int UNKNOWN_CARD = -2;
	public static final int CARDS_PER_SUIT = 13;
	/*
	 *  Java doesn't allow casting between enum and int.  Well, at least
	 *  I don't know why to go from enum to int.  From int to enum is
	 *  straight forward to some extend:
	 *  CardSuit suit = CardSuit.values()[1];
	 *  But from enum to int?  Hopeless at the moment.
	 *  So I'm just manually define the enums as constant below, works just
	 *  as well.
	 */
	public static final int SUIT_UNDEFINED = -1;
	public static final int SUIT_DIAMOND = 0;  // Real suit should start with 0.
	public static final int SUIT_CLUB = 1;
	public static final int SUIT_HEART = 2;
	public static final int SUIT_SPADE = 3;
	public static final int SUIT_NO_TRUMP = 4;
	public static final int SUIT_NUM_SUITS = 5;
	
	public static final int NUMBER_TWO = 0;
	public static final int NUMBER_THREE = 1;
	public static final int NUMBER_FOUR = 2;
	public static final int NUMBER_FIVE = 3;
	public static final int NUMBER_SIX = 4;
	public static final int NUMBER_SEVEN = 5;
	public static final int NUMBER_EIGHT = 6;
	public static final int NUMBER_NINE = 7;
	public static final int NUMBER_TEN = 8;
	public static final int NUMBER_JACK = 9;
	public static final int NUMBER_QUEEN = 10;
	public static final int NUMBER_KING = 11;
	public static final int NUMBER_ACE = 12;
	/*
	 * This is following the standard "Bicycle" brand cards.  Where
	 * the "big" joker has just the bicycle on the card, while the "small"
	 * joker has a smaller bicycle plus some "guarantee" wording on the
	 * card.
	 */
	public static final int NUMBER_GUARANTEE = 13;  // i.e., small joker
	public static final int NUMBER_NO_GUARANTEE = 14;  // i.e., big joker

	/*
	 * We follow the conversion here that each deck of cards is number 0 through 53.
	 * And the numbering is:
	 * club: ace through 2
	 * diamond: ace through 2
	 * heart: ace through 2
	 * Space: ace through 2
	 * small joker (guarantee)
	 * big joker (no_guarantee)
	 */
	private int index_;
	private int suit_;
	private int number_;
	
	
	/**
	 * Custom Serialization, basically just an int. 
	 *
	 * MQC:
	 * Doesn't work, the class ImageView doesn't allow its serialization to be accessed
	 * 
	private static final long serialVersionUID = 0x8899aabb;
	
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		//out.defaultWriteObject();
		out.writeInt(index_);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		//in.defaultReadObject();
		index_ = in.readInt();
		//CalculateSuitAndNumber();
	}
	*/
	
	// MQC: instead use Externalizable, seems to work.
	// TODO: consider separating Card and ImageView (maybe a CardView class?)
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(index_);
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		index_ = in.readInt();
		CalculateSuitAndNumber();
	}
	
	// Note that we don't have method to set index, or number, or suit.  So that
	// means that each card create is final, i.e., it's not going to change.  This is
	// intended.
	
	// no-arg constructor is required for serialization
	public Card() {
		this(Card.BLANK_CARD);
	}
	
	public Card(int index) {
		index_ = index;
		CalculateSuitAndNumber();
	}
	
	public Card(int suit, int number) {
		this(ConvertToIndex(suit, number));
	}

	public int GetSuit() {
    	return suit_;
    }
    
    public int GetNumber() {
    	return number_;
    }
    
    public int GetIndex() {
    	return index_;
    }
    
    public int GetPoints() {
    	if (number_ == Card.NUMBER_FIVE)
    		return 5;
    	if (number_ == Card.NUMBER_TEN)
    		return 10;
    	if (number_ == Card.NUMBER_KING)
    		return 10;
    	return 0;
    }
    
    static int PointsBasedOnIndex(int index) {
    	Card c = new Card(index);
    	return c.GetPoints();
    }

    public static String SuitToString(int suit) {
    	if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE) {
    		switch (suit) {
    		case SUIT_NO_TRUMP:
    			return "No Trump";
    		case SUIT_SPADE:
    			return "Spade";
    		case SUIT_HEART:
    			return "Heart";
    		case SUIT_CLUB:
    			return "Club";	
    		case SUIT_DIAMOND:
    			return "Diamond";
    		}
    	}
    	if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE) {
    		switch (suit) {
    		case SUIT_NO_TRUMP:
    			return "无将";
    		case SUIT_SPADE:
    			return "黑桃";
    		case SUIT_HEART:
    			return "红桃";
    		case SUIT_CLUB:
    			return "梅花";	
    		case SUIT_DIAMOND:
    			return "方块";
    		}
    	}
    	return null;
    }
   
    public static String NumberToString(int number) {
    	// I'm being lazy here, instead of switch for every single number,
    	// just hard coding numbers less than Jack.
    	switch (number) {
    	case NUMBER_GUARANTEE:
    		return "No Trump";
    	case NUMBER_NO_GUARANTEE:
    		return "No Trump";
    	case NUMBER_ACE:
    		return "A";
    	case NUMBER_KING:
    		return "K";
    	case NUMBER_QUEEN:
    		return "Q";
    	case NUMBER_JACK:
    		return "J";
    	}
    	return Integer.toString(number + 2);
    }
    
    private void CalculateSuitAndNumber() {
		suit_ = index_ / CARDS_PER_SUIT;
		number_ = index_ % CARDS_PER_SUIT;
		if (suit_ == SUIT_NO_TRUMP)
			number_ += CARDS_PER_SUIT;
    }

	static final String SUIT_NAME[] = {"d", "c", "h", "s", "j"};
	static final String CARD_NAME[] = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "@", "O"};

	public String toString() {
	    return CARD_NAME[number_] + SUIT_NAME[suit_];	
	}
	
	/**
	 * Calculate the play suit according to trump_suit, trump_number.
	 * In particular, GUARANTEE and NO_GUARANTEE, as well as
	 * trump_number will count as trump_suit
	 * 
	 * @param trump_suit
	 * @param trump_number
	 * @return
	 */
	public int CalculatePlaySuit(int trump_suit, int trump_number) {
		if (suit_ == Card.SUIT_NO_TRUMP)
			return trump_suit;
		if (number_ == trump_number)
			return trump_suit;
		return suit_;
	}
	
	/**
	 * Return the actual suit of the card.  GUARANTEE and NO_GUARANTEE
	 * will become trump_suit, and other cards, includes trump_number
	 * maintains their original suit.
	 * 
	 * @param trump_suit
	 * @return
	 */
	public int CalculateActualSuit(int trump_suit) {
		if (suit_ == Card.SUIT_NO_TRUMP)
			return trump_suit;
		return suit_;
	}
	
	static public int ConvertToIndex(int suit, int number) {
		if (number == NUMBER_NO_GUARANTEE)
			return 53;
		if (number == NUMBER_GUARANTEE)
			return 52;
		return number + suit * CARDS_PER_SUIT;
	}

	/**
	 * Assumption: both cards and cards_to_be_deleted are sorted in the same order.
	 * 
	 * @param cards
	 * @param cards_to_be_deleted
	 * @return  a new set of Cards.
	 */
	public static Card[] DeleteCards(Card[] cards, Card[] cards_to_be_deleted) {
		if (cards_to_be_deleted == null)
			return cards;
		int j = 0;
		Vector<Card> new_cards = new Vector<Card>();
		new_cards.removeAllElements();
		for (int i = 0; i < cards.length; ++i) {
			if (j < cards_to_be_deleted.length && cards[i].GetIndex() == cards_to_be_deleted[j].GetIndex()) {
				j++;
			} else {
				new_cards.add(cards[i]);
			}
		}
		return (Card[]) new_cards.toArray(new Card[0]);
	}
	
	/**
	 * Delete to_be_deleted cards from initial_cards.  Alters initial_cards.  Sorts both initial_cards
	 * and to_be_deleted.
	 * 
	 * @param initial_cards
	 * @param to_be_deleted
	 */
	public static void DeleteCards(Vector<Card> initial_cards, Vector<Card> to_be_deleted) {
		Collections.sort(initial_cards);
		Collections.sort(to_be_deleted);
		int index_a = 0;  // for initial_cards
		for (Card c: to_be_deleted) {
			while (initial_cards.get(index_a).GetIndex() != c.GetIndex()) {
				++index_a;
			}
			initial_cards.remove(index_a);
		}
	}
	
	// Sigh, we should have used Vector instead of [] to for cards representation.
	// There is also the sorting function for Vector: Collections.sort(...)
	// Oh well.
	public static Card[] GetRidOfNullCards(Card[] cards) {
		Vector<Card> new_cards = new Vector<Card>();
		new_cards.removeAllElements();
		for (int i = 0; i < cards.length; ++i) {
			if (cards[i] != null) {
				new_cards.add(cards[i]);
			}
		}
		return (Card[]) new_cards.toArray(new Card[0]);
	}
	
	public static boolean Contains(Card[] container, Card[] cards) {
		if (cards.length == 0)
			return true;
		if (container.length == 0)
			return false;
		int j = 0;  // index for cards
		for (int i = 0; i < container.length && j < cards.length; ++i) {
			if (container[i].GetIndex() == cards[j].GetIndex()) {
				j++;
			}
		}
		if (j != cards.length)
			return false;
		return true;
	}
	
	public static void Shuffle(Card[] cards) {
		long seed = new Random().nextLong();
		Util.i("ShuffleCard", "seed=" + seed);
	    Random rng = new Random(seed);
        int n = cards.length;
        while (n > 1) 
        {
            int k = rng.nextInt(n);
            n--;
            Card temp = cards[n];
            cards[n] = cards[k];
            cards[k] = temp;
        }
	}

	public int compareTo(Card another) {
		return GetIndex() - another.GetIndex();
	}
	
	@Override
	public boolean equals(Object another) {
		return another!=null && 
		(another instanceof Card) && 
		compareTo((Card) another) == 0;
	}

	public static Card[] VectorToArray(Vector<Card> play_cards) {
		return play_cards.toArray(new Card[play_cards.size()]);
	}
}