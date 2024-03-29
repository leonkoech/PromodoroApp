package com.example.project2;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.neuos.INeuosSdk;
import io.neuos.INeuosSdkListener;
import io.neuos.NeuosQAProperties;
import io.neuos.NeuosSDK;


public class BrainActivity extends AppCompatActivity {


    private AlertDialog loadingDialog;
    final String TAG = "Neuos SDK";
    private TextView mytext;
    private Button fetch;
    // TODO: This API key should be elsewhere
    final String API_KEY = "nfAi32Ttc13SXWQP4";
    private INeuosSdk mService;
    private Runnable mPostConnection;
    private BrainActivity.DeviceConnectionReceiver deviceListReceiver;
    private int counter = 0;
    private int minuteCounter= 0; // counts 1 every minute
//    private Map<Object, Object> info = new HashMap<>();
//    private float EnjoymentArr[] = new float[26];
//    private float FocusArr[] = new float[26];
//    private ArrayList<Map<Object, Object>> finalResFocus= new ArrayList<Map<Object, Object>>();
    // use arraylist, it's expandable
    // because data is rare just record anything that is not 0 or NaN every 5 seconds
    ArrayList<Float> enjoyment = new ArrayList<Float>();
    ArrayList<Float> focus = new ArrayList<Float>();
    // I decided to record the data every 5 seconds as [ {time:data},{time:data} ] for focus and enjoyment
    // then create a dict that takes all this at the end of the session

    
    FirebaseFirestore database = FirebaseFirestore.getInstance();

    TextView starter, mainCounter;
    Button endSession, launchButton;
    LinearLayout initialCounterScreen, mainCounterScreen;
    CountDownTimer timer = null;
    String name;
    String uid;
    String sessionsCount;
    HashMap<String, String> values;
    boolean started=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register to receive device pairing callbacks
        deviceListReceiver = new BrainActivity.DeviceConnectionReceiver();
        registerReceiver(deviceListReceiver,
                new IntentFilter(NeuosSDK.IO_NEUOS_DEVICE_PAIRING_ACTION));
        // Start the flow by verifying the permissions to use the SDK
        checkSDKPermissions();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(R.layout.progress_dialog);
        // This should be called once in your Fragment's onViewCreated() or in Activity onCreate() method to avoid dialog duplicates.
        loadingDialog = builder.create();
        setContentView(R.layout.activity_brain);
        launchButton = findViewById(R.id.startSession);
//        enjoyment = findViewById(R.id.enjoyment);
//        focus = findViewById(R.id.focus);
//        timer = findViewById(R.id.timer);
        starter = findViewById(R.id.startCountdown);
        mainCounter = findViewById(R.id.mainCounter);
        initialCounterScreen = findViewById(R.id.initialCounterScreen);
        mainCounterScreen  = findViewById(R.id.mainCounterScreen);
        endSession = findViewById(R.id.killSession);

        Intent intent = getIntent();
        values = (HashMap<String, String>) intent.getSerializableExtra("values");
        name =  Objects.requireNonNull(values.get("name"));
        uid = Objects.requireNonNull(values.get("uid"));
//        sessionsCount = Objects.requireNonNull(values.get("sessionsCount"));
        sessionsCount = String.valueOf(0);
        endSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killTimer();
            }
        });
        // add the basic user information to firebase
//        addUserDataToFirebase();
        // This begins the flow of login -> check calibration -> start session -> Qa -> display data
