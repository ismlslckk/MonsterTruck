package com.app.beastruck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.RequiresApi;

public class MainReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(context);
        } else {
            startService(context);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startForeground(Context context) {
        context.startForegroundService(new Intent(context, APIService.class));
    }

    private void startService(Context context) {
        context.startService(new Intent(context, APIService.class));
    }
}