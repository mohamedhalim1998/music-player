package com.mohamed.halim.essa.mymusic.data;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

@Parcel
public class AudioFile {
    int id;
    String title;
    String artist;
    String album;
    int albumId;
    int dateAdded;
    long duration;

    @ParcelConstructor
    public AudioFile(int id, String title, String artist, String album, int albumId, int dateAdded, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumId = albumId;
        this.dateAdded = dateAdded;
        this.duration = duration;
    }

    public int getDateAdded() {
        return dateAdded;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public int getAlbumId() {
        return albumId;
    }

    public long getDuration() {
        return duration;
    }
}
