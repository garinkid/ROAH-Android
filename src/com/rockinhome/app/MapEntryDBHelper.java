package com.rockinhome.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class MapEntryDBHelper extends SQLiteOpenHelper{
	
	public static final String TABLE_MAPS = "maps",
	  COLUMN_MAP_NAME = "mapName",
	  COLUMN_PATH = "path",
	  COLUMN_WIDTH = "width",
	  COLUMN_HEIGHT = "height",
	  COLUMN_X_ORIGIN = "xOrigin",
	  COLUMN_Y_ORIGIN = "yOrigin",
	  COLUMN_FLIP_X = "flipX",
	  COLUMN_FLIP_Y = "flipY";
	
	private static final String DATABASE_NAME = "maps.db";
	private static final int DATABASE_VERSION = 1;

	private static final String SQL_CREATE_ENTRIES = 
	  "CREATE TABLE " + TABLE_MAPS + "( " 
	  + COLUMN_MAP_NAME + " TEXT, "
	  + COLUMN_PATH + " TEXT, " 
	  + COLUMN_WIDTH + " REAL, " 
	  + COLUMN_HEIGHT + " REAL, " 
	  + COLUMN_X_ORIGIN + " REAL, " 
	  + COLUMN_Y_ORIGIN + " REAL, " 
	  + COLUMN_FLIP_X + " NUMERIC, "
	  + COLUMN_FLIP_Y + " NUMERIC)";

	private static final String SQL_DELETE_ENTRIES = 
	  "DROP TABLE IF EXISTS " + TABLE_MAPS;

	public MapEntryDBHelper (Context context){
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_ENTRIES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
	}
}
