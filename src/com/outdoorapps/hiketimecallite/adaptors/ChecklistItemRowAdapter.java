package com.outdoorapps.hiketimecallite.adaptors;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.outdoorapps.hiketimecallite.R;

public class ChecklistItemRowAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<String> itemList;
	private ArrayList<Integer> checkedItemList;

	public ChecklistItemRowAdapter(Context context, ArrayList<String> itemList, ArrayList<Integer> checkedItemList){
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.itemList = itemList;
		this.checkedItemList = checkedItemList;
	}

	@Override
	public int getCount() {
		return itemList.size();
	}

	@Override
	public String getItem(int index) {
		return itemList.get(index);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {		
		String item = getItem(position);
		View vi = convertView;
		if (vi == null)
			vi = inflater.inflate(R.layout.row_planner_item, null);

		TextView name = (TextView) vi.findViewById(R.id.name);
		name.setText(item);
		
		// Checked selected item
		CheckBox checkbox = (CheckBox)vi.findViewById(R.id.checkBox);
		if(checkedItemList.contains(position))
			checkbox.setChecked(true);
		else
			checkbox.setChecked(false);
		return vi;
	}

}
