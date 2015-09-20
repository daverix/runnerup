package org.runnerup.view.start;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.runnerup.R;

import java.util.Arrays;
import java.util.List;

public class BasicFragment extends Fragment {
    private ListView list;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        list = (ListView) view.findViewById(R.id.list);


        list.setAdapter(new Adapter(Arrays.asList(new ListItem("Audio cue settings", "Default"),
                        new ListItem("Sport", "Running"),
                        new ListItem("Target", "Pace"),
                        new ListItem("Target Pace", "00:03:00"))));

        return view;
    }

    private class ListItem {
        private String title;
        private String description;

        public ListItem(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    private class Adapter extends BaseAdapter {
        private final List<ListItem> objects;

        public Adapter(List<ListItem> objects) {
            this.objects = objects;
        }

        @Override
        public int getCount() {
            return objects.size();
        }

        @Override
        public Object getItem(int position) {
            return objects.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            TextView title = (TextView) view.findViewById(android.R.id.text1);
            TextView description = (TextView) view.findViewById(android.R.id.text2);

            ListItem item = (ListItem) getItem(position);
            title.setText(item.getTitle());
            description.setText(item.getDescription());
            return view;
        }
    }
}
