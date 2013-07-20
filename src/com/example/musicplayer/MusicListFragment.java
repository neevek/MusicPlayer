package com.example.musicplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
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

    private View mLayout;

    private List<Song> mSongList;
    private ListView mListView;
    private MusicListAdapter mAdapter;

    private MusicPlayerDAO mMusicPlayerDAO;

    public final static String LIST_TYPE = "list_type";
    public final static int TYPE_ALL_MUSIC = 0;
    public final static int TYPE_BY_ARTIST = 1;
    public final static int TYPE_BY_ALBUM = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMusicPlayerDAO = MusicPlayerApplication.getInstance().getMusicPlayerDAO();


        final Bundle args = getArguments();
        final int type = args.getInt(LIST_TYPE, TYPE_ALL_MUSIC);
        if (DEBUG) Log.d(TAG, ">>>> list type: " + type);

        TaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                List<Song> songList = null;
                switch (type) {
                    case TYPE_ALL_MUSIC:
                        songList = mMusicPlayerDAO.getAllSongs();
                        break;
                    case TYPE_BY_ARTIST:
                        songList = mMusicPlayerDAO.getSongsByAlbumId(args.getInt("artist_id"));
                        break;
                    case TYPE_BY_ALBUM:
                        songList = mMusicPlayerDAO.getSongsByAlbumId(args.getInt("album_id"));
                        break;
                }

                final List<Song> finalSongList = songList;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSongList = finalSongList;
                        if (mAdapter == null)
                            mAdapter = new MusicListAdapter();
                        mListView.setAdapter(mAdapter);
                    }
                });
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSongList.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
    }
}
