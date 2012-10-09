package com.sam.graphkeyboard;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;

import com.sam.render.RenderEngine;
import com.sam.render.RenderShape;
import com.sam.suggestion.SuggestionEngine;

/*
 * Create a set of keys placed on the screen.
 */
public class GraphKeyboard 
{
	//use this suggestion engine to guess expected chars
    private SuggestionEngine suggest = null;
    //use this engine to place keys on screen
    private RenderEngine render = null;
    //view used to show input word
    private KeyboardResultView resultsView = null;
    
	//map char and Key on a keyboard
    //map key is a an integer converted from Keyboard char for single characters
    //and some unique index starting from 0x10100 for other keys
	public TreeMap<Integer, KeyboardKey> keyboardMain;
	private KeyboardKey fixedKey = null;

	private TextWatcher mTextWatcher = null;
	private int trackingTimeout = 0;
	private KeyboardKey prevTouchedKey = null;
	private KeyboardKey nowTouchedKey = null;
	private KeyboardKey fixedTouchedKey = null;
	
	private ScheduledThreadPoolExecutor timer = null;
	private ScheduledFuture<?> timeoutCall = null;
	
	//private long userDelayMs = longKeyTime;
	private boolean trackingMode = false;
	//how to draw a text
	private Paint paintTrack;
	//what to draw when in tracking mode
	public final static String trackingLabel = "TRACKING";
	
	public final static int KEYBOARD_USER_INDEX = 0x10100;
	public final static int KEYBOARD_END_KEYCODE = KEYBOARD_USER_INDEX + 0x100;
	
	// A tracking timeout class for a timer
	final class TrackingTimeout implements Runnable
	{
		public void run() 
		{			
			if(nowTouchedKey == null)
				return;
				
			//if we press key for more than longKeyTime ms and it is not the same key as previous fixed
			if(nowTouchedKey == fixedTouchedKey)
			{		
				return;
			}
				
			//process touched key by GraphKeyboard (draw fixed, add suggestions)
			processKey(nowTouchedKey);
				
			fixedTouchedKey = nowTouchedKey;
			prevTouchedKey = nowTouchedKey;
		}
	}
	
	// A default timeout class for a timer
	final class DefaultTimeout implements Runnable
	{
		public void run() 
		{
			trackingMode = true;
			Log.v("test", "tracking mode ON");
			
			if(!trackingMode)
			{	
				keyboardMain.put(KEYBOARD_END_KEYCODE, new KeyboardKey(KEYBOARD_END_KEYCODE, "END"));
			}
			else
			{
				keyboardMain.remove(KEYBOARD_END_KEYCODE);
			}
			
			//when switching to tracking mode - try to select the key
			//which we are standing on
			timer.execute(new TrackingTimeout());
		}
	}
	
	//make layout know where we could draw keys of the keyboard
	public GraphKeyboard(Context context)
	{	
		loadDefaultKeys();
		
		///TODO: remove dependency on SharedPreferences
		SharedPreferences prefs = 
			    PreferenceManager.getDefaultSharedPreferences(context);
		trackingTimeout = prefs.getInt("my_seekbar_preference", MySeekBarPreference.DEFAULT_TRACKING_DELAY);
		
		timer = new ScheduledThreadPoolExecutor(3);
		
		paintTrack = new Paint();
		paintTrack.setColor(Color.RED);
		paintTrack.setStyle(Style.FILL);
		///TODO: set text size depending on screen size
		paintTrack.setTextSize(20);
	}
	
	private void loadDefaultKeys()
	{
		keyboardMain = new TreeMap<Integer, KeyboardKey>();
		
		String languageChars = SuggestionEngine.frequentCharsEn;
		
		if(suggest != null)
		{
			languageChars = suggest.getCurrentLanguageFrequentChars();
		}
		
		for(int i = 0; i < languageChars.length(); i++)
		{
			char a = languageChars.charAt(i);
			keyboardMain.put((int)(a), new KeyboardKey((int)(a), "" + a));
			
		}
		
		keyboardMain.put((int)('\''), new KeyboardKey((int)('\''), "" + '\''));
		keyboardMain.put((int)('.'), new KeyboardKey((int)('.'), "" + '.'));
		keyboardMain.put((int)(','), new KeyboardKey((int)(','), "" + ','));
		keyboardMain.put((int)(' '), new KeyboardKey((int)(' '), "" + ' '));
		
		if(!trackingMode)
		{	
			keyboardMain.put(KEYBOARD_END_KEYCODE, new KeyboardKey(KEYBOARD_END_KEYCODE, "END"));
		}
	}
	
