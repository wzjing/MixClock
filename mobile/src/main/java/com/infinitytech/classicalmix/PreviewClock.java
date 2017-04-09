package com.infinitytech.classicalmix;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AnalogClock;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import java.sql.Time;

public class PreviewClock extends View {

    public static final String Mark="wzjing";

    private Time time;
    private Paint clockpaint;
    private Paint textpaint;
    private Paint strokepaint;
    private Bitmap clockpanel;
    private Bitmap hourhand;
    private Bitmap minutehand;
    private Bitmap secondhand;
    private Bitmap centerpoint;
    private Rect src;
    private Rect dst;
    private float centerX;
    private float centerY;
    public int width;
    public int height;
    public Handler refreshhandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            time = new Time(System.currentTimeMillis());
            invalidate();
            sendEmptyMessageDelayed(1,1000);
        }
    };

    public PreviewClock(Context context) {
        super(context);
    }

    public PreviewClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(Mark,"Structure Start");
        time = new Time(System.currentTimeMillis());
        clockpaint = new Paint();
        clockpaint.setShadowLayer(5,3,3, Color.BLACK);
        clockpaint.setPathEffect(new DashPathEffect(new float[]{2,2},0));
        clockpaint.setStyle(Paint.Style.STROKE);
        textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(20);
        textpaint.setTypeface(Typeface.DEFAULT_BOLD);
        strokepaint = new Paint();
        strokepaint.setColor(Color.BLACK);
        strokepaint.setStrokeWidth(5);
        strokepaint.setStrokeCap(Paint.Cap.ROUND);
        clockpanel = BitmapFactory.decodeResource(getResources(),R.drawable.main_panel);
        hourhand = BitmapFactory.decodeResource(getResources(),R.drawable.hour_hand);
        minutehand = BitmapFactory.decodeResource(getResources(),R.drawable.minute_hand);
        secondhand = BitmapFactory.decodeResource(getResources(),R.drawable.second_hand);
        centerpoint = BitmapFactory.decodeResource(getResources(),R.drawable.point);
        src = new Rect(0,0,clockpanel.getWidth(),clockpanel.getHeight());
        Log.i(Mark,"Structure End");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i(Mark,"onDraw Start");
        dst = new Rect(0,0,width,height);
        centerX = width/2;
        centerY = height/2;
        float secRot = time.getSeconds() * 6;
        float minRot = time.getMinutes() * 6;
        float hrRot = time.getHours() * 30;
        canvas.save();
        canvas.drawBitmap(clockpanel,src,dst,clockpaint);
        canvas.restore();
        canvas.save();
        canvas.rotate(hrRot,centerX,centerY);
        canvas.drawBitmap(hourhand,src,dst,clockpaint);
        canvas.restore();
        canvas.save();
        canvas.rotate(minRot,centerX,centerY);
        canvas.drawBitmap(minutehand,src,dst,clockpaint);
        canvas.restore();
        canvas.save();
        canvas.rotate(secRot,centerX,centerY);
        canvas.drawBitmap(secondhand,src,dst,clockpaint);
        canvas.restore();
        canvas.save();
        canvas.drawBitmap(centerpoint,src,dst,clockpaint);
        canvas.save();
        refreshhandler.sendEmptyMessage(1);
        Log.i(Mark,"onDraw End");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.i(Mark,"onMeasure Start");
        width = getDefaultSize(getSuggestedMinimumWidth(),widthMeasureSpec);
        height = getDefaultSize(getSuggestedMinimumHeight(),heightMeasureSpec);
        Log.i(Mark,"onMeasure End");
    }
}
