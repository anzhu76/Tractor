/*
 * Copyright (C) 2008 An Zhu, Qicheng Ma.
 * 
 * Android specific.
 * 
 * The launcher for Tractor.  It collects various create/load/join options.
 * We've taken mostly of the decision making away from the user, i.e., the UI
 * will auto adjust if certain choice renders some other choices possible/impossible.
 * And so right now there is no need for error checking the user entries.  Tractor
 * will launch GameController activity, which is in charge of main display during 
 * the game play.
 */

package com.android.tractor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import java.lang.Integer;
import java.util.Vector;


public class Tractor extends Activity {
	static public final String kServer = "com.android.tractor.Paras.Server";
	static public final String kCurrentServer = "com.android.tractor.Paras.CurrentServer";
	static public final String kGameId = "com.android.tractor.Paras.GameId";
	static public final String kCreateOrJoin = "com.android.tractor.Paras.CreateOrJoin";
	static public final String kPreferredId = "com.android.tractor.Paras.PreferredId";
	static public final String kNumPlayers = "com.android.tractor.Paras.NumPlayers";
	static public final String kNumDecks = "com.android.tractor.Paras.NumDecks";
	static public final String kNumAIs = "com.android.tractor.Paras.NumAIs";
	public static final String PREFS_NAME = "com.android.tractor.TractorPreference";
	
    private static final int ACTIVITY_NEW_GAME = 0;
    
    private static final int START_GAME_ERROR_DIALOG_ID = 3;
    private static final int GAME_OPTION_ID = 4;
    
	public static final int MIN_PLAYERS = 4;
	public static final int MAX_PLAYERS = 6;  // higher number of players really don't have any play value.  Restrict to 6 for now.
    public static final int MIN_DECKS = 1;
    public static final int MAX_DECKS = 6;
    public static final int DEFAULT_NUM_PLAYERS = 4;
	public static final int DEFAULT_NUM_DECKS = 2;

    private AutoCompleteTextView mGameServerText; 
    private Spinner mPreferredPlayerIdText;
	private Spinner mNumPlayersText;
    private Spinner mNumDecksText;
    private Spinner mNumAIsText;
    private EditText mNumAIsFixedText;
    private EditText mServerAddressText;
    private EditText mGameIdText;
    private ArrayAdapter<Integer> mPreferredPlayerIdAdapter;
    private ArrayAdapter<Integer> mNumPlayersAdapter;
    private ArrayAdapter<Integer> mNumDecksAdapter;
    private ArrayAdapter<Integer> mNumAIsAdapter;
    private int preferred_player_id;
    private int num_players;
    private int num_decks;
    private int num_ais;
    private RadioButton mJoinGameChoice;
    private RadioButton mCreateGameChoice;
    private TableLayout mGameOptionLayout;
    private ScrollView gameOptionView;
    private AlertDialog game_option_dialog;
    private ArrayAdapter<String> server_list_adapter = null;
    private String last_non_localhost_server = "";
    // After consulting with android group, there is no easy way of passing
    // complicated object to another activity, but global variable works!
    public static String serverAddress = "";
    public static String gameId = "";
    
    /*
     * All I can say is that Eclipse sucks to some way.  I spent *a lot of* time
     * trying to obtain the following behavior:
     * User clicks on Tractor, they see activity A, they launch a new tractor game, 
     * now they see activity B.  They click home, they go back to the home screen.
     * They click on Tractor, they should see activity B.  However, if the app is launched
     * via Eclipse for the first time, they will see an additional activity A!  See this post:
     * http://groups.google.com/group/android-developers/browse_thread/thread/595682a864b2a93d/7e09afdf9c742155?lnk=gst&q=singletop#7e09afdf9c742155
     * and grep for Eclipse.
     */
    // private static int num_oncreate_calls = 0;

    // We need some default errorMessage
    // and errorTitle to make sure that message and title will both show up.
    // An empty title on creation will kill the title, curious, isn't it?
	private String errorMessage = "error message";
	private String errorTitle = "error title";
	
