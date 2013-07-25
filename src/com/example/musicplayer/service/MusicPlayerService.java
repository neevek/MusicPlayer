package com.example.musicplayer.service;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
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

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 5:58 PM
 */
public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
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

    private TelephonyManager mTelephonyManager;
    private boolean mStoppedByPhoneCalls;

    private boolean mMediaPlayerPrepared;

    private ComponentName mMediaButtonBroadcastReceiverComponentName;
    private HeadsetBroadcastReceiver mHeadsetBroadcastReceiver;

    private static WeakReference<MusicPlayerService> mMusicPlayerServiceRef;

    @Override
    public void onCreate() {
        super.onCreate();

        mMusicPlayerServiceRef = new WeakReference<MusicPlayerService>(this);

        mApp = MusicPlayerApplication.getInstance();

        mMusicPlayerDAO = mApp.getMusicPlayerDAO();

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);

        mMessageePump = mApp.getMessagePump();

        mActionQueue = new TaskQueue(10);
        mActionQueue.start();

        mPlayingProgressNotifier = new PlayingProgressNotifier();
        mPlayingProgressNotifier.start();


        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Intent openMainAppIntent = new Intent(this, MainActivity.class);
        openMainAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mOpenMainAppPendingIntent = PendingIntent.getActivity(this, 0, openMainAppIntent, 0);

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        mTelephonyManager.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

        mMediaButtonBroadcastReceiverComponentName = new ComponentName(this, MediaButtonBroadcastReceiver.class);
        ((AudioManager) getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(mMediaButtonBroadcastReceiverComponentName);

        mHeadsetBroadcastReceiver = new HeadsetBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadsetBroadcastReceiver, intentFilter);
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
            if (isPlaying()) {
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

        mMusicPlayerServiceRef.clear();

        unregisterReceiver(mHeadsetBroadcastReceiver);
        ((AudioManager) getSystemService(AUDIO_SERVICE)).unregisterMediaButtonEventReceiver(mMediaButtonBroadcastReceiverComponentName);
        mTelephonyManager.listen(null, 0);

        mPlayingProgressNotifier.stopThread();

        mActionQueue.stopTaskQueue();
        mMediaPlayer.release();

        stopForeground(true);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mMediaPlayerPrepared = true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        List<Song> playList = mApp.getCurrentPlayList();
        if (playList != null) {
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
    }

    public void pausePlayback() {
        mActionQueue.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (isPlaying())
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
        if (isPlaying())
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
                if (!isPlaying())
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

    public boolean isPlaying() {
        return mMediaPlayerPrepared && mMediaPlayer.isPlaying();
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

                if (!isPlaying())
                    continue;

                mLastPlayingProgress = mMediaPlayer.getCurrentPosition();

                mProgressBuffer.delete(0, mProgressBuffer.length());
                notification.setLatestEventInfo(MusicPlayerService.this, mCurrentSong.title + "(" + mCurrentSong.artist+ ")", Util.formatMilliseconds(mLastPlayingProgress, mProgressBuffer), mOpenMainAppPendingIntent);
                mNotificationManager.notify(1, notification);

                mMessageePump.broadcastMessage(Message.Type.ON_UPDATE_PLAYING_PROGRESS, mLastPlayingProgress);
            }
        }
    }

    class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (isPlaying() || mStoppedByPhoneCalls) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        pausePlaybackForCallEvents();
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mStoppedByPhoneCalls) {
                            mStoppedByPhoneCalls = false;
                            resumePlayback();
                        }
                        break;
                }
            }
        }

        private void pausePlaybackForCallEvents() {
            if (isPlaying()) {
                mStoppedByPhoneCalls = true;
                pausePlayback();
            }
        }
    }

    public static class HeadsetBroadcastReceiver extends BroadcastReceiver {
        private static boolean mPausedByHeadsetUnplug = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("state", 0);
            handleUnplugHeadset(state == 0);
            abortBroadcast();
        }

        private void handleUnplugHeadset (boolean unplugged) {
            MusicPlayerService musicPlayerService = mMusicPlayerServiceRef.get();
            if (musicPlayerService != null) {
                if (unplugged) {
                    if (musicPlayerService.isPlaying()) {
                        musicPlayerService.pausePlayback();
                        mPausedByHeadsetUnplug = true;
                    }
                } else if (mPausedByHeadsetUnplug) {
                    musicPlayerService.resumePlayback();
                    mPausedByHeadsetUnplug = false;
                }
            }
            if (DEBUG) Log.d(TAG, ">>>> headset unplugged: " + unplugged);
        }
    }

    public static class MediaButtonBroadcastReceiver extends BroadcastReceiver {
        private static long mLastActionDownTimestampForLongPressing = 0;

        private static long mFirstActionDownTimestamp = 0;
        private static long mLastActionUpTimestamp = 0;

        private final static int LONG_PRESS_THRESHOLD = 500;
        private final static int NON_DUPLICATE_PRESS_THRESHOLD = 500;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                handleKeyPress(event);
            }
            abortBroadcast();
        }

        private void handleKeyPress(KeyEvent event) {
            int action = event.getAction();

            if (action == KeyEvent.ACTION_DOWN) {
                long actionDownTime = event.getDownTime();
                if (actionDownTime != mFirstActionDownTimestamp)
                    mFirstActionDownTimestamp = actionDownTime;

                if (mFirstActionDownTimestamp != mLastActionDownTimestampForLongPressing) {
                    if (reachLongPressThreshold(event)) {
                        int keyCode = event.getKeyCode();
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_HEADSETHOOK:
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                handlePlayAndPausePress();
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                // volume up
                                break;
                            case KeyEvent.KEYCODE_MEDIA_NEXT:
                                // volume down
                                break;
                        }
                        mLastActionDownTimestampForLongPressing = mFirstActionDownTimestamp;
                    }
                }
            } else if (action == KeyEvent.ACTION_UP) {
                if (mFirstActionDownTimestamp != mLastActionDownTimestampForLongPressing) {
                    int keyCode = event.getKeyCode();
                    if (reachLongPressThreshold(event) && (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
                        // a long press
                        handlePlayAndPausePress();
                        mLastActionDownTimestampForLongPressing = mFirstActionDownTimestamp;
                    } else if (isActionUpValid(event)) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_HEADSETHOOK:
                            case KeyEvent.KEYCODE_MEDIA_NEXT:
                                handleMediaNextPress();
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                handleMediaPrevPress();
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                handlePlayAndPausePress();
                        }
                    }

                    mLastActionUpTimestamp = event.getEventTime();
                }
            }
        }

        private boolean reachLongPressThreshold(KeyEvent event) {
            return event.getEventTime() - mFirstActionDownTimestamp >= LONG_PRESS_THRESHOLD;
        }

        private boolean isActionUpValid(KeyEvent event) {
            return event.getEventTime() - mLastActionUpTimestamp > NON_DUPLICATE_PRESS_THRESHOLD;
        }

        private void handlePlayAndPausePress() {
            MusicPlayerService musicPlayerService = mMusicPlayerServiceRef.get();
            if (musicPlayerService != null) {
                if (musicPlayerService.isPlaying())
                    musicPlayerService.pausePlayback();
                else
                    musicPlayerService.resumePlayback();
            }
            if (DEBUG) Log.d(TAG, ">>>> media button PLAY/PAUSE pressed.");
        }

        private void handleMediaNextPress() {
            MusicPlayerService musicPlayerService = mMusicPlayerServiceRef.get();
            if (musicPlayerService != null) {
                musicPlayerService.playNextSong();
            }
            if (DEBUG) Log.d(TAG, ">>>> media button NEXT pressed.");
        }

        private void handleMediaPrevPress() {
            MusicPlayerService musicPlayerService = mMusicPlayerServiceRef.get();
            if (musicPlayerService != null) {
                musicPlayerService.playPrevSong();
            }
            if (DEBUG) Log.d(TAG, ">>>> media button PREVIOUS pressed.");
        }
    }
}
