/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

package com.rockinhome.app;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

	public final static String TAG = "Main activity",
	  ACTIVITY_LOG="activity log";
	
	public final static int BUTTON_CALL = 1,
	  MAP_CALL = 2,
	  RESUME = 3;

	double tabletBeaconX,
	  tabletBeaconY;
	
	byte[] message;

	boolean listening;
	
	static final int CONNECTION_CONFIGURATION_REQUEST = 1,
	  MAP_CONFIGURATION_REQUEST = 2;
	
	TextView activityLog;
	
	Button callRobot,
	  quit;
	
	ImageView mapView;
	
	Context context;
	
	Map map;
	
	UDPConfig uDPConfig;
	
	Time lastCallTime,
	  lastPoseTime;
	
	IntentFilter intentFilterPackage,
	  intentFilterLog;
	
	UDPReceiverService uDPReceiverService;
	UDPSenderService uDPSenderServiceContinuous;
	
	SharedPreferences preferences;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getBaseContext();

		//collect previously set values
		preferences =  PreferenceManager.getDefaultSharedPreferences(this);

		map = new Map();
		map.getFromPreference(preferences);
		
		//using predefine value as config:
		//hostIP = "10.255.255.255";
		uDPConfig = new UDPConfig();
		uDPConfig.reset();
		
		//set buttons
		callRobot = (Button)findViewById(R.id.call_robot_button);
		quit = (Button)findViewById(R.id.quit_button);
		callRobot.setOnClickListener(onClick);
		quit.setOnClickListener(onClick);

		//set map view
		mapView = (ImageView)findViewById(R.id.map);
		mapView.setOnTouchListener(onTouch);

		//hide map by default
		mapView.setVisibility(View.INVISIBLE);

		//activity log
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
		
		map.loadMapToImageView(mapView);
		informUser(uDPConfig.getInfo() + "\n\n" + map.getInfo());
		
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
		Intent intent;
		switch(id){
		case R.id.action_settings:
			intent = new Intent(context, ConfigureConnection.class);
			intent = uDPConfig.putExtras(intent);
			startActivityForResult(intent, CONNECTION_CONFIGURATION_REQUEST);
			return true;
		case R.id.action_show_hide_map:
			if(mapView.isShown()){
				mapView.setVisibility(View.INVISIBLE);
			}else{
				mapView.setVisibility(View.VISIBLE);
			}
			return true;
		case R.id.action_setting_map:
			intent = new Intent(context, ConfigureMap.class);
			intent = map.putExtras(intent);
			startActivityForResult(intent, MAP_CONFIGURATION_REQUEST);
			return true;
		case R.id.action_quit:
			finish();
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
		uDPReceiverService.run(uDPConfig.receivePort);
		message =  createTabletBeaconMessage(RESUME);
		uDPSenderServiceContinuous.run(uDPConfig.hostIP, uDPConfig.sendPort, uDPConfig.interval, 0, message);
	}

	@Override
	protected void onPause() {
		unregisterReceiver(packageReceiver);
		unregisterReceiver(logReceiver);
		
		uDPReceiverService.interrupt();
		uDPSenderServiceContinuous.interrupt();
		super.onPause();
	}

	@Override
	protected void onStop(){
		super.onStop();
		map.saveToPreference(preferences);
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
				uDPSenderServiceContinuous.run(uDPConfig.hostIP, uDPConfig.sendPort, uDPConfig.interval, 0, message);
				break;	
			case R.id.quit_button:
				finish();
				break;
			}
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Check which request we're responding to
		if (requestCode == CONNECTION_CONFIGURATION_REQUEST && resultCode == RESULT_OK) {
			uDPConfig.getExtras(intent.getExtras());
			informUser(uDPConfig.getInfo());
		}
		else if (requestCode == MAP_CONFIGURATION_REQUEST && resultCode == RESULT_OK
		  && null != intent){
			map.getExtras(intent.getExtras());
			map.loadMapToImageView(mapView);
			informUser(map.getInfo());
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
		
		/* Serialize the message
		* 12 extra bytes for: 
		* - 4 bytes for frame_header_version 
		* - 4 bytes for message byte length
		* - 2 bytes for COMP_ID
		* - 2 bytes for MSG_TYPE
		*/
		
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
				float touchX =  event.getX();
				float touchY = event.getY();
				//update image
				map.loadMapToImageView(mapView);
				mapView.buildDrawingCache();
				Bitmap mapBitmap = mapView.getDrawingCache();
				Canvas canvas = new Canvas(mapBitmap);
			    Paint paint = new Paint();
			    paint.setColor(Color.RED);
			    canvas.drawCircle(touchX, touchY, (float)30 , paint);
			    mapView.setImageDrawable(null);
			    mapView.setImageDrawable(new BitmapDrawable(getResources(), mapBitmap));

			    //send message
				double xCoordinate = map.calculateX(mapView.getWidth(), touchX);
				double yCoordinate = map.calculateY(mapView.getHeight(), touchY);
				setTabletBeaconCoordinate(xCoordinate, yCoordinate);
				byte[] message = createTabletBeaconMessage(MAP_CALL);
				Log.d(TAG, "x:" + xCoordinate + ", y:" + yCoordinate);
				uDPSenderServiceContinuous.interrupt();
				uDPSenderServiceContinuous.run(uDPConfig.hostIP, uDPConfig.sendPort, uDPConfig.interval, 0, message);
				break;
			}
			return false;
		}
		
	};

	//receiver for UDP message
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
	
	//receiver for log activity
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
