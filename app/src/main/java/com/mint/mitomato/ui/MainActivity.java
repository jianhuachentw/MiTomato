package com.mint.mitomato.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.mint.mitomato.utils.Constants;
import com.mint.mitomato.R;
import com.mint.mitomato.utils.Settings;
import com.mint.mitomato.rx.TomatoServiceSubscribe;

import rx.Observable;
import rx.Subscription;

public class MainActivity extends AppCompatActivity {

    private static final int PAGE_SETTING = 0;
    private static final int PAGE_TIMER = 1;
    private static final int PAGE_HISTORY = 2;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private SettingFragment mSettingFragment;
    private TimerFragment mTimerFragment;
    // retained fragment to hold the service connection through configuration changes
    private TomatoServiceFragment mTomatoServiceFragment;
    private Settings mSettings;
    private AdView mAdView;
    private Subscription mServiceSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, "ca-app-pub-2011075858762043/4214353017");

        // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
        // values/strings.xml.
        mAdView = (AdView) findViewById(R.id.ad_view);
        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("524F64ABB1FF29761B42BEA2382E235E")
                .build();

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.view_pager);

        mViewPager.setAdapter(mSectionsPagerAdapter);

        mSettings = new Settings(this);
        if (mSettings.isPaired()) {
            mViewPager.setCurrentItem(PAGE_TIMER);
        } else {
            mViewPager.setCurrentItem(PAGE_SETTING);
        }

        // find the retained fragment on activity restarts
        FragmentManager fm = getSupportFragmentManager();
        mTomatoServiceFragment = (TomatoServiceFragment) fm.findFragmentByTag("service");
        // create the fragment the first time
        if (mTomatoServiceFragment == null) {
            // add the fragment
            mTomatoServiceFragment = new TomatoServiceFragment();
            fm.beginTransaction().add(mTomatoServiceFragment, "service").commit();

            mServiceSubscription = Observable.create(new TomatoServiceSubscribe(MainActivity.this))
                    .subscribe(tomatoService -> {
                        mTomatoServiceFragment.mService = tomatoService;
                    });
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getBooleanExtra(Constants.EXTRA_FROM_NOTIFICATION, false)) {
            mViewPager.setCurrentItem(PAGE_TIMER);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }



    @Override
    public void onDestroy() {
        if (!isChangingConfigurations()) {
            if (mServiceSubscription != null) {
                mServiceSubscription.unsubscribe();
            }
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText("History\n\nComing soon...");
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    mSettingFragment = SettingFragment.newInstance();
                    return mSettingFragment;
                case 1:
                    mTimerFragment = TimerFragment.newInstance();
                    return mTimerFragment;
                default:
                    return PlaceholderFragment.newInstance(position + 1);
            }


        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Settings";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }
}
