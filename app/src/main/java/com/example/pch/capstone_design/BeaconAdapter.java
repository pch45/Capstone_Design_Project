package com.example.pch.capstone_design;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Vector;

/*
 * Created by 15U560 on 2017-10-19.
 */

public class BeaconAdapter extends BaseAdapter {


    private Vector<Beacon> beacons;
    private LayoutInflater layoutInflater;

    public BeaconAdapter(Vector<Beacon> beacons, LayoutInflater layoutInflater) {
        this.beacons = beacons;
        this.layoutInflater = layoutInflater;
    }

    @Override
    public int getCount() {
        return beacons.size();
    }

    @Override
    public Object getItem(int position) {
        return beacons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BeaconHolder beaconHolder;
        if (convertView == null) {
            beaconHolder = new BeaconHolder();
            convertView = layoutInflater.inflate(com.example.pch.capstone_design.R.layout.item_beacon, parent, false);
            beaconHolder.time = convertView.findViewById(com.example.pch.capstone_design.R.id.time);
            beaconHolder.rssi = convertView.findViewById(com.example.pch.capstone_design.R.id.rssi);
            beaconHolder.value = convertView.findViewById(com.example.pch.capstone_design.R.id.value);
            convertView.setTag(beaconHolder);
        } else {
            beaconHolder = (BeaconHolder)convertView.getTag();
        }

        beaconHolder.value.setText("VALUE :" + beacons.get(position).getNow()+"㎍/m³");
        beaconHolder.time.setText("TIME :"+beacons.get(position).getAddress());
        beaconHolder.rssi.setText("RSSI :"+beacons.get(position).getRssi() + "dBm");
        return convertView;
    }

    private class BeaconHolder {
        TextView time;
        TextView rssi;
        TextView value;

    }
}
