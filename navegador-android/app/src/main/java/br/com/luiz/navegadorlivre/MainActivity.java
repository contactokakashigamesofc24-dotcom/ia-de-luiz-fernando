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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    private static final String HOME = "https://contactokakashigamesofc24-dotcom.github.io/ia-de-luiz-fernando/navegador-livre/";
    private static final String WEB_CLIENT_ID = "876262009316-83gira3u67leg6i63bpe4nmnumuori6s.apps.googleusercontent.com";
    private static final String TAG = "NavegadorLivre";
    private boolean showingFallback = false;
    private CredentialManager credentialManager;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.rgb(12,16,32));
        getWindow().setNavigationBarColor(Color.rgb(12,16,32));
        credentialManager = CredentialManager.create(this);
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
            settings.setUserAgentString(settings.getUserAgentString() + " NavegadorLivreAndroid");
            settings.setSupportMultipleWindows(false);
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            web.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Uri uri = request.getUrl();
                    if (request.isForMainFrame() && "navegadorlivre".equals(uri.getScheme())) {
                        if ("google-signin".equals(uri.getHost())) startNativeGoogleSignIn();
                        else if ("google-signout".equals(uri.getHost())) clearGoogleCredentialState();
                        return true;
                    }
                    return handleNavigation(uri);
                }
                @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Uri uri = Uri.parse(url);
                    if ("navegadorlivre".equals(uri.getScheme())) {
                        if ("google-signin".equals(uri.getHost())) startNativeGoogleSignIn();
                        else if ("google-signout".equals(uri.getHost())) clearGoogleCredentialState();
                        return true;
                    }
                    return handleNavigation(uri);
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
            Log.e(TAG, "WebView startup failure", t);
            showFallback("Não consegui iniciar o componente WebView do Android. Atualize o Android System WebView/Chrome e tente novamente.");
        }
    }

    private void startNativeGoogleSignIn() {
        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setServerClientId(WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();
        credentialManager.getCredentialAsync(this, request, null, getMainExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential &&
                                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            try {
                                GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
                                JSONObject profile = new JSONObject();
                                profile.put("name", google.getDisplayName() == null ? "" : google.getDisplayName());
                                profile.put("given_name", google.getGivenName() == null ? "" : google.getGivenName());
                                profile.put("family_name", google.getFamilyName() == null ? "" : google.getFamilyName());
                                profile.put("email", google.getId() == null ? "" : google.getId());
                                profile.put("picture", google.getProfilePictureUri() == null ? "" : google.getProfilePictureUri().toString());
                                if (web != null) web.evaluateJavascript("window.dispatchEvent(new CustomEvent('navegador-google-profile',{detail:" + profile.toString() + "}));", null);
                            } catch (GoogleIdTokenParsingException | org.json.JSONException e) {
                                Log.e(TAG, "Could not parse Google credential", e);
                                Toast.makeText(MainActivity.this, "O Google retornou um perfil que não consegui ler.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "O Google retornou uma credencial não reconhecida.", Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override public void onError(GetCredentialException e) {
                        Log.e(TAG, "Google sign-in failed", e);
                        Toast.makeText(MainActivity.this, "Não foi possível entrar com Google: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void clearGoogleCredentialState() {
        try {
            credentialManager.clearCredentialStateAsync(
                    new androidx.credentials.ClearCredentialStateRequest(), null, getMainExecutor(),
                    new CredentialManagerCallback<Void, androidx.credentials.exceptions.ClearCredentialException>() {
                        @Override public void onResult(Void result) {}
                        @Override public void onError(androidx.credentials.exceptions.ClearCredentialException e) {
                            Log.w(TAG, "Could not clear credential state", e);
                        }
                    });
        } catch (Exception e) { Log.w(TAG, "Credential state clear unavailable", e); }
    }

    private boolean handleNavigation(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        String host = uri.getHost() == null ? "" : uri.getHost();
        if ("https".equalsIgnoreCase(scheme) && (host.equals("contactokakashigamesofc24-dotcom.github.io") || host.equals("accounts.google.com"))) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (Exception e) { Toast.makeText(this, "Não encontrei um app para abrir este link.", Toast.LENGTH_SHORT).show(); }
        return true;
    }

    private void showFallback(String message) {
        if (showingFallback || isFinishing()) return;
        showingFallback = true;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(32,32,32,32); root.setBackgroundColor(Color.rgb(12,16,32));
        TextView title = new TextView(this); title.setText("Navegador Livre"); title.setTextColor(Color.WHITE); title.setTextSize(24); title.setGravity(Gravity.CENTER);
        TextView body = new TextView(this); body.setText(message); body.setTextColor(Color.LTGRAY); body.setTextSize(16); body.setGravity(Gravity.CENTER); body.setPadding(0,20,0,20);
        Button retry = new Button(this); retry.setText("Tentar novamente"); retry.setOnClickListener(v -> openHome());
        Button browser = new Button(this); browser.setText("Abrir no navegador do celular"); browser.setOnClickListener(v -> { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HOME))); } catch (Exception e) { Toast.makeText(this,"Não consegui abrir o link.",Toast.LENGTH_SHORT).show(); }});
        root.addView(title,new LinearLayout.LayoutParams(-1,-2)); root.addView(body,new LinearLayout.LayoutParams(-1,-2)); root.addView(retry,new LinearLayout.LayoutParams(-1,-2)); root.addView(browser,new LinearLayout.LayoutParams(-1,-2)); setContentView(root);
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
