package com.android.tractor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * This is intended by AIPlayer to either keep track of their own cards, or opponents played cards.
 * So one CardOrganizer per player.  We can probably add a lot of functionality here to keep track of 
 * various properties for each player.  Functions like: HasSuit(), or HasPropertyInSuit(SingletonCardProperty)
 * etc.  For now, I just implement the simple thing of organize cards into suits and maintain them as we play along.
 * important thing: keep things sorted in the PerDealCardComparator order.  Is not, please start to make local
 * copies.  Some cares are from TractorGameState, and they need to be sorted.
 * 
 * @author anzhu
 *
 */
public class CardOrganizer {
	// suit situation.  default is UNCERTAIN.  VOID is definitely. EXIST means that we see the card played.
	// It's probably a bit surer than UNCERTAIN.
	// MQC: put these in rough order of sureness of void.
	public static int UNCERTAIN = 0;
	public static int VOID = -1;
	public static int EXIST = 1;
	
	public PerDealCardComparator comparator;
	Vector<Vector<Card> > suited_cards;
	int[] suit_info;
	int trumpSuit;
	int trumpNumber;
	CardAnalyzer analyzer;
	
	public class PropertyTypeInfo {
		// The lead property that initiated this.  Used to identify and group together
		// PropertyTypeInfo.  We should have only one such object per lead_property type per suit.
		public SingletonCardProperty lead_property;
		// The properties that are missing when matching the lead_property.  Note that
		// we store the lowest possible property that is missing.  For instance, AAAKKK,
		// we played 777456, then a pair will be stored here.
		public Vector<SingletonCardProperty> void_properties;
		// The properties that could exist and not played due to the fact that it has higher type.
		// Note that it depends on void_properties as well.  For instance AAAKKK, if we played 777222,
		// then the maybe_properties will contain the types: 4444 (4 of a kind), and AAAKKKQQQ (3-3).
		// However, if we played 777228, then AAAKKKQQQ(3-3) can't be in the maybe_properties.
		// Think of maybe_properties being empty as equivalent as holding a non-existing property.
		public Vector<SingletonCardProperty> maybe_properties;
		
		public PropertyTypeInfo() {
			lead_property = null;
			void_properties = new Vector<SingletonCardProperty>();
			maybe_properties = new Vector<SingletonCardProperty>();
		}
	};
	
	Vector<Vector<PropertyTypeInfo>> property_type_info;
	/**
	 * Get cards of the same specified by "suit".  Assume that cards have been
	 * sorted the property PerDealCardComparator. Preserves ordering in cards
	 * passed in.
	 * 
	 * @param suit Should be calculated through CalculatePlaySuit first
	 * @param sorted_cards Sorted cards
	 * @param trump suit
	 * @param trump number
	 * @return Card array that contains the suit.
	 */
	static public  Card[] GetSameSuitCards(int suit, Card[] sorted_cards, int trump_suit, int trump_number) {
		Vector<Card> same_suit_cards = new Vector<Card>();
		for (int i = 0; i < sorted_cards.length; ++i) {
			if (sorted_cards[i].CalculatePlaySuit(trump_suit, trump_number) == suit) {
				same_suit_cards.add(sorted_cards[i]);
			} else if (same_suit_cards.size() != 0) {
				break;  // cards is sorted.
			}
		}
		return same_suit_cards.toArray(new Card[0]);
	}
	
	/**
	 * Separate cards passed in into same suit card vector.  cards will
	 * become sorted afterwards.
	 * 
	 * @param cards cards to be separated, don't have to be sorted
	 * @param suit trump suit
	 * @param number trump number
	 * @return
	 */
	static public Vector<Card[]> SeparateIntoSameSuit(Card[] cards, int suit, int number) {
		Arrays.sort(cards, new PerDealCardComparator(suit, number));
		if (cards == null || cards.length == 0)
			return null;
		Vector<Card[]> cards_array  = new Vector<Card[]>();
		Vector<Card> same_suit_cards = null;
		int initial_suit = Card.SUIT_UNDEFINED;
		Arrays.sort(cards, new PerDealCardComparator(suit, number));
		for (Card c: cards) {
			if (c.CalculatePlaySuit(suit, number) != initial_suit) {
				if (same_suit_cards != null) {
					cards_array.add(same_suit_cards.toArray(new Card[0]));
				}
				initial_suit = c.CalculatePlaySuit(suit, number);
				same_suit_cards = new Vector<Card>();
			}
			same_suit_cards.add(c);
			
		}
		// same_suit_cards guaranteed to be not null.
		cards_array.add(same_suit_cards.toArray(new Card[0]));
		return cards_array;
	}

