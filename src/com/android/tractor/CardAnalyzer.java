package com.android.tractor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class CardAnalyzer {
	private int trumpSuit;
	private int trumpNumber;
	public int numDecks;
	private int numPlayers;
	public CardTracker unknown_cards_pool;
	public CardOrganizer[] per_player_cards;
	
	public CardProperty dummy_card_property;
	// A common type comparator that many uses.
	public SingletonCardPropertyComparator de_de_comparator;
	public SingletonCardPropertyComparator de_in_comparator;
	public SingletonCardPropertyComparator in_in_comparator;
	// when points preference is needed.
	public SingletonCardPropertyComparator de_points_in_imp_comparator;
	public SingletonCardPropertyComparator in_points_in_imp_comparator;
	
	public SingletonCardPropertyComparator in_imp_in_points_comparator;
	public SingletonCardPropertyComparator in_points_de_leading_number_comparator;
	public SingletonCardPropertyComparator de_leading_number_comparator;
	public SingletonCardPropertyComparator in_leading_number_comparator;
	
	public SingletonCardPropertyComparator de_de_identical_comparator;
	
	public PerDealCardComparator card_comparator;
	
	// Checking the validity of cards that follows  i.e. match same type but not necessarily higher
	public static final int VALIDATE_FOLLOW_MODE = 1;  
	// Checking the validity of cards that will take the lead in the current play i.e. match same type and higher
	public static final int VALIDATE_LEAD_MODE = 2; 
	// The above two mode are used by CardAnalyzer for checking validity of card play and determine winners.
	
	public static final int VALIDATE_MAX_LEAD_POINT_MODE = 3;
	public static final int VALIDATE_MAX_LEAD_NO_POINT_MODE = 4;
	public static final int VALIDATE_MIN_LEAD_POINT_MODE = 5;
	public static final int VALIDATE_MIN_LEAD_NO_POINT_MODE = 6;
	public static final int VALIDATE_FOLLOW_POINT_MODE = 7;
	public static final int VALIDATE_FOLLOW_NO_POINT_MODE = 8;
	public static final int VALIDATE_NO_POINT_MAX_LEAD_MODE = 9;
	
	public CardAnalyzer() {
		
	}
	
	/**
	 * Main routine for the constructor to set all per deal related parameters.
	 * 
	 * @param suit
	 * @param number
	 * @param decks
	 * @param players
	 */
	public void SetParameters(int suit, int number, int decks, int players) {
		trumpSuit = suit;
		trumpNumber = number;
		numDecks = decks;
		numPlayers = players;
		dummy_card_property = new CardProperty(null, trumpSuit, trumpNumber);
		// -1 for invalid mode.  Or a random mode.
		de_de_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.DE_TYPE_DE_LEADING_NUMBER, numDecks, numPlayers, trumpSuit);
		de_in_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.DE_TYPE_IN_LEADING_NUMBER, numDecks, numPlayers, trumpSuit);
		in_in_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.IN_TYPE_IN_LEADING_NUMBER, numDecks, numPlayers, trumpSuit);
		de_points_in_imp_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.DE_POINTS_IN_IMPORTANCE, numDecks, numPlayers, trumpSuit);
		in_points_in_imp_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.IN_POINTS_IN_IMPORTANCE, numDecks, numPlayers, trumpSuit);
		in_imp_in_points_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.IN_IMPORTANCE_IN_POINTS, numDecks, numPlayers, trumpSuit);
		in_points_de_leading_number_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.IN_POINTS_DE_LEADING_NUMBER, numDecks, numPlayers, trumpSuit);
		de_leading_number_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.DE_LEADING_NUMBER, numDecks, numPlayers, trumpSuit);
		in_leading_number_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.IN_LEADING_NUMBER, numDecks, numPlayers, trumpSuit);
		de_de_identical_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.DE_TYPE_DE_IDENTICAL_CARDS, numDecks, numPlayers, trumpSuit);
		card_comparator = new PerDealCardComparator(trumpSuit, trumpNumber);
	}
	
	/**
	 * CardAnalyzer provides analysis during game play, include:
	 * Check to see if a play is valid.
	 * Check to see which play wins the current round.
	 * Used by AIPlayer to find cards to play
	 * 
	 * @param suit Trump Suit
	 * @param number Trump Number
	 * @param decks Number of Decks
	 * @param players Number of Players
	 */
	public CardAnalyzer(int suit, int number, int decks, int players) {
		SetParameters(suit, number, decks, players);
	}
	
	/**
	 * Returns the cards that should be played according to lead_play.  If things go
	 * well, all lead_play cards will be returned.  If it's a throw and unsuccessful, the
	 * most important properties' cards (with the smallest leading number) is returned.
	 * 
	 * @param lead_play
	 * @param others_hands
	 * @return valid cards to be played as lead play
	 */
	public Card[] FindLegalLeadingCards(Card[] lead_play, Vector<Card[]> others_hands) {
		CardProperty property = new CardProperty(lead_play, trumpSuit, trumpNumber);
		if (property.is_mixed_suit) {
			return null;  // TODO: maybe we should penalize them and do something?
		}
		if (property.properties.size() == 1) {
			return lead_play;  // All cards allowed
		}
		SingletonCardProperty p = null;
		for (Card[] cards : others_hands) {
			SingletonCardProperty pp = FindViolationInThrowHand(property, cards);
			if (pp != null &&
				(p == null ||
				 de_de_comparator.compare(pp, p) < 0))
				p = pp;
		}
		if (p == null)
			return lead_play;
		return p.ToCards();
	}
	
	
	
	/**
	 * Determines if attempt_cards is throwable against target_cards.
	 * Return the violated property that is the most important and
	 * with the smallest leading number.
	 * null means the throw is successful.
	 * 
	 * @param attempt_cards Cards to be throwed
	 * @param target_cards Card to check against the throw.
	 * @return the violated property that is the most important and with the smallest leading number.
	 */
	private SingletonCardProperty FindViolationInThrowHand(CardProperty lead_property, Card[] target_cards) {
		CardProperty verifier = new CardProperty(target_cards, trumpSuit, trumpNumber);
		Collections.sort(lead_property.properties, de_de_comparator);
		for (SingletonCardProperty attempt_property : lead_property.properties) {
			for (SingletonCardProperty verify_property: verifier.properties) {
				if (verify_property.IsBreakable(attempt_property)) {
					Util.w("Break property", Arrays.toString(verify_property.ToCards()) + "broke " + Arrays.toString(attempt_property.ToCards()));
					Util.w("From property:", Arrays.toString(target_cards));
					return attempt_property;
				}
			}
			for (SingletonCardProperty verify_property: verifier.secondary_properties) {
				if (verify_property.IsBreakable(attempt_property)) {
					Util.w("Break property", verify_property.ToCards().toString() + "broke " + attempt_property.ToCards().toString());
					Util.w("From secondary property:", Arrays.toString(target_cards));	
					return attempt_property;
				}
			}
		}
		return null;
	}

	/**
	 * Give a set of leading cards, return the property that should be followed
	 * in the subsequent plays to determine winner.  It's usually the property
	 * of the most importance and the biggest leading number
	 * 
	 * @param play leading cards
	 * @return property to be beaten in order to win.
	 */
	public SingletonCardProperty GetWinningPropertyForPlay(Card[] play) {
		CardProperty p = new CardProperty(play, trumpSuit, trumpNumber);
		Collections.sort(p.properties, de_de_comparator);
		return p.properties.get(0);
	}
	
	public SingletonCardProperty GetWinningPropertyForCardProperty(CardProperty p) {
		Collections.sort(p.properties, de_de_comparator);
		return p.properties.get(0);
	}
	
	/**
	 * Decides of properties contain a property that is of equal or bigger type than p
	 * 
	 * @param properties
	 * @param p
	 * @return
	 */
	static public boolean ContainsProperty(Vector<SingletonCardProperty> properties, SingletonCardProperty p) {
		for (SingletonCardProperty pp : properties) {
			if (pp.IsBiggerTypeThan(p))
				return true;
		}
		return false;
	}
	
	/**
	 * From the matched_properties list, find all the properties that are missing from the matched_properties
	 * w.r.t. p.  Useful to figure out if the user is void of certain properties etc.
	 * Note: unfortunately this function is not complete.  Say AAAKKK, and somebody followed with say 4455.
	 * Then, we should also be able to infer that that somebody doesn't have say 2 triples.  Because 2 triples will
	 * have a higher probability then 4455.  Enumerate through all possible ways to match AAAKKK should be doable.  But
	 * let's get the other parts of the game work first.
	 * 
	 * @param p
	 * @param matched_properties
	 * @return
	 */
	private Vector<SingletonCardProperty> FindMissingProperties(SingletonCardProperty p,
			Vector<SingletonCardProperty> matched_properties) {
		Vector<SingletonCardProperty> missing_properties = new Vector<SingletonCardProperty>();
		for (SingletonCardProperty m_p : matched_properties) {
			for (SingletonCardProperty parent_p : m_p.ParentPropertyTypes()) {
				if (p.IsBiggerTypeThan(parent_p) && !ContainsProperty(matched_properties, parent_p))
					missing_properties.add(parent_p);
			}
		}
		return missing_properties;
	}
	
	/**
	 * delete all properties that are equal or bigger to p in properties, leaving just a copy of p
	 * 
	 * @param properties
	 * @param p
	 */
	public void EliminateProperty(Vector<SingletonCardProperty> properties, SingletonCardProperty p) {
		int index = 0;
		while (index < properties.size()) {
			SingletonCardProperty pp = properties.get(index);
			if (p.IsSmallerTypeThan(pp))
				properties.remove(index);
			else
				index++;
		}
		properties.add(p);
	}
	
	/**
	 * Given a bunch of properties, decide that if p should be added to properties.
	 * p will either replace a smaller property (or an equal property with larger
	 * leading number), or consumed by a bigger property
	 * already in the collection.  Note: this function makes use of the last_num_identical_cards and last_num_sequences
	 * fields.  When two type are the same, we will favor the one with a lesser last(original) type.
	 *
	 * @param properties A bunch of properties that are not compatible with each other. not in any sorted order.
	 * @param p
	 * @param want_points whether we favor exact type properties with more points, or less points.
	 */
	public void ConsumeProperty(Vector<SingletonCardProperty> properties, SingletonCardProperty p) {
		// We won't consume singleton cards, just not interesting.
		if (p.num_cards == 1)
			return;
		boolean add_p = true;
		for (SingletonCardProperty pp : properties) {
			if (p.IsExactType(pp)) {
				add_p = false;
				if (p.last_num_sequences == 0 && pp.last_num_sequences != 0) {
					pp.Copy(p);
				} if (p.last_num_sequences != 0 && pp.last_num_sequences == 0) {
					// p will be discareded
				} else if (p.last_num_sequences != pp.last_num_sequences ||
						p.last_num_identical_cards  != pp.last_num_identical_cards) {
					// They come from two different types, take the one with lesser important.
					SingletonCardProperty dummy_p = SingletonCardProperty.CreatePropertyOfType(p.last_num_identical_cards,
							p.last_num_sequences);
					SingletonCardProperty dummy_pp = SingletonCardProperty.CreatePropertyOfType(pp.last_num_identical_cards,
							pp.last_num_sequences);
					if (in_in_comparator.compare(p, pp) < 0)
						pp.Copy(p);
				}
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
	 * Find all properties in the current hand that is of smaller type to property and hence
	 * should be matched.  Return all non-compatible such properties.  Note that if we have AAKK
	 * and the property to match is 22, we are forced to return a pair.
	 * 
	 * @return
	 */
	private Vector<SingletonCardProperty> MatchProperty(SingletonCardProperty property, Card[] cards) {
		CardProperty properties = new CardProperty(cards, trumpSuit, trumpNumber);
		Collections.sort(properties.properties, de_in_comparator);
		Vector<SingletonCardProperty> matched_properties = new Vector<SingletonCardProperty>();
		for (SingletonCardProperty p : properties.properties) {
			if (p.num_cards == 1)
				break;  // no longer interesting.
			if (property.IsExactType(p)) {
				matched_properties.clear();
				matched_properties.add(p);
				return matched_properties;
			} else if (property.IsBiggerTypeThan(p)) {
				ConsumeProperty(matched_properties, p);
			} else if (property.num_identical_cards >= p.num_identical_cards) {
				// We can still force p.num_identical_cards
				// TODO: we should match not the leading number, but the
				// trailing number. i.e., 8877 match 22, we should play 77 instead
				// of 88.
				p.ConvertToType(p.num_identical_cards, 1);
				ConsumeProperty(matched_properties, p);
			}
		}
		return matched_properties;
	}
	
	/**
	 * p is a property, which dominates to_be_deleted.  We want to break up p to rid of
	 * the part that contains to_be_deleted
	 * 
	 * @param p
	 * @param to_be_deleted
	 * @return zero, one, or two properties that remain.
	 */
	private Vector<SingletonCardProperty> DeleteProperty(SingletonCardProperty p, SingletonCardProperty to_be_deleted) {
		Vector<SingletonCardProperty> properties = new Vector<SingletonCardProperty>();
		SingletonCardProperty first_candidate = new SingletonCardProperty(trumpSuit, trumpNumber);
		first_candidate.Copy(p);
		first_candidate.num_sequences -= to_be_deleted.num_sequences;
		first_candidate.num_cards -= to_be_deleted.num_cards;
		SingletonCardProperty second_candidate = new SingletonCardProperty(trumpSuit, trumpNumber);
		second_candidate.Copy(p);
		second_candidate.num_identical_cards -= to_be_deleted.num_identical_cards;
		second_candidate.num_sequences = to_be_deleted.num_sequences;
		second_candidate.num_cards = second_candidate.num_identical_cards * second_candidate.num_sequences;
		if (first_candidate.num_sequences > 0)
			properties.add(first_candidate);
		if (second_candidate.num_identical_cards > 1)
			properties.add(second_candidate);
		return properties;
	}
	
	/**
	 * Given a set of properties, find all the properties contained in the same suit
	 * cards that are forced by the set of the properties.  We do this on a per property
	 * basis.  In case of multiple choices, i.e., AAAKKK, should it force QQJJ or 999777,
	 * we'll return the combination of the higher importance.  We'll use a local copy of cards
	 * same_suit_cards are NOT altered.  If void_properties is not null, we'll return the set
	 * of properties that are missing when matching properties.  i.e., if we don't have a pair, etc.
	 * want_point controls when we find all forced properties, for properties of the exact type, if we
	 * favor points or not.
	 * 
	 * @param properties
	 * @param same_suit_cards
	 * @param void_properties
	 * @param want_points.
	 * @return
	 */
	public Vector<SingletonCardProperty> FindAllForcedProperties(Vector<SingletonCardProperty> properties, Card[] same_suit_cards,
			Vector<SingletonCardProperty> void_properties) {
		Vector<SingletonCardProperty> forced_properties = new Vector<SingletonCardProperty>();
		Card[] remaining_cards = same_suit_cards.clone();
		while (properties != null && properties.size() > 0) {
			// de_*_ comparator will do.
			Collections.sort(properties, de_de_identical_comparator);
			SingletonCardProperty p = properties.get(0);
			if (p.num_cards == 1)
				return forced_properties;
			Vector<SingletonCardProperty> matched_properties = MatchProperty(p, remaining_cards);
			// shortcut
			if (matched_properties.size() == 0) {
				properties.remove(0);
				if (void_properties != null) {
					// We didn't find anything to match the current property, that means that
					// we are missing the most basic card property, and that's the worst that can happen.
					SingletonCardProperty void_p = SingletonCardProperty.CreatePropertyOfType(2, 1);
					EliminateProperty(void_properties, void_p);
				}
				continue;
			}
			if (matched_properties.size() == 1 && matched_properties.get(0).IsExactType(p)) {
				forced_properties.add(matched_properties.get(0));
				remaining_cards = Card.DeleteCards(remaining_cards, matched_properties.get(0).ToCards());
				properties.remove(0);
				continue;
			}
			if (void_properties != null) {
				for (SingletonCardProperty missed_p : FindMissingProperties(p, matched_properties)) {
					EliminateProperty(void_properties, missed_p);
				}
			}
			Vector<Vector<SingletonCardProperty> > properties_all_possible_way = new Vector<Vector<SingletonCardProperty> >();
			for (SingletonCardProperty pp: matched_properties) {
				Card[] new_remaining_cards = Card.DeleteCards(remaining_cards, pp.ToCards());
				Vector<SingletonCardProperty> remaining_properties = DeleteProperty(p, pp);
				Vector<SingletonCardProperty> accumulate_properties = new Vector<SingletonCardProperty>();
				accumulate_properties.add(pp);
				accumulate_properties.addAll(FindAllForcedProperties(remaining_properties, new_remaining_cards, void_properties));
				properties_all_possible_way.add(accumulate_properties);
			}
			int best_choice = -1;
			if (properties_all_possible_way.size() == 1)
				best_choice = 0;
			else 
				best_choice = FindMaxPropertiesIndex(properties_all_possible_way);
			if (best_choice == -1) {
				Util.e("ArrayOutOfBound", "ERR");
			}
			forced_properties.addAll(properties_all_possible_way.get(best_choice));
			for (SingletonCardProperty ppp: properties_all_possible_way.get(best_choice)) {
				remaining_cards = Card.DeleteCards(remaining_cards, ppp.ToCards());
			}
			properties.remove(0);
		}
		return forced_properties;
	}
	
	
	public int FindMaxPropertiesIndex(Vector<Vector<SingletonCardProperty>> properties_vector) {
		// When we have lots of decks, a lot of the properties have probability 1, hence we have a tie situation.
		// If this is the case, we'll break ties w.r.t. num_identical_cards.
		for (Vector<SingletonCardProperty> properties: properties_vector) {
			Collections.sort(properties, de_de_identical_comparator);
		}
		Collections.sort(properties_vector, new Comparator<Vector<SingletonCardProperty>>(){

			@Override
			public int compare(Vector<SingletonCardProperty> arg0,
					Vector<SingletonCardProperty> arg1) {
				int index = 0;
				int result = 0;
				while (result == 0 && index < arg0.size() && index < arg1.size()) {
					result = de_de_identical_comparator.compare(arg0.get(index), arg1.get(index));
					index++;
				}
				return result;
			}});
		// 2 to be safe.  When we have lots of decks, there are a lot of the property
		// that will have probability 1, for instance, 10 decks, 4 players, AAKKQQ
		// is a sure thing, ;).
		double min_probability = 2;
		int index_in_vector = -1;
		int index = 0;
		for (Vector<SingletonCardProperty> properties: properties_vector) {
			double probability = 1;
			for (SingletonCardProperty p: properties) {
				probability *= p.Probability(numPlayers, numDecks); 
			}
			if (min_probability > probability) {
				index_in_vector = index;
				min_probability = probability;
			}
			index++;
		}
		return index_in_vector;
	}
	
	/**
	 * Get all instances of SingletonCardProperty that can cover the property.  But we don't
	 * want to repeat unnecessary properties.  For instance, if we want to cover a pair.
	 * And if we find a pair, which does not overlap with any other properties, then just
	 * that single pair is enough.  When a singleton card property overlaps with others
	 * (denoted by the has_other_combo) field, then we'll include that as a possibility.
	 * Note: reorders properties.
	 * 
	 * @param property property to be covered
	 * @param properties properties that can cover (i.e., of equal or bigger type) property
	 * @param mode potentially for AI, we want to enhance this function a bit and pass in
	 * more directions on how we should match up the cards, in new modes.
	 * For now, VALIDATE_FOLLOW_MODE and VALIDATE_LEAD_MODE is complete, which is used
	 * for TractorGameState.  And the AIplay just borrows from these two modes.
	 * @return
	 */
	private Vector<SingletonCardProperty> CoverPropertyAllPossibleWay(SingletonCardProperty property,
			Vector<SingletonCardProperty> properties, int mode) {
		Collections.sort(properties, in_in_comparator);
		Vector<SingletonCardProperty> covering_properties = new Vector<SingletonCardProperty>();
		for (SingletonCardProperty p : properties) {
			if (property.IsSmallerTypeThan(p) &&
				(mode == VALIDATE_FOLLOW_MODE ||
				 mode == VALIDATE_FOLLOW_POINT_MODE ||
				 mode == VALIDATE_FOLLOW_NO_POINT_MODE ||
				 (p.IsBreakable(property) ||
				  p.IsTrumpable(property)))) {
				// If we just have to match up properties, and if we found an exact match.
				// Then this has to be our best bet.  Simply return that to reduce the search
				// space.
				if (mode == VALIDATE_FOLLOW_MODE && p.IsExactType(property) && !p.has_other_combo) {
					covering_properties.clear();
					covering_properties.add(p);
					return covering_properties;
				}
				boolean add_p = true;
				for (SingletonCardProperty pp : covering_properties) {
					/*
					if (p.IsExactType(pp) && !p.has_other_combo && p.leading_number >
						pp.leading_number) {
						// Of the same type, larger leading number appears later.
						// so actually the last check is not necessary.
						pp.Copy(p);
						add_p = false;
						break;
					}
					*/
					if (mode == VALIDATE_FOLLOW_MODE &&
						pp.IsExactType(p) && !pp.has_other_combo) {
						// No point to bring in another property into our search space.
						add_p = false;
						break;
					}
				}
				if (add_p)
					covering_properties.add(p);
			}
		}
		return covering_properties;
	}

	/**
	 * Give a set of properties and some cards.  Find a set of cards that can completely cover
	 * the set of property, i.e., the number of cards returned should equal to total number
	 * of cards involved in the property.  Otherwise, return null.  Note that this function
	 * doesn't check for leading numbers in the properties.  This function is used by the GameState to
	 * check for validity of follow plays, and check that a set of trump cards can indeed trump
	 * a lead play.  In these situations, we don't care about the actual set of cards that can
	 * cover the properties, we'll pass in null for cover_cards.
	 * Currently this is also used by AIPlayer to select a set of cards to trump a lead play, after
	 * the lead property is taken care of, with cover_cards passed in and the function will
	 * fill it up.
	 * 
	 * @param properties
	 * @param same_suit_cards
	 * @param cover_cards if not null, the function will fill the array with the actual
	 * set of cards that completely over properties.
	 * @return
	 */
	private boolean FindCardsToCoverProperties(Vector<SingletonCardProperty> initial_properties, Card[] same_suit_cards,
			Card[] cover_cards, boolean want_points) {
		if (initial_properties == null || initial_properties.size() == 0)
			return true;
		Vector<SingletonCardProperty> properties = new Vector<SingletonCardProperty>(initial_properties);
		Collections.sort(properties, de_de_comparator);
		int total_cards = 0;
		for (SingletonCardProperty p : properties)
			total_cards += p.num_cards;
		if (total_cards > same_suit_cards.length)
			return false;
		SingletonCardProperty first_property = properties.get(0);
		// Some shortcut
		if (first_property.num_cards == 1) {
			if (cover_cards != null)
				FillRestReturnPlay(same_suit_cards, cover_cards, total_cards, want_points);
			return true;
		} 
		/* This shortcut routine is giving me too much trouble.
		else if (first_property.num_cards == 2) {
			int num_pairs = 0;
			for (SingletonCardProperty p : properties) {
				if (p.num_cards == 2)
					num_pairs++;
				else
					break;  // we already sorted properties.
			}
			CardProperty same_suit_property = new CardProperty(same_suit_cards, trumpSuit, trumpNumber);
			// Here since we are only matching pairs, breaking up lesser properties first, hence in_in.
			if (want_points)
				Collections.sort(same_suit_property.properties, de_points_in_imp_comparator);
			else
				Collections.sort(same_suit_property.properties, in_points_in_imp_comparator);
			int same_suit_pairs = 0;
			for (SingletonCardProperty p: same_suit_property.properties)
				same_suit_pairs += p.num_identical_cards / 2 * p.num_sequences;
			if (same_suit_pairs < num_pairs)
				return false;
			else if (cover_cards == null)
				return true;
			// we need to fill cover_cards
			int pairs_matched = 0;
		    int index = cover_cards.length - total_cards;
		    Vector<Card> current_cover_cards = new Vector<Card>();
		    for (SingletonCardProperty p : same_suit_property.properties) {
				if (pairs_matched >= num_pairs)
					break;
				if (p.num_identical_cards < 2)
					continue;
				pairs_matched += (p.num_identical_cards / 2) * p.num_sequences;
				p.num_identical_cards = p.num_identical_cards / 2 * 2;
				p.num_cards = p.num_identical_cards * p.num_sequences;
				Vector<Card> new_cards = p.ToCardVectorReverse();
				int length = new_cards.size();
				if (pairs_matched > num_pairs) {
					length -= (pairs_matched - num_pairs) * 2;
					new_cards = SelectCardTargetingPoints(new_cards, length, ConvertWantPointsToAllowedPoints(want_points), false);
				}
				current_cover_cards.addAll(new_cards);
				System.arraycopy(new_cards.toArray(new Card[0]), 0, cover_cards, index, length);
				index += length;
			}
			if (index < cover_cards.length) {
				Collections.sort(current_cover_cards, card_comparator);
				Card[] remaining_cards = Card.DeleteCards(same_suit_cards, current_cover_cards.toArray(new Card[0]));
				FillRestReturnPlay(remaining_cards, cover_cards, cover_cards.length - index, want_points);
			}
			return true;
		}
		*/
		// Now the heavy routine, cover the first property and then recurse.
		properties.remove(0);
		int mode = VALIDATE_FOLLOW_POINT_MODE;
		if (!want_points)
			mode = VALIDATE_FOLLOW_NO_POINT_MODE;
		if (FindBestCovering(first_property, properties, same_suit_cards, cover_cards, mode) != null)
			return true;
		return false;
	}
	
	/**
	 * From the cards, find the best possible way of covering both leading_property
	 * and the rest_properties, depending on the mode.  Also fill the rest of cover_cards
	 * with the cards that covering the property if successful.
	 * 
	 * @param leading_property the property to beat in VALIDATE_LEAD_MODE, to be covered in other modes
	 * @param rest_properties rest of the property to be covered
	 * @param cards cards that should cover all the properties
	 * @param cover_cards cards that actually cover the properties if not null
	 * @param mode
	 * @return a property if covering is successful, and in VALIDATE_LEAD_MODE, the property
	 * beats the leading_property and has the biggest leading number.
	 */
	public SingletonCardProperty FindBestCovering(SingletonCardProperty leading_property,
			Vector<SingletonCardProperty> rest_properties,
			Card[] cards, Card[] cover_cards, int mode) {
		// Should not change the property passed in.
		SingletonCardProperty to_beat = leading_property.clone();
		// sorting of card_property properties done in CoverPropertyAllPossibleWay.
		CardProperty card_property = new CardProperty(cards, trumpSuit, trumpNumber);
		Vector<SingletonCardProperty> winning_properties =
			CoverPropertyAllPossibleWay(to_beat, card_property.properties, mode);
		Vector<SingletonCardProperty> winning_properties_2 =
			CoverPropertyAllPossibleWay(to_beat, card_property.secondary_properties, mode);
		winning_properties.addAll(winning_properties_2);
		if (mode == VALIDATE_LEAD_MODE || mode == VALIDATE_MAX_LEAD_NO_POINT_MODE ||
				mode == VALIDATE_MAX_LEAD_POINT_MODE)  // leading number, that's all that matters.
			Collections.sort(winning_properties, de_leading_number_comparator);
		else if (mode == VALIDATE_FOLLOW_MODE)  // use increasing property type
			Collections.sort(winning_properties, in_in_comparator);
		else if (mode == VALIDATE_MIN_LEAD_POINT_MODE || mode == VALIDATE_FOLLOW_POINT_MODE)
			Collections.sort(winning_properties, de_points_in_imp_comparator);
		else if (mode == VALIDATE_MIN_LEAD_NO_POINT_MODE || mode == VALIDATE_FOLLOW_NO_POINT_MODE)
			Collections.sort(winning_properties, in_points_in_imp_comparator);
		else if (mode == VALIDATE_NO_POINT_MAX_LEAD_MODE)
			Collections.sort(winning_properties, in_points_de_leading_number_comparator);
		boolean want_points = false;
		if (mode == VALIDATE_MAX_LEAD_POINT_MODE || mode == VALIDATE_MIN_LEAD_POINT_MODE || mode == VALIDATE_FOLLOW_POINT_MODE)
			want_points = true;
		for (SingletonCardProperty p : winning_properties) {
			Vector<SingletonCardProperty> new_lead_properties = new Vector<SingletonCardProperty>();
			if (mode == VALIDATE_LEAD_MODE || mode == VALIDATE_MAX_LEAD_NO_POINT_MODE ||
					mode == VALIDATE_MAX_LEAD_POINT_MODE || mode == VALIDATE_FOLLOW_MODE) {
				p.ConvertToType(to_beat);    // This will keep the leading number unchanged.
				new_lead_properties.add(p);
			} else {
				// There is no point to make p as big as possible, in fact, we should respect want_points more.
				new_lead_properties.addAll(p.ConvertToTypeAllLeadingNumber(to_beat));
				if (mode == VALIDATE_MIN_LEAD_POINT_MODE || mode == VALIDATE_MIN_LEAD_NO_POINT_MODE ||
						mode == VALIDATE_NO_POINT_MAX_LEAD_MODE) {
					// we have breaking up the property, we'll need to check to make sure that individual ones still
					// can lead
					int index = 0;
					while (index < new_lead_properties.size()) {
						if (!new_lead_properties.get(index).IsBreakable(to_beat) &&
								!new_lead_properties.get(index).IsTrumpable(to_beat))
							new_lead_properties.remove(index);
						else
							++index;
					}
				}
				// resort according to various modes.
				if (mode == VALIDATE_MIN_LEAD_POINT_MODE || mode == VALIDATE_FOLLOW_POINT_MODE)
					Collections.sort(new_lead_properties, de_points_in_imp_comparator);
				if (mode == VALIDATE_MIN_LEAD_NO_POINT_MODE || mode == VALIDATE_FOLLOW_NO_POINT_MODE)
					Collections.sort(new_lead_properties, in_points_in_imp_comparator);
				if (mode == VALIDATE_NO_POINT_MAX_LEAD_MODE)
					Collections.sort(new_lead_properties, in_points_de_leading_number_comparator);
			}
			for (SingletonCardProperty pp : new_lead_properties) {
				Card[] remaining_cards = Card.DeleteCards(cards, pp.ToCards());
				if (FindCardsToCoverProperties(rest_properties, remaining_cards, cover_cards, want_points)) {
					if (cover_cards != null) {
						int num_cards_remaining = 0;
						if (rest_properties != null)
							for (SingletonCardProperty ppp: rest_properties)
								num_cards_remaining += ppp.num_cards;
						System.arraycopy(pp.ToCards(), 0, cover_cards, cover_cards.length - num_cards_remaining - pp.num_cards, pp.num_cards);
					}
					return pp;
				}
			}
		}
		return null;
	}
	
	/**
	 * Compare the two sets of cards lead_play and follow_play and decide
	 * which if follow_play is winning the analysis so far, based on the current
	 * property to beat.
	 * We assume this function is called by the monitor to decide if follow_play
	 * is winning, thus:
	 * 0. lead_play and follow_play has the same number of cards
	 * 1. follow_play must strictly beat the winning_property. This is due to
	 * the tie breaking rule follow the earlier players. 
	 * 
	 * @param lead_play The set of cards that leads the current round
	 * @param follow_play The set of cards that followed (doesn't have to immediately follow)
	 * @param winning_property The winning property from previously analyzed follow_plays.
	 * @return the winning SingletonCardProperty if indeed winning, null otherwise.
	 */
	public SingletonCardProperty IsWinningPlay(Card[] lead_play, Card[] follow_play, SingletonCardProperty property) {
		// Note that the following sorts lead_play and follow_play.
		CardProperty lead_property = new CardProperty(lead_play, trumpSuit, trumpNumber);
		CardProperty follow_property = new CardProperty(follow_play, trumpSuit, trumpNumber);
		// mixed suit hand is the worst no matter what.
		if (follow_property.is_mixed_suit)
			return null;
		// Shortcut for early returns
		if (follow_property.suit != lead_property.suit && !follow_property.is_trump_suit)
			return null;
		if (follow_property.suit == lead_property.suit && lead_property.properties.size() > 1)
			return null;  // A legal throw hand cannot be beaten by a hand of the same suit.
		// Should not change the property passed in.
		SingletonCardProperty to_beat = property.clone();
		Collections.sort(lead_property.properties, de_de_comparator);
		lead_property.properties.remove(0);  // the first property is actually to_beat.
		return FindBestCovering(to_beat, lead_property.properties, follow_play, null, VALIDATE_LEAD_MODE);
	}

	/**
	 * Decides of the follow play is a legal play to follow lead_play, out of 
	 * the current_hand.  Updates error_message
	 * 
	 * @param lead_play the cards that lead the current round
	 * @param follow_play the cards that followed
	 * @param current_hand current hand where the follow_play is from
	 * @param error_message error_message[0] will be updated.
	 * @return true if follow_play is legal.
	 */
	public boolean IsFollowPlayFollowProperty(Card[] lead_play, Card[] follow_play, Card[] current_hand,
			int[] error_message_code, Vector<SingletonCardProperty> properties) {
		// Get all cards of the same suit in current_hand.
		int suit = lead_play[0].CalculatePlaySuit(trumpSuit, trumpNumber);
		Card[] same_suit_cards = CardOrganizer.GetSameSuitCards(suit, current_hand, trumpSuit, trumpNumber);
		if (same_suit_cards.length <= lead_play.length) {
			if (!Card.Contains(follow_play, same_suit_cards)) {
				error_message_code[0] = TractorMessageCenter.FOLLOW_SUIT;
				error_message_code[1] = suit;
				return false;
			}
			return true;
		}
		// Check to make sure that follow_play are following suit.
		for (Card card: follow_play) {
			if (card.CalculatePlaySuit(trumpSuit, trumpNumber) != suit) {
				error_message_code[0] = TractorMessageCenter.FOLLOW_SUIT;
				error_message_code[1] = suit;
				return false;
			}
		}
		// Now check to make sure that follow_play honored properties.
		CardProperty lead_property = new CardProperty(lead_play, trumpSuit, trumpNumber);
		Vector<SingletonCardProperty> forced_properties = FindAllForcedProperties(lead_property.properties, same_suit_cards, null);
		// CoverProperties will alter follow_play, we make a local copy first.
		Card[] tmp_follow_play = follow_play.clone();
		Vector<SingletonCardProperty> tmp_forced_properties = (Vector<SingletonCardProperty>) forced_properties.clone();
		if (!FindCardsToCoverProperties(tmp_forced_properties, tmp_follow_play, null, false)) {
			error_message_code[0] = TractorMessageCenter.FOLLOW_PROPERTY;
			properties.clear();
			properties.addAll(forced_properties);
			return false;
		}
		return true;
	}
	
	protected int GetTotalPoints(Card[] cards) {
		int points = 0;
		for (Card c : cards)
			points += c.GetPoints();
		return points;
	}

	protected int GetTotalPoints(Vector<Card> cards) {
		int points = 0;
		for (Card c : cards)
			points += c.GetPoints();
		return points;
	}
	
	/**
	 * Return a set of cards that will best match up with the num_cards with the allowed_points limit.
	 * If respect_points_limit is on, then we won't be adding cards which will result in points exceed.
	 * 
	 * @param cards
	 * @param num_cards
	 * @param allowed_points
	 * @param respect_points_limit
	 * @return
	 */
	public Vector<Card> SelectCardTargetingPoints(Vector<Card> cards, int num_cards, int allowed_points, boolean
			respect_points_limit) {
		return SelectCardTargetingPoints(cards.toArray(new Card[0]), num_cards, allowed_points, respect_points_limit);
	}
	
	private Card[] ConserveHighTrumpSuitCards(Card[] trump_cards, int num_cards) {
		if (trump_cards.length <= num_cards)
			return trump_cards;
		// Should we try to avoid points, we should not sacrifice too much big trump cards.  Because if we will
		// be trumped out, then we lose these points that we are trying to save anyway.
		Arrays.sort(trump_cards, card_comparator);
		Vector<Card> high_trump_cards = new Vector<Card>();
		for (Card c : trump_cards) {
			if (SingletonCardProperty.ConvertToPropertyNumber(c, trumpSuit, trumpNumber) >= SingletonCardProperty.MINOR_TRUMP_NUMBER)
				high_trump_cards.add(c);
			else
				break;  // trump_cards sorted!
		}
		Card[] high_trump_cards_array = high_trump_cards.toArray(new Card[0]);
		CardProperty permanent_trump_property = new CardProperty(high_trump_cards_array, trumpSuit, trumpNumber);
		Collections.sort(permanent_trump_property.properties, in_leading_number_comparator);
		Collections.reverse(permanent_trump_property.properties);
		// We should only plan to keep a few of these high cards, while trying to conserve high cards
		// We'll try to save numDeck of trumps.
		int total_trump_cards = trump_cards.length;
		int num_cards_to_save = Math.min(numDecks, total_trump_cards - num_cards);
		if (num_cards_to_save >= high_trump_cards_array.length) {
			// Easy route, all the high trumps can be saved.
			return Card.DeleteCards(trump_cards, high_trump_cards_array);
		}
		Card[] new_remaining_cards = new Card[total_trump_cards - num_cards_to_save];
		int index = total_trump_cards - high_trump_cards_array.length;
		System.arraycopy(trump_cards, high_trump_cards_array.length, new_remaining_cards, 0, index);
		int num_cards_saved = 0;
		while (permanent_trump_property.properties.size() > 0 && num_cards_saved < num_cards_to_save) {
			SingletonCardProperty p = permanent_trump_property.properties.get(0);
			permanent_trump_property.properties.remove(0);
			if (p.num_cards > num_cards_to_save - num_cards_saved) {
				// Need to break up the property, we should add back some cards
				Vector<Card> cards = p.ToCardVector();
				// Delete the first n cards, these are cards that should not be considered further.
				for (int i = 0; i < num_cards_to_save - num_cards_saved ; ++i)
					cards.remove(0);
				System.arraycopy(cards.toArray(new Card[0]), 0, new_remaining_cards, index, cards.size());
				index += cards.size();
				break;
			}
			num_cards_saved += p.num_cards;
		}
		// Now copy the rest of the properties remaining in permanent_trump_property.properties
		for (SingletonCardProperty p : permanent_trump_property.properties) {
			System.arraycopy(p.ToCards(), 0, new_remaining_cards, index, p.num_cards);
			index += p.num_cards;
		}
		if (index != new_remaining_cards.length) {
			Util.e("Error", "Didn't fill out cards.");
		}
		return new_remaining_cards;
	}
	
	/**
	 * Return a set of cards that will best match up with the num_cards with the allowed_points limit.
	 * If respect_points_limit is on, then we won't be adding cards which will result in points exceed.
	 * 
	 * @param cards
	 * @param num_cards
	 * @param allowed_points
	 * @param respect_points_limit
	 * @return
	 */
	public Vector<Card> SelectCardTargetingPoints(Card[] cards, int num_cards, int allowed_points,
			boolean respect_points_limit) {
		Arrays.sort(cards, card_comparator);
		// First get out all the trump cards and make them separate from the rest.
		Card[] trump_cards = CardOrganizer.GetSameSuitCards(trumpSuit, cards, trumpSuit, trumpNumber);
		Card[] non_trump_cards = Card.DeleteCards(cards, trump_cards);
		CardProperty property = new CardProperty(non_trump_cards, trumpSuit, trumpNumber);
		// TODO: perhaps we should consider other ordering, instead of the type probability, for instance, calculate
		// some probability that takes into account the current cards remaining, the various suit situation for each players
		// etc.  But this is likely too complicated to get a clean rule.  AI can probably rank the properties differently.
		if (allowed_points == Integer.MAX_VALUE)
			Collections.sort(property.properties, de_points_in_imp_comparator);
		else
			Collections.sort(property.properties, in_imp_in_points_comparator);
		if (property.total_num_cards < num_cards) {
			// We'll need to involve trump suit no matter what.
			Card[] real_trump_cards = trump_cards;
			if (allowed_points != Integer.MAX_VALUE)
				real_trump_cards = ConserveHighTrumpSuitCards(trump_cards, num_cards - property.total_num_cards);
			CardProperty trump_property = new CardProperty(real_trump_cards, trumpSuit, trumpNumber);
			if (allowed_points == Integer.MAX_VALUE) {
				Collections.sort(trump_property.properties, de_points_in_imp_comparator);
			} else {
				Collections.sort(trump_property.properties, in_imp_in_points_comparator);
			}
			property.properties.addAll(trump_property.properties);
		}
		Vector<Card> return_cards = new Vector<Card>(); 
		int index = 0;  // index into property.properties.
		int total_cards = 0;
		while (property.properties.size() > 0 && index < property.properties.size()) {
			SingletonCardProperty p = property.properties.get(index);
			Vector<Card> new_cards = p.ToCardVectorReverse();
			if (total_cards + new_cards.size() <= num_cards) {
				if (p.TotalPoints() <= allowed_points) {
					return_cards.addAll(new_cards);
					total_cards += p.num_cards;
					if (allowed_points != Integer.MAX_VALUE) {
						allowed_points -= p.TotalPoints();
					}
					property.properties.remove(index);
				} else {
					++index;
				}
			} else {
				// We maybe need to break up the property, or skip it all together.
				if (allowed_points == Integer.MAX_VALUE) {
					// We only ordered the properties in decreasing point order, need to do that for
					// individual cards.
					if (p.num_sequences > 1)
						Collections.sort(new_cards, new Comparator<Card>() {
							public int compare(Card object1, Card object2) {
								int point_diff = object2.GetPoints() - object1.GetPoints();
								if (point_diff != 0)
									return point_diff;
								return -card_comparator.compare(object1, object2);
							}
						});
					return_cards.addAll(new_cards);
					return_cards.setSize(num_cards);
					return return_cards;
				} else {
					if (p.num_sequences > 1)
						Collections.sort(new_cards, new Comparator<Card>() {
							public int compare(Card object1, Card object2) {
								int point_diff = object1.GetPoints() - object2.GetPoints();
								if (point_diff != 0)
									return point_diff;
								return -card_comparator.compare(object1, object2);
							}
						});
					new_cards.setSize(num_cards - total_cards);
					if (GetTotalPoints(new_cards) <= allowed_points) {
						return_cards.addAll(new_cards);
						return return_cards;
					} else {
						index++;
					}
				}
			}
		}
		if (total_cards < num_cards && !respect_points_limit) {
			// We can't fit the allowed_points.  Just do another sorting and fill
			Collections.sort(property.properties, in_points_in_imp_comparator);
			for (SingletonCardProperty p : property.properties) {
				Vector<Card> new_cards = p.ToCardVectorReverse();
				if (total_cards + new_cards.size() > num_cards) {
					Collections.sort(new_cards, new Comparator<Card>() {
						public int compare(Card object1, Card object2) {
							int point_diff = object1.GetPoints() - object2.GetPoints();
							if (point_diff != 0)
								return point_diff;
							return -card_comparator.compare(object1, object2);
						}
					});
				}
				return_cards.addAll(new_cards);
				total_cards += p.num_cards;
				if (total_cards >= num_cards) {
					return_cards.setSize(num_cards);
					return return_cards;
				}
			}
		}
		return return_cards;
	}
	
	private int ConvertWantPointsToAllowedPoints(boolean want_points) {
		int points_limit = 0;
		if (want_points)
			points_limit = Integer.MAX_VALUE;
		return points_limit;
	}
	
	/**
	 * Fill the last n cards of return play with cards selected out of candidate_pool.
	 * Actually now it's smarter.  We sort cards according to properties, and we fill the return
	 * play with the worst possible properties.
	 * 
	 * @param candidate_pool available cards to select from
	 * @param return_play cards to return, we should fill the last "number" number of cards
	 * @param number number of cards to be selected and filled in
	 * @param want_points wheather prefer to put points in.
	 */
	protected void FillRestReturnPlay(Card[] candidate_pool, Card[] return_play, int number, boolean want_points) {
		if (number <= 0)
			return;
		Vector<Card> new_cards = SelectCardTargetingPoints(candidate_pool, number,
				ConvertWantPointsToAllowedPoints(want_points), false);
		System.arraycopy(new_cards.toArray(new Card[0]), 0, return_play, return_play.length - number, number);
	}
	
	public SingletonCardPropertyComparator getPropertyComparator(int mode) {
		return new SingletonCardPropertyComparator(mode, numDecks, numPlayers, trumpSuit);
	}
}
