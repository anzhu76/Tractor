package com.android.tractor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

/**
 *  The following cards are considered a singleton:
 *  
 *  single card
 *  pair of cards
 *  3 of a kind
 *  4 of a kind etc.
 *  The above with consecutive appearances (not single card though)
 *  
 *  The rest are considered multiton, i.e., Shuai Pai.
 *  
 * @author anzhu
 */
public class SingletonCardProperty implements Serializable, Cloneable {
	private static final long serialVersionUID = -3052032611346491100L;
	
	// We need our own counting of number, which is slightly different from
	// the Card class.
	public static final int UNDEFINED = -1;
	public static final int TWO = 0;
	public static final int THREE = 1;
	public static final int FOUR = 2;
	public static final int FIVE = 3;
	public static final int SIX = 4;
	public static final int SEVEN = 5;
	public static final int EIGHT = 6;
	public static final int NINE = 7;
	public static final int TEN = 8;
	public static final int JACK = 9;
	public static final int QUEEN = 10;
	public static final int KING = 11;
	public static final int ACE = 12;
	// This is the part that starts to differ from the Card class.
	public static final int MINOR_TRUMP_NUMBER = 13;
	public static final int MAJOR_TRUMP_NUMBER = 14;
	public static final int SMALL_JOKER = 15;
	public static final int BIG_JOKER = 16;
	
	public int num_identical_cards;
	public int num_sequences;
	public int num_cards;  // num_cards = num_identical_cards * num_sequences;
	public boolean is_consecutive;
	// This is used for noting properties generated for AAKKK.  AA and KKK are going
	// to be two separate properties, but both AA and KKK will have the has_other_combo
	// set to true.
	public boolean has_other_combo;
	// If we have a trump_number, then suit will be trump_suit, but card_suit
	// will record the original card's suit, one card_suit per num_sequences.
	public int suit;
	public Vector<Integer> card_suit; 
	public int leading_number;
	
	// The following field is quite optional and is only useful under certain conditions.
	// Sometimes we converted a property type into another property type, and so we'll store
	// what was the original property type, note that this only remembers the last conversion.
	// 0 means that the property is original.
	public int last_num_sequences;
	public int last_num_identical_cards;
	
	private int trumpSuit;
	private int trumpNumber;
	
	public SingletonCardProperty(Card card, int trump_suit, int trump_number) {
		card_suit = new Vector<Integer>();
		trumpSuit = trump_suit;
		trumpNumber = trump_number;
		suit = card.CalculatePlaySuit(trump_suit, trump_number);
		card_suit.add(card.CalculateActualSuit(trump_suit));
		leading_number = ConvertToPropertyNumber(card, trump_suit, trump_number);
		num_identical_cards = 1;
		num_sequences = 1;
		num_cards = 1;
		last_num_identical_cards = 0;
		last_num_sequences = 0;
		is_consecutive = false;
		has_other_combo = false;
	}
	
	public SingletonCardProperty(int trump_suit, int trump_number) {
		trumpSuit = trump_suit;
		trumpNumber = trump_number;
	}
	
	public SingletonCardProperty(int[] array) {
		card_suit = new Vector<Integer>();
		int index = 0;
		trumpSuit = array[index++];
		trumpNumber = array[index++]; 
		suit = array[index++];
		leading_number = array[index++];
		num_identical_cards = array[index++];
		num_sequences = array[index++];
		num_cards = array[index++];
		is_consecutive = array[index++] == 1 ? true : false;
		has_other_combo = array[index++] == 1 ? true : false;
		for (int i = index; i < array.length; ++i) {
			card_suit.add(array[i]);
		}
		last_num_identical_cards = 0;
		last_num_sequences = 0;
	}
	
	public String toString() {
		return "{" + num_identical_cards + "x" + num_sequences + ": " +
		     Arrays.toString(this.ToCards()) + "}";
	}
	
	public int[] toIntArray() {
		int[] array = new int[9 + num_sequences];
		int index = 0;
		array[index++] = trumpSuit;
		array[index++] = trumpNumber;
		array[index++] = suit;
		array[index++] = leading_number;
		array[index++] = num_identical_cards;
		array[index++] = num_sequences;
		array[index++] = num_cards;
		array[index++] = is_consecutive ? 1 : 0;
		array[index++] = has_other_combo? 1 : 0;
		for (int suit : card_suit)
			array[index++] = suit;
		return array;
	}

