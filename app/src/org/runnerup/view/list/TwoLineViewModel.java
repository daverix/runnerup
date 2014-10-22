package org.runnerup.view.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.runnerup.R;

public class TwoLineViewModel implements ListViewModel {
    private final LayoutInflater inflater;
    private final long itemId;
    private final boolean isEnabled;
    private final String title;
    private final String subTitle;

    public TwoLineViewModel(LayoutInflater inflater, long itemId, boolean isEnabled, String title, String subTitle) {
        this.inflater = inflater;
        this.itemId = itemId;
        this.isEnabled = isEnabled;
        this.title = title;
        this.subTitle = subTitle;
    }

    @Override
    public int getItemViewType() {
        return ListItemTypes.LIST_ITEM_TYPE_TWO_LINE;
    }

    @Override
    public long getItemId() {
        return itemId;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public View getView(View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if(convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.two_line_list_item, viewGroup, false);
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.subTitle = (TextView) convertView.findViewById(R.id.subTitle);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(title);
        holder.subTitle.setText(subTitle);

        return convertView;
    }

    private static class ViewHolder {
        public TextView title;
        public TextView subTitle;
    }
}