	/*
	 * We want the following behavior.  The activity running in the background might be killed randomly
	 * by the system due to memory issues, or whatever. If this Tractor activity is killed, no big deal, we'll simply restart.
	 * But when GameController is killed, we essentially lost the last game played.  In that case, the sensitive
	 * thing to do would be that upon restart, we exit GameController and return back to Tractor.  I don't find
	 * a neat Android way of doing that.  So static variable comes to the rescue once more.
	 */
	public static boolean gameControllerActivityInitiated = false;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        // Restore saved game options
        last_non_localhost_server = settings.getString(kServer, "");
        GameOptions.LoadGameOptions(this);
        
        setContentView(R.layout.main);

        
        mServerAddressText = (EditText) findViewById(R.id.server_address);
        mServerAddressText.setText(settings.getString(kCurrentServer, GameController.kLocalGameServerAddr));
        serverAddress = mServerAddressText.getText().toString();
        mGameIdText = (EditText) findViewById(R.id.game_id);
        mGameIdText.setText(settings.getString(kGameId, GameController.kDefaultGameId));
        mJoinGameChoice = (RadioButton) findViewById(R.id.join_game);
        mCreateGameChoice = (RadioButton) findViewById(R.id.create_game);
        if (settings.getBoolean(kCreateOrJoin, true)) {
        	mCreateGameChoice.setChecked(true);
        	mJoinGameChoice.setChecked(false);
        } else {
        	mCreateGameChoice.setChecked(false);
        	mJoinGameChoice.setChecked(true);
        }
        num_players = settings.getInt(kNumPlayers, Tractor.DEFAULT_NUM_PLAYERS);
        num_decks = settings.getInt(kNumDecks, Tractor.DEFAULT_NUM_DECKS);
        num_ais = settings.getInt(kNumAIs, 0);
        preferred_player_id = settings.getInt(kPreferredId, 0);
        
        
        LayoutInflater factory = LayoutInflater.from(this);
    	gameOptionView = GameController.SetUpGameOptionView(factory, this);

        mPreferredPlayerIdText = (Spinner) findViewById(R.id.preferred_player_id);
    	mNumPlayersText = (Spinner) findViewById(R.id.num_players);
        mNumDecksText = (Spinner) findViewById(R.id.num_decks);
        mNumAIsText = (Spinner) findViewById(R.id.num_ais);
        mNumAIsFixedText = (EditText) findViewById(R.id.num_ais_fixed);
        mNumAIsFixedText.setEnabled(false);
        mNumAIsFixedText.setFocusable(false);
        
