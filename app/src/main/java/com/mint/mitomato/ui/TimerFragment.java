package com.mint.mitomato.ui;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.mint.mitomato.R;
import com.mint.mitomato.databinding.FragmentTimerBinding;
import com.mint.mitomato.service.TomatoService;
import com.mint.mitomato.viewmodel.TimerViewModel;

import java.util.Locale;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;


/**
 * A simple {@link Fragment} subclass.
 */
public class TimerFragment extends Fragment {
    public static final String TAG = TimerFragment.class.toString();
    private TimerViewModel mTimerViewModel;

    static TimerFragment newInstance() {
        return new TimerFragment();
    }

    public TimerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentTimerBinding mBinding = FragmentTimerBinding.inflate(inflater);
        mTimerViewModel = new TimerViewModel(getActivity());
        mBinding.setVm(mTimerViewModel);

        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        mTimerViewModel.destroy();

        super.onDestroyView();
    }
}
