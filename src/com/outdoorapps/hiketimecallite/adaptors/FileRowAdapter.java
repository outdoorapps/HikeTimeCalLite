package com.outdoorapps.hiketimecallite.adaptors;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.support.Storage;

public class FileRowAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<String> fileList;

	public FileRowAdapter(Context context, ArrayList<String> fileList) {
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.fileList = fileList;
	}

	@Override
	public int getCount() {
		return fileList.size();
	}

	@Override
	public String getItem(int index) {
		return fileList.get(index);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		if (vi == null)
			vi = inflater.inflate(R.layout.file_row, null);
		TextView fileView = (TextView) vi.findViewById(R.id.file_name);
		ImageView image = (ImageView) vi.findViewById(R.id.icon_file_object);
		String fileName = getItem(position);
		
		if(position==0) {
			if(fileName.equals(Storage.UP_FOLDER_TEXT)) // up folder item
				image.setImageResource(R.drawable.ic_up_folder);
			else{ // If storage root has been reached, no up folder item (assume there must be a child folder)
				fileName = fileName.replace("/", "");
				image.setImageResource(R.drawable.ic_folder);
			}
		} else {			
			if(fileName.endsWith("/")) { // a folder
				fileName = fileName.replace("/", "");
				image.setImageResource(R.drawable.ic_folder);
			} else {
				image.setImageResource(R.drawable.ic_file);
			}		
		}
		fileView.setText(fileName);
		return vi;
	}
}
