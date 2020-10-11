package com.example.chamber10;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxFile;

import com.bevelcloud.sdk.api.NomicApi;
import com.bevelcloud.sdk.model.Nomic;


public class ChamberService extends Service {

    public static final String CHANNEL_ID = "ChamberServiceChannel";
    public static final int NOTIFICATION_ID = 20;
    public static final int SYNC_FREQUENCY = 30 * 60 * 1000;  // 30 minutes

    private final IBinder serviceBinder = new ChamberServiceBinder();
    private Timer mTimer = null;

    private BoxAPIConnection box_api;
    private BoxFolder chamber_folder;
    private boolean is_logged_in = false;

    public class ChamberServiceBinder extends Binder {
        ChamberService getService(){
            return ChamberService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // schedule on-going file upload
        if (mTimer != null) mTimer.cancel();
        else mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new SyncTimerTask(), 0, SYNC_FREQUENCY);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void publishNotification(){
        Notification notification = getNotification();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null)
            mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Chamber Service Foreground Channel",
                NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null)
                manager.createNotificationChannel(serviceChannel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private Notification getNotification(){
        Intent intent = new Intent(this, MainActivity.class);
        String status = "Running";

        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_skylight_notification)
            .setColor(getColor(R.color.colorPrimary))
            .setContentIntent(contentIntent)
            .setSound(null)
            .build();
    }

    private void developerLogin(){
        box_api = new BoxAPIConnection("xTp8DLPY7W57oTEqC58QMEmlySZH7r3j");
        is_logged_in = true;
    }

    private void boxLogin(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    box_api = new BoxAPIConnection("xTp8DLPY7W57oTEqC58QMEmlySZH7r3j");
                    BoxFolder rootFolder = BoxFolder.getRootFolder(box_api);
                    String chamberFolderID = "";
                    for (BoxItem.Info itemInfo : rootFolder) {
                        //System.out.format("[%s] %s\n", itemInfo.getID(), itemInfo.getName());
                        if (itemInfo.getName().equals("CHAMBER")) {
                            chamberFolderID = itemInfo.getID();
                        }
                    }
                    chamber_folder = new BoxFolder(box_api, chamberFolderID);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void uploadFile(final File file, final String filename){
        if (!is_logged_in) {
            boxLogin();
            if (!is_logged_in) {
                System.out.println("Unable to login to Box");
                return;
            }
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    FileInputStream stream = new FileInputStream(file);
                    BoxFile.Info newFileInfo = chamber_folder.uploadFile(stream, filename);
                    stream.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void receiveFilesAndUpload(){
        NomicApi nomicApi = new NomicApi();
        List<Nomic> nomicData;
        try {
            nomicData = nomicApi.getNomic();
            for (int i=0; i<nomicData.size(); i++){
                Nomic nomicItem = nomicData.get(i);

                // do something with the data
                System.out.println(nomicItem.getFileName());
                System.out.println(nomicItem.getFilePath());
                System.out.println(nomicItem.getCreatedOn());

                File nomicFile = new File(nomicItem.getFilePath());
                uploadFile(nomicFile, nomicItem.getFileName());
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void manualRefresh() {
        try {
            receiveFilesAndUpload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void uploadTestImage(){
        if (!is_logged_in) {
            boxLogin();
            if (!is_logged_in) {
                System.out.println("Unable to login to Box");
                return;
            }
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    AssetManager assetManager = getAssets();
                    AssetFileDescriptor fileDescriptor = assetManager.openFd("test.jpg");
                    FileInputStream stream = fileDescriptor.createInputStream();
                    BoxFile.Info newFileInfo = chamber_folder.uploadFile(stream, "test.jpg");
                    stream.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void changeLogin(){
        System.out.println("Implement!");
    }

    class SyncTimerTask extends TimerTask {
        @Override
        public void run() {
            try {
                receiveFilesAndUpload();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
