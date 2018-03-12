package sk.dotcube.ev3;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity{

    private Button saveButton;
    private EditText ip, motorLeft, motorRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        ip = (EditText) findViewById(R.id.EV3IPAddress);
        motorLeft = (EditText) findViewById(R.id.motorLeft);
        motorRight = (EditText) findViewById(R.id.motorRight);
        saveButton = (Button) findViewById(R.id.button);

        saveButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Preferences.writeString(getApplicationContext(), Preferences.IP, ip.getText().toString());
                Preferences.writeString(getApplicationContext(), Preferences.MOTOR_LEFT, motorLeft.getText().toString());
                Preferences.writeString(getApplicationContext(), Preferences.MOTOR_RIGHT, motorRight.getText().toString());
                Preferences.writeString(getApplicationContext(), Preferences.ULTRASONIC_PORT, "S4");
                Preferences.writeString(getApplicationContext(), Preferences.COLOR_PORT, "S1");
            }
        });
    }
}