package org.runnerup.view;

import android.support.v4.app.Fragment;

public class FragmentPage {
    private final String title;
    private final String tag;
    private final FragmentFactory factory;

    public FragmentPage(String title, String tag, FragmentFactory factory) {
        this.title = title;
        this.tag = tag;
        this.factory = factory;
    }

    public Fragment create() {
        return factory.build();
    }

    public String getTitle() {
        return title;
    }

    public String getTag() {
        return tag;
    }
}