	public void setTextWatcher(TextWatcher parent) {
		mTextWatcher = parent;
	}
	
	public void setSuggestionEngine(SuggestionEngine suggestSet)
	{
		suggest = suggestSet;
	}
	
	public void setRenderEngine(RenderEngine render)
	{
		this.render = render;
	}
	
	public void setResultsView(KeyboardResultView v)
	{
		this.resultsView = v;
		resultsView.clear();
	}
	
	//select mode of the keyboard
	public synchronized void setTrackingMode(boolean mode)
	{
		trackingMode = mode;
		loadDefaultKeys();
	}
	
	public synchronized void draw(Canvas canvas)
	{		
		if(trackingMode)
		{
			///TODO: set text size and position depending on screen size
			canvas.drawText(trackingLabel, 5, 25, paintTrack);
		}
		
		//draw pressed key
		if(fixedKey != null)
		{
			fixedKey.draw(canvas);
		}
		
		//draw all keys
		for (KeyboardKey key1 : keyboardMain.values()) 
		{
			key1.draw(canvas);
		}
	}
	
	//initialize list of key positions on the create event
	public void setBounds(Rect screenBounds)
	{		
		if(render == null)
			return;
		
		render.setScreen(screenBounds);
		processKey(null);
	}
	
	public synchronized KeyboardKey findKeyByEvent(MotionEvent event)
	{
		for (KeyboardKey key1 : keyboardMain.values()) 
		{			
			//if we found touched key - stop searching
			if(key1.isEventInBounds(event))
			{
				return key1;
			}
		}
		
		return null;
	}
	
	private synchronized void finishedInput()
	{
		//send result to upper view
		String input = "";
			
		//ask view with result chars to get word
		if(resultsView != null)
		{
			input = resultsView.getWord();
			mTextWatcher.onTextChanged(input, 0, input.length(), input.length());
		}
			
		if((suggest != null) && (input.length() > 0))
		{
			suggest.addStringToUser(input);
		}
	}
	
	//touch event in tracking mode
	private synchronized boolean touchEventTracking(int eventAction)
	{
		//if our event does not touch a key - ignore it
		if((nowTouchedKey == null) && 
				(eventAction != MotionEvent.ACTION_UP) &&
				(eventAction != MotionEvent.ACTION_CANCEL))
		{			
			if(timeoutCall != null)
				timeoutCall.cancel(false);
			
			return false;
		}
		
		//if we detected touch on a key
		if(eventAction == MotionEvent.ACTION_DOWN)
		{
			fixedTouchedKey = null;
			prevTouchedKey = nowTouchedKey;
			
			if(timeoutCall != null)
				timeoutCall.cancel(false);
			
			//start timer here since we could not get more events after moving on a key
			try
			{
				timeoutCall = timer.schedule(new TrackingTimeout(), trackingTimeout, TimeUnit.MILLISECONDS);
			}
			catch(IllegalStateException e)
			{
				return false;
			}
					
			return true;
		}
		
		//if we detected up action - send result string to upper view
		if((eventAction == MotionEvent.ACTION_UP) || (eventAction == MotionEvent.ACTION_CANCEL))
		{			
			if(timeoutCall != null)
				timeoutCall.cancel(false);
			
			finishedInput();
					
			return true;
		}
		
		//if we detected move on a key and pause on it for longKeyTime ms
		if(eventAction == MotionEvent.ACTION_MOVE)
		{				
			//if we changed key before timer expired			
			//fixedTouchedKey = null;
			if(prevTouchedKey == nowTouchedKey)
				return true;
			
			if(timeoutCall != null)
				timeoutCall.cancel(false);
			
			//start timer here since we could not get more events after moving on a key	
			try
			{
				timeoutCall = timer.schedule(new TrackingTimeout(), trackingTimeout, TimeUnit.MILLISECONDS);
			}
			catch(IllegalStateException e)
			{
				return false;
			}
			
			return true;
		}
		
		Log.v("test", "action is " + eventAction);
		
		if(timeoutCall != null)
			timeoutCall.cancel(false);
		
		fixedTouchedKey = null;
		return false;
	}
	
