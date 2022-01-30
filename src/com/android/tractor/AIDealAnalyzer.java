package com.android.tractor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

public class AIDealAnalyzer extends CardAnalyzer {
	// various mode we can do when trumping.  Roughly, we'll do:
	// Try the maximum card without points
	// *unimplemented* - use the absolutely maximum card with points.
	// Try the minimum card with points
	// Try a trump that will prevent points card from winning, without points.
	// *unimplemented* add some points, but not max.  Too complicated yeah, for now.
	public static int MAX_TRUMPING_WITHOUT_POINTS = 0;
	public static int MIN_TRUMPING_WITH_POINTS = 1;
	public static int PREVENT_POINT_TRUMPING = 2;
	
	// This one we change around to decide how aggressive the AIplayer is.
	// useful for debugging various situations.  Keep in mind that this number
	// is per AIplayer, we have n - 1 AIPlayers.  The total aggressiveness is
	// much greater than the individual level.
	static final int TOTAL_FEN_QING_LEVEL = 37;
	static final int FEN_QING_DEGREE = 3;
	
	int trumpSuit;
	int trumpNumber;
	int numPlayers;
	int numDecks;
	int myId;
	int rounds_remaining;
	Vector<Integer> other_players;
	Vector<Integer> all_players;
	
	private TractorGameState state;
	AILearner learner;
	Vector<Integer> last_afterstate;
	
	
	// TODO: maybe binary search on this value?  0.7 is about the min. probability of a consecutive pair in a 2 deck, 4 player game.
	// Or actually maybe make these values depend on numDecks and numPlayers.
	// I think it's worth running AITrainer a bit and find optimal values for popular (numPlayer, numDeck) pairs.
	// TODO: some of the probability here should be adjustment based on the possible points that could be involved in the
	// resulting round of play.
	double high_winning_chance_for_tractor_cards = 0.7;
	double high_winning_chance_for_leading_cards = 0.6;
	double high_winning_chance_for_follow_with_points = 0.7;  // We are also playing points.  Should be more restrict.
	double low_winning_chance_for_no_points = 0.2;
	
	
	public static final int AI_MODE_RULE_BASED = 0;
	public static final int AI_MODE_RANDOM = 1;
	public static final int AI_MODE_LEARNING = 2;
	
	private int AIMode = AI_MODE_RULE_BASED;
	public void setAIMode(int mode) {
		AIMode = mode;
		if (AIMode != AI_MODE_LEARNING) {
			// learner = null;
		}
	}

	private static final boolean checkCandidatesIncludeRuleBasedPlay = false;
	private static final boolean showAICandidates = false;

	
	/*  I haven't finalized how many number there should be.
	public void setProbabilityParams(double tractor_cards, double leading, double follow_with_points, double no_point_lead) {
		high_winning_chance_for_tractor_cards = tractor_cards;
		high_winning_chance_for_leading_cards = leading;
		high_winning_chance_for_follow_with_points = follow_with_points;
		low_winning_chance_for_no_points = no_point_lead;
	}
	*/
	
	public AIDealAnalyzer() {
		super();
		learner = new AILearner();  // dummy, does nothing.
	}
	
