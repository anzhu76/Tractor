/**
 * 
 */
package com.android.tractor;

import com.maqicheng.games.Game;
import com.maqicheng.games.GameParam;
import com.maqicheng.games.GamePlayer;
import com.maqicheng.games.GameState;

/**
 * @author Qicheng Ma
 *
 */
public class TractorGame extends Game {

	public static final int kMinTractorCards = 6;  // Oh the magic six.

	/**
	 * 
	 */
	public TractorGame() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.maqicheng.games.Game#createGamePlayer(com.maqicheng.games.GameParam)
	 */
	@Override
	public GamePlayer createGamePlayer(GameParam param) {
		// This is only called on the server side to create a mirror of real players
		// a very dummy GamePlayer will do. no need to keep state
		return new GamePlayer(param);
	}

	/* (non-Javadoc)
	 * @see com.maqicheng.games.Game#createGameState(com.maqicheng.games.GameParam)
	 */
	@Override
	public GameState createGameState(GameParam param) {
		// TODO Auto-generated method stub
		return new TractorGameState(param);
	}
	
}
