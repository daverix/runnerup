package org.runnerup.view;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

public class FragmentAdapter extends FragmentPagerAdapter {
    private List<FragmentPage> pages;

    FragmentAdapter(FragmentManager fm, List<FragmentPage> pages) {
        super(fm);
        this.pages = pages;
    }

    @Override
    public Fragment getItem(int position) {
        return pages.get(position).create();

    }

    @Override
    public int getCount() {
        return 4;
    }
}
