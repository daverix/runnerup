package org.runnerup.view.list;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListViewModelAdapter extends BaseAdapter {
    private List<ListViewModel> items;
    private int viewTypeCount;

    public ListViewModelAdapter(List<ListViewModel> items) {
        this.items = items;
        recalculateViewTypeCount();
    }

    public ListViewModelAdapter() {
        this(new ArrayList<ListViewModel>());
    }

    public void setItems(List<ListViewModel> items) {
        this.items = items;
        recalculateViewTypeCount();
    }

    private void recalculateViewTypeCount() {
        Set<Integer> types = new HashSet<Integer>();
        for(ListViewModel item : items) {
            types.add(item.getItemViewType());
        }
        viewTypeCount = types.size();

        if(viewTypeCount == 0)
            viewTypeCount = 1;
    }

    @Override
    public int getViewTypeCount() {
        return viewTypeCount;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getItemViewType();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public ListViewModel getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getItemId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getItem(position).getView(convertView, parent);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
