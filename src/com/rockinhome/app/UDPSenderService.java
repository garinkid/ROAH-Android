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
	
	public void run(Intent intent) {
		Log.d("UDP", "is called");
		host = intent.getStringExtra(MainActivity.HOST_IP);
		port = intent.getIntExtra(MainActivity.SEND_PORT, 666);
		interval = intent.getIntExtra(MainActivity.INTERVAL, 10);
		repetition = intent.getIntExtra(MainActivity.REPETITION, 10);
		message = intent.getByteArrayExtra(MESSAGE);
		Log.d(TAG, "Host: " + host + ", port: " + port);
		running = true;
		Log.d("TAG", "calling UDP");
		new UDPSenderAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	public class UDPSenderAsyncTask extends AsyncTask<Void, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(Void... Void) {
			// create message
			DatagramSocket socket = null;
			byte[] messageByte = message;
			Log.d(TAG, "Size send:" + messageByte.length);
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
			
			for(int i = 0; i<repetition; i++){
				try {
					/*
					ByteBuffer buf = ByteBuffer.allocateDirect(messageByte.length + 12).order(ByteOrder.BIG_ENDIAN);
					EnumDescriptor desc = TabletBeacon.getDescriptor().findEnumTypeByName("CompType");
					int cmp_id = desc.findValueByName("COMP_ID").getNumber();
					int msg_id = desc.findValueByName("MSG_TYPE").getNumber();
					byte[] cmpID = 	ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort((short)cmp_id).array();
					byte[] msgId = 	ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort((short)msg_id).array();
					buf.putInt(2);
					buf.putInt(messageByte.length + 4);
					buf.put(cmpID);
					buf.put(msgId);
					buf.put(messageByte);		
					*/	
					DatagramPacket datagramPacket = new DatagramPacket(messageByte, messageByte.length, InetAddress.getByName(host), port);
					//DatagramChannel channel = DatagramChannel.open();
					//channel.send(buf, new InetSocketAddress(host, port));
					socket.send(datagramPacket);
					SystemClock.sleep(interval);
					//Log.d("TAG", "send to" + host + ": " + port);
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
