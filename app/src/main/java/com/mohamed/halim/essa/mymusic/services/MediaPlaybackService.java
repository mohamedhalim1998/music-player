package com.mohamed.halim.essa.mymusic.services;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;
import androidx.preference.PreferenceManager;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.mohamed.halim.essa.mymusic.data.AudioFile;
import com.mohamed.halim.essa.mymusic.helpers.NotificationHelper;
import com.mohamed.halim.essa.mymusic.ui.MainActivity;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MediaPlaybackService extends Service implements ExoPlayer.EventListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = MediaPlaybackService.class.getSimpleName();
    // media state vars
    private static MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private SimpleExoPlayer mExoPlayer;
    private ArrayList<AudioFile> mAudioFiles;
    private int mCurrentWindowIndex;
    private long mCurrentPosition;
    private boolean mCurrentState;
    private ConcatenatingMediaSource mConcatenatingMediaSource;
    // keys for save the parameters on the exo player
    public static final String CURRENT_WINDOW_INDEX_KEY = "current-window-index";
    public static final String CURRENT_POSITION_KEY = "current-position";
    public static final String CURRENT_STATE_KEY = "current-state";
    private TimerTask timerTask;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeMediaSession();
        // create a ConcatenatingMediaSource to play
        mConcatenatingMediaSource = new ConcatenatingMediaSource();
        //restore the state of the player
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentWindowIndex = sharedPreferences.getInt(CURRENT_WINDOW_INDEX_KEY, 0);
        mCurrentPosition = sharedPreferences.getLong(CURRENT_POSITION_KEY, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        //   updateNotification();
        return START_NOT_STICKY;
    }

    /**
     * Initializes the Media Session to be enabled with media buttons, transport controls, callbacks
     * and media controller.
     */
    private void initializeMediaSession() {
        Log.d(TAG, "initializeMediaSession: from service");
        // Create a MediaSessionCompat.
        mediaSession = new MediaSessionCompat(this, TAG);
        // Do not let MediaButtons restart the player when the app is not visible.
        mediaSession.setMediaButtonReceiver(null);
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player.
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_FAST_FORWARD |
                                PlaybackStateCompat.ACTION_REWIND |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);

        mediaSession.setPlaybackState(stateBuilder.build());
        // MySessionCallback has methods that handle callbacks from a media controller.
        mediaSession.setCallback(new MySessionCallback());
        // Start the Media Session since the activity is active.
        mediaSession.setActive(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mAudioFiles = Parcels.unwrap(intent.getParcelableExtra("MediaFiles"));
        createAllPlayList();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        startService(intent);
        return new MyBinder();
    }

    /**
     * build a media source from uri using
     * DefaultDataSourceFactory
     *
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
     * Create a play list from all the tracks
     */
    private void createAllPlayList() {
        Log.d(TAG, "createAllPlayList: from service");

        for (int i = 0; i < mAudioFiles.size(); i++) {
            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mAudioFiles.get(i).getId());
            MediaSource mediaSource = buildMediaSource(uri);
            mConcatenatingMediaSource.addMediaSource(mediaSource);
        }
        if (mExoPlayer != null)
            releasePlayer();
        initializePlayer();
        mExoPlayer.prepare(mConcatenatingMediaSource, false, false);
    }

    /**
     * initialize the player and make it ready to play
     */
    private void initializePlayer() {
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(this);
        mExoPlayer.setPlayWhenReady(mCurrentState);
        // add an event listener
        mExoPlayer.addListener(this);
        // set the track to play
        mExoPlayer.seekTo(mCurrentWindowIndex, mCurrentPosition);
        updateNotification();
        Log.d(TAG, "initializePlayer: play from service");
    }

    private void updateNotification() {

        AudioFile file = mAudioFiles.get(mExoPlayer.getCurrentWindowIndex());
        // show the notification media control
        boolean podcastMode = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(MainActivity.PODCAST_MODE_ENABLED_KEY, false);

        Notification notification = !podcastMode ? NotificationHelper.showNotification(getApplicationContext(), stateBuilder.build(), mediaSession,
                file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId()) :
                NotificationHelper.showPodcastNotification(getApplicationContext(), stateBuilder.build(), mediaSession,
                        file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
        startForeground(1105, notification);
    }

    /**
     * release the player if no longer needed
     */
    private void releasePlayer() {
        if (mExoPlayer != null) {
            Log.e(TAG, "releasePlayer");
            updatePreference();
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    /**
     * update the player state in the preference
     */
    private void updatePreference() {
        Log.e(TAG, "updatePreference ");
        mCurrentState = mExoPlayer.getPlayWhenReady();
        mCurrentPosition = mExoPlayer.getCurrentPosition();
        mCurrentWindowIndex = mExoPlayer.getCurrentWindowIndex();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putLong(CURRENT_POSITION_KEY, mCurrentPosition)
                .putInt(CURRENT_WINDOW_INDEX_KEY, mCurrentWindowIndex).apply();
    }

    /**
     * update the playing track
     *
     * @param id : of the desired track
     */
    public void changeTrack(long id) {
        for (int i = 0; i < mAudioFiles.size(); i++) {
            if (mAudioFiles.get(i).getId() == id) {
                setCurrentWindowIndex(i);
            }
        }
        mExoPlayer.setPlayWhenReady(true);
    }

    public void setTimer(int min) {
        Timer timer = new Timer();
        if (timerTask != null) {
            timerTask.cancel();
        }
        timerTask = new TimerTask() {
            @Override
            public void run() {
                mExoPlayer.setPlayWhenReady(false);
                stopForeground(true);
                updatePreference();
                Log.e(TAG, "timer up");
            }

            @Override
            public boolean cancel() {
                Log.e(TAG, "cancel: " + min);
                return super.cancel();
            }
        };
        timer.schedule(timerTask, TimeUnit.MINUTES.toMillis(min));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    /* ---------------------- Getter and Setter for field ---------------*/

    /**
     * @return exo player object
     */
    public SimpleExoPlayer getExoPlayer() {
        return mExoPlayer;
    }

    public int getCurrentWindowIndex() {
        return mCurrentWindowIndex;
    }

    public void setCurrentWindowIndex(int currentWindowIndex) {
        this.mCurrentWindowIndex = currentWindowIndex;
        mCurrentState = true;
        mExoPlayer.seekTo(mCurrentWindowIndex, 0);
        mExoPlayer.setPlayWhenReady(true);
    }

    public long getCurrentPosition() {
        return mCurrentPosition;
    }

    public void setCurrentPosition(long currentPosition) {
        this.mCurrentPosition = currentPosition;
        mExoPlayer.seekTo(mCurrentWindowIndex, mCurrentPosition);

    }

    public boolean isCurrentState() {
        return mCurrentState;
    }

    public void setCurrentState(boolean currentState) {
        this.mCurrentState = currentState;
    }

    /* ---------------------- ExoPlayer.EventListener -------------------*/

    /**
     * deal with player state change from play to pause and vice versa
     * update the notification
     *
     * @param playWhenReady : play pause state
     * @param playbackState : player state
     */
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if ((playbackState == ExoPlayer.STATE_READY) && playWhenReady) {
            stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    mExoPlayer.getCurrentPosition(), 1f);
        } else if ((playbackState == ExoPlayer.STATE_READY)) {
            stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    mExoPlayer.getCurrentPosition(), 1f);
            Log.d(TAG, "stop foreground");
        }
        mediaSession.setPlaybackState(stateBuilder.build());
        updateNotification();
        if (!playWhenReady) {
            stopForeground(false);
            updatePreference();
        }
    }

    /**
     * deal with the player change tracks
     * update the notification
     *
     * @param reason : of the change
     */
    @Override
    public void onPositionDiscontinuity(int reason) {
        updateNotification();
    }


    /**
     * deal with the change in shuffle mode
     *
     * @param shuffleModeEnabled : the state of the shuffle mode
     */
    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        mExoPlayer.setShuffleModeEnabled(shuffleModeEnabled);
    }

    /**
     * deal with the change in repeat mode
     *
     * @param repeatMode : the state of the repeat mode
     */
    @Override
    public void onRepeatModeChanged(int repeatMode) {
        mExoPlayer.setRepeatMode(repeatMode);
    }

    /* ------------------- shared preference change --------------------*/
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(MainActivity.PODCAST_MODE_ENABLED_KEY)) {
            updateNotification();
        }
        Log.e(TAG, "onSharedPreferenceChanged");
    }

    /* ----------------- inner classes ------------------*/
    // inner class for MediaSession callback
    private class MySessionCallback extends MediaSessionCompat.Callback {
        /**
         * start the playback on play pressed
         */
        @Override
        public void onPlay() {
            mCurrentState = true;
            mExoPlayer.setPlayWhenReady(true);
        }

        /**
         * pause the playback on pause pressed
         */
        @Override
        public void onPause() {
            mCurrentState = false;
            mExoPlayer.setPlayWhenReady(false);
            stopForeground(false);
            updatePreference();
            Log.d(TAG, "stop foreground");
        }

        /**
         * skip to the prev track if the current track exceeds 2 s
         * else reset the track
         * update the notification
         */
        @Override
        public void onSkipToPrevious() {
            if (mExoPlayer.hasPrevious() && mExoPlayer.getCurrentPosition() < 2000) {
                mExoPlayer.previous();
                updateNotification();
            } else {
                mExoPlayer.seekTo(0);
            }
        }

        /**
         * skip to the next track
         * update the notification
         */
        @Override
        public void onSkipToNext() {
            if (mExoPlayer.hasNext()) {
                mExoPlayer.next();
                updateNotification();
            }
        }

        @Override
        public void onRewind() {
            mExoPlayer.seekTo(mExoPlayer.getCurrentPosition() - 15000);
        }

        @Override
        public void onFastForward() {
            mExoPlayer.seekTo(mExoPlayer.getCurrentPosition() + 15000);
        }
    }

    // inner class for broad cast handle notification action
    public static class MediaReceiver extends BroadcastReceiver {

        public MediaReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MediaButtonReceiver.handleIntent(mediaSession, intent);
        }
    }

    // inner class binder to bind the service to the activity
    public class MyBinder extends Binder {
        /**
         * @return instance of the service
         */
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }

    }
}
