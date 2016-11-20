package com.mint.mitomato.ui;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mint.mitomato.R;
import com.mint.mitomato.utils.Settings;
import com.mint.mitomato.service.TomatoService;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFragment extends Fragment {
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private static final String TAG = SettingFragment.class.toString();

    private ScanDeviceAdapter mAdapter;

    private Button mSearchOrUnpairButton;
    private Button mTestVibrateButton;
    private TextView mDeviceInfoText;
    private TextView mPairedTitle;
    private TextView mWorkDurationValue;
    private TextView mBreakDurationValue;
    private TextView mLongBreakDurationValue;
    private TextView mLongBreakIntervalValue;
    private SeekBar mWorkDurationSeekBar;
    private SeekBar mBreakDurationSeekBar;
    private SeekBar mLongBreakDurationSeekBar;
    private SeekBar mLongBreakIntervalSeekBar;

    private TomatoService mService;
    private Settings mSettings;


    static SettingFragment newInstance() {
        return new SettingFragment();
    }

    public SettingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_setting, container, false);

        mSearchOrUnpairButton = (Button)layout.findViewById(R.id.search_or_unpair);
        mSearchOrUnpairButton.setOnClickListener(mOnClickListener);
        mTestVibrateButton = (Button) layout.findViewById(R.id.test_vibrate);
        mTestVibrateButton.setOnClickListener(mOnClickListener);

        mPairedTitle = (TextView)layout.findViewById(R.id.paired_title);
        mDeviceInfoText = (TextView)layout.findViewById(R.id.paired_info);

        mWorkDurationValue = (TextView) layout.findViewById(R.id.work_duration_value);
        mBreakDurationValue = (TextView) layout.findViewById(R.id.break_duration_value);
        mLongBreakDurationValue = (TextView) layout.findViewById(R.id.long_break_duration_value);
        mLongBreakIntervalValue = (TextView) layout.findViewById(R.id.long_break_interval_value);
        mWorkDurationSeekBar = (SeekBar) layout.findViewById(R.id.work_duration_seekbar);
        mWorkDurationSeekBar.setMax(Settings.WORK_DURATION_MAX - Settings.WORK_DURATION_MIN);
        mWorkDurationSeekBar.setProgress((mSettings.getWorkDuration() - Settings.WORK_DURATION_MIN) * mWorkDurationSeekBar.getMax() / Settings.WORK_DURATION_MAX);
        mWorkDurationSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        mBreakDurationSeekBar = (SeekBar) layout.findViewById(R.id.break_duration_seekbar);
        mBreakDurationSeekBar.setMax(Settings.BREAK_DURATION_MAX - Settings.BREAK_DURATION_MIN);
        mBreakDurationSeekBar.setProgress((mSettings.getBreakDuration() - Settings.BREAK_DURATION_MIN) * mBreakDurationSeekBar.getMax() / Settings.BREAK_DURATION_MAX);
        mBreakDurationSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        mLongBreakDurationSeekBar = (SeekBar) layout.findViewById(R.id.long_break_duration_seekbar);
        mLongBreakDurationSeekBar.setMax(Settings.LONG_BREAK_DURATION_MAX - Settings.LONG_BREAK_DURATION_MIN);
        mLongBreakDurationSeekBar.setProgress((mSettings.getLongBreakDuration() - Settings.LONG_BREAK_DURATION_MIN) * mLongBreakDurationSeekBar.getMax() / Settings.LONG_BREAK_DURATION_MAX);
        mLongBreakDurationSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        mLongBreakIntervalSeekBar = (SeekBar) layout.findViewById(R.id.long_break_interval_seekbar);
        mLongBreakIntervalSeekBar.setMax(Settings.LONG_BREAK_INTERVAL_MAX - Settings.LONG_BREAK_INTERVAL_MIN);
        mLongBreakIntervalSeekBar.setProgress((mSettings.getLongBreakInterval() - Settings.LONG_BREAK_INTERVAL_MIN) * mLongBreakIntervalSeekBar.getMax() / Settings.LONG_BREAK_INTERVAL_MAX);
        mLongBreakIntervalSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        return layout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mSettings = new Settings(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mSettings = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        getActivity().bindService(new Intent(getActivity(), TomatoService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        getActivity().unbindService(mConnection);

        super.onStop();
    }

    private void startScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermission();
                return;
            }
        }

        BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        scanner.startScan(mScanCallBack);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(new ProgressBar(getActivity()));

        mAdapter = new ScanDeviceAdapter(getActivity());
        builder.setAdapter(mAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothDevice device = (BluetoothDevice) mAdapter.getItem(which);

                mService.setDevice(device);
                mPairedTitle.setVisibility(View.VISIBLE);
                mDeviceInfoText.setVisibility(View.VISIBLE);
                mTestVibrateButton.setVisibility(View.VISIBLE);
                mDeviceInfoText.setText(mService.getMacAddr());
                mSearchOrUnpairButton.setText(getString(R.string.unpair));

                stopScan();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                stopScan();
            }
        }).show();

    }

    private void stopScan() {
        BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        scanner.stopScan(mScanCallBack);
    }

    private ScanCallback mScanCallBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final BluetoothDevice device = result.getDevice();
            Log.d(TAG, "onScanResult: " + device.getName() + " " + device.getAddress() );

            if (device.getName() != null && device.getName().toLowerCase().contains("mi")) {
                mAdapter.add(device);
            }
            super.onScanResult(callbackType, result);
        }
    };

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: permission denied");
            } else {
                startScan();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.search_or_unpair:
                    if (mService.isPaired()) {
                        mPairedTitle.setVisibility(View.INVISIBLE);
                        mDeviceInfoText.setVisibility(View.INVISIBLE);
                        mTestVibrateButton.setVisibility(View.INVISIBLE);
                        mService.unpair();
                        mSearchOrUnpairButton.setText(getString(R.string.search));
                    } else {
                        startScan();
                    }
                    break;
                case R.id.test_vibrate:
                    mService.vibrate();
                    break;
            }
        }
    };

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TomatoService.LocalBinder binder = (TomatoService.LocalBinder) service;
            mService = binder.getSevice();

            if (mService.isPaired()) {
                mSearchOrUnpairButton.setText(getText(R.string.unpair));
                mPairedTitle.setVisibility(View.VISIBLE);
                mDeviceInfoText.setVisibility(View.VISIBLE);
                mTestVibrateButton.setVisibility(View.VISIBLE);
                mDeviceInfoText.setText(mService.getMacAddr());
            }

            setWorkDuration(mService.getWorkDuration());
            setBreakDuration(mService.getBreakDuration());
            setLongBreakDuration(mService.getLongBreakDuration());
            setLongBreakInterval(mService.getLongBreakInterval());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int value = 0;
            switch (seekBar.getId()) {
                case R.id.work_duration_seekbar:
                    value = progress * (Settings.WORK_DURATION_MAX - Settings.WORK_DURATION_MIN) / seekBar.getMax() + Settings.WORK_DURATION_MIN;
                    setWorkDuration(value);
                    break;
                case R.id.break_duration_seekbar:
                    value = progress * (Settings.BREAK_DURATION_MAX - Settings.BREAK_DURATION_MIN) / seekBar.getMax() + Settings.BREAK_DURATION_MIN;
                    setBreakDuration(value);
                    break;
                case R.id.long_break_duration_seekbar:
                    value = progress * (Settings.LONG_BREAK_DURATION_MAX - Settings.LONG_BREAK_DURATION_MIN) / seekBar.getMax() + Settings.LONG_BREAK_DURATION_MIN;
                    setLongBreakDuration(value);
                    break;
                case R.id.long_break_interval_seekbar:
                    value = progress * (Settings.LONG_BREAK_INTERVAL_MAX - Settings.LONG_BREAK_INTERVAL_MIN) / seekBar.getMax() + Settings.LONG_BREAK_INTERVAL_MIN;
                    setLongBreakInterval(value);
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void setWorkDuration(int value) {
        String valueText = String.valueOf(value) + " minutes";
        mWorkDurationValue.setText(valueText);
        mSettings.setWorkDuration(value);
    }

    private void setBreakDuration(int value) {
        String valueText = String.valueOf(value) + " minutes";
        mBreakDurationValue.setText(valueText);
        mSettings.setBreakDuration(value);
    }

    private void setLongBreakDuration(int value) {
        String valueText = String.valueOf(value) + " minutes";
        mLongBreakDurationValue.setText(valueText);
        mSettings.setLongBreakDuration(value);
    }

    private void setLongBreakInterval(int value) {
        String valueText;

        if (value == 0) {
            valueText = "Disabled";
        } else {
            valueText = "Every " + String.valueOf(value) + " breaks";
        }

        mLongBreakIntervalValue.setText(valueText);
        mSettings.setLongBreakInterval(value);
    }
}
