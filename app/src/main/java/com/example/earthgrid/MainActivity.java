package com.example.earthgrid;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor pressure;
    private Sensor light;
    private Sensor magnetic;
    MqttHelper mqtt;
    double lastLon = 0;
    double lastLat = 0;
    double lastPressure = 0;
    double lastLuminance = 0;
    double lastMagX = 0;
    double lastMagY = 0;
    double lastMagZ = 0;
    long lastPublish = 0;

    TextView pressureTextView;
    TextView luminanceTextView;
    TextView magxTextView;
    TextView magyTextView;
    TextView magzTextView;
    TextView lonTextView;
    TextView latTextView;
    TextView connectedTextView;
    TextView publishTextView;

    boolean mqttConnected = false;

    public class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d("LocationListener", location.toString());
            lastLon = location.getLongitude();
            lastLat = location.getLatitude();

            lonTextView.setText("Longitude: " + lastLon);
            latTextView.setText("Latitude: " + lastLat);
            publish();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            Log.d("Sensor", getType(sensor.getType()));
        }

        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        pressureTextView = findViewById(R.id.pressureTextView);
        luminanceTextView = findViewById(R.id.luminanceTextView);
        magxTextView = findViewById(R.id.magneticXTextView);
        magyTextView = findViewById(R.id.magneticYTextView);
        magzTextView = findViewById(R.id.magneticZTextView);
        lonTextView = findViewById(R.id.lonTextView);
        latTextView = findViewById(R.id.latTextView);
        connectedTextView = findViewById(R.id.connectedTextView);
        publishTextView = findViewById(R.id.publishTextView);

        startMqtt();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Need permission for GPS");
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private String getType(int type) {
        String strType;
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER: strType = "TYPE_ACCELEROMETER";break;
            case Sensor.TYPE_GRAVITY:strType = "TYPE_GRAVITY";break;
            case Sensor.TYPE_GYROSCOPE:    strType = "TYPE_GYROSCOPE";    break;
            case Sensor.TYPE_LIGHT:strType = "TYPE_LIGHT";break;
            case Sensor.TYPE_LINEAR_ACCELERATION:strType = "TYPE_LINEAR_ACCELERATION";
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:strType = "TYPE_MAGNETIC_FIELD";break;
            case Sensor.TYPE_ORIENTATION:strType = "TYPE_ORIENTATION";break;
            case Sensor.TYPE_PRESSURE:strType = "TYPE_PRESSURE";break;
            case Sensor.TYPE_PROXIMITY:    strType = "TYPE_PROXIMITY";    break;
            case Sensor.TYPE_ROTATION_VECTOR:    strType = "TYPE_ROTATION_VECTOR";break;
            case Sensor.TYPE_TEMPERATURE:strType = "TYPE_TEMPERATURE";break;
            default: strType = "TYPE_UNKNOW";break;
        }
        return strType;
    }

    private void startMqtt () {
        mqtt = new MqttHelper(getApplicationContext());
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w("MainActivity", "======== MQTT connected =====");
                mqttConnected = true;
                connectedTextView.setText("Connected: " + mqttConnected);
            }

            @Override
            public void connectionLost(Throwable cause) {
                mqttConnected = false;
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void publish() {
        long currentMillis = System.currentTimeMillis();
        if (currentMillis - lastPublish > 2000 && mqttConnected) {
            try {
                String payload = new JSONObject()
                        .put("from", "device02")
                        .put("pos", new JSONObject()
                                .put("lon", lastLon)
                                .put("lat", lastLat))
                        .put("data", new JSONObject()
                                .put("pressure", lastPressure)
                                .put("luminance", lastLuminance)
                                .put("magnetic", new JSONObject()
                                        .put("x", lastMagX)
                                        .put("y", lastMagY)
                                        .put("z", lastMagZ)))
                        .toString();

                mqtt.publish(payload);
                publishTextView.setText("Last publish: " + currentMillis);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            lastPublish = currentMillis;
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            float millibarsOfPressure = event.values[0];
            lastPressure = millibarsOfPressure;
            Log.d("MainActivity", "Pressure: " + millibarsOfPressure);
            pressureTextView.setText("Pressure: " + lastPressure);
        }

        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            lastLuminance = event.values[0];
            Log.d("MainActivity", "Luminance: " +lastLuminance);
            luminanceTextView.setText("Luminance: " + lastLuminance);
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            lastMagX = event.values[0];
            lastMagY = event.values[1];
            lastMagZ = event.values[2];
            Log.d("MainActivity", "Magnetic: x: " + lastMagX + ", y:" + lastMagY + ", z:" + lastMagZ);
            magxTextView.setText("Magnetic x: " + lastMagX);
            magyTextView.setText("Magnetic y: " + lastMagY);
            magzTextView.setText("Magnetic z: " + lastMagZ);
        }

        publish();
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