	public void SetParameters(int suit, int number, int players, int decks, int my_id, TractorGameState state) {
		super.SetParameters(suit, number, decks, players);
		this.state = state;
		unknown_cards_pool = new CardTracker(suit, number, decks);
		per_player_cards = new CardOrganizer[players];
		for (int i = 0; i < players; ++i) {
			per_player_cards[i] = new CardOrganizer(suit, number, this);
		}
		trumpSuit = suit;
		trumpNumber = number;
		numPlayers = players;
		numDecks = decks;
		myId = my_id;
		other_players = new Vector<Integer>();
		all_players = new Vector<Integer>();
		for (int i = 0; i < numPlayers; ++i) {
			all_players.add(i);
			if (i == myId)
				continue;
			other_players.add(i);
		}
		rounds_remaining = 0;
		de_de_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.DE_TYPE_DE_LEADING_NUMBER, numDecks, numPlayers, trumpSuit);
		de_points_in_imp_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.DE_POINTS_IN_IMPORTANCE, numDecks, numPlayers, trumpSuit);
		in_points_in_imp_comparator = new SingletonCardPropertyComparator(
				SingletonCardPropertyComparator.IN_POINTS_IN_IMPORTANCE, numDecks, numPlayers, trumpSuit);
	}

	/**
	 * Create an analyzer to track all player's move, suggest moves etc.
	 * 
	 * @param suit trump suit
	 * @param number trump number
	 * @param players total number of players
	 * @param decks total number of decks
	 * @param my_id player's own id (between [0, players - 1]).
	 */
	public AIDealAnalyzer(int suit, int number, int players, int decks, int my_id, TractorGameState state) {
		SetParameters(suit, number, players, decks, my_id, state);
	}
	
	// constructor to make unit test happy
	public AIDealAnalyzer(int suit, int number, int players, int decks, int my_id) {
		TractorGameParam param = new TractorGameParam();
		param.numPlayers = players;
		param.num_decks = decks;
		
		TractorGameState state = new TractorGameState(param);
		state.playerId = my_id;
		state.trump_suit = suit;
		state.trump_number = number;
		state.dealer_index_ = my_id;
		SetParameters(suit, number, players, decks, my_id, state);
	}


	public void AddCardsToMyHand(Card[] cards) {
		per_player_cards[myId].AddCards(cards, null);
		unknown_cards_pool.DeleteCards(cards);
		if (cards != null)
			rounds_remaining += cards.length;
	}
	
	public void DeleteTractorCardsFromMyHand(Card[] tractor_cards) {
		per_player_cards[myId].DeleteCards(tractor_cards);
		if (tractor_cards != null)
			rounds_remaining -= tractor_cards.length;
	}
	
	public void PlayerPlayedCards(Card[] cards, int id, Card[] lead_cards) {
		if (id == myId) {
			per_player_cards[myId].DeleteCards(cards);
			if (cards != null)
				rounds_remaining -= cards.length;
		} else {
			per_player_cards[id].AddCards(cards, lead_cards);
			// my cards are already deleted in AddCardsToMyHand.
			unknown_cards_pool.DeleteCards(cards);
		}
		if (rounds_remaining != 0 )
			ObserveLastRoundRewards(false);
		// last round's observation is called in LastHandResult
	}
	
	private void ObserveLastRoundRewards(boolean last_hand) {
		if (state != null && learner != null && state.current_round_played_players == 0) {
			// last round just ended, compute my last round's reward
			int reward = state.last_round_points / 5;
			if (last_hand) {
				reward += state.tractor_cards_sum / 5;
			}
			if (! state.isOnSameTeam(myId, state.current_round_winning_id)) {
				reward = - reward;
				//reward = 0;
			}
			//if (last_hand) System.out.println("Last Hand! tractor_sum=" + state.tractor_cards_sum + "reward=" + reward);

			//Util.always("LEARNER" + myId, "winner=" + state.current_round_winning_id + " points=" + state.last_round_points + " reward=" + reward);
			learner.observeAfterStateReward(last_afterstate, reward);
			if (last_hand) {
				learner.gameOver();
			}
		}
	}

	public int EstimateAllowedPointsInTractorCards(int player_id, int num_tractor_cards) {
		// Calculate the average trump cards number.  
		// For now, using the scaling factor, say we allow 10 points in 8 cards, so that's 5% of points
		// per 8 cards, about 5/8% of points per tractor cards, rounded down.  And that's for if
		// we have exactly that number of tractor cards.  For the rest, let's use this function:
		// (our_number / average_number);
		int average_trump_number = numDecks * 18 / numPlayers;
		int our_trump_number = per_player_cards[player_id].GetSuit(trumpSuit).size();
		return (int) (((double) 5) / ((double) 8) * numDecks * num_tractor_cards * our_trump_number /
			average_trump_number); 
	}

	class SuitNumberPoints implements Comparable<SuitNumberPoints> {
		public int number_cards_in_suit;
		public int card_points_in_suit;
		public int suit;
		Vector<Card> cards;
		
		public SuitNumberPoints(int s) {
			suit = s;
			number_cards_in_suit = 0;
			card_points_in_suit = 0;
			cards = new Vector<Card>();
		}

		public int compareTo(SuitNumberPoints another) {
			// TODO: other tie breakers such as card property etc?
			if (number_cards_in_suit == another.number_cards_in_suit)
				return card_points_in_suit - another.card_points_in_suit;
			return number_cards_in_suit - another.number_cards_in_suit;
		}
	};
	
	private Vector<Card> FindRestTractorCards(SuitNumberPoints[] remaining_cards, int num_cards) {
		Vector<Card> cards_vector = new Vector<Card>();
		for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
			cards_vector.addAll(remaining_cards[i].cards);
		}
		return SelectCardTargetingPoints(cards_vector, num_cards, 0, false);
	}
	
	/**
	 * Give a group of players, give the probability that they can trump the card_property, given the
	 * current_winning_property (might already been trumped).  Note that players must be void of the suit
	 * before they can attempt to trump.  Here we rely only on cards played so far.  We should probably
	 * develop a function to calculate the probability that a particular player is void of the suit.  To
	 * make this more accurate.
	 * 
	 * @param current_winning_property
	 * @param card_property
	 * @param group
	 * @return
	 */
	private double GroupTrumpPropertiesProbability(SingletonCardProperty current_winning_property,
			CardProperty card_property, Vector<Integer> group) {
		if (card_property.suit == trumpSuit)
		 	return 0;  // Nobody can trump the trumpSuit.  People can only lead in trumpSuit.
		// multiplier will keep track of 
		double[] individual_trump_probability = new double[group.size()];
		// First figure out the probability based on purely the number of cards.
		for (int i = 0; i < individual_trump_probability.length; ++i) {
			// Need to be void in order to trump.
			individual_trump_probability[i] = PlayerVoidOfSuitProbability(card_property.suit, group.get(i));
		}
		boolean encountered_current_winning_property = false;
		Vector<Integer> one_player_group = new Vector<Integer>();
		one_player_group.add(-1);
		for (SingletonCardProperty p : card_property.properties) {
			// First figure that what is the property that we need to win as a trump.
			SingletonCardProperty trump_p = p.clone();
			trump_p.suit = trumpSuit;
			// This shouldn't have mattered, but just to be thorough.
			for (int i = 0; i < trump_p.card_suit.size(); ++i)
				trump_p.card_suit.set(i, trumpSuit);
			trump_p.leading_number = LowestLeadingNumberForProperty(trump_p);
			if (!encountered_current_winning_property && p.IsExactType(current_winning_property)) {
				encountered_current_winning_property = true;
				if (card_property.suit != current_winning_property.suit)
						trump_p = current_winning_property.clone();
				else
					trump_p.leading_number--;  // Just need to trump, the winning property is still of the non-trump-suit.
			} else {
				// This is not the winning_property, we only need the smallest trump
				// property that we need to beat it.
				trump_p.leading_number--;
			}
			for (int i = 0; i < group.size(); ++i) {
				if (individual_trump_probability[i] > 0) {
					one_player_group.set(0, group.get(i));
					individual_trump_probability[i] *= OthersBeatProperty(trump_p, one_player_group);
				}
			}
		}
		double group_cannot_trump_probability = 1;
		for (double p : individual_trump_probability)
			group_cannot_trump_probability *= 1 - p;
		return 1 - group_cannot_trump_probability;
	}
	
	private double LeadingPlayProbability(CardProperty lead_property, Vector<Integer> friends_group, Vector<Integer> foes_group,
			SingletonCardProperty current_winning_property) {
		if (lead_property.suit == trumpSuit) {
			if (lead_property.total_num_cards == rounds_remaining) {
				return 1;
			}
			return 0;  // Not the end of game, don't do the throw.
		}
		// If that's all we have left, why not?
		if (lead_property.total_num_cards == rounds_remaining)
			return 1;
		if (PlayersWithSuit(trumpSuit, foes_group).size() == 0)
			// If they are all out of trumps, then we are again safe.
			return 1;
		// We basically are checking for foe_players.  We basically want to calculate the average number of same suit
		// cards remaining for foe players, if they have the suit.
		// And also calculate the average trump cards per foe players, if they are void of the suit.
		if (current_winning_property == null)
			current_winning_property = super.GetWinningPropertyForCardProperty(lead_property);
		return 1 - GroupTrumpPropertiesProbability(current_winning_property, lead_property, foes_group);
	}

	// This assume that the lead play is already tested for winning probability of 1 in winning in the current suit.
	private double LeadingPlayProbability(Vector<Card> cards, Vector<Integer> friends_group, Vector<Integer> foes_group,
			SingletonCardProperty current_winning_property) {
		CardProperty lead_property = new CardProperty(cards.toArray(new Card[0]), trumpSuit, trumpNumber);
		return LeadingPlayProbability(lead_property, friends_group, foes_group, current_winning_property);
	}
	
	private Vector<Integer> GetRemainingFriendGroup(int player_id, Vector<Integer> dealer_group, int num_players_remaining) {
		return GetRemainingPlayersInGroup(player_id, dealer_group, num_players_remaining, true);
	}
	
	private Vector<Integer> GetRemainingFoeGroup(int player_id, Vector<Integer> dealer_group, int num_players_remaining) {
		return GetRemainingPlayersInGroup(player_id, dealer_group, num_players_remaining, false);
	}
	
	private Vector<Integer> GetRemainingPlayersInGroup(int player_id, Vector<Integer> group,
			int num_players_remaining, boolean same_group) {
		Vector<Integer> remainder = new Vector<Integer>();
		boolean me_in_group = group.contains(player_id) ^ !same_group;
		int index = player_id;
		for (int i = 0; i < num_players_remaining; ++i) {
			index = (index + 1) % numPlayers;
			if (me_in_group == group.contains(index))
				remainder.add(index);
		}
		return remainder;
	}
	
	/**
	 * Find all the properties with winning probability 1, for a particular player's particular suit.
	 * 
	 * @param suited_cards
	 * @param suit
	 * @return
	 */
	private Vector<Card> FindThrowCards(int player_id, int suit) {
		Vector<Card> winning_cards = new Vector<Card>();
		Vector<Card> suited_cards = per_player_cards[player_id].GetSuit(suit);
		if (suited_cards.size() == 0)
			return winning_cards;
		// Here is a situation that unknown_cards_pool couldn't handle.  We are not the dealer, and so
		// we don't get to see all the cards, ever, and so in that case, unknown_cards_pool will always think that
		// there are some cards (maybe even a high value) remaining in the suit.  We should check VOIDness of all other players.
		
		if (LonePlayerToHaveTheSuit(player_id, suit) == true) {
			winning_cards = (Vector<Card>) suited_cards.clone();
			return winning_cards;
		}
		// Normal situation, find all cards with probability 1 ba.
		int num_players_with_suit = PlayersWithSuit(suit, other_players).size();
		CardProperty property =  new CardProperty(suited_cards.toArray(new Card[0]), trumpSuit, trumpNumber);
		for (SingletonCardProperty p: property.properties) {
			if (unknown_cards_pool.CurrentPropertyProbability(p, num_players_with_suit, false) >= 1.0 ||
					(p.num_cards > 1 && OthersBeatProperty(p, other_players) == 0.0)) {
				winning_cards.addAll(p.ToCardVector());
			}
		}
		return winning_cards;
	}

	/**
	 * Based on per_player_cards[player_id] suggest tractor cards to be played 
	 * 
	 * @param player_id
	 * @param num_tractor_cards
	 * @return
	 */
	public Vector<Card> SuggestTractorCards(int player_id, int num_tractor_cards) {
		Vector<Card> tractor_cards = new Vector<Card>();
		// First take care of very rare case, we have so many trump cards.
		int total_non_trump_cards = 0;
		for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
			if (i == trumpSuit)
				continue;
			total_non_trump_cards += per_player_cards[player_id].GetSuit(i).size();
		}
		if (total_non_trump_cards <= num_tractor_cards) {
			for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
				if (i == trumpSuit)
					continue;
				tractor_cards.addAll(per_player_cards[player_id].GetSuit(i));
			}
			if (tractor_cards.size() < num_tractor_cards) {
				tractor_cards.addAll(
						SelectCardTargetingPoints(per_player_cards[player_id].GetSuit(trumpSuit),
								num_tractor_cards - tractor_cards.size(), 0, false));
			}
			return tractor_cards;
		}
		
		// Now the more common case, we'll simply select cards out of the none-trump suit, we have enough.
		SuitNumberPoints[] suit_number_points = new SuitNumberPoints[Card.SUIT_NUM_SUITS];
		SuitNumberPoints[] remaining_suit_number_points = new SuitNumberPoints[Card.SUIT_NUM_SUITS];
		double probability = high_winning_chance_for_tractor_cards;
		// We might have really good cards, so that if we keep all the cards with high_winning_chance_for_tractor_cards, 
		// then we won't have enough cards remaining.  And so here we'll increase the probability by 0.05 if we don't have enough
		// cards for tractor cards.
		do {
			total_non_trump_cards = 0;
			for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
				suit_number_points[i] = new SuitNumberPoints(i);
				remaining_suit_number_points[i] = new SuitNumberPoints(i);
				// Don't consider trump suit.
				if (i == trumpSuit) {
					continue;
				}
				CardProperty property = per_player_cards[player_id].GetSuitProperty(i);
				// Keep all cards with very high chance of winning.
				// For each suit, keep all the good cards, i.e., the ones with good probability of winning.
				// TODO: maybe consider secondary_properties as well?  Then we'll have to solve some matching problem
				// probably.
				for (SingletonCardProperty p : property.properties) {
					if (unknown_cards_pool.CurrentPropertyProbability(p, numPlayers - 1, false) 
							< probability) {
						suit_number_points[i].number_cards_in_suit += p.num_cards;
						suit_number_points[i].card_points_in_suit += p.TotalPoints();
						suit_number_points[i].cards.addAll(p.ToCardVector());
					} else {
						remaining_suit_number_points[i].number_cards_in_suit += p.num_cards;
						remaining_suit_number_points[i].card_points_in_suit += p.TotalPoints();
						remaining_suit_number_points[i].cards.addAll(p.ToCardVector());
					}
				}
				total_non_trump_cards += suit_number_points[i].number_cards_in_suit;
			}
			if (probability >= 1.0)
				break;
			probability = Math.min(1.0, probability + 0.05);
		} while (total_non_trump_cards < num_tractor_cards);
		
		// Another extreme corner case
		if (total_non_trump_cards < num_tractor_cards) {
			for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
				if (suit_number_points[i].number_cards_in_suit == 0)
					continue;
				tractor_cards.addAll(suit_number_points[i].cards);
			}
			tractor_cards.addAll(FindRestTractorCards(remaining_suit_number_points, num_tractor_cards - total_non_trump_cards));
			return tractor_cards;
		}
		
		// Sort them in increasing order of candidate_card_num_in_suit first.
		Arrays.sort(suit_number_points);
		int total_allowed_points = EstimateAllowedPointsInTractorCards(player_id, num_tractor_cards);
		// Greedy algorithm ba.  See if we can get rid of all useless cards in one suit.
		for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
			if (suit_number_points[i].card_points_in_suit <= total_allowed_points - GetTotalPoints(tractor_cards)) {
				if (suit_number_points[i].number_cards_in_suit <= num_tractor_cards - tractor_cards.size()) {
					tractor_cards.addAll(suit_number_points[i].cards);
					suit_number_points[i].cards.clear();  // Already considered this suit.
				} else {
					Vector<Card> adding_cards = SelectCardTargetingPoints(suit_number_points[i].cards, num_tractor_cards - tractor_cards.size(),
							total_allowed_points, false);
					tractor_cards.addAll(adding_cards);
					Card.DeleteCards(suit_number_points[i].cards, adding_cards);
					return tractor_cards;
				}	
			}
		}
		if (tractor_cards.size() < num_tractor_cards) {
			// Second pass, try to get rid of cards in the shorter suits without increase points.
			Arrays.sort(suit_number_points);
			for (int i = 0; i < Card.SUIT_NUM_SUITS && tractor_cards.size() < num_tractor_cards; ++i) {
				Vector<Card> adding_cards = SelectCardTargetingPoints(suit_number_points[i].cards, num_tractor_cards - tractor_cards.size(),
						total_allowed_points - GetTotalPoints(tractor_cards), true);
				tractor_cards.addAll(adding_cards);
				Card.DeleteCards(suit_number_points[i].cards, adding_cards);
			}
		}
		if (tractor_cards.size() < num_tractor_cards) {
			tractor_cards.addAll(FindRestTractorCards(suit_number_points, num_tractor_cards - tractor_cards.size()));
		}
		return tractor_cards;
	}
	
	/**
	 * Returns the players that have cards still in their suit, i.e., not VOID yet.
	 * 
	 * @param suit
	 * @param initial_player_pool
	 * @return sets of players still have the suit
	 */
	private Vector<Integer> PlayersWithSuit(int suit, Vector<Integer> initial_player_pool) {
		Vector<Integer> qualified_players = new Vector<Integer>();
		for (Integer i : initial_player_pool) {
			if (per_player_cards[i].SuitInfo(suit) != CardOrganizer.VOID)
				qualified_players.add(i);
		}
		return qualified_players;
	}

	
	/**
	 * Returns the players that do NOT have cards still in their suit, i.e., not VOID yet.
	 * 
	 * @param suit
	 * @param initial_player_pool
	 * @return sets of players still have the suit
	 */
	private Vector<Integer> PlayersWithoutSuit(int suit, Vector<Integer> initial_player_pool) {
		Vector<Integer> qualified_players = new Vector<Integer>();
		for (Integer i : initial_player_pool) {
			if (per_player_cards[i].SuitInfo(suit) == CardOrganizer.VOID)
				qualified_players.add(i);
		}
		return qualified_players;
	}
	
	private int LowestLeadingNumberForProperty(SingletonCardProperty property) {
		int bottom_number = unknown_cards_pool.GetLowestNumberForSuit(property.suit);
		int leading_number = bottom_number + property.num_sequences - 1;
		if (leading_number >= trumpNumber && bottom_number < trumpNumber)
			leading_number++;
		return leading_number;
	}
	
	/**
	 * assume that property is already winning in the current suit, find the minimum property needed to guarantee the win.
	 * i.e., if we have AK and both are biggest, and we know A is winning, we want to figure out that K is also winning, and we'll
	 * return Q as the ToBeat property.
	 * 
	 * @param property
	 * @param foes_group
	 * @return
	 */
	private SingletonCardProperty GetMinToBeatProperty(SingletonCardProperty property, Vector<Integer> foes_group) {
		SingletonCardProperty new_property = property.clone();
		while (OthersBeatProperty(new_property, foes_group) <= 0) {
			// Note, the card_suit field might not be accurate, because MINOR_TRUMP_NUMBER requires some suit change.
			// But this card_suit field is never really in use, which is OK.
			// It's not being used in IsPropertyAvailableInSuit, CurrentPropertyProbability,
			// And CompleteFollowCardSet.
			new_property.leading_number = CardTracker.GetNextLowerNumber(new_property.leading_number, trumpNumber);
			// leading_number might become invalid, need to explicitly check.
			int ending_number = new_property.leading_number - new_property.num_sequences;
			if (new_property.leading_number > trumpNumber &&
					ending_number < trumpNumber)
				ending_number--;
			if (ending_number < 0)
				break;
		}
		return new_property;
	}
	
	/**
	 * Return the probability that one of the people in foes_group will be able to beat property.  Note, this is
	 * based on the fact that cards are distributed among all possible players.  Right now we count the card as being
     * distributed among the rest of the players if property is simple.
	 * 
	 * @param property
	 * @param foes_group
	 * @return
	 */
	private double OthersBeatProperty(SingletonCardProperty property, Vector<Integer> foes_group) {
		double[] individual_beat_probability = new double[foes_group.size()];
		for (int i = 0; i < individual_beat_probability.length; ++i)
			individual_beat_probability[i] = PlayerHasCardsInSuitProbability(property.suit, property.num_cards, foes_group.get(i));
		int num_players_with_suit = PlayersWithSuit(property.suit, other_players).size();
		// num_players_with_suit is only useful when the property is complicated I think.  Try just use foes_group, instead of other_players.
		if (property.num_cards == 1 && num_players_with_suit > 0)
			num_players_with_suit = PlayersWithSuit(property.suit, foes_group).size();
		for (int i = 0; i < individual_beat_probability.length; ++i) {
			if (individual_beat_probability[i] > 0) {
				Vector<SingletonCardProperty> possible_properties = new Vector<SingletonCardProperty>();
				for (SingletonCardProperty p: per_player_cards[foes_group.get(i)].IsPropertyAvailableInSuit(property, property.suit))
					super.EliminateProperty(possible_properties, p);
				if (possible_properties.size() == 0) {
					individual_beat_probability[i] = 0;
				} else {
					individual_beat_probability[i] *= 1 - unknown_cards_pool.CurrentPropertyProbability(property, num_players_with_suit, true);
				}
			}
		}
		double group_not_beat_probability = 1;
		for (int i = 0; i < individual_beat_probability.length; ++i)
			group_not_beat_probability *= 1 - individual_beat_probability[i];
		return 1 - group_not_beat_probability;
		/* Sigh, still not ready.  We actually have a problem:
		 * Say I play AA, somebody followed by KQ, and we conclude that the follower doesn't have pairs, but possibly triples.
		 * However, they could have KKK to start with, and they have already played K, and so it's impossible for KKK to exist now...
		double probability = 1.0;
		for (SingletonCardProperty p : possible_properties) {
			p.leading_number = property.leading_number;
			p.suit = property.suit;
			int num_players = PlayersWithSuit(p.suit, all_players).size();
			// We want to calculate the probability that if a higher ordered probability exist such that they might beat
			// "property", this is equivalent as asking the probability of "property".
			probability *= (1 - unknown_cards_pool.CurrentPropertyProbability(p, num_players, num_players));
			if (probability == 0)
				return 0;
		}
		return probability;
		*/
	}

	private boolean LonePlayerToHaveTheSuit(int player_id, int suit) {
		for (int i = 0; i < numPlayers; ++i) {
			if (i == player_id) {
				if (i == myId) {
					// we have much better approximation
					if (per_player_cards[i].CardsInSuit(suit) == 0)
						return false;
				} else {
					if (per_player_cards[i].SuitInfo(suit) == CardOrganizer.VOID)
						return false;  // player_id doesn't have cards in that suit
				}
			} else {
				if (i == myId) {
					// we have much better approximation
					if (per_player_cards[i].CardsInSuit(suit) != 0)
						return false;
				} else {
					if (per_player_cards[i].SuitInfo(suit) != CardOrganizer.VOID)
						return false;
				}
			}
		}
		return true;
	}

	public Card[] SugguestLeadingCards(int player_id, Card[] current_hand, Vector<Integer> dealer_group) {
		Card[] cards = null;
		if (showAICandidates) {
			// See what learning algorithm says
			SugguestLeadingCardsLearning(player_id, current_hand, dealer_group);
		}
		switch(AIMode) {
		case AI_MODE_RULE_BASED:
			cards =  SugguestLeadingCardsRuleBased(player_id, current_hand, dealer_group);
			break;
		case AI_MODE_RANDOM:
			cards = SugguestLeadingCardsRandom(player_id, current_hand, dealer_group);
			break;
		case AI_MODE_LEARNING:
			cards = SugguestLeadingCardsLearning(player_id, current_hand, dealer_group);
			break;
		}
		if (learner != null) {
			last_afterstate = ComputeAfterState(player_id,
					cards, null, player_id, null, player_id, dealer_group);
		}
		if (checkCandidatesIncludeRuleBasedPlay) {
			Vector<Card[]> candidates = SugguestLeadingCardsCandidates(player_id, current_hand, dealer_group);
			if (!CandidatesContains(candidates, cards)) {
				Util.e("SuggestLeadingCards", "RULE_BASED played cards not in candidate set");
			}
		}
		return cards;

	}
	
	private boolean CandidatesContains(Vector<Card[]> candidates, Card[] cards) {
		PerDealCardComparator comp = new PerDealCardComparator(trumpSuit, trumpNumber);
		Arrays.sort(cards, comp);
		for (Card[] c : candidates) {
			Arrays.sort(c, comp);
			if (Arrays.equals(cards, c)) return true;
		}
		return false;
	}

	private SingletonCardProperty GetMaxPointProperty(Vector<Card> suited_cards) {
		CardProperty property = new CardProperty(suited_cards.toArray(new Card[0]), trumpSuit, trumpNumber);
		Collections.sort(property.properties, de_points_in_imp_comparator);
		return property.properties.get(0);
	}

	// a short version without current_hand, to make unit test happy
	public Card[] SugguestLeadingCardsRuleBased(int player_id, Vector<Integer> dealer_group) {
		Card[] current_hand = per_player_cards[player_id].getAllCards();
		return SugguestLeadingCardsRuleBased(player_id, current_hand, dealer_group);
	}

	public Card[] SugguestLeadingCardsRuleBased(int player_id, Card[] current_hand, Vector<Integer> dealer_group) {
		Vector<Integer> friends_group = GetRemainingFriendGroup(player_id, dealer_group, numPlayers - 1);
		Vector<Integer> foes_group = GetRemainingFoeGroup(player_id, dealer_group, numPlayers - 1);
		
		Vector<Card> play_cards = new Vector<Card>();
		double lead_throw_probability = -1;
		Vector<Card> best_throw_cards = new Vector<Card>();
		// Try to find some sure property to lead from non-trump suit first.
		for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
			play_cards = FindThrowCards(player_id, i);
			if (play_cards.size() > 0) {
				double new_probability = LeadingPlayProbability(play_cards, friends_group, foes_group, null);
				if (new_probability > lead_throw_probability) {
					// Can only play from one suit.
					lead_throw_probability = new_probability;
					best_throw_cards.clear();
					best_throw_cards.addAll(play_cards);
				}
			}
		}
		
		double void_suit_probability = -1;
		Card[] best_void_suit_single_card = new Card[1];
		best_void_suit_single_card[0] = null;
		SingletonCardProperty origin_property_of_single_card = null;
		int best_single_card_point = -1;
		for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
			if (i == trumpSuit || per_player_cards[player_id].CardsInSuit(i) == 0)
				continue;
			if (PlayersWithSuit(i, foes_group).size() == 0 &&
					PlayersWithSuit(trumpSuit, foes_group).size() == 0) {
				// hoho, this is a good scenario, foes are out of the suit, and out of the trumps.  We
				// have small cards in the suit.  In that case, we should just play that suit for sure.
				play_cards = SelectCardTargetingPoints(per_player_cards[player_id].GetSuit(i).toArray(new Card[0]),
						1, Integer.MAX_VALUE, false );
				return Card.VectorToArray(play_cards);
			}
			SingletonCardProperty property = GetMaxPointProperty(per_player_cards[player_id].GetSuit(i));
			Card single_card = property.MaxPointCard();
			int single_card_point = single_card.GetPoints();
			SingletonCardProperty single_card_property = new SingletonCardProperty(single_card, trumpSuit, trumpNumber);
			double probability = (1 - GroupTrumpPropertiesProbability(single_card_property,
					new CardProperty(trumpSuit, trumpNumber, single_card_property), foes_group))
				* GroupTrumpPropertiesProbability(single_card_property,
						new CardProperty(trumpSuit, trumpNumber, single_card_property), friends_group);
			if (best_void_suit_single_card[0] == null ||
				(probability >= void_suit_probability && best_single_card_point == 0 && single_card_point > 0) ||
				 (probability >= void_suit_probability && in_in_comparator.compare(property, origin_property_of_single_card) < 0)) {
				best_void_suit_single_card[0] = single_card;
				void_suit_probability = probability;
				origin_property_of_single_card = property;
				best_single_card_point = single_card_point;
			}
		}
		
		// We didn't find a good leading property, just play a single property of the most highest probability.
		// Excluding trump suit first.
		double non_trump_probability = -1.0;
		Vector<Card> best_non_trump_cards = new Vector<Card>();
		for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
			Vector<Card> cards = per_player_cards[player_id].GetSuit(i);
			if (i == trumpSuit)
				continue;
			if (cards.size() == 0)
				continue;
			CardProperty property = new CardProperty(cards.toArray(new Card[0]), trumpSuit, trumpNumber);
			for (SingletonCardProperty p: property.properties) {
				// LeadingPlayProbability already included the others trump probability!
				double current_probability = LeadingPlayProbability(p.ToCardVector(), friends_group, foes_group, p) *
					(1 - OthersBeatProperty(p, foes_group));
				if (current_probability == 0)
					continue;
				if (current_probability > non_trump_probability) {
					non_trump_probability = current_probability;
					best_non_trump_cards.clear();
					best_non_trump_cards.addAll(p.ToCardVector());
				}
			}
		}
		
		// We have collected all three possibilities. Let's try to play the one with the best probability.  Note, void_suit_probability
		// usually plays points, and so we should give some discount if a point card is played.
		if (best_single_card_point > 0)
			void_suit_probability -= 0.1;
		if (best_single_card_point > 5)
			void_suit_probability -= 0.1;
		if (lead_throw_probability >= void_suit_probability &&
			lead_throw_probability >= non_trump_probability &&
				lead_throw_probability >= high_winning_chance_for_leading_cards) {
			return best_throw_cards.toArray(new Card[0]);
		}
		if (non_trump_probability >= lead_throw_probability &&
				non_trump_probability >= void_suit_probability &&
				non_trump_probability >= high_winning_chance_for_leading_cards)
			return best_non_trump_cards.toArray(new Card[0]);
		if (void_suit_probability >= high_winning_chance_for_leading_cards) {
			return best_void_suit_single_card;
		}
		
		
		
		// Next try trump suit
		// TODO: trump suit is really tricky.  Ideally, we should only play pair with a purpose, and not
		// randomly throw away pairs.
		double trump_probability = -1.0;
		SingletonCardProperty best_trump_property = null;
		CardProperty trump_property =  new CardProperty(per_player_cards[player_id].GetSuit(trumpSuit).toArray(new Card[0]), trumpSuit, trumpNumber);
		// I'm debating how to sort the properties here.  If we have several winning ones, shall we play small? big?
		// I think that perhaps we'll play with the biggest.  That way, maximize partner's chance of adding points.  Otherwise, say
		// we have one big joker and a pair of small jokers.  We'll play big joker first, and then the pair of small joker is guaranteed
		// to be big...
		Collections.sort(trump_property.properties, de_leading_number_comparator);
		for (SingletonCardProperty p: trump_property.properties) {
			double current_probability = 1 - OthersBeatProperty(p, foes_group);
			if (current_probability > trump_probability) {
				trump_probability = current_probability;
				best_trump_property = p;
			}
			// TODO: add a bit more condition, like if we still have some non-trump cards, consider playing them etc.?
			if (trump_probability > 2 * non_trump_probability && trump_probability >= high_winning_chance_for_leading_cards &&
				PlayersWithSuit(trumpSuit, foes_group).size() > 0)  {  // If no foe has trump, don't lead just because it's high probability, it's really silly.
				return best_trump_property.ToCards();
			}
		}
		
		// Nothing is really good.  Try to play a small trump?  But if foes are out of trumps, no need.  Or if friends are
		// out of trumps, no need.
		if (trump_property.properties.size() > 0 &&
				PlayersWithSuit(trumpSuit, foes_group).size() > 0 &&
				PlayersWithSuit(trumpSuit, friends_group).size() > 0) {
			Collections.sort(trump_property.properties, in_points_in_imp_comparator);
			SingletonCardProperty small_trump = trump_property.properties.get(0);
			if (small_trump.TotalPoints() == 0)
				return small_trump.ToCards();
		}
		
		// Ok, we have exhausted all possible suits, and nothing is really good.
		// We can try to play something that is overall have none-zero chance of winning, but no points.
		/*
		double best_probability = 0;
		play_cards.clear();
		if (best_probability < lead_throw_probability && GetTotalPoints(best_throw_cards) == 0) {
			best_probability = lead_throw_probability;
			play_cards = best_throw_cards;
		}
		if (best_probability < void_suit_probability) {
			Vector<Card> void_suit_cards = SelectCardTargetingPoints(best_void_suit_property.ToCards(), 1, 0, false );
			if (GetTotalPoints(void_suit_cards) == 0) {
				best_probability = void_suit_probability;
				play_cards = void_suit_cards;
			}
		}
		if (best_probability < non_trump_probability && GetTotalPoints(best_non_trump_cards) == 0) {
			best_probability = non_trump_probability;
			play_cards = best_non_trump_cards;
		}
		if (best_probability > 0)
			return play_cards.toArray(new Card[0]);
			*/

		// Try to play from a not so bad suit with a small card, or better yet, clean up that suit?
		Vector<Integer> bad_suit = new Vector<Integer>();
		int candidate_suit_length = Integer.MAX_VALUE;
		SingletonCardProperty candidate_property = null;
		for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
			Vector<Card> cards = per_player_cards[player_id].GetSuit(i);
			if (i == trumpSuit)
				continue;
			if (cards.size() == 0) {
				bad_suit.add(i);
				continue;
			}
			if (PlayersWithSuit(i, foes_group).size() < foes_group.size() &&
				PlayersWithSuit(trumpSuit, PlayersWithoutSuit(i, foes_group)).size() > 0 &&
				PlayersWithSuit(i, friends_group).size() == friends_group.size()) {
				bad_suit.add(i);
				continue;
			}
			CardProperty property = new CardProperty(cards.toArray(new Card[0]), trumpSuit, trumpNumber);
			Collections.sort(property.properties, in_points_in_imp_comparator);
			SingletonCardProperty p = property.properties.get(0);
			int total_points = p.TotalPoints();
			if (total_points > 0)
				continue;
			if (candidate_property == null ||
				(candidate_property.num_cards != candidate_suit_length && property.properties.size() == 1) ||
				((candidate_property.num_cards != candidate_suit_length ||
				  property.properties.size() == 1) && property.total_num_cards < candidate_suit_length)) {
				candidate_property = p;
				candidate_suit_length = property.total_num_cards;
			}
		}
		if (candidate_property != null)
			return candidate_property.ToCards();

		// So it looks like we have tons of points, or there is not much option.  Just play a random small cards ba.
		// TODO: fix the condition that we only have points card remaining in the non-trump suit.  Play them first ba.
		// OK, so we'll just play a cards which is of non-trump suit if possible.  And highest importance.
		CardProperty final_property = new CardProperty(current_hand, trumpSuit, trumpNumber);
		Collections.sort(final_property.properties, in_imp_in_points_comparator);
		return final_property.properties.get(0).ToCards();
	}

	public Card[] SugguestLeadingCardsRandom(int player_id, Card[] current_hand, Vector<Integer> dealer_group) {
		Vector<Card[]> candidates = SugguestLeadingCardsCandidates(player_id, current_hand, dealer_group);
		return PickRandom(candidates);
	}
	
	public Card[] SugguestLeadingCardsLearning(int player_id, Card[] current_hand, Vector<Integer> dealer_group) {
		Vector<Card[]> candidates = SugguestLeadingCardsCandidates(player_id, current_hand, dealer_group);
		Vector<Vector<Integer>> afterStates = new Vector<Vector<Integer>>();
		for (Card[] play_cards : candidates) {
			Vector<Integer> as = ComputeAfterState(player_id, play_cards,
					null, player_id,
					null, player_id,
					dealer_group
					);
			afterStates.add(as);
		}
		return candidates.get(FindBestAfterState(afterStates));
		//return PickRandom(candidates);
	}

	// AZ: I think that we can replace part of this code (of finding throw cards, with the function: 
	//  FindThrowCards, which is more accurate (at least for 2 decks).
	public Vector<Card[]> SugguestLeadingCardsCandidates(
			int player_id, Card[] current_hand, Vector<Integer> dealer_group) {
		Vector<Card[]> candidates = new Vector<Card[]>();
		for (int i = 0; i < Card.SUIT_NUM_SUITS; ++i) {
			Vector<Card> cards = per_player_cards[player_id].GetSuit(i);
			if (cards.size() == 0)
				continue;
			// Consider throw cards and playing each singleton property 
			Vector<Card> throw_cards = new Vector<Card>();
			CardProperty property =  new CardProperty(cards.toArray(new Card[0]), trumpSuit, trumpNumber);
			for (SingletonCardProperty p: property.properties) {
				double prob_win = unknown_cards_pool.CurrentPropertyProbability(p, numPlayers - 1, false);
				if (prob_win == 1.0) {
					throw_cards.addAll(p.ToCardVector());
				} 
				candidates.add(p.ToCards());
			}
			if (throw_cards.size() > 0) {
				candidates.add(throw_cards.toArray(new Card[0]));
			}
		}
		return RemoveDuplicateCandidates(candidates);
	}

	public Card[] SuggestCardsToFollow(int player_id, Card[] lead_play, Card[] current_hand,
			SingletonCardProperty current_winning_property,
			boolean is_current_winner_my_team, int num_players_left, Vector<Integer> dealer_group) {
		Card[] cards = null;
		if (showAICandidates) {
			// See what learning algorithm says
			SuggestCardsToFollowLearning(player_id, lead_play, current_hand, current_winning_property, is_current_winner_my_team, num_players_left, dealer_group);
		}
		switch(AIMode) {
		case AI_MODE_RULE_BASED:
			cards = SuggestCardsToFollowRuleBased(player_id, lead_play, current_hand, current_winning_property, is_current_winner_my_team, num_players_left, dealer_group);
			break;
		case AI_MODE_RANDOM:
			cards = SuggestCardsToFollowRandom(player_id, lead_play, current_hand, current_winning_property, is_current_winner_my_team, num_players_left, dealer_group);
			break;
		case AI_MODE_LEARNING:
			cards = SuggestCardsToFollowLearning(player_id, lead_play, current_hand, current_winning_property, is_current_winner_my_team, num_players_left, dealer_group);
			break;
		}
		if (learner != null) {
			last_afterstate = ComputeAfterState(
					player_id, cards, lead_play, state.current_round_leader_id,
					current_winning_property, state.current_round_winning_id, 
					dealer_group);
		}
		if (checkCandidatesIncludeRuleBasedPlay) {
			Vector<Card[]> candidates = SuggestCardsToFollowCandidates(lead_play, current_hand, current_winning_property);
			if (!CandidatesContains(candidates, cards)) {
				Util.e("SuggestCardsToFollow", "RULE_BASED played cards not in candidate set");
			}
		}
		return cards;
	}
	
	/**
	 * Returns if of the lead_play's leading number is big enough to prevent point
	 * point cards from being played when others try to lead in the same suit.
	 * 
	 * @param winning_p the winning property for lead_play
	 * @param lead_play
	 * @param foes_group
	 * @return
	 */
	private boolean WinningPropertyPreventsPoints(SingletonCardProperty winning_p, CardProperty lead_play,
			Vector<Integer> foes_group) {
		if (lead_play.properties.size() > 1)  // lead trump through, definitely yes.  Nothing can beat that.
			return true;
		SingletonCardProperty p = GetMinToBeatProperty(winning_p, foes_group);
		return unknown_cards_pool.TotalPointsRemainingInSuit(lead_play.suit, Math.max(winning_p.leading_number, p.leading_number)) <= 0;
	}
		
	/**
	 * Find the cards to follow for player_id, based on the current lead_play and current_winning_property.  Also
	 * based on current_hand etc.
	 * Note that the follow play attribute should be one of the following three:
	 * forced_follow (same suit cards maybe with some additional cards)
	 * garbage_follow (no same suit cards, random cards)
	 * trump_follow (all trump suit cards, take the lead)
	 * lead_follow (same suit cards, take the lead)
	 * no_lead_follow (same suit cards, do not take the lead)
	 * We simply have to decide for each of the attribute, what would be the point attribute and 
	 * 
	 * @param player_id
	 * @param lead_play
	 * @param current_hand
	 * @param current_winning_property
	 * @param is_current_winner_my_team
	 * @param num_players_left
	 * @param dealer_group
	 * @return
	 */
	public Card[] SuggestCardsToFollowRuleBased(int player_id, Card[] lead_play, Card[] current_hand,
			SingletonCardProperty current_winning_property,
			boolean is_current_winner_my_team, int num_players_left, Vector<Integer> dealer_group) {
		// Util.e("Follow", "ruleBased" + Integer.toString(player_id));
		Vector<Integer> friends_group = GetRemainingFriendGroup(player_id, dealer_group, num_players_left - 1);
		Vector<Integer> foes_group = GetRemainingFoeGroup(player_id, dealer_group, num_players_left - 1);
		CardProperty lead_property = new CardProperty(lead_play, trumpSuit, trumpNumber);
		Vector<Integer>  foes_with_suit = PlayersWithSuit(lead_property.suit, foes_group);
		
		double already_won_prob = 0;
		if (is_current_winner_my_team) {
			if (foes_group.size() > 0) {  // we might have to lower the already_won_prob.
				if (lead_property.properties.size() > 1) {
					// A special case, if the current play is a throw, we want to say that it has probability of 1
					// of winning in the current suit. A good indicator of this situation is that the current_winning_property
					// has the same suit as the lead_property.
					already_won_prob = 1;
				} else {
					// We first calculate the probability that the hand will be good in the same suit.
					// Single probability is easy to calculate.
					if (foes_with_suit.size() > 0)
						already_won_prob = 1 - OthersBeatProperty(current_winning_property, foes_with_suit);
					else
						already_won_prob = 1;
				}
				// foes might trump though
				if (lead_property.suit != trumpSuit)
					already_won_prob *= 1 - GroupTrumpPropertiesProbability(current_winning_property, lead_property, foes_group);
			} else {
				already_won_prob = 1;
			}
		} else {
			if (friends_group.size() > 0 && lead_property.suit != trumpSuit)  {  // we might raise the already_won_prob
				already_won_prob = GroupTrumpPropertiesProbability(current_winning_property, lead_property, friends_group);
				if (already_won_prob > 0)  // What if others can trump as well?  be safe yeah.
					already_won_prob *= (1 - GroupTrumpPropertiesProbability(current_winning_property, lead_property, foes_group));
			}
		}
		// Util.e("Follow", "already_won_prob");	
		if (already_won_prob >= high_winning_chance_for_follow_with_points) {
			// If this is the case, we don't have to care about leading the current round, just try to play as many
			// cards as possible.
			Vector<FollowCardSet> sets= FollowCardSet.AllPossibleNonLeadingMaxPointSets();
			for (FollowCardSet s : sets) {
				CompleteFollowCardSet(lead_play, current_hand, current_winning_property, s);
				if (s.follow_cards != null)
					return s.follow_cards;
			}
		}
		
		// Util.e("Follow", "begin_lead_won_prob");	
		// We are not winning, we should think about taking the lead, or trumping.
		// First try to find out if we were to try to win the current round, what would be our lead.
		FollowCardSet lead_set = new FollowCardSet(FollowCardSet.MAX_POINTS, FollowCardSet.MAX_POSSIBLE,
				FollowCardSet.LEAD_FOLLOW);
		SingletonCardProperty new_lead_winning_property = CompleteFollowCardSet(lead_play, current_hand,
				current_winning_property, lead_set);
		
		double can_win_this_round = 0.0;
		if (new_lead_winning_property != null) {
			// We'll need to follow suit, check to see how good is the property
			if (foes_group.size() > 0) {
				can_win_this_round = 1 - OthersBeatProperty(new_lead_winning_property, foes_with_suit);
				SingletonCardProperty min_to_beat_property = null;
				if (can_win_this_round == 1) {
					min_to_beat_property = GetMinToBeatProperty(new_lead_winning_property, foes_group);
					if (min_to_beat_property.suit == current_winning_property.suit &&
							min_to_beat_property.leading_number < current_winning_property.leading_number) {
						// We have might hold lots of good cards, and if this is the case, we should still try to
						// beat the original property.
						min_to_beat_property = current_winning_property;
					}
				}
				// Also others might trump.
				if (lead_property.suit != trumpSuit)
					can_win_this_round *= 1 - GroupTrumpPropertiesProbability(current_winning_property, lead_property, foes_group);
				if (can_win_this_round >= high_winning_chance_for_follow_with_points) {
					// Let's lead this round
					if (min_to_beat_property != null) {
						lead_set.lead_magnitude_attribute = FollowCardSet.MIN_POSSIBLE;
						lead_set.point_attribute = FollowCardSet.MAX_POINTS;
						CompleteFollowCardSet(lead_play, current_hand, min_to_beat_property, lead_set);
						return lead_set.follow_cards;
					} else {
						lead_set.lead_magnitude_attribute = FollowCardSet.MAX_POSSIBLE;
						lead_set.point_attribute = FollowCardSet.MAX_POINTS;
						CompleteFollowCardSet(lead_play, current_hand, current_winning_property, lead_set);
						return lead_set.follow_cards;
					}
				} else {
					// First trump suit.  If our friends are winning and preventing point cards from playing, not over lead them.
					if (lead_property.suit == trumpSuit && is_current_winner_my_team && WinningPropertyPreventsPoints(current_winning_property, lead_property, foes_group)) {
						FollowCardSet no_lead_set = new FollowCardSet(FollowCardSet.MIN_POINTS, FollowCardSet.NO_MAGNITUDE_POSSIBLE,
								FollowCardSet.NO_LEAD_FOLLOW);
						CompleteFollowCardSet(lead_play, current_hand, current_winning_property, no_lead_set);
						return no_lead_set.follow_cards;
					}
					
					lead_set.point_attribute = FollowCardSet.MIN_POINTS;
					lead_set.lead_magnitude_attribute = FollowCardSet.NO_POINT_MAX_POSSIBLE;
					CompleteFollowCardSet(lead_play, current_hand, current_winning_property, lead_set);
					if (GetTotalPoints(lead_set.follow_cards) > 0) {
						// Sigh, The only way to lead is to play point cards.
						FollowCardSet no_lead_set = new FollowCardSet(FollowCardSet.MIN_POINTS, FollowCardSet.NO_MAGNITUDE_POSSIBLE,
								FollowCardSet.NO_LEAD_FOLLOW);
						CompleteFollowCardSet(lead_play, current_hand, current_winning_property, no_lead_set);
						if (GetTotalPoints(no_lead_set.follow_cards) < GetTotalPoints(lead_set.follow_cards) &&
							(can_win_this_round < low_winning_chance_for_no_points || is_current_winner_my_team || friends_group.size() > 0)) {
							return no_lead_set.follow_cards;
						}
					}
					return lead_set.follow_cards;
				}
			} else {
				// no foes left, can we can win this round, why not?
				// TODO: we might need to be tricky and say that if there is no point involved in this
				// round, we'll just be small.  Later ba.
				lead_set.lead_magnitude_attribute = FollowCardSet.MIN_POSSIBLE;
				lead_set.point_attribute = FollowCardSet.MAX_POINTS;
				CompleteFollowCardSet(lead_play, current_hand, current_winning_property, lead_set);
				return lead_set.follow_cards;
			}	
		}
		// Util.e("Follow", "lead_won_prob");	
		
		// The following two are no longer used.  Setting them to null to prevent accidental usage.
		lead_set = null;
		new_lead_winning_property = null;
		
		FollowCardSet trump_set = new FollowCardSet(FollowCardSet.MAX_POINTS, FollowCardSet.MAX_POSSIBLE,
				FollowCardSet.TRUMP_FOLLOW);
		SingletonCardProperty new_trump_winning_property = CompleteFollowCardSet(lead_play, current_hand,
				current_winning_property, trump_set);
		if (new_trump_winning_property != null) {
			if (foes_group.size() > 0) {
				// if new_trump_winning_property is not null, that means that we are definitely not following
				// the trumpSuit.  When playing trumpSuit, we won't be able to trump.
				can_win_this_round = 1 - GroupTrumpPropertiesProbability(current_winning_property, lead_property, foes_group);
				if (can_win_this_round >= high_winning_chance_for_follow_with_points) {
					// Let's do min trump with points.
					trump_set.lead_magnitude_attribute = FollowCardSet.MIN_POSSIBLE;
					trump_set.point_attribute = FollowCardSet.MAX_POINTS;
					CompleteFollowCardSet(lead_play, current_hand, current_winning_property, trump_set);
					return trump_set.follow_cards;
				} else {
					// We don't have good chance of winning.  Let's do max trump in any case.
					trump_set.lead_magnitude_attribute = FollowCardSet.NO_POINT_MAX_POSSIBLE;
					trump_set.point_attribute = FollowCardSet.MIN_POINTS;
					CompleteFollowCardSet(lead_play, current_hand, current_winning_property, trump_set);
					return trump_set.follow_cards;
				}
			} else {
				// no foes left, we can win this round, why not?
				// TODO: we might need to be tricky and say that if there is no point involved in this
				// round, we'll just be small.  Later ba.
				trump_set.lead_magnitude_attribute = FollowCardSet.MIN_POSSIBLE;
				trump_set.point_attribute = FollowCardSet.MAX_POINTS;
				CompleteFollowCardSet(lead_play, current_hand, current_winning_property, trump_set);
				return trump_set.follow_cards;
			}	
		}
		// Util.e("Follow", "trump_won_prob");	
		// If we reach here, we have decided that we simply can't win the current round.
		// Let's just follow with no points ba.
		Vector<FollowCardSet> sets= FollowCardSet.AllPossibleNonLeadingMinPointSets();
		for (FollowCardSet s : sets) {
			CompleteFollowCardSet(lead_play, current_hand, current_winning_property, s);
			if (s.follow_cards != null)
				return s.follow_cards;
		}
		return null;  // Should not happen!
	}
	
	/**
	 * Estimate for that a particular suit, if dealer has get rid of N
	 * cards of that suit.
	 * 
	 * @param suit
	 * @return
	 */
	private int EstimateNumMissingCardsForDealer(int suit) {
		// I don't think that the idea really works well.  For one thing, it seems that it makes dealer's partner
		// wrongly penalize some void suit of the dealer.  In the worst case, we'll be wrong only once.  So I think
		// for now, I'll leave it out.
		return 0;
		/*
		// The idea here is to see of the non-trump suit, where the tractor cards could go to.  Usually
		// we can maybe assume some even split between the suits.  But if we have all the information for one particular
		// suit, we can be more accurate.
		int total_non_trump_suit = 3;
		if (trumpSuit == Card.SUIT_NO_TRUMP)
			total_non_trump_suit++;
		int total_tractor_cards = state.num_tractor_cards;
		Vector<Integer> remaining_suits = new Vector<Integer>();
		Vector<Integer> remaining_cards = new Vector<Integer>();
		for (int i = 0; i < 4; ++i) {
			if (i == trumpSuit)
				continue;
			boolean has_accurate_information_in_suit = true;
			for (int j = 0; j < state.numPlayers; ++j) {
				if (j == myId)
					continue;
				if (per_player_cards[j].SuitInfo(suit) != CardOrganizer.VOID) {
					has_accurate_information_in_suit = false;
					break;
				}
			}
			if (has_accurate_information_in_suit) {
				if (i == suit)  // Cards are missing for sure.
					return unknown_cards_pool.RemainingCardsInSuit(i);
				total_non_trump_suit--;
				total_tractor_cards -= unknown_cards_pool.RemainingCardsInSuit(i);
			} else {
				remaining_suits.add(i);
				remaining_cards.add(unknown_cards_pool.RemainingCardsInSuit(i));
			}
		}
		double average = total_tractor_cards / total_non_trump_suit;
		int index = 0;
		while (index < remaining_suits.size()) {
			if (remaining_cards.get(index) < average) {
				if (remaining_suits.get(index) == suit)
					return remaining_cards.get(index);
				total_tractor_cards -= remaining_cards.get(index);
				total_non_trump_suit--;
				remaining_cards.remove(index);
				remaining_suits.remove(index);
			} else {
				index++;
			}
		}
		// For the remaining suit, we'll just do an even distribution, rounding up just to be safe.
		return (int)Math.ceil(total_tractor_cards / total_non_trump_suit);
		*/
	}
	
	/**
	 * Returns the probability that a particular player_id is void of that suit.
	 * @param suit
	 * @param player_id
	 * @return
	 */
	private double PlayerVoidOfSuitProbability(int suit, int player_id) {
		// If we know for sure that the player is void of the suit, say so.
		if (player_id == myId)
			return Math.max(1, per_player_cards[player_id].CardsInSuit(suit));
		if (per_player_cards[player_id].SuitInfo(suit) == CardOrganizer.VOID)
			return 1;
		int total_cards = unknown_cards_pool.RemainingCardsInSuit(suit);
		// TODO: give discount for dealer?  Especially if we haven't seen dealer play
		// cards in a particular suit?
		// So a simple approximation ba.  Ideally, here we should factor into the cards
		// that player_id has played in this suit.
		if (myId != state.dealer_index_) {
			// We are not the dealer, we don't have the most accurate information.
			total_cards -= EstimateNumMissingCardsForDealer(suit);
			
		}
		int total_players = PlayersWithSuit(suit, other_players).size();
		return Math.pow(1 - (double) 1 / total_players, total_cards);
	}
	
	/**
	 * Given a particular suit, and the player's id.  Returns the probability that they have at least
	 * that many number of cards in the suit.
	 * 
	 * 
	 * @param suit
	 * @param num_cards
	 * @param player_id
	 * @return
	 */
	private double PlayerHasCardsInSuitProbability(int suit, int num_cards, int player_id) {
		// If we already have accurate information that the player is void of the suit, say so.
		if (per_player_cards[player_id].SuitInfo(suit) == CardOrganizer.VOID)
			return 0;
		
		// addition records the probability that a particular user has exactly 0, 1, 2, --- num_cards - 1  cards.
		double addition = 0.0;
		// Do the simplest thing, calculate the probability that a particular player has at least
		// num_cards cards.  
		int total_players = PlayersWithSuit(suit, other_players).size();
		if (total_players == 0)
			return 0;
		int total_cards = unknown_cards_pool.RemainingCardsInSuit(suit);
		if (total_cards < num_cards)
			return 0;
		// Sigh, we have to do heavy binomial calculation, before I figure out a good approximation, let's do some optimization.
		// Term here will denote (total_cards choose denominator) * (1 / total_players) ^ denominator * ((total_players - 1) / total_players) ^ (total_cards - denominator)
		// when we decrease denominator by 1, term will only differ from the previous one by little.  We don't have to recalculate
		// from scratch.
		double term = Math.pow((double)((total_players - 1) / total_players), total_cards);
		double nominator = total_cards;
		final double additional_division = total_players - 1;
		addition += term;
		for (int denominator = 1; denominator < num_cards; ++denominator) {
			if (term > 0)
				term *= nominator / denominator / additional_division;
			else
				term = SingletonCardPropertyProbability.NChooseM(total_cards, denominator) * Math.pow((double)(1 / total_players), denominator) *
					Math.pow((double)((total_players - 1) / total_players), total_cards - denominator);
			addition += term;
			nominator--;
		}
		return 1 - addition;
	}

	// randomly play a move in suggested moves
	public Card[] SuggestCardsToFollowRandom(int player_id, Card[] lead_play, Card[] current_hand,
			SingletonCardProperty current_winning_property,
			boolean is_current_winner_my_team, int num_players_left, Vector<Integer> dealer_group) {
		Vector<Card[]> candidates = SuggestCardsToFollowCandidates(lead_play, current_hand, current_winning_property);
		return PickRandom(candidates);
	}
	
	private int ConvertToFindCoveringMode(FollowCardSet card_set) {
		int mode = VALIDATE_LEAD_MODE;
		if (card_set.lead_magnitude_attribute == FollowCardSet.MAX_POSSIBLE &&
				card_set.point_attribute == FollowCardSet.MAX_POINTS) {
			mode = VALIDATE_MAX_LEAD_POINT_MODE;
		}
		if (card_set.lead_magnitude_attribute == FollowCardSet.MAX_POSSIBLE &&
				card_set.point_attribute == FollowCardSet.MIN_POINTS) {
			mode = VALIDATE_MAX_LEAD_NO_POINT_MODE;
		}
		if (card_set.lead_magnitude_attribute == FollowCardSet.MIN_POSSIBLE &&
				card_set.point_attribute == FollowCardSet.MAX_POINTS) {
			mode = VALIDATE_MIN_LEAD_POINT_MODE;
		}
		if (card_set.lead_magnitude_attribute == FollowCardSet.MIN_POSSIBLE &&
				card_set.point_attribute == FollowCardSet.MIN_POINTS) {
			mode = VALIDATE_MIN_LEAD_NO_POINT_MODE;
		}
		if (card_set.lead_magnitude_attribute == FollowCardSet.NO_POINT_MAX_POSSIBLE) {
			mode = VALIDATE_NO_POINT_MAX_LEAD_MODE;
		}
		if (card_set.play_attribute == FollowCardSet.NO_LEAD_FOLLOW &&
				card_set.point_attribute == FollowCardSet.MAX_POINTS) {
			mode = VALIDATE_FOLLOW_POINT_MODE;
		}
		if (card_set.play_attribute == FollowCardSet.NO_LEAD_FOLLOW &&
				card_set.point_attribute == FollowCardSet.MIN_POINTS) {
			mode = VALIDATE_FOLLOW_NO_POINT_MODE;
		}	
		return mode;
	}
	
	/**
	 * Based on the three attribute fields in the card_set passed in, generate the necessary cards
	 * to fill the follow_card field.  If we can't satisfy all three attributes, follow_card field will
	 * be empty.
	 * 
	 * @param card_set
	 * @return a new winning property if we take the lead.    If card_set.lead_magnitude_attribute = MAX_POSSIBLE, then
	 * the max such property is return.  If card_set.lead_magnitude_attribute = MIN_POSSIBLE, then the smallest property
	 * that beats the current winning property is returned.  If card_set.lead_magnitude_attribute = NO_MAGNITUDE_POSSIBLE,
	 * then null is returned.
	 */
	public SingletonCardProperty CompleteFollowCardSet(Card[] lead_play, Card[] current_hand, SingletonCardProperty property,
			FollowCardSet card_set) {
		card_set.follow_cards = new Card[lead_play.length];
		boolean want_points = card_set.point_attribute == FollowCardSet.MAX_POINTS;
		SingletonCardProperty new_winning_property = null;
		
		CardProperty lead_property = null; 
		int suit = lead_play[0].CalculatePlaySuit(trumpSuit, trumpNumber);
		Arrays.sort(current_hand, new PerDealCardComparator(trumpSuit, trumpNumber));
		Card[] same_suit_cards = CardOrganizer.GetSameSuitCards(suit, current_hand, trumpSuit, trumpNumber);
		
		if (card_set.play_attribute == FollowCardSet.FORCED_FOLLOW
				&& same_suit_cards.length <= lead_play.length &&
			(same_suit_cards.length != 0 || suit == trumpSuit)) {
			card_set.follow_cards = new Card[lead_play.length];
			// Forced follow mode.  We have the same suit cards, but we are short in this suit.
			System.arraycopy(same_suit_cards, 0, card_set.follow_cards, 0, same_suit_cards.length);
			int still_need = lead_play.length - same_suit_cards.length;
			if (still_need > 0) {
				Card[] remaining_cards = Card.DeleteCards(current_hand, same_suit_cards);
				FillRestReturnPlay(remaining_cards, card_set.follow_cards, still_need, want_points);
			}
		} else if ((card_set.play_attribute == FollowCardSet.GARBAGE_FOLLOW ||
				card_set.play_attribute == FollowCardSet.TRUMP_FOLLOW) && same_suit_cards.length == 0) {
			// We are out of the suit, we should either garbage_follow, or trump.
			if (card_set.play_attribute == FollowCardSet.GARBAGE_FOLLOW) {
				FillRestReturnPlay(current_hand, card_set.follow_cards, card_set.follow_cards.length,
						want_points);
			} else if (card_set.play_attribute == FollowCardSet.TRUMP_FOLLOW){
				// Try to trump
				SingletonCardProperty to_beat = new SingletonCardProperty(trumpSuit, trumpNumber);
				to_beat.Copy(property);
				Card[] trump_cards = CardOrganizer.GetSameSuitCards(trumpSuit, current_hand, trumpSuit, trumpNumber);
				if (trump_cards.length < lead_play.length) {
					// don't have enough trump, can't trump.
					card_set.follow_cards = null;
					return null;
				} else {
					lead_property = new CardProperty(lead_play, trumpSuit, trumpNumber);
					Collections.sort(lead_property.properties, de_de_comparator);
					lead_property.properties.remove(0);  // MQC2AZ: why? The first property is covered in to_beat property.
					// The FindBestCovering considers to win against to_beat, and just cover the rest of the lead_property.properties.
					int mode = ConvertToFindCoveringMode(card_set);
					new_winning_property = FindBestCovering(to_beat, lead_property.properties,
							trump_cards, card_set.follow_cards, mode);
					if (new_winning_property == null) {
						// can't trump still.
						card_set.follow_cards = null;
						return null;
					}
				}
			}
		} else if ((card_set.play_attribute == FollowCardSet.NO_LEAD_FOLLOW || card_set.play_attribute == FollowCardSet.LEAD_FOLLOW)
				&& same_suit_cards.length > lead_play.length) {
		 	// follow suit mode, lead or no lead.
			SingletonCardProperty to_beat = new SingletonCardProperty(trumpSuit, trumpNumber);
			to_beat.Copy(property);
			lead_property = new CardProperty(lead_play, trumpSuit, trumpNumber);
			if (card_set.play_attribute == FollowCardSet.NO_LEAD_FOLLOW) {
				// This play can always exist.
				Vector<SingletonCardProperty> forced_properties =
					FindAllForcedProperties(lead_property.properties, same_suit_cards, null);
				int mode = ConvertToFindCoveringMode(card_set);
				int index  = 0;
				Card[] remaining_cards = same_suit_cards.clone();
				if (forced_properties.size() > 0) {
					for (SingletonCardProperty p : forced_properties)
						index += p.num_cards;
					Card[] forced_play = new Card[index];
					Vector<SingletonCardProperty> rest_forced_properties = (Vector<SingletonCardProperty>) forced_properties.clone();
					rest_forced_properties.remove(0);
					FindBestCovering(forced_properties.get(0), rest_forced_properties, same_suit_cards.clone(), forced_play, mode);
					Arrays.sort(forced_play, card_comparator);
					System.arraycopy(forced_play, 0, card_set.follow_cards, 0, index);
					remaining_cards = Card.DeleteCards(remaining_cards, forced_play);
				}
				if (index < card_set.follow_cards.length)
					FillRestReturnPlay(remaining_cards, card_set.follow_cards, card_set.follow_cards.length - index, want_points);
			} else if (card_set.play_attribute == FollowCardSet.LEAD_FOLLOW) {
				int mode = ConvertToFindCoveringMode(card_set);
				new_winning_property = FindBestCovering(to_beat, null, same_suit_cards, card_set.follow_cards, mode);
				if (new_winning_property == null) {
					card_set.follow_cards = null;
					return null;
				}
			}
		} else {
			// Everything else
			card_set.follow_cards = null;
			return null;
		}
		Arrays.sort(card_set.follow_cards, new PerDealCardComparator(trumpSuit, trumpNumber));
		for (Card c: card_set.follow_cards) {
			if (c==null) {
				Util.e("FindCardsToFollow", "NULL CARD " + Arrays.toString(card_set.follow_cards));
			}
		}
		return new_winning_property;
	}
	
	public int SuggestSuitToDeclare(Card[] cards, Vector<Integer> declarable_suits, int total_cards_per_player,
			int num_decks, int num_players, int trump_number, int declared_trump_suit) {
		switch(AIMode) {
			case AI_MODE_RULE_BASED:
				return SuggestSuitToDeclareRuleBased(cards, declarable_suits, total_cards_per_player, num_decks, num_players, trump_number, declared_trump_suit);
			case AI_MODE_RANDOM:
				return SuggestSuitToDeclareRandom(cards, declarable_suits, total_cards_per_player, num_decks, num_players, trump_number, declared_trump_suit);
			case AI_MODE_LEARNING:
				return SuggestSuitToDeclareRuleBased(cards, declarable_suits, total_cards_per_player, num_decks, num_players, trump_number, declared_trump_suit);
		}
		return Card.SUIT_UNDEFINED;
	}
	
	Random random_declaration = new Random();
	public int SuggestSuitToDeclareRandom(Card[] cards, Vector<Integer> declarable_suits, int total_cards_per_player,
			int num_decks, int num_players, int trump_number, int declared_trump_suit) {
		
		if (declarable_suits.size() > 0 && random_declaration.nextInt(TOTAL_FEN_QING_LEVEL) < FEN_QING_DEGREE) {
			Collections.shuffle(declarable_suits);
			return declarable_suits.get(0);
		}
		return Card.SUIT_UNDEFINED;
	}
	
	public int SuggestSuitToDeclareRuleBased(Card[] cards, Vector<Integer> declarable_suits, int total_cards_per_player,
			int num_decks, int num_players, int trump_number, int declared_trump_suit) {
		if (declarable_suits.size() == 0)  // shortcut.
			return Card.SUIT_UNDEFINED;
		// First consider suits in decreasing order of number of cards per suit.
		// Here we use NO_TRUMP as the trump suit on purpose, so we'll separate out the permanent trumps from the
		// rest.
		Card[] null_void_cards = Card.GetRidOfNullCards(cards);
		if (null_void_cards.length <= 0)  // shouldn't have happend because of the shortcut above, but just to be sure.
			return Card.SUIT_UNDEFINED;
		Vector<Card[]> same_suit_cards = CardOrganizer.SeparateIntoSameSuit(null_void_cards,
				Card.SUIT_NO_TRUMP, trump_number);
		double average_non_permanent_trump_cards = 0;
		int permanent_trump_cards = 0;
		for (Card c : null_void_cards) {
			if (c.CalculatePlaySuit(Card.SUIT_NO_TRUMP, trump_number) != Card.SUIT_NO_TRUMP)
				average_non_permanent_trump_cards += 0.25;  // hard coded that fact that we have 4 suits.
			else
				permanent_trump_cards++;
		}
		double exceeding_number =  (total_cards_per_player / null_void_cards.length - 1) * average_non_permanent_trump_cards * 4 / null_void_cards.length;
		Collections.sort(same_suit_cards, new Comparator<Card[]>(){

			public int compare(Card[] object1, Card[] object2) {
				// sort them in decreasing order of length
				return object2.length - object1.length;
			}});
		int declared_suit_length = 0;
		boolean suit_declared = declared_trump_suit != Card.SUIT_UNDEFINED;
		// Comfortable suit: have about 90% of the average trump, or no less than num_decks less than average.
		double comfortable_trump_number = 0.9 * (num_decks * 18 / num_players) * null_void_cards.length / total_cards_per_player;
		double comfortable_absolute_shortage = num_decks;
		for (Card[] suited_cards : same_suit_cards) {
			if (suited_cards.length == 0)
				continue;
			int suit = suited_cards[0].CalculatePlaySuit(Card.SUIT_NO_TRUMP, trump_number);
			if (suit == declared_trump_suit)
				declared_suit_length = suited_cards.length;
			if (!declarable_suits.contains(suit))
				continue;
			if (suited_cards.length < declared_suit_length)
				break;
			if (declared_trump_suit != Card.SUIT_UNDEFINED && declared_trump_suit != suit && 
				(suited_cards.length + permanent_trump_cards >= comfortable_trump_number ||
					suited_cards.length + comfortable_absolute_shortage >= average_non_permanent_trump_cards))
				return suit;  // we can improve comfortably, why not.  For solidify the same suit, we should use the next if.
			if (suited_cards.length >= average_non_permanent_trump_cards + exceeding_number)
				return suit;
		}
		// We don't have good suits to declare.  Should we declare something that is not too bad for us?
		// In case that we have a really really bad suit.  We require that we should have at least 60% of the trumps.
		// (That 60% already gives slight discount to the tractor cards).
		// Or in the case where we only received few cards, some hard coded number.
		double reasonable_trump_number = 0.6 * (num_decks * 18 / num_players) * null_void_cards.length / total_cards_per_player;
		double absolute_shortage = 2 * num_decks;
		boolean exist_bad_suit = false;
		for (Card[] suited_cards : same_suit_cards) {
			int suit = suited_cards[0].CalculatePlaySuit(Card.SUIT_NO_TRUMP, trump_number);
			if (suit == Card.SUIT_NO_TRUMP)
				continue;
			if (suited_cards.length + permanent_trump_cards < reasonable_trump_number &&
					suited_cards.length + absolute_shortage < average_non_permanent_trump_cards)
				exist_bad_suit = true;
		}
		if (suit_declared) {
			if (declared_suit_length + permanent_trump_cards < reasonable_trump_number &&
					declared_suit_length + absolute_shortage < average_non_permanent_trump_cards)
				exist_bad_suit = true;
		}
		if (exist_bad_suit) {
			for (Card[] suited_cards : same_suit_cards) {
				if (suited_cards.length == 0)
					continue;
				int suit = suited_cards[0].CalculatePlaySuit(Card.SUIT_NO_TRUMP, trump_number);
				if (!declarable_suits.contains(suit))
					continue;
				if (suited_cards.length < declared_suit_length)
					break;
				if (suited_cards.length + permanent_trump_cards >= comfortable_trump_number ||
						suited_cards.length + comfortable_absolute_shortage >= average_non_permanent_trump_cards)
					return suit;
			}
		}
		return Card.SUIT_UNDEFINED;
	}

	// Return a list of candidate plays to follow
	public Vector<Card[]> SuggestCardsToFollowCandidates(
			Card[] lead_play, Card[] current_hand, SingletonCardProperty property) {
		// First generate all possible FollowCardSet with different attributes.  Try all and return.
		Vector<FollowCardSet> sets = FollowCardSet.AllPossibleSets();
		Vector<Card[]> candidates = new Vector<Card[]>();
		for (FollowCardSet set: sets) {
			CompleteFollowCardSet(lead_play, current_hand, property, set);
			if (set.follow_cards != null)
				candidates.add(set.follow_cards);
		}
		//return candidates;
		return RemoveDuplicateCandidates(candidates);
		
	}

	private Card[] PickRandom(Vector<Card[]> candidates) {
		Card[] return_play = candidates.get(new Random().nextInt(candidates.size()));
		for (Card[] can : candidates) {
			//System.out.println("CANDIDATE\t" + Arrays.toString(can));
		}
		//System.out.println("CHOICE\t" + Arrays.toString(return_play));
		return return_play;
	}
	
	public Card[] SuggestCardsToFollowLearning(int player_id, Card[] lead_play, Card[] current_hand,
		SingletonCardProperty current_winning_property,
		boolean is_current_winner_my_team, int num_players_left, Vector<Integer> dealer_group) {
		Vector<Card[]> candidates = SuggestCardsToFollowCandidates(lead_play, current_hand, current_winning_property);
		Vector<Vector<Integer>> afterStates = new Vector<Vector<Integer>>();
		for (Card[] play_cards : candidates) {
			Vector<Integer> as = ComputeAfterState(player_id, play_cards,
					lead_play, state.current_round_leader_id,
					current_winning_property, state.current_round_winning_id,
					dealer_group
					);
			afterStates.add(as);
		}
		return candidates.get(FindBestAfterState(afterStates));
		//return PickRandom(candidates);
	}
	

	/**
	 * Returns a vector representing a (state, action) pair, aka "after-state"
	 * Because the whole (state, action) pair is too large/complex, this
	 * practically extracts a list of features based on the exact after-states
	 * so that machine learning can be used to learn from after-state to a value function.
	 * 
	 * @param player_id
	 * @param play_cards
	 * @param lead_play, null if play_cards is the lead play
	 * @param current_round_leader_id
	 * @param current_round_winning_property
	 * @param current_round_winning_id
	 * @param dealer_group
	 * @return a vector of features extracted from the after effect of playing  
	 */
	private Vector<Integer> ComputeAfterState(int player_id,
			Card[] play_cards, Card[] lead_play,
			int current_round_leader_id,
			SingletonCardProperty current_round_winning_property,
			int current_round_winning_id, Vector<Integer> dealer_group) {
		
		final boolean extended = false;
		Vector<Integer> vi = new Vector<Integer>();
		int suit;
		int next_winner_id;
		SingletonCardProperty next_winning_property;
		if (lead_play == null) {
			// mine is the lead play
			suit = play_cards[0].CalculatePlaySuit(trumpSuit, trumpNumber);
			next_winner_id = player_id;
			next_winning_property = GetWinningPropertyForPlay(play_cards);
		} else {
			suit = lead_play[0].CalculatePlaySuit(trumpSuit, trumpNumber);
			next_winning_property = IsWinningPlay(lead_play, play_cards, current_round_winning_property);
			if (next_winning_property == null) {
				next_winner_id = current_round_winning_id;
				next_winning_property = current_round_winning_property;
			} else {
				next_winner_id = player_id;
			}
		}

		int players_left = numPlayers - state.current_round_played_players - 1;
		// winning prob in the same suit:
		double winning_prob = 1;
		if (players_left > 0) 
			winning_prob = unknown_cards_pool.CurrentPropertyProbability(
				next_winning_property, numPlayers - 1, false);
		
		vi.add(dealer_group.contains(player_id) ? 1 : 0);  // am I in dealer group
		if (extended) vi.add(rounds_remaining - play_cards.length);  // rounds remaining
		vi.add(suit == trumpSuit ? 1 : 0); // playing trump or not
		vi.add(next_winning_property.suit == trumpSuit ? 1 : 0); // winning play is trump or not
		if (extended) vi.add(players_left); // players left
		if (extended) vi.add(next_winner_id == player_id ? 1: 0); // am I winning?
		vi.add(state.isOnSameTeam(next_winner_id, player_id) ? 1: 0); // is my team winning?
		
		int total_points = state.sumUpPointCards(play_cards);
		int[] num_remaining_players_void_in_suit = new int[2]; // friend and foe
		int[] num_remaining_players_void_and_can_trump = new int[2]; // friend and foe
		for (int i=0; i<numPlayers; i++) {
			int pid = (current_round_leader_id + i) % numPlayers;
			if (i < state.current_round_played_players) {
				// for played players, count points
				total_points += state.sumUpPointCards(state.per_round_played_cards.get(pid));
			} else {
				// for unplayed players, count whether void, and/or void-and-can-trump
				int team = state.isOnSameTeam(pid, player_id) ? 0 : 1; // friend or foe
				if (per_player_cards[pid].SuitInfo(suit) == CardOrganizer.VOID) {
					num_remaining_players_void_in_suit[team] ++;
					if (per_player_cards[pid].SuitInfo(trumpSuit) != CardOrganizer.VOID) {
						num_remaining_players_void_and_can_trump[team] ++;
					}
				}
			}
		}
		
		for (int i=0; i<2; i++) {
			if (extended) vi.add(num_remaining_players_void_in_suit[i]);
			vi.add(num_remaining_players_void_and_can_trump[i]);
		}
		
		// total points presented so far, including my play
		vi.add(total_points);
		
		int discrete_winning_prob = (int) (winning_prob * 5);
		
		if (extended) vi.add(next_winning_property.num_identical_cards);
		if (extended) vi.add(next_winning_property.num_sequences);
		if (extended) vi.add(discrete_winning_prob);  // discretise winning prob
		// TODO: compute winning prob taking into account trumping

		vi.add(state.isOnSameTeam(next_winner_id, player_id) ? discrete_winning_prob : 0);  // discretise winning prob
		vi.add(state.isOnSameTeam(next_winner_id, player_id) ? play_cards.length : 0);  // total played cards
		vi.add(state.isOnSameTeam(next_winner_id, player_id) ? next_winning_property.num_cards : 0);  // width of the leading property

		Util.i("player" + player_id + " thinks", 
				Arrays.toString(play_cards) + " -> " + vi.toString());

		return vi;
	}

	private Random random = new Random();
	private int FindBestAfterState(Vector<Vector<Integer>> afterStates) {
		if (random.nextFloat() < 0.00) {
			// epsilon-greedy
			return random.nextInt(afterStates.size());
		}
		double max = -Double.MAX_VALUE;
		Vector<Integer> maxIndex = new Vector<Integer>();
		for (int i=0; i<afterStates.size(); i++) {
			double value = EvaluateAfterState(afterStates.get(i));
			if (value > max) {
				max = value;
				maxIndex.clear();
				maxIndex.add(i);
			} else if (value == max) {
				maxIndex.add(i);
				
			}
		}
		// sample randomly if several state have same value
		return maxIndex.get(random.nextInt(maxIndex.size()));
	}


	private double EvaluateAfterState(Vector<Integer> afterstate) {
		// TODO plug into a ML model
		return learner.evaluateAfterState(afterstate);
		
		
		// return prob winning * points
		//int dim = afterstate.size();
		//return afterstate.get(dim-1) * afterstate.get(dim-2);
	}

	private Vector<Card[]> RemoveDuplicateCandidates(Vector<Card[]> candidates) {
		// remove dups
		Map<String, Card[]> unique_plays = new HashMap<String, Card[]>();
		for (Card[] cards : candidates) {
			Arrays.sort(cards, new PerDealCardComparator(trumpSuit, trumpNumber));
			String str = Arrays.toString(cards);
			unique_plays.put(str, cards);
		}
		return new Vector<Card[]>(unique_plays.values());
	}

	public void LastHandResult() {
		// Last hand ended, let learner observe final results.
		ObserveLastRoundRewards(true);
	}


}