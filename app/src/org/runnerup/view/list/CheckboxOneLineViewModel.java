package org.runnerup.view.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.runnerup.R;

public class CheckboxOneLineViewModel implements ListViewModel {
    private LayoutInflater inflater;
    private long itemId;
    private boolean enabled;
    private String title;
    private boolean checked;

    public CheckboxOneLineViewModel(LayoutInflater inflater, long itemId, boolean enabled, String title, boolean checked) {
        this.inflater = inflater;
        this.itemId = itemId;
        this.enabled = enabled;
        this.title = title;
        this.checked = checked;
    }

    @Override
    public int getItemViewType() {
        return ListItemTypes.LIST_ITEM_TYPE_CHECKBOX;
    }

    @Override
    public long getItemId() {
        return itemId;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public View getView(View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if(convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.checkbox_one_line_list_item, viewGroup, false);
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.checkox = (CheckBox) convertView.findViewById(R.id.checkbox);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(title);
        holder.checkox.setChecked(checked);

        return convertView;
    }

    private static class ViewHolder {
        CheckBox checkox;
        TextView title;
    }
}
