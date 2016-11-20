package com.mint.mitomato.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;

import java.util.Arrays;
import java.util.UUID;

import rx.Observable;
import rx.Observer;
import rx.Subscription;

/**
 * Created by mint924 on 2016/5/29.
 */

public class MiController {
    private static final String TAG = MiController.class.toString();
    public static int SHORT_VIBRATE = 0;
    public static int LONG_VIBRATE = 0;

    private static final UUID ALERT_SERVICE_UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHAR_UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

    private final Handler mHandler;
    private Context mContext;
    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private final RxBleClient mRxBleClient;
    private Subscription mSubscription;
    private Observable<RxBleConnection> mConnectionObservable;

    public MiController(Context context, BluetoothDevice device, Handler handler) {
        mContext = context;
        mDevice = device;
        mHandler = handler;

        mRxBleClient = RxBleClient.create(context);

        RxBleDevice rxBleDevice = mRxBleClient.getBleDevice(mDevice.getAddress());
        mConnectionObservable = rxBleDevice.establishConnection(mContext, false)
                .compose(new ConnectionSharingAdapter());
    }

    public void connect() {
        disconnect();

        mSubscription = mConnectionObservable
                .flatMap(rxBleConnection -> rxBleConnection.discoverServices()
                        .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getService(ALERT_SERVICE_UUID))
                        .flatMap(bluetoothGattService -> {
                            if (bluetoothGattService.getCharacteristic(ALERT_LEVEL_CHAR_UUID) != null) {
                                return rxBleConnection.writeCharacteristic(ALERT_LEVEL_CHAR_UUID, new byte[] {0x01});
                            } else {
                                return Observable.error(new BleCharacteristicNotFoundException(ALERT_LEVEL_CHAR_UUID));
                            }
                        }))
                .subscribe(new Observer<byte[]>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(byte[] bytes) {
                        Log.d(TAG, "Alert level " + Arrays.toString(bytes) + " is written");
                    }
                });
    }

    public void disconnect() {
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
    }
}
