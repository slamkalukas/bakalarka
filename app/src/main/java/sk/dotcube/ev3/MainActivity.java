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

import lejos.hardware.Keys;
import lejos.hardware.motor.Motor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.remote.ev3.RemoteRequestEV3;
import lejos.remote.ev3.RemoteRequestPilot;
import lejos.robotics.SampleProvider;
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
    private RemoteRequestEV3 evBrick;
    private EV3ColorSensor cSensor;
    private float[] colourSample, ultrasonicDistSample;
    private int colourData = 0;
    private Button connButton1,connButton2;
    private Boolean stop1,stop2 = false;
    private EV3UltrasonicSensor uSensor;
    private static final int SCAN_DELAY = 70;
    private static final int REPEAT_SCAN_TIMES = 20;
    private static final double SCAN_STABLE_THRESHOLD = 0.5;
    private static final float OCCUPIED_THRESHOLD = 30;

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

        new Control().execute("connect", HOST);

        connButton1 = (Button) findViewById(R.id.Color);
        connButton1.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Control().execute("color");
            }
        });
        connButton1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                stop1 = true;
                return true;
            }
        });

        connButton2 = (Button) findViewById(R.id.Distance);
        connButton2.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Control().execute("distance");
            }
        });
        connButton2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                stop2 = true;
                return true;
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

    // get one single distance (gurantee a non-INFINITY return)
    private float getOneDistance() {
        try {
            Thread.sleep(SCAN_DELAY);
        } catch (InterruptedException e) {
            // this won't happen
        }
        // ask ultrasonic sensor for raw data
        ultrasonicDistSP.fetchSample(ultrasonicDistSample, 0);
        float distance = ultrasonicDistSample[0] * 100;
        // if the distance is INIFINITY, try to move back or rotate a little bit
        // to avoid it
        int count = 1;
        // repeat the following actions until a non-INIFINITY number is returned
        while (Float.isInfinite(distance)) {
            // move back and scan again
            if (count == 1) {
                pilot.travel(-1);
                ultrasonicDistSP.fetchSample(ultrasonicDistSample, 0);
                distance = ultrasonicDistSample[0] * 100;
                count = -1;
                // if moving back does not work, rotate one degree and scan
                // again
            } else if (count == -1) {
                pilot.rotate(1);
                ultrasonicDistSP.fetchSample(ultrasonicDistSample, 0);
                distance = ultrasonicDistSample[0] * 100;
                pilot.rotate(-1);
                count = 1;
            }
        }
        return distance;
    }

    // get a stable and accruate distance
    private double getAccurateDistance() {
        double average = 0;
        while (true) {
            // get a set of data by scanning and compute an average
            average = 0;
            float[] distances = new float[REPEAT_SCAN_TIMES];
            for (int i = 0; i <= REPEAT_SCAN_TIMES - 1; i++) {
                distances[i] = getOneDistance();
                average += distances[i] / REPEAT_SCAN_TIMES;
            }
            // if each distance we got is close to the average, then this set of
            // data is reliable
            // if any distance in the data set is very different from the
            // average, then this set
            // of data will be considered unreliable and will be scanned again
            for (int i = 0; i <= REPEAT_SCAN_TIMES - 1; i++) {
                if (Math.abs(average - distances[i]) > SCAN_STABLE_THRESHOLD) {
                    continue;
                }
            }
            break;
        }
        System.out.println(average);
        return average;
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
        try {
            cSensor = new EV3ColorSensor(ev3.getPort("S1"));
            colourSP = cSensor.getRGBMode();
            colourSample = new float[colourSP.sampleSize()];
        } catch (Exception e)
        {
            System.out.print(e);
        }
    }

    private void setupUltrasonicSensor() {
        try {
            uSensor = new EV3UltrasonicSensor(ev3.getPort("S4"));
            ultrasonicDistSP = uSensor.getDistanceMode();
            ultrasonicDistSample = new float[ultrasonicDistSP.sampleSize()];
        } catch (Exception e)
        {
            System.out.print(e);
        }
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

    public class Control extends AsyncTask<String, Integer, Long> {
        protected Long doInBackground(String... cmd) {
            switch (cmd[0]) {
                case "connect":
                    try {
                        ev3 = new RemoteRequestEV3(cmd[1]);
                        pilot = (RemoteRequestPilot) ev3.createPilot(3.5f, 20f, "B", "C");
                        pilot.setLinearSpeed(30f);
                        setupColorSensor();
                        setupUltrasonicSensor();
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
                    pilot.backward();
                    break;
                case "forward":
                    if (ev3 == null)
                        return 2l;
                    pilot.forward();
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
                case "color":
                    if (ev3 == null)
                        return 2l;
                        stop1 = false;
                        do {
                            int theColor = cSensor.getColorID();
                            System.out.println("My color is " + theColor);
                            Delay.msDelay(200);
                        } while (!stop1);
                        stop1 = false;
                case "distance":
                    if (ev3 == null)
                        return 2l;
                    stop2 = false;
                        do {
                            Double theDistance = getAccurateDistance();
                            System.out.println("My distance is " + theDistance);
                            Delay.msDelay(200);
                        } while (!stop2);
                        stop2 = false;
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