	//обработка прикосновения - пересылаем его всем клавишам
	//возвращает true, если событие принадлежит именно этому
	//объекту
	public synchronized boolean touchEvent(MotionEvent event)
	{		
		nowTouchedKey = findKeyByEvent(event);
		
		if(trackingMode)
		{
			return touchEventTracking(event.getAction());
		}
		
		//if we detected touch on a key
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			fixedTouchedKey = null;
			prevTouchedKey = nowTouchedKey;
			
			if(timeoutCall != null)
				timeoutCall.cancel(false);
			
			//start timer here since we could not get more events after moving on a key
			try
			{
				timeoutCall = timer.schedule(new DefaultTimeout(), trackingTimeout, TimeUnit.MILLISECONDS);
			}
			catch(IllegalStateException e)
			{
				return false;
			}
					
			return true;
		}
		
		//if we detected up action - send result string to upper view
		if((event.getAction() == MotionEvent.ACTION_UP) || (event.getAction() == MotionEvent.ACTION_CANCEL))
		{			
			if(timeoutCall != null)
				timeoutCall.cancel(false);
			
			if(nowTouchedKey == null)
				return true;
				
			//process touched key by GraphKeyboard (draw fixed, add suggestions)
			processKey(nowTouchedKey);
				
			fixedTouchedKey = nowTouchedKey;
			prevTouchedKey = nowTouchedKey;
		}
		
		//if we detected move on a key
		if(event.getAction() == MotionEvent.ACTION_MOVE)
		{			
			return true;
		}
		
		Log.v("test", "action is " + event.getAction());
		
		if(timeoutCall != null)
			timeoutCall.cancel(false);
		
