/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

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
	  SUCCESS ="success";

	boolean running = true;

	Context context;

	UDPReceiverService(Context context){
		this.context = context;
	}

    public void run(int receive_port) {
		Log.d("UDP", "is called");
		int port = receive_port;
		running = true;
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
				Log.d("UDP", "socket created with timeout:" + socket.getSoTimeout());
			} catch (SocketException e1) {
				Log.e("UDPReceiverService", "Fail creating a socket");
				this.cancel(true);
				e1.printStackTrace();
			}
			Intent broadcastActivityIntent = new Intent(MainActivity.ACTIVITY_LOG);
			broadcastActivityIntent.putExtra(MainActivity.ACTIVITY_LOG, "listening to port:" + port[0]);
			context.sendBroadcast(broadcastActivityIntent);
			while(running){
				try {
					byte[] receiveMessage = new byte[socket.getReceiveBufferSize()];
					DatagramPacket packet = new DatagramPacket(receiveMessage, receiveMessage.length);
					socket.receive(packet);
					Intent broadcastResultIntent = new Intent(UDPReceiverService.RECEIVED_PACKAGE);
					broadcastResultIntent.putExtra(UDPReceiverService.RESULT, SUCCESS);
					broadcastResultIntent.putExtra(UDPReceiverService.RECEIVED_PACKAGE, receiveMessage);
					context.sendBroadcast(broadcastResultIntent);
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			socket.close();
			return null;
		}
	}
}