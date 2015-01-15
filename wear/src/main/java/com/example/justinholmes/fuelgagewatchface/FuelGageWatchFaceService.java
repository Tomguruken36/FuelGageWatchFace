package com.example.justinholmes.fuelgagewatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.TimeZone;


public class FuelGageWatchFaceService extends CanvasWatchFaceService {


    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new FuelEngine();
    }

    /* implement service callback methods */
    private class FuelEngine extends CanvasWatchFaceService.Engine {

        public static final int DEGREES_PER_HOUR = 26;
        public static final int HOUR_OFFSET_TENS = 54;
        static final int MSG_UPDATE_TIME = 0;
        static final int INTERACTIVE_UPDATE_RATE_MS = 1000;
        public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND = "Black";
        public static final double DEGREES_PER_MINUTE = 0.45    ;
        public static final int INITAL_Y_OFFSET_TO_SET_ZERO = 58;

        public  final int COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND =
                parseColor(COLOR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND);
        int mInteractiveBackgroundColor = COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND;

        public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_SECOND_DIGITS = "Gray";
        public final int COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS =
                parseColor(COLOR_NAME_DEFAULT_AND_AMBIENT_SECOND_DIGITS);

        private final Typeface BOLD_TYPEFACE =
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        private final Typeface NORMAL_TYPEFACE =
                Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

        /* a time object */
        Time mTime;
        Bitmap mBackgroundBitmap;
        Bitmap mHandBitmap;
        Bitmap mLightNumberBitmap;
        Bitmap mDarkNumberBitmap;
        Paint mBackgroundPaint;
        Paint mForegroundPaint;
        Paint mFilterBitmapPaint;
        Bitmap mBackgroundScaledBitmap;
        boolean mRegisteredTimeZoneReceiver;
        boolean mLowBitAmbient;
        boolean mBurnInProtection;

        boolean mIsRound;
        int mChinSize;


        /* handler to update the time once a second in interactive mode */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };




        /* receiver to update the time zone */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        private int parseColor(String colorName) {
            return Color.parseColor(colorName.toLowerCase());
        }


        @Override
        public void onCreate(SurfaceHolder holder) {
            /* initialize your watch face */
            super.onCreate(holder);
             /* load the background image */

            Resources resources = FuelGageWatchFaceService.this.getResources();
            Drawable handDrawable = resources.getDrawable(R.drawable.hand);
            mHandBitmap = ((BitmapDrawable) handDrawable).getBitmap();

            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg_ccw);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            Drawable darkNumberDrawable = resources.getDrawable(R.drawable.numbers_dark);
            mDarkNumberBitmap = ((BitmapDrawable) darkNumberDrawable).getBitmap();

            Drawable lightNumberDrawable = resources.getDrawable(R.drawable.numbers_light);
            mLightNumberBitmap = ((BitmapDrawable) lightNumberDrawable).getBitmap();


            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mInteractiveBackgroundColor);

            mFilterBitmapPaint = new Paint();
            mFilterBitmapPaint.setFilterBitmap(true);

            mForegroundPaint = new Paint();
            mForegroundPaint.setColor(COLOR_VALUE_DEFAULT_AND_AMBIENT_SECOND_DIGITS);


            setWatchFaceStyle(new WatchFaceStyle.Builder(FuelGageWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setShowUnreadCountIndicator(false)
                    .build());

             /* allocate an object to hold the time */
            mTime = new Time();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            /* get device features (burn-in, low-bit ambient) */
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,
                    false);

        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor);
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            FuelGageWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            FuelGageWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }



        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
            Log.d("onTimeTick", "Tick");
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            /* the wearable switched between modes */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            super.onDraw(canvas,bounds);

            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();

            //TODO:Draw time before background
            int[] minutesArray = null;
            int[] hoursArray = null;

            if(mTime.minute == 0){
                minutesArray = new int[2];
                minutesArray[0] = 0;
                minutesArray[1] = 0;
            } else {
               minutesArray = convertIntToArray(mTime.minute);
                if(mTime.minute < 10){
                    minutesArray[1] = minutesArray[0];
                    minutesArray[0] = 0;
                }

            }


            if(mTime.hour == 0 || mTime.hour == 23){
                hoursArray = new int[2];
                hoursArray[0] = 1;
                hoursArray[1] = 2;
            } else {
                int adjustedHour = mTime.hour;
                if(mTime.hour > 12) {
                    adjustedHour = mTime.hour - 12;
                }
                Log.d("computeFuelAngle", "adjustedHour = " + adjustedHour);

                int length = String.valueOf(adjustedHour).length();
                hoursArray = convertIntToArray(adjustedHour);
                if(length == 1){
                    hoursArray[1] = hoursArray[0];
                    hoursArray[0] = 0;
                }

            }



            Log.d("computeFuelAngle", "minutesArray[0] = " + minutesArray[0]);
            Log.d("computeFuelAngle", "minutesArray[1] = " + minutesArray[1]);
            Log.d("computeFuelAngle", "hoursArray[0] = " + hoursArray[0]);
            Log.d("computeFuelAngle", "hoursArray[1] = " + hoursArray[1]);

            // Draw the background.
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            //Hour 1
            canvas.drawBitmap(mLightNumberBitmap, bounds.width()/2 - HOUR_OFFSET_TENS, bounds.height()/2 + INITAL_Y_OFFSET_TO_SET_ZERO - (41 * hoursArray[0]), mFilterBitmapPaint);
            //Hour 2
            canvas.drawBitmap(mLightNumberBitmap, bounds.width()/2 - 27, bounds.height()/2 + INITAL_Y_OFFSET_TO_SET_ZERO - (41 * hoursArray[1]), mFilterBitmapPaint);
            //Minute 1
            canvas.drawBitmap(mLightNumberBitmap, bounds.width()/2 - 1, bounds.height()/2 + INITAL_Y_OFFSET_TO_SET_ZERO - (41 * minutesArray[0]), mFilterBitmapPaint);
            //Minute 2
            canvas.drawBitmap(mDarkNumberBitmap, bounds.width()/2 + 24, bounds.height()/2 + INITAL_Y_OFFSET_TO_SET_ZERO  - (41 * minutesArray[1]), mFilterBitmapPaint);





            Log.d("onDraw", "Width: " +width );
            Log.d("onDraw", "Height: "+height);


            // Draw the background, scaled to fit.
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            double angleOfRotation = computeFuelAngle(mTime);

            canvas.rotate((float)angleOfRotation, width/2, height/2);
            canvas.drawBitmap(mHandBitmap, 20, 20, mFilterBitmapPaint);



        }

        private int[] convertIntToArray(int hour) {

            String temp = Integer.toString(hour);
            int[] newArray = new int[2];
            for (int i = 0; i < temp.length(); i++) {
                newArray[i] = temp.charAt(i) - '0';
            }
            return newArray;
        }

        private double computeFuelAngle(Time mTime) {
            int hour = mTime.hour;
            int minute = mTime.minute;
            double angle;

            //To account for the 0-23 and 0-59 values that are returned by the mTime;
            hour++;
            minute++;

            Log.d("computeFuelAngle", "hour = " + hour);
            Log.d("computeFuelAngle", "minute = " + minute);

            if(hour > 13){
                Log.d("computeFuelAngle", " hour >= 13 ");
                if(hour >= 18){
                    //Day is over
                    Log.d("computeFuelAngle", " hour > 18 ");
                    angle = -135;
                }else{
                    hour = hour - 13;
                    angle =  (hour * -DEGREES_PER_HOUR) + ((60 - minute) * -DEGREES_PER_MINUTE);
                }
            }else{
                Log.d("computeFuelAngle", " hour < 13 ");
                if(hour < 9){
                    Log.d("computeFuelAngle", " hour < 9 ");
                    //Day hasn't started
                    angle = 135;
                }else{
                    angle =  ((13 - hour) * DEGREES_PER_HOUR) + ( (60 - minute) * DEGREES_PER_MINUTE);
                }
            }
            Log.d("computeFuelAngle", "angle computed = " + angle);

            return angle;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            /* the watch face became visible or invisible */
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode), so we may need to start or stop the timer
            updateTimer();
        }
    }
}