	/**
	 * @param cards
	 * @param trump_suit
	 * @param trump_number
	 */
	public CardOrganizer(int trump_suit, int trump_number, CardAnalyzer a) {
		suited_cards = new Vector<Vector<Card> >();
		property_type_info = new Vector<Vector<PropertyTypeInfo>>();
		for (int i = 0; i < 5; i++) {
			suited_cards.add(new Vector<Card>());
			property_type_info.add(new Vector<PropertyTypeInfo>());
		}
		trumpSuit = trump_suit;
		trumpNumber = trump_number;
		comparator = new PerDealCardComparator(trump_suit, trump_number);
		suit_info = new int[Card.SUIT_NUM_SUITS];
		for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
			suit_info[i] = UNCERTAIN;
		}
		analyzer = a;
		
	}
	

	/**
	 * Get rid of the expired cards in our cards.  The expired cards
	 * need to be in our hand.
	 *  
	 * @param expired_cards
	 */
	public void DeleteCards(Card[] expired_cards) {
		// As crazy as this may sound, I think the bug here is that expired_cards is shared among threads.
		// Create local copy ba.
		if (expired_cards == null || expired_cards.length == 0)
			return;
		Card[] expired_cards_2 = expired_cards.clone();
		// Util.g("Cards To Be Deleted: ", Arrays.toString(expired_cards));
		// Util.g("Current cards: ", toString());
		Vector<Card[]> separated_expired_cards = SeparateIntoSameSuit(expired_cards_2, trumpSuit, trumpNumber);
		for (Card[] ex_cards : separated_expired_cards) {
			int suit = ex_cards[0].CalculatePlaySuit(trumpSuit, trumpNumber);
			Vector<Card> same_suit_cards = suited_cards.get(suit);
			Collections.sort(same_suit_cards, comparator);
			int j = 0;  // index for same_suit_cards
			for (int i = 0; i < ex_cards.length;) {
				if (same_suit_cards.get(j).GetIndex() == ex_cards[i].GetIndex()) {
					i++;
					same_suit_cards.remove(j);
				} else {
					j++;
				}
			}
		}
		// Util.g("Current cards after deletion: ", toString());
	}
	
	public Vector<Card> GetSuit(int suit) {
		return suited_cards.get(suit);
	}
	
	public CardProperty GetSuitProperty(int suit) {
		return new CardProperty(suited_cards.get(suit).toArray(new Card[0]), trumpSuit, trumpNumber);
	}
	
	public String toString() {
		String cards_string = "";
		for (Vector<Card> cards : suited_cards) {
			cards_string += Arrays.toString(cards.toArray(new Card[0]));
		}
		return cards_string;
	}
	
	/**
	 * Used to get the suit info for a player for which we don't know their initial hand.
	 * 
	 * @param suit
	 * @return
	 */
	public int SuitInfo(int suit) {
		return suit_info[suit];
	}
	
	/**
	 * Used to get the suit for myself, which is accurate information on exactly the number
	 * of cards remaining.
	 * 
	 * @param suit
	 * @return
	 */
	public int CardsInSuit(int suit) {
		return suited_cards.get(suit).size();
	}
	
	/**
	 * Given a bunch of properties, decide that if p should be added to properties.
	 * p will either replace a smaller property, or consumed by a bigger property
	 * already in the collection.
	 * 
	 * @param properties A bunch of properties that are not compatible with each other. not in any sorted order.
	 * @param p
	 * @param want_points whether we favor exact type properties with more points, or less points.
	 */
	public void ConsumePropertyType(Vector<SingletonCardProperty> properties, SingletonCardProperty p) {
		// We won't consume singleton cards, just not interesting.
		if (p.num_cards == 1)
			return;
		boolean add_p = true;
		for (SingletonCardProperty pp : properties) {
			if (p.IsExactType(pp)) {
				add_p = false;
				break;
			} else if (p.IsBiggerTypeThan(pp)) {
				pp.Copy(p);
				add_p = false;
				break;
			} else if (pp.IsBiggerTypeThan(p)) {
				add_p = false;
				break;
			}
		}
		if (add_p)
			properties.add(p);
	}
	
	/**
	 * Judge if property p could possibly exist in suit.  Note that p could exist in a much higher form.
	 * For instance, you ask me if a pair exist in suit heart.  I might say that if a pair were to exist, it
	 * has to be in the form of 4 of a kind.
	 * 
	 * @param p a SingletonCardProperty
	 * @param suit 
	 * @return a vector of possible property types that p could take in.  Empty if p doesn't exist in any form.
	 */
	public Vector<SingletonCardProperty> IsPropertyAvailableInSuit(SingletonCardProperty p, int suit) {
		Vector<SingletonCardProperty> possible_types = new Vector<SingletonCardProperty>();
		if (SuitInfo(suit) == VOID)
			return possible_types;
		possible_types.add(p.clone());
		for (PropertyTypeInfo info : property_type_info.get(suit)) {
			boolean match = false;
			for (SingletonCardProperty pp : info.void_properties) {
				if (p.IsBiggerTypeThan(pp)) {
					match = true;
					break;
				}
			}
			if (match) {
				if (info.maybe_properties.size() == 0) {
					possible_types.clear();
					return possible_types;
				}
				for (SingletonCardProperty pp : info.maybe_properties) {
					if (pp.num_identical_cards >= p.num_identical_cards)
						ConsumePropertyType(possible_types,
								SingletonCardProperty.CreatePropertyOfType(pp.num_identical_cards,
										Math.max(pp.num_sequences, p.num_sequences)));
				}
			}
		}
		return possible_types;
	}
	
	private void UpdatePropertyTypeInfo(Vector<SingletonCardProperty> missing_properties,
			SingletonCardProperty lead_property, int suit) {
		PropertyTypeInfo new_info = new PropertyTypeInfo();
		new_info.lead_property = lead_property.clone();
		new_info.void_properties = (Vector<SingletonCardProperty>) missing_properties.clone();
		// There are two new maybe_property candidates.
		// Note the ordered-ness of the maybe_properties.
		if (lead_property.num_identical_cards < analyzer.numDecks) {
			SingletonCardProperty p = SingletonCardProperty.CreatePropertyOfType(lead_property.num_identical_cards + 1, 1);
			new_info.maybe_properties.add(p);
		}
		if (missing_properties.size() == 1 && lead_property.IsExactType(missing_properties.get(0))
				&& lead_property.num_sequences > 1) {
			SingletonCardProperty p = SingletonCardProperty.CreatePropertyOfType(lead_property.num_identical_cards,
					lead_property.num_sequences + 1);
			new_info.maybe_properties.add(p);
		}
		for (PropertyTypeInfo info : property_type_info.get(suit)) {
			if (new_info.lead_property.IsExactType(info.lead_property)) {
				// Merge the two types.  Note that new_info usually improves upon info.
				// Because of the ordering, but not always.  For instance, player could
				// be hiding a triple and play against a pair at the right moment.
				if (new_info.maybe_properties.size() < info.maybe_properties.size())
					info.maybe_properties = new_info.maybe_properties;
				for (SingletonCardProperty p : missing_properties) {
					analyzer.EliminateProperty(info.void_properties, p);
				}
				return;
			}
		}
		property_type_info.get(suit).add(new_info);
	}
	
	/**
	 * Add cards to our collection.  Maintain sortedness in each suit.
	 * 
	 * @param new_cards
	 */
	public void AddCards(Card[] new_cards_2, Card[] lead_play_2) {
		if (new_cards_2 == null | new_cards_2.length == 0)
			return ;
		Card[] new_cards = new_cards_2.clone();
		Card[] lead_play = null;
		if (lead_play_2 != null)
			lead_play = lead_play_2.clone();
		// Util.g("Cards To Be added: ", Arrays.toString(new_cards));
		// Util.g("Current cards: ", toString());
		int lead_suit = Card.SUIT_UNDEFINED;
		if (lead_play != null)
			lead_suit = lead_play[0].CalculatePlaySuit(trumpSuit, trumpNumber);
		boolean followed_suit = true;
		Vector<Card[]> separated_cards = SeparateIntoSameSuit(new_cards, trumpSuit, trumpNumber);
		for (Card[] same_suit_cards : separated_cards) {
			// separated_cards are already sorted via SeparateIntoSameSuit.
			int suit = same_suit_cards[0].CalculatePlaySuit(trumpSuit, trumpNumber);
			if (lead_play != null && suit != lead_suit) {
				suit_info[lead_suit] = VOID;
				suit_info[suit] = EXIST;
				followed_suit = false;
			}
			Vector<Card> current_cards = suited_cards.get(suit);
			Collections.sort(current_cards, comparator);
			int i = 0;  // index for same_suit_cards
			int j = 0;  // index for current_cards
			while (j < current_cards.size() & i < same_suit_cards.length) {
				Card c = current_cards.get(j);
				while (comparator.compare(same_suit_cards[i], c) <= 0) {
					current_cards.add(j, same_suit_cards[i]);
					i++;
					j++;
					if (i == same_suit_cards.length)
						break;
				}
				j++;
			}
			for (int k = i; k < same_suit_cards.length; ++k) {
				current_cards.add(current_cards.size(), same_suit_cards[k]);
			}
		}
		// Util.g("Current cards after addition: ", toString());
		if (lead_play != null && followed_suit) {
			suit_info[lead_suit] = EXIST;
			// Now do an analysis on the properties of the new_cards.	
			CardProperty lead_property = new CardProperty(lead_play, trumpSuit, trumpNumber);
			Vector<SingletonCardProperty> missing_properties = new Vector<SingletonCardProperty>();
			for (SingletonCardProperty p : lead_property.properties) {
				if (p.num_cards == 1)
					continue;  // not an interesting property to us.
				missing_properties.clear();
				Vector<SingletonCardProperty> tmp_property_vector = new Vector<SingletonCardProperty>();
				tmp_property_vector.add(p);
				analyzer.FindAllForcedProperties(tmp_property_vector, new_cards, missing_properties);
				if (missing_properties.size() > 0)
					UpdatePropertyTypeInfo(missing_properties, p, lead_suit);
			}
		}
	}

	public Card[] getAllCards() {
		Vector<Card> current_hand = new Vector<Card>();
		for (Vector<Card> cards : suited_cards) {
			current_hand.addAll(cards);
		}
		return current_hand.toArray(new Card[current_hand.size()]);
	}
	
}