	public SingletonCardProperty Copy(SingletonCardProperty property) {
		trumpSuit = property.trumpSuit;
		trumpNumber = property.trumpNumber;
		num_identical_cards = property.num_identical_cards;
		is_consecutive = property.is_consecutive;
		suit = property.suit;
		leading_number = property.leading_number;
		num_sequences = property.num_sequences;
		num_cards = property.num_cards;
		has_other_combo = property.has_other_combo;
		card_suit = (Vector<Integer>) property.card_suit.clone();
		last_num_identical_cards = property.last_num_identical_cards;
		last_num_sequences = property.last_num_sequences;
		return this;
	}
	
	public SingletonCardProperty clone() {
		return new SingletonCardProperty(trumpSuit, trumpNumber).Copy(this);
	}
	
	public void ConvertToType(int new_num_identical_cards, int new_num_sequences) {
		last_num_identical_cards = num_identical_cards;
		last_num_sequences = num_sequences;
		num_identical_cards = new_num_identical_cards;
		num_sequences = new_num_sequences;
		num_cards = num_identical_cards * num_sequences;
		is_consecutive = num_sequences > 1;
		card_suit.setSize(num_sequences);
	}
	
	public void ConvertToType(SingletonCardProperty property) {
		ConvertToType(property.num_identical_cards, property.num_sequences);
	}
	
	public void ConvertToType(int new_num_identical_cards, int new_num_sequences, boolean want_points) {
		Vector<Card> cards = ToCardVector();
		int initial_points = 0;
		for (int i = 0; i < new_num_sequences; ++i) {
			initial_points += cards.get(i * num_identical_cards).GetPoints();
		}
		int lead_card_index = 0;  // leading card index in the card_suit array.
		int lead_card_number = leading_number;  // new leading number.
		int best_points = initial_points;
		int total_cards_in_group = num_identical_cards * new_num_sequences;
		for (int i = 0; i < num_sequences - new_num_sequences; ++i) {
			int new_start_index = i * num_identical_cards;
			initial_points -= cards.get(new_start_index).GetPoints();
			initial_points += cards.get(total_cards_in_group + new_start_index).GetPoints();
			// The equal below ensures that in case of a tie, we'll favor lesser leading number.
			if ((want_points && initial_points >= best_points) ||
					(!want_points && initial_points <= best_points)) {
				best_points = initial_points;
				lead_card_index = i + 1;
				lead_card_number = 
					ConvertToPropertyNumber(cards.get(new_start_index + num_identical_cards), trumpSuit, trumpNumber);
			}
		}
		for (int i = 0; i < lead_card_index; ++i)
			card_suit.remove(0);
		leading_number = lead_card_number;
		ConvertToType(new_num_identical_cards, new_num_sequences);
	}
	
	public void ConvertToType(SingletonCardProperty property, boolean want_points) {
		ConvertToType(property.num_identical_cards, property.num_sequences, want_points);
	}
	
	public Vector<SingletonCardProperty> ConvertToTypeAllLeadingNumber(SingletonCardProperty property) {
		Vector<SingletonCardProperty> properties = new Vector<SingletonCardProperty>();
		for (int i = 0; i < num_sequences - property.num_sequences + 1; ++i) {
			SingletonCardProperty p = this.clone();
			p.leading_number -= i;
			if (p.leading_number == trumpNumber)
				p.leading_number--;
			for (int j = 0; j < i; ++j)
				p.card_suit.remove(0);
			p.ConvertToType(property);
			properties.add(p);
		}
		return properties;
	}
	
