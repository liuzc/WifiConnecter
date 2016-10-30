package com.android.utils.wificonnecter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

public class WiFiConnecter {

    // Combo scans can take 5-6s to complete
    private static final int WIFI_RESCAN_INTERVAL_MS = 5 * 1000;

    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;
    private static final String TAG = WiFiConnecter.class.getSimpleName();
    public static final int MAX_TRY_COUNT = 3;

    private Context mContext;
    // private WifiManager mWifiManager;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private final Scanner mScanner;
    private ActionListener mListener;
    private String mSsid;
    private String mPassword;

    private boolean isRegistered;
    private boolean isActiveScan;
    private boolean bySsidIgnoreCase;
    private TimerTask mTimerTask = null;
    private boolean isWifiConnected = false;
    private Timer mTimer = null;

    public WiFiConnecter(Context context) {
        this.mContext = context;
        // mWifiManager = (WifiManager)
        // context.getSystemService(Context.WIFI_SERVICE);

        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };

        context.registerReceiver(mReceiver, mFilter);
        isRegistered = true;
        bySsidIgnoreCase = true;
        mScanner = new Scanner();
    }

    public void openWifi() {
        WiFiAdmin.getInstance(mContext).openWifi();
    }

    public List<ScanResult> getWifiList() {
        List<ScanResult> wifiList = WiFiAdmin.getInstance(mContext).getScanResults();
        List<ScanResult> newlist = new ArrayList<ScanResult>();
        newlist.clear();
        for (ScanResult result : wifiList) {
            if (!TextUtils.isEmpty(result.SSID) && !containName(newlist, result.SSID))
                newlist.add(result);
        }
        // return WiFiAdmin.getInstance(mContext).getWifiList();
        return newlist;
    }

    public boolean containName(List<ScanResult> sr, String name) {
        for (ScanResult result : sr) {
            if (!TextUtils.isEmpty(result.SSID) && result.SSID.equals(name))
                return true;
        }
        return false;
    }

    public String getConnectSsid() {
        return WiFiAdmin.getInstance(mContext).getCurrentWifiNetName();
    }

    public boolean isWifiActive() {
        return WiFiAdmin.getInstance(mContext).isWifiConnected();
    }

    /**
     * Connect to a WiFi with the given ssid and password
     *
     * @param ssid
     * @param password
     * @param listener
     *            : for callbacks on start or success or failure. Can be null.
     */
    public void connect(String ssid, String password, ActionListener listener) {
        this.mListener = listener;
        this.mSsid = ssid;
        this.mPassword = password;

        if (listener != null) {
            listener.onStarted(ssid);
        }

        WifiInfo info = WiFiAdmin.getInstance(mContext).getWifiInfo();
        String quotedString = StringUtils.convertToQuotedString(mSsid);
        boolean ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(StringUtils.convertToQuotedString(info.getSSID())) : quotedString.equals(StringUtils.convertToQuotedString(info.getSSID()));
        if (ssidEquals) {
            if (listener != null) {
                listener.onSuccess(info);
                listener.onFinished(true);
            }
            return;
        }

        mScanner.forceScan();
        // start timmer
        stopTimer();
        startTimer();
    }

    private void startTimer() {
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if (!isWifiConnected) { // wifi maybe connect failed
                        mScanner.removeMessages(1);
                        mScanner.sendEmptyMessage(1);
                    }
                }
            };
        }
        if (mTimer != null && mTimerTask != null)
            mTimer.schedule(mTimerTask, 12 * 1000, 1);
        Log.i(TAG, "start timmer ------> ");
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        isWifiConnected = false;
        Log.i(TAG, "stop timmer ------> ");
    }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        // An access point scan has completed, and results are available from
        // the supplicant.
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) && isActiveScan) {
            Log.i(TAG, "receiver action --> android.net.wifi.SCAN_RESULTS");
            List<ScanResult> results = getWifiList();
            Log.i(TAG, "results.size() == " + (results == null ? 0 : results.size()));
            for (ScanResult result : results) {
                Log.i(TAG, "ssid ---> " + result.SSID + " <-- bySsidIgnoreCase == " + bySsidIgnoreCase);
                // 1.scan dest of ssid
                String quotedString = StringUtils.convertToQuotedString(mSsid);
                boolean ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(StringUtils.convertToQuotedString(result.SSID)) : quotedString.equals(StringUtils.convertToQuotedString(result.SSID));
                Log.i(TAG, mSsid + " wifi isExist --> " + ssidEquals);
                if (ssidEquals) {
                    mScanner.pause();
                    // 2.input error password
                    if (!WiFi.connectToNewNetwork(WiFiAdmin.getInstance(mContext).getWifiManager(), result, mPassword)) {
                        Log.i(TAG, "connect failure");
                        if (mListener != null) {
                            mListener.onFailure();
                            mListener.onFinished(false);
                        }
                        onPause();
                    }
                    break;
                }
            }

            // Broadcast intent action indicating that the state of Wi-Fi
            // connectivity has changed.
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            Log.i(TAG, "receiver action --> android.net.wifi.STATE_CHANGE");
            NetworkInfo mInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            WifiInfo mWifiInfo = WiFiAdmin.getInstance(mContext).getWifiInfo();
            // ssid equals&&connected
            Log.i(TAG, " mInfo.getState() --> " + mInfo.getState());
            if (mWifiInfo != null && mInfo.isConnected() && mWifiInfo.getSSID() != null) {
                Log.i(TAG, "connect Success!");
                String quotedString = StringUtils.convertToQuotedString(mSsid);
                boolean ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(StringUtils.convertToQuotedString(mWifiInfo.getSSID())) : quotedString.equals(StringUtils.convertToQuotedString(mWifiInfo.getSSID()));
                if (ssidEquals) {
                    isWifiConnected = true;
                    stopTimer(); // connect success stop timer
                    if (mListener != null) {
                        mListener.onSuccess(mWifiInfo);
                        mListener.onFinished(true);
                    }
                    onPause();
                } else {
                    // TODO connect other wifi ssid
                }
            } else {
                Log.i(TAG, "mInfo.isConnected() --> " + mInfo.isConnected());
            }
        }
    }

    public boolean isScanResultOpen(ScanResult sr) {
        return WiFi.OPEN.equals(WiFi.getScanResultSecurity(sr));
    }

    public void onPause() {
        if (isRegistered) {
            mContext.unregisterReceiver(mReceiver);
            isRegistered = false;
        }
        mScanner.pause();
    }

    public void onResume() {
        if (!isRegistered) {
            mContext.registerReceiver(mReceiver, mFilter);
            isRegistered = true;
        }
        mScanner.resume();
    }

    @SuppressLint("HandlerLeak")
    private class Scanner extends Handler {
        private int mRetry = 0;

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            mRetry = 0;
            isActiveScan = false;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
            case 0:
                if (mRetry < MAX_TRY_COUNT) {
                    mRetry++;
                    isActiveScan = true;
                    openWifi();
                    boolean startScan = WiFiAdmin.getInstance(mContext).startScan();
                    Log.d(TAG, "startScan:" + startScan);
                    if (!startScan) {
                        if (mListener != null) {
                            mListener.onFailure();
                            mListener.onFinished(false);
                        }
                        onPause();
                        return;
                    }
                } else {
                    mRetry = 0;
                    isActiveScan = false;
                    if (mListener != null) {
                        mListener.onFailure();
                        mListener.onFinished(false);
                    }
                    onPause();
                    return;
                }
                sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
                break;

            case 1:
                stopTimer();
                if (mListener != null) {
                    mListener.onFailure();
                    mListener.onFinished(false);
                }
                onPause();
                break;
            }
        }
    }

    public interface ActionListener {

        /**
         * The operation started
         *
         * @param ssid
         */
        public void onStarted(String ssid);

        /**
         * The operation succeeded
         *
         * @param info
         */
        public void onSuccess(WifiInfo info);

        /**
         * The operation failed
         */
        public void onFailure();

        /**
         * The operation finished
         *
         * @param isSuccessed
         */
        public void onFinished(boolean isSuccessed);
    }

}
