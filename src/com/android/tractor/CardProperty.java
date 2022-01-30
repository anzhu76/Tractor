/*
 * This class defines interesting card properties associated with tractor cards, e.g.,
 * pairs, triples, consecutive pairs etc.  This class provides functions that allow
 * the game monitor to check for validity of each hand played, and it also provides
 * functions that help the AIPlay select valid cards to play.
 */

package com.android.tractor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Vector;

public class CardProperty implements Serializable {
	private static final long serialVersionUID = -6424221693448922985L;


	/*
	 * Note: some properties will overlap, for instance:
	 * AAKKKQQQQ
	 * in the properties, we'll record the following properties:
	 * AA
	 * KKK
	 * QQQQ
	 * This particular calculation should be useful for break down the properties
	 * for the Shuai Pai action, or is certain cards are forced to be played.
	 * But there is actually the other property:
	 * AAKKQQ
	 * KKKQQQ
	 * This we'll record in secondary_properties.  secondary_properties might
	 * overlap with each other.  The secondary properties will be useful to check
	 * to see if the Shuai Pai actions are allowed.  For instance, if a user tries to
	 * play:
	 * AQQJJ
	 * and if we have AAAKK, we want to be able to say that AQQJJ is not playable, because
	 * we have the AAKK property.
	 */
	public Vector<SingletonCardProperty> properties;
	public Vector<SingletonCardProperty> secondary_properties;
	
	public boolean is_mixed_suit;  // We don't allow players to play mixed suit card.
	public boolean is_trump_suit;
	public int total_num_cards;
	public int suit;  // only meaningful if is_mixed_suit is false.
	
	private int trump_suit;
	private int trump_number;
	

	/**
	 * Creates CardProperty that represents the cards passed in
	 * 
	 * @param original_cards cards used to build the properties
	 * @param suit trump suit
	 * @param number trump number
	 * @param decks number of decks
	 */
	public CardProperty(Card[] original_cards, int t_suit, int number) {
		trump_suit = t_suit;
		trump_number = number;
		properties = new Vector<SingletonCardProperty>();
		secondary_properties = new Vector<SingletonCardProperty>();
		if (original_cards == null || original_cards.length == 0) {
			is_mixed_suit = true;
			is_trump_suit = false;
			total_num_cards = 0;
			suit = Card.SUIT_UNDEFINED;
			return;
		}
		total_num_cards = original_cards.length;
		is_mixed_suit = false;
		is_trump_suit = false;
		Vector<Card[]> separated_cards = CardOrganizer.SeparateIntoSameSuit(original_cards, trump_suit, trump_number);
		if (separated_cards.size() > 1) {
			is_mixed_suit = true;
			suit = Card.SUIT_UNDEFINED;
		} else {
			suit = separated_cards.get(0)[0].CalculatePlaySuit(trump_suit, trump_number);
			is_trump_suit = (suit == trump_suit);
		}
		Vector<SingletonCardProperty> per_suit_properties = new Vector<SingletonCardProperty>();
		Vector<SingletonCardProperty> per_suit_secondary_properties = new Vector<SingletonCardProperty>();
		for (Card[] cards: separated_cards) {
			per_suit_properties.clear();
			per_suit_secondary_properties.clear();
			CalculateAllProperties(cards, per_suit_properties, per_suit_secondary_properties);
			properties.addAll(per_suit_properties);
			secondary_properties.addAll(per_suit_secondary_properties);
		}
	}
	
	/**
	 * Create a CardProperty that just contains the SingletonCardProperty p
	 * 
	 * @param p
	 */
	public CardProperty(int t_suit, int number, SingletonCardProperty p) {
		trump_suit = t_suit;
		trump_number = number;
		properties = new Vector<SingletonCardProperty>();
		properties.add(p.clone());
		secondary_properties = new Vector<SingletonCardProperty>();
		is_mixed_suit = false;
		total_num_cards = p.num_cards;
		suit = p.suit;
		is_trump_suit = suit == trump_suit;
	}
	
