package sk.dotcube.ev3;

import android.content.Intent;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lejos.remote.ev3.RemoteRequestEV3;
import lejos.robotics.RegulatedMotor;

public class SpeechControl extends AppCompatActivity implements OnClickListener {

    private ListView commands;
    private RemoteRequestEV3  ev3;
    private RegulatedMotor left, right;
    private TextView intro;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_control);
        Button speechBtn = (Button) findViewById(R.id.speech_btn);
        Button connectBtn = (Button) findViewById(R.id.connect_btn);
        //Button disconnectBtn = (Button) findViewById(R.id.disconnect_btn);
        //Button stopBtn = (Button) findViewById(R.id.stop_btn);
        connectBtn.setOnClickListener(this);
        commands = (ListView) findViewById(R.id.command_list);
        intro = (TextView) findViewById(R.id.header_text);
        // Check if speech recognition activity is available
        List<android.content.pm.ResolveInfo> activities = getPackageManager().queryIntentActivities
                (new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() > 0) speechBtn.setOnClickListener(this);
        else Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_LONG).show();

        // Nasty hack to allow network access in main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); StrictMode.setThreadPolicy(policy);

        // Choose a command from the list
        commands.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                doCmd((String) ((TextView) view).getText());
            }
        });
    }

    // When button is clicked, listen for a command
    public void onClick(View v) {
        if (v.getId() == R.id.speech_btn) listen();
        else if (v.getId() == R.id.connect_btn && ev3 == null) {
            try {
                ev3 = new RemoteRequestEV3(MainActivity.HOST);
                left = ev3.createRegulatedMotor("A", 'L');
                right = ev3.createRegulatedMotor("B", 'L');
            } catch (IOException e) {
                Toast.makeText(this, "Could not connect to EV3", Toast.LENGTH_LONG).show();
            }
        } else if (v.getId() == R.id.disconnect_btn && ev3 != null) {
            left.close();
            right.close();
            ev3.disConnect();
            ev3 = null;
        } else if (v.getId() == R.id.stop_btn && ev3 != null) {
            left.stop(true);
            right.stop(true);
        }
    }

    private int REQUEST_CODE = 100;

    /*
     * Start the listen activity
     */
    private void listen() {
        Intent listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        listenIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        listenIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Robot command");
        listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        int MAX_RESULTS = 5;
        listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS);

        startActivityForResult(listenIntent, REQUEST_CODE);
    }

    /*
     * Get the listen results
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> suggestions = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            commands.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, suggestions));

            for(String cmd: suggestions) {
                if (validCommand(cmd)) {
                    doCmd(cmd);
                    break;
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Send a command to the EV3
    private void doCmd(String msg) {
        if (ev3 != null) {
            ev3.getAudio().systemSound(1);
            intro.setText("Battery: " + ev3.getPower().getVoltage());
            if (msg.contains("forward")) {
                left.forward();
                right.forward();
            } else if (msg.contains("backward")) {
                left.backward();
                right.backward();
            } else if (msg.contains("stop")) {
                left.stop(true);
                right.stop(true);
            } else if ((msg.contains("rotate") || msg.contains("turn")) && msg.contains("left")) {
                left.backward();
                right.forward();
            } else if ((msg.contains("rotate") || msg.contains("turn")) && msg.contains("right")) {
                left.forward();
                right.backward();
            }
        } else Toast.makeText(this, "Not connected to EV3", Toast.LENGTH_LONG).show();
    }

    private boolean validCommand(String cmd) {
        return (cmd.contains("stop") || cmd.contains("forward") || cmd.contains("backward") || cmd.contains("rotate"));
    }
}
