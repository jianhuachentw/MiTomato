package com.mint.mitomato.ui;


import android.content.Context;
import android.content.ServiceConnection;
import android.support.v4.app.Fragment;

import com.mint.mitomato.service.TomatoService;

/**
 * A simple {@link Fragment} subclass.
 */
public class TomatoServiceFragment extends Fragment {

    // data object we want to retain
    public ServiceConnection mConnection;
    public TomatoService mService;

    public TomatoServiceFragment() {
        // Required empty public constructor
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }
}
