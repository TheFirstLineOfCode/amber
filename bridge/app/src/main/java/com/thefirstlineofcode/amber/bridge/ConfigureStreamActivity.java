package com.thefirstlineofcode.amber.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;

import java.net.Inet4Address;
import java.net.InetAddress;

public class ConfigureStreamActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_configure_stream);
		
		Intent intent = getIntent();
		String sStreamConfiguration = intent.getStringExtra(getString(R.string.stream_configuration));
		if (sStreamConfiguration == null)
			throw new RuntimeException("Can't get stream configuration from intent extra");
		
		StandardStreamConfig streamConfig = readStreamConfiguration(sStreamConfiguration);
		TextView tvHost = findViewById(R.id.tv_host);
		tvHost.setText(streamConfig.getHost());
		EditText etPort = findViewById(R.id.et_port);
		etPort.setText(String.valueOf(streamConfig.getPort()));

		CheckBox cbEnableTls = findViewById(R.id.cb_enable_tls);
		cbEnableTls.setChecked(streamConfig.isTlsPreferred());
	}
	
	private StandardStreamConfig readStreamConfiguration(String sStreamConfiguration) {
		return null;
	}
	
	public void configureStream(View view) {
		EditText etPort = findViewById(R.id.et_port);
		if (TextUtils.isEmpty(etPort.getText().toString())) {
			runOnUiThread(() -> {
				Toast.makeText(this, getString(R.string.port_cant_be_null), Toast.LENGTH_LONG).show();
				etPort.requestFocus();
			});

			return;
		}

		boolean portIsInvalid = false;
		int port = -1;
		try {
			port = Integer.parseInt(etPort.getText().toString());

			if (port <= 0) {
				portIsInvalid = true;
			}
		} catch (NumberFormatException e) {
			portIsInvalid = true;
		}

		if (portIsInvalid) {
			runOnUiThread(() -> {
				Toast.makeText(this, getString(R.string.port_must_be_an_positive_integer), Toast.LENGTH_LONG).show();
				etPort.selectAll();
				etPort.requestFocus();
			});

			return;
		}

		CheckBox cbEnableTls = findViewById(R.id.cb_enable_tls);
		
		TextView tvHost = findViewById(R.id.tv_host);
		StandardStreamConfig streamConfig = new StandardStreamConfig(
				tvHost.getText().toString(), port);
		streamConfig.setTlsPreferred(cbEnableTls.isChecked());

		finish();
	}
}
