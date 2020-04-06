package com.mohamed.halim.essa.mymusic.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ViewSwitcher;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.mohamed.halim.essa.mymusic.data.AudioFile;
import com.mohamed.halim.essa.mymusic.R;
import com.mohamed.halim.essa.mymusic.adapters.AudioAdapter;
import com.mohamed.halim.essa.mymusic.helpers.NotificationHelper;
import com.mohamed.halim.essa.mymusic.services.MediaPlaybackService;

import org.parceler.Parcels;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, ExoPlayer.EventListener {
    // permission code
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 120;
    // key for podcast shared preference
    public static final String PODCAST_MODE_ENABLED_KEY = "podcast-mode-enabled";
    // log tag
    private static final String TAG = MainActivity.class.getSimpleName();
    //  loader id for loading the media files
    private static final int LOAD_FILES_LOADER_ID = 1001;
    // List adapter
    private AudioAdapter mAdapter;
    private ListView mAudioListView;
    private PlayerView mExoPlayerView;
    private PlayerView mExoPlayerPodcastView;
    private SearchView searchView;
    private ViewSwitcher viewSwitcher;
    // list of all the audio files
    private ArrayList<AudioFile> mAudioFiles;
    // instance of a MediaPlaybackService
    private MediaPlaybackService mMediaPlaybackService;
    private boolean mPodcastMode;
    // connection to the service
//    private TimerReceiver mTimerReceiver;
    private ServiceConnection myServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mMediaPlaybackService = ((MediaPlaybackService.MyBinder) binder).getService();
            Log.d(TAG, "onServiceConnected: service initialize");
            mExoPlayerView.setPlayer(mMediaPlaybackService.getExoPlayer());
            mExoPlayerPodcastView.setPlayer(mMediaPlaybackService.getExoPlayer());
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
//        mTimerReceiver = new TimerReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(Intent.ACTION_TIME_TICK);
//        registerReceiver(mTimerReceiver, intentFilter);
        mAdapter = new AudioAdapter(this, null);
        mAudioListView = findViewById(R.id.music_list);
        mExoPlayerView = findViewById(R.id.exo_player);
        mExoPlayerPodcastView = findViewById(R.id.exo_player_podcast);
        searchView = findViewById(R.id.track_search_bar);
        viewSwitcher = findViewById(R.id.mode_view_switcher);
        mExoPlayerView.showController();
        mExoPlayerPodcastView.showController();
        mAudioListView.setAdapter(mAdapter);
        // create a notification channel
        NotificationHelper.createTaskNotificationChannel(this);
        // save a podcast mode in sharedpreference
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.contains(PODCAST_MODE_ENABLED_KEY)) {
            sharedPreferences.edit().putBoolean(PODCAST_MODE_ENABLED_KEY, false).apply();
            mPodcastMode = false;
        } else {
            mPodcastMode = sharedPreferences.getBoolean(PODCAST_MODE_ENABLED_KEY, false);
        }
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
        mAudioListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mMediaPlaybackService.changeTrack(id);
            }
        });
        // create the search query
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (TextUtils.isEmpty(query)) {
                    mAdapter.swapList(mAudioFiles);
                } else {
                    mAdapter.getFilter().filter(query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (TextUtils.isEmpty(query)) {
                    mAdapter.swapList(mAudioFiles);
                } else {
                    mAdapter.getFilter().filter(query);
                }
                return false;
            }
        });
        if (mPodcastMode) {
            viewSwitcher.showNext();
        }
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.podcast_mode_action);
        int title = mPodcastMode ? R.string.podcast_mode_action_name_disable : R.string.podcast_mode_action_name_enable;
        item.setTitle(title);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.podcast_mode_action:
                viewSwitcher.showNext();
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                mPodcastMode = !mPodcastMode;
                sharedPreferences.edit().putBoolean(PODCAST_MODE_ENABLED_KEY, mPodcastMode).apply();
                int title = mPodcastMode ? R.string.podcast_mode_action_name_disable : R.string.podcast_mode_action_name_enable;
                item.setTitle(title);
                return true;
            case R.id.enable_timer_action:
//                long alarmTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
//                Log.e(TAG, "timer is on " + new SimpleDateFormat("HH:mm:ss").format(new Date(alarmTime)));
//                mMediaPlaybackService.setTimer(1);
                TimerDialog timerDialog = new TimerDialog(new TimerDialog.TimerDialogListener() {
                    @Override
                    public void onSetTime(int time) {
                        long alarmTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(time);
                        Log.e(TAG, "timer is on " + new SimpleDateFormat("HH:mm:ss").format(new Date(alarmTime)));
                        mMediaPlaybackService.setTimer(time);
                    }
                });
                timerDialog.show(getSupportFragmentManager(), "Timer Dialog");
                return true;
        }


        return super.onOptionsItemSelected(item);
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
        // add the files to the list
        int idColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media._ID);
        int titleColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumIdColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int dateAddedColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
        int durationColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        while (audioCursor.moveToNext()) {
            int id = audioCursor.getInt(idColumn);
            String title = audioCursor.getString(titleColumn);
            String artist = audioCursor.getString(artistColumn);
            String album = audioCursor.getString(albumColumn);
            int albumId = audioCursor.getInt(albumIdColumn);
            int dateAdded = audioCursor.getInt(dateAddedColumn);
            long duration = audioCursor.getLong(durationColumn);
            mAudioFiles.add(new AudioFile(id, title, artist, album, albumId, dateAdded, duration));
        }
        mAdapter.swapList(mAudioFiles);
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
        mAdapter.swapList(null);
    }

}