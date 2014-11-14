package com.android.utils.wificonnecter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.utils.wificonnecter.WiFiConnecter.ActionListener;

public class TestActivity extends Activity implements ActionListener {

    private WifiManager mWifiManager;
    private TextView tv_CurrentSsid;
    private EditText et_Ssid;
    private EditText et_Password;
    private WiFiConnecter mWiFiConnecter;

    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        init();
    }

    private void init() {
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWiFiConnecter = new WiFiConnecter(this);

        mDialog = new ProgressDialog(this);

        tv_CurrentSsid = (TextView) findViewById(R.id.tv_currentSsid);
        et_Ssid = (EditText) findViewById(R.id.et_ssid);
        et_Password = (EditText) findViewById(R.id.et_password);

        //Debug only
        //et_Ssid.setText("OTT_IPTV_3");
        //et_Password.setText("12345678");
        setCurrentSsid();
    }
    private void setCurrentSsid() {
        WifiInfo info = mWifiManager.getConnectionInfo();
        String s = (info == null) ? "null" : info.getSSID();
        tv_CurrentSsid.setText(String.format(getString(R.string.current_ssid), s));
    }

    public void connect(View view) {
        final String ssid = et_Ssid.getText().toString();
        final String password = et_Password.getText().toString();
        mWiFiConnecter.connect(ssid, password, this);
    }

    @Override
    public void onStarted(String ssid) {
        System.out.println("------onStarted------");
        Toast.makeText(TestActivity.this, "onStarted", Toast.LENGTH_SHORT).show();
        mDialog.setMessage("Connecting to " + ssid + " ...");
        mDialog.show();
    }

    @Override
    public void onSuccess(WifiInfo info) {
        System.out.println("------onSuccess------");
        Toast.makeText(TestActivity.this, "onSuccess", Toast.LENGTH_SHORT).show();
        setCurrentSsid();
    }

    @Override
    public void onFailure() {
        System.out.println("------onFailure------");
        Toast.makeText(TestActivity.this, "onFailure", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFinished() {
        System.out.println("------onFinished------");
        mDialog.dismiss();
        Toast.makeText(TestActivity.this, "onFinished", Toast.LENGTH_SHORT).show();
    }
}
