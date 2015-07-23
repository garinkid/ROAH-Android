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
import java.nio.ByteBuffer;

import com.google.protobuf.InvalidProtocolBufferException;

import eu.rockin.roah_rsbb_msgs.RoahRsbbBeaconProtos.RoahRsbbBeacon;
import eu.rockin.roah_rsbb_msgs.RobotBeaconProtos.RobotBeacon;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class UDPReceiverService extends Thread{
	public final static String RECEIVED_PACKAGE = "received package";

	public final static String RESULT ="result";

	public final static String SUCCESS ="success";

	public final String TAG = "UDP Receiver Service";

	boolean running = true;
	
	Context context;
	
	public UDPReceiverService(Context context){
		this.context = context;
	}
	
    public void run(int receive_port) {
		Log.d("UDP", "is called");
		int port = receive_port;
		running = true;
		new UDPReceiverAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, port);
	}

	public void interrupt(){
		running = false;
	}

	private class UDPReceiverAsyncTask extends AsyncTask<Integer, Integer, Boolean>{
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
			while(running){
				try {
					byte[] receiveMessage = new byte[socket.getReceiveBufferSize()];
					DatagramPacket packet = new DatagramPacket(receiveMessage, receiveMessage.length);
					socket.receive(packet);
					translatePacket(packet);
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
	
	private void translatePacket(DatagramPacket packetData) throws InvalidProtocolBufferException{
		byte[] header = new byte[12];
		System.arraycopy(packetData.getData(), 0, header, 0, 12);
		ByteBuffer headerBuff = ByteBuffer.wrap(header);
		headerBuff.rewind();
		
		int frameHeaderVersion = headerBuff.getInt(); //not being used
		int size = headerBuff.getInt() - 4; 
		int cmpId = headerBuff.getShort();
		int msgId = headerBuff.getShort();
		//4 bytes are used for cmp_id, msg_id
		//8 bytes are used for udp header;
		if(size == 0){
			return;
		}
		
		Log.d(TAG, "Size:" + size + ", cmp_id:" + cmpId + ", msg_id: " + msgId);
		if(size < 0){return;} // TODO usually happens when simultaneously sending and receiving 
		//message is RobotBeacon from eu.rockin.roah_rsbb_msgs
		if(msgId == 30){
			byte[] protobuf = new byte[size];
			System.arraycopy(packetData.getData(), 12, protobuf, 0, size);
			RobotBeacon robotBeacon = RobotBeacon.parseFrom(protobuf);
			Log.d("UDP", "robot name: " + robotBeacon.getRobotName().toString() + 
				", team name: " + robotBeacon.getTeamName().toString() +
				", time: " + robotBeacon.getTime().toString());
		}else if(msgId == 10){
			//message is RoahRsbbBeacon from eu.rockin.roah_rsbb_msgs
			byte[] protobuf = new byte[size];
			System.arraycopy(packetData.getData(), 12, protobuf, 0, size);
			RoahRsbbBeacon roahRsbbBeacon = RoahRsbbBeacon.parseFrom(protobuf);
			Intent broadcastResultIntent = new Intent(UDPReceiverService.RECEIVED_PACKAGE);
			broadcastResultIntent.putExtra(UDPReceiverService.RESULT, SUCCESS);
			broadcastResultIntent.putExtra(UDPReceiverService.RECEIVED_PACKAGE, roahRsbbBeacon.getTabletDisplayMap());
			context.sendBroadcast(broadcastResultIntent);
		}else{
			//unknown message
			//Log.d("UDP", "unknown message id: " + msgId);
		}
	}
		
}