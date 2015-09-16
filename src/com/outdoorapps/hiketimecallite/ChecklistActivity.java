package com.outdoorapps.hiketimecallite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.outdoorapps.hiketimecallite.adaptors.ChecklistCategoryRowAdapter;
import com.outdoorapps.hiketimecallite.adaptors.ChecklistItemRowAdapter;
import com.outdoorapps.hiketimecallite.checklist.Category;
import com.outdoorapps.hiketimecallite.database.ChecklistDatabaseOpenHelper;
import com.outdoorapps.hiketimecallite.dialogs.GoProDialog;
import com.outdoorapps.hiketimecallite.dialogs.InstantCheckDialog;
import com.outdoorapps.hiketimecallite.files.CSVExporter;
import com.outdoorapps.hiketimecallite.files.CSVParser;
import com.outdoorapps.hiketimecallite.support.ImportExport.ImportExportType;
import com.outdoorapps.hiketimecallite.support.SplitActionBarStyle;
import com.outdoorapps.hiketimecallite.support.Storage;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

public class ChecklistActivity extends FragmentActivity implements
ActionBar.OnNavigationListener,
OnItemClickListener,
OnCreateContextMenuListener,
InstantCheckDialog.InstantCheckDialogListener {

	// Make variable static to save through screen rotation
	private static ChecklistCategoryRowAdapter adapter;
	private static ChecklistItemRowAdapter itemAdapter;
	private static Integer currentCategory; // null if it is at main category screen
	private static String listTitle;
	private static ArrayList<Category> categoryList;
	private TextView titleView, totalItemsView;
	private ListView list;
	private boolean doubleBackToExitPressedOnce;
	private MenuItem newCategoryButton, newItemButton, 
	changeCheckListTitleButton, sortAlphabeticallyButton;
	private static int itemIndex;
	private static final String[] contextMenuItems = {"Move Up","Move Down","Edit","Delete"};
	private static final String ADD_TAG = "add";
	private static final String EDIT_TAG = "edit";
	private static final String TITLE_TAG = "title";
	private static ChecklistDatabaseOpenHelper checklistDBHandler;

	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_checklist);
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getActionBarThemedContextCompat(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, MainActivity.dropdownMenuItems), this);
		actionBar.setSelectedNavigationItem(4);		
		setupActionBar();
		
		list =(ListView)findViewById(android.R.id.list);
		list.setOnItemClickListener(this);
		list.setOnCreateContextMenuListener(this);
		registerForContextMenu(list);
		titleView = (TextView) this.findViewById(R.id.title);
		totalItemsView = (TextView) this.findViewById(R.id.total_items);

		if(checklistDBHandler==null) {
			checklistDBHandler = new ChecklistDatabaseOpenHelper(this);
		}
		
		checkFirstTimeUsing();
		if(categoryList==null) // Load from database
			loadFromDatabase();

		// Set the correct adaptor
		if(currentCategory==null) { // in a category
			displayCheckList();
		} else {
			Category category = categoryList.get(currentCategory);
			itemAdapter = new ChecklistItemRowAdapter(this,category.getItemList(),category.getCheckedItemList());			
			list.setAdapter(itemAdapter);
			setCategoryItems();
		}
		displayTitle();		
	}

	/**
	 * Display a checklist after currentCategory and listTitle are loaded
	 */
	private void displayCheckList() {
		adapter = new ChecklistCategoryRowAdapter(this,categoryList);
		list.setAdapter(adapter);
		displayTitle();
		setTotalItems();
	}

	@Override
	public void onResume() {
		super.onResume();			
	}	

	@Override
	public void onPause() {
		super.onPause();
		saveToDatabase();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.checklist, menu);
		newCategoryButton = menu.findItem(R.id.new_category);
		newItemButton = menu.findItem(R.id.new_item);
		sortAlphabeticallyButton = menu.findItem(R.id.sort_alphabetically);
		changeCheckListTitleButton = menu.findItem(R.id.change_checklist_title);
		changeButtonState();
		return true;
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}
	
	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		Intent intent;
		switch(position) {		
		case 0: // Map
			finish();
			break;
		case 1: // Select Route 
			intent = new Intent(this,SelectRouteActivity.class);
			finish();
			startActivityForResult(intent, RequestCode.SELECT_ROUTE);
			break;
		case 2: // Select Track
			intent = new Intent(this,SelectTrackActivity.class);
			finish();
			startActivityForResult(intent, RequestCode.SELECT_TRACK);
			break;
		case 3: // simple calculator
			intent = new Intent(this,SimpleCalculatorActivity.class);
			finish();
			startActivityForResult(intent, RequestCode.SIMPLE_CALCULATOR);
			break;
		case 4: // checklist
			break;
		default:
			break;
		}
		return true;
	}

	private void changeButtonState() {
		if(currentCategory==null) {
			newCategoryButton.setVisible(true);
			newItemButton.setVisible(false);
			sortAlphabeticallyButton.setEnabled(false);
			changeCheckListTitleButton.setVisible(true);
		} else {
			newCategoryButton.setVisible(false);
			newItemButton.setVisible(true);
			sortAlphabeticallyButton.setEnabled(true);
			changeCheckListTitleButton.setVisible(false);
		}
		Drawable sortAlphabeticallyIcon = getResources().getDrawable(R.drawable.ic_sort_alphabetically);
		if(sortAlphabeticallyButton.isEnabled()==false)
			sortAlphabeticallyIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		sortAlphabeticallyButton.setIcon(sortAlphabeticallyIcon);
	}

	@SuppressLint("NewApi")
	private void setupActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			SplitActionBarStyle.setSplitActionBarOverlayColor(actionBar);

			// Calculate ActionBar height
			TypedValue tv = new TypedValue();
			if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)){
				int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
				ListView list =(ListView)findViewById(android.R.id.list);
				list.setPadding(0, 0, 0, actionBarHeight);
			}			
		}
	}

	private void setTotalItems() {
		int totalItems = 0;
		int checkedItems = 0;
		for(int i=0;i<categoryList.size();i++) {
			totalItems += categoryList.get(i).getItemList().size();
			checkedItems += categoryList.get(i).getCheckedItemList().size();
		}
		totalItemsView.setText("Total Items Completed: "+checkedItems+"/"+totalItems);
		if(totalItems==checkedItems) {
			totalItemsView.setTextColor(Color.GREEN);
		} else {
			totalItemsView.setTextColor(Color.RED);
		}
	}

	private void setCategoryItems() {
		Category category = categoryList.get(currentCategory);
		String name = category.getName();
		titleView.setText(name);
		int checkedItems = category.getCheckedItemList().size();
		int totalItems = category.getItemList().size();
		totalItemsView.setText("Items Completed: "+checkedItems+"/"+totalItems);
		if(totalItems==checkedItems) {
			totalItemsView.setTextColor(Color.GREEN);
		} else {
			totalItemsView.setTextColor(Color.RED);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if(currentCategory==null) { // in the main category screen
			currentCategory = position;
			Category category = categoryList.get(currentCategory);
			itemAdapter = new ChecklistItemRowAdapter(this,category.getItemList(),category.getCheckedItemList());
			list.setAdapter(itemAdapter);
		} else { // in a certain Category
			CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkBox);
			checkBox.toggle();
			Category category = categoryList.get(currentCategory);
			if(checkBox.isChecked())
				category.addCheckedItem(position);
			else {
				if(checkBox.isChecked()==false)
					category.removeCheckedItem(position);
			}
			// Update menu items based on the number of checked items
			itemAdapter.notifyDataSetChanged();
			invalidateOptionsMenu();
		}
		changeButtonState();
		setCategoryItems();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId()==android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
			String title;
			if(currentCategory==null)
				title = categoryList.get(info.position).getName();
			else
				title = categoryList.get(currentCategory).getItemList().get(info.position);
			menu.setHeaderTitle(title);
			for (int i = 0; i<contextMenuItems.length; i++) {
				menu.add(Menu.NONE, i, i, contextMenuItems[i]);
			}
		}
	}

	private void moveItemUp(int itemIndex) {
		if(currentCategory==null) { // Move a category
			if(itemIndex>0) {
				Category category = categoryList.get(itemIndex);
				categoryList.remove(itemIndex);
				categoryList.add(itemIndex-1, category);
				adapter.notifyDataSetChanged();
			}
		} else { // Move an item
			if(itemIndex>0) {
				Category category = categoryList.get(currentCategory);
				ArrayList<String> itemList = category.getItemList();
				String itemName = itemList.get(itemIndex);
				// Save checked item names
				ArrayList<Integer> checkedItemList = category.getCheckedItemList();
				ArrayList<String> checkedItemNameList = new ArrayList<String>();
				for(int i=0;i<checkedItemList.size();i++) {
					int index = checkedItemList.get(i);
					String name = itemList.get(index);
					checkedItemNameList.add(name);
				}		

				// Move item up
				itemList.remove(itemIndex);
				itemList.add(itemIndex-1, itemName);
				// re-add checked item index
				checkedItemList.clear();
				for(int i=0;i<checkedItemNameList.size();i++) {
					String name = checkedItemNameList.get(i);
					int index = itemList.indexOf(name);
					checkedItemList.add(index);
				}
				itemAdapter.notifyDataSetChanged();
			}
		}
	}

	private void moveItemDown(int itemIndex) {
		if(currentCategory==null) { // Move a category
			if(itemIndex<categoryList.size()-1) {
				Category category = categoryList.get(itemIndex);
				if(itemIndex==categoryList.size()-2) { // Special case: move to last
					categoryList.remove(itemIndex);
					categoryList.add(category);
				} else {
					categoryList.remove(itemIndex);
					categoryList.add(itemIndex+1, category);
				}
				adapter.notifyDataSetChanged();
			}
		} else {
			Category category = categoryList.get(currentCategory);
			ArrayList<String> itemList = category.getItemList();
			if(itemIndex<itemList.size()-1) {			
				String itemName = itemList.get(itemIndex);
				// Save checked item names
				ArrayList<Integer> checkedItemList = category.getCheckedItemList();
				ArrayList<String> checkedItemNameList = new ArrayList<String>();
				for(int i=0;i<checkedItemList.size();i++) {
					int index = checkedItemList.get(i);
					String name = itemList.get(index);
					checkedItemNameList.add(name);
				}		

				// Move item down
				if(itemIndex==itemList.size()-2) { // Special case: move to last
					itemList.remove(itemIndex);
					itemList.add(itemName);
				} else {
					itemList.remove(itemIndex);
					itemList.add(itemIndex+1, itemName);
				}
				// re-add checked item index
				checkedItemList.clear();
				for(int i=0;i<checkedItemNameList.size();i++) {
					String name = checkedItemNameList.get(i);
					int index = itemList.indexOf(name);
					checkedItemList.add(index);
				}
				itemAdapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		itemIndex = info.position;
		int menuItemIndex = item.getItemId();
		InstantCheckDialog instantCheckDialog;
		String itemName;
		switch(menuItemIndex) {
		case 0: // Move Up
			moveItemUp(info.position);
			break;
		case 1: // Move Down
			moveItemDown(info.position);
			break;
		case 2: // Edit
			if(currentCategory==null) {
				ArrayList<String> categoryNameList = new ArrayList<String>();
				for(int i=0;i<categoryList.size();i++) {
					String name = categoryList.get(i).getName();
					categoryNameList.add(name);
				}
				// Show dialog
				instantCheckDialog = new InstantCheckDialog();
				instantCheckDialog.show(getSupportFragmentManager(), EDIT_TAG);
				instantCheckDialog.setTitle(getString(R.string.edit_category));
				itemName = categoryList.get(itemIndex).getName();
				instantCheckDialog.setInitialName(itemName);
				instantCheckDialog.setCompareList(categoryNameList,false);
			} else {
				// Create compareList
				ArrayList<String> itemNameList = new ArrayList<String>();
				ArrayList<String> itemList = categoryList.get(currentCategory).getItemList();
				for(int i=0;i<itemList.size();i++) {
					String name = itemList.get(i);
					itemNameList.add(name.toLowerCase(Locale.getDefault()));
				}
				// Show dialog
				instantCheckDialog = new InstantCheckDialog();
				instantCheckDialog.show(getSupportFragmentManager(), EDIT_TAG);
				instantCheckDialog.setTitle(getString(R.string.edit_item));
				itemName = categoryList.get(currentCategory).getItemList().get(itemIndex);
				instantCheckDialog.setInitialName(itemName);
				instantCheckDialog.setCompareList(itemNameList,false);
			}
			break;
		case 3: // Delete
			if(currentCategory==null) {
				new AlertDialog.Builder(this)
				.setTitle(R.string.remove_category)
				.setMessage(R.string.remove_category_message)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						categoryList.remove(itemIndex);							
						adapter.notifyDataSetChanged();
						setTotalItems();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// do nothing
					}
				}).show();

			} else {
				itemName = categoryList.get(currentCategory).getItemList().get(itemIndex);
				categoryList.get(currentCategory).removeItem(itemName);
				itemAdapter.notifyDataSetChanged();
				setCategoryItems();
			}
			break;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		if(currentCategory!=null) {
			// Exit to category screen (if in a category)
			backToMainCategoryScreen();
			return;
		} else {
			// Double press to return to main (if in checklist main)
			if (doubleBackToExitPressedOnce) {
				super.onBackPressed();
				return;
			}

			this.doubleBackToExitPressedOnce = true;
			Toast.makeText(this, "Press back again to return to main screen", Toast.LENGTH_SHORT).show();
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					doubleBackToExitPressedOnce=false; 
				}
			}, 2000);
		}
	}

	private void backToMainCategoryScreen() {
		currentCategory = null;
		adapter.notifyDataSetChanged();
		list.setAdapter(adapter);
		titleView.setText(listTitle);
		setTotalItems();
		changeButtonState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		InstantCheckDialog instantCheckDialog;
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.open_checklist:
			intent = new Intent(this,GeneralImportActivity.class);
			intent.putExtra(ExtraTypes.IMPORT_TYPE, ImportExportType.checklist.toString());
			intent.putExtra(ExtraTypes.FILE_EXTENSION, ".csv");
			intent.putExtra(ExtraTypes.PREFERRED_START_FOLDER, Storage.CHECKLISTS_FOLDER);
			startActivityForResult(intent, RequestCode.IMPORT_GENERAL);
			break;
		case R.id.save_checklist:
			GoProDialog goProDialog_export = new GoProDialog();
			goProDialog_export.setFunctionName("Save Checklist");
			goProDialog_export.show(getSupportFragmentManager(), "go_pro_export");
			break;
		case R.id.sort_alphabetically:
			sort();
			break;
		case R.id.new_category:
			// Create compareList
			ArrayList<String> categoryNameList = new ArrayList<String>();
			for(int i=0;i<categoryList.size();i++) {
				String name = categoryList.get(i).getName();
				categoryNameList.add(name.toLowerCase(Locale.getDefault()));
			}
			// Show dialog
			instantCheckDialog = new InstantCheckDialog();
			instantCheckDialog.show(getSupportFragmentManager(), ADD_TAG);
			instantCheckDialog.setTitle(getString(R.string.new_category));
			instantCheckDialog.setCompareList(categoryNameList,false);
			break;
		case R.id.new_item:
			// Create compareList
			ArrayList<String> itemNameList = new ArrayList<String>();
			ArrayList<String> itemList = categoryList.get(currentCategory).getItemList();
			for(int i=0;i<itemList.size();i++) {
				String name = itemList.get(i);
				itemNameList.add(name.toLowerCase(Locale.getDefault()));
			}
			// Show dialog
			instantCheckDialog = new InstantCheckDialog();
			instantCheckDialog.show(getSupportFragmentManager(), ADD_TAG);
			instantCheckDialog.setTitle(getString(R.string.new_item));
			instantCheckDialog.setCompareList(itemNameList,false);
			break;
		case R.id.change_checklist_title:
			instantCheckDialog = new InstantCheckDialog();
			instantCheckDialog.show(getSupportFragmentManager(), TITLE_TAG);
			instantCheckDialog.setTitle(getString(R.string.change_checklist_title));
			instantCheckDialog.setCompareList(null,false);
			break;
		case R.id.selectAll:
			new AlertDialog.Builder(this)
			.setTitle(R.string.select_all_title)
			.setMessage(R.string.select_all_message)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					if(currentCategory==null) { // select all items in all categories
						for(int i=0;i<categoryList.size();i++) {
							Category category = categoryList.get(i);
							category.selectAll();
						}
						adapter.notifyDataSetChanged();
						setTotalItems();
					} else { // select all items in the present category
						Category category = categoryList.get(currentCategory);
						category.selectAll();
						itemAdapter.notifyDataSetChanged();
						setCategoryItems();
					}
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// do nothing
				}
			}).show();
			break;
		case R.id.deselectAll:
			new AlertDialog.Builder(this)
			.setTitle(R.string.deselect_all_title)
			.setMessage(R.string.deselect_all_message)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					if(currentCategory==null) { // select all items in all categories
						for(int i=0;i<categoryList.size();i++) {
							Category category = categoryList.get(i);
							category.deselectAll();
						}
						adapter.notifyDataSetChanged();
						setTotalItems();
					} else { // select all items in the present category
						Category category = categoryList.get(currentCategory);
						category.deselectAll();
						itemAdapter.notifyDataSetChanged();
						setCategoryItems();
					}
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// do nothing
				}
			}).show();
			break;
		case R.id.help:
			Uri uri = Uri.parse(Links.HELP_LINK); 
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.action_settings:
			intent = new Intent(this,SettingsActivity.class);
			startActivity(intent);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void sort() {
		Category category = categoryList.get(currentCategory);
		ArrayList<String> itemList = category.getItemList();

		// Save checked item names
		ArrayList<Integer> checkedItemList = category.getCheckedItemList();
		ArrayList<String> checkedItemNameList = new ArrayList<String>();
		for(int i=0;i<checkedItemList.size();i++) {
			int index = checkedItemList.get(i);
			String name = itemList.get(index);
			checkedItemNameList.add(name);
		}

		// Case insensitive sort
		Collections.sort(itemList, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
			}
		});

		// re-add checked item index
		checkedItemList.clear();
		for(int i=0;i<checkedItemNameList.size();i++) {
			String name = checkedItemNameList.get(i);
			int index = itemList.indexOf(name);
			checkedItemList.add(index);
		}
		itemAdapter.notifyDataSetChanged();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(resultCode==RESULT_OK && requestCode==RequestCode.IMPORT_GENERAL){
			String filePath = intent.getStringExtra(ExtraTypes.FILE_PATH);
			loadCSV(filePath);
		}
		if(resultCode==RESULT_OK && requestCode==RequestCode.EXPORT_GENERAL){
			String filePath = intent.getStringExtra(ExtraTypes.FILE_PATH);
			exportCSV(filePath);
		}
	}

	private void loadCSV(String filePath) {
		CSVParser parser = new CSVParser();
		try {
			categoryList = parser.parseCSV(filePath);
			listTitle = parser.getTitle();
			backToMainCategoryScreen();
			displayCheckList();
			displayTitle();
		} catch (Exception e) {
			Toast.makeText(this, "Error during import: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void exportCSV(String filePath) {
		CSVExporter exporter = new CSVExporter();
		try {
			exporter.exportCSV(filePath, listTitle, categoryList);
		} catch (Exception e) {
			e.printStackTrace();//
			Toast.makeText(this, "Error during export: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void displayTitle() {
		if(listTitle==null)
			titleView.setText("<No Title>");
		else {
			if(listTitle.equals(""))
				titleView.setText("<No Title>");
			else
				titleView.setText(listTitle);
		}
	}

	private void saveToDatabase() {
		SQLiteDatabase db = checklistDBHandler.getWritableDatabase();

		// Clear Database
		db.delete(ChecklistDatabaseOpenHelper.CHECKLIST_TITLE_TABLE_NAME, null, null);
		db.delete(ChecklistDatabaseOpenHelper.CHECKLIST_CATEGORY_TABLE_NAME, null, null);
		db.delete(ChecklistDatabaseOpenHelper.CHECKLIST_TABLE_NAME, null, null);

		// Add Title
		ContentValues values1 = new ContentValues(); 
		values1.put("TITLE", listTitle);
		db.insert(ChecklistDatabaseOpenHelper.CHECKLIST_TITLE_TABLE_NAME, null, values1);

		// Add Categories
		for(int i=0;i<categoryList.size();i++) {
			Category category = categoryList.get(i);
			ContentValues values2 = new ContentValues();
			String categoryName = category.getName();
			values2.put("CATEGORY", categoryName);
			db.insert(ChecklistDatabaseOpenHelper.CHECKLIST_CATEGORY_TABLE_NAME, null, values2);

			// Add Items
			ArrayList<String> itemList = category.getItemList();
			ArrayList<Integer> checkedItemList = category.getCheckedItemList();
			for(int j=0;j<itemList.size();j++) {
				int checked = 0;
				if(checkedItemList.contains(j))
					checked = 1;
				ContentValues values3 = new ContentValues(); 
				values3.put("CATEGORY", categoryName);
				values3.put("ITEM", itemList.get(j));
				values3.put("CHECKED", checked);
				db.insert(ChecklistDatabaseOpenHelper.CHECKLIST_TABLE_NAME, null, values3);								
			}
		}
		db.close();
	}

	private void loadFromDatabase() {
		SQLiteDatabase db = checklistDBHandler.getReadableDatabase();

		// Retrieves list title
		String sql1 = "SELECT * FROM " + ChecklistDatabaseOpenHelper.CHECKLIST_TITLE_TABLE_NAME + ";";
		Cursor cursor1 = db.rawQuery(sql1, null);		
		if(cursor1.moveToFirst())
			listTitle = cursor1.getString(0);

		// Retrieves categories
		categoryList = new ArrayList<Category>();
		String sql2 = "SELECT * FROM " + ChecklistDatabaseOpenHelper.CHECKLIST_CATEGORY_TABLE_NAME + ";";
		Cursor cursor2 = db.rawQuery(sql2, null);		
		if(cursor2.moveToFirst()) {
			do {
				Category category = new Category(cursor2.getString(0));
				categoryList.add(category);
			} while(cursor2.moveToNext());
		}

		// Retrieves items
		for(int i=0;i<categoryList.size();i++) {
			String categoryName = categoryList.get(i).getName();
			ArrayList<String> itemList = categoryList.get(i).getItemList();
			ArrayList<Integer> checkedItemList = categoryList.get(i).getCheckedItemList();

			Cursor cursor3 = db.query(ChecklistDatabaseOpenHelper.CHECKLIST_TABLE_NAME, 
					new String[]{"ITEM","CHECKED"},"CATEGORY=?", new String[]{categoryName}, 
					null, null,	null);
			if(cursor3.moveToFirst()) {
				int index = 0;
				do {
					String item = cursor3.getString(0);
					int checked = cursor3.getInt(1);
					itemList.add(item);
					if(checked==1)
						checkedItemList.add(index);
					index++;
				} while(cursor3.moveToNext());
			}
		}
		db.close();
	}

	@Override
	public void onDialogPositiveClick(InstantCheckDialog dialog) {
		String tag = dialog.getTag();
		String name = dialog.getName();	
		if(tag.equals(TITLE_TAG)) {
			listTitle = name;
			displayTitle();
		}
		if(currentCategory==null) { // category
			if(tag.equals(ADD_TAG)) {
				Category category = new Category(name);
				categoryList.add(category);	
			}
			if(tag.equals(EDIT_TAG)) {
				Category category = categoryList.get(itemIndex);
				category.setName(name);
			}
			adapter.notifyDataSetChanged();
		} else { // item
			if(tag.equals(ADD_TAG)) {
				Category category = categoryList.get(currentCategory);
				category.addItem(name);
			}
			if(tag.equals(EDIT_TAG)) {
				Category category = categoryList.get(currentCategory);
				category.changeItemName(itemIndex, name);
			}
			itemAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onDialogNegativeClick(InstantCheckDialog dialog) {
		// Do nothing
	}

	private void checkFirstTimeUsing() {
		// Load sample checklist to the database if starting the first time
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);		
		boolean sampleLoaded = prefs.getBoolean(PrefKeys.KEY_SAMPLE_CHECKLIST_COPIED, false);
		if(sampleLoaded==false) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PrefKeys.KEY_SAMPLE_CHECKLIST_COPIED, true);
			editor.commit();
			CSVParser parser = new CSVParser();
			try {
				categoryList = parser.parseCSV(Storage.SAMPLE_CHECKLIST_PATH);
				listTitle = parser.getTitle();
				saveToDatabase();
			} catch (Exception e) {
				Toast.makeText(this, "Error during sample checklist initiation: " + e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}
}
