package com.sam.graphkeyboard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MySeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {
	private int value;
	private SeekBar picker;
	private TextView textView;
	
	public final static int DEFAULT_TRACKING_DELAY = 200;
	
	public MySeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.settings_dialog_delay);
	}
	
	@Override
	  protected View onCreateDialogView() {
		View v = super.onCreateDialogView();
		picker = (SeekBar)v.findViewById(R.id.seekBar1);
		textView = (TextView)v.findViewById(R.id.textView1);
		picker.setOnSeekBarChangeListener(this);
	    return v;
	  }

	  @Override
	  public void onDismiss(DialogInterface dialog) {
	    value = picker.getProgress();
	    super.onDismiss(dialog);
	  }

	  @Override
	  protected void onBindDialogView(View view) {
	    super.onBindDialogView(view);
	    picker.setProgress(value);
	    textView.setText(Integer.toString(value));
	  }

	  @Override
	  protected Object onGetDefaultValue(TypedArray a, int index) {
	    return Integer.parseInt(a.getString(index));
	  }

	  @Override
	  ///TODO: set default value from external source
	  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
	    if (restorePersistedValue) {
	      setValue(getPersistedInt(DEFAULT_TRACKING_DELAY));
	    } else {
	      setValue(DEFAULT_TRACKING_DELAY);
	    }
	  }

	  void setValue(int value) {
	    persistInt(value);

	    notifyDependencyChange(false);
	    this.value = value;
	  }

	  @Override
	  protected void onDialogClosed(boolean positiveResult) {
	    super.onDialogClosed(positiveResult);

	    if (positiveResult) {
	      if (callChangeListener(value)) {
	        setValue(value);
	      }
	    }
	  }

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		textView.setText(Integer.toString(progress));
		// TODO Auto-generated method stub
		
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
}
