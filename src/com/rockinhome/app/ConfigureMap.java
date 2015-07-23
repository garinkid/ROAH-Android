/* 
 * This file is part of the RoCKIn@Home Android App.
 * Author: Rhama Dwiputra
 * 
 */

package com.rockinhome.app;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigureMap extends Activity{

	public final String TAG="ConfigureMap";
	public final int LOAD_MAP = 1;

	Map map;

	MapDBSource mapDBSource;

	ArrayAdapter<String> mapAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure_map);

		map = new Map();
		map.getExtras(getIntent().getExtras());
		//get views
		//set views value
		setViewsValues();
		startChangedStateListener();
		
		//set Buttons
		Button okInput = (Button)findViewById(R.id.ok_input);
		okInput.setOnClickListener(onClick);
		Button saveMapConfig = (Button)findViewById(R.id.save_map_config);
		saveMapConfig.setOnClickListener(onClick);
		Button loadMap = (Button)findViewById(R.id.select_map_file);
		loadMap.setOnClickListener(onClick);
		
		loadMap();
		
		//set map database
		mapDBSource = new MapDBSource(this);
		mapDBSource.open();
		List<String> mapList = mapDBSource.getAllMapNames();
		
		/* at first launch (after installation) the database contains no map.
		 * create two maps as the default maps
		 */
		if(mapList.size() == 0){
			Map map = new Map();
			map.setToToulouse();
			mapDBSource.addMapEntry(map);
			map.setToIST();
			mapDBSource.addMapEntry(map);
			mapList = mapDBSource.getAllMapNames();
		}

		ListView mapListView = (ListView)findViewById(R.id.map_list);
		mapAdapter = new ArrayAdapter<String>(this,
		  android.R.layout.simple_list_item_1, mapList);
		mapListView.setAdapter(mapAdapter);
		mapListView.setOnItemClickListener(mapListListener);
	}

	private void startChangedStateListener(){
		/* Start change listener for all input.
		 * Used for instant update on the map
		 * Need to be stopped when loading a previously configured maps.
		 */
		
		EditText originX = (EditText)findViewById(R.id.origin_x_input);
		EditText originY = (EditText)findViewById(R.id.origin_y_input);
		EditText imageWidth = (EditText)findViewById(R.id.image_width_input);
		EditText imageHeight = (EditText)findViewById(R.id.image_height_input);
		Switch flipX = (Switch)findViewById(R.id.flip_x_switch);
		Switch flipY = (Switch)findViewById(R.id.flip_y_switch);
		
		originX.addTextChangedListener(tW);
		originY.addTextChangedListener(tW);
		imageWidth.addTextChangedListener(tW);
		imageHeight.addTextChangedListener(tW);
		flipX.setOnCheckedChangeListener(oCCL);
		flipY.setOnCheckedChangeListener(oCCL);
	}

	private void stopChangedStateListener(){
		/* Stop change listener for all input
		 * Used when loading a previously configured maps.
		 */

		EditText originX = (EditText)findViewById(R.id.origin_x_input);
		EditText originY = (EditText)findViewById(R.id.origin_y_input);
		EditText imageWidth = (EditText)findViewById(R.id.image_width_input);
		EditText imageHeight = (EditText)findViewById(R.id.image_height_input);
		Switch flipX = (Switch)findViewById(R.id.flip_x_switch);
		Switch flipY = (Switch)findViewById(R.id.flip_y_switch);
		
		originX.removeTextChangedListener(tW);
		originY.removeTextChangedListener(tW);
		imageWidth.removeTextChangedListener(tW);
		imageHeight.removeTextChangedListener(tW);
		flipX.setOnCheckedChangeListener(null);
		flipY.setOnCheckedChangeListener(null);
	}
	
	private void setViewsValues(){
		// Set all inputs based on the current variable value of the map
		TextView mapFilePathInfo = (TextView)findViewById(R.id.map_file_path_info);		
		mapFilePathInfo.setText("File path: " + map.filePath);
		
		EditText originX = (EditText)findViewById(R.id.origin_x_input);
		EditText originY = (EditText)findViewById(R.id.origin_y_input);
		EditText imageWidth = (EditText)findViewById(R.id.image_width_input);
		EditText imageHeight = (EditText)findViewById(R.id.image_height_input);
		Switch flipX = (Switch)findViewById(R.id.flip_x_switch);
		Switch flipY = (Switch)findViewById(R.id.flip_y_switch);
				
		originX.setText(Double.toString(map.xOrigin));
		originY.setText(Double.toString(map.yOrigin));
		imageWidth.setText(Double.toString(map.imageWidth));
		imageHeight.setText(Double.toString(map.imageHeight));
		flipX.setChecked(map.flipX);
		flipY.setChecked(map.flipY);
	}

	private void getViewsValues(){
		// Set the map's variable values based on the input
		EditText originX = (EditText)findViewById(R.id.origin_x_input);
		EditText originY = (EditText)findViewById(R.id.origin_y_input);
		EditText imageWidth = (EditText)findViewById(R.id.image_width_input);
		EditText imageHeight = (EditText)findViewById(R.id.image_height_input);
		Switch flipX = (Switch)findViewById(R.id.flip_x_switch);
		Switch flipY = (Switch)findViewById(R.id.flip_y_switch);
				
		map.xOrigin = Double.parseDouble(originX.getText().toString());
		map.yOrigin = Double.parseDouble(originY.getText().toString());
		map.imageWidth = Double.parseDouble(imageWidth.getText().toString());
		map.imageHeight = Double.parseDouble(imageHeight.getText().toString());
		map.flipX = flipX.isChecked();
		map.flipY = flipY.isChecked();
	}

	private boolean checkExistingMap(String mapName){
		// Check if a map with the same name already exist in the database
		List<String> mapList = mapDBSource.getAllMapNames();
		boolean mapExist = false;
    	for(int i = 0; i < mapList.size(); i++){
    		if(mapList.get(i).toString().contentEquals(mapName)){
    			mapExist=true;
    			break;
    		}
    	}
    	return mapExist;
	}

	private TextWatcher tW = new TextWatcher(){
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {//Do nothing
		}
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
			int count) {// Do nothing
		}
		@Override
		public void afterTextChanged(Editable s) {
			try{
				Double.parseDouble(s.toString());
				getViewsValues();
				loadMap();
			}catch(NumberFormatException e){
				//Do nothing
			}
		}
	};
	
	private OnItemClickListener mapListListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			ListView mapListView = (ListView)findViewById(R.id.map_list);
			final String selectedMap = mapListView.getItemAtPosition(position).toString();
			new AlertDialog.Builder(ConfigureMap.this)
			  .setMessage("Load/delete '" + selectedMap + "' map?")
			  .setPositiveButton("LOAD!!!", new DialogInterface.OnClickListener(){
				  @Override
				  public void onClick(DialogInterface dialog, int which){
					  stopChangedStateListener();
					  map = mapDBSource.getMapEntry(selectedMap);
					  setViewsValues();
					  Toast.makeText(getApplicationContext(), selectedMap + 
					    " is loaded", Toast.LENGTH_SHORT).show();
					  loadMap();
					  startChangedStateListener();
				  }
			  })
			  .setNegativeButton("CANCEL", new DialogInterface.OnClickListener(){
				  @Override
				  public void onClick(DialogInterface dialog, int which){
					  //do nothing
				  }
				  
			  })
			  .setNeutralButton("DELETE!!!", new DialogInterface.OnClickListener(){
				  @Override
				  public void onClick(DialogInterface dialog, int which){
					  if (selectedMap.contentEquals("IST") || selectedMap.contentEquals("RoCKIn 2014")){
						Toast.makeText(getApplicationContext(), "IST and RoCKIn 2014 are the default maps." + 
						  "Default map cannot be deleted.", Toast.LENGTH_SHORT).show();  
					  }else{
						  mapDBSource.removeMapEntry(selectedMap);
						  mapAdapter.remove(selectedMap);
						  Toast.makeText(getApplicationContext(), selectedMap + 
								  " is deleted", Toast.LENGTH_SHORT).show();
					  }
				  }
			  })
			  .show();
		}
	};

	private OnCheckedChangeListener oCCL = new OnCheckedChangeListener(){
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
		  boolean isChecked) {
			getViewsValues();
			loadMap();
		}
	};

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
			case R.id.select_map_file:
				intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				startActivityForResult(intent, LOAD_MAP);
				break;
			case R.id.save_map_config:
				LayoutInflater inflater = (LayoutInflater)getBaseContext().
				  getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				final View dialogSaveMap = inflater.inflate(R.layout.dialog_save_map, null);
				new AlertDialog.Builder(ConfigureMap.this)
				  .setView(dialogSaveMap)
				  .setMessage("Save map configuration")
				  .setPositiveButton("OK!!!", new DialogInterface.OnClickListener() {
					  @Override
					  public void onClick(DialogInterface dialog, int which) {
						  EditText editText = (EditText)dialogSaveMap.findViewById(R.id.map_name);
						  String mapName = editText.getText().toString();
						  if(!checkExistingMap(mapName)){
							  map.name = mapName;
							  getViewsValues();
							  mapAdapter.add(map.name);
							  mapDBSource.addMapEntry(map);
						  }else{
							  Toast.makeText(getApplicationContext(), "Save failed. Map '" + 
							    mapName + "' already exist", Toast.LENGTH_SHORT).show();
						  }	
					}
				  })
				  .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					  @Override
					  public void onClick(DialogInterface dialog, int which) {
						  //do nothing
					  }
				  })
				  .show();
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Check which request we're responding to
		if (requestCode == LOAD_MAP && resultCode == RESULT_OK && null != intent){
			Uri selectedImage = intent.getData();
			String filePath;
			String fileManagerString, selectedImagePath;

			//check result from File Manager or Media Gallery
			fileManagerString = selectedImage.getPath();//OI FILE Manager
			selectedImagePath = getPathFromMediaGallery(selectedImage);//MEDIA GALLERY
			if(selectedImagePath!=null){
				filePath = selectedImagePath;
			}else{
				filePath = fileManagerString;
			}
			//check whether it is a file image or not
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filePath, options);
			if (options.outWidth != -1 && options.outHeight != -1) {
				// it is an image file
				map.filePath = filePath;
				TextView mapFilePathInfo = (TextView)findViewById(R.id.map_file_path_info);				
				mapFilePathInfo.setText("File path: " + map.filePath);
				loadMap();
			}
			else {
				// This is not an image file.
				Toast.makeText(getBaseContext(), filePath + " is not an image file", Toast.LENGTH_LONG).show();
			}
		}
	}

	private String getPathFromMediaGallery(Uri uri){
		String[] filePathColumn = { MediaStore.Images.Media.DATA };
		Cursor cursor = getContentResolver().query(uri,
		  filePathColumn, null, null, null);
		if(cursor!=null){
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			cursor.moveToFirst();
			String filePath = cursor.getString(columnIndex);
			cursor.close();
			return filePath;
		}else{
			return null;
		}
	}
	
	private void loadMap(){
		ImageView mapPreview = (ImageView)findViewById(R.id.map_preview);
		if(!map.loadMapToImageView(mapPreview)){
			Toast.makeText(getApplicationContext(), 
			  "File '" + map.filePath + "' doesn't exist", Toast.LENGTH_SHORT).show();
		};
	}
}