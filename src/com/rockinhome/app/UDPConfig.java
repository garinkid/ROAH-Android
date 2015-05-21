package com.rockinhome.app;

import android.content.Intent;
import android.os.Bundle;

public class UDPConfig {
	
	public final static String
	  //reference for connection setting
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
		intent.putExtra(UDPConfig.HOST_IP, this.hostIP);
		intent.putExtra(UDPConfig.RECEIVE_PORT, this.receivePort);
		intent.putExtra(UDPConfig.SEND_PORT, this.sendPort);
		intent.putExtra(UDPConfig.INTERVAL, this.interval);
		intent.putExtra(UDPConfig.REPETITION, this.repetition);
		return intent;
	}
	
	public void getExtras(Bundle extra){
		this.hostIP = extra.getString(UDPConfig.HOST_IP);
		this.receivePort = extra.getInt(UDPConfig.RECEIVE_PORT);
		this.sendPort = extra.getInt(UDPConfig.SEND_PORT);
		this.interval = extra.getInt(UDPConfig.INTERVAL);
		this.repetition = extra.getInt(UDPConfig.REPETITION);
	}
	
	public void reset(){
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
