package com.example.pch.capstone_design;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

public class Data_Check extends AppCompatActivity{

    private static String IP_ADDRESS = "49.171.113.56";

    private static String TAG = "test";

    private Button button;

    private TextView[] textViews = new TextView[5];

    private int[] layout = new int[]{com.example.pch.capstone_design.R.id.textView1, com.example.pch.capstone_design.R.id.textView2, com.example.pch.capstone_design.R.id.textView3, com.example.pch.capstone_design.R.id.textView4, com.example.pch.capstone_design.R.id.textView5};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.pch.capstone_design.R.layout.data);
        Intent intent = new Intent(Data_Check.this, MainActivity.class);
        startActivity(intent);

        button = (Button) findViewById(com.example.pch.capstone_design.R.id.button);

        for (int i = 0; i < 5; i++) {
            textViews[i] = (TextView) findViewById(layout[i]);
        }
        rec();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rec();
            }
        });
    }

    private void rec(){
        for (int i = 0; i < 5; i++) {
            findData task = new findData();
            task.execute("http://" + IP_ADDRESS + "/find.php", "gongdae" + (i + 1));
        }
    }

    class findData extends AsyncTask<String, Void ,String> {
        int idx;
        @Override
        protected String doInBackground(String... arg0) {

            try {
                idx = Integer.parseInt(arg0[1].substring(7));
                String id =  arg0[1];
                String address = arg0[0];

                String link = address+"?id=" + id;
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

        @Override
        protected void onPostExecute(String result){
            textViews[idx-1].setText("공대"+(idx)+"호관 : "+result+"㎍/m³");
        }
    }

}
