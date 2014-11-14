package com.rockinhome.app;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.robocup_logistics.llsf_msgs.BeaconSignalProtos.BeaconSignal;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Descriptors.EnumDescriptor;

import eu.rockin.roah_rsbb_msgs.RoahRsbbBeaconProtos.RoahRsbbBeacon;
import eu.rockin.roah_rsbb_msgs.RobotBeaconProtos.RobotBeacon;
import eu.rockin.roah_rsbb_msgs.TabletBeaconProtos.TabletBeacon;
import eu.rockin.roah_rsbb_msgs.TimeProtos.Time;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public final static String RECEIVE_PORT="receive port",
			  SEND_PORT="send port",
			  HOST_IP="host ip",
			  TIMEOUT="time out",
			  TRIAL="trial",
			  INTERVAL="interval",
			  REPETITION="repetition",
			  TAG = "Main activity",
			  ACTIVITY_LOG="activity log";
	
	public final static int BUTTON_CALL = 1,
			MAP_CALL = 2;

	int sendPort,
	  receivePort,
	  interval,
	  repetition,
	  trial,
	  timeout;

	boolean connectionFlag,
	  listening;
	
	static final int SETTING_REQUEST = 1;
	
	double viewToMapXScale,
	  viewToMapYScale,
	  mapXsize = 9.0, // in meter
	  mapYsize = 6.0; // in meter
	
	String hostIP;
	
	TextView activityLog;
	
	Button setting, 
	  listen, 
	  callRobot,
	  map;
	
	ImageView mapView;
	
	Context context;
	
	Time lastCallTime,
	  lastPoseTime;
	
	IntentFilter intentFilterPackage,
	  intentFilterLog;
	
	UDPReceiverService uDPReceiverService;
	
	UDPSenderService uDPSenderService;
	
	SharedPreferences preferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getBaseContext();

		//collect previously set values
		preferences =  PreferenceManager.getDefaultSharedPreferences(this);
		//In the case where it is decided to use previous config as default config
		hostIP = preferences.getString(MainActivity.HOST_IP, "");
		sendPort = preferences.getInt(MainActivity.SEND_PORT, -1);
		repetition = preferences.getInt(MainActivity.REPETITION, -1);
		interval = preferences.getInt(MainActivity.INTERVAL, -1);
		receivePort = preferences.getInt(MainActivity.RECEIVE_PORT, -1);
		timeout = preferences.getInt(MainActivity.TIMEOUT, -1);
		trial = preferences.getInt(MainActivity.TRIAL, -1);
		
		//using predefine value as config:
		/*
		hostIP = "10.255.255.255";
		sendPort = 6666;
		repetition = 1; // not being used TODO  eliminate
		interval = 1000;
		receivePort = 6666;
		timeout = 0;  // Always listening
		trial = 1; 
		*/
		informUserSetting();
		
		//set buttons
		setting = (Button)findViewById(R.id.setting_button);
		callRobot = (Button)findViewById(R.id.call_robot_button);
		listen = (Button)findViewById(R.id.listen_button);
		map = (Button)findViewById(R.id.map_button);
		map.setOnClickListener(onClick);
		setting .setOnClickListener(onClick);
		callRobot.setOnClickListener(onClick);
		listen.setOnClickListener(onClick);
		
		//set map view
		mapView = (ImageView)findViewById(R.id.map);
		mapView.setOnTouchListener(onTouch);
		
		//hide map by default
		mapView.setVisibility(View.INVISIBLE);

		activityLog = (TextView)findViewById(R.id.log);

		intentFilterPackage = new IntentFilter(UDPReceiverService.RECEIVED_PACKAGE);
		intentFilterLog = new IntentFilter(ACTIVITY_LOG);
		listening=false;
		
		uDPReceiverService = new UDPReceiverService(getBaseContext());
		uDPSenderService = new UDPSenderService(getBaseContext());

		
		//set last time to zero the app is activated
		lastCallTime = Time.newBuilder().setSec(0).setNsec(0).build();
		lastPoseTime = Time.newBuilder().setSec(0).setNsec(0).build();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected void onResume() {
		super.onResume();
		registerReceiver(packageReceiver, intentFilterPackage);
		registerReceiver(logReceiver, intentFilterLog);
	}

	@Override
	protected void onPause() {
		unregisterReceiver(packageReceiver);
		unregisterReceiver(logReceiver);
		super.onPause();
	}

	@Override
	protected void onStop(){
		super.onStop();
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(MainActivity.RECEIVE_PORT, receivePort);
		editor.putInt(MainActivity.INTERVAL, interval);
		editor.putInt(MainActivity.SEND_PORT, sendPort);
		editor.putInt(MainActivity.REPETITION, repetition);
		editor.putInt(MainActivity.TIMEOUT, timeout);
		editor.putInt(MainActivity.TRIAL, trial);
		editor.putString(MainActivity.HOST_IP,hostIP);
		editor.commit();
		Log.d("Save state", "State saved");
	}

	
	private OnClickListener onClick = new OnClickListener(){
		@Override
		public void onClick(View view) {
			Intent intent;
			switch(view.getId()){
			case R.id.listen_button:
				//Check application status
				if(listening){
					uDPReceiverService.interrupt();
					listen.setText("Listen");
					listening = false;
				}else{
					if(receivePort > 0){
						intent = new Intent(context, UDPReceiverService.class);
						intent.putExtra(MainActivity.TIMEOUT, timeout);
						intent.putExtra(MainActivity.TRIAL, trial);
						intent.putExtra(MainActivity.RECEIVE_PORT,receivePort);
						uDPReceiverService.run(intent);
						//startService(intent);
						listening = true;
						listen.setText("Stop");
					}else{
						informUser("No receive port has been defined");
					}
				}
				break;
			case R.id.call_robot_button:
				Log.d(TAG, "Call robot button is pressed");
				byte[] message = createTabletBeaconMessage(0.0, 0.0, BUTTON_CALL);
				Log.d(TAG, "size result:" + message.length);
				sendMessage(message);
				break;	
			case R.id.setting_button:
				intent = new Intent(context, Setting.class);
				intent.putExtra(MainActivity.RECEIVE_PORT, receivePort);
				intent.putExtra(MainActivity.SEND_PORT, sendPort);
				intent.putExtra(MainActivity.HOST_IP, hostIP);
				intent.putExtra(MainActivity.INTERVAL, interval);
				intent.putExtra(MainActivity.TIMEOUT, timeout);
				intent.putExtra(MainActivity.TRIAL, trial);
				intent.putExtra(MainActivity.REPETITION, repetition);
				startActivityForResult(intent, SETTING_REQUEST);
				break;
			case R.id.map_button:
				if(mapView.isShown()){
					mapView.setVisibility(View.INVISIBLE);
					map.setText("Show map");
				}else{
					mapView.setVisibility(View.VISIBLE);
					map.setText("Hide map");
				}
				break;
				
			}
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Check which request we're responding to
		if (requestCode == SETTING_REQUEST) {
			// Make sure the request was successful
			if (resultCode == RESULT_OK) {
				hostIP = intent.getStringExtra(MainActivity.HOST_IP);
				sendPort = intent.getIntExtra(MainActivity.SEND_PORT, -1);
				repetition = intent.getIntExtra(MainActivity.REPETITION, -1);
				interval = intent.getIntExtra(MainActivity.INTERVAL, -1);
				receivePort = intent.getIntExtra(MainActivity.RECEIVE_PORT, -1);
				timeout = intent.getIntExtra(MainActivity.TIMEOUT, -1);
				trial = intent.getIntExtra(MainActivity.TRIAL, -1);
				informUserSetting();
			}
		}
	}
	
	protected byte[] createTabletBeaconMessage(double x, double y, int method) {
		TabletBeacon robotCall = TabletBeacon.newBuilder()
		  .setLastCall(lastCallTime)
		  .setLastPos(lastPoseTime)
		  .setX(x)
		  .setY(y).build();
		
		Log.d(TAG, "TabletBeacon: x=" +  x + ",y=" + y + ",lastCall=" + lastCallTime.getSec() +
		  ",lastPose" + lastPoseTime.getSec());
		
		//set last call
		long unixTime = System.currentTimeMillis();
		long sec = unixTime / 1000L;
		long nsec = (unixTime%1000L) * 1000;
		if(method == BUTTON_CALL){
			lastCallTime = Time.newBuilder().setSec(sec).setNsec(nsec).build();
		}else{
			lastPoseTime = Time.newBuilder().setSec(sec).setNsec(nsec).build();
		}
		
		//Serialize the message
		//12 extra bytes for: 
		//4 bytes for frame_header_version 
		//4 bytes for message byte length + COMP_ID + MSG_TYPE
		//2 bytes for COMP_ID
		//2 bytes for MSG_TYPE
		int size = robotCall.getSerializedSize();
		ByteBuffer buf = ByteBuffer.allocateDirect(size + 12).order(ByteOrder.BIG_ENDIAN);
		EnumDescriptor desc = TabletBeacon.getDescriptor().findEnumTypeByName("CompType");
		int cmp_id = desc.findValueByName("COMP_ID").getNumber();
		int msg_id = desc.findValueByName("MSG_TYPE").getNumber();
		buf.putInt(2);
		buf.putInt(size + 4);
		buf.putShort((short)cmp_id);
		buf.putShort((short)msg_id);
		buf.put(robotCall.toByteArray());		
		buf.rewind();
		return buf.array();
	}
	
	private void sendMessage(byte[] message){
		Intent intent = new Intent(context, UDPSenderService.class);
		intent.putExtra(MainActivity.SEND_PORT,sendPort);
		intent.putExtra(MainActivity.HOST_IP,hostIP);
		intent.putExtra(MainActivity.INTERVAL,interval);
		intent.putExtra(MainActivity.REPETITION, repetition);
		intent.putExtra(UDPSenderService.MESSAGE, message);
		uDPSenderService.run(intent);
	}

	protected void informUserSetting(){
		// Collect value
		String message = "Configuration: \n";
		if(!hostIP.contentEquals("")){message += "Host: " + hostIP + "\n";}
		if(sendPort > 0){message += "Send port: " + sendPort + "\n";}
		if(repetition > 0){message += "Send repetition: " + repetition + "\n";}
		if(interval > 0){message += "Send interval: " + interval + " (ms) \n";}
		if(receivePort > 0){message += "Receive port: " + receivePort + "\n";}
		if(timeout > 0){message += "Receive timeout: " + timeout + " (ms) \n";}
		if(trial > 0){message += "Receive trial: " + trial;}
		
		//check whether any value has been set
		if(message.contentEquals("Configuration: \n")){message = "No configuration";}
		
		//inform user
		informUser(message);
	}
	
	protected void informUser(String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	private void writeToLog(String str){
		activityLog.setText(str + " at " + 
		  DateFormat.format("dd.MM.yyyy kk:mm:ss ", System.currentTimeMillis()));
	}
	
	OnTouchListener onTouch = new OnTouchListener(){
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch(v.getId()){
			case R.id.map:
				Log.d(TAG, "height:" + mapView.getHeight() + "width:" + mapView.getWidth());
				viewToMapXScale = mapXsize / mapView.getWidth();
				viewToMapYScale = mapYsize / mapView.getHeight();
				double xCoordinate = event.getX() * viewToMapXScale; 
				double yCoordinate = event.getY()* viewToMapYScale;
				byte[] message = createTabletBeaconMessage(xCoordinate, yCoordinate, MAP_CALL);
				sendMessage(message);
				break;
			}
			return false;
		}
		
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	BroadcastReceiver packageReceiver = new BroadcastReceiver () {
		@Override
		public void onReceive(Context context, Intent intent) {
			String result = intent.getStringExtra(UDPReceiverService.RESULT);
			Log.d(TAG, result);
			if(result.contentEquals(UDPReceiverService.SUCCESS)){
				byte[] receivedByte = intent.getByteArrayExtra(UDPReceiverService.RECEIVED_PACKAGE);
				DatagramPacket datagramPacket = new DatagramPacket(receivedByte, receivedByte.length);
				Log.d("MAIN", "packageReceived");
				try {
					translatePacket(datagramPacket);
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
			}else{
				listen.setText("Listen");
				listening = false;
			}
		}
	};
	
	BroadcastReceiver logReceiver = new BroadcastReceiver () {
		@Override
		public void onReceive(Context context, Intent intent) {
			String message = intent.getStringExtra(MainActivity.ACTIVITY_LOG);
			writeToLog(message);
		}
	};
	
	
	protected void translatePacket(DatagramPacket packetData) throws InvalidProtocolBufferException{
		byte[] header = new byte[12];
		String messageType;
		System.arraycopy(packetData.getData(), 0, header, 0, 12);
		ByteBuffer headerBuff = ByteBuffer.wrap(header);
		headerBuff.rewind();
		//for(int i = 0; i<8; i++){Log.d("Read Packet", "Data at: " + i + "->"+ uDPHeader[i]);};
		//Log.d("Read Packet", "Cheksum: " + uDPHeaderBuf.getShort(6));
		//get packet length from checksum
		int frameHeaderVersion = headerBuff.getInt(); //not being used for now 
		int size = headerBuff.getInt() - 4; 
		int cmpId = headerBuff.getShort();
		int msgId = headerBuff.getShort();
		//4 bytes are used for cmp_id, msg_id
		//8 bytes are used for udp header;
		if(size == 0){
			Log.d("UDP", "message size 0");
			return;
		}
		Log.d("UDP", "Size:" + size + ", cmp_id:" + cmpId + ", msg_id: " + msgId);
		if(size < 0){return;} // TODO usually happens when simultaneously sending and receiving 
		byte[] protobuf = new byte[size];
		System.arraycopy(packetData.getData(), 12, protobuf, 0, size);
		//message is RobotBeacon from eu.rockin.roah_rsbb_msgs
		if(msgId == 30){
			messageType = "RobotBeacon"; 
			RobotBeacon robotBeacon = RobotBeacon.parseFrom(protobuf);
			Log.d("UDP", "robot name: " + robotBeacon.getRobotName().toString() + 
				", team name: " + robotBeacon.getTeamName().toString() +
				", time: " + robotBeacon.getTime().toString());
		}else if(msgId ==1){
			//message is BeaconSignal from org.robocup_logistic.llsf_msgs
			messageType = "BeaconSignal"; 
			BeaconSignal beaconSignal = BeaconSignal.parseFrom(protobuf);
			Log.d("UDP", "robot name: " + beaconSignal.getTeamName());
		}else if(msgId == 10){
			//message is RoahRsbbBeacon from eu.rockin.roah_rsbb_msgs
			messageType = "RoahRsbbBeacon"; 
			RoahRsbbBeacon roahRsbbBeacon = RoahRsbbBeacon.parseFrom(protobuf);
			boolean showMap = roahRsbbBeacon.getTabletDisplayMap();
			if(showMap){
				mapView.setVisibility(View.VISIBLE);
			}else{
				mapView.setVisibility(View.INVISIBLE);
			}
		}else{
			//unknown message
			messageType = "Unknown message"; 
			Log.d("UDP", "unknown message id: " + msgId);
		}
		writeToLog("received " + messageType);
	}
}
