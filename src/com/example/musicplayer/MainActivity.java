package com.example.musicplayer;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.example.musicplayer.db.MusicPlayerDAO;
import com.example.musicplayer.db.MusicPlayerDBHelper;
import com.example.musicplayer.util.TaskExecutor;
import com.example.musicplayer.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {
    private final static boolean DEBUG = true;
    private final static String TAG = MainActivity.class.getSimpleName();

    private MusicPlayerApplication mApp;
    private MusicPlayerDAO mMusicPlayerDAO;

    private MainFragment mMainFragment;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mApp = (MusicPlayerApplication)getApplication();
        mMusicPlayerDAO = mApp.getMusicPlayerDAO();

        setMainFragment();
	}

    private void setMainFragment() {
        mMainFragment = new MainFragment();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(R.id.container, mMainFragment);
//        ft.setTransition(FragmentTransaction.TRANSIT_ENTER_MASK);
        ft.commit();
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case R.id.action_scan:
                Toast.makeText(this, "开始扫描SD卡...", Toast.LENGTH_SHORT).show();
                TaskExecutor.executeTask(new Runnable() {
                    @Override
                    public void run() {
                        scanMP3Files();

                        mMainFragment.setItemDataCounts(true);

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "扫描完成！", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                break;
		case R.id.action_quit:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}


    private void scanMP3Files () {
        if (DEBUG) Log.d(TAG, ">>>> start scanning for mp3 files...");
        String rootDir = "/sdcard" ;
        traverseScanMP3Files(new File(rootDir));
        if (DEBUG) Log.d(TAG, ">>>> done scanning for mp3 files...");
    }

    private void traverseScanMP3Files (File dir) {
        if (dir.exists()) {
//            if (DEBUG) Log.d(TAG, ">>>> scanning dir: " + dir.getAbsoluteFile());
            if (dir.getName().startsWith(".") || dir.getAbsolutePath().startsWith("/sdcard/Android/"))
                return;

            File[] files = dir.listFiles();
            if (files == null)
                return;

            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    traverseScanMP3Files(file);
                } else {
                    if (file.getName().toLowerCase().endsWith("mp3")) {
                        String filePath = file.getAbsolutePath();

                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(filePath);

                        String duration = Util.ensureNotNull(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION), "");

                        int intDuration = 0;
                        if (duration != null && duration.length() > 0)
                            intDuration = Integer.parseInt(duration);

                        if (intDuration == 0)
                            continue;

                        String title = Util.ensureNotNull(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE), file.getName());
                        String artist = Util.ensureNotNull(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST), "");
                        String album = Util.ensureNotNull(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM), "");

                        int albumId = 0;
                        int artistId = 0;
                        if (!album.equals(""))
                            albumId = mMusicPlayerDAO.addAlbum(album);
                        if (!artist.equals(""))
                            artistId = mMusicPlayerDAO.addArtist(artist);

                        mMusicPlayerDAO.addSong(title, artistId, artist, albumId, album, intDuration, filePath);

                        if (DEBUG) {
                            Log.d(TAG, ">>>> song info: " + artist + ", " + title + ", " + album + ", " + duration + ", " + artistId + ", " + albumId);
                        }
                    }
                }
            }
        }
    }
}