        Vector<Integer> num_player_vector = new Vector<Integer>();
        for (int i = MIN_PLAYERS; i <= MAX_PLAYERS; i = i + 2)
        	num_player_vector.add(i);
        mNumPlayersAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, num_player_vector.toArray(new Integer[0]));
        mNumPlayersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNumPlayersText.setAdapter(mNumPlayersAdapter);
        mNumPlayersText.setSelection(num_players / 2 - 2);
        
        Vector<Integer> num_decks_vector = new Vector<Integer>();
        for (int i = MIN_DECKS; i <= MAX_DECKS; ++i)
        	num_decks_vector.add(i);
        mNumDecksAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, num_decks_vector.toArray(new Integer[0]));
        mNumDecksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNumDecksText.setAdapter(mNumDecksAdapter);
        mNumDecksText.setSelection(num_decks - 1);
        
        mNumPlayersText.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				num_players = 2 * (2 + arg2);
				UpdateNumAIsSpinner();
		        UpdatePreferredIdSpinnerAndView();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}});
        
        mNumDecksText.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				num_decks = 1 + arg2;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}});
        
        mPreferredPlayerIdText.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				preferred_player_id = arg2;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}});
        
        mNumAIsText.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				num_ais = arg2;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}});
        
        mGameOptionLayout = (TableLayout) findViewById(R.id.game_options);

        // Set up the auto complete for server address
    	server_list_adapter = new ArrayAdapter<String>(this , android.R.layout.simple_dropdown_item_1line);
    	server_list_adapter.add(GameController.kLocalGameServerAddr);
    	if (last_non_localhost_server != "")
    		server_list_adapter.add(last_non_localhost_server);
        mGameServerText = (AutoCompleteTextView)
                findViewById(R.id.server_address);
        mGameServerText.setAdapter(server_list_adapter);
        mGameServerText.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
				serverAddress = mServerAddressText.getText().toString();
				UpdateNumAIsSpinner();
				UpdateRadioGroupChoice();
				UpdateGameOptionView();
				// TODO Auto-generated method stub
				return false;
			}});

        mGameServerText.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				serverAddress = mServerAddressText.getText().toString();
				UpdateNumAIsSpinner();
				UpdateRadioGroupChoice();
				UpdateGameOptionView();
			}});
        
        final Button button = (Button) findViewById(R.id.start_game_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Start a new game
            	LaunchNewGame();
            }
        });
        
        final Button button2 = (Button) findViewById(R.id.game_option_button);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Start game options screen.
            	LaunchGameOption();
             }
        });

        ((Button) findViewById(R.id.game_instruction_button)).setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://docs.google.com/Doc?id=dddbdn3f_02qmbpbhp")));
				if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://docs.google.com/Doc?id=dddbdn3f_135z2kxdm2")));
			}});


        mCreateGameChoice.setOnCheckedChangeListener(new RadioButton.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
			     UpdatePreferredIdSpinnerAndView();
			}});
        /*
         * test Socket implementation of game server.
         *
        try {
			TestGameClient.main(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
    }
	
	private void UpdateRadioGroupChoice() {
		if (serverAddress.compareTo(GameController.kLocalGameServerAddr) != 0) {
			mGameIdText.setEnabled(true);
		} else {
			mGameIdText.setEnabled(false);
		}
		UpdateSecondGameChoiceText();
	}
	
	private void UpdateNumAIsSpinner() {
		int position = num_ais;
		if (serverAddress.compareTo(GameController.kLocalGameServerAddr) != 0) {
			Integer[] choices = new Integer[num_players];
			for (int i = 0; i < choices.length; ++i)
				choices[i] = i;
			mNumAIsAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, choices);
			mNumAIsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mNumAIsText.setAdapter(mNumAIsAdapter);
			mNumAIsText.setVisibility(View.VISIBLE);
			mNumAIsText.setSelection(Math.min(position, num_players - 1));
			num_ais = mNumAIsText.getSelectedItemPosition();
			mNumAIsAdapter.notifyDataSetChanged();
			mNumAIsFixedText.setVisibility(View.GONE);
		} else {
			mNumAIsText.setVisibility(View.GONE);
			mNumAIsFixedText.setVisibility(View.VISIBLE);
			num_ais = num_players - 1;
			mNumAIsFixedText.setText(Integer.toString(num_ais));
		}
	}
	
	private void UpdateGameOptionView() {
		boolean create_game = ((RadioButton) findViewById(R.id.create_game)).isChecked();
		if (create_game) {
			for (int i = 0; i < 4; ++i) {
				mGameOptionLayout.getChildAt(i).setVisibility(View.VISIBLE);
			}
		} else {
			for (int i = 1; i < 4; ++i) {
				mGameOptionLayout.getChildAt(i).setVisibility(View.INVISIBLE);
			}
			if (serverAddress.compareTo(GameController.kLocalGameServerAddr) == 0)
				mGameOptionLayout.getChildAt(0).setVisibility(View.INVISIBLE);
			else
				mGameOptionLayout.getChildAt(0).setVisibility(View.VISIBLE);
		}
	}
	
	private void UpdatePreferredIdSpinnerAndView() {
		UpdateGameOptionView();
		int position = preferred_player_id;
		Integer[] choices = null;
		boolean create_game = ((RadioButton) findViewById(R.id.create_game)).isChecked();
		if (create_game) {
			choices = new Integer[num_players];
		} else {
			choices = new Integer[MAX_PLAYERS];
		}
		for (int i = 0; i < choices.length; ++i)
			choices[i] = i;
		mPreferredPlayerIdAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, choices);
		mPreferredPlayerIdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPreferredPlayerIdText.setAdapter(mPreferredPlayerIdAdapter);
		preferred_player_id = Math.min(position, choices.length - 1);
		mPreferredPlayerIdText.setSelection(preferred_player_id);
		mPreferredPlayerIdAdapter.notifyDataSetChanged();
	}
	
    @Override
    protected void onStop(){
       super.onStop();
    
      // Save user preferences. We need an Editor object to
      // make changes. All objects are from android.context.Context
      SharedPreferences settings = getSharedPreferences(Tractor.PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putString(kServer, last_non_localhost_server);
      editor.putString(kCurrentServer, ((EditText) findViewById(R.id.server_address)).getText().toString());
      editor.putString(kGameId, ((EditText) findViewById(R.id.game_id)).getText().toString());
      editor.putBoolean(kCreateOrJoin, mCreateGameChoice.isChecked());
      editor.putInt(kPreferredId, preferred_player_id);
      editor.putInt(kNumPlayers, num_players);
      editor.putInt(kNumDecks, num_decks);
      editor.putInt(kNumAIs, num_ais);
      // Don't forget to commit your edits!!!
      editor.commit();
      
      // Save GameOptions as well
      GameOptions.SaveGameOptions(this);
    }
    
    public void UpdateSecondGameChoiceText() {
    	RadioButton r = (RadioButton) findViewById(R.id.join_game);
    	if (serverAddress.compareTo(GameController.kLocalGameServerAddr) != 0) {
    		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
    			r.setText(R.string.join_game_english);
    		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
    			r.setText(R.string.join_game_chinese);
    	} else {
    		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
    			r.setText(R.string.load_game_english);
    		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
    			r.setText(R.string.load_game_chinese);
    	}
    }
	
	public void UpdateDisplayText() {
		TextView t = (TextView) findViewById(R.id.server_address_text);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			t.setText(R.string.server_address_english);
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			t.setText(R.string.server_address_chinese);
		t = (TextView) findViewById(R.id.game_id_text);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			t.setText(R.string.game_id_english);
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			t.setText(R.string.game_id_chinese);
		t = (TextView) findViewById(R.id.preferred_player_id_text);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			t.setText(R.string.preferred_player_id_english);
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			t.setText(R.string.preferred_player_id_chinese);
		t = (TextView) findViewById(R.id.num_players_text);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			t.setText(R.string.num_players_english);
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			t.setText(R.string.num_players_chinese);
		t = (TextView) findViewById(R.id.num_decks_text);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			t.setText(R.string.num_decks_english);
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			t.setText(R.string.num_decks_chinese);
		t = (TextView) findViewById(R.id.num_ais_text);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			t.setText(R.string.num_ais_english);
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			t.setText(R.string.num_ais_chinese);
		
		Button b = (Button) findViewById(R.id.start_game_button);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			b.setText(R.string.start_game_english);
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			b.setText(R.string.start_game_chinese);
		b = (Button) findViewById(R.id.game_instruction_button);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			b.setText(R.string.game_instruction_english);
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			b.setText(R.string.game_instruction_chinese);
		
		RadioButton r = (RadioButton) findViewById(R.id.create_game);
		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
			r.setText(R.string.create_game_english);
		if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
			r.setText(R.string.create_game_chinese);
		UpdateSecondGameChoiceText();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// Set a bunch of text according to language
		UpdateDisplayText();
		UpdateRadioGroupChoice();
		UpdateNumAIsSpinner();
		UpdatePreferredIdSpinnerAndView();
	}
	
	private void LaunchGameOption() {
		showDialog(GAME_OPTION_ID);
	}
	
	private void LaunchNewGame() {
    	gameId = mGameIdText.getText().toString();
    	serverAddress = mServerAddressText.getText().toString();
    	
    	// If the address is not localhost, we'll store it somewhere.
    	if (serverAddress.compareTo(GameController.kLocalGameServerAddr) != 0 && serverAddress.length() != 0)
    		last_non_localhost_server = serverAddress;
    	server_list_adapter.remove(serverAddress);
    	server_list_adapter.add(serverAddress);

    	boolean create_game = mCreateGameChoice.isChecked();

    	Intent i = new Intent(this, GameController.class);
    	i.putExtra(GameController.PREFERRED_PLAYER_ID, preferred_player_id);
		i.putExtra(GameController.NUM_PLAYERS, num_players);
		i.putExtra(GameController.NUM_DECKS, num_decks);
		i.putExtra(GameController.NUM_AIS, num_ais);
		i.putExtra(GameController.SERVER_ADDRESS, serverAddress);
		i.putExtra(GameController.GAME_ID, gameId);
		i.putExtra(GameController.IS_CREATOR, create_game);
		// Apparently, the new activity is launched first, and then the old activity is put on stop.  And we need
		// to save the preference before the new activity is launched.
		GameOptions.SaveGameOptions(this);
		gameControllerActivityInitiated = true;
		startActivityForResult(i, ACTIVITY_NEW_GAME);
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
            Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	if (requestCode == ACTIVITY_NEW_GAME) {
			switch (resultCode) {
    		case GameController.RESULT_CANNOT_CONNECT_TO_SERVER:
    			if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
    				errorMessage = "Cannot connect to server.";
    			if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
    				errorMessage = "连不上服务器.";
    			showDialog(START_GAME_ERROR_DIALOG_ID);
        		break;
    		case GameController.RESULT_CANNOT_CREATE_GAME:
    			if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
    				errorMessage = "Cannot create game.";
    			if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
    				errorMessage = "不能创建游戏.";
    			showDialog(START_GAME_ERROR_DIALOG_ID);
        		break;
    		case GameController.RESULT_CANNOT_JOIN_GAME:
    			if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
    				errorMessage = "Cannot join game.";
    			if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
    				errorMessage = "不能加入游戏.";
    			showDialog(START_GAME_ERROR_DIALOG_ID);
        		break;
    		case GameController.RESULT_ABANDON_GAME:
    			if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
    				errorMessage = "Other player(s) left the game.";
    			if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
    				errorMessage = "其他玩家退出了.";
    			showDialog(START_GAME_ERROR_DIALOG_ID);
    			break;
    		case GameController.RESULT_OK:
    		}
    	}
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	super.onPrepareDialog(id, dialog);
    	switch (id) {
    	case START_GAME_ERROR_DIALOG_ID:
    		if (GameOptions.display_language == GameOptions.ENGLISH_LANGUAGE)
				errorTitle = "Game Ended";
			if (GameOptions.display_language == GameOptions.CHINESE_LANGUAGE)
				errorTitle = "游戏结束了";
			((AlertDialog) dialog).setTitle(errorTitle);
    		((AlertDialog) dialog).setMessage(errorMessage);
    		break;
    	case GAME_OPTION_ID:
    		GameController.UpdateGameOptionView(gameOptionView, this);
    	}
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	if (id == GAME_OPTION_ID) {
    		game_option_dialog = new AlertDialog.Builder(this)
    		.setTitle("Options/选项")
    		.setView(gameOptionView)
    		.setIcon(android.R.drawable.ic_dialog_info)
    		.setPositiveButton(R.string.alert_dialog_ok_english, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                   }
                })
                .create();

    		return game_option_dialog;
    	}
    	return new AlertDialog.Builder(this)
        .setTitle(errorTitle)
        .setMessage(errorMessage)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .create();
    }
}