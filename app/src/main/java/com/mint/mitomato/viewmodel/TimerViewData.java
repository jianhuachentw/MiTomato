package com.mint.mitomato.viewmodel;

import android.databinding.ObservableArrayMap;

/**
 * Created by mint924 on 2016/11/19.
 */
public class TimerViewData {
    private String mState;
    private String mRemainTime;
    private boolean mStarted;

    TimerViewData(String state, String remainTime, boolean started) {
        mState = state;
        mRemainTime = remainTime;
        mStarted = started;
    }

    public String getRemainTime() {
        return mRemainTime;
    }

    public String getState() {
        return mState;
    }

    public boolean isStarted() {
        return mStarted;
    }
}
