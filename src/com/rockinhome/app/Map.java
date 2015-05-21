/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

package com.rockinhome.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

// class for map and its parameter
/* By default an image view has the following characteristic
 *  -- the origin of coordinate is top left, max is at bottom right.
 *  -- right is positive X and 
 *  -- down is positive Y.
 * This is, at least IMHO, not very intuitive so the application is set so that  
 *  -- the origin of coordinate is bottom left, max is at top right
 *  -- right is positive X and
 *  -- up is positive Y
 * Therefore, Y is flip (although from the user perspective it is not).
 */
public class Map {
	
	public final static String TAG = "Map",
	  MAP_FILE_PATH = "map",
	  MAP_X_LENGTH = "map x length",
	  MAP_Y_LENGTH = "map y length",
	  MAP_X_ORIGIN = "map x origin",
	  MAP_Y_ORIGIN = "map y origin",
	  MAP_FLIP_X = "map flip x",
	  MAP_FLIP_Y = "map flip y";
	
	String filePath; // path of the map
	double xLength, 
	  yLength, 
	  xOrigin, 
	  yOrigin;

	boolean flipX,
	  flipY;
	
	public double calculateX(double viewXLength, double xValue){
		double x; // calculated x
		// scale image input to map size
		x = xValue * ( this.xLength / viewXLength );
		if(this.flipX){
			x = this.xLength - this.xOrigin - x;
		}else{
			x = x - this.xOrigin;
		}
		return x;
	}
	
	public double calculateY(double viewYLength, double yValue){
		double y; // calculated y
		// scale image input to map size
		y = yValue * ( this.yLength / viewYLength );
		if(this.flipY){
			y = y - this.yOrigin;
		}else{
			y = this.yLength - this.yOrigin - y;

		}
		return y;
	}
	
	public Intent putExtras(Intent intent){
		intent.putExtra(MAP_FILE_PATH, this.filePath);
		intent.putExtra(MAP_X_LENGTH, this.xLength);
		intent.putExtra(MAP_Y_LENGTH, this.yLength);
		intent.putExtra(MAP_X_ORIGIN, this.xOrigin);
		intent.putExtra(MAP_Y_ORIGIN, this.yOrigin);
		intent.putExtra(MAP_FLIP_X, this.flipX);
		intent.putExtra(MAP_FLIP_Y, this.flipY);
		return intent;
	}
	
	public void getExtras(Bundle extra){
		this.filePath = extra.getString(MAP_FILE_PATH);
		this.xLength = extra.getDouble(MAP_X_LENGTH);
		this.yLength = extra.getDouble(MAP_Y_LENGTH);
		this.xOrigin = extra.getDouble(MAP_X_ORIGIN);
		this.yOrigin = extra.getDouble(MAP_Y_ORIGIN);
		this.flipX = extra.getBoolean(MAP_FLIP_X);
		this.flipY = extra.getBoolean(MAP_FLIP_Y);
	}
	
	public void loadMapToImageView(ImageView imageView){
		//check map file path
		imageView.destroyDrawingCache();
		if(this.filePath.equals("")){
			imageView.setImageResource(R.drawable.defaultmap);
		}else{
			imageView.setImageBitmap(BitmapFactory.decodeFile(this.filePath));
		}
	}
	
	public void reset(){
		// set the map to the default value which are
		// image = R.drawable.defaultmap 
		this.filePath = "";
		this.xLength = 9;
		this.yLength = 7;
		this.xOrigin = 4;
		this.yOrigin = 5;
		this.flipX = false;
		this.flipY = false;
	}
	
	public void saveToPreference(SharedPreferences preferences){
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(MAP_FILE_PATH, this.filePath);
		editor.putLong(MAP_X_LENGTH, (long) this.xLength);
		editor.putLong(MAP_Y_LENGTH, (long) this.yLength);
		editor.putLong(MAP_X_ORIGIN, (long) this.xOrigin);
		editor.putLong(MAP_Y_ORIGIN, (long) this.yOrigin);
		editor.putBoolean(MAP_FLIP_X, this.flipX);
		editor.putBoolean(MAP_FLIP_Y, this.flipY);
		editor.commit();
	}
	
	public void getFromPreference(SharedPreferences preferences){
		this.filePath = preferences.getString(Map.MAP_FILE_PATH, "");
		this.xLength = (double) preferences.getLong(Map.MAP_X_LENGTH, 9);
		this.yLength = (double) preferences.getLong(Map.MAP_Y_LENGTH, 7);
		this.xOrigin = (double) preferences.getLong(Map.MAP_X_ORIGIN, 4);
		this.yOrigin = (double) preferences.getLong(Map.MAP_Y_ORIGIN, 5);
		this.flipX = preferences.getBoolean(Map.MAP_FLIP_X, false);
		this.flipY = preferences.getBoolean(Map.MAP_FLIP_Y, false);
	}
	
	public String getInfo(){
		String info = "-- Map -- \n";
		info += "File path: " + this.filePath + "\n";
		info += "X,Y length (m): " + this.xLength 
		  + "," + this.yLength + " \n";
		info += "X,Y origin (m): " + this.xOrigin 
		  + "," + this.yOrigin + " \n";
		info += "X,Y flip: " + this.flipX 
		  + "," + this.flipY;
		return info; 
	}
}

