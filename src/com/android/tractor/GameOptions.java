package com.android.tractor;

import android.content.Context;
import android.content.SharedPreferences;

public class GameOptions {
	static public final int ANIMATION_SPEED_FAST = 0;
	static public final int ANIMATION_SPEED_MEDIUM = 1;
	static public final int ANIMATION_SPEED_SLOW = 2;
	
	static public final int ENGLISH_LANGUAGE = 0;
	static public final int CHINESE_LANGUAGE = 1;

	static public boolean auto_select_cards = true;
	static public int animation_speed = ANIMATION_SPEED_MEDIUM;
	static public int display_language = ENGLISH_LANGUAGE;
	static public boolean auto_select_cards_changed = false;
	
	public static final String PREFS_NAME = "com.android.tractor.GameOptions";
    public static final String kLocalGameServerAddr = "localhost";
	public static final String kDefaultGameId = "defaultGameId";
	public static final String kDebugTag = "GameController";

	static public final String kLanguage = "com.android.tractor.Options.Language";
	static public final String kAutoSelect = "com.android.tractor.Options.AutoSelect";
	static public final String kSpeed = "com.android.tractor.Options.Speed";
	
	static public void LoadGameOptions(Context context) {
		SharedPreferences settings = context.getSharedPreferences(GameOptions.PREFS_NAME, 0);
        // Restore saved game options
        GameOptions.display_language = settings.getInt(kLanguage, GameOptions.ENGLISH_LANGUAGE);
        GameOptions.animation_speed = settings.getInt(kSpeed, GameOptions.ANIMATION_SPEED_MEDIUM);
        GameOptions.auto_select_cards = settings.getBoolean(kAutoSelect, true);
	}
	
	static public void SaveGameOptions(Context context) {
	      SharedPreferences settings = context.getSharedPreferences(GameOptions.PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      editor.putInt(kLanguage, GameOptions.display_language);
	      editor.putInt(kSpeed, GameOptions.animation_speed);
	      editor.putBoolean(kAutoSelect, GameOptions.auto_select_cards);
	      editor.commit();
	}
}
