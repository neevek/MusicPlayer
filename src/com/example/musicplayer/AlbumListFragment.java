package com.example.musicplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.example.musicplayer.db.MusicPlayerDAO;
import com.example.musicplayer.pojo.Album;
import com.example.musicplayer.util.TaskExecutor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 10:12 AM
 */
public class AlbumListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final static boolean DEBUG = true;
    private final static String TAG = AlbumListFragment.class.getSimpleName();

    private List<Album> mAlbumList;
    private ListView mListView;
    private AlbumListAdapter mAdapter;

    private MusicPlayerDAO mMusicPlayerDAO;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMusicPlayerDAO = MusicPlayerApplication.getInstance().getMusicPlayerDAO();

        mListView = (ListView)LayoutInflater.from(getActivity()).inflate(R.layout.music_list, null, false);
        mListView.setOnItemClickListener(this);

        TaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                mAlbumList = mMusicPlayerDAO.getAlbums();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter = new AlbumListAdapter();
                        mListView.setAdapter(mAdapter);
                    }
                });
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAlbumList.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mListView != null && mListView.getParent() != null) {
            ((ViewGroup)mListView.getParent()).removeView(mListView);
            return mListView;
        }

        return mListView;
    }

    private class AlbumListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mAlbumList.size();
        }

        @Override
        public Object getItem(int position) {
            return mAlbumList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater(null).inflate(R.layout.album_list_item, null);
                holder = new ViewHolder();

                holder.albumTitle = (TextView) convertView.findViewById(R.id.tv_album_title);
                holder.albumInfo = (TextView) convertView.findViewById(R.id.tv_album_info);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Album album = mAlbumList.get(position);
            holder.albumTitle.setText(position + 1 + ". " + album.name);
            holder.albumInfo.setText("共" + album.songCount + "首歌曲");

            return convertView;
        }

        class ViewHolder {
            TextView albumTitle;
            TextView albumInfo;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }
}
