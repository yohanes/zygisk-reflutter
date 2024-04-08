package com.tinyhack.zygiskreflutter;

import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownloadFileTask {

    //create listener for error and success
    public interface DownloadListener {
        void onSuccess();
        void onError(String message);
    }

    private ProgressBar progressBar;
    private ExecutorService executorService;
    private Handler handler;
    private  DownloadListener listener;

    public DownloadFileTask(ProgressBar progressBar) {
        this.progressBar = progressBar;
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void setListener(DownloadListener listener) {
        this.listener = listener;
    }

    public void downloadFile(String urlPath, String dest) {
        Future<?> future = executorService.submit(() -> {
            try {
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // Get file length
                int fileLength = connection.getContentLength();

                // Download the file
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(dest);

                byte data[] = new byte[1024];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // Publish the progress
                    int finalCount = (int) (total * 100 / fileLength);
                    handler.post(() -> progressBar.setProgress(finalCount));
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                connection.disconnect();
                if (listener != null) {
                    handler.post(() -> listener.onSuccess());
                }
            } catch (Exception e) {
                if (listener != null) {
                    handler.post(() -> listener.onError(e.getMessage()));
                }
                e.printStackTrace();
            }
        });
    }
}
