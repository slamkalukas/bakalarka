package sk.dotcube.ev3;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.util.Objects;

import lejos.remote.ev3.RemoteRequestEV3;
import lejos.remote.ev3.RemoteRequestPilot;
import lejos.utility.Delay;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnTouchListener, SensorEventListener {

    private ImageButton forward, backward, left, right;
    public static final String HOST = "10.40.50.30";
    private RemoteRequestEV3 ev3;
    private RemoteRequestPilot pilot;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(MainActivity.this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        new Control().execute("connect", HOST);

        //EV3 code part

        forward = (ImageButton) findViewById(R.id.btnForward);
        forward.setOnTouchListener(this);

        backward = (ImageButton) findViewById(R.id.btnBackward);
        backward.setOnTouchListener(this);

        left = (ImageButton) findViewById(R.id.btnLeft);
        left.setOnTouchListener(this);

        right = (ImageButton) findViewById(R.id.btnRight);
        right.setOnTouchListener(this);

        /*Button connButton = (Button) findViewById(R.id.EV3connect);
        connButton.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                new Control().execute("connect", HOST);
            }
        });

        Button disconnButton = (Button) findViewById(R.id.EV3disconnect);
        disconnButton.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                new Control().execute("close", HOST);
            }
        });*/

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String direction = "stop";
        float x = event.values[0];
        float y = event.values[1];
        
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            if (x > y)
                if (x > 0f)
                    direction = "left";
                else direction = "right";
            if (y > x)
                if (y > 0f)
                    direction = "forward";
                else direction = "backward";

            if (Objects.equals(direction, "forward"))
                new Control().execute("forward");
            else if (Objects.equals(direction, "backward"))
                new Control().execute("backward");
            else if (Objects.equals(direction, "left"))
                new Control().execute("left");
            else if (Objects.equals(direction, "right"))
                new Control().execute("right");
            else if (Objects.equals(direction, "stop"))
                new Control().execute("stop");
            
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
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
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP)
            new Control().execute("stop");
        else if (action == MotionEvent.ACTION_DOWN) {
            if (v == forward)
                new Control().execute("forward");
            else if (v == backward)
                new Control().execute("backward");
            else if (v == left)
                new Control().execute("left");
            else if (v == right)
                new Control().execute("right");
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        new Control().execute("close", HOST);
        Toast.makeText(MainActivity.this, "Disconnecting…",
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
                    pilot.close();
                    ev3.disConnect();
                    break;
            }
            return 0L;
        }

        protected void onPostExecute(Long result) {
            if (result == 1L)
                Toast.makeText(MainActivity.this, "Could not connect to EV3",
                        Toast.LENGTH_LONG).show();
            else if (result == 2L)
                Toast.makeText(MainActivity.this, "Not connected",
                        Toast.LENGTH_LONG).show();
        }
    }

}
