package org.runnerup.view.list;

import android.view.View;
import android.view.ViewGroup;

public interface ListViewModel {
    int getItemViewType();
    long getItemId();
    boolean isEnabled();
    View getView(View convertView, ViewGroup viewGroup);
}
