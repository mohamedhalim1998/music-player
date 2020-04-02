package com.mohamed.halim.essa.mymusic.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.media.session.MediaButtonReceiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.ListView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.mohamed.halim.essa.mymusic.data.AudioFile;
import com.mohamed.halim.essa.mymusic.R;
import com.mohamed.halim.essa.mymusic.adapters.AudioAdapter;
import com.mohamed.halim.essa.mymusic.helpers.NotificationHelper;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, ExoPlayer.EventListener {
    // permission code
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 120;
    // log tag
    private static final String TAG = MainActivity.class.getSimpleName();
    //  loader id for loading the media files
    private static final int LOAD_FILES_LOADER_ID = 1001;
    private static final String CURRENT_WINDOW_INDEX_KEY = "current-window-index";
    private static final String CURRENT_POSITION_KEY = "current-position";
    private static final String CURRENT_STATE_KEY = "current-state";
    // List adapter
    private AudioAdapter mAdapter;
    private ListView mAudioListView;
    private PlayerView mExoPlayerView;
    private SimpleExoPlayer mExoPlayer;
    private static MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private ConcatenatingMediaSource mConcatenatingMediaSource;
    private ArrayList<AudioFile> mAudioFiles;

    private int mCurrentWindowIndex;
    private long mCurrentPosition;
    private boolean mCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initialize views and adapter
        mAudioFiles = new ArrayList<>();
        mAdapter = new AudioAdapter(this, null);
        mAudioListView = findViewById(R.id.music_list);
        mExoPlayerView = findViewById(R.id.exo_player);
        mExoPlayerView.showController();
        mAudioListView.setAdapter(mAdapter);
        mConcatenatingMediaSource = new ConcatenatingMediaSource();
        mCurrentState = false;
        mCurrentPosition = 0;
        mCurrentWindowIndex = 0;
        if (savedInstanceState != null) {
            mCurrentState = savedInstanceState.getBoolean(CURRENT_STATE_KEY);
            mCurrentPosition = savedInstanceState.getLong(CURRENT_POSITION_KEY);
            mCurrentWindowIndex = savedInstanceState.getInt(CURRENT_WINDOW_INDEX_KEY);
        }
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
            mExoPlayer.seekTo(position, 0);
            mExoPlayer.setPlayWhenReady(true);
        });
    }

    /**
     * initilize the player and make it ready to play
     */
    private void initializePlayer() {
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(this);
        mExoPlayerView.setPlayer(mExoPlayer);
        mExoPlayer.setPlayWhenReady(mCurrentState);
        mExoPlayer.addListener(this);
        mExoPlayer.seekTo(mCurrentWindowIndex, mCurrentPosition);
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
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
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
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        mCurrentState = mExoPlayer.getPlayWhenReady();
        mCurrentPosition = mExoPlayer.getCurrentPosition();
        mCurrentWindowIndex = mExoPlayer.getCurrentWindowIndex();
        outState.putInt(CURRENT_WINDOW_INDEX_KEY, mCurrentWindowIndex);
        outState.putLong(CURRENT_POSITION_KEY, mCurrentPosition);
        outState.putBoolean(CURRENT_STATE_KEY, mCurrentState);
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
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // load the data if the permission granted
                LoaderManager.getInstance(this).initLoader(LOAD_FILES_LOADER_ID, null, this);

            } else {
                // ask again if not
                getPermission();
            }
        } else {
            Log.e(TAG, "onRequestPermissionsResult: no such request code" + requestCode);
        }
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
        mAdapter.swapCursor(audioCursor);
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
        mAdapter.swapCursor(null);
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
                NotificationHelper.showNotification(MainActivity.this, mStateBuilder.build(), mMediaSession,
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
                NotificationHelper.showNotification(MainActivity.this, mStateBuilder.build(), mMediaSession,
                        file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
            }
        }
    }

    public static class MediaReceiver extends BroadcastReceiver {

        public MediaReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MediaButtonReceiver.handleIntent(mMediaSession, intent);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if ((playbackState == ExoPlayer.STATE_READY) && playWhenReady) {
            mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    mExoPlayer.getCurrentPosition(), 1f);
        } else if ((playbackState == ExoPlayer.STATE_READY)) {
            mStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    mExoPlayer.getCurrentPosition(), 1f);
        }
        mMediaSession.setPlaybackState(mStateBuilder.build());
        AudioFile file = mAudioFiles.get(mExoPlayer.getCurrentWindowIndex());
        NotificationHelper.showNotification(this, mStateBuilder.build(), mMediaSession,
                file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        AudioFile file = mAudioFiles.get(mExoPlayer.getCurrentWindowIndex());
        NotificationHelper.showNotification(this, mStateBuilder.build(), mMediaSession,
                file.getTitle(), file.getArtist(), file.getAlbum(), file.getAlbumId());
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        mExoPlayer.setShuffleModeEnabled(shuffleModeEnabled);
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        mExoPlayer.setRepeatMode(repeatMode);
    }
}