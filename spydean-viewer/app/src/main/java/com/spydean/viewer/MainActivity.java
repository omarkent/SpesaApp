package com.spydean.viewer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.*;
import java.io.*;
import java.net.*;

public class MainActivity extends Activity {
    private EditText url;
    private TextView status;
    private ImageView video;
    private volatile boolean stop = false;
    private Thread streamThread;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        url = findViewById(R.id.url);
        status = findViewById(R.id.status);
        video = findViewById(R.id.video);
        String saved = getPreferences(MODE_PRIVATE).getString("url", "");
        url.setText(saved);
        findViewById(R.id.connect).setOnClickListener(v -> connect());
    }

    private void connect() {
        stop = true;
        if (streamThread != null) try { streamThread.join(250); } catch(Exception ignored) {}
        stop = false;
        String u = url.getText().toString().trim();
        if (u.length() == 0) {
            status.setText("Inserisci l'indirizzo mostrato sul J1");
            return;
        }
        getPreferences(MODE_PRIVATE).edit().putString("url", u).apply();
        streamThread = new Thread(() -> readStream(u), "SpyDeanViewer");
        streamThread.start();
    }

    private void readStream(String u) {
        HttpURLConnection c = null;
        try {
            runOnUiThread(() -> status.setText("Connessione..."));
            c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(7000);
            c.setReadTimeout(15000);
            c.setUseCaches(false);
            c.connect();
            BufferedInputStream in = new BufferedInputStream(c.getInputStream(), 64 * 1024);
            runOnUiThread(() -> status.setText("LIVE"));
            while (!stop) {
                String line;
                int contentLength = -1;
                do {
                    line = readLine(in);
                    if (line == null) throw new EOFException();
                    String lower = line.toLowerCase();
                    if (lower.startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                    }
                } while (line.length() != 0);

                if (contentLength <= 0 || contentLength > 2_000_000) continue;
                byte[] jpg = readExactly(in, contentLength);
                readLine(in);
                Bitmap bmp = BitmapFactory.decodeByteArray(jpg, 0, jpg.length);
                if (bmp != null) runOnUiThread(() -> video.setImageBitmap(bmp));
            }
        } catch(Exception e) {
            if (!stop) runOnUiThread(() -> status.setText("Offline / errore: " + e.getMessage()));
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int ch;
        boolean got = false;
        while ((ch = in.read()) != -1) {
            got = true;
            if (ch == '\n') break;
            if (ch != '\r') b.write(ch);
        }
        if (!got && ch == -1) return null;
        return b.toString("UTF-8");
    }

    private byte[] readExactly(InputStream in, int n) throws IOException {
        byte[] b = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(b, off, n - off);
            if (r < 0) throw new EOFException();
            off += r;
        }
        return b;
    }

    @Override protected void onDestroy() {
        stop = true;
        super.onDestroy();
    }
}
