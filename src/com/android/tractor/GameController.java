/*
 * Copyright (C) 2008 An Zhu, Qicheng Ma.
 * 
 * Android specific.
 * 
 * Manages the actual game display and user interaction for Tractor.
 * Interacts with HumanPlayer through IHumanPlayerController.
 */

package com.android.tractor;


import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.maqicheng.games.GameServer;
import com.maqicheng.games.GameServerInterface;
import com.maqicheng.games.GameServerProxy;


public class GameController extends Activity implements IHumanPlayerController {
    private static final int NEW_GAME_ID = Menu.FIRST;
    private static final int CHECK_SCORE_ID = Menu.FIRST + 1;
    private static final int GAME_OPTION_ID = Menu.FIRST + 2;
    private static final int SAVE_GAME_ID = Menu.FIRST + 3;
    private static final int LOAD_GAME_ID = Menu.FIRST + 4;
    
    // Constants used for identify various game preferences.
    static public final String kNumberOfScores = "com.android.tractor.GameOptions.NumberOfScores";
    static public final String kOverallScore = "com.android.tractor.GameOptions.OverallScore";
    static public final String kDealerGroupSize = "com.android.tractor.GameOptions.DealerGroupSize";
	static public final String kDealerGroup = "com.android.tractor.GameOptions.DealerGroup";
	static public final String kDealer = "com.android.tractor.GameOptions.Dealer";
	static public final String kPreferredId = "com.android.tractor.GameOptions.PerferredId";
	static public final String kNumDecks = "com.android.tractor.GameOptions.NumDecks";
	public static final String PREFS_NAME = "com.android.tractor.GameControllerPreference";
	
	// We have 5 columns for card suits:
	// Permanent trumps
	// Spade
	// Heart
	// Club
	// Diamond
    public static final int NUM_DISPLAY_COL = 5;
    
    /*
     * The following constants define the various display mode for display updated information.
     * 
     * DEALING_MODE: When a particular new card is dealt.
     *               we may let user click on various suit to declare trump_suit.  But not the
     *               actual cards.
     * WAITING_MODE: When other player played some cards.  During this mode, there is not much
     *               a user can do except for watching.
     * TRACTOR_CARD_SWAPPING_MODE: When the player needs to select tractor cards to swap.
     *                             Cards can be selected.
     * FOLLOWING_MODE: When the player needs to follow some cards led by other players.
     * LEADING_MODE: When the player needs to lead with certain cards.
     * ENDING_DEAL_MODE: When a particular deal is over, we display game_stats_dialog.
     * ERROR_MESSAGE_MODE: When there is an error message, we should display error_message_dialog.
     * ABANDON_MODE: When current game should be abandoned, we'll exit this activity.
     * WAITING_FOR_GAME_START_MODE: When we are waiting for other players to join the game.
     * WAITING_FOR_PLAY_START_MODE: When the trump suit & dealer are decided after dealing is complete.
     *                              This mode will be display for instance, when we are waiting for the
     *                              dealer to swap tractor cards.
     * DEALING_MODE_DISPLAY_ONLY: When a particular trump suit has been declared, there is no new card
     *                            arriving.  This mode is only for leading the cards.
     */
    public static final int DEALING_MODE = 0;
    public static final int WAITING_MODE = 1;
    public static final int TRACTOR_CARD_SWAPPING_MODE = 2;
    public static final int FOLLOWING_MODE = 3;
    public static final int LEADING_MODE = 4;
    public static final int ENDING_DEAL_MODE = 5;
    public static final int ERROR_MESSAGE_MODE = 6;
    public static final int ABANDON_MODE = 7;
    public static final int WAITING_FOR_GAME_START_MODE = 8;
    public static final int WAITING_FOR_PLAY_START_MODE = 9;
    public static final int DEALING_MODE_DISPLAY_ONLY = 10;
    
    // Constants used for identify various game parameters passed in from the Tractor activity.
    public static final String NUM_PLAYERS = "num_players";
    public static final String NUM_DECKS = "num_decks";
	public static final String NUM_AIS = "num_ais";
    public static final String SERVER_ADDRESS = "server_address";
	public static final String IS_CREATOR = "create_game";
    public static final String GAME_ID = "game_id";
    public static final String PREFERRED_PLAYER_ID = "preferred_player_id";

    // Random constants used during game play.
    public static final String kLocalGameServerAddr = "localhost";
	public static final String kDefaultGameId = "defaultGameId";
	public static final String kDebugTag = "GameController";
	public static final String kGameType = TractorGame.class.getSimpleName();

    // During playing, we need to display cards played by other players.
    // We'll do it in the vertical fashion, i.e., each column represents one
    // Player.  Right now, we'll stick with 6.
    private static final int MAX_PLAYERS_PER_ROW = 6;
    
    // When a particular suit is declard with more than 1 card, we usually display the same card
    // multiple times.  But eventually we'll hit the limit which is the screen width.  Ideally,
    // we want to automatically identify what the max number of cards that we can display, before
    // we switch over to the format of a card and then times and then the number.  But now for, let's
    // just stick with the hard coded 3.
    private final int max_suit_declaration_cards = 3;
    
    // Result code to return to Tractor activity
    // RESULT_OK is defined in the base class already.
	public static final int RESULT_CANNOT_JOIN_GAME = 1;
	public static final int RESULT_CANNOT_CREATE_GAME = 2;
	public static final int RESULT_CANNOT_CONNECT_TO_SERVER = 3;
	public static final int RESULT_ABANDON_GAME = 4;
    
    // Variables related to display purposes.
	// TODO: provide comments for each variable ba.
    private ScrollView game_table;
    private ScrollView game_view;
    private Button game_action_button;
    private Button game_view_toggle_button;
    private LinearLayout[] card_suit_layout;
    private ImageView[] card_suit_image;
    private ImageView[] undeclarable_card_suit_image;
    private TextView declare_suit;
    private TextView declaration_text;
    private LinearLayout declaration_view;
    private LinearLayout hand_summary_view;
    private LinearLayout[] col_layout;
    private int display_mode;
	private int play_cards_remaining_;
	private TextView tractor_card_multiplier_view_;
	private TableLayout game_stats_view_;
	private LinearLayout tractor_cards_view_;
	private ScrollView gameStatsView;
	private ScrollView errorDisplayView;
	private ScrollView gameOptionView;
	private TextView error_message_text;
	private LinearLayout error_cards_view;
	private int normal_card_display_height = 0;
	private TextView own_player_view_text = null;
	private AlertDialog game_stats_dialog;
	private AlertDialog error_message_dialog;
	private AlertDialog new_game_dialog;
	private AlertDialog game_option_dialog;
	private int original_text_color;
	private float original_text_size;
    
    // Variables related to the actual playing logic
    private HumanPlayer human_player = null;
    private CardPlayer[] players;
    private int[] initial_scores = null;
    private int[] dealer_group = null;
    private int dealer_index = -1;
    private int preferred_player_id = 0;
    private int num_players = Tractor.DEFAULT_NUM_PLAYERS;
    private int num_decks = Tractor.DEFAULT_NUM_DECKS;
    
    /*
     * This lock is shared with HumanPlayer.  Our main thread does the
     * display stuff for HumanPlayer.  And that means that the actual play
     * operations should be in a separate thread.  And we want the display
     * action on GameController to be able to block game play operations, i.e.,
     * if we are displaying a newly dealt card to the user, the dealing
     * should be put on hold.  The way we achieve this is as follows:
     * 1. Server sends some update to the player.  Server waits for player's acknowledgment.
     * 2. Player upon receive the update, tries to acquire the game_lock_.  Once acquired,
     *    send some display request to GameController, but do not release game_lock_.
     * 3. GameController receives some display request, does the display (maybe
     *    wait for a bit etc.), and then release the lock.  And in times, send back the
     *    player's acknowledgment on HumanPlayer's behave.
     */
    private Semaphore game_lock_;

    private NotificationManager notification_manager_;
    private GameServer localServer = null;

    // Record when is the last time that a particular display happened.  This
    // variable is only useful when we are playing remotely.  In general, we want to
    // simulate the real dealing of cards, i.e., pause between two cards.  And when
    // playing remotely, instead of wait for certain time as in local play, we simply
    // require that certain time has elapsed since the last display time.
    private long lastDisplayTime;
    
    // When loading a local game, there is currently no way to tell the previous HumanPlayer to
    // "shut up" right away.  So, we are going to keep on receiving some update from it, and we'll
    // make sure to only display messages from the current HumanPlayer, namely, the one with
    // the latest GameId.  Remote play is not affected by this additional requirement.
    // TODO: to be thorough, probably want to add a stop_process_update function for CardPlayer, that
    // GameController can call on.  Right now, if other players are still playing, we'll still have
    // a few more updates to go through.
    private String lastGameId;
	
