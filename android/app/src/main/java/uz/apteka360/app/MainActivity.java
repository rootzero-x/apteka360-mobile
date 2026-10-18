package uz.apteka360.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.activity.OnBackPressedCallback;

import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;
import com.getcapacitor.WebViewListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Apteka 360 — WebView qobig'i (https://apteka.agrom24.uz).
 *
 *  - Orqaga tugmasi WebView tarixida yuradi, tarix tugaganda ilova orqa fonga o'tadi.
 *  - Internet yo'q bo'lsa Capacitor `server.errorPath` (www/error.html) ko'rsatiladi;
 *    tarmoq qaytganda ilova o'zi bosh sahifani qayta yuklaydi.
 *  - Asosiy (main-frame) navigatsiyalarga X-Client sarlavhasi qo'shiladi.
 *  - Tashqi domenlar Capacitor tomonidan tizim brauzerida ochiladi (Bridge.launchIntent).
 */
public class MainActivity extends BridgeActivity {

    /** API hujjatidagi format: X-Client: mobile/<versiya>. */
    public static final String X_CLIENT = "mobile/" + BuildConfig.VERSION_NAME;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private volatile boolean showingErrorPage = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bridge bridge = getBridge();
        final WebView webView = bridge.getWebView();

        // 1) Orqaga tugmasi: WebView tarixi, so'ng orqa fon (ilova yopilmaydi).
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!showingErrorPage && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    moveTaskToBack(true);
                }
            }
        });

        // 2) Xato sahifasi holatini kuzatish.
        bridge.addWebViewListener(new WebViewListener() {
            @Override
            public void onReceivedError(WebView view) {
                showingErrorPage = true;
            }

            @Override
            public void onPageLoaded(WebView view) {
                String url = view.getUrl();
                showingErrorPage = url != null && url.endsWith("/error.html");
            }
        });

        // 3) Foydalanuvchi bosgan havolalar uchun X-Client sarlavhasi.
        bridge.setWebViewClient(new BridgeWebViewClient(bridge) {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (super.shouldOverrideUrlLoading(view, request)) {
                    return true; // Capacitor tashqi havolani brauzerga yubordi
                }
                if (request.isForMainFrame()
                        && "GET".equalsIgnoreCase(request.getMethod())
                        && isAppHost(request.getUrl().getHost())
                        && !request.getRequestHeaders().containsKey("X-Client")) {
                    view.loadUrl(request.getUrl().toString(), clientHeaders());
                    return true;
                }
                return false;
            }
        });

        // 4) Boshlang'ich yuklashni sarlavha bilan qaytaramiz.
        String appUrl = bridge.getAppUrl();
        if (appUrl != null) {
            webView.loadUrl(appUrl, clientHeaders());
        }

        // 5) Tarmoq qaytganda avtomatik qayta yuklash.
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (showingErrorPage) {
                        showingErrorPage = false;
                        webView.loadUrl(bridge.getAppUrl(), clientHeaders());
                    }
                });
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        if (connectivityManager != null && networkCallback != null) {
            NetworkRequest req = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(req, networkCallback);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {
                // allaqachon o'chirilgan
            }
        }
    }

    private boolean isAppHost(String host) {
        if (host == null) return false;
        String appUrl = getBridge().getAppUrl();
        return appUrl != null && host.equalsIgnoreCase(android.net.Uri.parse(appUrl).getHost());
    }

    private static Map<String, String> clientHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("X-Client", X_CLIENT);
        h.put("X-Client-Device", Build.MANUFACTURER + " " + Build.MODEL + " · Apteka 360");
        return h;
    }
}