	/**
	 * We have an original property p, now there are some number of cards that follow
	 * p, increase the num_sequence of p by 1 to include these cards.  We might need
	 * to adjust (decrease) the num_identical_cards.
	 * 
	 * @param p Current property
	 * @param another_num_identical_cards Number of identical cards that follows
	 * @param new_actual_suit The actual suit of the cards that follows (for the minor trump numbers)
	 * @return the new property, the original property p is not changed.
	 */
	private SingletonCardProperty GrowToNewProperty(SingletonCardProperty p, int another_num_identical_cards,
			int new_actual_suit) {
		SingletonCardProperty new_property = new SingletonCardProperty(trump_suit, trump_number);
		new_property.Copy(p);
		new_property.is_consecutive = true;
		new_property.num_identical_cards = Math.min(p.num_identical_cards,
				another_num_identical_cards);
		if (p.num_identical_cards != another_num_identical_cards)
			new_property.has_other_combo = true;
		new_property.num_sequences++;
		new_property.num_cards = new_property.num_identical_cards * new_property.num_sequences;
		new_property.card_suit.add(new_actual_suit);
		return new_property;	
	}
	
	// Calculates the properties of the same suit cards, and return them into
	// properties and secondary_properties.  Note: properties, and secondary_properties
	// will be cleared at the beginning, so make sure to save whatever you had earlier!
	private void CalculateAllProperties(Card[] same_suit_cards,
			Vector<SingletonCardProperty> ps,
			Vector<SingletonCardProperty> sps) {
		// Assumes same_suit_cards sorted from high to low
		ps.clear();
		sps.clear();
		// "current" means properties that could still grow using the current card
		// since we are scanning the cards from high to low order, once a "current"
		// becomes non-growable (i.e. cannot connect with the currently scanning card)
		// it is shelved to ps and sps and not looked at again.
		Vector<SingletonCardProperty> current_ps = new Vector<SingletonCardProperty>();
		Vector<SingletonCardProperty> current_sps = new Vector<SingletonCardProperty>();

		for (int i = 0; i < same_suit_cards.length;) {
			// Beginning of the for loop, we should always construct a new
			// SingletonCardProperty.  Well, at least temporarily, to include all of
			// the same cards together.
			SingletonCardProperty property = new SingletonCardProperty(same_suit_cards[i], trump_suit, trump_number);
			int new_actual_suit = same_suit_cards[i].CalculateActualSuit(trump_suit);
			Card current_card = same_suit_cards[i]; 
			int current_card_index =
				SingletonCardProperty.ConvertToPropertyNumber(same_suit_cards[i], trump_suit, trump_number);
			i++;
			while (i < same_suit_cards.length && 
				/* MQC: why so complicated? just want exactly the same card, right?
				SingletonCardProperty.ConvertToPropertyNumber(same_suit_cards[i], trump_suit, trump_number) == current_card_index &&
				same_suit_cards[i].CalculateActualSuit(trump_suit) == new_actual_suit
				*/ 
					current_card.GetIndex() == same_suit_cards[i].GetIndex()
				) {
				property.num_identical_cards++;
				property.num_cards++;
				i++;
			}
			
			// Shortcut, now, get rid of all current properties/secondary properties that
			// can no longer grow with the current property.
			MoveNonCurrentProperties(current_ps, ps, current_card_index);
			MoveNonCurrentProperties(current_sps, sps, current_card_index);
			
			// Shortcut.  We only have one identical card, simply add that final property
			// directly to ps and continue.
			if (property.num_identical_cards == 1) {
				ps.add(property);
				continue;
			}
			
			// Now loop through current_sps, which could be overlapping in any possible way.
			Vector<SingletonCardProperty> new_sps = new Vector<SingletonCardProperty>();
			for (SingletonCardProperty p : current_sps) {
				if (NextValidConsecutiveCard(p) == current_card_index) {
					SingletonCardProperty new_p =
						GrowToNewProperty(p, property.num_identical_cards, new_actual_suit);
					if (p.num_identical_cards <= property.num_identical_cards) {
						p.Copy(new_p);
					} else {
						new_sps.add(new_p);
					}
				}
			}
			current_sps.addAll(new_sps);
			// Now loop through current_ps
			// There is a reason that we go through current_ps in the order that properties
			// are added, consider the following situation, we are playing: 3 of spade.  And
			// we have:
			// 3s 3s 3h 3h 3d 3d As As
			// There are two interpretations:
			// 3s 3s 3h 3h As As and 3D 3D, or
			// 3s 3s 3h 3h and 3d 3d As As
			// Actually the first declaration has advantage, so we'll take that one
			// as properties, the secondary interpretation of 3d 3d As As will count
			// as secondary properties.
			boolean property_used_for_extension = false;
			for (SingletonCardProperty p : current_ps) {
				int next_card_index = NextValidConsecutiveCard(p);
				if (next_card_index == current_card_index &&
						p.num_identical_cards == property.num_identical_cards) {
					SingletonCardProperty new_p =
						GrowToNewProperty(p, p.num_identical_cards, new_actual_suit);
					if (!property_used_for_extension) {
						// Great, we should just extend this particular property.
						p.Copy(new_p);
						// We consumed property (properties is non-overlapping)
						property_used_for_extension = true;
					} else {
						// Sigh, grow secondary property ba.
						property.has_other_combo = true;
						current_sps.add(GrowToNewProperty(p, p.num_identical_cards, new_actual_suit));
					}
				} else if (next_card_index == current_card_index) {
					// we don't have identical cards, ;(.
					// Should start a secondary_property.
					property.has_other_combo = true;
					// New secondary property.
					current_sps.add(GrowToNewProperty(p, property.num_identical_cards, new_actual_suit));	
				}
			}
			if (!property_used_for_extension)
				current_ps.add(property);
		}
		ps.addAll(current_ps);
		sps.addAll(current_sps);

		// Sometimes we might have discovered some identical secondary properties, detect and delete them.
		// Note that due to the multiplicity of the minor trump numbers, secondary properties might have some
		// property that IsEqual() property in properties.  But this is probably rare, and doesn't hurt us too
		// much.  Note that we should do this on a per suit basis.  Since we only want to match the same type
		// with each other, we are free in choosing the number of decks and number of players
		
		Collections.sort(sps, new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.SAME_TYPE_TOGETHER));
		
