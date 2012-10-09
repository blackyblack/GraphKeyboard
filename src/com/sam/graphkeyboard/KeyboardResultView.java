package com.sam.graphkeyboard;

import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/*
 * Working field of our keyboard
 */
public class KeyboardResultView extends View
{
	Paint paintBck;
	//this is the result word of the keyboard
	///HACK: use CopyOnWriteArrayList since it is thread-safe on iterators
	private CopyOnWriteArrayList<KeyboardKey> resultWord;
	//bounds of this view
	private Rect viewBounds;
	
	//height of the view
	final static int viewHeightPx = 40;
	//divider between elements
	final static int dividerPx = 2;
	
    //Create here frequently used objects (paints and fonts)
	public KeyboardResultView(Context context, AttributeSet attr)
	{
		super(context, attr);
		
		paintBck = new Paint();
		paintBck.setColor(Color.BLACK);
		paintBck.setStyle(Style.FILL);
		paintBck.setAlpha(100);
		
		viewBounds = new Rect();
		
		resultWord = new CopyOnWriteArrayList<KeyboardKey>();
	}
	
	//add char to the result word
	public void addChar(char value)
	{
		addString("" + value);
	}
	
	//add string to the result word
	public void addString(String value)
	{
		for(int i = 0; i < value.length(); i++)
		{
			KeyboardKey a = new KeyboardKey(i, "" + value.charAt(i));
			
			a.paintFont.setColor(Color.RED);
			a.paintKey.setColor(Color.RED);
			
			resultWord.add(a);
		}
			
		alignChars();
	}
	
	//acquire result word
	public String getWord()
	{
		String word = "";
		
		for (KeyboardKey key1 : resultWord) 
		{			
			word += key1.keyString;
		}
		
		return word;
	}
	
	public void clear()
	{
		resultWord.clear();
	}
	
	//align chars on the view so keys should fit the view bounds
	private void alignChars()
	{
		int wordLength = resultWord.size();
		
		if(wordLength == 0)
			return;
		
		int widthKeySize = viewBounds.width() / wordLength;
		int heightKeySize = viewBounds.height() - dividerPx * 2;
		
		int keySize = Math.min(widthKeySize, heightKeySize);
		
		int position = 0;
		
		for (KeyboardKey key1 : resultWord) 
		{			
			Rect keyBounds = key1.keyShape.copyBounds();
			
			keyBounds.left = position + dividerPx;
			keyBounds.bottom = viewBounds.height() - dividerPx;
			keyBounds.right = position + keySize + dividerPx;
			keyBounds.top = viewBounds.height() - keySize - dividerPx;
			
			key1.keyShape.setBounds(keyBounds);
			
			position += keySize;
		}
	}
	
	@Override
	//Redirect touch event to every key on this view
	///TODO: now does not anything.
	///      will remove char from result word after long touch
	public boolean onTouchEvent(MotionEvent event)
	{		
		//TODO: send event to every key on the view
		return true;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = View.MeasureSpec.getSize(widthMeasureSpec);
		//int height = View.MeasureSpec.getSize(heightMeasureSpec);		
		setMeasuredDimension(width, viewHeightPx);
	}
	
	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		
		viewBounds.set(0, 0, this.getWidth(), this.getHeight());
		
		alignChars();
	}
     
	@Override
	//Redirect draw event to every key of keyboard. Also draw background here
	//synchronized because of List<> use
	protected void onDraw(Canvas canvas)
	{
		canvas.drawPaint(paintBck);
		
		//paint all chars
		for (KeyboardKey key1 : resultWord) 
		{
			key1.draw(canvas);
		}
		
		invalidate();
	}
}
