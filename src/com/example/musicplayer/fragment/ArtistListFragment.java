package com.example.musicplayer.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.example.musicplayer.MusicPlayerApplication;
import com.example.musicplayer.R;
import com.example.musicplayer.db.MusicPlayerDAO;
import com.example.musicplayer.pojo.Artist;
import com.example.musicplayer.util.TaskExecutor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 10:12 AM
 */
public class ArtistListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final static boolean DEBUG = true;
    private final static String TAG = ArtistListFragment.class.getSimpleName();

    private List<Artist> mArtistList;
    private ListView mListView;
    private ArtistListAdapter mAdapter;

    private MusicPlayerDAO mMusicPlayerDAO;

    private String mTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTitle = getResources().getString(R.string.title_artist);

        mMusicPlayerDAO = MusicPlayerApplication.getInstance().getMusicPlayerDAO();

        mListView = (ListView)LayoutInflater.from(getActivity()).inflate(R.layout.music_list, null, false);
        mListView.setOnItemClickListener(this);

        TaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                mArtistList = mMusicPlayerDAO.getArtists();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter = new ArtistListAdapter();
                        mListView.setAdapter(mAdapter);
                    }
                });
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mArtistList.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.title_artist);

        if (mListView != null && mListView.getParent() != null) {
            ((ViewGroup)mListView.getParent()).removeView(mListView);
            return mListView;
        }

        return mListView;
    }

    private class ArtistListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mArtistList.size();
        }

        @Override
        public Object getItem(int position) {
            return mArtistList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater(null).inflate(R.layout.artist_list_item, null);
                holder = new ViewHolder();

                holder.artistTitle = (TextView) convertView.findViewById(R.id.tv_artist_name);
                holder.artistInfo = (TextView) convertView.findViewById(R.id.tv_artist_info);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Artist artist = mArtistList.get(position);
            holder.artistTitle.setText(position + 1 + ". " + artist.name);
            holder.artistInfo.setText("共" + artist.songCount + "首歌曲");

            return convertView;
        }

        class ViewHolder {
            TextView artistTitle;
            TextView artistInfo;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }
}
