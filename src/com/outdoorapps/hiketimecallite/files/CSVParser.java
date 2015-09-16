package com.outdoorapps.hiketimecallite.files;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.outdoorapps.hiketimecallite.checklist.Category;

public class CSVParser {
	private String title;
	
	public ArrayList<Category> parseCSV(String CSVFile) throws IOException, InvalidChecklistCSVFileException {
		ArrayList<Category> categoryList = new ArrayList<Category>();
		BufferedReader br = new BufferedReader(new FileReader(CSVFile));

		try {
			String line;
			// Use the first line as the title
			if((line = br.readLine()) != null) {
				title = line.replace(",", "");
			}
			
			// Use the second line as category names			
			if((line = br.readLine()) != null) {
				String[] categoryNameList = line.split(",");
				for(int i=0;i<categoryNameList.length;i++) {
					Category category = new Category(categoryNameList[i]);
					categoryList.add(category);
				}
			}
			
			// Add items to categories
			while ((line = br.readLine()) != null) {			
				String[] itemList = line.split(",");
				for(int i=0;i<itemList.length;i++) {
					Category category = categoryList.get(i);
					if(!itemList[i].equals(""))
						category.addItem(itemList[i]);
				}			
			}
		} catch(IndexOutOfBoundsException e) {
			throw new InvalidChecklistCSVFileException();
		} finally {
			br.close();
		}		
		return categoryList;
	}
	
	public String getTitle() {
		return title;
	}

	@SuppressWarnings("serial")
	public class InvalidChecklistCSVFileException extends Exception {
	}
}
