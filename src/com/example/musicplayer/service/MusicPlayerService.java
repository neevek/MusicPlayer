package com.example.musicplayer.service;


import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.example.musicplayer.MusicPlayerApplication;
import com.example.musicplayer.db.MusicPlayerDAO;
import com.example.musicplayer.message.Message;
import com.example.musicplayer.message.MessagePump;
import com.example.musicplayer.pojo.Song;
import com.example.musicplayer.sync.TaskQueue;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 5:58 PM
 */
public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener {
    private final static boolean DEBUG = true;
    private final static String TAG = MusicPlayerService.class.getSimpleName();

    private MusicPlayerApplication mApp;
    private MediaPlayer mMediaPlayer;

    private Song mCurrentSong;

    private MessagePump mMessageePump;
    private TaskQueue mActionQueue;
    private PlayingProgressNotifier mPlayingProgressNotifier;

    private MusicPlayerDAO mMusicPlayerDAO;

    private List<Song> mPlayList;

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = MusicPlayerApplication.getInstance();

        mMusicPlayerDAO = mApp.getMusicPlayerDAO();

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mMessageePump = mApp.getMessagePump();

        mActionQueue = new TaskQueue(10);
        mActionQueue.start();

        mPlayingProgressNotifier = new PlayingProgressNotifier();
        mPlayingProgressNotifier.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            startPlayingANewSong(intent.getIntExtra("songId", 0), intent.getIntExtra("progress", 0));
        }
        return START_NOT_STICKY;
    }

    private void startPlayingANewSong(final int songId, final int progress) {
        mPlayingProgressNotifier.mLastPlayingProgress = progress;

        mActionQueue.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer.isPlaying() && mCurrentSong.id == songId) {
                    return;
                }

                Song song = mMusicPlayerDAO.getSongById(songId);
                if (song == null)
                    return;

                playSong(song, progress);
            }
        });
    }

    private void playSong(Song song, int progress) {
        if (DEBUG) Log.d(TAG, ">>>> start playing: " + song.title);
        try {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }

            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(song.filePath);
//                    mMediaPlayer.setOnPreparedListener(MusicPlayerService.this);
            mMediaPlayer.prepare();
            mMediaPlayer.seekTo(progress);
            mMediaPlayer.start();

            mMessageePump.broadcastMessage(Message.Type.ON_START_PLAYBACK, song);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "播放音乐失败：" + song.filePath, Toast.LENGTH_LONG).show();
        } finally {
            mCurrentSong = song;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MusicPlayerServiceBinder(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mPlayingProgressNotifier.stopThread();

        mActionQueue.stopTaskQueue();
        mMediaPlayer.release();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    public void pausePlayback() {
        mActionQueue.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer.isPlaying())
                    mMediaPlayer.pause();

                if (mCurrentSong != null)
                    mMessageePump.broadcastMessage(Message.Type.ON_PAUSE_PLAYBACK, mCurrentSong);
            }
        });
    }

    public void resumePlayback() {
        mActionQueue.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (!mMediaPlayer.isPlaying())
                    mMediaPlayer.start();

                if (mCurrentSong != null)
                    mMessageePump.broadcastMessage(Message.Type.ON_RESUME_PLAYBACK, mCurrentSong);
            }
        });
    }

    public void playNextSong() {
        List<Song> songList = mApp.getCurrentPlayList();
        if (songList != null && songList.size() > 0) {
            int nextSongIndex = songList.indexOf(mCurrentSong);
            if (nextSongIndex != -1) {
                if (nextSongIndex < songList.size() - 1)
                    ++nextSongIndex;
                else
                    nextSongIndex = 0;
            }

            if (nextSongIndex == -1)
                nextSongIndex = 0;

            playSong(songList.get(nextSongIndex), 0);
        }
    }

    public void playPrevSong() {
        List<Song> songList = mApp.getCurrentPlayList();
        if (songList != null && songList.size() > 0) {
            int prevSongIndex = songList.indexOf(mCurrentSong);
            if (prevSongIndex != -1) {
                if (prevSongIndex > 0)
                    --prevSongIndex;
                else
                    prevSongIndex = songList.size() - 1;
            }

            if (prevSongIndex == -1)
                prevSongIndex = 0;

            playSong(songList.get(prevSongIndex), 0);
        }
    }

    public Song getCurrentSong () {
        return mCurrentSong;
    }

    public boolean isPlayingSong () {
        return mMediaPlayer.isPlaying();
    }

    public int getPlayingProgress () {
        return mPlayingProgressNotifier.mLastPlayingProgress;
    }

    class PlayingProgressNotifier extends Thread {
        private boolean mRunning;
        private int mLastPlayingProgress;

        @Override
        public synchronized void start() {
            mRunning = true;
            super.start();
        }

        public void stopThread () {
            mRunning = false;
            interrupt();
        }

        @Override
        public void run() {
            while (mRunning) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    continue;
                }

                if (!mMediaPlayer.isPlaying())
                    continue;

                mLastPlayingProgress = mMediaPlayer.getCurrentPosition();
                mMessageePump.broadcastMessage(Message.Type.ON_UPDATE_PLAYING_PROGRESS, mLastPlayingProgress);
            }
        }
    }
}
