package com.example.musicplayer.service;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MusicPlayerApplication;
import com.example.musicplayer.R;
import com.example.musicplayer.db.MusicPlayerDAO;
import com.example.musicplayer.message.Message;
import com.example.musicplayer.message.MessagePump;
import com.example.musicplayer.pojo.Song;
import com.example.musicplayer.sync.TaskQueue;
import com.example.musicplayer.util.Util;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 5:58 PM
 */
public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener {
    private final static boolean DEBUG = true;
    private final static String TAG = MusicPlayerService.class.getSimpleName();

    private MusicPlayerApplication mApp;
    private MediaPlayer mMediaPlayer;

    private Song mCurrentSong;

    private MessagePump mMessageePump;
    private TaskQueue mActionQueue;
    private PlayingProgressNotifier mPlayingProgressNotifier;

    private MusicPlayerDAO mMusicPlayerDAO;

    private NotificationManager mNotificationManager;
    private PendingIntent mOpenMainAppPendingIntent;
    private Notification notification;
    private StringBuilder mProgressBuffer = new StringBuilder();

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = MusicPlayerApplication.getInstance();

        mMusicPlayerDAO = mApp.getMusicPlayerDAO();

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);

        mMessageePump = mApp.getMessagePump();

        mActionQueue = new TaskQueue(10);
        mActionQueue.start();

        mPlayingProgressNotifier = new PlayingProgressNotifier();
        mPlayingProgressNotifier.start();


        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Intent openMainAppIntent = new Intent(this, MainActivity.class);
        openMainAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mOpenMainAppPendingIntent = PendingIntent.getActivity(this, 0, openMainAppIntent, 0);
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
            mMediaPlayer.prepare();
            mMediaPlayer.seekTo(progress);
            mMediaPlayer.start();

            mMessageePump.broadcastMessage(Message.Type.ON_START_PLAYBACK, song);

            notification = new Notification();
            notification.icon = R.drawable.notification_icon;
            notification.flags &= ~Notification.FLAG_AUTO_CANCEL;
            notification.tickerText = "正在播放 " + song.title;

            mProgressBuffer.delete(0, mProgressBuffer.length());
            notification.setLatestEventInfo(this, song.title + "(" + song.artist+ ")", Util.formatMilliseconds(progress, mProgressBuffer), mOpenMainAppPendingIntent);

            startForeground(1, notification);
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

        stopForeground(true);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        List<Song> playList = mApp.getCurrentPlayList();
        int songIndex = playList.indexOf(mCurrentSong);
        if (songIndex == -1 || songIndex == playList.size() - 1) {
            mp.stop();
            getSharedPreferences(MusicPlayerApplication.SHARED_PREF, MODE_PRIVATE).edit()
                    .remove(MusicPlayerApplication.PREF_KEY_LAST_PLAYED_SONG_PROGRESS)
                    .commit();

            stopForeground(true);

            mMessageePump.broadcastMessage(Message.Type.ON_PAUSE_PLAYBACK, mCurrentSong);
        } else {
            playNextSong();
        }
    }

    public void pausePlayback() {
        mActionQueue.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer.isPlaying())
                    mMediaPlayer.pause();

                if (mCurrentSong != null) {
                    getSharedPreferences(MusicPlayerApplication.SHARED_PREF, MODE_PRIVATE).edit()
                            .putInt(MusicPlayerApplication.PREF_KEY_LAST_PLAYED_SONG_ID, mCurrentSong.id)
                            .putInt(MusicPlayerApplication.PREF_KEY_LAST_PLAYED_SONG_PROGRESS, getPlayingProgress())
                            .commit();

                    mMessageePump.broadcastMessage(Message.Type.ON_PAUSE_PLAYBACK, mCurrentSong);
                }
            }
        });
    }

    public void stopPlaybackDirectly() {
        if (mMediaPlayer.isPlaying())
            mMediaPlayer.stop();

        if (mCurrentSong != null) {
            getSharedPreferences(MusicPlayerApplication.SHARED_PREF, MODE_PRIVATE).edit()
                    .remove(MusicPlayerApplication.PREF_KEY_LAST_PLAYED_SONG_ID)
                    .remove(MusicPlayerApplication.PREF_KEY_LAST_PLAYED_SONG_PROGRESS)
                    .commit();

            mMessageePump.broadcastMessage(Message.Type.ON_DELETE_CURRENT_SONG, mCurrentSong);
        }
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
        mActionQueue.scheduleTask(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    public void playPrevSong() {
        mActionQueue.scheduleTask(new Runnable() {
            @Override
            public void run() {
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
        });
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

                mProgressBuffer.delete(0, mProgressBuffer.length());
                notification.setLatestEventInfo(MusicPlayerService.this, mCurrentSong.title + "(" + mCurrentSong.artist+ ")", Util.formatMilliseconds(mLastPlayingProgress, mProgressBuffer), mOpenMainAppPendingIntent);
                mNotificationManager.notify(1, notification);

                mMessageePump.broadcastMessage(Message.Type.ON_UPDATE_PLAYING_PROGRESS, mLastPlayingProgress);
            }
        }
    }
}
