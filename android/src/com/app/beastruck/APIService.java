package com.app.beastruck;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

import androidx.core.app.NotificationCompat;
import io.socket.client.IO;
import io.socket.client.Socket;

public class APIService extends Service {
    private static SharedPreferences sharedPreferences;
    private String appNameVal;
    private boolean showAd = false;
    private boolean adShowed = false;
    private WebSocketClient mWebSocketClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connectWebSocket();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForeground();
    }

    private void connectWebSocket() {

        URI uri;
        try {
            uri = new URI("ws://api.kontrolapi.work/socket");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                String e="";
            }

            @Override
            public void onMessage(final String s) {
                String e="";
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                System.out.println("socket closed");
            }

            @Override
            public void onError(Exception e) {
                System.out.println("socket error");
            }
        };
        mWebSocketClient.connect();
    }

    private void startForeground() {
        String NOTIFICATION_CHANNEL_ID = getApplicationContext().getPackageName();
        String channelName = getResources().getString(R.string.app_name);
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID);
            builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID);
        }
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_notif);
        builder.setContent(remoteViews);
        builder.setSmallIcon(R.drawable.notification_action_background);
        builder.setPriority(NotificationCompat.PRIORITY_LOW);
        startForeground(1, builder.build());
    }
}