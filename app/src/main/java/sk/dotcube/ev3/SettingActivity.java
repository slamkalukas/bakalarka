package sk.dotcube.ev3;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Objects;

import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.lcd.LCD;
import lejos.hardware.lcd.TextLCD;
import lejos.remote.ev3.RemoteRequestEV3;
import lejos.remote.ev3.RemoteRequestPilot;
import lejos.utility.Delay;

public class SettingActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private static final float ZERO_X = 0f, ZERO_Y = 7.5f, LEEWAY = 0.5f,
            SPEED_FACTOR = 50f, TURN_FACTOR = 20f;
    public String forward, backward, left, right, direction;
    private TextView xAccel, yAccel;
    private float x = 0, y = 0, z = 0;
    public static final String HOST = "10.40.50.30";
    private RemoteRequestEV3 ev3;
    private RemoteRequestPilot pilot;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);



        xAccel = (TextView) findViewById(R.id.xAccel);
        yAccel = (TextView) findViewById(R.id.yAccel);

        //new SettingActivity.Control().execute("connect", HOST);

    }

    @Override
    protected void onResume() {
        sensorManager.registerListener(SettingActivity.this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    @Override
    protected void onStop() {
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];

        xAccel.setText("x = " + x);
        yAccel.setText("y = " + y);

        /*if (x > y)
            if (x > 0f)
            direction = left;
        else direction = right;
        if (y > x)
            if (y > 0f)
                direction = forward;
        else direction = backward;

        if (Objects.equals(direction, forward))
            new SettingActivity.Control().execute("forward");
        else if (Objects.equals(direction, backward))
            new SettingActivity.Control().execute("backward");
        else if (Objects.equals(direction, left))
            new SettingActivity.Control().execute("left");
        else if (Objects.equals(direction, right))
            new SettingActivity.Control().execute("right");

        yAccel.setText(direction);*/

        new Control().execute("forward");
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected( MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.arrow_control) {
            // Handle the camera action
        } else if (id == R.id.settings) {
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        new SettingActivity.Control().execute("close", HOST);
        Toast.makeText(SettingActivity.this, "Disconnecting…",
                Toast.LENGTH_SHORT).show();
    }

    public class Control extends AsyncTask<String, Integer, Long> {
        protected Long doInBackground(String... cmd) {
            switch (cmd[0]) {
                case "connect":
                    try {
                        ev3 = new RemoteRequestEV3(cmd[1]);
                        pilot = (RemoteRequestPilot) ev3.createPilot(3.5f, 20f, "A", "B");
                        pilot.setLinearSpeed(20f);
                    } catch (IOException e) {
                        return 1L;
                    }
                    break;
                case "left":
                    if (ev3 == null)
                        return 2L;
                    pilot.rotateLeft();
                    break;
                case "right":
                    if (ev3 == null)
                        return 2L;
                    pilot.rotateRight();
                    break;
                case "backward":
                    if (ev3 == null)
                        return 2L;
                    pilot.forward();
                    break;
                case "forward":
                    if (ev3 == null)
                        return 2L;
                    pilot.backward();
                    break;
                case "stop":
                    if (ev3 == null)
                        return 2L;
                    pilot.stop();
                    break;
                case "close":
                    if (ev3 == null)
                        return 2L;
                    pilot.stop();
                    ev3.disConnect();
                    break;
            }
            return 0L;
        }
    }

    protected void onPostExecute(Long result) {
        if (result == 1L)
            Toast.makeText(SettingActivity.this, "Could not connect to EV3",
                    Toast.LENGTH_LONG).show();
        else if (result == 2L)
            Toast.makeText(SettingActivity.this, "Not connected",
                    Toast.LENGTH_LONG).show();
    }
}