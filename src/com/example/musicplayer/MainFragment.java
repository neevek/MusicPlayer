package com.example.musicplayer;

import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.musicplayer.db.MusicPlayerDAO;
import com.example.musicplayer.util.TaskExecutor;

/**
 * Created with IntelliJ IDEA.
 * User: neevek
 * Date: 7/20/13
 * Time: 10:12 AM
 */
public class MainFragment extends Fragment implements AdapterView.OnItemClickListener {
    private View mLayout;

    private MusicPlayerApplication mApp;
    private MusicPlayerDAO mMusicPlayerDAO;

    private GridView mGridView;
    private BlockMenuAdapter mAdapter;

    private final static String[] MENU_ITEM_TEXT = new String[]{"全部音乐", "歌手", "专辑"};
    private final static int[] MENU_ITEM_DATA_COUNT = new int[3];

    private MusicListFragment mMusicListFragment;
    private ArtistListFragment mArtistListFragment;
    private AlbumListFragment mAlbumListFragment;

    public MainFragment () {
        mApp = MusicPlayerApplication.getInstance();
        mMusicPlayerDAO = mApp.getMusicPlayerDAO();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMusicListFragment = new MusicListFragment();
        mMusicListFragment.setArguments(new Bundle());

        mArtistListFragment = new ArtistListFragment();
        mAlbumListFragment = new AlbumListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mLayout != null) {
            ((ViewGroup)mLayout.getParent()).removeView(mLayout);
            return mLayout;
        }

        mLayout = inflater.inflate(R.layout.block_menu, null, false);

        mGridView = (GridView) mLayout;
        mGridView.setOnItemClickListener(this);

        System.out.println(">>>>>>>>>>>>> main frag createview");

        TaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                setItemDataCounts(false);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter = new BlockMenuAdapter();
                        mGridView.setAdapter(mAdapter);
                    }
                });
            }
        });

        return mLayout;
    }

    public void setItemDataCounts(boolean redraw) {
        MENU_ITEM_DATA_COUNT[0] = mMusicPlayerDAO.getAllMusicCount();
        MENU_ITEM_DATA_COUNT[1] = mMusicPlayerDAO.getArtistCount();
        MENU_ITEM_DATA_COUNT[2] = mMusicPlayerDAO.getAlbumCount();

        if (redraw) {
            if (Looper.myLooper() == Looper.getMainLooper())
                mAdapter.notifyDataSetChanged();
            else
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
        }
    }


    private class BlockMenuAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return MENU_ITEM_TEXT.length;
        }

        @Override
        public Object getItem(int position) {
            return MENU_ITEM_TEXT[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater(null).inflate(R.layout.block_menu_item, null);
                holder = new ViewHolder();

                holder.icon = (ImageView) convertView.findViewById(R.id.menu_item_icon);
                holder.name = (TextView) convertView.findViewById(R.id.menu_item_name);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(MENU_ITEM_TEXT[position] + "("+ MENU_ITEM_DATA_COUNT[position] +")");

            return convertView;
        }

        class ViewHolder {
            ImageView icon;
            TextView name;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                mMusicListFragment.getArguments().putInt(MusicListFragment.LIST_TYPE, MusicListFragment.TYPE_ALL_MUSIC);
                fragment = mMusicListFragment;
                break;
            case 1:
                fragment = mArtistListFragment;
                break;
            case 2:
                fragment = mAlbumListFragment;
                break;
        }

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }
}
