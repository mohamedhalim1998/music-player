package com.mohamed.halim.essa.mymusic.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.media.session.MediaButtonReceiver;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.ListView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.mohamed.halim.essa.mymusic.R;
import com.mohamed.halim.essa.mymusic.adapters.AudioAdapter;
import com.mohamed.halim.essa.mymusic.helpers.NotificationHelper;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, ExoPlayer.EventListener {
    // permission code
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 120;
    // log tag
    private static final String TAG = MainActivity.class.getSimpleName();
    //  loader id for loading the media files
    private static final int LOAD_FILES_LOADER_ID = 1001;
    // List adapter
    private AudioAdapter mAdapter;
    private ListView mAudioListView;
    private PlayerView mExoPlayerView;
    private SimpleExoPlayer mExoPlayer;
    private static MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initialize views and adapter
        mAdapter = new AudioAdapter(this, null);
        mAudioListView = findViewById(R.id.music_list);
        mExoPlayerView = findViewById(R.id.exo_player);
        mAudioListView.setAdapter(mAdapter);
        // create a nofification channel
        NotificationHelper.createTaskNotificationChannel(this);

        initializeMediaSession();
        // check the permission for sdk > M
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // if the permission granted read the data
                LoaderManager.getInstance(this).initLoader(LOAD_FILES_LOADER_ID, null, this);

                Log.d(TAG, "onCreate: get the list");
            } else {
                // else ask for the permission
                getPermission();
            }
        }
        // start playing if a track clicked
        mAudioListView.setOnItemClickListener((parent, view, position, id) -> {
            releasePlayer();
            initializePlayer(id);
            showNotification(mStateBuilder.build());
        });
    }

    /**
     * initilize the player and make it ready to play
     * @param id : of the track
     */
    private void initializePlayer(long id) {
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(this);
        mExoPlayerView.setPlayer(mExoPlayer);
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        MediaSource mediaSource = buildMediaSource(uri);
        mExoPlayer.setPlayWhenReady(true);
        mExoPlayer.addListener(this);
        mExoPlayer.prepare(mediaSource, false, false);
    }

    /**
     * build a media source from uri
     * @param uri : to build a media source from
     * @return : media source
     */
    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()));
        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);
    }

    /**
     * release the player if no longer needed
     */
    private void releasePlayer() {
        if (mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    /**
     * Initializes the Media Session to be enabled with media buttons, transport controls, callbacks
     * and media controller.
     */
    private void initializeMediaSession() {

        // Create a MediaSessionCompat.
        mMediaSession = new MediaSessionCompat(this, TAG);
        // Do not let MediaButtons restart the player when the app is not visible.
        mMediaSession.setMediaButtonReceiver(null);
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player.
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);

        mMediaSession.setPlaybackState(mStateBuilder.build());


        // MySessionCallback has methods that handle callbacks from a media controller.
        mMediaSession.setCallback(new MySessionCallback());

        // Start the Media Session since the activity is active.
        mMediaSession.setActive(true);

    }


    /**
     * ask for the read external storage permission
     */
    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // show request permission dialog
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_EXTERNAL_STORAGE_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    /**
     * handle the result of the permission request
     *
     * @param requestCode  : the permission request code
     * @param permissions  : the list of permission for the request code
     * @param grantResults : the result for every permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // load the data if the permission granted
                    LoaderManager.getInstance(this).initLoader(LOAD_FILES_LOADER_ID, null, this);

                } else {
                    // ask again if not
                    getPermission();
                }
                break;
            default:
                Log.e(TAG, "onRequestPermissionsResult: no such request code" + requestCode);

        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection;
        projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATE_ADDED
        };

        return new CursorLoader(this,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
        cursor.moveToFirst();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if ((playbackState == ExoPlayer.STATE_READY) && playWhenReady) {
            mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    mExoPlayer.getCurrentPosition(), 1f);
        } else if ((playbackState == ExoPlayer.STATE_READY)) {
            Log.d(TAG, "onPlayerStateChanged: PAUSED");
            mStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    mExoPlayer.getCurrentPosition(), 1f);
        }
        mMediaSession.setPlaybackState(mStateBuilder.build());
        showNotification(mStateBuilder.build());
    }

    private class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            mExoPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            mExoPlayer.setPlayWhenReady(false);
        }

        @Override
        public void onSkipToPrevious() {
            mExoPlayer.seekTo(0);
        }
    }

    /**
     * Shows Media Style notification, with an action that depends on the current MediaSession
     * PlaybackState.
     *
     * @param state The PlaybackState of the MediaSession.
     */
    private void showNotification(PlaybackStateCompat state) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationHelper.NOTI_CH_ID);
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
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE));

        NotificationCompat.Action restartAction = new NotificationCompat
                .Action(R.drawable.exo_controls_previous, "restart",
                MediaButtonReceiver.buildMediaButtonPendingIntent
                        (this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        PendingIntent contentPendingIntent = PendingIntent.getActivity
                (this, 0, new Intent(this, MainActivity.class), 0);

        builder.setContentTitle("test")
                .setContentText("test")
                .setContentIntent(contentPendingIntent)
                .setSmallIcon(R.drawable.music_art_place_holder)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(restartAction)
                .addAction(playPauseAction)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1));


        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, builder.build());
    }

    public static class MediaReceiver extends BroadcastReceiver {

        public MediaReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MediaButtonReceiver.handleIntent(mMediaSession, intent);
        }
    }
}