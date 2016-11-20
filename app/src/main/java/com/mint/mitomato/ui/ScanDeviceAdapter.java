package com.mint.mitomato.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.mint.mitomato.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mint924 on 2016/6/2.
 */

public class ScanDeviceAdapter extends BaseAdapter {
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private Context mContext;

    ScanDeviceAdapter(Context context) {
        mContext = context;
    }

    public void add(BluetoothDevice device) {
        if (!isFound(device)) {
            mDevices.add(device);
            notifyDataSetChanged();
        }
    }

    private boolean isFound(BluetoothDevice device) {
        for (BluetoothDevice d : mDevices) {
            if (d.getAddress().equals(device.getAddress())) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return mDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mDevices.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
        }

        TextView title = (TextView)convertView.findViewById(R.id.title);
        TextView detail = (TextView)convertView.findViewById(R.id.detail);

        BluetoothDevice device = (BluetoothDevice) getItem(position);
        title.setText(device.getName());
        detail.setText(device.getAddress());

        return convertView;
    }
}
