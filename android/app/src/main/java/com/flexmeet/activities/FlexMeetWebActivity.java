package com.flexmeet.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class FlexMeetWebActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "meeting_url";
    private static final String WEB_ORIGIN = "https://flexmeet-aiman-3859.vercel.app";
    private static final int MEDIA_PERMISSION_REQUEST = 81;
    private WebView webView;
    private PermissionRequest pendingRequest;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equals(uri.getScheme()) && "flexmeet-aiman-3859.vercel.app".equals(uri.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> requestMediaAccess(request));
            }
        });
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || !url.startsWith(WEB_ORIGIN)) url = WEB_ORIGIN + "/";
        webView.loadUrl(url);
    }

    private void requestMediaAccess(PermissionRequest request) {
        boolean needsCamera = false;
        boolean needsMic = false;
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) needsCamera = true;
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) needsMic = true;
        }
        boolean cameraGranted = !needsCamera || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean micGranted = !needsMic || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if (cameraGranted && micGranted) {
            request.grant(request.getResources());
            return;
        }
        pendingRequest = request;
        String[] permissions = needsCamera && needsMic
                ? new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}
                : new String[]{needsCamera ? Manifest.permission.CAMERA : Manifest.permission.RECORD_AUDIO};
        requestPermissions(permissions, MEDIA_PERMISSION_REQUEST);
    }

    @Override public void onRequestPermissionsResult(int code, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code != MEDIA_PERMISSION_REQUEST || pendingRequest == null) return;
        boolean granted = true;
        for (int result : results) granted &= result == PackageManager.PERMISSION_GRANTED;
        if (granted) pendingRequest.grant(pendingRequest.getResources());
        else pendingRequest.deny();
        pendingRequest = null;
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (pendingRequest != null) pendingRequest.deny();
        if (webView != null) { webView.stopLoading(); webView.destroy(); }
        super.onDestroy();
    }
}
