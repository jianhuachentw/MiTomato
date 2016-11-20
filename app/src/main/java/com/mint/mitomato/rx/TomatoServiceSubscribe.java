package com.mint.mitomato.rx;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.mint.mitomato.service.TomatoService;

import rx.Observable;
import rx.Subscriber;
import rx.android.MainThreadSubscription;

/**
 * Created by mint924 on 2016/10/23.
 */

public class TomatoServiceSubscribe implements Observable.OnSubscribe<TomatoService> {
    private final Context mContext;
    private ServiceConnection mConnection;

    public TomatoServiceSubscribe(Context context) {
        mContext = context;
    }

    @Override
    public void call(final Subscriber<? super TomatoService> subscriber) {
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                TomatoService.LocalBinder binder = (TomatoService.LocalBinder) service;
                subscriber.onNext(binder.getSevice());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        mContext.getApplicationContext().bindService(new Intent(mContext, TomatoService.class), mConnection, Context.BIND_AUTO_CREATE);

        subscriber.add(new MainThreadSubscription() {
            @Override
            protected void onUnsubscribe() {
                mContext.getApplicationContext().unbindService(mConnection);
            }
        });
    }
}