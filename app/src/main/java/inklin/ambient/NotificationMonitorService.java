package inklin.ambient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.util.Log;

import static java.lang.Math.abs;

/**
 * Created by acaoa on 2017/10/1.
 */

public class NotificationMonitorService extends NotificationListenerService {
    private final BroadcastReceiver receiver = new BroadcastReceiver(){
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if(intent!= null){
                if(TextUtils.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF))
                    context.startService(new Intent(context, NotificationMonitorService.class)
                            .putExtra("start", true));
                else if(TextUtils.equals(intent.getAction(), Intent.ACTION_SCREEN_ON))
                    context.startService(new Intent(context, NotificationMonitorService.class)
                            .putExtra("stop", true));
            }
        }

    };

    float[] ovalue = new float[3];
    float offset_value = 2;
    float offset_value2 = 1.5f;
    float offset_Y = 1;
    long otime = 0;
    int offset_time = 2000;

    private SensorManager mSensorManager;
    @Override
    public void onCreate(){
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(receiver, filter);

        updateValue();
    }
    private void updateValue(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        offset_value = Float.parseFloat(sp.getString("offset_trigger", "2"));
        offset_value2 = Float.parseFloat(sp.getString("offset_fluctuation", "1"));
        offset_Y = Float.parseFloat(sp.getString("offset_y", "1"));
        offset_time = Integer.parseInt(sp.getString("offset_time", "2000"));
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if(intent != null){
            if(intent.hasExtra("start")){
                Log.v("onStartCommand", "start");
                mSensorManager.registerListener(prox,
                        mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                        SensorManager.SENSOR_DELAY_NORMAL);
            }else if(intent.hasExtra("stop")){
                Log.v("onStartCommand", "stop");
                mSensorManager.unregisterListener(prox);
                mSensorManager.unregisterListener(acc);
            }else if(intent.hasExtra("update")){
                updateValue();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    SensorEventListener prox = new SensorEventListener() {
        boolean recover = false;
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.values[0] == 0){
                mSensorManager.unregisterListener(acc);
                recover = false;
            }else{
                recover = true;
                mSensorManager.registerListener(acc,
                        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener acc = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] value = event.values;
            float dx = value[0] - ovalue[0];
            float dy = value[1] - ovalue[1];
            long time = System.currentTimeMillis();

            if(abs(dx) > offset_value || abs(dy) > offset_value) {
                //Log.v("dx/dy", value[0] + "/" + value[1] + "/" + value[2]);
                otime = time;//wakeUp();
            }
            if(time - otime < offset_time &&
                    abs(dx) < offset_value2 && abs(dy) < offset_value2 &&
                    value[1] > offset_Y && value[2] > abs(value[0])) {
                wakeUp();
                otime = 0;
            }
            ovalue = value.clone();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private void wakeUp(){
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("ambient")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_MIN)
                .setDefaults(Notification.DEFAULT_LIGHTS);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, builder.build());
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
    }
}
