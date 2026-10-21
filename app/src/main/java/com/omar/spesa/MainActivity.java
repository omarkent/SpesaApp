package com.omar.spesa;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int PHOTO_CAMERA = 101;
    private static final int PHOTO_GALLERY = 102;
    private WebView webView;
    private Uri cameraPhotoUri;
    private File cameraPhotoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().setStatusBarColor(Color.rgb(11, 13, 16));
            getWindow().setNavigationBarColor(Color.rgb(11, 13, 16));

            webView = new WebView(this);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);

            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            webView.addJavascriptInterface(new SpesaBridge(), "SpesaNative");
            webView.setBackgroundColor(Color.rgb(11, 13, 16));
            setContentView(webView);
            webView.loadUrl("file:///android_asset/index.html");
        } catch (Throwable error) {
            TextView fallback = new TextView(this);
            fallback.setText("Spesa non riesce ad avviarsi.\n\n"
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
            fallback.setTextColor(Color.WHITE);
            fallback.setBackgroundColor(Color.rgb(11, 13, 16));
            fallback.setTextSize(16);
            fallback.setGravity(Gravity.CENTER);
            fallback.setPadding(40, 40, 40, 40);
            setContentView(fallback);
        }
    }

    private class SpesaBridge {
        @JavascriptInterface
        public void takePhoto() {
            runOnUiThread(() -> openCamera());
        }

        @JavascriptInterface
        public void pickImage() {
            runOnUiThread(() -> openGallery());
        }
    }

    private void openCamera() {
        try {
            File photos = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (photos == null) throw new IOException("Archivio foto non disponibile");
            cameraPhotoFile = File.createTempFile("spesa_", ".jpg", photos);
            cameraPhotoUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", cameraPhotoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            intent.setClipData(ClipData.newUri(getContentResolver(), "Foto lista spesa", cameraPhotoUri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, PHOTO_CAMERA);
        } catch (Exception error) {
            reportPhotoResult("", "Impossibile aprire la fotocamera: " + error.getMessage());
        }
    }

    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, PHOTO_GALLERY);
        } catch (Exception error) {
            reportPhotoResult("", "Impossibile aprire la galleria: " + error.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PHOTO_CAMERA && requestCode != PHOTO_GALLERY) return;
        if (resultCode != RESULT_OK) {
            reportPhotoResult("", "Operazione annullata.");
            return;
        }
        Uri selected = requestCode == PHOTO_CAMERA
                ? cameraPhotoUri
                : data != null ? data.getData() : null;
        if (selected == null) {
            reportPhotoResult("", "Nessuna immagine ricevuta.");
            return;
        }
        if (requestCode == PHOTO_CAMERA && (cameraPhotoFile == null
                || !cameraPhotoFile.isFile() || cameraPhotoFile.length() == 0)) {
            reportPhotoResult("", "La fotocamera non ha salvato la foto.");
            return;
        }
        readTextFromPhoto(selected);
    }

    private void readTextFromPhoto(Uri photo) {
        reportPhotoStatus("Lettura delle scritte in corso…");
        try {
            InputImage image = InputImage.fromFilePath(this, photo);
            TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(result -> reportPhotoResult(result.getText(), ""))
                    .addOnFailureListener(error -> reportPhotoResult("",
                            "Non riesco a leggere la foto: " + error.getMessage()))
                    .addOnCompleteListener(task -> recognizer.close());
        } catch (Exception error) {
            reportPhotoResult("", "Impossibile leggere l'immagine: " + error.getMessage());
        }
    }

    private void callJavaScript(String expression) {
        runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(expression, null);
        });
    }

    private void reportPhotoStatus(String status) {
        callJavaScript("window.spesaOCRStatus(" + JSONObject.quote(status) + ");");
    }

    private void reportPhotoResult(String text, String error) {
        callJavaScript("window.spesaOCRResult(" + JSONObject.quote(text) + ","
                + JSONObject.quote(error) + ");");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }
}
