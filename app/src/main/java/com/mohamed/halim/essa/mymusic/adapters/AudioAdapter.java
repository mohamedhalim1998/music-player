package com.mohamed.halim.essa.mymusic.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.mohamed.halim.essa.mymusic.R;
import com.mohamed.halim.essa.mymusic.data.AudioFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private List<AudioFile> audioFiles;
    private List<AudioFile> originalFiles;
    private static final String TAG = AudioAdapter.class.getSimpleName();

    public AudioAdapter(Context context, List<AudioFile> audioFiles) {
        this.originalFiles = audioFiles;
        this.context = context;
    }


    public void swapList(List<AudioFile> audioFiles) {
        this.originalFiles = audioFiles;
        if (originalFiles != null)
            this.audioFiles = new ArrayList<>(originalFiles);
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        if (audioFiles != null)
            return audioFiles.size();
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return audioFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return audioFiles.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.audio_list_item, parent, false);
        }
        AudioFile currentTrack = audioFiles.get(position);
        String title = currentTrack.getTitle();
        String artist = currentTrack.getArtist();
        String album = currentTrack.getAlbum();
        int albumId = currentTrack.getAlbumId();
        long dataAdded = currentTrack.getDateAdded();
        long duration = currentTrack.getDuration();
        TextView titleTextView = convertView.findViewById(R.id.title_tv);
        TextView artistTextView = convertView.findViewById(R.id.album_artist_tv);
        TextView durationTextView = convertView.findViewById(R.id.duration_tv);
        titleTextView.setText(title);
        artistTextView.setText(String.format("%s - %s", artist, album));
        durationTextView.setText(formatDuration(duration));
        return convertView;
    }

    /**
     * format the duration to HH:mm:ss
     *
     * @param duration : of the track
     * @return formatted duration
     */
    private String formatDuration(long duration) {
        long seconds = duration / 1000;
        long min = seconds / 60;
        seconds = seconds - min * 60;
        long hours = min / 60;
        min = min - hours * 60;
        if (hours != 0)
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, min, seconds);
        else
            return String.format(Locale.getDefault(), "%d:%02d", min, seconds);

    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString().toLowerCase();
                FilterResults filterResults = new FilterResults();
                List<AudioFile> filtered = new ArrayList<>();
                audioFiles = new ArrayList<>(originalFiles);
                for (AudioFile f : audioFiles) {
                    if (f.getAlbum().toLowerCase().contains(constraint)
                            || f.getArtist().toLowerCase().contains(constraint)
                            || f.getTitle().toLowerCase().contains(constraint)) {
                        filtered.add(f);
                    }
                }
                filterResults.values = filtered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                audioFiles = (List<AudioFile>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}
