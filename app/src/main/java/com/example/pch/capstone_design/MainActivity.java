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
import android.widget.Toast;

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

    String prev = "";

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

    }

    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final ScanRecord temp = result.getScanRecord();
            try {
                ScanRecord scanRecord = result.getScanRecord();
                final ScanResult scanResult = result;
                Toast.makeText(MainActivity.this, "ScanResult", Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {                     // 데이터 가져오는 것만 수정하면 됨
                                Log.d("DATA",temp.getBytes()+"");
                                String value = temp.getBytes()[8]+"";
                                String HH=temp.getBytes()[2]+"", MM=temp.getBytes()[3]+"", SS=temp.getBytes()[4]+"";      //값을 넣겠지?
                                Toast.makeText(MainActivity.this, "???", Toast.LENGTH_SHORT).show();
                                if(HH.equals("-1")){
                                    //time_list에 있는걸 평균내서 서버에 올리는 함수를 작성
                                    AVG_To_Server();
                                }
                                else {
                                    if (time_list.isEmpty()) {
                                        prev = MM;
                                    } else if (!prev.equals(MM)) {
                                        AVG_To_Server();
                                        prev = MM;
                                    }

                                    time_list.add(new Time(HH, MM, SS, value));

                                    beacon.add(0, new Beacon(Integer.parseInt(HH) + ":" + Integer.parseInt(MM)
                                            + ":" + Integer.parseInt(SS), scanResult.getRssi(), value));
                                    Toast.makeText(MainActivity.this, HH + ":" + time_list.size(), Toast.LENGTH_LONG).show();

                                    beaconAdapter = new BeaconAdapter(beacon, getLayoutInflater());
                                    beaconListView.setAdapter(beaconAdapter);
                                    beaconAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        public void AVG_To_Server(){
            editData task = new editData();
            int size = time_list.size();
            if(size == 0) return;
            Time tmp = time_list.remove(0);
            int avg = Integer.parseInt(tmp.value);
            for(int i = 1; i < size; i++){
                tmp = time_list.remove(0);
                avg += Integer.parseInt(tmp.value);
            }
            avg /= size;
            Toast.makeText(MainActivity.this, avg+"", Toast.LENGTH_SHORT).show();
            task.execute("http://" + IP_ADDRESS + "/edit.php", "Information",avg+"", Integer.parseInt(tmp.HH)+":"+Integer.parseInt(tmp.MM)+":00");
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
                Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                return sb.toString();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Fail", Toast.LENGTH_SHORT).show();
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
