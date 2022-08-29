package com.example.project2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.neuos.INeuosSdk;
import io.neuos.INeuosSdkListener;
import io.neuos.NeuosSDK;

public class MainActivity extends AppCompatActivity {


    Button fifty, twentyFive, proceed, newSession;
    ImageButton backBtn;
    LinearLayout stepOne, stepTwo;
    EditText username;
    HashMap<String, String> values =  new HashMap<String, String>();
    String type;
    // promodoro count fetches from firebase
    // when saving to firebase make sure to have the time stamp with the data
    // then create
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        proceed = findViewById(R.id.proceed);
        stepOne = findViewById(R.id.stepOne);
        stepTwo = findViewById(R.id.stepTwo);
        backBtn = findViewById(R.id.backBtn);
        fifty = findViewById(R.id.longSession);
        twentyFive = findViewById(R.id.shortSession);
        username = findViewById(R.id.username);
        fifty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateBtn(fifty,twentyFive,"50");
                Log.d("val","50");
            }
        });
        twentyFive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateBtn(twentyFive,fifty,"25");
                Log.d("val","25");
            }
        });
        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO check that the values are there (no empty values)
                submit();
                values.entrySet().forEach(entry -> {
                    Log.d("Values:",entry.getKey() + " " + entry.getValue());
                });
                stepOne.setVisibility(View.GONE);
                stepTwo.setVisibility(View.VISIBLE);
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stepTwo.setVisibility(View.GONE);
                stepOne.setVisibility(View.VISIBLE);
            }
        });
    }

    public void startSession(View view) {
        Intent intent = new Intent(this, BrainActivity.class);
        startActivity(intent);
    }

    public void activateBtn(Button activeBtn, Button deactivateBtn, String value){
        activeBtn.setBackgroundResource(R.drawable.activebtn);
        activeBtn.setTextColor(Color.WHITE);
        deactivateBtn.setBackgroundResource(R.drawable.buttonback);
        deactivateBtn.setTextColor(Color.BLACK);
        type =  value;
    }
    public void submit(){
        String date= getTimeNow("MM-dd-yyyy");
        String name = String.valueOf(username.getText());
        values.put("name",name);
        values.put("type", type);
        values.put("date", date);
        Intent intent = new Intent(this, BrainActivity.class);
        intent.putExtra("values",values);
        startActivity(intent);
    }
    public String getTimeNow(String format){
        Date date =  new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }
}
