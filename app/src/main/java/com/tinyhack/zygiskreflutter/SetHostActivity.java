package com.tinyhack.zygiskreflutter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.text.TextWatcher;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class SetHostActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_host);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        EditText editTextProxyIP = findViewById(R.id.proxyIP);
        //get the current value
        String proxyIP = Util.getPoxyIPFromFile(getFilesDir().getAbsolutePath());
        editTextProxyIP.setText(proxyIP);

        TextView IPStatus = findViewById(R.id.IPStatus);
        IPStatus.setText("Valid IP");

        //monitor changes
        editTextProxyIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                //check if entry is valid IP
                String ip = s.toString();
                if (!ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                    IPStatus.setText("Invalid IP");
                    return;
                }
                IPStatus.setText("Valid IP");
                Util.saveProxyIPToFile(getFilesDir().getAbsolutePath(),s.toString());
            }
        });
    }
}
