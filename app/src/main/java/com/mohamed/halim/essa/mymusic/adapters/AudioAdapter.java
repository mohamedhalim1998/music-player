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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        int titleColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumIdColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int dataAddedColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
        int durationColumn = audioCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        audioCursor.moveToPosition(position);
        String title = audioCursor.getString(titleColumn);
        String artist = audioCursor.getString(artistColumn);
        String album = audioCursor.getString(albumColumn);
        int albumId = audioCursor.getInt(albumIdColumn);
        long dataAdded = audioCursor.getLong(dataAddedColumn);
        long duration = audioCursor.getLong(durationColumn);
        TextView titleTextView = convertView.findViewById(R.id.title_tv);
        TextView artistTextView = convertView.findViewById(R.id.album_artist_tv);
        TextView durationTextView = convertView.findViewById(R.id.duration_tv);
        titleTextView.setText(title);
        artistTextView.setText(String.format("%s - %s", artist, album));
        durationTextView.setText(formatDuration(duration));
        return convertView;
    }

    private String formatDuration(long duration) {
        long seconds = duration / 1000;
        long min = seconds / 60;
        seconds = seconds - min * 60;
        long hours = min / 60;
        min = min - hours * 60;
        if (hours != 0)
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, min, seconds);
        else
            return String.format(Locale.getDefault(), "%02d:%02d", min, seconds);

    }
}
