package com.outdoorapps.hiketimecallite.files;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.outdoorapps.hiketimecallite.checklist.Category;

public class CSVExporter {

	public void exportCSV(String path, String title, ArrayList<Category> categoryList) 
			throws IOException {
		FileWriter fw = new FileWriter(path);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter out = new PrintWriter (bw);

		int categoryNum = categoryList.size();

		// Print title line
		out.print(title);
		for(int i=0;i<categoryNum;i++) {
			out.print(",");
		}
		out.println();
		
		// Print category names
		if(categoryNum>0) {
			out.print(categoryList.get(0).getName());
			for(int i=1;i<categoryNum;i++) {
				out.print(","+categoryList.get(i).getName());
			}
			
			// Print items
			int maxSize = 0;
			for(int i=0;i<categoryNum;i++) {
				Category category = categoryList.get(i);
				int size = category.getItemList().size();
				if(size>maxSize)
					maxSize = size;
			}
			
			Category category0 = categoryList.get(0);
			ArrayList<String> itemList0 = category0.getItemList();
			for(int row=0;row<maxSize;row++) {
				// Print the first item of each line
				out.println();
				if(itemList0.size()>row)
					out.print(itemList0.get(row));					
				
				for(int col=1;col<categoryNum;col++) {
					String item;
					Category category = categoryList.get(col);
					try {
						item = category.getItemList().get(row);
					} catch(IndexOutOfBoundsException e) {
						item = "";
					}
					out.print(","+item);
				}
			}
		}
		out.close();
	}
}
