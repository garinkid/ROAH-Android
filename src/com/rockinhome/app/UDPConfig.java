/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

package com.rockinhome.app;

import android.content.Intent;
import android.os.Bundle;

public class UDPConfig {

	public final static String
	  HOST_IP="host ip",
	  RECEIVE_PORT="receive port",
	  SEND_PORT="send port",
	  INTERVAL="interval",
	  REPETITION="repetition";

	public String hostIP;

	public int sendPort,
	  receivePort,
	  repetition,
	  interval;

	public Intent putExtras(Intent intent){
		intent.putExtra(HOST_IP, this.hostIP);
		intent.putExtra(RECEIVE_PORT, this.receivePort);
		intent.putExtra(SEND_PORT, this.sendPort);
		intent.putExtra(INTERVAL, this.interval);
		intent.putExtra(REPETITION, this.repetition);
		return intent;
	}

	public void getExtras(Bundle extra){
		this.hostIP = extra.getString(HOST_IP);
		this.receivePort = extra.getInt(RECEIVE_PORT);
		this.sendPort = extra.getInt(SEND_PORT);
		this.interval = extra.getInt(INTERVAL);
		this.repetition = extra.getInt(REPETITION);
	}

	public void reset(){
		//previous default hostIP = "10.255.255.255";
		this.hostIP = "255.255.255.255"; // yes it is a hack, yes we know it and yes we haven't fix it yet.
		this.sendPort = 6666;
		this.repetition = 1; // not being used TODO  eliminate
		this.interval = 1000;
		this.receivePort = 6666;
	}

	public String getInfo(){
		String info = "-- Connection -- \n";
		info += "Host: " + this.hostIP + "\n";
		info += "Send port: " + this.sendPort + "\n";
		info += "Send interval: " + this.interval + " (ms) \n";
		info += "Receive port: " + this.receivePort;		
		return info; 
	}
}