		fixedTouchedKey = null;
		return false;
	}
	
	private void renderAddKey(Rect setBounds, KeyboardKey key1)
	{
		render.addShape(new RenderShape(setBounds, key1.getKeyId(), key1.keyString));
	}
	
	//what to do on key press
	private synchronized void processKey(KeyboardKey pressedKey)
	{		
		//if we selected END key
		if(!trackingMode && (pressedKey != null) && (pressedKey.getKeyId() == KEYBOARD_END_KEYCODE))
		{
			Log.v("test", "END key pressed");
			finishedInput();
			return;
		}
		
		if(render == null)
			return;
		
		render.clear();
		loadDefaultKeys();

		int nonkeys = 0;
		
		if(pressedKey != null)
		{
			Log.v("test", "processKey " + pressedKey.keyString);
		}
		else
		{
			Log.v("test", "processKey: null");
		}
		
		//1. Render pressed key first
		
		if(pressedKey != null)
		{
			Log.v("test", "fixed is " + pressedKey.keyString);
		
			fixedKey = new KeyboardKey(nonkeys + KEYBOARD_USER_INDEX, pressedKey);
		
			fixedKey.paintKey.setColor(Color.RED);
			fixedKey.paintFont.setColor(Color.RED);
			
			//add this key to result word
			if(resultsView != null)
			{
				resultsView.addString(pressedKey.keyString);
			}
			
			RenderShape fixed = new RenderShape(fixedKey.keyShape.getBounds(), fixedKey.getKeyId(), fixedKey.keyString);
			fixed.rendered = true;
			render.addShape(fixed);
			nonkeys++;
		}
		
		if(suggest == null)
			return;
		
		String word = "";
		
		//ask result view for a word
		if(resultsView != null)
		{
			word = resultsView.getWord();
		}
		
		Log.v("test", "start suggest input at " + System.currentTimeMillis());
		
		suggest.input(word, word.length());
		
		Log.v("test", "start suggest output at " + System.currentTimeMillis());
		
		List<String> words = suggest.outputStrings(SuggestionEngine.NUM_SUGGESTED_CHARS);
		String charsResult = suggest.suggestedChars(words, "", word.length());
		String frequentChars = suggest.appendFrequentChars(charsResult, SuggestionEngine.NUM_SUGGESTED_CHARS);
		
		int charsLen = charsResult.length();
		int freqLen = frequentChars.length();
		
		String reversChars = "";
		
		for(int i = 0; i < freqLen; i++)
		{
			reversChars += frequentChars.charAt(freqLen - i - 1);
		}
		
		Log.v("test", "stop suggest at " + System.currentTimeMillis());
		
		//add space for fixed key and suggested words
		int num_keys = keyboardMain.size();
		
		if(fixedKey != null)
		{
			num_keys++;	
		}
		
		if(words.size() <= 2)
		{
			num_keys += words.size();
		}
		
		int side = render.getDesiredSize(num_keys);
		
		//2a. For non-tracking mode add "END" key 
		if(!trackingMode)
		{
			KeyboardKey key1 = keyboardMain.get(KEYBOARD_END_KEYCODE);
			
			if(key1 != null)
			{
				key1.paintFont.setColor(Color.WHITE);
				key1.paintKey.setColor(Color.WHITE);
				
				Rect newBounds = new Rect(0, 0, 
						(int)Math.floor(side * Math.sqrt(key1.keyString.length() / 2)), 
						(int)Math.floor(side * Math.sqrt(key1.keyString.length() / 2)));
				
				renderAddKey(newBounds, key1);
			}
		}
		
		//2. Render words completion
		
		//draw full words here if words.size() <= 2
		if(words.size() <= 2)
		{
			for(int i = 0; i < words.size(); i++)
			{
				String wordStr = words.get(words.size() - i - 1);
						
				if(word.length() >= (wordStr.length() - 1))
					continue;					
						
				wordStr = wordStr.substring(word.length());
						
				KeyboardKey key1 = new KeyboardKey(nonkeys + KEYBOARD_USER_INDEX, wordStr);
					
				key1.paintFont.setColor(Color.WHITE);
				key1.paintKey.setColor(Color.WHITE);
					
				Rect newBounds = new Rect(0, 0, 
								(int)Math.floor(side * Math.sqrt(wordStr.length() / 2)), 
								(int)Math.floor(side * Math.sqrt(wordStr.length() / 2)));
					
				keyboardMain.put(key1.getKeyId(), key1);
				renderAddKey(newBounds, key1);
				nonkeys++;
			}
		}
		
		if(charsResult.length() > 0)
		{
			//3. Render most suitable letters
			
			for(int i = 0; i < charsLen; i++)
			{
				KeyboardKey key1 = this.keyboardMain.get((int)(reversChars.charAt(freqLen - charsLen + i)));
				
				if(key1 == null)
					continue;
				
				key1.paintFont.setColor(Color.WHITE);
				key1.paintKey.setColor(Color.WHITE);
				
				Rect newBounds = new Rect(0, 0, side, side);
				renderAddKey(newBounds, key1);
			}
			
			//4. Render most frequent letters
			
			for(int i = 0; i < (freqLen - charsLen); i++)
			{
				KeyboardKey key1 = this.keyboardMain.get((int)(reversChars.charAt(i)));
				
				if(key1 == null)
					continue;
				
				key1.paintFont.setColor(Color.BLUE);
				key1.paintKey.setColor(Color.BLUE);
				
				Rect newBounds = new Rect(0, 0, (int)(side * 0.8), (int)(side * 0.8));
				renderAddKey(newBounds, key1);
			}
			
			//5. Render rest
			
			for (KeyboardKey key1 : keyboardMain.values()) 
			{				
				if(frequentChars.contains(key1.keyString))
					continue;
				if(key1.getKeyId() >= KEYBOARD_USER_INDEX)
					continue;
				
				key1.paintFont.setColor(Color.GREEN);
				key1.paintKey.setColor(Color.GREEN);
				
				Rect newBounds = new Rect(0, 0, (int)(side * 0.5), (int)(side * 0.5));
				renderAddKey(newBounds, key1);
			}
		}
		else
		{
			//6. Render all letters
			
			//if no suggested chars found
			for (KeyboardKey key1 : keyboardMain.values()) 
			{				
				if(key1.getKeyId() == KEYBOARD_END_KEYCODE)
					continue;
				
				key1.paintFont.setColor(Color.GRAY);
				key1.paintKey.setColor(Color.GRAY);
				
				Rect newBounds = new Rect(0, 0, (int)(side * 0.5), (int)(side * 0.5));
				renderAddKey(newBounds, key1);
			}
		}
		
		Log.v("test", "start render at " + System.currentTimeMillis());
		
		//render is ready to start
		render.start();
		
		List<RenderShape> shapes = render.getShapes();
		
		for (RenderShape s : shapes) 
		{			
			if(s == null)
				continue;
			
			KeyboardKey key = keyboardMain.get(s.id);
			
			if(key == null)
				continue;
			
			key.keyShape.setBounds(new Rect(s.bounds));
		}
		
		Log.v("test", "stop render at " + System.currentTimeMillis());
	}
}
