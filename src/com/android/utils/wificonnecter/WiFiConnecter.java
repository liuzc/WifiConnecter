package com.android.utils.wificonnecter;

import java.util.List;

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
    private WifiManager mWifiManager;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private final Scanner mScanner;
    private ActionListener mListener;
    private String mSsid;
    private String mPassword;

    private boolean isRegistered;
    private boolean isActiveScan;
    private boolean bySsidIgnoreCase;

    public WiFiConnecter(Context context) {
        this.mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

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


    /**
     * Connect to a WiFi with the given ssid and password
     *
     * @param ssid
     * @param password
     * @param listener : for callbacks on start or success or failure. Can be null.
     */
    public void connect(String ssid, String password, ActionListener listener) {
        this.mListener = listener;
        this.mSsid = ssid;
        this.mPassword = password;

        if (listener != null) {
            listener.onStarted(ssid);
        }

        WifiInfo info = mWifiManager.getConnectionInfo();
        String quotedString = StringUtils.convertToQuotedString(mSsid);
        boolean ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(info.getSSID())
                : quotedString.equals(info.getSSID());
        if (ssidEquals) {
            if (listener != null) {
                listener.onSuccess(info);
                listener.onFinished(true);
            }
            return;
        }

        mScanner.forceScan();
    }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        //An access point scan has completed, and results are available from the supplicant.
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) && isActiveScan) {
            List<ScanResult> results = mWifiManager.getScanResults();
            for (ScanResult result : results) {
                //1.scan dest of ssid
                String quotedString = StringUtils.convertToQuotedString(mSsid);
                boolean ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(result.SSID)
                        : quotedString.equals(result.SSID);
                if (ssidEquals) {
                    //TODO ?
                    mScanner.pause();
                    //2.input error password
                    if (!WiFi.connectToNewNetwork(mWifiManager, result, mPassword)) {
                        if (mListener != null) {
                            mListener.onFailure();
                            mListener.onFinished(false);
                        }
                        onPause();
                    }
                    break;
                }
            }

            //Broadcast intent action indicating that the state of Wi-Fi connectivity has changed.
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo mInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            //ssid equals&&connected
            if (mWifiInfo != null && mInfo.isConnected() && mWifiInfo.getSSID() != null) {
                String quotedString = StringUtils.convertToQuotedString(mSsid);
                boolean ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(mWifiInfo.getSSID())
                        : quotedString.equals(mWifiInfo.getSSID());
                if (ssidEquals) {
                    if (mListener != null) {
                        mListener.onSuccess(mWifiInfo);
                        mListener.onFinished(true);
                    }
                    onPause();
                }

            }
        }
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
            if (mRetry < MAX_TRY_COUNT) {
                mRetry++;
                isActiveScan = true;
                //1.打开Wifi
                if (!mWifiManager.isWifiEnabled()) {
                    mWifiManager.setWifiEnabled(true);
                }
                //TODO startScan什么时候返回false
                boolean startScan = mWifiManager.startScan();
                Log.d(TAG, "startScan:" + startScan);
                //执行扫描失败（bind机制）
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
