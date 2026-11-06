package br.com.luiz.navegadorlivre;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.RenderProcessGoneDetail;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView web;
    private static final String HOME = "https://contactokakashigamesofc24-dotcom.github.io/ia-de-luiz-fernando/navegador-livre/";
    private boolean showingFallback = false;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.rgb(12,16,32));
        getWindow().setNavigationBarColor(Color.rgb(12,16,32));
        openHome();
    }

    private void openHome() {
        showingFallback = false;
        try {
            if (web == null) web = new WebView(this);
            web.setBackgroundColor(Color.rgb(12,16,32));
            WebSettings settings = web.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setSupportMultipleWindows(false);
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            web.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return handleNavigation(request.getUrl());
                }
                @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return handleNavigation(Uri.parse(url));
                }
                @Override public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                    if (request.isForMainFrame()) showFallback("Não consegui carregar a página. Confira a internet e tente novamente.");
                }
                @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    showFallback("O navegador do Android reiniciou. Toque em Tentar novamente.");
                    return true;
                }
            });
            web.setWebChromeClient(new WebChromeClient());
            setContentView(web);
            web.loadUrl(HOME);
        } catch (Throwable t) {
            showFallback("Não consegui iniciar o componente WebView do Android. Atualize o Android System WebView/Chrome e tente novamente.");
        }
    }

    private boolean handleNavigation(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        String host = uri.getHost() == null ? "" : uri.getHost();
        if ("https".equalsIgnoreCase(scheme) && (host.equals("contactokakashigamesofc24-dotcom.github.io") || host.equals("accounts.google.com"))) return false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Não encontrei um app para abrir este link.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void showFallback(String message) {
        if (showingFallback || isFinishing()) return;
        showingFallback = true;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.rgb(12,16,32));
        TextView title = new TextView(this);
        title.setText("Navegador Livre");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        TextView body = new TextView(this);
        body.setText(message);
        body.setTextColor(Color.LTGRAY);
        body.setTextSize(16);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, 20, 0, 20);
        Button retry = new Button(this);
        retry.setText("Tentar novamente");
        retry.setOnClickListener(v -> openHome());
        Button browser = new Button(this);
        browser.setText("Abrir no navegador do celular");
        browser.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HOME))); }
            catch (Exception e) { Toast.makeText(this, "Não consegui abrir o link.", Toast.LENGTH_SHORT).show(); }
        });
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        root.addView(body, new LinearLayout.LayoutParams(-1, -2));
        root.addView(retry, new LinearLayout.LayoutParams(-1, -2));
        root.addView(browser, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    @Override public void onBackPressed() {
        if (showingFallback) { super.onBackPressed(); return; }
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (web != null) { web.stopLoading(); web.destroy(); web = null; }
        super.onDestroy();
    }
}
