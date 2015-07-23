/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

package com.rockinhome.app;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

public class UDPSenderService extends Thread{
	public final String TAG="UDPSend";

	public final int CONTINUOUS_INTERVAL = 1000; //milisecond 

	public boolean running;

	UDPConfig uDPConfig;

	byte[] message;

	public void run(UDPConfig uDPConfig, byte[] message) {
		Log.d("UDP", "is called");
		this.uDPConfig = uDPConfig;
		this.message = message;
		if(uDPConfig.repetition==0){
			this.uDPConfig.interval = CONTINUOUS_INTERVAL;
		}
		Log.d(TAG, "Host: " + uDPConfig.hostIP + ", port: " + uDPConfig.sendPort);
		running = true;
		Log.d("TAG", "calling UDP");
		new UDPSenderAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
			try{
				socket = new DatagramSocket();
				//socket.setReuseAddress(true);
				socket.setBroadcast(true);
				Log.d("UDP", "socket created");
			} catch (SocketException e1) {
				Log.e("UDPReceiverService", "Fail creating a socket");
				this.cancel(true);
				e1.printStackTrace();
			}
			
			if(uDPConfig.repetition == 0){
				while(running){
					try {
						DatagramPacket datagramPacket = new DatagramPacket(message, 
						  message.length, InetAddress.getByName(uDPConfig.hostIP), uDPConfig.sendPort);
						socket.send(datagramPacket);
						SystemClock.sleep(uDPConfig.interval);
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}	
				}
			}else{
				for(int i = 0; i<uDPConfig.repetition; i++){
					try {
						DatagramPacket datagramPacket = new DatagramPacket(message,
						  message.length, InetAddress.getByName(uDPConfig.hostIP), uDPConfig.sendPort);
						socket.send(datagramPacket);
						SystemClock.sleep(uDPConfig.interval);
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			socket.disconnect();
			socket.close();
			return null;
		}
	}
}