		for (int i = sps.size() - 1; i > 0; i--) {
			if (sps.get(i).equals(sps.get(i - 1)))
				sps.remove(i);
		}
		
	}
	
	private void MoveNonCurrentProperties(
			Vector<SingletonCardProperty> current_ps,
			Vector<SingletonCardProperty> ps, int current_card_index) {
		Vector<SingletonCardProperty> new_current = new Vector<SingletonCardProperty>();
		for (SingletonCardProperty p : current_ps) {
			if (NextValidConsecutiveCard(p) > current_card_index)
				ps.add(p);
			else
				new_current.add(p);
		}
		current_ps.clear();
		current_ps.addAll(new_current);
	}

	/**
	 * Return the most important property of the highest leading number.
	 * 
	 * @return
	 */
	public SingletonCardProperty getLeadingProperty(SingletonCardPropertyComparator c) {
		if (properties.size() == 0)
			return null;
		Collections.sort(properties, c);
		return properties.get(0);
	}

	/**
	 * Return the next (lower end) card's index that would form a valid tractor with property
	 * 
	 * @return 
	 */
	private int NextValidConsecutiveCard(SingletonCardProperty property) {
		if (property.num_identical_cards <= 1)
			return -1;  // Need at least a pair to start consecutive hands.
		int ending_number = property.leading_number - property.num_sequences;
		if (property.leading_number > trump_number && ending_number <= trump_number) {
			ending_number--;  // Skip the trump_number
		}
		return ending_number;
	}
}
