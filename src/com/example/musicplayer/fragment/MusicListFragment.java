package com.example.musicplayer.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MusicPlayerApplication;
import com.example.musicplayer.R;
import com.example.musicplayer.db.MusicPlayerDAO;
import com.example.musicplayer.pojo.Song;
import com.example.musicplayer.util.TaskExecutor;

import java.util.List;

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

                mApp.setCurrentPlayList(songList);

                final List<Song> finalSongList = songList;
                getActivity().runOnUiThread(new Runnable() {
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


        return mLayout;
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

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Song song = mSongList.get(position);
            holder.title.setText(position + 1 + ". " + song.title);
            holder.artist.setText(song.artist);

            return convertView;
        }

        class ViewHolder {
            TextView title;
            TextView artist;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mApp.startPlayingSong(mSongList.get(position).id, 0);
    }
}
