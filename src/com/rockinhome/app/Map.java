/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

package com.rockinhome.app;

import java.io.File;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
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
	public final String TAG = "Map",
	  MAP_NAME = "map name",
	  MAP_FILE_PATH = "map",
	  MAP_IMAGE_WIDTH = "map image width",
	  MAP_IMAGE_HEIGHT = "map image height",
	  MAP_X_ORIGIN = "map x origin",
	  MAP_Y_ORIGIN = "map y origin",
	  MAP_FLIP_X = "map flip x",
	  MAP_FLIP_Y = "map flip y";

	public String filePath,
	  name; // path of the map
	
	public double imageWidth, 
	  imageHeight, 
	  xOrigin, 
	  yOrigin;
	
	public boolean flipX,
	  flipY;
	
	public double calculateX(double viewWidth, double xValue){
		double x; // calculated x
		// scale image input to map size
		x = xValue * ( this.imageWidth / viewWidth );
		if(this.flipX){
			x = this.imageWidth - this.xOrigin - x;
		}else{
			x = x - this.xOrigin;
		}
		return x;
	}

	public double calculateY(double viewHeight, double yValue){
		double y; // calculated y
		// scale image input to map size
		y = yValue * ( this.imageHeight / viewHeight );
		if(this.flipY){
			y = y - this.yOrigin;
		}else{
			y = this.imageHeight - this.yOrigin - y;
		}
		return y;
	}

	public Intent putExtras(Intent intent){
		intent.putExtra(MAP_FILE_PATH, this.filePath);
		intent.putExtra(MAP_IMAGE_WIDTH, this.imageWidth);
		intent.putExtra(MAP_IMAGE_HEIGHT, this.imageHeight);
		intent.putExtra(MAP_X_ORIGIN, this.xOrigin);
		intent.putExtra(MAP_Y_ORIGIN, this.yOrigin);
		intent.putExtra(MAP_FLIP_X, this.flipX);
		intent.putExtra(MAP_FLIP_Y, this.flipY);
		return intent;
	}

	public void getExtras(Bundle extra){
		this.filePath = extra.getString(MAP_FILE_PATH);
		this.imageWidth = extra.getDouble(MAP_IMAGE_WIDTH);
		this.imageHeight = extra.getDouble(MAP_IMAGE_HEIGHT);
		this.xOrigin = extra.getDouble(MAP_X_ORIGIN);
		this.yOrigin = extra.getDouble(MAP_Y_ORIGIN);
		this.flipX = extra.getBoolean(MAP_FLIP_X);
		this.flipY = extra.getBoolean(MAP_FLIP_Y);
	}
	public void saveToPreference(SharedPreferences preferences){
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(MAP_NAME, this.name);
		editor.putString(MAP_FILE_PATH, this.filePath);
		editor.putLong(MAP_IMAGE_WIDTH, (long) this.imageWidth);
		editor.putLong(MAP_IMAGE_HEIGHT, (long) this.imageHeight);
		editor.putLong(MAP_X_ORIGIN, (long) this.xOrigin);
		editor.putLong(MAP_Y_ORIGIN, (long) this.yOrigin);
		editor.putBoolean(MAP_FLIP_X, this.flipX);
		editor.putBoolean(MAP_FLIP_Y, this.flipY);
		editor.commit();
	}

	public void getFromPreference(SharedPreferences preferences){
		this.name = preferences.getString(MAP_NAME, "RoCKIn 2014");
		this.filePath = preferences.getString(MAP_FILE_PATH, "RoCKIn 2014");
		this.imageWidth = (double) preferences.getLong(MAP_IMAGE_WIDTH, 9);
		this.imageHeight = (double) preferences.getLong(MAP_IMAGE_HEIGHT, 7);
		this.xOrigin = (double) preferences.getLong(MAP_X_ORIGIN, 4);
		this.yOrigin = (double) preferences.getLong(MAP_Y_ORIGIN, 5);
		this.flipX = preferences.getBoolean(MAP_FLIP_X, false);
		this.flipY = preferences.getBoolean(MAP_FLIP_Y, false);
	}

	public String getInfo(){
		String info = "-- Map -- \n";
		info += "File path: " + this.filePath + "\n";
		info += "Image width, length (m): " + this.imageWidth 
		  + "," + this.imageHeight + " \n";
		info += "X,Y origin (m): " + this.xOrigin 
		  + "," + this.yOrigin + " \n";
		info += "X,Y flip: " + this.flipX 
		  + "," + this.flipY;
		return info; 
	}
	
	//the following are two default maps available in the database
	public void setToIST(){
		this.name = "IST";
		this.filePath = "IST";
		this.imageWidth = 8;
		this.imageHeight = 6;
		this.xOrigin = 4;
		this.yOrigin = 2.5;
		this.flipX = true;
		this.flipY = true;
	}
	
	public void setToToulouse(){
		this.name = "RoCKIn 2014";
		this.filePath = "RoCKIn 2014";
		this.imageWidth = 9;
		this.imageHeight = 7;
		this.xOrigin = 4;
		this.yOrigin = 5;
		this.flipX = false;
		this.flipY = false;
	}

	public boolean loadMapToImageView(ImageView mapView){
		//check map file path
		boolean mapLoaded = true;
		if(this.filePath.equals("RoCKIn 2014")){
			mapView.setImageResource(R.drawable.toulouserockin2014);
			loadOriginToImageView(mapView);
		}else if(this.filePath.equals("IST")){
			mapView.setImageResource(R.drawable.ist);
			loadOriginToImageView(mapView);
		}else{
			File file = new File(filePath);
			if(file.exists()){
				Bitmap bitmap = rescaledBitmap(this.filePath);
				mapView.setImageBitmap(bitmap);
				loadOriginToImageView(mapView);
			}else{
				mapView.destroyDrawingCache();
				mapView.setImageDrawable(null);
				mapLoaded = false;
			}
		}
		return mapLoaded;
	}

	private void loadOriginToImageView(ImageView view){
		view.buildDrawingCache();
		Bitmap arrowBitmap =  ((BitmapDrawable)view.getDrawable()).getBitmap();
		Bitmap immutable = arrowBitmap.copy(Bitmap.Config.ARGB_8888, true);
		Canvas canvas = new Canvas(immutable);
		Log.d(TAG, "Density:" + Integer.toString(canvas.getDensity()));

		double canvasWidth = canvas.getWidth();
		double canvasHeight = canvas.getHeight();		
		double scaleCanvasWidth = canvasWidth / this.imageWidth;
		double scaleCanvasHeight = canvasHeight / this.imageHeight;
		float pathXOrigin = (float)(this.xOrigin *  scaleCanvasWidth);
		float pathYOrigin = (float)((this.imageHeight - this.yOrigin) * scaleCanvasHeight);

		Paint paint = new Paint();
		Path path = new Path();
		Matrix transformMatrix = new Matrix();
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);

		//determine default arrow size
		float arrowLength, arrowThickness;
		if(canvasWidth < canvasHeight){
			arrowLength= (float) (canvasWidth * 0.2);
			arrowThickness = (float) (canvasWidth * 0.01);
		}else{
			arrowLength= (float) (canvasHeight * 0.2);
			arrowThickness = (float) (canvasHeight * 0.01);
		}
		

		//draw y axis arrow
		//checking whether there is sufficient space for default arrow length
		paint.setColor(android.graphics.Color.GREEN);
		if(this.flipY && (canvasHeight - pathYOrigin) < arrowLength){
			path = drawArrow((float)(canvasHeight - pathYOrigin),  arrowThickness);
		}else if(!this.flipY && pathYOrigin < arrowLength){
			path = drawArrow((float)pathYOrigin, arrowThickness);
		}else{			
			path = drawArrow((float) arrowLength,  arrowThickness);
		}

		if(this.flipY){
			transformMatrix.setRotate(180, 0, 0);
			path.transform(transformMatrix);
		}
		path.offset(pathXOrigin, pathYOrigin);
		drawPathFillAndStroke(canvas, path, android.graphics.Color.GREEN);

		//draw x axis arrow
		//checking whether there is sufficient space for default arrow length
		paint.setColor(android.graphics.Color.RED);
		if(this.flipX && (pathXOrigin < arrowLength)){
			path = drawArrow(pathXOrigin,  arrowThickness);
		}else if(!this.flipX && ((canvasWidth - pathXOrigin) < arrowLength)){
			path = drawArrow((float)(canvasWidth - pathXOrigin), arrowThickness);
		}else{			
			path = drawArrow((float) arrowLength,  arrowThickness);
		}
		
		//axis orientation and offset
		if(this.flipX){
			transformMatrix.setRotate(270, 0, 0);
		}else{
			transformMatrix.setRotate(90, 0 ,0);
		}
		path.transform(transformMatrix);
		path.offset(pathXOrigin, pathYOrigin);
		drawPathFillAndStroke(canvas, path, android.graphics.Color.RED);
		
		//label the arrow 
		double xLabelXOrigin, xLabelYOrigin, yLabelXOrigin, yLabelYOrigin, 
		  distanceFromReferredAxis, distanceFromOtherAxis;
		distanceFromReferredAxis = (6 * arrowThickness);
		distanceFromOtherAxis = arrowLength - arrowThickness;
		if(this.flipY && this.flipX){
			xLabelXOrigin = pathXOrigin - distanceFromOtherAxis;
			xLabelYOrigin = pathYOrigin + distanceFromReferredAxis;
			yLabelXOrigin = pathXOrigin - distanceFromReferredAxis;
			yLabelYOrigin = pathYOrigin + distanceFromOtherAxis;
		}else if(this.flipY && !this.flipX){
			xLabelXOrigin = pathXOrigin + distanceFromOtherAxis;
			xLabelYOrigin = pathYOrigin + distanceFromReferredAxis;
			yLabelXOrigin = pathXOrigin + distanceFromReferredAxis;
			yLabelYOrigin = pathYOrigin + distanceFromOtherAxis;			
		}else if(!this.flipY && this.flipX){
			xLabelXOrigin = pathXOrigin - distanceFromOtherAxis;
			xLabelYOrigin = pathYOrigin - distanceFromReferredAxis;
			yLabelXOrigin = pathXOrigin - distanceFromReferredAxis;
			yLabelYOrigin = pathYOrigin - distanceFromOtherAxis;
		}else{
			xLabelXOrigin = pathXOrigin + distanceFromOtherAxis;
			xLabelYOrigin = pathYOrigin - distanceFromReferredAxis;
			yLabelXOrigin = pathXOrigin + distanceFromReferredAxis;
			yLabelYOrigin = pathYOrigin - distanceFromOtherAxis;
		}
		drawTextFillAndStroke(canvas, "x", (float)xLabelXOrigin, (float)xLabelYOrigin, android.graphics.Color.RED);
		drawTextFillAndStroke(canvas, "y", (float)yLabelXOrigin, (float)yLabelYOrigin, android.graphics.Color.GREEN);
		Log.d(TAG, "Y origin:" + Double.toString(pathYOrigin));
		Log.d(TAG, "Distance from X axis:" + Double.toString(distanceFromReferredAxis));

		//draw to canvas
		view.setImageDrawable(new BitmapDrawable(view.getContext().getResources(), immutable));
	}
	
	private void drawPathFillAndStroke(Canvas canvas, Path path, int color){
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(color);
		canvas.drawPath(path, paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(android.graphics.Color.BLACK);
		canvas.drawPath(path, paint);
	}
	
	private void drawTextFillAndStroke(Canvas canvas, String text, float xOrigin, float yOrigin, int color){
		Paint paint = new Paint();
		Rect textBound = new Rect();
		paint.getTextBounds(text, 0, text.length(), textBound);
		float textXOrigin = xOrigin - textBound.exactCenterX();
		float textYOrigin = yOrigin - textBound.exactCenterY();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(color);
		canvas.drawText(text, textXOrigin, textYOrigin, paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(android.graphics.Color.BLACK);
		canvas.drawText(text, textXOrigin, textYOrigin, paint);
	}

	private Path drawArrow(float length, float thickness){
		float pointLength = thickness * 3;
		float arrowBodyLength =  length - pointLength - thickness;
		// a function to create a path for arrow pointing up
		Path path = new Path();
		path.moveTo(0, 0);
		path.lineTo(0 - thickness, 0 - thickness);
		path.lineTo(0 - thickness, 0 - thickness - arrowBodyLength);
		path.lineTo(0 - thickness - pointLength, 0 - thickness - arrowBodyLength);
		path.lineTo(0, 0 - thickness - arrowBodyLength - pointLength);
		path.lineTo(0 + thickness + pointLength, 0 - thickness - arrowBodyLength);
		path.lineTo(0 + thickness, 0 - thickness - arrowBodyLength);
		path.lineTo(0 + thickness, 0 - thickness);
		path.close();
		return path;
	}

	private Bitmap rescaledBitmap(String filePath){
		Bitmap bitmap = BitmapFactory.decodeFile(filePath);
		double maxWidth = 1280.0;
		double maxHeight = 720.0;
		double heightDiff = bitmap.getHeight() - maxHeight;
		double widthDiff = bitmap.getWidth() - maxWidth;
		if(widthDiff > heightDiff && widthDiff > 0){
			//rescaled based on width
			double newHeight = bitmap.getHeight() * (maxWidth/bitmap.getWidth());
			Bitmap scaled = Bitmap.createScaledBitmap(bitmap, (int) maxWidth, (int) newHeight, true);
			return scaled;
		}else if (widthDiff < heightDiff && heightDiff > 0){
			//rescaled based on height
			double newWidth = bitmap.getWidth() * (maxHeight/bitmap.getHeight());
			Bitmap scaled = Bitmap.createScaledBitmap(bitmap, (int) newWidth, (int) maxHeight, true);
			return scaled;
		}else{
			//no need for rescaled
			return bitmap;
		}
	}
}