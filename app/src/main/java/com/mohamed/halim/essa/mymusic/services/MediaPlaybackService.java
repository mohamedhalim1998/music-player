package com.mohamed.halim.essa.mymusic.services;

import android.app.Notification;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.media.MediaBrowserServiceCompat;

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

import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends Service implements ExoPlayer.EventListener {
    private static final String TAG = MediaPlaybackService.class.getSimpleName();

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private SimpleExoPlayer mExoPlayer;
    private ArrayList<AudioFile> mAudioFiles;
    private int mCurrentWindowIndex;
    private long mCurrentPosition;
    private boolean mCurrentState;
    private ConcatenatingMediaSource mConcatenatingMediaSource;

    class MyBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeMediaSession();
        mConcatenatingMediaSource = new ConcatenatingMediaSource();
        if (mAudioFiles != null) {
            createAllPlayList();
        }
    }

    /**
     * Initializes the Media Session to be enabled with media buttons, transport controls, callbacks
     * and media controller.
     */
    private void initializeMediaSession() {

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
        return new MyBinder();
    }

    /**
     * release the player if no longer needed
     */
    private void releasePlayer() {

        if (mExoPlayer != null) {
            mCurrentState = mExoPlayer.getPlayWhenReady();
            mCurrentPosition = mExoPlayer.getCurrentPosition();
            mCurrentWindowIndex = mExoPlayer.getCurrentWindowIndex();
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    /**
     * build a media source from uri
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

    private void createAllPlayList() {
        for (int i = 0; i < mAudioFiles.size(); i++) {
            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mAudioFiles.get(i).getId());
            MediaSource mediaSource = buildMediaSource(uri);
            mConcatenatingMediaSource.addMediaSource(mediaSource);
        }
        releasePlayer();
        initializePlayer();
        mExoPlayer.prepare(mConcatenatingMediaSource, false, false);
    }

    /**
     * initilize the player and make it ready to play
     */
    private void initializePlayer() {
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(this);
        // mExoPlayerView.setPlayer(mExoPlayer);
        mExoPlayer.setPlayWhenReady(mCurrentState);
        mExoPlayer.addListener(this);
        mExoPlayer.seekTo(mCurrentWindowIndex, mCurrentPosition);
        AudioFile file = mAudioFiles.get(mExoPlayer.getCurrentWindowIndex());
        Notification notification = NotificationHelper.showNotification(getApplicationContext(), stateBuilder.build(), mediaSession,
                file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
        startForeground(0, notification);
    }

    public void setAudioFiles(ArrayList<AudioFile> mAudioFiles) {
        this.mAudioFiles = mAudioFiles;
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

            if (mExoPlayer.hasPrevious() && mExoPlayer.getCurrentPosition() > 2000) {
                mExoPlayer.previous();
                AudioFile file = mAudioFiles.get(mExoPlayer.getCurrentWindowIndex());
                NotificationHelper.showNotification(getApplicationContext(), stateBuilder.build(), mediaSession,
                        file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
            } else {
                mExoPlayer.seekTo(0);
            }
        }

        @Override
        public void onSkipToNext() {
            if (mExoPlayer.hasNext()) {
                mExoPlayer.next();
                AudioFile file = mAudioFiles.get(mExoPlayer.getCurrentWindowIndex());
                NotificationHelper.showNotification(getApplicationContext(), stateBuilder.build(), mediaSession,
                        file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
            }
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if ((playbackState == ExoPlayer.STATE_READY) && playWhenReady) {
            stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    mExoPlayer.getCurrentPosition(), 1f);
        } else if ((playbackState == ExoPlayer.STATE_READY)) {
            stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    mExoPlayer.getCurrentPosition(), 1f);
        }
        mediaSession.setPlaybackState(stateBuilder.build());
        AudioFile file = mAudioFiles.get(mExoPlayer.getCurrentWindowIndex());
        NotificationHelper.showNotification(this, stateBuilder.build(), mediaSession,
                file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        AudioFile file = mAudioFiles.get(mExoPlayer.getCurrentWindowIndex());
        NotificationHelper.showNotification(this, stateBuilder.build(), mediaSession,
                file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
    }

    public SimpleExoPlayer getmExoPlayer() {
        return mExoPlayer;
    }
}