    /**
     * Updates a particular game_option_view, based on fields in GameOptions.
     * The ScrollView is displayed by either activity:
     * Tractor/GameController.  Each activity hold it's own ScrollView.
     * For this reason, game_option_view and related listeners are handled in static fashion.
     * 
     * @param game_option_view
     * @param context
     */
	public static void UpdateGameOptionView(ScrollView game_option_view, Context context) {
		Spinner speed_spinner = (Spinner) game_option_view.findViewById(R.id.animation_speed);
		CheckBox auto_selection_box = (CheckBox) game_option_view.findViewById(R.id.auto_select_cards);
		RadioGroup language_group = (RadioGroup) game_option_view.findViewById(R.id.language_choice);
		
		ArrayAdapter<CharSequence> speed_adapter = null;
		if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE) {
        	speed_adapter = ArrayAdapter.createFromResource(
        			context, R.array.animation_speed_english, android.R.layout.simple_spinner_item);
        	auto_selection_box.setText(R.string.auto_select_cards_english);
            language_group.check(R.id.english_language);
		}
        if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE) {
        	speed_adapter = ArrayAdapter.createFromResource(
        			context, R.array.animation_speed_chinese, android.R.layout.simple_spinner_item);
        	auto_selection_box.setText(R.string.auto_select_cards_chinese);
        	language_group.check(R.id.chinese_language);
        }
        speed_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		speed_spinner.setAdapter(speed_adapter);
        speed_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				GameOptions.animation_speed = arg2;
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}});
        auto_selection_box.setChecked(GameOptions.auto_select_cards);
		speed_spinner.setSelection(GameOptions.animation_speed);
		speed_adapter.notifyDataSetChanged();
	}
	
	// Language choice will trigger the text update of the game_option_view ScrollView.
	static class LanguageChangeListener implements android.widget.RadioGroup.OnCheckedChangeListener {
		ScrollView view_;
		Activity activity_;
		
		public LanguageChangeListener(ScrollView view, Activity activity) {
			view_ = view;
			activity_ = activity;
		}
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			switch (checkedId) {
			case R.id.english_language:
				GameOptions.display_language =GameOptions.ENGLISH_LANGUAGE;
				break;
			case R.id.chinese_language:
				GameOptions.display_language =GameOptions.CHINESE_LANGUAGE;
				break;
			}
			UpdateGameOptionView(view_, activity_);
			GameController controller = null;
			try {
				controller = (GameController) activity_;
			} catch (ClassCastException cce) {
				controller = null;
			}
			if (controller != null)
				controller.UpdateDisplayText();
			Tractor tractor = null;
			try {
				tractor = (Tractor) activity_;
			} catch (ClassCastException cce) {
				tractor = null;
			}	
			if (tractor != null)
				tractor.UpdateDisplayText();
		}
	}

	/**
	 * A static function that can be called by a particular activity to set up a game_option_view
	 * dialog
	 * 
	 * @param factory
	 * @param activity
	 * @return
	 */
	public static ScrollView SetUpGameOptionView(LayoutInflater factory, Activity activity) {
		ScrollView gameOptionView = (ScrollView) factory.inflate(R.layout.game_options, null);
		RadioGroup r1 = (RadioGroup) gameOptionView.findViewById(R.id.language_choice);
		r1.setOnCheckedChangeListener(new LanguageChangeListener(gameOptionView, activity));
		
		CheckBox c = (CheckBox) gameOptionView.findViewById(R.id.auto_select_cards);
		c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				GameOptions.auto_select_cards = arg1;
				GameOptions.auto_select_cards_changed = true;
			}});
		UpdateGameOptionView(gameOptionView, activity);
		return gameOptionView;
	}
	
	/**
	 * When user switches languages, we call this function to update all
	 * visible display text.  The other texts will be set correctly upon display later.
	 */
	public void UpdateDisplayText() {
		UpdateGameActionButtonText();
		UpdateDeclareSuitText();
		UpdateDeclarationText();
		Button b = (Button) findViewById(R.id.game_view_toggle_button);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE) {
			if (game_view.getVisibility() == View.VISIBLE)
				b.setText(R.string.collapse_english);
			else
				b.setText(R.string.expand_english);
		}
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE) {
			if (game_view.getVisibility() == View.VISIBLE)
				b.setText(R.string.collapse_chinese);
			else
				b.setText(R.string.expand_chinese);
		}
		if (own_player_view_text != null)
			own_player_view_text.setText(getDisplayId(human_player.state.playerId, human_player.state.playerId));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		UpdateDisplayText();
	}
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        game_lock_ = new Semaphore(1, true);
        // remove window title 
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        // remove status bar 
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN); 
       
        setContentView(R.layout.game_table);
        
        // Load GameOptions
        GameOptions.LoadGameOptions(this);
        
        // Check to see if we should be displaying the activity
        if (Tractor.gameControllerActivityInitiated)  // initiated via LaunchNewGame in Tractor activity
        	Tractor.gameControllerActivityInitiated = false;
        else
        	finish();  // current activity is not initiated the proper way, exit.
 
        notification_manager_ = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Set up a bunch of display related variables.  We mostly map the resource from xml to
        // local variables and these will be accessed later.
        game_table = (ScrollView) findViewById(R.id.game_table);
        card_suit_layout = new LinearLayout[NUM_DISPLAY_COL];
        card_suit_layout[0] = (LinearLayout) findViewById(R.id.trump_suit_col);
        card_suit_layout[1] = (LinearLayout) findViewById(R.id.suit1_col);
        card_suit_layout[2] = (LinearLayout) findViewById(R.id.suit2_col);
        card_suit_layout[3] = (LinearLayout) findViewById(R.id.suit3_col);
        card_suit_layout[4] = (LinearLayout) findViewById(R.id.suit4_col);

        card_suit_image = new ImageView[NUM_DISPLAY_COL];
        undeclarable_card_suit_image = new ImageView[NUM_DISPLAY_COL];
        card_suit_image[4] = (ImageView) findViewById(R.id.trump_suit_image);  	// no_trump
        card_suit_image[3] = (ImageView) findViewById(R.id.suit1_image);       	// spade
        card_suit_image[2] = (ImageView) findViewById(R.id.suit2_image);		// heart
        card_suit_image[1] = (ImageView) findViewById(R.id.suit3_image);		// club
        card_suit_image[0] = (ImageView) findViewById(R.id.suit4_image);		// diamond
        for (int i = 0; i < NUM_DISPLAY_COL; ++i) {
        	undeclarable_card_suit_image[i] = new ImageView(this);
        	undeclarable_card_suit_image[i].setAdjustViewBounds(true);
        }
        undeclarable_card_suit_image[3].setImageResource(R.drawable.spade_suit_undeclarable);
        undeclarable_card_suit_image[2].setImageResource(R.drawable.heart_suit_undeclarable);
        undeclarable_card_suit_image[1].setImageResource(R.drawable.club_suit_undeclarable);
        undeclarable_card_suit_image[0].setImageResource(R.drawable.diamond_suit_undeclarable);
        
        col_layout = new LinearLayout[NUM_DISPLAY_COL];
        col_layout[0] = (LinearLayout) findViewById(R.id.col0);
        col_layout[1] = (LinearLayout) findViewById(R.id.col1);
        col_layout[2] = (LinearLayout) findViewById(R.id.col2);
        col_layout[3] = (LinearLayout) findViewById(R.id.col3);
        col_layout[4] = (LinearLayout) findViewById(R.id.col4);
        
        game_view = (ScrollView) findViewById(R.id.game_view);
        game_action_button = (Button) findViewById(R.id.game_action_button);
        game_view_toggle_button = (Button) findViewById(R.id.game_view_toggle_button);
        game_view_toggle_button.setOnClickListener(game_view_toggle_listener);
        declare_suit = (TextView) findViewById(R.id.declare_suit);
        declaration_view = (LinearLayout) findViewById(R.id.declaration_view);
        declaration_text =  new TextView(this);
        new TextView(this);

        hand_summary_view = (LinearLayout) findViewById(R.id.hand_summary_view);
        
        display_mode = WAITING_FOR_GAME_START_MODE;
        
        LayoutInflater factory = LayoutInflater.from(this);
    	gameStatsView = (ScrollView) factory.inflate(R.layout.game_stats, null);
    	game_stats_view_ = (TableLayout) gameStatsView.findViewById(R.id.game_stats_view);
        tractor_cards_view_ = (LinearLayout) gameStatsView.findViewById(R.id.tractor_cards_view);
        tractor_card_multiplier_view_ = (TextView) gameStatsView.findViewById(R.id.tractor_cards_multiplier_view);
        
        gameOptionView = SetUpGameOptionView(factory, this);

        errorDisplayView = (ScrollView) factory.inflate(R.layout.error_display, null);
        error_message_text = (TextView) errorDisplayView.findViewById(R.id.error_message_text);
        error_cards_view = (LinearLayout) errorDisplayView.findViewById(R.id.error_cards_view);
        
        TextView dummy_view = new TextView(this);
        original_text_color = dummy_view.getTextColors().getDefaultColor();
        original_text_size = dummy_view.getTextSize();
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	// We need to grab the lock earlier, i.e., before the players are created
        	// and started their loop.  Otherwise, there is a deadlock situation:
        	// HumanPlayer receives an update from the server, before we can get to
        	// the display, HumanPlayer grab the lock.  And HumanPlayer is relying on
        	// GameController to release the lock.  But GameController in this specific call
        	// needs to grab the lock first.  And so it's a deadlock.
        	try {
				game_lock_.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			preferred_player_id = extras.getInt(GameController.PREFERRED_PLAYER_ID);
        	num_players = extras.getInt(GameController.NUM_PLAYERS);
        	num_decks = extras.getInt(GameController.NUM_DECKS);
        	int num_aiplayers = extras.getInt(GameController.NUM_AIS);
        	String serverAddress = extras.getString(GameController.SERVER_ADDRESS);
        	String game_id = extras.getString(GameController.GAME_ID);
        	boolean create_game = extras.getBoolean(GameController.IS_CREATOR);

        	// TODO: this logic is common for controllers, move to a base class?
        	int result = CreateOrJoinGame(
					num_aiplayers, serverAddress, game_id, create_game);
        	if (result != RESULT_OK) {
        		this.setResult(result);
        		this.finish();
        		return;
        	}
      	 
        	// Another option would be to set the initial value of the Semaphore
        	// to be zero, and don't acquire here, but I don't think doing so
        	// buys us anything.
        	lastDisplayTime = System.currentTimeMillis();
        	DisplayHand(display_mode);
        }
    }
    
    @Override
    protected void onStop(){
      super.onStop();
      
      // Save GameOptions
      GameOptions.SaveGameOptions(this);
    
      /* I haven't decided what should we do in the case of the android phone kills the app.  If there is anyway of
       * receiving that signal, then the sensible thing to do would be to store that information and then next time
       * onCreate, we should just exit, since there is no way of store half of the game.
       * For now, nothing will be saved, and next time, onCreate will start to deal from last saved progress.
       */
    }
    
    /**
     * Useful when we are loading a game, set the game state according to saved preferences.
     */
    private void SetGameState() {
    	// Restore saved game options
    	SharedPreferences settings = getSharedPreferences(GameController.PREFS_NAME, 0);
        num_players = settings.getInt(kNumberOfScores, Tractor.DEFAULT_NUM_PLAYERS);
        initial_scores = new int[num_players];
        for (int i = 0; i < num_players; ++i) {
        	initial_scores[i] = settings.getInt(kOverallScore + Integer.toString(i), TractorGameState.INITIAL_SCORE);
        }
        int dealer_group_size = settings.getInt(kDealerGroupSize, 0);  // by default, nobody is the dealer
        if (dealer_group_size <= 0 || dealer_group_size > num_players) {
        	dealer_group = null;
        } else {
        	dealer_group = new int[dealer_group_size];
        	for (int i = 0; i < dealer_group_size; ++i)
        		dealer_group[i] = settings.getInt(kDealerGroup + Integer.toString(i), i * 2);
        }
        dealer_index = settings.getInt(kDealer, -1);
        preferred_player_id = settings.getInt(kPreferredId, 0);
        num_decks = settings.getInt(kNumDecks, Tractor.DEFAULT_NUM_DECKS);
    }
    
    /**
     * If creating a new game, we need to clear some parameters.
     */
    private void ClearGameState() {
    	initial_scores = null;
    	dealer_group = null;
    	dealer_index = -1;
    }
    
    /**
     * Save all game related information that is needed by SetGameState.
     */
    private void SaveGameState() {
    	if (localServer == null)  // We only save game instances for local server.
    		return;
    	// Save user preferences. We need an Editor object to
        // make changes. All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(GameController.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(kNumberOfScores, human_player.state.numPlayers);
        for (int i = 0; i < human_player.state.numPlayers; ++i) {
      	  editor.putInt(kOverallScore + Integer.toString(i), human_player.state.cumulative_scores_[i]);
        }
        editor.putInt(kDealerGroupSize, human_player.state.dealerGroup.size());
        for (int i = 0; i < human_player.state.dealerGroup.size(); ++i) {
      	  editor.putInt(kDealerGroup + Integer.toString(i), human_player.state.dealerGroup.get(i));
        }
        editor.putInt(kDealer, human_player.state.dealer_index_);
        editor.putInt(kPreferredId, preferred_player_id);
        editor.putInt(kNumDecks, num_decks);
        // Don't forget to commit your edits!!!
        editor.commit();
    }

    /**
     * Create/Join/Load a game according to various parameters.
     * 
     * @param num_aiplayers
     * @param serverAddress
     * @param game_id
     * @param create_game Note: Join/Load doesn't count as create.
     * @return
     */
	protected int CreateOrJoinGame(int num_aiplayers, String serverAddress,
			String game_id, boolean create_game) {
		// For local play, we either create_game or load_game
		// For remote play, we either create_game or join_game
		// Load game is sort of like create_game in terms of actual execution.
		boolean isLocalPlay = serverAddress.equals(kLocalGameServerAddr);
		boolean load_game = isLocalPlay && !create_game;
		if (create_game)
			ClearGameState();
		if (load_game)
			SetGameState();
		// when playing locally all other players a AIs
		if (isLocalPlay) {
			num_aiplayers = num_players - 1;
			CardPlayer.kDelayBetweenUpdates = 100;
		} else {
			// to not flood network.
			CardPlayer.kDelayBetweenUpdates = 500;
		}
		
		if (!create_game && !load_game){
			// AI player should have been added by the creator.
			num_aiplayers = 0;
		}
		
		/* TODO: or replace with a remote server */ 
		GameServerInterface server = getGameServer(serverAddress);
		if (server == null) {
			return RESULT_CANNOT_CONNECT_TO_SERVER;
		}
		
		TractorGameParam param;
		
		if (create_game || load_game) {

			players = new CardPlayer[num_players];
			
			param = new TractorGameParam();
			param.setNumPlayers(num_players);
			param.setNumDecks(num_decks);
			if (isLocalPlay) {
				// During local play, especially if we are loading a game when another game
				// is being played, we want to avoid using the same game id. This is because
				// we are reusing the same server and there might still be updates from the
				// previous game in the update queue,
				// or arriving to the update queue, e.g., during dealing.
				// Adding a time string at the end solves the problem.
				game_id += Long.toString(System.nanoTime());
			}
			param.setGameId(game_id);
			param.setInitialScores(initial_scores);
			param.setDealerGroup(dealer_group);
			param.setDealer(dealer_index);

			param.setPreferredPlayerId(preferred_player_id);
			int assignedId = server.createNewGame(kGameType, game_id, param);
			if (assignedId < 0) {
				return RESULT_CANNOT_CREATE_GAME;
			}
			Util.v(kDebugTag, "assignedId = " + assignedId);
			human_player = new HumanPlayer(param.setPlayerId(assignedId), this, server, getGameServer(serverAddress));
			players[assignedId] = human_player;

		} else {
			
			param = (TractorGameParam) server.getGameParams(kGameType, game_id);
			if (param == null) {
				return RESULT_CANNOT_JOIN_GAME;
			}
			
			// Update various game parameters first.
			num_players = param.numPlayers;
			num_decks = param.num_decks;
			
			players = new CardPlayer[num_players];
			
			param.setPreferredPlayerId(preferred_player_id);
			int assignedId = server.joinGame(kGameType, game_id, param);
			if (assignedId < 0) {
				return RESULT_CANNOT_JOIN_GAME;
			}
			human_player = new HumanPlayer(param.setPlayerId(assignedId), this, server, getGameServer(serverAddress));
			players[assignedId] = human_player;
		}
		
		// AI players join the game
		for (int i = 1; i <= num_aiplayers; ++i) {
			param.setPreferredPlayerId(i);
			int assignedId = server.joinGame(kGameType, game_id, param);
			if (assignedId < 0 && create_game) {
				return RESULT_CANNOT_JOIN_GAME;
			}
			Util.v(kDebugTag, "assignedId = " + assignedId);
			players[assignedId] = new AIPlayer(param.setPlayerId(assignedId), 
					// Use a new GameServer(Proxy) instance to ensure parallel socket comm.
					getGameServer(serverAddress), getGameServer(serverAddress));
		}

		// everybody (that I controls) starts
		for (int i=0; i<num_players; i++) {
			if (players[i] != null) {
				players[i].startPlayLoopInNewThreadDefault();
			}
		}
		
		// Record game id, we'll only respond to requests with this id.
		lastGameId = game_id;
       	// Reset game_stats_view_.  This is because num_players might
		// have changed.
		while (game_stats_view_.getChildCount() > 2)
			game_stats_view_.removeViewAt(2);
    	for (int i = 0; i < num_players; ++i) {
    		TableRow new_row = new TableRow(this);
    		for (int j = 0; j < 3; j++) {
    			TextView new_text_view = new TextView(this);
    			new_text_view.setPadding(5, 5, 5, 5);
    			// new_text_view.setText("0");
    			new_row.addView(new_text_view);
    		}
    		game_stats_view_.addView(new_row);
    	}
		
		return RESULT_OK;
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (human_player != null)
    		human_player.PlayerAction(TractorGameAction.ABANDON_CURRENT_GAME, null);
    }
    
    private GameServerInterface getGameServer(String serverAddr) {
    	
    	if (serverAddr.equals(kLocalGameServerAddr)) {
        	// option one, direct testing with a local GameServer.
    		if (localServer == null) {
    			localServer = new GameServer();
    		}
    		localServer.registerGameType(TractorGame.class);
    		return localServer;

        	// option two, setup local game server to do socket comm.
        	/*
        	new Thread(
        	new Runnable() {
    			public void run() {
    				try {
    					GameServer.startServeSimple();
    				} catch (Exception e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    			}
    		}
        	, "GameServer").start();
        	Util.Sleep(500);  // wait for Server to start
    		return new GameServerProxy("localhost", 7788);
    		*/
    	} else {
    		// Connect to a remote game server
        	//String host = "192.168.8.11"; // replace with your PC's ip
    		try {
				return new GameServerProxy(serverAddr, 7788);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
    	}

	}

	public void onConfigurationChanged(Configuration newConfig) { 
    	super.onConfigurationChanged(newConfig);
    } 
    
	// Now a bunch of listeners.
    OnClickListener game_view_toggle_listener = new OnClickListener() {

		public void onClick(View v) {
			ToggleGameViewButton();
		}
    };
    
    private void ToggleGameViewButton() {
    	if (game_view.getVisibility() != View.VISIBLE) {
			game_view.setVisibility(View.VISIBLE);
			//game_view_toggle_button.setText(R.string.collapse_english);
			// FillGameView(display_mode);
		} else {
			game_view.setVisibility(View.GONE);
			//game_view_toggle_button.setText(R.string.expand_english);
		}
    	this.UpdateDisplayText();
    }
    
    OnClickListener click_card_listener = new OnClickListener() {

		public void onClick(View v) {
			// TODO Auto-generated method stub
			CardClicked(v);
		}	
	};
	
	/**
	 * When a card is long clicked, we consider that a signal from the user that he/she wants
	 * to un-select all the auto-selected cards.
	 */
	OnLongClickListener long_click_card_listener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			UnSelectAllCards();
			CardClicked(v);
			return true;
		}
		
	};
    
    OnClickListener play_cards_button_listener = new OnClickListener() {
        public void onClick(View v) {
            PlayCards();
        }
    };
    
    OnClickListener swap_cards_button_listener = new OnClickListener() {
        public void onClick(View v) {
            SwapCards();

        }
    };

    OnClickListener dealing_mode_listener = new OnClickListener() {
        public void onClick(View v) {
        	int id = v.getId();
        	int suit = Card.SUIT_UNDEFINED;
        	switch (id) {
        	case R.id.suit1_image:
        		suit = Card.SUIT_SPADE;
        		break;
        	case R.id.suit2_image:
        		suit = Card.SUIT_HEART;
        		break;
        	case R.id.suit3_image:
        		suit = Card.SUIT_CLUB;
        		break;
        	case R.id.suit4_image:
        		suit = Card.SUIT_DIAMOND;
        		break;
        	}
        	if (suit == Card.SUIT_UNDEFINED)
        		return;
        	human_player.PlayerAction(TractorGameAction.DECLARE_TRUMP_SUIT_ACTION, suit);
        }
    };
  
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(0, NEW_GAME_ID, 0, R.string.new_game_english);
        item.setIcon(android.R.drawable.ic_menu_revert);
        item = menu.add(0, CHECK_SCORE_ID, 0, R.string.check_score_english);
        item.setIcon(android.R.drawable.ic_menu_agenda);
        if (localServer != null) {
        	item = menu.add(0, SAVE_GAME_ID, 0, R.string.save_game_english);
            item.setIcon(android.R.drawable.ic_menu_save);
            item = menu.add(0, LOAD_GAME_ID, 0, R.string.load_game_english);
            item.setIcon(android.R.drawable.ic_menu_recent_history);
        }
        item = menu.add(0, GAME_OPTION_ID, 0, R.string.game_option);
        item.setIcon(android.R.drawable.ic_menu_preferences);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE) {
        	menu.findItem(NEW_GAME_ID).setTitle(R.string.new_game_english);
        	menu.findItem(CHECK_SCORE_ID).setTitle(R.string.check_score_english);
        	menu.findItem(GAME_OPTION_ID).setTitle(R.string.game_option);
        	if (localServer != null) {
        		menu.findItem(SAVE_GAME_ID).setTitle(R.string.save_game_english);
        		menu.findItem(LOAD_GAME_ID).setTitle(R.string.load_game_english);
        	}
        }
        if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE) {
        	menu.findItem(NEW_GAME_ID).setTitle(R.string.new_game_chinese);
        	menu.findItem(CHECK_SCORE_ID).setTitle(R.string.check_score_chinese);
        	menu.findItem(GAME_OPTION_ID).setTitle(R.string.game_option);
        	if (localServer != null) {
        		menu.findItem(SAVE_GAME_ID).setTitle(R.string.save_game_chinese);
        		menu.findItem(LOAD_GAME_ID).setTitle(R.string.load_game_chinese);
        	}
        }
        return true;
    }
    
	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
        	case NEW_GAME_ID:
        		setResult(RESULT_OK);
        		// ABANDON_GAME_ACTION will be called by onDestroy.
        		if (new_game_dialog == null) {
        			showDialog(NEW_GAME_ID);
        		} else {
        			UpdateExitDialog();
        			new_game_dialog.show();
        		}
        		return true;
        	case CHECK_SCORE_ID:
    			if (game_stats_dialog == null) {
    				showDialog(CHECK_SCORE_ID);
    			} else {
    				UpdateGameStatsDialog(game_stats_dialog);
    				game_stats_dialog.show();
    			}
        		return true;
        	case GAME_OPTION_ID:
        		GameOptions.auto_select_cards_changed = false;
        		showDialog(GAME_OPTION_ID);
        		return true;
        	case SAVE_GAME_ID:
        		SaveGameState();
        		return true;
        	case LOAD_GAME_ID:
        		if (human_player != null)
            		human_player.PlayerAction(TractorGameAction.ABANDON_CURRENT_GAME, null);
        		// num_ais should be a good number instead of zero.  However, CreateOrJoinGame updates
        		// this parameter automatically, so we are just passing in the dummy 0.
        		CreateOrJoinGame(0, GameController.kLocalGameServerAddr, GameController.kDefaultGameId, false);
        		return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	if (id == ERROR_MESSAGE_MODE) {
    		error_message_dialog = new AlertDialog.Builder(this)
            .setTitle("dummy")
            .setView(errorDisplayView)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.alert_dialog_ok_english, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
				}
                    })
            .create();
    		error_message_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				public void onDismiss(DialogInterface dialog) {
					// TODO Auto-generated method stub
					game_lock_.release();
				}
    			
    		}
    		);
    		return error_message_dialog;
    	}
    	if (id == NEW_GAME_ID) {
    		new_game_dialog = new AlertDialog.Builder(this)
            .setTitle("dummy")
            .setMessage("dummy")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.alert_dialog_cancel_english, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
             })
            .setNegativeButton(R.string.alert_dialog_ok_english, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
             })
            .create();
    		return new_game_dialog;
    	}
    	if (id == CHECK_SCORE_ID) {
    		game_stats_dialog = new AlertDialog.Builder(GameController.this)
    		.setTitle("Game Stats")  // this title will be overwritten.
    		.setView(gameStatsView)
    		.setIcon(android.R.drawable.ic_dialog_info)
    		.setPositiveButton(R.string.alert_dialog_ok_english, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .create();
    		game_stats_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					// TODO Auto-generated method stub
				 	if (human_player.current_deal_ended_)
                		human_player.PlayerAction(TractorGameAction.NEW_DEAL_ACTION, null);
				}
    			
    		});
    		return game_stats_dialog;
    	}
    	if (id == GAME_OPTION_ID) {
    		game_option_dialog = new AlertDialog.Builder(GameController.this)
    		.setTitle("Options/选项")
    		.setView(gameOptionView)
    		.setIcon(android.R.drawable.ic_dialog_info)
    		.setPositiveButton(R.string.alert_dialog_ok_english, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                   }
                })
                .create();
    		game_option_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				public void onDismiss(DialogInterface dialog) {
					// TODO Auto-generated method stub
					if (GameOptions.auto_select_cards_changed && GameOptions.auto_select_cards &&
							(display_mode == FOLLOWING_MODE || display_mode == TRACTOR_CARD_SWAPPING_MODE || display_mode == LEADING_MODE ) && human_player.pre_select_cards_ != null) {
						game_table.smoothScrollTo(0, PreSelectCards(human_player.pre_select_cards_));
					}
				}
    			
    		}
    		);
    		return game_option_dialog;
    	}
    	return null;
    }
    
    // A bunch of functions that does text/display updates etc. etc.
    private void UpdateExitDialog() {
    	String title = "";
		if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
			title = "Exiting Game...";
		if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
			title = "退出当前游戏...";
		String message = "";
		if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
			message = "Click OK to exit.  Click Cancel to return to the current game";
		if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
			message = "按下 OK 退出.  按下 Cancel 返回当前游戏.";
		new_game_dialog.setTitle(title);
		new_game_dialog.setMessage(message);
	}
    
    private void UpdateErrorDialog() {
    	String title = "";
		if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
			title = "Game Message";
		if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
			title = "游戏信息";
		error_message_dialog.setTitle(title);
    }
    
    private void FillTractorCardsView(Card[] cards, int num_cards, LinearLayout cards_view) {
    	// Ideally, we'll have to maybe scale the cards etc.  But I don't know
    	// a super clean way of getting the measured length etc.  So just
    	// some hard coded numbers here.
    	int num_cards_per_row = 4;
    	cards_view.removeAllViews();
    	LinearLayout card_holder = null;
    	for (int i = 0; i < num_cards; ++i) {
			if (i % num_cards_per_row == 0) {
				if (card_holder != null) {
					cards_view.addView(card_holder);
				}
				card_holder = new LinearLayout(this);
			}
			DisplayCard new_card = null;
			if (cards == null || cards[i] == null)
				new_card = new DisplayCard(this, Card.UNKNOWN_CARD);
			else 
				new_card = new DisplayCard(this, cards[i].GetIndex());
			new_card.setMaxHeight(DisplayCard.DISPLAY_HEIGHT);
			new_card.setMaxWidth(DisplayCard.DISPLAY_WIDTH);
			new_card.setAdjustViewBounds(true);
			card_holder.addView(new_card);
		}
    	if (card_holder != null)
    		cards_view.addView(card_holder);
    }
    
    /**
     * Depends on the player_index, return a display text.
     * Usually, we just convert the int to a string, but if it's the
     * HumanPlayer, we'll display "you!" in addition, :).
     * TODO: allow each player to input a nickname.
     * 
     * @param player_index
     * @param own_index
     * @return
     */
    static public String getDisplayId(int player_index, int own_index) {
    	String player_id = Integer.toString(player_index);
		if (player_index == own_index) {
			if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
				player_id += "(You)";
			if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
				player_id += "(您)";
		}
		return player_id;
    }
    
    private void UpdateGameScoreForWinnerGroup(int score_gained_for_dealer_group) {
    	for (int i = 2; i < game_stats_view_.getChildCount(); ++i) {
    		int player_index = i - 2;
    		TableRow row = (TableRow) game_stats_view_.getChildAt(i);
    		if ((score_gained_for_dealer_group <= 0 && !human_player.state.dealerGroup.contains(player_index)) ||
    				(score_gained_for_dealer_group > 0 && human_player.state.dealerGroup.contains(player_index))) {
    			int positive_score = score_gained_for_dealer_group;
    			if (positive_score < 0)
    				positive_score = -positive_score;
    			int previous_score = human_player.state.cumulative_scores_[player_index] - positive_score;
    			((TextView) row.getChildAt(1)).setText(
    					cumulativeScoreToString(previous_score) 
    					+ " + " + Integer.toString(positive_score));
    		}
    	}
    }
    
    /**
     * Add one * for each round of 2-A completed.
     * @param cumulative_score
     * @return
     */
    private String cumulativeScoreToString(int cumulative_score) {
    	String score_string = Card.NumberToString(TractorGameState.scoreFromCumulativeScore(cumulative_score));
    	for (int i = 0; i < TractorGameState.roundsFromCumulativeScore(cumulative_score); ++i)
    		score_string += "*";
    	return score_string;
    }
    
    private void UpdateGameStatsDialog(Dialog dialog) {
    	int none_dealer_group_sum = 0;
    	for (int i = 2; i < game_stats_view_.getChildCount(); ++i) {
    		int player_index = i - 2;
    		TableRow row = (TableRow) game_stats_view_.getChildAt(i);
    		String player_id = getDisplayId(player_index, human_player.state.playerId);
    		if (player_index == human_player.state.dealer_index_) {
    			if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
    				player_id += " - dealer";
    			if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
    				player_id += " - 庄家";
    		}
    		((TextView) row.getChildAt(0)).setText(player_id);
    		if (player_index == human_player.state.playerId)
    			((TextView) row.getChildAt(0)).setTypeface(Typeface.DEFAULT_BOLD);
    		// Note: dealerGroup might be slightly out of sync with the actual
    		// dealerGroup that should be displayed. But this is minor.
    		if (human_player.state.dealerGroup.contains(player_index)) {
    			row.setBackgroundColor(0x550000ff);
    		} else {
    			row.setBackgroundColor(Color.TRANSPARENT);
    			none_dealer_group_sum += human_player.state.currentDealScores[player_index];
    		}
    		if (human_player.state.dealer_index_ == player_index)
    			row.setBackgroundColor(0xff0000ff);
    		((TextView) row.getChildAt(1)).setText(cumulativeScoreToString(human_player.state.cumulative_scores_[player_index]));
    		String points_break_down = Integer.toString(human_player.state.currentDealScores[player_index]);
    		if (player_index == human_player.state.current_round_leader_id && human_player.current_deal_ended_) {
    			points_break_down += " + " + Integer.toString(human_player.state.tractor_cards_sum);
    			none_dealer_group_sum += human_player.state.tractor_cards_sum;
    		}
    		((TextView) row.getChildAt(2)).setText(points_break_down);
    			
    	}
    	if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE) {
    		game_stats_view_.getChildAt(0).setVisibility(View.VISIBLE);
    		game_stats_view_.getChildAt(1).setVisibility(View.GONE);
    	}
    	if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE) {
    		game_stats_view_.getChildAt(0).setVisibility(View.GONE);
    		game_stats_view_.getChildAt(1).setVisibility(View.VISIBLE);
    	}
    	Resources res = this.getResources();
    	String title = "";
    	if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
    		title = "Defender group score: ";
    	if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
    		title = "台下组得分: ";
    	title += Integer.toString(none_dealer_group_sum);
    	CharSequence tractor_cards_multiplication = "";
		if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
			tractor_cards_multiplication = res.getText(R.string.tractor_cards_multiplication_english);
		if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
			tractor_cards_multiplication = res.getText(R.string.tractor_cards_multiplication_chinese);
    	if (human_player.current_deal_ended_) {
    		UpdateGameScoreForWinnerGroup(human_player.state.dealer_group_rise_score);
    		if (human_player.state.dealer_group_rise_score > 0) {
    			if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
    	    		title = "Dealer group gains: " + Integer.toString(human_player.state.dealer_group_rise_score);
    			if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
    	    		title = "庄家组升" + Integer.toString(human_player.state.dealer_group_rise_score) + "级";
    		} else if (human_player.state.dealer_group_rise_score == 0) {
    			if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
    				title = "Defender group on deal";
    			if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
    	    		title = "台下组上庄";
    		} else {
    			if (GameOptions.display_language ==GameOptions.ENGLISH_LANGUAGE)
    				title = "Defender group gains: " + Integer.toString(-human_player.state.dealer_group_rise_score);
    			if (GameOptions.display_language ==GameOptions.CHINESE_LANGUAGE)
    	    		title = "台下组升" + Integer.toString(-human_player.state.dealer_group_rise_score)  + "级";
    		}
    		tractor_cards_multiplication = tractor_cards_multiplication + " "
    			+Integer.toString(human_player.state.multiplier);
    	} else {
    		tractor_cards_multiplication = tractor_cards_multiplication + " ?";
    	}
    	tractor_card_multiplier_view_.setText(tractor_cards_multiplication);
    	FillTractorCardsView(human_player.state.tractor_cards,
    			human_player.state.num_tractor_cards, tractor_cards_view_);
    	dialog.setTitle(title);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	super.onPrepareDialog(id, dialog);
    	if (dialog == error_message_dialog) {
    		UpdateErrorDialog();
    		error_message_text.setText(human_player.error_message);
    		if (human_player.error_cards != null) {
    			error_cards_view.setVisibility(View.VISIBLE);
    			FillTractorCardsView(human_player.error_cards, human_player.error_cards.length,
    					error_cards_view);
    		} else {
    			error_cards_view.setVisibility(View.GONE);
    		}
    	} else if (dialog == game_stats_dialog){ 
    		UpdateGameStatsDialog(dialog);
    	} else if (dialog == game_option_dialog) {
    		UpdateGameOptionView(gameOptionView, this);
    	} else if (dialog == new_game_dialog) {
    		UpdateExitDialog();
    	}
    }
    
    // Create our message handler.  This handler is exclusive being used by
    // the HumanPlayer, and we should release game_lock_ when done.
    Handler msgHandler = new Handler() { 
    	@Override 
    	/**
    	 * HumanPlayer always grabs the lock when requesting a display via message.  GameController always
    	 * release the lock upon handling the message.
    	 */
    	public void handleMessage(Message msg) {
    		if ((String)msg.obj != lastGameId) {  // Not a message from the last game, just ignore.
    			game_lock_.release();
    			return;
    		}
    		if (msg.arg1 == ENDING_DEAL_MODE) {
    			// End of deal.  We don't have to do extra display, just need to
    			// pop up the game stats dialog.  showDialog() doesn't work here?
    			// Really weird.  Looks like I can't be calling showDialog here,
    			// could it be that this is actually in a separate thread and the
    			// whole thing is not handled well by Android?  Explicitly maintaining
    			// a pointer of game_stats_dialog seems to solve the problem.
    			int total_score = human_player.per_deal_defender_group_score_;
    			if (!human_player.state.dealerGroup.contains(human_player.current_round_winning_id_))
    				total_score += human_player.state.tractor_cards_sum;
    			declare_suit.setText(Integer.toString(total_score));
    			if (game_stats_dialog == null) {
    				showDialog(CHECK_SCORE_ID);
    			} else {
    				UpdateGameStatsDialog(game_stats_dialog);
    				game_stats_dialog.show();
    			}
    			game_lock_.release();
    			return;
    		} else if (msg.arg1 == ABANDON_MODE) {
    			game_lock_.release();
    		  	// Remote server abandon game, should terminate and do the proper display.
    		  	// Local server abandon game, then we should automatically be going back,
    		  	// if user is exiting.  Or, we are loading a game, then we don't want to
    		  	// exit the current activity.  Either way, no need to finish.
    			if (localServer == null) {
    				setResult(RESULT_ABANDON_GAME);
    				finish();
    			}
    			return;
    		} else if (msg.arg1 == ERROR_MESSAGE_MODE) {
    			SendNotification(msg);
    			showDialog(ERROR_MESSAGE_MODE);
    			// game_lock_ will be released when the dialog is dismissed.
    			return;
    		}
    		DisplayHand(msg.arg1);
    	}
    };

    private void DisplayHand(int mode) {
    	// Log.v(this.kDebugTag, "Mode: " + Integer.toString(mode));
    	// Log.e("DISPLAY_MODE", Integer.toString(mode));
    	display_mode = mode;
    	if (mode == TRACTOR_CARD_SWAPPING_MODE) {
    		play_cards_remaining_ = human_player.state.num_tractor_cards;
    	}
    	if (mode == FOLLOWING_MODE) {
    		play_cards_remaining_ = human_player.current_round_num_cards_;
    	}
    	if (mode == LEADING_MODE) {
    		play_cards_remaining_ = 0;
    	}
    	
    	// This is used to decide how long we should wait for the user to respond
    	// Or rather, read the output before we move on to the next action.
    	// by default, things happen really fast.  But if there are interesting
    	// thing for the user to watch, this number will go up throughout the function.
    	int wait_time = 0;
    	wait_time += ArrangeGameTable(mode);
    	wait_time += FillCardView(mode);
    	wait_time += FillGameView(mode);
    	// Log.e("Children", Integer.toString(declaration_view.getChildCount()));
    	// Log.e("Children_height", Integer.toString(declaration_view.getChildAt(1).getHeight()));

		if (GameOptions.auto_select_cards && (mode == FOLLOWING_MODE || mode == TRACTOR_CARD_SWAPPING_MODE || mode == LEADING_MODE ) && human_player.pre_select_cards_ != null) {
			game_table.smoothScrollTo(0, PreSelectCards(human_player.pre_select_cards_));
		}
		long sleep_time = wait_time;
		if (mode == DEALING_MODE || mode == DEALING_MODE_DISPLAY_ONLY) {
			// during dealing mode, we only care that cards appear one by one.
			// If there is network delay, we don't have to wait for extra time.
			long currentDisplayTime = System.currentTimeMillis();
			sleep_time = wait_time - (currentDisplayTime - lastDisplayTime);
			if (sleep_time < 0)
				sleep_time = 0;
		}
		Runnable display_delay = new DisplayDelay(sleep_time, mode);
        Thread display_delay_thread = new Thread(display_delay);
        display_delay_thread.start();
    }
    
    private int CalculateWaitTimeMultiplier() {
		switch (GameOptions.animation_speed) {
		case GameOptions.ANIMATION_SPEED_FAST:
			return 1;
		case GameOptions.ANIMATION_SPEED_MEDIUM:
			return 2;
		case GameOptions.ANIMATION_SPEED_SLOW:
			return 3;
		}
		return 1;
	}
    	
    // This thread is only used to make sure that once we display something
    // to the user, we wait a bit, to avoid display changing too rapidly.
	class DisplayDelay implements Runnable {
		long wait_time_;
		int mode_;
		DisplayDelay(long wait_time, int mode) {
			wait_time_ = wait_time;
			mode_ = mode;
		}
		public void run() {
			try {
				Thread.sleep(wait_time_ * CalculateWaitTimeMultiplier());
				if (mode_ == DEALING_MODE || mode_ == WAITING_FOR_PLAY_START_MODE) {
					human_player.PlayerAction(TractorGameAction.READY_FOR_NEXT_CARD_ACTION, null);
				}
				lastDisplayTime = System.currentTimeMillis();
				game_lock_.release();
			} catch (InterruptedException exception) {
				
			}
		}
	};

	private int ArrangeGameTable(int mode) {
		int wait_time = 0;
    	if (mode == DEALING_MODE || mode == DEALING_MODE_DISPLAY_ONLY || mode == TRACTOR_CARD_SWAPPING_MODE || mode == WAITING_FOR_GAME_START_MODE ||
    			mode == WAITING_FOR_PLAY_START_MODE) {
    		game_view_toggle_button.setVisibility(View.GONE);
    		game_view.setVisibility(View.GONE);
    		declare_suit.setVisibility(View.VISIBLE);
    		declare_suit.setGravity(Gravity.CENTER_VERTICAL);
    		declare_suit.setTextColor(original_text_color);
    		declare_suit.setTextSize(original_text_size);
    		declare_suit.setMinHeight(game_action_button.getHeight());
    		declaration_view.setVisibility(View.VISIBLE);
    		declaration_view.removeAllViews();
    		// Setup default declaration view message.  The actual content
    		// is going to be filled during the FillGameView routine, which
    		// is a but messy.
    		declaration_text.setMinHeight(DisplayCard.DISPLAY_HEIGHT);
		    declaration_text.setMaxHeight(DisplayCard.DISPLAY_HEIGHT);
		    declaration_text.setGravity(Gravity.CENTER_VERTICAL);
    		declaration_view.addView(declaration_text);
    		declaration_view.addView(new DisplayCard(this, Card.BLANK_CARD));
    		// now fill in the text for declare_suit.
    		if (mode == TRACTOR_CARD_SWAPPING_MODE) {
    			game_action_button.setWidth(120);
    			game_action_button.setVisibility(View.VISIBLE);
    		} else if (mode == DEALING_MODE || mode == DEALING_MODE_DISPLAY_ONLY){
    			game_action_button.setVisibility(View.INVISIBLE);
    		} else if (mode == WAITING_FOR_GAME_START_MODE) {
    			game_action_button.setVisibility(View.INVISIBLE);
    		} else if (mode == WAITING_FOR_PLAY_START_MODE) {
    			game_action_button.setVisibility(View.INVISIBLE);
    			// Let people know that we are about to start the game, and display this information.
				// For dealer, we simply have 0 wait time and this screen is skipped asap.
    			if (human_player.state.dealer_index_ != human_player.state.playerId)
    				wait_time += 2000 / CalculateWaitTimeMultiplier();
    		}
    	} else {
    		game_action_button.setWidth(120);
    		game_action_button.setVisibility(View.VISIBLE);

    		game_view_toggle_button.setVisibility(View.VISIBLE);   		
    		declare_suit.setVisibility(View.VISIBLE);
    		declare_suit.setText(Integer.toString(human_player.per_deal_defender_group_score_));
    		declare_suit.setTextColor(Color.MAGENTA);
    		declare_suit.setGravity(Gravity.CENTER);
    		declare_suit.setTextSize(30);
    		declare_suit.setMinHeight(game_action_button.getHeight());
    		declaration_view.setVisibility(View.GONE);
    	}
    	if (mode == WAITING_MODE || mode == FOLLOWING_MODE) {
    		if (game_view.getVisibility() != View.VISIBLE) {
    			ToggleGameViewButton();
    		}
    	} else if (mode == LEADING_MODE) {
    		if (game_view.getVisibility() != View.GONE) {
    			ToggleGameViewButton();
    		}
    	}
    	if (mode == WAITING_MODE)  // If we are just watch the game being played, let's not mess with the game view.
    		game_view_toggle_button.setClickable(false);
    	else
    		game_view_toggle_button.setClickable(true);

    	if (mode == FOLLOWING_MODE || mode == LEADING_MODE) {
			game_action_button.setClickable(true);
			game_action_button.setOnClickListener(play_cards_button_listener);
		} else if (mode == TRACTOR_CARD_SWAPPING_MODE) {
			game_action_button.setClickable(true);
			declare_suit.setGravity(Gravity.CENTER_VERTICAL);
			game_action_button.setOnClickListener(swap_cards_button_listener);
		} else {
			game_action_button.setClickable(false);
		}
    	
    	if	(human_player.suit_declarer_id != -1 ||
    			human_player.state.trump_suit != Card.SUIT_UNDEFINED) {
    		// Ah, TextView.  This sucks.  I spent almost an hour debugging, and
    		// then only to realized that by default, TextView stretched the
    		// entire width.  So unless I'm adding the view with WRAP_CONTENT option,
    		// the view afterwards (which is the image) will never appear on the screen.
    		LinearLayout.LayoutParams pp =
    			new LinearLayout.LayoutParams(
    					LinearLayout.LayoutParams.WRAP_CONTENT,
    					LinearLayout.LayoutParams.WRAP_CONTENT);
    		declaration_text.setGravity(Gravity.CENTER);
    		declaration_view.removeAllViews();
    		declaration_text.setMinHeight(DisplayCard.DISPLAY_HEIGHT);
		    declaration_text.setMaxHeight(DisplayCard.DISPLAY_HEIGHT);
		    declaration_view.addView(declaration_text, pp);
		    // Log.v("NUMBER OF SUITS", Integer.toString(human_player.num_declared_suit_cards_));
		    int num_suits_to_display = Math.max(1, human_player.num_declared_suit_cards);
		    int declared_suit = human_player.declared_trump_suit;
		    if (declared_suit == Card.SUIT_UNDEFINED)
		    	declared_suit = human_player.state.trump_suit;
		    if (human_player.num_declared_suit_cards > max_suit_declaration_cards) {
		    	num_suits_to_display = 1;
		    }
    		for (int i = 0; i < num_suits_to_display; ++i) {
    			DisplayCard c =
    				new DisplayCard(this, Card.ConvertToIndex(declared_suit, human_player.state.trump_number));
    			c.setMaxHeight(DisplayCard.DISPLAY_HEIGHT);
    			c.setAdjustViewBounds(true);
    			declaration_view.addView(c, pp);
    		}
    		if (human_player.num_declared_suit_cards > max_suit_declaration_cards) {
    			TextView brief_text = new TextView(this);
    			brief_text.setGravity(Gravity.CENTER);
    			brief_text.setText(" × " + Integer.toString(human_player.num_declared_suit_cards));
    			brief_text.setMinHeight(DisplayCard.DISPLAY_HEIGHT);
    			brief_text.setMaxHeight(DisplayCard.DISPLAY_HEIGHT);
			    declaration_view.addView(brief_text, pp);
    		}
    	}
    	UpdateGameActionButton();
		UpdateDisplayText();  // Fill in all the text here.
		
		if (mode == LEADING_MODE || (mode == WAITING_MODE && human_player.current_round_played_players_ == 1))
			game_table.smoothScrollTo(0, 0);
	
		if (mode == DEALING_MODE_DISPLAY_ONLY)
			wait_time += 500 / CalculateWaitTimeMultiplier();  // We must have something to show to the user, pause a bit.
		return wait_time;
	}
	
	private void UpdateGameActionButton() {
		if (human_player == null)
			return;  // nothing has started yet.
		if (display_mode == FOLLOWING_MODE || display_mode == TRACTOR_CARD_SWAPPING_MODE) {
			if (play_cards_remaining_ == 0) {
				game_action_button.setTextColor(Color.BLACK);
			} else {
				game_action_button.setTextColor(Color.LTGRAY);
			}
		} else if (display_mode == LEADING_MODE) {
			if (play_cards_remaining_ != 0) {
				game_action_button.setTextColor(Color.BLACK);
			} else {
				game_action_button.setTextColor(Color.LTGRAY);
			}
		} else {
			game_action_button.setTextColor(Color.LTGRAY);
		}
		UpdateGameActionButtonText();
	}
	
	public void UpdateGameActionButtonText() {
		if (human_player == null)
			return;  // nothing has started yet.
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE) {
			if (display_mode == TRACTOR_CARD_SWAPPING_MODE) {
				game_action_button.setText(R.string.swap_cards_english);
			} else if (display_mode == FOLLOWING_MODE && play_cards_remaining_ != 0) {
				if (play_cards_remaining_ > 0) {
					if (play_cards_remaining_ == 1)
						game_action_button.setText("Need " + Integer.toString(play_cards_remaining_) + " card");
					else
						game_action_button.setText("Need " + Integer.toString(play_cards_remaining_) + " cards");
				} else {
					if (play_cards_remaining_ == -1)
						game_action_button.setText("Less " + Integer.toString(-play_cards_remaining_) + " card");
					else
						game_action_button.setText("Less " + Integer.toString(-play_cards_remaining_) + " cards");
				
				}
			} else {
				game_action_button.setText(R.string.play_cards_english);
			}
		}
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE) {
			if (display_mode == TRACTOR_CARD_SWAPPING_MODE) {
				game_action_button.setText(R.string.swap_cards_chinese);
			} else if (display_mode == FOLLOWING_MODE && play_cards_remaining_ != 0) {
				if (play_cards_remaining_ > 0)
					game_action_button.setText("少了 " + Integer.toString(play_cards_remaining_) + "张牌");
				else
					game_action_button.setText("多了" + Integer.toString(-play_cards_remaining_) + "张牌");
			} else {
				game_action_button.setText(R.string.play_cards_chinese);
			}
		}
	}
	
	public void UpdateDeclareSuitText() {
		if (human_player == null)
			return;  // nothing has started yet.
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE) {
			if (display_mode == TRACTOR_CARD_SWAPPING_MODE) {
				if (play_cards_remaining_ != 0) {
					declare_suit.setText("Please select " +
						Integer.toString(play_cards_remaining_) +
						" more tractor cards to swap");
				} else {
					declare_suit.setText(">>   Please click button to\n>>   swap tractor cards");
				}
			} else if (display_mode == DEALING_MODE || display_mode == DEALING_MODE_DISPLAY_ONLY){
				if (human_player.state.dealer_index_ == -1)
					declare_suit.setText("Click on a colored suit to declare.  Last declarer will be the dealer");
				else
					declare_suit.setText("Click on a colored suit to declare.  Dealer this deal: player " +
							getDisplayId(human_player.state.dealer_index_, human_player.state.playerId));
			} else if (display_mode == WAITING_FOR_GAME_START_MODE) {
				declare_suit.setText("Waiting for server to start game and/or other players to join game.");
			} else if (display_mode == WAITING_FOR_PLAY_START_MODE) {
				declare_suit.setText("Waiting for dealer (player " + 
						getDisplayId(human_player.state.dealer_index_, human_player.state.playerId) + ") to swap tractor cards.");
			}
		}
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE) {
			if (display_mode == TRACTOR_CARD_SWAPPING_MODE) {
				if (play_cards_remaining_ != 0) {
					declare_suit.setText("还要选" +
						Integer.toString(play_cards_remaining_) +
						"张底牌     ");
				} else {
					declare_suit.setText(">>   按下按钮埋底牌");
				}
			} else if (display_mode == DEALING_MODE || display_mode == DEALING_MODE_DISPLAY_ONLY){
				if (human_player.state.dealer_index_ == -1)
					declare_suit.setText("按下彩色花色按钮亮牌.  抢主抢庄");
				else 
					declare_suit.setText("按下彩色花色按钮亮牌.  这手庄家是: 玩家" +
							getDisplayId(human_player.state.dealer_index_, human_player.state.playerId));
			} else if (display_mode == WAITING_FOR_GAME_START_MODE) {
				declare_suit.setText("正在等待服务器开始游戏, 并等待其他玩家加入");
			} else if (display_mode == WAITING_FOR_PLAY_START_MODE) {
				declare_suit.setText("正在等待庄家 (玩家  " +
						getDisplayId(human_player.state.dealer_index_, human_player.state.playerId) + ") 埋底牌");
			}
		}
	}
	
	public void UpdateDeclarationText() {
		if (human_player == null)
			return;  // nothing has started yet.
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			declaration_text.setText("Trump number this deal: " + Card.NumberToString(human_player.GetTrumpNumber()) + ".  No suit declared.");
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			declaration_text.setText("正在打: " + Card.NumberToString(human_player.GetTrumpNumber()) + ".  没有玩家亮花色");
		if (display_mode == TRACTOR_CARD_SWAPPING_MODE || display_mode == WAITING_FOR_PLAY_START_MODE) {
			if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
				declaration_text.setText("Trump suit for this deal: ");
			if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
				declaration_text.setText("这手主牌是: ");
	    } else if (display_mode == WAITING_FOR_GAME_START_MODE) {
	    	declaration_text.setText("");
	    }
		if (display_mode == DEALING_MODE || display_mode == DEALING_MODE_DISPLAY_ONLY ||
				display_mode == TRACTOR_CARD_SWAPPING_MODE ||
	    		display_mode == WAITING_FOR_GAME_START_MODE || display_mode == WAITING_FOR_PLAY_START_MODE) {
	    	if	(human_player.suit_declarer_id != -1) {
	    		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
	    			declaration_text.setText("Player " +
	    					getDisplayId(human_player.suit_declarer_id, human_player.state.playerId) + " declared: ");
	    		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
	    			declaration_text.setText("玩家 " +
	    					getDisplayId(human_player.suit_declarer_id, human_player.state.playerId) + " 亮了: ");
	    	}
	    }
	}
  
    private int FillCardView(int mode) {
    	int wait_time = 250;  // default wait time for each animation.
    	switch (human_player.state.trump_number) {  // Set the image of the permanent trump column
    		case Card.NUMBER_ACE:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.ace_bg);
    			break;
    		case Card.NUMBER_KING:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.king_bg);
    			break;
    		case Card.NUMBER_QUEEN:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.queen_bg);
    			break;
    		case Card.NUMBER_JACK:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.jack_bg);
    			break;
    		case Card.NUMBER_TEN:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.ten_bg);
    			break;
    		case Card.NUMBER_NINE:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.nine_bg);
    			break;
    		case Card.NUMBER_EIGHT:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.eight_bg);
    			break;
    		case Card.NUMBER_SEVEN:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.seven_bg);
    			break;
    		case Card.NUMBER_SIX:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.six_bg);
    			break;
    		case Card.NUMBER_FIVE:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.five_bg);
    			break;
    		case Card.NUMBER_FOUR:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.four_bg);
    			break;
    		case Card.NUMBER_THREE:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.three_bg);
    			break;
    		case Card.NUMBER_TWO:
    			undeclarable_card_suit_image[4].setImageResource(R.drawable.two_bg);
    			break;
    	}
    	for (int i = 0; i < card_suit_layout.length; ++i) {
    		card_suit_layout[i].removeAllViews();
    		col_layout[i].removeAllViews();
    	}
    	hand_summary_view.removeAllViews();
		Card[][] display_hand = human_player.CurrentDisplayHand(mode);
		// The order of suit displayed.  No_trump is always the first.
		// And after dealing mode, the trump_suit follows;
		int[] suit_mapping = human_player.CurrentDisplaySuitMapping();

		int col_width =
			(game_table.getMeasuredWidth() - game_table.getVerticalScrollbarWidth()) / 5;
		normal_card_display_height = col_width / 5 * 4;
		boolean has_new_declarable_suit = false;
		for (int col = 0; col < NUM_DISPLAY_COL; ++col) {
			int suit = suit_mapping[col];
			// Add image first.  Use colored suit for declarable ones, and grey suit for others.
			card_suit_image[suit].setMaxHeight(col_width);
			card_suit_image[suit].setMaxWidth(col_width);
			undeclarable_card_suit_image[suit].setMaxHeight(col_width);
			undeclarable_card_suit_image[suit].setMaxWidth(col_width);
			ImageView real_suit_image = undeclarable_card_suit_image[suit];
			final boolean is_suit_declarable = (mode == DEALING_MODE || mode == DEALING_MODE_DISPLAY_ONLY)
				&& human_player.state.IsDeclarableSuit(human_player.playerId, suit);
			final boolean is_suit_trump_suit = (mode != DEALING_MODE && mode != DEALING_MODE_DISPLAY_ONLY &&
					human_player.state.trump_suit == suit);
			if (is_suit_declarable || is_suit_trump_suit)
				real_suit_image = card_suit_image[suit];
			col_layout[col].setMinimumWidth(col_width);
			col_layout[col].addView(real_suit_image);
			real_suit_image.setPadding(5, 5, 5, 5);
			if (is_suit_declarable) {
				if (real_suit_image.isClickable() == false) {
					has_new_declarable_suit = true;
					real_suit_image.setClickable(true);
				}
				real_suit_image.setOnClickListener(dealing_mode_listener);	
			} else {
				real_suit_image.setClickable(false);
			}
			if (mode != DEALING_MODE && mode != DEALING_MODE_DISPLAY_ONLY) {
				// after dealing, we'll make the suit images a bit smaller, so we
				// have more room for display cards.
				real_suit_image.setMaxHeight(col_width - 18);
				real_suit_image.setMaxWidth(col_width - 18);
				real_suit_image.setPadding(0, 5, 0, 5);
			}
			// If a suit is set for sure, we'll mark it grey.
			if (suit ==  human_player.state.trump_suit ||
					(suit == Card.SUIT_NO_TRUMP && human_player.state.trump_suit != Card.SUIT_UNDEFINED)) {
				col_layout[col].setBackgroundColor(0xff666666 );
			} else {
				col_layout[col].setBackgroundColor(Color.TRANSPARENT);
			}
		}
		for (int col = 0; col < NUM_DISPLAY_COL; ++col) {
			col_layout[col].addView(card_suit_layout[col]);	
			// Add cards
			int num_cards = 0;
			int new_width = col_width;
			if (mode == DEALING_MODE || mode == DEALING_MODE_DISPLAY_ONLY) {
				// That's the suit declaration mode, we'll make the card
				// appear smaller, so more cards can fit in the view.
				// The user wouldn't click on the cards anyway.
				new_width /= 2;
			}
			for (int j = 0; j < display_hand[col].length; ++j) {
				if (display_hand[col][j] != null) {
					DisplayCard display_card = new DisplayCard(this, display_hand[col][j]);
					if (mode == TRACTOR_CARD_SWAPPING_MODE ||
							mode == FOLLOWING_MODE || mode == LEADING_MODE) {
						display_card.setOnClickListener(click_card_listener);
						display_card.setOnLongClickListener(long_click_card_listener);
						display_card.setClickable(true);
					} else {
						display_card.setClickable(false);
					}
					display_card.setMaxHeight(new_width * 4 / 5);
					display_card.setMaxWidth(new_width);
					card_suit_layout[col].addView(display_card);
					num_cards++;
				}
			}
			TextView card_count_view = new TextView(this);
			card_count_view.setText(Integer.toString(num_cards));
			card_count_view.setLayoutParams(
					new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
			card_count_view.setGravity(Gravity.CENTER);
			hand_summary_view.addView(card_count_view);
		}
		
		if (has_new_declarable_suit)
			wait_time += 1000 / CalculateWaitTimeMultiplier();  // more time to respond.
		if (mode == DEALING_MODE &&
				human_player.local_cards_for_dealing_mode_only.size() == human_player.state.cards_per_player)
			wait_time += 1000 / CalculateWaitTimeMultiplier();  // Before the tractor cards can be dealt, one last chance for declaring suit.

    	return wait_time;
    }
    
    private int FillGameView(int mode) {
    	int wait_time = 0;
		
		LinearLayout game_display = (LinearLayout) findViewById(R.id.game_display);
		int padding_per_column = 2;
		// -4 for 2 on right and 2 on left.
		// Sigh, here we need to take the width of the game_table instead of
		// game_display_view.  game_table is always visible, and so we can get
		// the measure easily.  However, game_display_view is switching between
		// visible and gone, and so the measure is not reliable.
		int max_card_width = game_table.getMeasuredWidth()
			/ Math.min(human_player.state.numPlayers, MAX_PLAYERS_PER_ROW)
			- padding_per_column * 2;
		if (max_card_width > DisplayCard.DISPLAY_WIDTH) {
			padding_per_column += (max_card_width - DisplayCard.DISPLAY_WIDTH) / 2;
			max_card_width = DisplayCard.DISPLAY_WIDTH;
		}
		
		LinearLayout.LayoutParams pp =
			new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
	    game_display.removeAllViews();
	    int winning_id = -1;
	    if (mode == WAITING_MODE)
	    	winning_id = human_player.getWinnerOfCurrentRound();
	    AlphaAnimation ani = new AlphaAnimation(0, 1);
    	ani.setDuration(200);
    	ani.setRepeatCount(2 * CalculateWaitTimeMultiplier());
    	if (winning_id != -1)
    		wait_time += 400 + 100 / CalculateWaitTimeMultiplier();
	    
	    for (int player_index = 0; player_index < human_player.state.numPlayers; player_index++) {
			// col represents one column that usually shows one player's
			// played cards.
			LinearLayout col_holder = null;
			LinearLayout col = null;
			if (player_index >= MAX_PLAYERS_PER_ROW) {
				col_holder = (LinearLayout) game_display.getChildAt(player_index % MAX_PLAYERS_PER_ROW);
				col = (LinearLayout) col_holder.getChildAt(0);  // Only one child.
				// add some padding.
				TextView dummy_text = new TextView(this);
				dummy_text.setHeight(10);
				col.addView(dummy_text);
			} else {
				col_holder = new LinearLayout(this);
				col_holder.setPadding(padding_per_column, 0, padding_per_column, 0);
				col = new LinearLayout(this);
				col.setOrientation(LinearLayout.VERTICAL);
				col.setMinimumWidth(max_card_width);
				col_holder.addView(col);
			}
			
		    // Set display ID:
		    TextView player_text =  new TextView(this);
		    if (player_index == human_player.state.playerId)
		    	own_player_view_text = player_text;
		    player_text.setText(getDisplayId(player_index, human_player.state.playerId));
			player_text.setWidth(max_card_width);
			if (human_player.state.dealerGroup != null &&
					human_player.state.dealerGroup.contains(player_index)) {
				// player_text.setTextColor(Color.BLACK);
				player_text.setBackgroundColor(0x770000ff);
			}
			if (player_index == human_player.state.dealer_index_)
				player_text.setBackgroundColor(0xff0000ff);
			
			// Don't set wrap_content here so that the gravity center would
		    // actually make sense.  Sigh, TextView is tricky.  The best way
		    // would seem to specify the full set of layout parameters...
		    player_text.setGravity(Gravity.CENTER);
		    if (mode == WAITING_MODE && winning_id == player_index) {
		    	player_text.setAnimation(ani);
		    }
		    col.addView(player_text);

		    if (mode == DEALING_MODE || mode == DEALING_MODE_DISPLAY_ONLY || mode == TRACTOR_CARD_SWAPPING_MODE ||
		    		mode == WAITING_FOR_GAME_START_MODE || mode == WAITING_FOR_PLAY_START_MODE) {
		    	// do nothing here, don't have much left to do.
		    } else {  // Display cards played by other players.
		    	Card[] cards = human_player.CurrentRoundHistory().get(player_index);
		    	if (cards != null) {
		    		for (Card c : cards) {
		    			DisplayCard new_card = new DisplayCard(this, c.GetIndex());
		    			new_card.setMaxWidth(max_card_width);
		    			if (mode == WAITING_MODE && winning_id == player_index) {
		    		    	new_card.setAnimation(ani);
		    		    }
		    			col.addView(new_card);
		    		}
		    	} else {
		    		// For display purpose, create an empty card
		    		for (int i = 0; i < human_player.current_round_num_cards_; ++i) {
		    			DisplayCard blank_card = new DisplayCard(this, Card.BLANK_CARD);
		    			blank_card.setMaxWidth(max_card_width);
		    			col.addView(blank_card);
		    		}
		    	}
		     }
		    if (player_index < MAX_PLAYERS_PER_ROW) {
		    	game_display.addView(col_holder, pp);
		    }
		}
	    if (winning_id == -1)
	    	wait_time += human_player.current_round_num_cards_ * 50;
		return wait_time;
    }

    
    private void CardClicked(View v) {
    	DisplayCard c = (DisplayCard) v;
    	c.ToggleSelected();
    	if (display_mode == TRACTOR_CARD_SWAPPING_MODE ||
    			display_mode == FOLLOWING_MODE ||
    			display_mode == LEADING_MODE) {
    		if (c.isSelected()) {
    			play_cards_remaining_--;
    		} else {
    			play_cards_remaining_++;
    		}
    		UpdateGameActionButton();
    		UpdateDeclareSuitText();
    	}
    }
    
    private void UnSelectAllCards() {
       	for (LinearLayout l : card_suit_layout) {
    		for (int i = 0; i < l.getChildCount(); ++i) {
    			DisplayCard card = (DisplayCard) l.getChildAt(i);
    			if (card.isSelected())
    				CardClicked(card);
    		}
    	}
    }
    
    private ArrayList<Card> GetSelectedCards() {
    	ArrayList<Card> cards = new ArrayList<Card>();
    	for (LinearLayout l : card_suit_layout) {
    		for (int i = 0; i < l.getChildCount(); ++i) {
    			DisplayCard card = (DisplayCard) l.getChildAt(i);
    			if (card.isSelected())
    				cards.add(card.ConvertToPlainCard());
    		}
    	}
    	return cards;
    }
    
    private int PreSelectCards(Card[] selected_cards) {
    	Card[] cards = selected_cards.clone();
    	// First get rid of all already selected cards
    	for (LinearLayout l : card_suit_layout) {
    		for (int i = 0; i < l.getChildCount(); ++i) {
    			DisplayCard card = (DisplayCard) l.getChildAt(i);
    			if (card.isSelected())
    				CardClicked(card);
    		}
    	}
    	int earliest_x_coordinate = Integer.MAX_VALUE;
    	for (LinearLayout l : card_suit_layout) {
    		for (int i = 0; i < l.getChildCount(); ++i) {
    			DisplayCard card = (DisplayCard) l.getChildAt(i);
    			int card_top = i * normal_card_display_height;
    			if (card.isSelected()) continue;
    			// MQC: this may be slow, optimize.
    			for (int j = 0; j < cards.length; j++) {
    				if (cards[j] != null && cards[j].GetIndex() == card.GetIndex()) {
    					cards[j] = null;
   	    				CardClicked(card);
   	    				// Util.e("Card top", Integer.toString(card_top));
   	    				if (card_top < earliest_x_coordinate)
   	    					earliest_x_coordinate = card_top;
   	    				break;
    				}
    			}
    		}
    	}
    	// The extra term at the end takes care of the Suit image per column
    	return earliest_x_coordinate + normal_card_display_height / 4 * 5;
    }

    /**
     * Gather the selected cards, and send the cards to server on HumanPlayer's behave.
     * Note that the actual legal play checking is done on the server side.  And if there is
     * illegal play, the server will notify us.
     */
    private void PlayCards() {
		// Get the list of selected cards;
    	ArrayList<Card> cards = GetSelectedCards();
     	human_player.PlayerAction(TractorGameAction.PLAY_CARD_ACTION, cards.toArray(new Card[0]));
    }
    
    private void SwapCards() {
    	ArrayList<Card> cards = GetSelectedCards();
    	Card[] selected_cards = cards.toArray(new Card[0]);
    	human_player.PlayerAction(TractorGameAction.SWAP_TRACTOR_CARD_ACTION, selected_cards);
    }

	public HumanPlayer getPlayer() {
		return human_player;
	}

	public void setPlayer(HumanPlayer player) {
		this.human_player = player;
		
	}

	public void SetGameControllerMode(int mode, String id) {
		Message msg = msgHandler.obtainMessage();
		msg.arg1 = mode;
		msg.obj = id;
		msgHandler.sendMessage(msg);
	}

	/**
	 * So far this is basically an audio bell to signal that something has happened.
	 * TODO: search for better sound.
	 * 
	 */
	public void SendNotification(Object msg) {
		final Notification notification = new Notification();
		notification.defaults = Notification.DEFAULT_SOUND;
		// we use a string id because it is a unique number.  we use it later to cancel the notification.
		notification_manager_.notify(R.layout.game_table, notification);

	}

	public Semaphore getGameControllerLock() {
		return this.game_lock_;
	}

}