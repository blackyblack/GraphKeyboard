package com.sam.graphkeyboard;

import android.inputmethodservice.InputMethodService;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.sam.render.RenderEngine;
import com.sam.suggestion.SuggestionEngine;

public class GraphKeyboardService extends InputMethodService implements TextWatcher
{
	private GraphKeyboardView mView = null;
	private KeyboardResultView resultView = null;
	private GraphKeyboard mKeyboard = null;
	private SuggestionEngine suggest = null;
	private RenderEngine render = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		suggest = new SuggestionEngine(this);        
        render = new RenderEngine();
	}
	
	@Override
	public void onDestroy() {
		suggest.close();
		super.onDestroy();
	}
	
	@Override
	public void onWindowHidden() {
		super.onWindowHidden();
		suggest.close();
	}
	
	@Override
	public void onWindowShown() {		
		super.onWindowShown();

		suggest.open();
		
        mKeyboard = new GraphKeyboard(getBaseContext());
        
        mKeyboard.setSuggestionEngine(suggest);
		mKeyboard.setRenderEngine(render);
		mKeyboard.setTextWatcher(this);
		
		render.clear();
		
		mKeyboard.setResultsView(resultView);
		mView.setKeyboard(mKeyboard);
	}
	
	@Override
    public View onCreateInputView() 
	{
		View x = getLayoutInflater().inflate(R.layout.main, null);
		mView = (GraphKeyboardView)x.findViewById(R.id.view1);
		resultView = (KeyboardResultView)x.findViewById(R.id.view_result);
		
	    return x;
	}

	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
		
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	//catch here words from keyboard
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		this.getCurrentInputConnection().commitText(s + " ", 1);
		//hide keyboard to let user see input field
		requestHideSelf(0);
	}
}