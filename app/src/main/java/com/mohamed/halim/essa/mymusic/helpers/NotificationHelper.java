package com.mohamed.halim.essa.mymusic.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mohamed.halim.essa.mymusic.R;

public class NotificationHelper {

    public static final String NOTI_CH_ID = "track notification";


    public static void createTaskNotificationChannel(Context c){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(NOTI_CH_ID
                    ,"Track Channel",
                    NotificationManager.IMPORTANCE_HIGH);
            AudioAttributes attributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build();
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            channel.setSound(sound, attributes);

            channel.enableVibration(true);
            NotificationManager manager = c.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }



}
