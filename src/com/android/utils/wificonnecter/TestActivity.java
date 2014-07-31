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

public class TestActivity extends Activity {

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
		
		findViewById(R.id.btn_connect).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final String ssid = et_Ssid.getText().toString();
				final String password = et_Password.getText().toString();

				mWiFiConnecter.connect(ssid, password, new ActionListener() {

					@Override
					public void onStart() {
						System.out.println("------onStart------");
						Toast.makeText(TestActivity.this, "onStart", Toast.LENGTH_SHORT).show();
						mDialog.setMessage("Connecting to "+ssid+" ...");
						mDialog.show();
					}

					@Override
					public void onSuccess() {
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
				});
			}
		});
	}
	
	private void setCurrentSsid(){
		WifiInfo info = mWifiManager.getConnectionInfo();
		tv_CurrentSsid.setText(String.format(getString(R.string.current_ssid), (info == null) ? "null" : info.getSSID()));
	}
}
