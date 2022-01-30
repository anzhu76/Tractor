package com.android.tractor;

import com.maqicheng.games.GameServer;

public class AITrainer {

	private static final String kTractorGame = "TractorGame";
	private static final String kGameId = "trainingGameId";
	private TractorGameServer server;
	private AIPlayer[] players;
	private TractorGameParam param;

	// For now just simulate an all-AI game. good for testing too. 
	public void train() {
		int num_players = 4;
		int num_decks = 2;
		
		server = new TractorGameServer();
		players = new AIPlayer[num_players];
		param = new TractorGameParam();
		param.setNumPlayers(num_players);
		param.setNumDecks(num_decks);
		param.setGameId(kGameId);

		Util.debug_level = Util.ERROR;
		//Util.debug_level = Util.WARN;  // only start/end game info + errors
		//Util.debug_level = Util.INFO;  // + game play info
		
		AILearner learner1 = new AILinearLearner(2);
		//AILearner learner1 = new AIMixGaussianLearner();
		//AILearner learner1 = new AIStatefulLearner();
		//AILearner learner2 = new AILinearLearner(1);
		AILearner learner2 = new AILearner();
		
		// Alternating learning and testing epoches

		//AILearner[] learners = {learner1};
		AILearner[] learners = {learner1, learner2};
		int[] aiModes = {AIDealAnalyzer.AI_MODE_LEARNING, AIDealAnalyzer.AI_MODE_RULE_BASED};
		
		// learing vs testing rounds
		//int[] schedule = {500, 1}; 
		int[] schedule = {100, 1000};
		
		for (int iter=1; iter<100; iter++) {
			for (AILearner l : learners) l.enableLearning(true);
			PlayNumDeals(num_players, schedule[0], learners, aiModes);
			for (AILearner l : learners) l.enableLearning(false);
			PlayNumDeals(num_players, schedule[1], learners, aiModes);
		}
		
		if (false) {
			Util.Sleep(30000);
			players[0].PlayerAction(TractorGameAction.ABANDON_CURRENT_GAME, null);
		}
		
	}
	
	private void PlayNumDeals(int num_players, int deals, AILearner[] learners,
			int[] aiModes) {
		for (int i=0; i<num_players; i++) {
			param.setPreferredPlayerId(i);
			players[i] = new AIPlayer(param, server, server);
			int assignedId;
			if (i==0) {
				assignedId = server.createNewGame(kTractorGame, kGameId, param);
			} else {
				assignedId = server.joinGame(kTractorGame, kGameId, param);
			}
			players[i] = new AIPlayer(param.setPlayerId(assignedId), server, server);
			players[i].analyzer.setAIMode(aiModes[i % aiModes.length]);
			players[i].analyzer.learner = learners[i % learners.length];
			// Plays exactly N deals and quit.
			players[i].setPlayNumDeals(deals);
		}
		for (AIPlayer p : players) {
			p.startPlayLoopInNewThreadDefault();
		}
		

		for (AIPlayer p : players) {
			p.waitUntilGameEnded();
			Util.Sleep(100);
		}
		
		Util.debug_level = Util.WARN;
		players[0].state.PrintStats();
		Util.debug_level = Util.ERROR;
	}

	public static void main(String[] argv) {
		AITrainer trainer = new AITrainer();
		trainer.train();
	}
}