//        checkNeuosLoginStatus();

    }
    public String getTimeNow(String format){
        Date date =  new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }
    public void startTimer(int seconds,TextView initialCounter, TextView mainCounter, boolean initial){
        int totalTime = seconds*1000;
        int sessionTime = Integer.parseInt(values.get("type"))*60;
        timer  = new CountDownTimer(totalTime, 1000) {
            public void onTick(long millisUntilFinished) {
                if(initial){
                    initialCounter.setText("00:" + String.format("%02d",millisUntilFinished / 1000));
                }
                else{
                    initialCounter.setText("" +String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes( millisUntilFinished),
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
                }
            }
            public void onFinish() {
                if(initial) {
                    startMainCounter(sessionTime, initialCounter, mainCounter);
                }
                else{
                    String startTime  = getTimeNow("HH:mm:ss");
                    values.put("end time",startTime);
                    //TODO end session automatically here
                    // display loading page here while uploading to firebase and thank the user
//                    checkNeuosLoginStatus();
                }
            }
        }.start();
    }
    public void killTimer(){
        if(timer!=null){
            timer.cancel();
            endSession.setText("Back");
            endSession.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String startTime  = getTimeNow("HH:mm:ss");
                    values.put("end time",startTime);
                    finish();
                }
            });
        }
    }
    public void startMainCounter(int time, TextView initialCounter, TextView mainCounter){
        initialCounterScreen.setVisibility(View.GONE);
        mainCounterScreen.setVisibility(View.VISIBLE);
        startTimer(time, mainCounter, initialCounter,false);
        String startTime  = getTimeNow("HH:mm:ss");
        values.put("start time",startTime);
    }

    @Override
    protected void onDestroy() {
        try {
            // clean up the service
            mService.shutdownSdk();
            unregisterReceiver(deviceListReceiver);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
    public void addUserDataToFirebase(){
        HashMap<String, String> userInformation = new HashMap<String, String>();
        userInformation.put("name",name);
        userInformation.put("uid",uid);
        database.collection("promodoro").document(uid).set(userInformation);
        startTimer(10,starter,mainCounter, true);
    }
    public void uploadPromodoroSessions(ArrayList<Float> data){
        database.collection("promodoro").document(uid).collection(values.get("type"))
                .document(String.valueOf(Integer.parseInt(sessionsCount)+1)).set(data);
    }
//    public void addDataToFirebase(Object time,Object value){
//        info.put("Enjoyment",value);
//        info.put("time",time);
//        database.collection("userInformation").document("user"+String.valueOf(counter+1))
//                .collection("Enjoyment").add(info)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Log.i(TAG,"Updated Data in Firebase");
//                    }
//                });
//    }

//
    public void onLaunchClick(View view) {
        launchButton.setEnabled(false);
//        toString();
        Log.i(TAG, String.valueOf(launchButton.isEnabled()));
        // This begins the flow of login -> check calibration -> start session -> Qa -> display data
        checkNeuosLoginStatus();
    }

    // Activity launcher from login
    private final ActivityResultLauncher<Intent> appLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadingDialog.show();
                    finishSession();
                }
            });

    // Callback class for device connections from Neuos
    private class DeviceConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra(NeuosSDK.DEVICE_ADDRESS_EXTRA);
            Log.d(TAG, "Connection Intent : " + address);
            try {
                mService.connectSensorDevice(address);
            } catch (RemoteException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }
    // Neuos SDK Listener
    private final INeuosSdkListener mCallback = new INeuosSdkListener.Stub() {

        @Override
        public void onConnectionChanged(int previousConnection, int currentConnection) throws RemoteException {
            Log.d(TAG,"tsttsttstst");
            Log.i(TAG, "onConnectionChanged P: " + previousConnection + " C: " + currentConnection);
            if (currentConnection == NeuosSDK.ConnectionState.CONNECTED){
                if (mPostConnection != null){
                    mPostConnection.run();
                    mPostConnection = null;
                }
            }
        }
        // problem statement is that I have to post information to firebase every x number of seconds
        // solution check if time is divisible by 5
        ArrayList<Float> tempEnjoyment = new ArrayList<Float>(),
                tempFocus = new ArrayList<Float>();
        float enjoymentVal = 0,
                focusVal = 0;
        int minuteTimer = 0,
        secondTimer = 0;// this timer should
        @Override
        public void onValueChanged(String key, float value) throws RemoteException {


            counter++;


            if (counter % 25 == 0) {
                // in every 25 that's 5 seconds
                secondTimer++;
                if(secondTimer%12 == 0 ){
                    minuteTimer++;
                    // upload data
                    String TAG = "items to be uploaded";
                    Log.d(TAG,"Enjoyment items to be uploaded are "+minuteTimer +" : "+enjoyment);
                    Log.d(TAG,"Focus items to be uploaded are "+minuteTimer +" : "+focus);
                }
                enjoymentVal = findMax(tempEnjoyment);
                focusVal = findMax(tempFocus);

                enjoyment.add(enjoymentVal);
                focus.add(focusVal);

                // TODO empty tempEnjoyment and tempFocus
                tempEnjoyment.clear();
                tempFocus.clear();

                counter = 1;

            }
            else{
                switch(key) {
                    case NeuosSDK.PredictionValues.ENJOYMENT_STATE:
                        tempEnjoyment.add(value);
                        break;
                    case NeuosSDK.PredictionValues.FOCUS_STATE:
                        tempFocus.add(value);
                        break;
                }
            }
        }
        public float findMax(ArrayList<Float> arr){
            float max = -2;
            for (int i=0;i<arr.size();i++){
                if(arr.get(i)>max){
                    max = arr.get(i);
                }
            }
            return max;
        }
        public  boolean checkAnomaly(float average, float param){
            float diff= average-param;
            if (diff>0 && diff>30) {
                return false;
            }
            else if (diff<0 && diff<-30) {
                return false;
            }
            return true;
        }
        public  boolean checkZero(float param){
            return (param==0.0);
        }
        public  ArrayList<Float> cleanList(float data[]){
            Arrays.sort(data);
            ArrayList<Float> ret = new ArrayList<Float>();
            float count = 0;
            float sum = 0;
            int init = 0;
            while(data.length>0){
                if(checkZero(data[init])){
                    init++;
                }
                else{
                    break;
                }
            }
            float tempAverage = (data[init]+data[data.length-1])/2;
            float average = (tempAverage-data[init])<(tempAverage-data[data.length-1])?data[init]:data[data.length-1];
            for (int i=0;i<data.length;i++){
                float val = data[i];

                if (checkAnomaly(average,val)){
                    sum+=val;
                    count++;
                    average = sum/count;
                    ret.add((float) val);
                }
            }
            return ret;
        }

//        public float arrayAverage(float[] arr){
////            int pos = 0;
//            float total = 0;
//            int totalNum = 0;
//            for(int pos=0;pos<arr.length;pos++){
//                if(arr[pos] > 0 && arr[pos]<=100){
//                    total += arr[pos];
//                    totalNum ++;
//                }
//            }
//            return(total / totalNum);
//        }
//        public void readData(float value,String key, float[] arr, TextView type, String text){
//            Log.i(TAG, "onValueChanged K: " + key + " V: " + value);
//            if (value > 0){
//                arr[arrCounter] = value;
//            }
//        }

        @Override
        public void onQAStatus(boolean passed , int type){
            Log.i(TAG, "on QA Passed: " + passed + " T: " + type);
        }

        @Override
        public void onSessionComplete() throws RemoteException {
            Log.i(TAG, "onSessionComplete");
            // Once the session upload is complete, hide the dialog and allow re-launch
            runOnUiThread( () -> {
                loadingDialog.dismiss();
                //TODO when session is completed
            });
        }


        @Override
        public void onError(int errorCode, String message) throws RemoteException {
            Log.i(TAG, "onError Code: " + errorCode + " " + message);
        }
    };

    // Activity launcher from login
    private final ActivityResultLauncher<Intent> loginResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    checkCalibrationStatus();
                }
            });
    private final ActivityResultLauncher<Intent> resultsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {

                }
            });
    // Activity launcher for QA result
    private final ActivityResultLauncher<Intent> qaResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK) {
                    // Here you should work with your user to get his headband working
                    showError("QA Failed!");
                    // Or terminate the session
                    try {
                        mService.terminateSessionQaFailed();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    // TODO go nowhere?
                    // we are good to go, start the timer
                    startTimer(10,starter,mainCounter, true);
                }
            });
