package com.example.musicplayer.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MusicPlayerApplication;
import com.example.musicplayer.R;
import com.example.musicplayer.db.MusicPlayerDAO;
import com.example.musicplayer.message.Message;
import com.example.musicplayer.pojo.Song;
import com.example.musicplayer.service.MusicPlayerService;
import com.example.musicplayer.util.TaskExecutor;
import com.example.musicplayer.util.Util;

import java.io.File;
import java.util.List;

import static android.app.Dialog.*;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 10:12 AM
 */
public class MusicListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final static boolean DEBUG = true;
    private final static String TAG = MusicListFragment.class.getSimpleName();

    private MusicPlayerApplication mApp;
    private View mLayout;

    private List<Song> mSongList;
    private ListView mListView;
    private MusicListAdapter mAdapter;

    private MusicPlayerDAO mMusicPlayerDAO;

    private String mTitle;

    private final static int CONTEXT_MENU_ITEM_DELETE = 1;

    private MusicPlayerService mMusicPlayerService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = MusicPlayerApplication.getInstance();
        mMusicPlayerDAO = mApp.getMusicPlayerDAO();


        final Bundle args = getArguments();
        final int type = args.getInt(MainActivity.LIST_TYPE, MainActivity.TYPE_ALL_MUSIC);

        mTitle = args.getString(MainActivity.EXTRA_TITLE);
        if (DEBUG) Log.d(TAG, ">>>> list type: " + type);

        TaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                if (isDetached())
                    return;

                List<Song> songList = null;
                switch (type) {
                    case MainActivity.TYPE_ALL_MUSIC:
                        songList = mApp.getCachedAllMusicSongList(true);
                        break;
                    case MainActivity.TYPE_BY_ARTIST:
                        songList = mMusicPlayerDAO.getSongsByArtistId(args.getInt(MainActivity.EXTRA_ID));
                        break;
                    case MainActivity.TYPE_BY_ALBUM:
                        songList = mMusicPlayerDAO.getSongsByAlbumId(args.getInt(MainActivity.EXTRA_ID));
                        break;
                }

                if (getActivity() == null)
                    return;

                mApp.setCurrentPlayList(songList);

                final List<Song> finalSongList = songList;
                mApp.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSongList = finalSongList;

                        if (mAdapter == null)
                            mAdapter = new MusicListAdapter();
                        mListView.setAdapter(mAdapter);

                        getActivity().setTitle(mTitle);
                    }
                });
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSongList = null;
        mListView.setAdapter(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(mTitle);

        if (mLayout != null) {
            ((ViewGroup)mLayout.getParent()).removeView(mLayout);
            return mLayout;
        }

        mLayout = inflater.inflate(R.layout.music_list, null, false);
        mListView = (ListView) mLayout;
        mListView.setOnItemClickListener(this);

        registerForContextMenu(mListView);


        return mLayout;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.addSubMenu(Menu.NONE, CONTEXT_MENU_ITEM_DELETE, Menu.NONE, "删除歌曲");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_MENU_ITEM_DELETE:
                final Song song = mSongList.get(info.position);
                TaskExecutor.executeTask(new Runnable() {
                    @Override
                    public void run() {
                        // delete song from database
                        mMusicPlayerDAO.deleteSong(song.id, song.artistId, song.albumId);

                        // delete song from list in memory
                        mSongList.remove(song);
                        List<Song> cachedSongList = mApp.getCachedAllMusicSongList(false);
                        if (cachedSongList != null && mSongList != mApp.getCachedAllMusicSongList(false))
                            cachedSongList.remove(song);

                        mApp.getMessagePump().broadcastMessage(Message.Type.REDRAW_LIST, null);

                        // redraw the current listview and show a dialog to ask
                        // if the user wants to delete the underlying file
                        mApp.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                                showAskDeleteFileDialog();
                            }

                            private void showAskDeleteFileDialog() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle("删除歌曲")
                                        .setMessage("同时删除目标文件吗？")
                                        .setCancelable(false)
                                        .setPositiveButton("是", new OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                TaskExecutor.executeTask(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Song currentSong = mMusicPlayerService.getCurrentSong();
                                                        if (currentSong != null && currentSong.id == song.id)
                                                            mMusicPlayerService.stopPlaybackDirectly();

                                                        File file = new File(song.filePath);
                                                        if (file.exists())
                                                            file.delete();
                                                    }
                                                });
                                            }
                                        })
                                        .setNegativeButton("否", new OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.show();
                            }
                        });
                    }
                });
                break;
        }
        return true;
    }

    public void setMusicPlayerService (MusicPlayerService service) {
        mMusicPlayerService = service;
    }

    private class MusicListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mSongList.size();
        }

        @Override
        public Object getItem(int position) {
            return mSongList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater(null).inflate(R.layout.music_list_item, null);
                holder = new ViewHolder();

                holder.title = (TextView) convertView.findViewById(R.id.tv_title);
                holder.artist = (TextView) convertView.findViewById(R.id.tv_artist);
                holder.duration = (TextView) convertView.findViewById(R.id.tv_duration);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Song song = mSongList.get(position);
            holder.title.setText(position + 1 + ". " + song.title);
            holder.duration.setText("[" + Util.formatMilliseconds(song.duration, null) + "]");
            holder.artist.setText(song.artist);

            return convertView;
        }

        class ViewHolder {
            TextView title;
            TextView duration;
            TextView artist;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mApp.startPlayingSong(mSongList.get(position).id, 0);
    }
}
