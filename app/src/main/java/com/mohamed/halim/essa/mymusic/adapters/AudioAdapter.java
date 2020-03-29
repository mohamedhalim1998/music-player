package com.mohamed.halim.essa.mymusic.adapters;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mohamed.halim.essa.mymusic.R;

public class AudioAdapter extends BaseAdapter {
    private Context context;
    private Cursor audioCursor;
    private static final String TAG = AudioAdapter.class.getSimpleName();

    public AudioAdapter(Context context, Cursor audioCursor) {
        this.context = context;
        this.audioCursor = audioCursor;
    }


    public void swapCursor(Cursor newCursor) {
        audioCursor = newCursor;
        notifyDataSetChanged();
    }

    public Cursor getAudioCursor() {
        return audioCursor;
    }

    @Override
    public int getCount() {
        if (audioCursor != null)
            return audioCursor.getCount();
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return audioCursor.moveToPosition(position);
    }

    @Override
    public long getItemId(int position) {
        audioCursor.moveToPosition(position);
        return audioCursor.getLong(audioCursor.getColumnIndex(MediaStore.Audio.Media._ID));
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.audio_list_item, parent, false);
        }
        int idColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media._ID);
        int titleColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumIdColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int dataAddedColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);

        audioCursor.moveToPosition(position);
        int id = audioCursor.getInt(idColumn);
        String title = audioCursor.getString(titleColumn);
        String artist= audioCursor.getString(artistColumn);
        String album = audioCursor.getString(albumColumn);
        int albumId = audioCursor.getInt(albumIdColumn);
        Log.d(TAG, "album ID: " + albumId);
        long dataAdded = audioCursor.getLong(dataAddedColumn);
        TextView titleTextView = convertView.findViewById(R.id.title_tv);
        titleTextView.setText(title);
        return convertView;
    }
}
