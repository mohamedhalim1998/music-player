package com.mohamed.halim.essa.mymusic.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ListView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.mohamed.halim.essa.mymusic.data.AudioFile;
import com.mohamed.halim.essa.mymusic.R;
import com.mohamed.halim.essa.mymusic.adapters.AudioAdapter;
import com.mohamed.halim.essa.mymusic.helpers.NotificationHelper;
import com.mohamed.halim.essa.mymusic.services.MediaPlaybackService;

import org.parceler.Parcels;

import java.util.ArrayList;

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
    // list of all the audio files
    private ArrayList<AudioFile> mAudioFiles;
    // instance of a MediaPlaybackService
    MediaPlaybackService mMediaPlaybackService;
    // connection to the service
    private ServiceConnection myServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mMediaPlaybackService = ((MediaPlaybackService.MyBinder) binder).getService();
            Log.d(TAG, "onServiceConnected: service initialize");
            mExoPlayerView.setPlayer(mMediaPlaybackService.getExoPlayer());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

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
        // create a notification channel
        NotificationHelper.createTaskNotificationChannel(this);
        // check the permission for sdk > M
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // if the permission granted read the data
                LoaderManager.getInstance(this).initLoader(LOAD_FILES_LOADER_ID, null, this);
            } else {
                // else ask for the permission
                getPermission();
            }
        }
        // start playing if a track clicked
        mAudioListView.setOnItemClickListener((parent, view, position, id) -> mMediaPlaybackService.setCurrentWindowIndex(position));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
        unbindService(myServiceConnection);
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

    /*---------------------- Loader to get the media files -------------------*/
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // array of the column to get
        String[] projection;
        projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DURATION
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
        // add the cursor to the list adapter
        mAdapter.swapCursor(audioCursor);
        // add the files to the list
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
        // start the media service
        Intent i = new Intent(this, MediaPlaybackService.class);
        // send the files to the service
        Parcelable parcelable = Parcels.wrap(mAudioFiles);
        i.putExtra("MediaFiles", parcelable);
        bindService(i, myServiceConnection, BIND_AUTO_CREATE);
        //  startService(i);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

}