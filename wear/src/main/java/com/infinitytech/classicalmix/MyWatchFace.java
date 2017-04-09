package com.infinitytech.classicalmix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.LevelListDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MyWatchFace extends CanvasWatchFaceService {

    public int powerlevel=0;

    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private final String TAG = "Engine";
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;

        private int width;
        private int height;
        private PointF center;
        private PointF lCenter;
        private PointF rCenter;
        private Rect lSrc;
        private RectF lDst;
        private Rect rSrc;
        private RectF rDst;
        private Rect centerSrc;
        private RectF centerDst;
        private Rect cursorSrc;
        private RectF cursorDst;

        Bitmap mPanel;
        Bitmap mHourHand;
        Bitmap mMinuteHand;
        Bitmap mSecondHand;
        Bitmap mCenterPoint;
        Bitmap mPowerPanel;
        Bitmap mStepPanel;
        Bitmap mPowerCursor;
        private LevelListDrawable mBatteryDrawable;

        Paint clockpaint;
        Paint textpaint;
        Paint battarypaint;
        Rect baseRect;
        Rect littlePanelRect;
        int battarylinewidth;
        boolean mAmbient;
        BattaryReceiver battaryrc;
        IntentFilter filter;
        //Pedometer
        private int step = 0;

        private Calendar mCalendar = Calendar.getInstance();

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        int mTapCount;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            getResources().getDisplayMetrics();
            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setStatusBarGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .setViewProtectionMode(WatchFaceStyle.PROTECT_STATUS_BAR)
                    .build());

            //基础参数
            Resources resources = MyWatchFace.this.getResources();
            DisplayMetrics metrics = resources.getDisplayMetrics();
            width = metrics.widthPixels;
            height = metrics.heightPixels;
            center = new PointF(width/2, height/2);
            lCenter = new PointF(width*312f/1024f, center.y);
            rCenter = new PointF(width*712f/1024f, center.y);

            //绘制资源设置
            mPanel= BitmapFactory.decodeResource(resources,R.drawable.main_panel);
            mHourHand = BitmapFactory.decodeResource(resources,R.drawable.hour_hand);
            mMinuteHand = BitmapFactory.decodeResource(resources,R.drawable.minute_hand);
            mSecondHand = BitmapFactory.decodeResource(resources,R.drawable.second_hand);
            mCenterPoint = BitmapFactory.decodeResource(resources,R.drawable.center);
            mPowerPanel = BitmapFactory.decodeResource(resources, R.drawable.power_panel);
            mStepPanel = BitmapFactory.decodeResource(resources, R.drawable.step_panel);
            mPowerCursor = BitmapFactory.decodeResource(resources, R.drawable.power_cursor);
            mBatteryDrawable = (LevelListDrawable) resources.getDrawable(R.drawable.battery, null);

            //绘制区域设置
            float centerSize = width*52f/(1024f*2f);
            centerSrc = new Rect(0,0,mCenterPoint.getWidth(),mCenterPoint.getHeight());
            centerDst = new RectF(center.x-centerSize, center.y-centerSize, center.x+centerSize, center.y+centerSize);
            float littlePanelSize = width*516f/(1024f*2f);
            lSrc = new Rect(0,0,mPowerPanel.getWidth(),mPowerPanel.getHeight());
            rSrc = new Rect(0,0,mStepPanel.getWidth(),mStepPanel.getHeight());
            lDst = new RectF(lCenter.x-littlePanelSize,lCenter.y-littlePanelSize,lCenter.x+littlePanelSize,lCenter.y+littlePanelSize);
            rDst = new RectF(rCenter.x-littlePanelSize,rCenter.y-littlePanelSize,rCenter.x+littlePanelSize,rCenter.y+littlePanelSize);
            float cursorSize = width*228f/(1024f*2f);
            cursorSrc = new Rect(0,0,mPowerCursor.getWidth(),mPowerCursor.getHeight());
            cursorDst = new RectF(lCenter.x-cursorSize,lCenter.y-cursorSize,lCenter.x+cursorSize,lCenter.y+cursorSize);
            float batterySize = width*94f/(1024f*2);
            mBatteryDrawable.setBounds((int)(lCenter.x-batterySize),(int)(lCenter.y-batterySize),(int)(lCenter.x+batterySize),(int)(lCenter.y+batterySize));
            baseRect= new Rect(0,0,mPanel.getWidth(),mPanel.getHeight());
            littlePanelRect = new Rect(0,0,mPowerPanel.getWidth(),mPowerPanel.getHeight());

            //Paint画笔类
            clockpaint = new Paint();
            clockpaint.setFlags(Paint.FILTER_BITMAP_FLAG);
            clockpaint.setAntiAlias(true);
            textpaint=new Paint();
            textpaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            textpaint.setShadowLayer(0,0,0,Color.BLACK);
            textpaint.setColor(getResources().getColor(R.color.text_color));
            textpaint.setTypeface(Typeface.createFromAsset(getAssets(),"Arame-Regular.otf"));

            //获取电池信息
            battaryrc = new BattaryReceiver();
            filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            getBaseContext().registerReceiver(battaryrc,filter);

            //计步器
            SensorManager manager = (SensorManager) getSystemService(SENSOR_SERVICE);
            Sensor motion = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            manager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    step = (int) event.values[0];
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            }, motion, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            getBaseContext().unregisterReceiver(battaryrc);
            mPanel.recycle();
            mStepPanel.recycle();
            mPowerPanel.recycle();
            mPowerCursor.recycle();
            mHourHand.recycle();
            mMinuteHand.recycle();
            mSecondHand.recycle();
            mCenterPoint.recycle();
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    clockpaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = MyWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    clockpaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
//            mTime.setToNow();

            float centerX = bounds.width() / 2f;
            float centerY = bounds.height() / 2f;

            //绘制表盘
            canvas.save();
            canvas.drawBitmap(mPanel,baseRect,bounds, clockpaint);

            //绘制电量表盘
            canvas.restore();
            canvas.save();
            int x = bounds.width();
            int y = bounds.height();
            canvas.drawBitmap(mPowerPanel, lSrc, lDst, clockpaint);

            //绘制电池图标
            canvas.save();
            canvas.restore();
            mBatteryDrawable.setLevel(powerlevel);
            mBatteryDrawable.draw(canvas);

            //绘制电量指针
            canvas.save();
            canvas.restore();
            float rotateAngle = powerlevel*360/100;
            canvas.rotate(rotateAngle, lCenter.x, lCenter.y);
            canvas.drawBitmap(mPowerCursor, cursorSrc, cursorDst, clockpaint);
            System.out.println("rotate is :"+rotateAngle);

            //绘制计步器
            canvas.restore();
            canvas.save();
            canvas.drawBitmap(mStepPanel, rSrc, rDst, clockpaint);

            //绘制步数
            canvas.restore();
            canvas.save();
            textpaint.setTextSize(0.1f*centerX);
            float textWidth = textpaint.measureText(step+"");
            float textHeight = textpaint.getTextSize();
            canvas.drawText(step+"",rCenter.x-textWidth/2,rCenter.y+textHeight/2,textpaint);

            //绘制日期
            canvas.restore();
            canvas.save();
            textpaint.setTextSize(0.1f*centerX);
            canvas.drawText(getData(),0.9f*centerX,1.55f*centerY, textpaint);

            float secRot = mCalendar.get(Calendar.SECOND) * 6;
            float minRot = mCalendar.get(Calendar.MINUTE) * 6;
            float hrRot = mCalendar.get(Calendar.HOUR) * 30;
            //时针
            canvas.restore();
            canvas.save();
            canvas.rotate(hrRot,centerX,centerY);
            canvas.drawBitmap(mHourHand,baseRect,bounds, clockpaint);
            //分针
            canvas.restore();
            canvas.save();
            canvas.rotate(minRot,centerX,centerY);
            canvas.drawBitmap(mMinuteHand,baseRect,bounds, clockpaint);
            //秒针
            canvas.restore();
            canvas.save();
            canvas.rotate(secRot,centerX,centerY);
            if (!mAmbient) {
                canvas.drawBitmap(mSecondHand,baseRect,bounds, clockpaint);
            }
            //顶点
            clockpaint.setShadowLayer(0,0,0,Color.BLACK);
            canvas.drawBitmap(mCenterPoint,centerSrc,centerDst, clockpaint);

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            if (!insets.isRound()){
                battarylinewidth=3;
            }
        }
    }

    /**
     * calculate the shadow deviation
     * @param theta the angle in degree
     * @return return the deviation in x y
     */
    public float[] getdeviation(double theta){

        float dx=(float)(2*Math.sin((theta+45)/57.3));
        float dy=(float)(2*Math.cos((theta+45)/57.3));

        float[] result = {dx, dy};

        return result;
    }

    public float[] getdeviation(double theta, Boolean out){

        float dx=(float)(3*Math.sin((theta+45)/57.3));
        float dy=(float)(3*Math.cos((theta+45)/57.3));

        float[] result = {dx, dy};
        if(out){
            Log.i("wzjing", "dx=" + dx + "dy=" + dy + "theta="+theta );
        }
        return result;
    }

    /**
     * get the Day
     * @return return the Day
     */
    public static String getData(){
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String mWay = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
        if("1".equals(mWay)){
            mWay ="Sun.";
        }else if("2".equals(mWay)){
            mWay ="Mon.";
        }else if("3".equals(mWay)){
            mWay ="Tues.";
        }else if("4".equals(mWay)){
            mWay ="Wed.";
        }else if("5".equals(mWay)){
            mWay ="Thus.";
        }else if("6".equals(mWay)){
            mWay ="Fri.";
        }else if("7".equals(mWay)){
            mWay ="Sa.";
        }
        return mWay;
    }


    /**
     * receive the battary station
     */
    private class BattaryReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getExtras().getInt("level");
            int scale = intent.getExtras().getInt("scale");
            powerlevel = level*100/scale;
        }
    }

}
