package com.tinyhack.zygiskreflutter;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class EngineHashInfo {

    public static class EngineHashEntry {
        String version;
        String Engine_commit;
        String Snapshot_hash;
    }

    EngineHashEntry findEntryBySnapshotHash(String snapshotHash) {
        for (EngineHashEntry entry : entries) {
            if (entry.Snapshot_hash.equals(snapshotHash)) {
                return entry;
            }
        }
        return null;
    }

    private Context context;

    private static EngineHashInfo instance = null;

    //entries
    private EngineHashEntry[] entries;

    public static EngineHashInfo getInstance(Context context) {
        if (instance == null) {
            instance = new EngineHashInfo(context);
        }
        return instance;
    }

    private EngineHashInfo(Context context) {
        this.context = context;
    }

    private String getLocalCacheFile() {
        //get files dir
        return context.getFilesDir().getAbsolutePath() + "/enginehash.csv";
    }

    private void readFromFile(String localCacheFile) throws IOException {
            //open file
            File file = new File(localCacheFile);
            InputStream inputStream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<EngineHashEntry> entries = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                EngineHashEntry entry = new EngineHashEntry();
                entry.version = parts[0];
                entry.Engine_commit = parts[1];
                entry.Snapshot_hash = parts[2];
                entries.add(entry);
            }
            this.entries = entries.toArray(new EngineHashEntry[0]);
    }

    void updateData() throws  IOException {
        //check if we hace a local cache
        String localCacheFile = getLocalCacheFile();
        File file = new File(localCacheFile);
        if (file.exists()) {
            try {
                //read from file
                readFromFile(localCacheFile);
                return;
            } catch (IOException e) {
                //if we can't read from file, download again
                file.delete();
            }
        }
        downloadData();
    }

    void downloadData() throws IOException {
            String localCacheFile = getLocalCacheFile();

            // Create a Url object from the string
            URL url = new URL("https://raw.githubusercontent.com/Impact-I/reFlutter/main/enginehash.csv");

            // Create a connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Get the input stream of the connection
            InputStream inputStream = connection.getInputStream();
            //save to file
            //open file
            FileOutputStream outputStream = new FileOutputStream(localCacheFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            //close file
            outputStream.close();
            readFromFile(localCacheFile);

            // Disconnect the connection
            connection.disconnect();
    }
}
