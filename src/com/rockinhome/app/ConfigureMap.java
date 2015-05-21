package com.rockinhome.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

public class ConfigureMap extends Activity{

	public final static String TAG="ConfigureMap";
	public final static int LOAD_MAP = 1;
	
	Map map;

	EditText originX,
	  originY,
	  lengthX,
	  lengthY;
	
	ImageView mapPreview;

	Button okInput, 
	  cancelInput,
	  loadMap,
	  resetMap;

	Switch flipX,
	  flipY;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure_map);

		map = new Map();
		map.getExtras(getIntent().getExtras());
		
		//get views
		originX = (EditText)findViewById(R.id.origin_x_input);
		originY = (EditText)findViewById(R.id.origin_y_input);
		lengthX = (EditText)findViewById(R.id.length_x_input);
		lengthY = (EditText)findViewById(R.id.length_y_input);
		flipX = (Switch)findViewById(R.id.flip_x_switch);
		flipY = (Switch)findViewById(R.id.flip_y_switch);
		mapPreview = (ImageView)findViewById(R.id.map_preview);

		//set views value
		setViewsValues();
		
		//set Buttons
		okInput = (Button)findViewById(R.id.ok_input);
		cancelInput = (Button)findViewById(R.id.cancel_input);
		okInput.setOnClickListener(onClick);
		cancelInput.setOnClickListener(onClick);
		loadMap = (Button)findViewById(R.id.load_map);
		loadMap.setOnClickListener(onClick);
		resetMap = (Button)findViewById(R.id.reset_map);
		resetMap.setOnClickListener(onClick);
	}
	
	private void setViewsValues(){
		originX.setText(Double.toString(map.xOrigin));
		originY.setText(Double.toString(map.yOrigin));
		lengthX.setText(Double.toString(map.xLength));
		lengthY.setText(Double.toString(map.yLength));
		flipX.setChecked(map.flipX);
		flipY.setChecked(map.flipY);
		map.loadMapToImageView(mapPreview);
	}
	
	private void getViewsValues(){
		map.xOrigin = Double.parseDouble(originX.getText().toString());
		map.yOrigin = Double.parseDouble(originY.getText().toString());
		map.xLength = Double.parseDouble(lengthX.getText().toString());
		map.yLength = Double.parseDouble(lengthY.getText().toString());
		map.flipX = flipX.isChecked();
		map.flipY = flipY.isChecked();
	}

	private OnClickListener onClick = new OnClickListener(){
		@Override
		public void onClick(View view) {
			Intent intent = new Intent();
			switch(view.getId()){
			case R.id.cancel_input:
				setResult(RESULT_CANCELED,intent);
				finish();
				break;
			case R.id.ok_input:
				getViewsValues();
				intent = map.putExtras(intent);
				setResult(RESULT_OK, intent);
				finish();
				break;	
			case R.id.load_map:
				intent = new Intent(Intent.ACTION_PICK,
				  android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(intent, LOAD_MAP);
				break;
			case R.id.reset_map:
				AlertDialog.Builder builder = new AlertDialog.Builder(ConfigureMap.this);
				builder.setMessage("Reset Map: Are you sure?").setPositiveButton("Yes", dialogClickListener)
				  .setNegativeButton("No", dialogClickListener).show();
				break;	
			}	
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Check which request we're responding to
		if (requestCode == LOAD_MAP && resultCode == RESULT_OK && null != intent){
			Uri selectedImage = intent.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(selectedImage,
			  filePathColumn, null, null, null);
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			map.filePath = cursor.getString(columnIndex);
			cursor.close();
			map.loadMapToImageView(mapPreview);			
		}
		
	}
	
	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	            map.reset();
	    		setViewsValues();
	            break;
	        case DialogInterface.BUTTON_NEGATIVE:
	            //No button clicked
	            break;
	        }
	    }
	};
}
