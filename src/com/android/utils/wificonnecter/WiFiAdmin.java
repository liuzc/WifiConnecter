package com.android.utils.wificonnecter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class WiFiAdmin {
    /**
     * android WifiManager
     */
    private WifiManager mWifiManager;
    /**
     * WifiAdmin
     */
    private static WiFiAdmin mInstance;

    private WiFiAdmin(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * WifiAdmin
     *
     * @param context
     *            android context
     * @return WifiAdmin
     */
    public static WiFiAdmin getInstance(Context mContext) {
        if (mInstance == null) {
            synchronized (WiFiAdmin.class) {
                if (mInstance == null) {
                    mInstance = new WiFiAdmin(mContext);
                }
            }
        }
        return mInstance;
    }

    public WifiInfo getWifiInfo() {
        return mWifiManager.getConnectionInfo();
    }

    public boolean startScan() {
        return mWifiManager.startScan();
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    /**
     * get ScanResult list from android system
     *
     * @return ScanResult list
     */
    public List<ScanResult> getScanResults() {
        mWifiManager.getConfiguredNetworks();
        mWifiManager.startScan();
        return (ArrayList<ScanResult>) mWifiManager.getScanResults();
    }

    public boolean openWifi() {
        if (mWifiManager != null && !mWifiManager.isWifiEnabled()) {
            return mWifiManager.setWifiEnabled(true);
        }
        return false;
    }

    /**
     * Get the current phone connection wifi name
     *
     * @return
     */
    public String getCurrentWifiNetName(){
        return getWifiInfo().getSSID();
    }

    public String getServerAddress(){
        int serverAddress = mWifiManager.getDhcpInfo().serverAddress;
        return "http://" + intToIp(serverAddress) + "/";
    }

    private String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "."
                + (0xFF & paramInt >> 24);
    }

    /**
     * get connected ap id
     *
     * @return ap ip
     */
    public String getCurrentWifiIp() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        int i = wifiInfo.getIpAddress();
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + ((i >> 24) & 0xFF);

    }

    public boolean isWifiConnected() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        int ipAddress = wifiInfo == null ? 0 : wifiInfo.getIpAddress();
        if (mWifiManager.isWifiEnabled() && ipAddress != 0)
            return true;
        else
            return false;
    }

}
