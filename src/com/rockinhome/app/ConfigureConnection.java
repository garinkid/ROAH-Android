/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

package com.rockinhome.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ConfigureConnection extends Activity{

	public final String TAG = "ConfigureConnection.class";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure_connection);

		//collect context for toast message		
		UDPConfig uDPConfig = new UDPConfig();
		uDPConfig.getExtras(getIntent().getExtras());

		EditText receivePortInput = (EditText)findViewById(R.id.receive_port_input);
		EditText sendPortInput = (EditText)findViewById(R.id.send_port_input);
		EditText intervalInput = (EditText)findViewById(R.id.send_interval_input);
		EditText hostIPInput = (EditText)findViewById(R.id.host_ip_input);

		//set values
		receivePortInput.setText(Integer.toString(uDPConfig.receivePort));
		sendPortInput.setText(Integer.toString(uDPConfig.sendPort));
		intervalInput.setText(Integer.toString(uDPConfig.interval));
		if(!uDPConfig.hostIP.contentEquals("")){
			hostIPInput.setText(uDPConfig.hostIP);
		}

		//set Buttons
		Button okInput = (Button)findViewById(R.id.ok_input);
		Button cancelInput = (Button)findViewById(R.id.cancel_input);
		okInput.setOnClickListener(onClick);
		cancelInput.setOnClickListener(onClick);
		
	}

	private int editTextViewInput(EditText editText){
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
				UDPConfig uDPConfig = new UDPConfig();
				uDPConfig.receivePort = editTextViewInput((EditText)findViewById(R.id.receive_port_input));
				uDPConfig.sendPort = editTextViewInput((EditText)findViewById(R.id.send_port_input));
				uDPConfig.interval = editTextViewInput((EditText)findViewById(R.id.send_interval_input));
				uDPConfig.hostIP = ((EditText)findViewById(R.id.host_ip_input)).getText().toString();
				intent = uDPConfig.putExtras(intent);
				//set result
				setResult(RESULT_OK, intent);
				finish();
				break;	
			}	
		}
	};
}
