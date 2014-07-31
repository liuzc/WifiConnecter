WifiConnecter
=============

A android library that allows you to connect to a Wi-Fi hotspot with the given ssid and password.

This library is very easy to use:

    	    WiFiConnecter mWiFiConnecter = new WiFiConnecter(mContext);
		    mWiFiConnecter.connect("ssid", "password", new WiFiConnecter.ActionListener() {
			@Override
			public void onStart() {
			}

			@Override
			public void onSuccess() {
			}

			@Override
			public void onFailure() {
			}

			@Override
			public void onFinished() {
			}

		});

#Thanks #
----------
This library built on the great [android-wifi-connecter](https://code.google.com/p/android-wifi-connecter/ "android-wifi-connecter") library.


#Contributing #
----------

Changes and improvements are more than welcome! Feel free to fork and open a pull request. Please make your changes in a specific branch and request to pull into master!