package org.runnerup.view.list;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.runnerup.R;

import java.util.List;

public abstract class ListViewModelFragment extends Fragment {
    private ListView listView;
    private ListViewModelAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.start_basic, container, false);

        listView = (ListView) view.findViewById(R.id.start_list);
        updateItems(onCreateListItems(inflater));

        return view;
    }

    protected abstract List<ListViewModel> onCreateListItems(LayoutInflater inflater);

    public void updateItems(List<ListViewModel> items) {
        if(items == null) throw new IllegalArgumentException("items is null");

        if(adapter == null) {
            adapter = new ListViewModelAdapter(items);
            listView.setAdapter(adapter);
        }
        else {
            adapter.setItems(items);
            adapter.notifyDataSetChanged();
        }
    }

    public void notifyDataSetChanged() {
        if(adapter == null) return;

        adapter.notifyDataSetChanged();
    }
}
