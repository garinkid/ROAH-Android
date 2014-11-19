package com.rockinhome.app;

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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
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
			  INTERVAL="interval",
			  TAG = "Main activity",
			  ACTIVITY_LOG="activity log";
	
	public final static int BUTTON_CALL = 1,
			MAP_CALL = 2,
			RESUME = 3;
	
	public final static int DEFAULT_RECEIVE_PORT = 6666;

	int sendPort,
	  receivePort,
	  interval,
	  repetition;
	
	double tabletBeaconX,
	  tabletBeaconY;
	
	byte[] message;

	boolean bitmapFlag,
	  listening;
	
	static final int SETTING_REQUEST = 1;
	
	String hostIP;
	
	TextView activityLog;
	
	Button callRobot;
	
	ImageView mapView;
	
	Context context;
	
	Time lastCallTime,
	  lastPoseTime;
	
	IntentFilter intentFilterPackage,
	  intentFilterLog;
	
	UDPReceiverService uDPReceiverService;
	
	UDPSenderService uDPSenderServiceContinuous;
	
	SharedPreferences preferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getBaseContext();

		//collect previously set values
		preferences =  PreferenceManager.getDefaultSharedPreferences(this);
		//In the case where it is decided to use previous config as default config
		/*
		hostIP = preferences.getString(MainActivity.HOST_IP, "");
		sendPort = preferences.getInt(MainActivity.SEND_PORT, -1);
		repetition = preferences.getInt(MainActivity.REPETITION, -1);
		interval = preferences.getInt(MainActivity.INTERVAL, -1);
		receivePort = preferences.getInt(MainActivity.RECEIVE_PORT, -1);
		timeout = preferences.getInt(MainActivity.TIMEOUT, -1);
		trial = preferences.getInt(MainActivity.TRIAL, -1);
		*/
		//using predefine value as config:
		hostIP = "10.255.255.255";
		sendPort = 6666;
		repetition = 1; // not being used TODO  eliminate
		interval = 1000;
		receivePort = DEFAULT_RECEIVE_PORT;
		informUserSetting();
		
		//set buttons
		callRobot = (Button)findViewById(R.id.call_robot_button);
		callRobot.setOnClickListener(onClick);
		
		//set map view
		mapView = (ImageView)findViewById(R.id.map);
		mapView.setOnTouchListener(onTouch);
		
		//hide map by default
		mapView.setVisibility(View.INVISIBLE);

		activityLog = (TextView)findViewById(R.id.log);
		activityLog.setVisibility(View.GONE);

		intentFilterPackage = new IntentFilter(UDPReceiverService.RECEIVED_PACKAGE);
		intentFilterLog = new IntentFilter(ACTIVITY_LOG);
		listening=false;
		
		uDPReceiverService = new UDPReceiverService(getBaseContext());
		uDPSenderServiceContinuous = new UDPSenderService(getBaseContext());
		
		//set last time to zero the app is activated
		lastCallTime = Time.newBuilder().setSec(0).setNsec(0).build();
		lastPoseTime = Time.newBuilder().setSec(0).setNsec(0).build();
		
		//TabletBeacon coordinate when the app is started;
		tabletBeaconX=0;
		tabletBeaconY=0;
		
		interval = 1000;
		
		//set bitmap false;
		bitmapFlag = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch(id){
		case R.id.action_settings:
			Intent intent = new Intent(context, Setting.class);
			intent.putExtra(MainActivity.RECEIVE_PORT, receivePort);
			intent.putExtra(MainActivity.SEND_PORT, sendPort);
			intent.putExtra(MainActivity.HOST_IP, hostIP);
			intent.putExtra(MainActivity.INTERVAL, interval);
			startActivityForResult(intent, SETTING_REQUEST);
			return true;
		case R.id.action_show_hide_map:
			if(mapView.isShown()){
				mapView.setVisibility(View.INVISIBLE);
			}else{
				mapView.setVisibility(View.VISIBLE);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected void onResume() {
		super.onResume();
		registerReceiver(packageReceiver, intentFilterPackage);
		registerReceiver(logReceiver, intentFilterLog);
		
		//stop currently running thread
		uDPReceiverService.interrupt();
		uDPSenderServiceContinuous.interrupt();
		//start new thread to listen with the new port
		uDPReceiverService.run(receivePort);
		message =  createTabletBeaconMessage(RESUME);
		uDPSenderServiceContinuous.run(hostIP, sendPort, interval, 0, message);
	}

	@Override
	protected void onPause() {
		unregisterReceiver(packageReceiver);
		unregisterReceiver(logReceiver);
		//stop currently running thread
		uDPReceiverService.interrupt();
		uDPSenderServiceContinuous.interrupt();
		super.onPause();
	}

	@Override
	protected void onStop(){
		super.onStop();
	}

	
	private OnClickListener onClick = new OnClickListener(){
		@Override
		public void onClick(View view) {
			switch(view.getId()){
			case R.id.call_robot_button:
				Log.d(TAG, "Call robot button is pressed");
				setTabletBeaconCoordinate(0, 0);
				byte[] message = createTabletBeaconMessage(BUTTON_CALL);
				Log.d(TAG, "size result:" + message.length);
				uDPSenderServiceContinuous.interrupt();
				uDPSenderServiceContinuous.run(hostIP, sendPort, interval, 0, message);
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
				interval = intent.getIntExtra(MainActivity.INTERVAL, 1000);
				receivePort = intent.getIntExtra(MainActivity.RECEIVE_PORT, DEFAULT_RECEIVE_PORT);
				informUserSetting();
			}
		}
	}
	
	protected void setTabletBeaconCoordinate(double x, double y){
		tabletBeaconX = x;
		tabletBeaconY = y;
	}
	
	protected byte[] createTabletBeaconMessage(int method) {
		
		//set last call
		long unixTime = System.currentTimeMillis();
		long sec = unixTime / 1000L;
		long nsec = (unixTime%1000L) * 1000;
		if(method == BUTTON_CALL){
			lastCallTime = Time.newBuilder().setSec(sec).setNsec(nsec).build();
		}else if(method == MAP_CALL){
			lastPoseTime = Time.newBuilder().setSec(sec).setNsec(nsec).build();
		}else if(method == RESUME){
			//do nothing
		}
		
		TabletBeacon robotCall = TabletBeacon.newBuilder()
		  .setLastCall(lastCallTime)
		  .setLastPos(lastPoseTime)
		  .setX(tabletBeaconX)
		  .setY(tabletBeaconY).build();
		
		Log.d(TAG, "TabletBeacon: x=" +  tabletBeaconX + ",y=" + tabletBeaconY + ",lastCall=" + lastCallTime.getSec() +
		  ",lastPose" + lastPoseTime.getSec());
		
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

	protected void informUserSetting(){
		// Collect value
		String message = "Configuration: \n";
		if(!hostIP.contentEquals("")){message += "Host: " + hostIP + "\n";}
		if(sendPort > 0){message += "Send port: " + sendPort + "\n";}
		if(interval > 0){message += "Send interval: " + interval + " (ms) \n";}
		if(receivePort > 0){message += "Receive port: " + receivePort;}
		
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
				
			    double mapLength =  9; //meter
			    double mapHeight = 9; //meter
			    double mapOffsetLength = 4.0; //meter
			    double mapOffsetHeight = 5.0; //meter
				double viewToMapLengthScale = mapLength / mapView.getWidth();
				double viewToMapHeightScale = mapHeight / mapView.getHeight();
				
				float touchX =  event.getX();
				float touchY = event.getY();
				
				//debugging
				
				//touchX = (float) (5.0 / viewToMapLengthScale);
				//touchY = (float) (4.0 / viewToMapHeightScale);
				
				//update image
				mapView.setImageResource(R.drawable.defaultmap);
				mapView.buildDrawingCache();
				Bitmap mapBitmap = mapView.getDrawingCache();
				Canvas canvas = new Canvas(mapBitmap);
			    Paint paint = new Paint();
			    paint.setColor(Color.RED);
			    canvas.drawCircle(touchX, touchY, (float)30 , paint);
			    mapView.setImageDrawable(null);
			    mapView.setImageDrawable(new BitmapDrawable(getResources(), mapBitmap));

			    //send message
				double xCoordinate = mapHeight - mapOffsetHeight - (event.getY() * viewToMapHeightScale);
				double yCoordinate = mapLength - mapOffsetLength - (event.getX() * viewToMapLengthScale);
				setTabletBeaconCoordinate(xCoordinate, yCoordinate);
				byte[] message = createTabletBeaconMessage(MAP_CALL);
				Log.d(TAG, "x:" + xCoordinate + ", y:" + yCoordinate);
				uDPSenderServiceContinuous.interrupt();
				uDPSenderServiceContinuous.run(hostIP, sendPort, interval, 0, message);
				break;
			}
			return false;
		}
		
	};

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
