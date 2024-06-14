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

public class ConfigureStreamActivity extends AppCompatActivity {
	private String currentHost;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_configure_stream);
		
		Intent intent = getIntent();
		currentHost = intent.getStringExtra(getString(R.string.current_host));
		if (currentHost == null)
			throw new RuntimeException("Can't get current host from intent extra");
		
		HostConfiguration hostConfiguration = MainApplication.getInstance().getHostConfiguration(currentHost);
		if (hostConfiguration == null) {
			hostConfiguration = new HostConfiguration(currentHost);
			MainApplication.getInstance().addHostConfiguration(hostConfiguration);
			
			if (MainApplication.getInstance().isHostConfigurationsChanged())
				MainApplication.getInstance().saveHostConfigurations();
		}
		
		TextView tvHost = findViewById(R.id.tv_host);
		tvHost.setText(hostConfiguration.getHost());
		EditText etPort = findViewById(R.id.et_port);
		etPort.setText(String.valueOf(hostConfiguration.getPort()));

		CheckBox cbTlsRequired = findViewById(R.id.cb_tls_required);
		cbTlsRequired.setChecked(hostConfiguration.isTlsRequired());
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
		
		HostConfiguration hostConfiguration = MainApplication.getInstance().getHostConfiguration(currentHost);
		if (hostConfiguration == null)
			throw new RuntimeException(String.format("Failed get host configuration for host %s.", currentHost));
		
		boolean hostConfigurationChanged = false;
		if (hostConfiguration.getPort() != port) {
			hostConfiguration.setPort(port);
			hostConfigurationChanged = true;
		}
		
		CheckBox cbTlsRequired = findViewById(R.id.cb_tls_required);
		boolean tlsRequired = cbTlsRequired.isChecked();
		if (hostConfiguration.isTlsRequired() != tlsRequired) {
			hostConfiguration.setTlsRequired(tlsRequired);
			hostConfigurationChanged = true;
		}
		
		if (hostConfigurationChanged) {
			MainApplication.getInstance().updateHostConfiguration(hostConfiguration);
			MainApplication.getInstance().saveHostConfigurations();
		}
		
		finish();
	}
}
