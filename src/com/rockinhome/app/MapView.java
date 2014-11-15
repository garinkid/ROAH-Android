package com.rockinhome.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class MapView extends ImageView implements OnTouchListener{

	Canvas canvas;
	public final static String TAG = "MAPVIEW";
	
	public MapView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
    public MapView(Context context, AttributeSet attr) {
        super(context, attr);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        this.canvas = canvas;
    }
    
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		double x_center = event.getX();
		double y_center = event.getY();
		Paint paint = new Paint(0);
		paint.setColor(Color.RED);
		Log.d("TAG", "draw circle");
		this.canvas.drawCircle((float)x_center, (float)y_center, (float)0.2, paint);
		this.invalidate();
		return true;
	}




}
