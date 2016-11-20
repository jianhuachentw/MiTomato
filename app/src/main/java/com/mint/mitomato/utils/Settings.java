package com.mint.mitomato.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by mint924 on 2016/7/10.
 */
public class Settings implements SharedPreferences {
    static final String SHARED_PREFERENCE_NAME = "Settings";
    static final String KEY_MAC_ADDRESS = "mac_address";
    public static final String KEY_WORK_DURATION = "work_duration";
    public static final String KEY_BREAK_DURATION = "break_duration";
    public static final String KEY_LONG_BREAK_DURATION = "long_break_duration";
    public static final String KEY_LONG_BREAK_INTERVAL = "long_break_interval";

    static final int DEFAULT_WORK_DURATION = 25;
    static final int DEFAULT_BREAK_DURATION = 5;
    static final int DEFAULT_LONG_BREAK_DURATION = 15;
    static final int DEFAULT_LONG_BREAK_INTEVAL = 2;

    public static final int WORK_DURATION_MIN = 5;
    public static final int WORK_DURATION_MAX = 60;
    public static final int BREAK_DURATION_MIN = 1;
    public static final int BREAK_DURATION_MAX = 60;
    public static final int LONG_BREAK_DURATION_MIN = 2;
    public static final int LONG_BREAK_DURATION_MAX = 60;
    public static final int LONG_BREAK_INTERVAL_MIN = 0;
    public static final int LONG_BREAK_INTERVAL_MAX = 10;

    private final SharedPreferences mSharePreferences;

    public Settings(Context context) {
        mSharePreferences = context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE);
    }

    @Override
    public Map<String, ?> getAll() {
        return mSharePreferences.getAll();
    }

    @Nullable
    @Override
    public String getString(String key, String defValue) {
        return mSharePreferences.getString(key, defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return mSharePreferences.getStringSet(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return mSharePreferences.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return mSharePreferences.getLong(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return mSharePreferences.getFloat(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return mSharePreferences.getBoolean(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return mSharePreferences.contains(key);
    }

    @Override
    public Editor edit() {
        return mSharePreferences.edit();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mSharePreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mSharePreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public String getMaccAddr() {
        return mSharePreferences.getString(KEY_MAC_ADDRESS, null);
    }

    public int getWorkDuration() {
        return mSharePreferences.getInt(KEY_WORK_DURATION, DEFAULT_WORK_DURATION);
    }

    public int getBreakDuration() {
        return mSharePreferences.getInt(KEY_BREAK_DURATION, DEFAULT_BREAK_DURATION);
    }

    public int getLongBreakDuration() {
        return mSharePreferences.getInt(KEY_LONG_BREAK_DURATION, DEFAULT_LONG_BREAK_DURATION);
    }

    public int getLongBreakInterval() {
        return mSharePreferences.getInt(KEY_LONG_BREAK_INTERVAL, DEFAULT_LONG_BREAK_INTEVAL);
    }

    public boolean isPaired() {
        return mSharePreferences.contains(KEY_MAC_ADDRESS);
    }

    public void unpair() {
        mSharePreferences.edit().remove(KEY_MAC_ADDRESS).apply();
    }

    public void setMacAddr(String address) {
        mSharePreferences.edit().putString(KEY_MAC_ADDRESS, address).apply();
    }

    public void setWorkDuration(int value) {
        mSharePreferences.edit().putInt(KEY_WORK_DURATION, value).apply();
    }

    public void setBreakDuration(int value) {
        mSharePreferences.edit().putInt(KEY_BREAK_DURATION, value).apply();
    }

    public void setLongBreakDuration(int value) {
        mSharePreferences.edit().putInt(KEY_LONG_BREAK_DURATION, value).apply();
    }

    public void setLongBreakInterval(int value) {
        mSharePreferences.edit().putInt(KEY_LONG_BREAK_INTERVAL, value).apply();
    }
}
