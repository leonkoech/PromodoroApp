package com.example.project2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
    FirebaseFirestore database = FirebaseFirestore.getInstance();

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
        newSession = findViewById(R.id.startSession);
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
//                countSessions();
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
        newSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newSession();
            }
        });
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
        String uid = String.valueOf(username.getText()).toLowerCase().replaceAll("\\s+", "_");
        values.put("name",name);
        values.put("uid", uid);
        values.put("type", type);
        values.put("date", date);
    }
    public String getTimeNow(String format){
        Date date =  new Date();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }
    public void newSession(){
        Intent intent = new Intent(this, BrainActivity.class);
        intent.putExtra("values",values);
        startActivity(intent);
    }
    public void countSessions(){
        String TAG = "firebase stuff";
        database.collection("promodoro")
                .document(Objects.requireNonNull(values.get("uid")))
                .collection(Objects.requireNonNull(values.get("type")))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int count = 0;
                            for (DocumentSnapshot document : task.getResult()) {
                                count++;
                            }
                            values.put("sessionsCount",String.valueOf(count));
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }
}
