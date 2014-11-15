package com.rockinhome.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Setting extends Activity{

	EditText receivePortInput, 
	  sendPortInput,
	  hostIPInput,
	  repetitionInput,
	  intervalInput;

	String hostIP;
	
	Button okInput, 
	  cancelInput;
	
	int sendPort,
	  receivePort,
	  interval,
	  repetition;
	
	boolean buttonOkPressed;
	
	Context context;
	
	Intent intent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting);
		
		//collect context for toast message
		context = getBaseContext();
		
		//preference manager to collect previously set values
		intent = getIntent();
		
		//set View 
		receivePortInput = onCreateEditTextView(R.id.receive_port_input, MainActivity.RECEIVE_PORT);
		sendPortInput = onCreateEditTextView(R.id.send_port_input, MainActivity.SEND_PORT);
		repetitionInput = onCreateEditTextView(R.id.send_repetition_input, MainActivity.REPETITION);
		intervalInput = onCreateEditTextView(R.id.send_interval_input, MainActivity.INTERVAL);
		
		//set Buttons
		okInput = (Button)findViewById(R.id.ok_input);
		cancelInput = (Button)findViewById(R.id.cancel_input);
		okInput.setOnClickListener(onClick);
		cancelInput.setOnClickListener(onClick);
		
		// collect preference and set default value
		hostIPInput = (EditText)findViewById(R.id.host_ip_input);
		hostIP = intent.getStringExtra(MainActivity.HOST_IP);
		if(!hostIP.contentEquals("")){hostIPInput.setText(hostIP);}
		// set values 
		receivePort = editTextViewInput(receivePortInput);
		sendPort = editTextViewInput(sendPortInput);
		interval = editTextViewInput(intervalInput);
		repetition =  editTextViewInput(repetitionInput);

	}
	
	protected EditText onCreateEditTextView(int viewId, String referenceKey){		
		// method to find the EditText, set to onFocusChangeListener, set intent value.
		//set View
		EditText view = (EditText)findViewById(viewId);
		//check preference value
		int value = intent.getIntExtra(referenceKey, -1);
		if(value > -1){view.setText(Integer.toString(value));}
		return view;
	}
	
	private OnClickListener onClick = new OnClickListener(){

		@Override
		public void onClick(View view) {
			Intent returnIntent = new Intent();
			switch(view.getId()){
			case R.id.cancel_input:
				setResult(RESULT_CANCELED,returnIntent);
				finish();
				break;
			case R.id.ok_input:
				//collect new value
				receivePort = editTextViewInput(receivePortInput);
				sendPort = editTextViewInput(sendPortInput);
				interval = editTextViewInput(intervalInput);
				repetition =  editTextViewInput(repetitionInput);
				hostIP = hostIPInput.getText().toString();
				
				//collect setting information to intent
				returnIntent.putExtra(MainActivity.RECEIVE_PORT, receivePort);
				returnIntent.putExtra(MainActivity.SEND_PORT, sendPort);
				returnIntent.putExtra(MainActivity.HOST_IP, hostIP);
				returnIntent.putExtra(MainActivity.INTERVAL, interval);
				returnIntent.putExtra(MainActivity.REPETITION, repetition);
				
				//set result
				setResult(RESULT_OK, returnIntent);
				finish();
				break;	
			}	
		}
	};
	

	int editTextViewInput(EditText editText){
		// return -1 for empty string, return the actual value when it is correct.
		String input = editText.getText().toString();
		if(input.contentEquals("")){return -1;};
		return Integer.parseInt(input);
	} 
}
