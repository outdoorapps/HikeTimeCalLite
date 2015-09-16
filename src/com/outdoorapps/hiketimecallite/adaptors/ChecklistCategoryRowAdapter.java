package com.outdoorapps.hiketimecallite.adaptors;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.checklist.Category;

public class ChecklistCategoryRowAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<Category> categoryList;	

	public ChecklistCategoryRowAdapter(Context context, ArrayList<Category> categoryList){
		// Caches the LayoutInflater for quicker use
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.categoryList = categoryList;
	}

	@Override
	public int getCount() {
		return categoryList.size();
	}

	@Override
	public Category getItem(int index) {
		return categoryList.get(index);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {		
		Category category = getItem(position);
		View vi = convertView;
		if (vi == null)
			vi = inflater.inflate(R.layout.row_category, null);

		TextView name = (TextView) vi.findViewById(R.id.name);
		name.setText(category.getName());

		int categoryTotalDone = category.getCheckedItemList().size();
		int categoryTotal = category.getItemList().size();
		TextView text = (TextView) vi.findViewById(R.id.info);		
		text.setText("Items completed " + categoryTotalDone + "/" + categoryTotal);
		
		if(categoryTotalDone==categoryTotal) {
			text.setTextColor(Color.GREEN);
		} else {
			text.setTextColor(Color.RED);
		}
		return vi;
	}

}
