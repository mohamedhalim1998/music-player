package com.mohamed.halim.essa.mymusic.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import com.mohamed.halim.essa.mymusic.R;
import com.mohamed.halim.essa.mymusic.ui.MainActivity;

public class NotificationHelper {

    public static final String NOTI_CH_ID = "track notification";


    public static void createTaskNotificationChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTI_CH_ID
                    , "Track Channel",
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

    /**
     * Shows Media Style notification, with an action that depends on the current MediaSession
     * PlaybackState.
     *
     * @param state        The PlaybackState of the MediaSession.
     * @param mediaSession
     */
    public static void showNotification(Context context, PlaybackStateCompat state, long id, MediaSessionCompat mediaSession) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.NOTI_CH_ID);
        int icon;
        String play_pause;
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            icon = R.drawable.exo_controls_pause;
            play_pause = "pause";
        } else {
            icon = R.drawable.exo_controls_play;
            play_pause = "play";
        }


        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                icon, play_pause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE));

        NotificationCompat.Action restartAction = new NotificationCompat
                .Action(R.drawable.exo_controls_previous, "restart",
                MediaButtonReceiver.buildMediaButtonPendingIntent
                        (context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        NotificationCompat.Action nextAction = new NotificationCompat
                .Action(R.drawable.exo_controls_next, "next",
                MediaButtonReceiver.buildMediaButtonPendingIntent
                        (context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

        PendingIntent contentPendingIntent = PendingIntent.getActivity
                (context, 0, new Intent(context, MainActivity.class), 0);
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        Cursor audioCursor = context.getContentResolver().query(uri,
                null, null, null, null);
        String title = "title";
        String artist = "Artist";
        String album = "album";
        long albumID;
        if (audioCursor != null && audioCursor.getCount() > 0) {
            audioCursor.moveToFirst();
            int titleColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int albumIdColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            title = audioCursor.getString(titleColumn);
            artist = audioCursor.getString(artistColumn);
            album = audioCursor.getString(albumColumn);
            albumID = audioCursor.getLong(albumIdColumn);
        }
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.music_art_place_holder);
        builder.setContentTitle(title)
                .setContentText(artist + " - " + album)
                .setContentIntent(contentPendingIntent)
                .setSmallIcon(R.drawable.music_art_place_holder)
                .setLargeIcon(largeIcon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(restartAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .setAutoCancel(false)
                .setOngoing(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1));
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(0, builder.build());
    }


}