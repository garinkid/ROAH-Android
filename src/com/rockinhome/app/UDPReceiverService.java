package com.rockinhome.app;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class UDPReceiverService extends Thread{
	public static final String RECEIVED_PACKAGE = "received package",
	  RESULT ="result",
	  SUCCESS ="success",
	  FAIL = "fail",
	  STOPPED = "stopped";
	boolean running = true;
	
	int timeout,
	  trialMax;
	
	Context context;
	
	UDPReceiverService(Context context){
		this.context = context;
	}

    public void run(Intent intent) {
		Log.d("UDP", "is called");
		int port = intent.getIntExtra(MainActivity.RECEIVE_PORT, 666);
		timeout = intent.getIntExtra(MainActivity.TIMEOUT, -1);
		trialMax = intent.getIntExtra(MainActivity.TRIAL, -1);
		running = true;
		//new UDPReceiverAsyncTask().execute(hostSendingPort);
		new UDPReceiverAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, port);
		
    }

    public void interrupt(){
		Intent broadcastActivityIntent = new Intent(MainActivity.ACTIVITY_LOG);
		broadcastActivityIntent.putExtra(MainActivity.ACTIVITY_LOG, "Listening stopped");
		context.sendBroadcast(broadcastActivityIntent);
		running = false;
    }
	
	public class UDPReceiverAsyncTask extends AsyncTask<Integer, Integer, Boolean>{
		@Override
		protected Boolean doInBackground(Integer... port) {
			DatagramSocket socket = null;
			try {
				Log.d("UDP", "creating socket on port " + port[0]);
				socket = new DatagramSocket(port[0]);
				socket.setReuseAddress(true);
				socket.setBroadcast(true);
				if(timeout > 0){
					socket.setSoTimeout(timeout);
				}else{
					socket.setSoTimeout(1000);
				}
				Log.d("UDP", "socket created with timeout:" + socket.getSoTimeout());
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				Log.e("UDPReceiverService", "Fail creating a socket");
				this.cancel(true);
				e1.printStackTrace();
			}
			Intent broadcastActivityIntent = new Intent(MainActivity.ACTIVITY_LOG);
			broadcastActivityIntent.putExtra(MainActivity.ACTIVITY_LOG, "listening to port:" + port[0]);
			context.sendBroadcast(broadcastActivityIntent);
			int trial = 0;
			while(running){
				try {
					byte[] receiveMessage = new byte[socket.getReceiveBufferSize()];
					DatagramPacket packet = new DatagramPacket(receiveMessage, receiveMessage.length);
					socket.receive(packet);
					Intent broadcastResultIntent = new Intent(UDPReceiverService.RECEIVED_PACKAGE);
					broadcastResultIntent.putExtra(RESULT, SUCCESS);
					broadcastResultIntent.putExtra(UDPReceiverService.RECEIVED_PACKAGE, receiveMessage);
					context.sendBroadcast(broadcastResultIntent);
					trial = 0;
				} catch (SocketException e) {
					trial += 1;
					e.printStackTrace();
				} catch (UnknownHostException e) {
					trial += 1;
					e.printStackTrace();
				} catch (IOException e) {
					trial += 1;
					e.printStackTrace();
				}
				if(trialMax < trial){
					broadcastActivityIntent = new Intent(MainActivity.ACTIVITY_LOG);
					broadcastActivityIntent.putExtra(MainActivity.ACTIVITY_LOG, "fail to receive packet after " + trialMax + " times time out");
					context.sendBroadcast(broadcastActivityIntent);
					Intent broadcastResultIntent = new Intent(UDPReceiverService.RECEIVED_PACKAGE);
					broadcastResultIntent.putExtra(RESULT, FAIL);
					context.sendBroadcast(broadcastResultIntent);
					running = false;
				}
			}
			socket.close();
			return null;
		}
	}
}
