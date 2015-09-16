package com.outdoorapps.hiketimecallite.checklist;

import java.util.ArrayList;

public class Category {
	private ArrayList<String> itemList;
	private ArrayList<Integer> checkedItemList; /** Save checked item index*/
	private String name; // must be unique

	public Category(String name) {
		itemList = new ArrayList<String>();
		checkedItemList = new ArrayList<Integer>();
		this.name = name;
	}

	public void changeItemName(int itemIndex, String itemName) {
		itemList.remove(itemIndex);
		itemList.add(itemIndex, itemName);
	}

	/**
	 * Add must be unique
	 * @param itemName
	 * @return
	 */
	public boolean addItem(String itemName) {
		if(itemList.contains(itemName)==false)
			return itemList.add(itemName);
		else
			return false;
	}

	/**
	 * Remove if the item exists
	 * @param itemName
	 * @return
	 */
	public boolean removeItem(String itemName) {
		int index = itemList.indexOf(itemName);
		if(index!=-1) {
			int checkedItemIndex = checkedItemList.indexOf(index);
			if(checkedItemIndex!=-1) {
				checkedItemList.remove((Object)index);
				// Shift indexes upwards
				for(int i=0;i<checkedItemList.size();i++) {
					int indexTemp = checkedItemList.get(i);
					if(indexTemp>index) {
						checkedItemList.remove(i);
						checkedItemList.add(i, indexTemp-1);
					}
				}
			}
			return itemList.remove(itemName);
		}
		else
			return false;
	}

	/**
	 * Add must be unique
	 * @param itemIndex
	 * @return
	 */
	public boolean addCheckedItem(int itemIndex) {		
		if(checkedItemList.contains(itemIndex)==false)
			return checkedItemList.add(itemIndex);
		else
			return false;
	}

	/**
	 * Remove if the itemIndex exists
	 * @param itemIndex
	 * @return
	 */
	public boolean removeCheckedItem(int itemIndex) {
		int index = checkedItemList.indexOf(itemIndex);
		if(index!=-1)
			return checkedItemList.remove((Object)itemIndex);		
		else
			return false;
	}

	public void selectAll() {
		checkedItemList.clear();
		for(int i=0;i<itemList.size();i++)
			checkedItemList.add(i);
	}

	public void deselectAll() {
		checkedItemList.clear();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<String> getItemList() {
		return itemList;
	}

	public ArrayList<Integer> getCheckedItemList() {
		return checkedItemList;
	}

	@Override
	public String toString() {
		return name + ": " + itemList;
	}
}
