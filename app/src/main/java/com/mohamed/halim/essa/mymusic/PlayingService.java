package com.mohamed.halim.essa.mymusic;

import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.Nullable;

public class PlayingService extends IntentService {
    
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public PlayingService() {
        super(PlayingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}