//    public void fetchData(View view){
//        mytext.setText(getIntent().getStringExtra("my value"));
//    }
    // Activity launcher for permissions request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your app
                    doBindService();
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Log.d("error","binding service error");
                }
            });

    // Helper function that tests if we have permissions
    // and starts the binding process or requests the permissions
    private void checkSDKPermissions(){
        if (ContextCompat.checkSelfPermission(
                this, NeuosSDK.NEUOS_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            doBindService();
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(
                    NeuosSDK.NEUOS_PERMISSION);
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(TAG, "Attached.");
            mService = INeuosSdk.Stub.asInterface(service);
            try {
                int response = mService.initializeNeuos(API_KEY);
                if ( response != NeuosSDK.ResponseCodes.SUCCESS){
                    Log.i(TAG, "initialize: failed with code " + response);
                    showError("initialize: failed with code " + response);
                }
                else{
                    response = mService.registerSDKCallback(mCallback);
                    if ( response != NeuosSDK.ResponseCodes.SUCCESS){
                        Log.i(TAG, "registerSDKCallback: failed with code " + response);
                        showError("registerSDKCallback failed with code " + response);
                    }
                    Log.i(TAG, "register callback: returned with code " + response);
                }
            } catch (RemoteException e) {
                Log.d(TAG,"service has an error");
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Detached.");
        }
    };

    // Check the user calibration status before starting a session
    private void checkCalibrationStatus(){
        try {
            int calibrationStatus = mService.checkUserCalibrationStatus();
            Log.i(TAG, "onUserCalibrationStatus: " + calibrationStatus);
            switch (calibrationStatus) {
                case NeuosSDK.UserCalibrationStatus.NEEDS_CALIBRATION:
                    // Here you can instead launch the calibration activity for the user.
                    // We don't do this in this context, but you can do this using the code in startCalibration()
                    showError("User is not calibrated. Cannot perform realtime stream");
                    break;
                case NeuosSDK.UserCalibrationStatus.CALIBRATION_DONE:
                    showError("Models for this user have yet to be processed");
                    break;
                case NeuosSDK.UserCalibrationStatus.MODELS_AVAILABLE:
                    connectToDevice();
                    mPostConnection = () -> {
                        // Start a session
                        if (startSession() == NeuosSDK.ResponseCodes.SUCCESS) {
                            // once started successfully, launch QA screen
                            launchQAScreen ();
                        }
                    };
                    break;
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }
    // Start a session
    private int startSession() {
        int response = -1;
        try {
            //we are predicting enjoyment
            response = mService.startPredictionSession(NeuosSDK.Predictions.ZONE);
            if ( response != NeuosSDK.ResponseCodes.SUCCESS){
                Log.i(TAG, "startSession: failed with code " + response);
                showError("startSession: failed with code " + response);
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        // Return the response code regardless of the co
        return response;
    }

    // Starts the process of connecting to a device
    private void connectToDevice(){
        Intent explicit = getExplicitIntent(new Intent(NeuosSDK.NEUOS_PAIR_DEVICE));
        if (explicit == null){
            showError("Cannot find Neuos pair device activity");
            return;
        }
        startActivity(explicit);
    }

    // Check the login status of a user
    private void checkNeuosLoginStatus() {
        try {
            Log.d(TAG,"testing");
            int status = mService.getUserLoginStatus();
            switch (status){
                case NeuosSDK.LoginStatus.LOGGED_IN:{
                    Log.i(TAG, "login: Logged In");
                    checkCalibrationStatus();
                    break;
                }
                case NeuosSDK.LoginStatus.NOT_LOGGED_IN:{
                    Log.i(TAG, "login: Not Logged In");
                    launchLogin();
                    break;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }
    // Launches the Neuos Login activity
    private void launchLogin(){
        Intent explicit = getExplicitIntent(new Intent(NeuosSDK.NEUOS_LOGIN));
        if (explicit == null){
            showError("Cannot find Neuos Login activity");
            return;
        }
        loginResultLauncher.launch(explicit);
    }
    // Launches the QA activity
    private void launchQAScreen() {
        Intent explicit = getExplicitIntent(new Intent(NeuosSDK.NEUOS_QA_SCREEN));
        if (explicit == null){
            showError("Cannot find QA activity");
            return;
        }
        explicit.putExtra(NeuosQAProperties.STAND_ALONE , true);
        explicit.putExtra(NeuosQAProperties.TASK_PROPERTIES ,
                new NeuosQAProperties(NeuosQAProperties.Quality.Normal , NeuosQAProperties.INFINITE_TIMEOUT));
        qaResultLauncher.launch(explicit);
    }
    // Binds to Neuos Service
    private boolean doBindService() {
        try {
            // Create an intent based on the class name
            Intent serviceIntent = new Intent(INeuosSdk.class.getName());
            // Use package manager to find intent receiver
            List<ResolveInfo> matches=getPackageManager()
                    .queryIntentServices(serviceIntent, 0);
            if (matches.size() == 0) {
                Log.d(TAG, "Cannot find a matching service!");
                showError("Cannot find a matching service!");
            }
            else if (matches.size() > 1) {
                // This is really just a sanity check
                // and should never occur in a real life scenario
                showError("Found multiple matching services!");
            }
            else {
                // Create an explicit intent
                Intent explicit=new Intent(serviceIntent);
                ServiceInfo svcInfo=matches.get(0).serviceInfo;
                ComponentName cn=new ComponentName(svcInfo.applicationInfo.packageName,
                        svcInfo.name);
                explicit.setComponent(cn);
                // Bind using AUTO_CREATE
                if (bindService(explicit, mConnection,  BIND_AUTO_CREATE)){
                    Log.d(TAG, "Bound to Neuos Service");
                } else {
                    Log.d(TAG, "Failed to bind to Neuos Service");
                    showError("Failed to bind to Neuos Service");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "can't bind to NeuosService, check permission in Manifest");
            showError("can't bind to NeuosService, check permission in Manifest");
        }
        return false;
    }
    // closes the session and uploads to cloud
    private void finishSession() {
        try {
            mService.finishSession();
        } catch (RemoteException e) {
            Log.e(TAG, "finishSession: ", e);
        }
    }

    private void showError(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // Use this to launch a calibration session in case your user isn't calibrated
    private void startCalibration() {
        Intent explicit = getExplicitIntent(new Intent(NeuosSDK.NEUOS_CALIBRATION));
        if (explicit != null) {
            startActivity(explicit);
        }
    }

    private Intent getExplicitIntent(Intent activityIntent){
        List<ResolveInfo> matches=getPackageManager()
                .queryIntentActivities(activityIntent , PackageManager.MATCH_ALL);
        // if we couldn't find one, return null
        if (matches.isEmpty()) {
            return null;
        }
        // there really should only be 1 match, so we get the first one.
        Intent explicit=new Intent(activityIntent);
        ActivityInfo info = matches.get(0).activityInfo;
        ComponentName cn = new ComponentName(info.applicationInfo.packageName,
                info.name);
        explicit.setComponent(cn);
        return explicit;
    }


}