package com.rockinhome.app;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class MapDBSource {
	private final String TAG = "MapDBSource";
	
	private SQLiteDatabase database;
	private MapEntryDBHelper dBHelper;
	String[] nameColumn = { 
	  MapEntryDBHelper.COLUMN_MAP_NAME
	  };
	
	String[] mapInfo = { MapEntryDBHelper.COLUMN_MAP_NAME, 
	  MapEntryDBHelper.COLUMN_PATH,
	  MapEntryDBHelper.COLUMN_WIDTH,
	  MapEntryDBHelper.COLUMN_HEIGHT,
	  MapEntryDBHelper.COLUMN_X_ORIGIN,
	  MapEntryDBHelper.COLUMN_Y_ORIGIN,
	  MapEntryDBHelper.COLUMN_FLIP_X,
	  MapEntryDBHelper.COLUMN_FLIP_Y };

	public MapDBSource(Context context){
		dBHelper = new MapEntryDBHelper(context);
	}
	
	public void open() throws SQLException {
		database = dBHelper.getWritableDatabase();
	}
	
	public void close(){
		dBHelper.close();
	}
	
	public void addMapEntry(Map mapEntry){
		ContentValues values = new ContentValues();
 		values.put(MapEntryDBHelper.COLUMN_MAP_NAME, mapEntry.name);
		values.put(MapEntryDBHelper.COLUMN_PATH, mapEntry.filePath);
		values.put(MapEntryDBHelper.COLUMN_WIDTH, mapEntry.imageWidth);
		values.put(MapEntryDBHelper.COLUMN_HEIGHT, mapEntry.imageHeight);
		values.put(MapEntryDBHelper.COLUMN_X_ORIGIN, mapEntry.xOrigin);
		values.put(MapEntryDBHelper.COLUMN_Y_ORIGIN, mapEntry.yOrigin);
		values.put(MapEntryDBHelper.COLUMN_FLIP_X, mapEntry.flipX);
		values.put(MapEntryDBHelper.COLUMN_FLIP_Y, mapEntry.flipY);
		database.insert(MapEntryDBHelper.TABLE_MAPS, null, values);
	}

	public void removeMapEntry(String mapName){
		database.delete(MapEntryDBHelper.TABLE_MAPS, 
		  MapEntryDBHelper.COLUMN_MAP_NAME + "=?", new String[]{mapName});
	}

	public Map getMapEntry(String mapName){
		Map map = new Map();
		Cursor cursor = database.query(MapEntryDBHelper.TABLE_MAPS, mapInfo,
		  MapEntryDBHelper.COLUMN_MAP_NAME + "=?", new String[]{mapName}, null, null, null);
		cursor.moveToFirst();
		map.name = cursor.getString(0);
		map.filePath = cursor.getString(1);
		map.imageWidth = cursor.getDouble(2);
		map.imageHeight = cursor.getDouble(3);
		map.xOrigin = cursor.getDouble(4);
		map.yOrigin = cursor.getDouble(5);
		map.flipX = cursor.getInt(6)>0;
		map.flipY = cursor.getInt(7)>0;
		Log.d(TAG, "flip y:" + map.flipY);
		return map;
	}
	
	public List<String> getAllMapNames(){
		List<String> mapNames = new ArrayList<String>();
		Cursor cursor = database.query(MapEntryDBHelper.TABLE_MAPS, nameColumn, 
		  null, null, null, null, null);
		for(int i = 0; i < cursor.getColumnCount(); i++){
			Log.d(TAG, "Column " + Integer.toString(i) 
			  + " name: " + cursor.getColumnName(i));
		}
		cursor.moveToFirst();
		while (!cursor.isAfterLast()){
			mapNames.add(cursor.getString(0));
			cursor.moveToNext();
		}
		cursor.close();
		return mapNames;
	}
}
