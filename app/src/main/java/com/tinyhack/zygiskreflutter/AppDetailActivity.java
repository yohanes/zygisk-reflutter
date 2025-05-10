package com.tinyhack.zygiskreflutter;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

public class AppDetailActivity extends AppCompatActivity {

    private boolean isFlutter = false;
    private EngineHashInfo.EngineHashEntry entry = null;

    private Set<String> getFlutterhash(String apkPath, String flutterPath) {
        try {
            // Open the APK file as a ZipFile
            try (ZipFile zipFile = new ZipFile(apkPath)) {

                // Get the ZipEntry for the flutterPath
                ZipEntry entry = zipFile.getEntry(flutterPath);

                // Open an InputStream for the ZipEntry
                InputStream stream = zipFile.getInputStream(entry);

                BufferedInputStream bstream = new BufferedInputStream(stream);

                // Initialize an empty StringBuilder for the current sequence of hexadecimal characters
                StringBuilder currentSequence = new StringBuilder();

                Set<String> hashsFound = new LinkedHashSet<>();
                // Read the InputStream byte by byte
                int b;
                while ((b = bstream.read()) != -1) {
                    char c = (char) b;

                    // Check if the character is a hexadecimal character
                    if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                        // If it is, append it to the current sequence
                        currentSequence.append(c);

                    } else {
                        // If the current sequence reaches 32 characters, return it
                        if (currentSequence.length() == 32) {
                            String thehash = currentSequence.toString();
                            Log.d("ZigiskReflutter", "flutter hash : " + thehash);
                            hashsFound.add(thehash);
                        }

                        // If it's not a hexadecimal character, reset the current sequence
                        currentSequence.setLength(0);
                    }
                }

                // If the end of the InputStream is reached and no 32-character sequence has been found, return null
                return hashsFound;
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private String getFirstLineFromFile(String path) {
        try {
            FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            bufferedReader.close();
            return line;
        } catch (IOException e) {
            return null;
        }
    }

    private void saveToFile(String path, String content) {
        try {
            java.io.FileWriter fileWriter = new java.io.FileWriter(path);
            fileWriter.write(content);
            fileWriter.close();
        } catch (IOException e) {
            //ignore
        }
    }

    private boolean hasV2File() {
        String dest = getFilesDir().getAbsolutePath() + "/v2-" + entry.Snapshot_hash + ".so";
        return new java.io.File(dest).exists();
    }

    private boolean hasV3File() {
        String dest = getFilesDir().getAbsolutePath() + "/v3-" + entry.Snapshot_hash + ".so";
        return new java.io.File(dest).exists();
    }

    private String arch;
    private String packageName;
    private String enable_path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_detail);

        Intent intent = getIntent();
        String appName = intent.getStringExtra("appName");
        packageName = intent.getStringExtra("packageName");
        TextView appNameTextView = findViewById(R.id.appName);
        appNameTextView.setText(appName);
        TextView packageNameTextView = findViewById(R.id.packageName);
        packageNameTextView.setText(packageName);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        ProgressBar progressBarDump = findViewById(R.id.progressBarDump);
        CheckBox checkBox = findViewById(R.id.enableHookCheckbox);
        TextView downloadStatus = findViewById(R.id.downloadStatus);
        downloadStatus.setText("");
        TextView downloadDumpStatus = findViewById(R.id.downloadDumpStatus);
        downloadDumpStatus.setText("");
        CheckBox checkBoxClassDump = findViewById(R.id.enableDumpHookCheckbox);
        checkBoxClassDump.setEnabled(false);
        checkBoxClassDump.setChecked(false);

        enable_path = getFilesDir().getAbsolutePath() + "/" + packageName + ".txt";

        //set as unchecked and disabled
        checkBox.setChecked(false);
        checkBox.setEnabled(false);
        //add handler
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                if (entry != null) {
                    saveToFile(enable_path, "v2-" + entry.Snapshot_hash);
                }
            } else {
                try {
                    new java.io.File(enable_path).delete();
                } catch (Exception e) {
                    //ignore
                }
            }
        });

        checkBoxClassDump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                //must disable first checkbox
                checkBox.setEnabled(false);
                checkBox.setChecked(true);
                if (entry != null) {
                    saveToFile(enable_path, "v3-" + entry.Snapshot_hash);
                }
            } else {
                if (hasV2File()) {
                    checkBox.setEnabled(true);
                    saveToFile(enable_path, "v2-" + entry.Snapshot_hash);
                } else {
                    checkBox.setChecked(false);
                    try {
                        new java.io.File(enable_path).delete();
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
        });

        arch = System.getProperty("os.arch");
        if (arch.equals("aarch64")) {
            arch = "arm64";
        } else if (arch.equals("armv71")) {
            arch = "arm";
        }

        Button downloadProxyLibButton = (Button) findViewById(R.id.downloadProxyLibButton);
        //disable the button
        downloadProxyLibButton.setEnabled(false);
        //download button handler
        downloadProxyLibButton.setOnClickListener(v -> {
            //start download
            EngineHashInfo engineHashInfo = EngineHashInfo.getInstance(AppDetailActivity.this);
            String url = "https://github.com/Impact-I/reFlutter/releases/download/android-v2-";
            url += entry.Snapshot_hash + "/libflutter_" + arch + ".so";

            String dest = getFilesDir().getAbsolutePath() + "/v2-" + entry.Snapshot_hash + ".so";
            downloadProxyLibButton.setEnabled(false);
            DownloadFileTask task = new DownloadFileTask(progressBar);
            task.setListener(new DownloadFileTask.DownloadListener() {
                @Override
                public void onSuccess() {
                    downloadProxyLibButton.setEnabled(false);
                    downloadStatus.setText("Downloaded " + entry.Snapshot_hash + ".so");
                    checkBox.setEnabled(true);
                }

                @Override
                public void onError(String message) {
                    downloadStatus.setText("Download failed: " + message);
                    downloadProxyLibButton.setEnabled(true);
                }
            });
            downloadStatus.setText("Downloading " + entry.Snapshot_hash + ".so");
            task.downloadFile(url, dest);
        });

        Button downloadClassDumpButton = (Button) findViewById(R.id.downloadClassDumpButton);
        //disable the button
        downloadClassDumpButton.setEnabled(false);
        downloadClassDumpButton.setOnClickListener(v -> {
            //start download
            EngineHashInfo engineHashInfo = EngineHashInfo.getInstance(AppDetailActivity.this);
            String url = "https://github.com/Impact-I/reFlutter/releases/download/android-v3-";
            url += entry.Snapshot_hash + "/libflutter_" + arch + ".so";
            String dest = getFilesDir().getAbsolutePath() + "/v3-" + entry.Snapshot_hash + ".so";
            downloadClassDumpButton.setEnabled(false);
            DownloadFileTask task = new DownloadFileTask(progressBarDump);
            task.setListener(new DownloadFileTask.DownloadListener() {
                @Override
                public void onSuccess() {
                    downloadClassDumpButton.setEnabled(false);
                    downloadDumpStatus.setText("Downloaded " + entry.Snapshot_hash + ".so");
                    checkBoxClassDump.setEnabled(true);
                }

                @Override
                public void onError(String message) {
                    downloadDumpStatus.setText("Download failed: " + message);
                    downloadClassDumpButton.setEnabled(true);
                }
            });
            downloadDumpStatus.setText("Downloading " + entry.Snapshot_hash + ".so");
            task.downloadFile(url, dest);
        });


        //get application info
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(packageName, 0);
            //if apk has splitPublicSourceDirs use that, otherwise create array from publicSourceDir
            String sourceDirs[];
            String libAPK; //apk containing .so
            if (appInfo.splitPublicSourceDirs != null) {
                sourceDirs = appInfo.splitPublicSourceDirs;
                //find which apk contains lib
                libAPK = null;
                for (String dir : appInfo.splitPublicSourceDirs) {
                    //name must contain splitconfig.arm or splitconfig.x86
                    if (dir.contains("split_config.")) {
                        libAPK = dir;
                        break;
                    }
                }
            } else {
                sourceDirs = new String[]{appInfo.publicSourceDir};
                libAPK = appInfo.publicSourceDir;
            }
            Log.d("ZigiskReflutter", "libAPK : " + libAPK);
            TextView apkName = findViewById(R.id.apkName);
            if (libAPK != null) {
                //apkName.setText(libAPK);
                //open zip file, find if libflutter.so exists
                //if it does, then it's a flutter app
                ZipFile zipFile = null;
                String flutterPath = null;
                try {


                    zipFile = new ZipFile(libAPK);
                    //iterate through entries
                    isFlutter = false;
                    for (java.util.Enumeration<? extends java.util.zip.ZipEntry> e = zipFile.entries(); e.hasMoreElements(); ) {
                        java.util.zip.ZipEntry entry = e.nextElement();
                        if (entry.getName().contains("libflutter.so")) {
                            flutterPath = entry.getName();
                            isFlutter = true;
                            break;
                        }
                    }

                    if (isFlutter) {
                        //find the hash is a new thread, since it is slow
                        String finalLibAPK = libAPK;
                        String finalFlutterPath = flutterPath;
                        Log.d("ZigiskReflutter", "flutterPath : " + flutterPath);
                        new Thread(() -> {
                            Set<String> flutterHashes = getFlutterhash(finalLibAPK, finalFlutterPath);
                            Log.d("ZigiskReflutter", "flutterHashes : " + flutterHashes);
                            runOnUiThread(() -> {
                                //try to find this hash
                                EngineHashInfo engineHashInfo = EngineHashInfo.getInstance(AppDetailActivity.this);
                                for (String flutterHash : flutterHashes) {
                                    entry = engineHashInfo.findEntryBySnapshotHash(flutterHash);
                                    if (entry != null) {
                                        break;
                                    }
                                }
                                if (entry != null) {

                                    apkName.setText("Supported Flutter " + entry.version);
                                    String dest = getFilesDir().getAbsolutePath() + "/v2-" + entry.Snapshot_hash + ".so";
                                    //if dest exists
                                    if (new java.io.File(dest).exists()) {
                                        downloadProxyLibButton.setEnabled(false);
                                        checkBox.setEnabled(true);
                                        String line = getFirstLineFromFile(enable_path);
                                        //if exists, then it is enabled
                                        if (line != null) {
                                            if (line.contains("v3")) {
                                                checkBox.setChecked(true);
                                                checkBox.setEnabled(false);
                                                checkBoxClassDump.setEnabled(true);
                                                checkBoxClassDump.setChecked(true);
                                            } else {
                                                checkBox.setChecked(true);
                                            }
                                        } else {
                                            checkBox.setChecked(false);
                                        }
                                    } else {
                                        downloadProxyLibButton.setEnabled(true);
                                        checkBox.setChecked(false);
                                        checkBox.setEnabled(false);
                                    }
                                    String destDump = getFilesDir().getAbsolutePath() + "/v3-" + entry.Snapshot_hash + ".so";
                                    //if dest exists
                                    if (new java.io.File(destDump).exists()) {
                                        downloadClassDumpButton.setEnabled(false);
                                        checkBoxClassDump.setEnabled(true);
                                        //if exists, then it is enabled
                                        if (new java.io.File(enable_path).exists()) {
                                            //read the file content
                                            String line = getFirstLineFromFile(enable_path);
                                            if (line != null && line.contains("v3")) {
                                                checkBoxClassDump.setChecked(true);
                                            } else {
                                                checkBoxClassDump.setChecked(false);
                                            }
                                        } else {
                                            checkBoxClassDump.setChecked(false);
                                        }
                                    } else {
                                        downloadClassDumpButton.setEnabled(true);
                                        checkBoxClassDump.setChecked(false);
                                        checkBoxClassDump.setEnabled(false);
                                    }

                                } else {
                                    apkName.setText("Unsupported Flutter " + flutterHashes);
                                }
                            });
                        }).start();

                        apkName.setText("Flutter app. Finding hash... ");

                    } else {
                        apkName.setText("Not a Flutter app");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } else {
                apkName.setText("No APK found");
            }

        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
