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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.Keys;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.remote.ev3.RMISampleProvider;
import lejos.remote.ev3.RemoteEV3;
import lejos.remote.ev3.RemoteRequestEV3;
import lejos.remote.ev3.RemoteRequestPilot;
import lejos.robotics.Color;
import lejos.robotics.ColorIdentifier;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.PublishedSource;
import lejos.robotics.filter.SubscribedProvider;
import lejos.utility.Delay;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnTouchListener, SensorEventListener {

    private ImageButton forward, backward, left, right;
    public static final String HOST = "10.40.50.30";
    private RemoteRequestEV3 ev3;
    private RemoteRequestPilot pilot;
    private SensorManager sensorManager;
    private Sensor mTouch;
    private float x = 0, y = 0, z = 0;
    private SampleProvider colourSP, ultrasonicDistSP;
    private TextView xAccel, yAccel, zAccel;
    private Brick evBrick;
    private EV3ColorSensor cSensor;
    private float[] colourSample, ultrasonicDistSample;
    private int colourData = 0;

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

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mTouch = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        xAccel = (TextView) findViewById(R.id.xAccel);
        yAccel = (TextView) findViewById(R.id.yAccel);
        zAccel = (TextView) findViewById(R.id.zAccel);

        //EV3 code part

        forward = (ImageButton) findViewById(R.id.btnForward);
        forward.setOnTouchListener(this);

        backward = (ImageButton) findViewById(R.id.btnBackward);
        backward.setOnTouchListener(this);

        left = (ImageButton) findViewById(R.id.btnLeft);
        left.setOnTouchListener(this);

        right = (ImageButton) findViewById(R.id.btnRight);
        right.setOnTouchListener(this);

        new Control2().execute("connect");

        /*Button connButton = (Button) findViewById(R.id.EV3connect);
        connButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ev32 = new RemoteRequestEV3(HOST);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                SampleProvider sensor = ev32.createSampleProvider("S1", "lejos.hardware.sensor.EV3ColorSensor", "Color ID");
                float[] color_id= new float[1];
                sensor.fetchSample(color_id, 0);
                System.out.println("Here is color id: " + color_id);
            }
        });*/

        Button connButton = (Button) findViewById(R.id.EV3connect);
        connButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Control2().execute("color");
            }
        });

        Button connButton1 = (Button) findViewById(R.id.EV3connect2);
        connButton1.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Control().execute("connect", HOST);
            }
        });


    }

    protected void onResume() {
        sensorManager.registerListener(this, mTouch, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                xAccel.setText("x = " + x);
                yAccel.setText("y = " + y);
                zAccel.setText("z = " + z);
            } else {
                xAccel.setText("NEFUNGUJE!");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void setupColorSensor() {
        cSensor = new EV3ColorSensor(evBrick.getPort("S1"));
        colourSP = cSensor.getRGBMode();
        colourSample = new float[colourSP.sampleSize()];
        colourData = cSensor.getColorID();
    }

    /* sensor methods */

    // get raw data from the color sensor (RGB mode)
    private float[] getRawColorData() {
        colourSP.fetchSample(colourSample, 0);
        return colourSample;
    }

    private String getRGBData() {
        float[] colorData = getRawColorData();
        float r = colorData[0];
        float g = colorData[1];
        float b = colorData[2];
        return "r " + r*100 + " g " + g*100 + " b " +b*100;
    }

    private class Control2 extends AsyncTask<String, Integer, Long> {

        private int numSamples = 0;
        protected Long doInBackground(String... cmd) {
            if (cmd[0].equals("connect")) {
                try {
                    evBrick = new RemoteRequestEV3(HOST);
                    setupColorSensor();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                } else if (cmd[0].equals("color")) {
                    Keys keys1 = evBrick.getKeys();
                    do {
                        int theColor = cSensor.getColorID();
                        System.out.println("My color is " + theColor);
                        Delay.msDelay(200);
                    } while (keys1.getButtons() != lejos.hardware.Button.ID_LEFT);
                    cSensor.close();
                    return (long) numSamples;
                } else if (cmd[0].equals("close")) {
                    cSensor.close();
                    return (long) numSamples;
                }
                return (long) numSamples;
            }

        protected void onPostExecute(Long result) {
            Toast.makeText(MainActivity.this, "Number of samples: " + result,
                    Toast.LENGTH_LONG).show();
        }
    }

    public class Control extends AsyncTask<String, Integer, Long> {
        protected Long doInBackground(String... cmd) {
            switch (cmd[0]) {
                case "connect":
                    try {
                        ev3 = new RemoteRequestEV3(cmd[1]);
                        pilot = (RemoteRequestPilot) ev3.createPilot(3.5f, 20f, "B", "C");
                        pilot.setLinearSpeed(30f);
                    } catch (IOException e) {
                        return 1l;
                    }
                    break;
                case "left":
                    if (ev3 == null)
                        return 2l;
                    pilot.rotateRight();
                    break;
                case "right":
                    if (ev3 == null)
                        return 2l;
                    pilot.rotateLeft();
                    break;
                case "backward":
                    if (ev3 == null)
                        return 2l;
                    pilot.forward();
                    break;
                case "forward":
                    if (ev3 == null)
                        return 2l;
                    pilot.backward();
                    break;
                case "stop":
                    if (ev3 == null)
                        return 2l;
                    pilot.stop();
                    break;
                case "bump":
                    if (ev3 == null)
                        return 2l;
                    pilot.stop();
                    pilot.travel(-20);
                    pilot.rotate(180);
                    pilot.travel(10);
                    break;
                case "close":
                    if (ev3 == null)
                        return 2l;
                    pilot.stop();
                    ev3.disConnect();
                    break;
            }
            return 0l;
        }

        protected void onPostExecute(Long result) {
            if (result == 1l)
                Toast.makeText(MainActivity.this, "Could not connect to EV3",
                        Toast.LENGTH_LONG).show();
            else if (result == 2l)
                Toast.makeText(MainActivity.this, "Not connected",
                        Toast.LENGTH_LONG).show();
        }
    }

}
