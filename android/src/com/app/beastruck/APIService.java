package com.app.beastruck;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;
import com.app.beastruck.SocketRequest.NewDeviceRegister;
import org.jetbrains.annotations.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import androidx.core.app.NotificationCompat;

public class APIService extends Service {
    private static SharedPreferences sharedPreferences;
    private NewDeviceRegister _newDeviceRegister;
    private String _baseSocketUri="ws://api.kontrolapi.work/";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            _newDeviceRegister= new NewDeviceRegister(new URI(_baseSocketUri+"NewDeviceRegister"));
            String deviceId=Helper.AlCihazId(this);
            _newDeviceRegister.send(deviceId);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForeground();
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