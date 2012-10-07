package com.sam.graphkeyboard;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.MotionEvent;

/*
 * A single key on the screen. Default is round green key.
 * Can be moved or resized with keyShape property. Text is resized
 * automatically on draw.
 * Can be checked for touch event with isTouchEvent function
 */

public class KeyboardKey
{
	final static int defaultSide = 40;
	
	//string to draw on the key
	public String keyString;
	//shape of the key
	public ShapeDrawable keyShape;
	
	//how to draw a key
	public Paint paintKey;
	//how to draw a text
	public Paint paintFont;
	
	public KeyboardKey()
	{
		paintKey = new Paint();
		paintKey.setColor(Color.GREEN);
		paintKey.setStyle(Style.STROKE);
		paintKey.setStrokeWidth(2);
		
		paintFont = new Paint();
		paintFont.setColor(Color.GREEN);
		paintFont.setStyle(Style.FILL);
		paintFont.setTextSize(defaultSide * 2);
		
		keyShape = new ShapeDrawable(new OvalShape());
		keyShape.setBounds(0, 0, defaultSide, defaultSide);
	}
	
	//create key with round shape as default
	public KeyboardKey(String keyString)
	{
		this();
			
		this.keyString = keyString;
	}
	
	//copy constructor
	public KeyboardKey(KeyboardKey key)
	{
		this();
		
		keyString = new String(key.keyString);
		
		keyShape = new ShapeDrawable();
		try {
			keyShape.setShape(key.keyShape.getShape().clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		keyShape.setBounds(key.keyShape.copyBounds());
	}
	
	//return true if event should touch our key
	public boolean isTouchEvent(MotionEvent event)
	{		
		//if event is not in our bounds
		if(!isEventInBounds(event))
			return false;
		
		//if event is down or move - key is touched
		if((event.getAction() == MotionEvent.ACTION_DOWN) || 
				(event.getAction() == MotionEvent.ACTION_MOVE))
		{
			return true;
		}
			
		return false;
	}
	
	//return true if event is in bounds of the key
	public boolean isEventInBounds(MotionEvent event)
	{		
		if(!keyShape.isVisible())
			return false;
		
		Rect bounds = keyShape.getBounds();

		int X = (int)event.getX();
		int Y = (int)event.getY();
			
		///TODO: Check for Shape intersection, not just Rect
		if(bounds.contains(X, Y))
		{
			return true;
		}
				
		return false;
	}
	
	public void draw(Canvas canvas)
	{		
		Rect bounds = keyShape.getBounds();
		float[] textWidths = new float[keyString.length()];

		int side = (int)Math.ceil(Math.sqrt(keyString.length())); 
		if(side == 0)
			side = 1;
		
		while(true)
		{
			paintFont.setTextSize(bounds.height() / side);
			paintFont.getTextWidths(keyString, textWidths);
			float textHeight = Math.abs(paintFont.ascent() + paintFont.descent());
		
			bounds = keyShape.getBounds();
		
			float centerX = bounds.exactCenterX();
			float centerY = bounds.exactCenterY();
		
			float textWidth = 0;
		
			for(int i = 0 ; i < textWidths.length; i++)
			{
				textWidth += textWidths[i];
			}
			
			if((paintFont.getTextSize() > 4) && (textWidth > bounds.width()))
			{
				side++;
				continue;
			}

			keyShape.getPaint().set(paintKey);
			keyShape.draw(canvas);
			canvas.drawText(keyString, 
				centerX - (textWidth / 2), 
				centerY + (textHeight / 2), paintFont);
			
			break;
		}
	}
}
