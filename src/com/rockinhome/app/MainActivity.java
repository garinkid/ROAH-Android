/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

package com.rockinhome.app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.google.protobuf.Descriptors.EnumDescriptor;

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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public final String TAG = "Main activity",
	  ACTIVITY_LOG="activity log";

	public final int BUTTON_CALL = 1,
	  MAP_CALL = 2,
	  RESUME = 3,
	  CONNECTION_CONFIGURATION_REQUEST = 4,
	  MAP_CONFIGURATION_REQUEST = 5;

	double tabletBeaconX, tabletBeaconY;

	Map map;

	UDPConfig uDPConfig;

	Time lastCallTime, lastPoseTime;

	UDPReceiverService uDPReceiverService;
	
	UDPSenderService uDPSenderServiceContinuous;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//collect previously set values
		SharedPreferences preferences =  PreferenceManager
		  .getDefaultSharedPreferences(this);

		map = new Map();
		map.getFromPreference(preferences);
		
		//using predefine value as config:
		uDPConfig = new UDPConfig();
		uDPConfig.reset();

		//set buttons
		Button callRobot = (Button)findViewById(R.id.call_robot_button);
		Button quit = (Button)findViewById(R.id.quit_button);
		callRobot.setOnClickListener(onClick);
		quit.setOnClickListener(onClick);

		//set map view
		ImageView mapView = (ImageView)findViewById(R.id.map);
		FrameLayout mapLayout = (FrameLayout)findViewById(R.id.map_layout);
		mapView.setOnTouchListener(onTouch);

		//hide map by default
		mapLayout.setVisibility(View.INVISIBLE);

		uDPReceiverService = new UDPReceiverService(getBaseContext());
		uDPSenderServiceContinuous = new UDPSenderService();

		//set last time to zero the app is activated
		lastCallTime = Time.newBuilder().setSec(0).setNsec(0).build();
		lastPoseTime = Time.newBuilder().setSec(0).setNsec(0).build();

		//TabletBeacon coordinate when the app is started;
		tabletBeaconX=0;
		tabletBeaconY=0;
		informUser(uDPConfig.getInfo() + "\n\n" + map.getInfo());
		loadMap();
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
			intent = new Intent(getBaseContext(), ConfigureConnection.class);
			intent = uDPConfig.putExtras(intent);
			startActivityForResult(intent, CONNECTION_CONFIGURATION_REQUEST);
			return true;
		case R.id.action_show_hide_map:
			FrameLayout mapLayout = (FrameLayout)findViewById(R.id.map_layout);
			showMap(!mapLayout.isShown());
			return true;
		case R.id.action_setting_map:
			intent = new Intent(getBaseContext(), ConfigureMap.class);
			intent = map.putExtras(intent);
			startActivityForResult(intent, MAP_CONFIGURATION_REQUEST);
			return true;
		case R.id.action_quit:
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	protected void onResume(){
		super.onResume();
		IntentFilter intentFilterPackage = new IntentFilter(UDPReceiverService.RECEIVED_PACKAGE);
		registerReceiver(packageReceiver, intentFilterPackage);

		//stop currently running thread
		uDPReceiverService.interrupt();
		uDPSenderServiceContinuous.interrupt();

		//start new thread to listen with the new port
		uDPReceiverService.run(uDPConfig.receivePort);
		byte[] message =  createTabletBeaconMessage(RESUME);
		uDPSenderServiceContinuous.run(uDPConfig, message);
	}

	@Override
	protected void onPause() {
		unregisterReceiver(packageReceiver);
		uDPReceiverService.interrupt();
		uDPSenderServiceContinuous.interrupt();
		super.onPause();
	}

	@Override
	protected void onStop(){
		super.onStop();
		SharedPreferences preferences =  PreferenceManager
		  .getDefaultSharedPreferences(this);
		map.saveToPreference(preferences);
	}

	private OnClickListener onClick = new OnClickListener(){
		@Override
		public void onClick(View view) {
			switch(view.getId()){
			case R.id.call_robot_button:
				setTabletBeaconCoordinate(0, 0);
				ImageView mapBlinkerView = (ImageView)findViewById(R.id.map_blinker);
				mapBlinkerView.destroyDrawingCache();
				mapBlinkerView.setImageDrawable(null);
				byte[] message = createTabletBeaconMessage(BUTTON_CALL);
				uDPSenderServiceContinuous.interrupt();
				uDPSenderServiceContinuous.run(uDPConfig, message);
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
			loadMap();
			informUser(map.getInfo());
		}
	}

	protected void setTabletBeaconCoordinate(double x, double y){
		tabletBeaconX = x;
		tabletBeaconY = y;
	}
	
	protected void showMap(boolean showMap){
		FrameLayout mapLayout = (FrameLayout)findViewById(R.id.map_layout);
		if(showMap){
			mapLayout.setVisibility(View.VISIBLE);
		}else{
			mapLayout.setVisibility(View.INVISIBLE);
		}
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

		Log.d(TAG, "TabletBeacon: x=" +  tabletBeaconX + ",y=" + tabletBeaconY + 
		  ",lastCall=" + lastCallTime.getSec() + ",lastPose" + lastPoseTime.getSec());

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
		Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
	}

	OnTouchListener onTouch = new OnTouchListener(){
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch(v.getId()){
			case R.id.map:
				ImageView mapView = (ImageView)findViewById(R.id.map);
				if(map.loadMapToImageView(mapView)){
					float touchX =  event.getX();
					float touchY = event.getY();
					//update image
					loadMap();
					ImageView mapBlinkerView = (ImageView)findViewById(R.id.map_blinker);
					mapBlinkerView.setImageDrawable(null);
					mapBlinkerView.buildDrawingCache();
					Bitmap mapBitmap = mapBlinkerView.getDrawingCache();
					Canvas canvas = new Canvas(mapBitmap);
					Paint paint = new Paint();
					paint.setColor(Color.BLUE);
					paint.setAntiAlias(true);
					canvas.drawCircle(touchX, touchY, (float)30 , paint);
					mapBlinkerView.setImageDrawable(new BitmapDrawable(getResources(), mapBitmap));
					Animation animation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.blink);
					mapBlinkerView.startAnimation(animation);
					//send message
					double xCoordinate = map.calculateX(mapView.getWidth(), touchX);
					double yCoordinate = map.calculateY(mapView.getHeight(), touchY);
					setTabletBeaconCoordinate(xCoordinate, yCoordinate);
					byte[] message = createTabletBeaconMessage(MAP_CALL);
					uDPSenderServiceContinuous.interrupt();
					uDPSenderServiceContinuous.run(uDPConfig, message);
				}else{
					Toast.makeText(getApplicationContext(), 
					  "File '" + map.filePath + "' doesn't exist", Toast.LENGTH_SHORT).show();
				}
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
			if(result.contentEquals(UDPReceiverService.SUCCESS)){
				FrameLayout mapLayout = (FrameLayout)findViewById(R.id.map_layout);
				showMap(intent.getBooleanExtra(UDPReceiverService.RECEIVED_PACKAGE, mapLayout.isShown()));				
			}
		}
	};

	private void loadMap(){
		ImageView mapView = (ImageView)findViewById(R.id.map);
		if(!map.loadMapToImageView(mapView)){
			Toast.makeText(getApplicationContext(), 
			  "File '" + map.filePath + "' doesn't exist", Toast.LENGTH_SHORT).show();
		};
	}
	
}