	public Card MaxPointCard() {
		int points = -1;
		Card card = null;
		for (Card c : ToCardVectorReverse()) {
			if (c.GetPoints() > points) {
				points = c.GetPoints();
				card = c;
			}
		}
		return card;
		
	}
	/**
	 * Check if current property can stop the target property from throwing.
	 * Must be of the same suit.  Note that the two properties don't need to 
	 * have the same number of cards.  For instance, AAKK can break 22.
	 * 
	 * @param target The target property to break
	 * @return true iff the current property has cards that can break
	 * the target property.
	 */
	public boolean IsBreakable(SingletonCardProperty target) {
		if (suit == target.suit &&
				leading_number > target.leading_number &&
				num_identical_cards >= target.num_identical_cards &&
				num_sequences >= target.num_sequences)
			return true;
		return false;
	}
	
	/**
	 * Check if the current property can trump (including over trumping) the
	 * target property.  Note that the two properties don't need to have the
	 * same number of cards.  For instance, AAKK can beat 22.
	 * 
	 * @param target The target property to trump
	 * @return true iff the current property has cards that can trump
	 * the target property.
	 */
	public boolean IsTrumpable(SingletonCardProperty target) {
		if (suit == trumpSuit &&
				num_identical_cards >= target.num_identical_cards &&
				num_sequences >= target.num_sequences &&
				(target.suit != trumpSuit || leading_number > target.leading_number))
			return true;
		return false;
	}
	
	public boolean equals(SingletonCardProperty target) {
		return (IsExactType(target) && leading_number == target.leading_number);
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof SingletonCardProperty 
		    && equals((SingletonCardProperty) other);
	}

	public boolean IsExactType(SingletonCardProperty target) {
		return (num_identical_cards == target.num_identical_cards &&
				num_sequences == target.num_sequences);
	}
	
	/**
	 * Check to see if target is a smaller type
	 * 
	 * @param target Property to compared to
	 * @return true iff target is a smaller type
	 */
	public boolean IsBiggerTypeThan(SingletonCardProperty target) {
		return (num_identical_cards >= target.num_identical_cards
				&& num_sequences >= target.num_sequences);
	}
	
	/**
	 * Check to see if target is a bigger type
	 * 
	 * @param target Property to compared to
	 * @return true iff target is a bigger type
	 */
	public boolean IsSmallerTypeThan(SingletonCardProperty target) {
		return (num_identical_cards <= target.num_identical_cards
				&& num_sequences <= target.num_sequences);
	}
	
	public Vector<Card> ToCardVectorReverse() {
		Vector<Card> cards = ToCardVector();
		Collections.reverse(cards);
		return cards;
	}

	public Vector<Card> ToCardVector() {
		Vector<Card> cards = new Vector<Card>();
		int property_number = leading_number;
		for (int i = 0; i < num_sequences; ++i) {
			for (int j = 0; j < num_identical_cards; ++j) {
				cards.add(new Card(ConvertToCardIndex(property_number, card_suit.get(i), trumpNumber)));
			}
			property_number--;
			if (property_number == trumpNumber)
				property_number--;
		}
		return cards;
	}
	
	// returns the card index in this singleton property.
	public Card[] ToCards() {
		return ToCardVector().toArray(new Card[0]);
	}
	
