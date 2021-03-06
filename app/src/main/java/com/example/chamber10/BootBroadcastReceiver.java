package com.example.chamber10;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            Intent serviceIntent = new Intent(context, ChamberService.class);
            if (Build.VERSION.SDK_INT > 26){
                context.startForegroundService(serviceIntent);
            }else{
                context.startService(serviceIntent);
            }
        }
    }
}
