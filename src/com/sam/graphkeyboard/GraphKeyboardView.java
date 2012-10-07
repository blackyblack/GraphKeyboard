package com.sam.graphkeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/*
 * Working field of our keyboard
 */
public class GraphKeyboardView extends View
{
	Paint paintBck;
	//helper field to store bounds of the view
	private Rect layoutBounds;
    
	//use this layout to place keys of the keyboards on the screen
    private GraphKeyboard keyboard = null;
	
    //Create here frequently used objects (paints and fonts)
	public GraphKeyboardView(Context context, AttributeSet attr)
	{
		super(context, attr);
		
		paintBck = new Paint();
		paintBck.setColor(Color.BLACK);
		paintBck.setStyle(Style.FILL);
		paintBck.setAlpha(100);
		
		layoutBounds = new Rect();
	}
	
	public void setKeyboard(GraphKeyboard keyboard)
	{
		layoutBounds.set(0, 0, this.getWidth(), this.getHeight());
		
		this.keyboard = keyboard;
		
		if(keyboard != null)
		{
			this.keyboard.setBounds(layoutBounds);
		}
	}
	
	@Override
	//Redirect touch event to every key of keyboard and check for a returned
	//char
	public boolean onTouchEvent(MotionEvent event)
	{		
		if(keyboard != null)
		{
			keyboard.touchEvent(event);
		}

		return true;
	}
	
	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom)
	{		
		super.onLayout(changed, left, top, right, bottom);
		
		layoutBounds.set(0, 0, this.getWidth(), this.getHeight());
		
		if(keyboard != null)
		{
			keyboard.setBounds(layoutBounds);
		}
		
		Log.v("test", "bounds " + layoutBounds.toString());
	}
     
	@Override
	//Redirect draw event to every key of keyboard. Also draw background here
	protected synchronized void onDraw(Canvas canvas)
	{		
		//закрасить фон
		canvas.drawPaint(paintBck);
		
		if(keyboard != null)
		{
			keyboard.draw(canvas);
		}
		
		invalidate();
	}
}