	public String toStringForType() {
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE) {
			String type = Integer.toString(num_identical_cards) + " of a kind";
			if (num_identical_cards == 2)
				type = "A pair";
			if (num_identical_cards == 3)
				type = "A triple";
			if (num_sequences > 1) {
				if (num_identical_cards == 2)
					type = "pairs";
				if (num_identical_cards == 3)
					type = "triples";
			
				type = "Consecutive " + Integer.toString(num_sequences) + " sequences of " + type;
			}
			return type;
		}
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE) {
			String type = Integer.toString(num_identical_cards) + "张相同的牌";
			if (num_identical_cards == 2)
				type = "一对";
			if (num_identical_cards == 3)
				type = "3张相同的牌";
			if (num_sequences > 1)
				type = "连续" + Integer.toString(num_sequences) + "个" + type;
			return type;
		}
		return "";
	}
	
	public int TotalPoints() {
		int points = 0;
		Card[] cards = ToCards();
		for (Card c : cards) {
			points += c.GetPoints();
		}
		return points;
	}
	
	public Vector<SingletonCardProperty> ParentPropertyTypes() {
		Vector<SingletonCardProperty> parents = new Vector<SingletonCardProperty>();
		SingletonCardProperty new_p = clone();
		if (new_p.num_identical_cards > 1) {
			new_p.num_sequences++;
			new_p.num_cards += new_p.num_identical_cards;
			parents.add(new_p);
		}
		SingletonCardProperty new_p_2 = clone();
		new_p_2.num_identical_cards++;
		new_p_2.num_cards += new_p_2.num_sequences;
		parents.add(new_p_2);
		return parents;
	}
	
	// Convert a Card's number to property number
	static public int ConvertToPropertyNumber(Card card, int trump_suit, int trump_number) {
		int number = card.GetNumber();
		if (number == trump_number) {
			if (card.GetSuit() == trump_suit)
				number = MAJOR_TRUMP_NUMBER;
			else
				number = MINOR_TRUMP_NUMBER;
		} else  if (number == Card.NUMBER_GUARANTEE) {
			number = SMALL_JOKER; 
		} else if (number == Card.NUMBER_NO_GUARANTEE) {
			number = BIG_JOKER;
		}
		return number;
	}
	
	/**
	 * Convert a SingletonCardProperty number to property card index
	 * suit is used when the number isn't JOKERs.
	 * 
	 * @param property_number
	 * @param suit doesn't matter when property_number is BIG_JOKER or SMALL_JOKER
	 * @param trump_number
	 * @return card index
	 */
	static public int ConvertToCardIndex(int property_number, int suit, int trump_number) {
		if (property_number == BIG_JOKER)
			return Card.ConvertToIndex(suit, Card.NUMBER_NO_GUARANTEE);
		if (property_number == SMALL_JOKER)
			return Card.ConvertToIndex(suit, Card.NUMBER_GUARANTEE);
		if (property_number == MINOR_TRUMP_NUMBER || property_number == MAJOR_TRUMP_NUMBER)
			return Card.ConvertToIndex(suit, trump_number);
		return Card.ConvertToIndex(suit, property_number);
	}

	/**
	 * Returns the probability of this property appearing, given the total_players and
	 * the number of cards for each card in p.num_sequences.
	 * 
	 * @param total_players total_players where the cards involved should be distributed to
	 * @param num_cards vector of integers denoting the number of cards per card in p.num_sequences 
	 * @param fixed_targeting_player if there is a specific play that this property has to happen upon.
	 * @return
	 */
	public double Probability(int total_players, Vector<Integer> num_cards, boolean fixed_targeting_player) {
	    //return ProbabilityExact(total_players, num_cards);
	    return SingletonCardPropertyProbability.ProbabilityApproximate(total_players, num_cards, fixed_targeting_player,
	    		num_identical_cards, num_sequences);
	}

	/**
	 * Retuns the probability of this property appearing, given the total_players and
	 * number of decks of cards.
	 * 
	 * @param total_players
	 * @param num_decks
	 * @return
	 */
	public double Probability(int total_players, int num_decks) {
		return SingletonCardPropertyProbability.Probability(total_players, num_decks, num_identical_cards, num_sequences);
	}
	
	/**
	 * Creates a dummy property of the particular type, the only interesting fields are:
	 * num_identical_cards, num_sequences, and num_cards.  The rest of the fields are filled
	 * in randomly.  Don't use them!
	 * 
	 * @param num_identical_cards
	 * @param num_sequences
	 * @return a dummy SingletonCardProperty.
	 */
	static public SingletonCardProperty CreatePropertyOfType(int num_identical_cards, int num_sequences) {	
		SingletonCardProperty p = new SingletonCardProperty(Card.SUIT_NO_TRUMP, Card.NUMBER_TWO);
		p.num_identical_cards = num_identical_cards;
		p.num_sequences = num_sequences;
		p.num_cards = p.num_identical_cards * p.num_sequences;
		// The rest of the field are of no interest to us actually.
		p.suit = Card.SUIT_SPADE;
		p.card_suit = new Vector<Integer>();
		for (int i = 0; i < num_sequences; ++i)
			p.card_suit.add(Card.SUIT_SPADE);
		p.has_other_combo = false;
		p.is_consecutive = p.num_sequences > 1 ? true : false;
		p.leading_number = Card.NUMBER_ACE;
		p.last_num_identical_cards = 0;
		p.last_num_sequences = 0;
		return p;
	}

 }