package com.mint.mitomato.service;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.mint.mitomato.utils.Constants;
import com.mint.mitomato.utils.MiController;
import com.mint.mitomato.R;
import com.mint.mitomato.utils.Settings;
import com.mint.mitomato.ui.MainActivity;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

public class TomatoService extends Service implements
        ITomatoService,
        SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = TomatoService.class.toString();
    public static final int STATE_IDLE = 0;
    public static final int STATE_WORKING = 1;
    public static final int STATE_SHORT_BREAK = 2;
    public static final int STATE_LONG_BREAK = 3;

    private static final int NOTIFICATION_ID = 1;
    public static final int SCAN_TIMEOUT = (int) TimeUnit.SECONDS.toSeconds(30);
    public static final String ACTION_SKIP = "com.mint.mitomato.ACTION_SKIP";
    public static final String ACTION_STOP = "com.mint.mitomato.ACTION_STOP";

    private Settings mSettings;
    MiController mMiController;
    private Binder mBinder = new LocalBinder();
    private Calendar mStartTime;

    private Calendar mEndTime;
    private int mWorkDuration;
    private int mBreakDuration;
    private int mLongBreakDuration;
    private int mLongBreakInterval;

    private int mShortBreakCount = 0;
    private Handler mHandler;
    private int mState;
    private long mRemainTime = 0;

    private Runnable mStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().stopScan(mScanCallback);
        }
    };

    private String mMacAddr;
    private Observable<Long> mTimerObservable;
    private Subscription mTimerSubscription;
    private BehaviorSubject<Integer> mStateSubject;
    private BehaviorSubject<Long> mRemainTimeSubject;
    private Subscription mNotificationSubscription;

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
        mSettings = new Settings(this);
        mSettings.registerOnSharedPreferenceChangeListener(this);

        mMacAddr = mSettings.getMaccAddr();
        mWorkDuration = mSettings.getWorkDuration();
        mBreakDuration = mSettings.getBreakDuration();
        mLongBreakDuration = mSettings.getLongBreakDuration();
        mLongBreakInterval = mSettings.getLongBreakInterval();

        if (mMacAddr != null) {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mMacAddr);
            mMiController = new MiController(this, device, mHandler);
        }

        mTimerObservable = Observable.interval(1, TimeUnit.SECONDS).share();
        mStateSubject = BehaviorSubject.create(STATE_IDLE);
        mRemainTimeSubject = BehaviorSubject.create(TimeUnit.MINUTES.toSeconds(mWorkDuration));

        mNotificationSubscription = Observable.combineLatest(
                mStateSubject,
                mRemainTimeSubject.sample(Observable.interval(1, 30, TimeUnit.SECONDS)),
                (state, remainTime) -> {
                    if (state != STATE_IDLE) {
                        sendNotification(state, remainTime);
                    }
                    return state;
                })
                .filter(state -> state == STATE_IDLE)
                .subscribe(
                        state -> cancelNotification(),
                        throwable -> throwable.printStackTrace()
                );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mNotificationSubscription.unsubscribe();
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_SKIP:
                    skipCurrentDuration();
                    break;
                case ACTION_STOP:
                    stop();
                    break;
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setDevice(BluetoothDevice device) {
        mSettings.setMacAddr(device.getAddress());
        mMacAddr = device.getAddress();

        if (mMiController != null) {
            mMiController.disconnect();
        }

        mMiController = new MiController(this, device, mHandler);
    }

    public boolean isPaired() {
        return mSettings.isPaired();
    }

    public void unpair() {
        mSettings.unpair();
        if (mMiController != null) {
            mMiController.disconnect();
        }
    }

    public String getMacAddr() {
        return mMacAddr;
    }

    private void scanForPairedMiBand() {
        if (mMacAddr != null) {
            BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (scanner != null) {
                Toast.makeText(this, "Pairing with MiBand", Toast.LENGTH_LONG).show();
                mHandler.postDelayed(mStopScanRunnable, SCAN_TIMEOUT);
                scanner.startScan(mScanCallback);
            } else {
                Log.d(TAG, "scanForPairedMiBand: failed to get LeScanner");
            }

        }
    }

    public void start() {
        vibrate();

        mState = STATE_WORKING;
        mStateSubject.onNext(mState);

        mStartTime = Calendar.getInstance();
        mEndTime = Calendar.getInstance();
        mEndTime.add(Calendar.MINUTE, mWorkDuration);
        mRemainTime = (mEndTime.getTimeInMillis() - mStartTime.getTimeInMillis()) / 1000;
        mRemainTimeSubject.onNext(mRemainTime);

        mTimerSubscription = getTimerObservable()
                .subscribe(
                    time -> {
                        Calendar current = Calendar.getInstance();

                        if (current.after(mEndTime)) {
                            changeToNextState();
                        }

                        mRemainTime = (mEndTime.getTimeInMillis() - current.getTimeInMillis()) / 1000;
                        mRemainTimeSubject.onNext(mRemainTime);
                    },
                    e -> e.printStackTrace()
                );
    }

    public void stop() {
        mState = STATE_IDLE;
        mStateSubject.onNext(mState);
        mRemainTime = TimeUnit.MINUTES.toSeconds(mWorkDuration);
        mRemainTimeSubject.onNext(mRemainTime);

        if (mTimerSubscription != null) {
            mTimerSubscription.unsubscribe();
        }

        stopSelf();
    }

    public void skipCurrentDuration() {
        changeToNextState();
    }

    private Observable<Long> getTimerObservable() {
        return mTimerObservable
                .startWith(0L)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void sendNotification(int state, long remainTime) {
        Log.d(TAG, "sendNotification: state " + state + " time " + remainTime);
        Intent intent = new Intent(this, TomatoService.class);
        intent.setAction(ACTION_SKIP);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action skipAction = new NotificationCompat.Action(R.drawable.nexttrack24x24, "Skip", pendingIntent);

        intent = new Intent(this, TomatoService.class);
        intent.setAction(ACTION_STOP);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        pendingIntent = PendingIntent.getService(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action stopAction = new NotificationCompat.Action(R.drawable.video24x24, "Stop", pendingIntent);

        intent = new Intent(this, MainActivity.class);
        intent.putExtra(Constants.EXTRA_FROM_NOTIFICATION, true);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

         pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon))
                        .setSmallIcon(R.drawable.small_icon)
                        .setContentTitle(getStateText(state))
                        .setContentText("Remain ~" + remainTime / 60 + "mins")
                        .setContentIntent(pendingIntent).addAction(skipAction).addAction(stopAction);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification() {
        Log.d(TAG, "cancelNotification");
        stopForeground(true);
    }

    public void vibrate() {
        if (mMiController != null) {
            mMiController.connect();
        } else {
            scanForPairedMiBand();
        }
    }

    static public String getStateText(int state) {
        switch (state) {
            case STATE_WORKING:
                return "Working";
            case STATE_SHORT_BREAK:
                return "Break";
            case STATE_LONG_BREAK:
                return "Long Break";
            case STATE_IDLE:
                return "Idle";
            default:
                return "Unknown State";
        }
    }

    private void changeToNextState() {
        int oldState = mState;
        int newState;
        int newDuration = 0;
        if (oldState == STATE_WORKING) {
            if (mShortBreakCount < mLongBreakInterval) {
                newState = STATE_SHORT_BREAK;
                mShortBreakCount++;
                newDuration = mBreakDuration;
            } else {
                newState = STATE_LONG_BREAK;
                mShortBreakCount = 0;
                newDuration = mLongBreakDuration;
            }
        } else {
            newState = STATE_WORKING;
            newDuration = mWorkDuration;
        }

        mState = newState;
        mStateSubject.onNext(mState);
        Calendar current = Calendar.getInstance();
        mStartTime.setTime(current.getTime());
        mEndTime.setTime(current.getTime());
        mEndTime.add(Calendar.MINUTE, newDuration);
        mRemainTime = (mEndTime.getTimeInMillis() - mStartTime.getTimeInMillis()) / 1000;
        mRemainTimeSubject.onNext(mRemainTime);

        vibrate();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (mMiController == null && result.getDevice().getAddress().equals(mMacAddr)) {
                Toast.makeText(TomatoService.this, "Paired with MiBand " + mMacAddr, Toast.LENGTH_LONG).show();
                mMiController = new MiController(TomatoService.this, result.getDevice(), mHandler);
                mHandler.removeCallbacks(mStopScanRunnable);
                mStopScanRunnable.run();
                vibrate();
            }
            super.onScanResult(callbackType, result);
        }
    };

    public int getWorkDuration() {
        return mWorkDuration;
    }

    public int getBreakDuration() {
        return mBreakDuration;
    }

    public int getLongBreakDuration() {
        return mLongBreakDuration;
    }

    public int getLongBreakInterval() {
        return mLongBreakInterval;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Settings.KEY_WORK_DURATION:
                mWorkDuration = mSettings.getWorkDuration();
                break;
            case Settings.KEY_BREAK_DURATION:
                mBreakDuration = mSettings.getBreakDuration();
                break;
            case Settings.KEY_LONG_BREAK_DURATION:
                mLongBreakDuration = mSettings.getLongBreakDuration();
                break;
            case Settings.KEY_LONG_BREAK_INTERVAL:
                mLongBreakInterval = mSettings.getLongBreakInterval();
                break;
        }
    }

    @Override
    public Observable<Integer> getStateObservable() {
        return mStateSubject.asObservable();
    }

    @Override
    public Observable<Long> getRemainTimeObservable() {
        return mRemainTimeSubject.asObservable();
    }

    public class LocalBinder extends Binder {
        public TomatoService getSevice() {
            return TomatoService.this;
        }
    }
}
