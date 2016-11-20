package com.mint.mitomato.viewmodel;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.os.IBinder;
import android.util.TimeUtils;
import android.view.View;

import com.mint.mitomato.R;
import com.mint.mitomato.service.TomatoService;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

/**
 * Created by mint924 on 2016/11/15.
 */

public class TimerViewModel {
    private final Context mContext;
    private final Subscription mSubscription;

    private BehaviorSubject<TomatoService> mServiceSubject;

    public ObservableField<String> state = new ObservableField<>();
    public ObservableField<String> remainTime = new ObservableField<>();
    public ObservableInt stopButtonVisibility = new ObservableInt(View.INVISIBLE);
    public ObservableInt startButtonVisibility = new ObservableInt(View.INVISIBLE);
    public ObservableInt skipButtonVisibility = new ObservableInt(View.INVISIBLE);
    private TomatoService mTomatoService;

    public TimerViewModel(Context context) {
        mContext = context;

        mServiceSubject = BehaviorSubject.create();
        mContext.bindService(new Intent(mContext, TomatoService.class), mConnection, Context.BIND_AUTO_CREATE);

        Observable<String> stateObservable = mServiceSubject.flatMap(TomatoService::getStateObservable)
                .map(TomatoService::getStateText);
        Observable<String> remainTimeObservable = mServiceSubject.flatMap(TomatoService::getRemainTimeObservable)
                .map(time -> String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.SECONDS.toMinutes(time), time % 60));

        mSubscription = Observable.combineLatest(
                stateObservable,
                remainTimeObservable,
                (state, remainTime) -> new TimerViewData(state, remainTime, !state.equals(TomatoService.getStateText(TomatoService.STATE_IDLE))))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                timerViewState -> {
                    state.set(timerViewState.getState());
                    remainTime.set(timerViewState.getRemainTime());
                    stopButtonVisibility.set(timerViewState.isStarted() ? View.VISIBLE : View.INVISIBLE);
                    startButtonVisibility.set(timerViewState.isStarted() ? View.INVISIBLE : View.VISIBLE);
                    skipButtonVisibility.set(timerViewState.isStarted() ? View.VISIBLE : View.INVISIBLE);
                },
                Throwable::printStackTrace);
    }

    public void destroy() {
        if (!mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        mContext.unbindService(mConnection);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                mContext.startService(new Intent(mContext, TomatoService.class));
                mTomatoService.start();
                break;
            case R.id.stop:
                mTomatoService.stop();
                mContext.stopService(new Intent(mContext, TomatoService.class));
                break;
            case R.id.skip:
                mTomatoService.skipCurrentDuration();
                break;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TomatoService.LocalBinder binder = (TomatoService.LocalBinder) service;
            mTomatoService = binder.getSevice();

            mServiceSubject.onNext(mTomatoService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
}
