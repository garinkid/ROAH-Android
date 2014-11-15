package com.rockinhome.app;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

import com.google.protobuf.Descriptors.EnumDescriptor;

import eu.rockin.roah_rsbb_msgs.TabletBeaconProtos.TabletBeacon;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

public class UDPSenderService extends Thread{
	public static final String TAG="UDPSend",
	  MESSAGE = "message";
	
	public static int CONTINUOUS_INTERVAL = 1000; //milisecond 
	
	public boolean running;
	
	int interval, 
	  repetition,
	  port;
	
	String host;
	
	byte[] message;
	
	Context context;
	
	UDPSenderService(Context context){
		this.context = context;
	}
	
	public void run(String host, int port, int interval, int repetition, byte[] message) {
		Log.d("UDP", "is called");
		this.host = host;
		this.port = port;
		this.interval = interval;
		this.repetition = repetition;
		this.message = message;
		//set interval to default interval value for continuous sending
		if(repetition==0){this.interval = CONTINUOUS_INTERVAL;};
		Log.d(TAG, "Host: " + this.host + ", port: " + this.port);
		running = true;
		Log.d("TAG", "calling UDP");
		new UDPSenderAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	public void setMessage(byte[] message){
		this.message = message;
	}
	
	public void interrupt(){
		this.running = false;
	}
	
	public class UDPSenderAsyncTask extends AsyncTask<Void, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(Void... Void) {
			// create message
			DatagramSocket socket = null;
			Log.d(TAG, "Size send:" + message.length);
			// create socket
			try {
				socket = new DatagramSocket();
				//socket.setReuseAddress(true);
				socket.setBroadcast(true);
				Log.d("UDP", "socket created");
				
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				Log.e("UDPReceiverService", "Fail creating a socket");
				this.cancel(true);
				e1.printStackTrace();
			}
			
			if(repetition == 0)
			{
				while(running){
					try {
						DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(host), port);
						socket.send(datagramPacket);
						SystemClock.sleep(interval);
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}	
				}
			}else
			{
				for(int i = 0; i<repetition; i++){
					try {
						DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(host), port);
						socket.send(datagramPacket);
						SystemClock.sleep(interval);
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			Intent broadcastActivityIntent = new Intent(MainActivity.ACTIVITY_LOG);
			broadcastActivityIntent.putExtra(MainActivity.ACTIVITY_LOG, "Message sent to "
			  + host + ":" + port + " (" + repetition + " repetition, " + 
			  interval + " ms interval)");
			context.sendBroadcast(broadcastActivityIntent);
			socket.disconnect();
			socket.close();
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
}
