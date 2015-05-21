/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

package com.rockinhome.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ConfigureConnection extends Activity{

	public final static String TAG = "ConfigureConnection.class";

	EditText receivePortInput, 
	  sendPortInput,
	  hostIPInput,
	  intervalInput;

	Button okInput, 
	  cancelInput;
	
	Intent intent;
	
	UDPConfig uDPConfig;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure_connection);
		
		//collect context for toast message		
		uDPConfig = new UDPConfig();
		
		uDPConfig.getExtras(getIntent().getExtras());
		
		//set View 
		receivePortInput = (EditText)findViewById(R.id.receive_port_input);
		sendPortInput = (EditText)findViewById(R.id.send_port_input);
		intervalInput = (EditText)findViewById(R.id.send_interval_input);
		hostIPInput = (EditText)findViewById(R.id.host_ip_input);
		
		//set values
		receivePortInput.setText(Integer.toString(uDPConfig.receivePort));
		sendPortInput.setText(Integer.toString(uDPConfig.sendPort));
		intervalInput.setText(Integer.toString(uDPConfig.interval));
		if(!uDPConfig.hostIP.contentEquals("")){
			hostIPInput.setText(uDPConfig.hostIP);
		}
		
		//set Buttons
		okInput = (Button)findViewById(R.id.ok_input);
		cancelInput = (Button)findViewById(R.id.cancel_input);
		okInput.setOnClickListener(onClick);
		cancelInput.setOnClickListener(onClick);
		
	}
	
	int editTextViewInput(EditText editText){
		// return -1 for empty string, return the actual value when it is correct.
		String input = editText.getText().toString();
		if(input.contentEquals("")){return -1;};
		return Integer.parseInt(input);
	} 
	
	private OnClickListener onClick = new OnClickListener(){
		@Override
		public void onClick(View view) {
			Intent intent= new Intent();
			switch(view.getId()){
			case R.id.cancel_input:
				setResult(RESULT_CANCELED, intent);
				finish();
				break;
			case R.id.ok_input:
				//collect new value
				uDPConfig.receivePort = editTextViewInput(receivePortInput);
				uDPConfig.sendPort = editTextViewInput(sendPortInput);
				uDPConfig.interval = editTextViewInput(intervalInput);
				uDPConfig.hostIP = hostIPInput.getText().toString();
				intent = uDPConfig.putExtras(intent);
				//set result
				setResult(RESULT_OK, intent);
				finish();
				break;	
			}	
		}
	};

}
