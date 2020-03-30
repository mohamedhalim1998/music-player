package com.mohamed.halim.essa.mymusic.services;

import android.app.Notification;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements ExoPlayer.EventListener, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = MediaPlaybackService.class.getSimpleName();
    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private SimpleExoPlayer mExoPlayer;
    private Context context;
    private ArrayList<AudioFile> mAudioFiles;
    private int mCurrentWindowIndex;
    private long mCurrentPosition;
    private boolean mCurrentState;
    private ConcatenatingMediaSource mConcatenatingMediaSource;


    @Override
    public void onCreate() {
        super.onCreate();
        mAudioFiles = new ArrayList<>();
        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(this, TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MySessionCallback());

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String s, int i, @Nullable Bundle bundle) {
        return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull Result<List<MediaItem>> result) {
      /*  //  Browsing not allowed
        if (TextUtils.equals(MY_EMPTY_MEDIA_ROOT_ID, s)) {
            result.sendResult(null);
            return;
        }

        // Assume for example that the music catalog is already loaded/cached.

        List<MediaItem> mediaItems = new ArrayList<>();

        // Check if this is the root menu:
        if (MY_MEDIA_ROOT_ID.equals(s)) {
            // Build the MediaItem objects for the top level,
            // and put them in the mediaItems list...
        } else {
            // Examine the passed parentMediaId to see which submenu we're at,
            // and put the children of that menu in the mediaItems list...
        }
        result.sendResult(mediaItems);*/
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
        Notification notification =   NotificationHelper.showNotification(context, stateBuilder.build(), mediaSession,
                file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
        startForeground(0, notification);
    }

    @NonNull
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
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor audioCursor) {
        int idColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media._ID);
        int titleColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumIdColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int dateAddedColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
        while (audioCursor.moveToNext()) {
            int id = audioCursor.getInt(idColumn);
            String title = audioCursor.getString(titleColumn);
            String artist = audioCursor.getString(artistColumn);
            String album = audioCursor.getString(albumColumn);
            int albumId = audioCursor.getInt(albumIdColumn);
            int dateAdded = audioCursor.getInt(dateAddedColumn);
            mAudioFiles.add(new AudioFile(id, title, artist, album, albumId, dateAdded));
        }
        //releasePlayer();
        createAllPlayList();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
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
                NotificationHelper.showNotification(context, stateBuilder.build(), mediaSession,
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
                NotificationHelper.showNotification(context, stateBuilder.build(), mediaSession,
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

}
