package com.example.pch.capstone_design;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private static String IP_ADDRESS = "49.171.113.56";

    private static String TAG = "test";



    BluetoothAdapter mBluetoothAdapter;

    BluetoothLeScanner mBluetoothLeScanner;

    BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private static final int PERMISSIONS = 100;

    Vector<Beacon> beacon;

    BeaconAdapter beaconAdapter;

    ListView beaconListView;

    ScanSettings.Builder mScanSettings;

    List<ScanFilter> scanFilters;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy:MM:dd", Locale.KOREAN);

    List<Time> time_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.pch.capstone_design.R.layout.activity_main);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS);
        beaconListView = (ListView) findViewById(com.example.pch.capstone_design.R.id.beaconListView);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        beacon = new Vector<>();
        mScanSettings = new ScanSettings.Builder();
        mScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        ScanSettings scanSettings = mScanSettings.build();

        time_list = new ArrayList<Time>();

        scanFilters = new Vector<>();
        ScanFilter.Builder scanFilter = new ScanFilter.Builder();
        scanFilter.setDeviceAddress("B8:27:EB:51:4C:6A"); //ex) 00:00:00:00:00:00
        ScanFilter scan = scanFilter.build();
        scanFilters.add(scan);
        mBluetoothLeScanner.startScan(scanFilters, scanSettings, mScanCallback);
        editData task = new editData();

    }

    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final ScanRecord temp = result.getScanRecord();
            try {
                ScanRecord scanRecord = result.getScanRecord();
                final ScanResult scanResult = result;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {                     // 데이터 가져오는 것만 수정하면 됨
                                String id = temp.getBytes()[2]+"";
                                String value = temp.getBytes()[8]+"";
                                String HH="", MM="", SS="";      //값을 넣겠지?
                                String finish = "";
                                if(finish.equals("FF")){
                                    //time_list에 있는걸 평균내서 서버에 올리는 함수를 작성
                                    AVG_To_Server();
                                }

                                time_list.add(new Time(HH,MM,SS,value));

                                beacon.add(0, new Beacon("1", scanResult.getRssi(), value));

                                beaconAdapter = new BeaconAdapter(beacon, getLayoutInflater());
                                beaconListView.setAdapter(beaconAdapter);
                                beaconAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        public void AVG_To_Server(){
            List<Time> ss = new ArrayList<Time>();
            editData task = new editData();
            for(int i = 0; i < time_list.size(); i++){
                Time tmp = time_list.get(i);
                ss.add(tmp);
                if(tmp.SS.equals("59")){
                    int avg = 0;
                    for(int j = 0 ; j < ss.size(); j++){
                        avg += Integer.parseInt(ss.get(j).value);
                    }
                    avg /= ss.size();
                    task.execute("http://" + IP_ADDRESS + "/edit.php", "Information",avg+"",tmp.HH+":"+tmp.MM+":00");
                    ss = new ArrayList<Time>();
                }
            }
            time_list = new ArrayList<Time>();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d("onBatchScanResults", results.size() + "");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("onScanFailed()", errorCode + "");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeScanner.stopScan(mScanCallback);
    }

    class editData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... arg0) {

            try {
                String id =  arg0[1];
                String value = arg0[2];
                String address = arg0[0];
                String time = arg0[3];

                String link = address+"?id=" + id+"&value="+value+"&date=" + simpleDateFormat.format(new Date()) +"&time="+time;
                URL url = new URL(link);
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet();
                request.setURI(new URI(link));
                HttpResponse response = client.execute(request);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                StringBuffer sb = new StringBuffer("");
                String line = "";

                while ((line = in.readLine()) != null) {
                    sb.append(line);
                    break;
                }
                in.close();
                return sb.toString();
            } catch (Exception e) {
                return new String("Exception: " + e.getMessage());
            }
        }
    }
    class Time{
        private String HH;
        private String MM;
        private String SS;
        private String value;
        Time(String HH, String MM, String SS, String value){
            this.HH = HH;
            this.MM = MM;
            this.SS = SS;
            this.value = value;
        }
    }
}
