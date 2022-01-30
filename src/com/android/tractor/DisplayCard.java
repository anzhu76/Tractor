/*
 * Copyright (C) 2008 An Zhu, Qicheng Ma.
 * 
 * Android specific.
 * 
 * The display card used in Android UI.  It takes a numeric card and manages
 * display image, according to the card value and also selection condition.
 */
package com.android.tractor;

import android.content.Context;
import android.widget.ImageView;

public class DisplayCard extends ImageView implements AbstractCard{
	private Card numeric_card;
	private boolean checked_;
	
	// Constants for card shape, size, etc.
	public static final int DISPLAY_WIDTH = 50;
	public static final int DISPLAY_HEIGHT = 40;
	
	
	public DisplayCard(Context context) {
		super(context);
	}
	
	public DisplayCard(Context context, Card card) {
		super(context);
		numeric_card = card;
		SetDisplayImage();
	}
	
	public DisplayCard(Context context, int index) {
		super(context);
		numeric_card = new Card(index);
		SetDisplayImage();
	}

	public int GetIndex() {
		return numeric_card.GetIndex();
	}

	public int GetNumber() {
		return numeric_card.GetNumber();
	}

	public int GetSuit() {
		return numeric_card.GetSuit();
	}
	
    private void SetDisplayImage() {
		// We have the suit and number, we should set the proper image.
			switch (numeric_card.GetNumber()) {
			case Card.NUMBER_ACE:
				setImageResource(R.drawable.black_ace);
				if (IsRedSuit())
					setImageResource(R.drawable.red_ace);
				break;
			case Card.NUMBER_KING:
				setImageResource(R.drawable.black_king);
				if (IsRedSuit())
					setImageResource(R.drawable.red_king);
				break;
			case Card.NUMBER_QUEEN:
				setImageResource(R.drawable.black_queen);
				if (IsRedSuit())
					setImageResource(R.drawable.red_queen);
				break;
			case Card.NUMBER_JACK:
				setImageResource(R.drawable.black_jack);
				if (IsRedSuit())
					setImageResource(R.drawable.red_jack);
				break;
			case Card.NUMBER_TEN:
				setImageResource(R.drawable.black_ten);
				if (IsRedSuit())
					setImageResource(R.drawable.red_ten);
				break;
			case Card.NUMBER_NINE:
				setImageResource(R.drawable.black_nine);
				if (IsRedSuit())
					setImageResource(R.drawable.red_nine);
				break;
			case Card.NUMBER_EIGHT:
				setImageResource(R.drawable.black_eight);
				if (IsRedSuit())
					setImageResource(R.drawable.red_eight);
				break;
			case Card.NUMBER_SEVEN:
				setImageResource(R.drawable.black_seven);
				if (IsRedSuit())
					setImageResource(R.drawable.red_seven);
				break;
			case Card.NUMBER_SIX:
				setImageResource(R.drawable.black_six);
				if (IsRedSuit())
					setImageResource(R.drawable.red_six);
				break;
			case Card.NUMBER_FIVE:
				setImageResource(R.drawable.black_five);
				if (IsRedSuit())
					setImageResource(R.drawable.red_five);
				break;
			case Card.NUMBER_FOUR:
				setImageResource(R.drawable.black_four);
				if (IsRedSuit())
					setImageResource(R.drawable.red_four);
				break;
			case Card.NUMBER_THREE:
				setImageResource(R.drawable.black_three);
				if (IsRedSuit())
					setImageResource(R.drawable.red_three);
				break;
			case Card.NUMBER_TWO:
				setImageResource(R.drawable.black_two);
				if (IsRedSuit())
					setImageResource(R.drawable.red_two);
				break;
			case Card.NUMBER_NO_GUARANTEE:
				setImageResource(R.drawable.blank_card);
				break;
			case Card.NUMBER_GUARANTEE:
				setImageResource(R.drawable.blank_card);
				break;
			}
			SetBackgroundImage(checked_);

			if (numeric_card.GetIndex() == Card.BLANK_CARD) {
				setImageResource(R.drawable.blank_card);
				setBackgroundResource(R.drawable.blank_card);
			}
			if (numeric_card.GetIndex() == Card.UNKNOWN_CARD) {
				setImageResource(R.drawable.blank_card);
				setBackgroundResource(R.drawable.unknown_card);
			}
			
			setMaxHeight(DISPLAY_HEIGHT);
			setMaxWidth(DISPLAY_WIDTH);
			setAdjustViewBounds(true);
    }

    
    private void SetBackgroundImage(boolean selected) {
		switch (numeric_card.GetSuit()) {
		case Card.SUIT_SPADE:
			if (selected)
				setBackgroundResource(R.drawable.spade_selected);
			else
				setBackgroundResource(R.drawable.spade);
			break;
		case Card.SUIT_HEART:
			if (selected)
				setBackgroundResource(R.drawable.heart_selected);
			else 
				setBackgroundResource(R.drawable.heart);
			break;
		case Card.SUIT_DIAMOND:
			if (selected)
				setBackgroundResource(R.drawable.diamond_selected);
			else
				setBackgroundResource(R.drawable.diamond);
			break;
		case Card.SUIT_CLUB:
			if (selected)
				setBackgroundResource(R.drawable.club_selected);
			else
				setBackgroundResource(R.drawable.club);
			break;
		}
		if (numeric_card.GetNumber() == Card.NUMBER_GUARANTEE) {
			if (selected)
				setBackgroundResource(R.drawable.wheel_mono_selected);
			else
				setBackgroundResource(R.drawable.wheel_mono);
		}
		if (numeric_card.GetNumber() == Card.NUMBER_NO_GUARANTEE) {
			if (selected)
				setBackgroundResource(R.drawable.wheel_color_selected);
			else
				setBackgroundResource(R.drawable.wheel_color);
		}
    }

    /**
     * A natural ordering of the cards, in increasing order of index.
     * Note that this does not take into consideration the trump suit
     * and number.
     */
	public int compareTo(Card another) {
		return this.GetIndex() - another.GetIndex();
	}

	public void ToggleSelected() {
		checked_ = !checked_;
		SetBackgroundImage(checked_);
	}
	
	public boolean isSelected() {
		return checked_;
	}
	
	public Card ConvertToPlainCard() {
		return numeric_card;
	}
	
	
	private boolean IsRedSuit() {
		if (numeric_card.GetSuit() == Card.SUIT_HEART ||
				numeric_card.GetSuit() == Card.SUIT_DIAMOND) {
			return true;
		}
		return false;
	}
}
