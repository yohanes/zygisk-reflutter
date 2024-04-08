package com.tinyhack.zygiskreflutter;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView textViewVersion = findViewById(R.id.textViewVersion);
        boolean zygiskModuleInstalled = false;
        //check if "installed.txt" exists
        String filesdir = getFilesDir().getAbsolutePath();
        String installedFile = filesdir + "/installed.txt";
        java.io.File file = new java.io.File(installedFile);
        if (file.exists())
            zygiskModuleInstalled = true;

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            if (!zygiskModuleInstalled)
                version += " (NO Zygisk module found)";
            textViewVersion.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Button visitGithubButton = findViewById(R.id.visitGithubButton);
        visitGithubButton.setOnClickListener(v -> {
            String url = "https://github.com/yohanes/zygisk-reflutter";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
    }
}
