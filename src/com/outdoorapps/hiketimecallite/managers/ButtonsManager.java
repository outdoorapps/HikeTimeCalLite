package com.outdoorapps.hiketimecallite.managers;

import java.util.HashMap;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.MenuItem;

import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;

public class ButtonsManager {
	
	private MainActivity main;
	private MenuItem trackButton, stopTrackingButton, unhideButton, 
	drawButton, importButton, undoButton, redoButton, doneButton, 
	reverseButton, discardButton, showDetailsButton, hideFromMapButton, editRouteButton, 
	exportRouteButton, removeRouteButton, mapLayerButton;
	
	public static final String TRACK_BUTTON = "trackButton";
	public static final String STOP_TRACKING_BUTTON = "stopTrackingButton";
	public static final String UNHIDE_BUTTON = "unhideButton";
	public static final String DRAW_BUTTON = "drawButton";
	public static final String IMPORT_BUTTON = "importButton";
	public static final String UNDO_BUTTON = "undoButton";
	public static final String REDO_BUTTON = "redoButton";
	public static final String DONE_BUTTON = "doneButton";
	public static final String REVERSE_BUTTON = "reverseButton";
	public static final String DISCARD_BUTTON = "discardButton";
	public static final String SHOW_DETAILS_BUTTON = "showDetailsButton";
	public static final String HIDE_FROM_MAP_BUTTON = "hideFromMapButton";
	public static final String EDIT_ROUTE_BUTTON = "editRouteButton";
	public static final String EXPORT_ROUTE_BUTTON = "exportRouteButton";
	public static final String REMOVE_ROUTE_BUTTON = "removeRouteButton";
	public static final String MAP_LAYER_BUTTON = "mapLayerButton";
	
	public ButtonsManager(MainActivity main, HashMap<String,MenuItem> buttonsMap) {
		this.main = main;
		trackButton = buttonsMap.get(TRACK_BUTTON);
		stopTrackingButton = buttonsMap.get(STOP_TRACKING_BUTTON);
		unhideButton = buttonsMap.get(UNHIDE_BUTTON);
		drawButton = buttonsMap.get(DRAW_BUTTON);
		importButton = buttonsMap.get(IMPORT_BUTTON);
		undoButton = buttonsMap.get(UNDO_BUTTON);
		redoButton = buttonsMap.get(REDO_BUTTON);
		doneButton = buttonsMap.get(DONE_BUTTON);
		reverseButton = buttonsMap.get(REVERSE_BUTTON);
		discardButton = buttonsMap.get(DISCARD_BUTTON);
		showDetailsButton = buttonsMap.get(SHOW_DETAILS_BUTTON);
		hideFromMapButton = buttonsMap.get(HIDE_FROM_MAP_BUTTON);
		editRouteButton = buttonsMap.get(EDIT_ROUTE_BUTTON);
		exportRouteButton = buttonsMap.get(EXPORT_ROUTE_BUTTON);
		removeRouteButton = buttonsMap.get(REMOVE_ROUTE_BUTTON);
		mapLayerButton = buttonsMap.get(MAP_LAYER_BUTTON);
	}
	
	public void checkAllButtonSets() {
		if(main.getTracking()) {
			trackButton.setVisible(false);
			stopTrackingButton.setVisible(true);
		} else {
			trackButton.setVisible(true);
			stopTrackingButton.setVisible(false);
		}

		if(main.isDrawing())
			setDrawingButtonSetVisible(true);
		else
			setDrawingButtonSetVisible(false);

		if(main.getMarkerInfo().checkInfoValid())				
			setMapObjectSelectButtonSetVisible(true);
		else
			setMapObjectSelectButtonSetVisible(false);

		if(!(main.isDrawing()) && main.getMarkerInfo().checkInfoValid()==false)
			this.setDefaultButtonSetVisible(true);
		else
			this.setDefaultButtonSetVisible(false);
	}

	public void setDefaultButtonSetVisible(boolean visible) {
		if(visible) {
			unhideButton.setVisible(true);
			drawButton.setVisible(true);
			importButton.setVisible(true);
			updateDefaultButtonsState();
		} else {
			unhideButton.setVisible(false);
			drawButton.setVisible(false);
			importButton.setVisible(false);
		}
	}

	public void setMapObjectSelectButtonSetVisible(boolean visible) {
		if(visible) {
			showDetailsButton.setVisible(true);
			hideFromMapButton.setVisible(true);
			exportRouteButton.setVisible(true);	
			removeRouteButton.setVisible(true);
			if(main.getMarkerInfo().getType()==MapObjectType.route)
				editRouteButton.setVisible(true);
			else
				editRouteButton.setVisible(false);
		} else {
			showDetailsButton.setVisible(false);
			hideFromMapButton.setVisible(false);
			editRouteButton.setVisible(false);
			exportRouteButton.setVisible(false);	
			removeRouteButton.setVisible(false);
		}
	}

	public void setDrawingButtonSetVisible(boolean visible) {
		if(visible) {
			undoButton.setVisible(true);
			redoButton.setVisible(true);
			doneButton.setVisible(true);
			reverseButton.setVisible(true);
			discardButton.setVisible(true);
			mapLayerButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			changeDrawingButtonSetState();
		} else {
			undoButton.setVisible(false);
			redoButton.setVisible(false);
			doneButton.setVisible(false);
			reverseButton.setVisible(false);
			discardButton.setVisible(false);
			mapLayerButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
	}

	public void changeDrawingButtonSetState() {		
		int pointCount = main.getDrawingMode().getPointCount();
		if(pointCount>0)
			undoButton.setEnabled(true);
		else
			undoButton.setEnabled(false);

		if(pointCount<main.getDrawingMode().getPointList().size())
			redoButton.setEnabled(true);
		else
			redoButton.setEnabled(false);

		if(pointCount>1)
			reverseButton.setEnabled(true);
		else
			reverseButton.setEnabled(false);

		Drawable undoIcon = main.getResources().getDrawable(R.drawable.ic_action_undo);
		Drawable redoIcon = main.getResources().getDrawable(R.drawable.ic_action_redo);
		Drawable reverseIcon = main.getResources().getDrawable(R.drawable.ic_reverse);
		if(undoButton.isEnabled()==false)
			undoIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(redoButton.isEnabled()==false)
			redoIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(reverseButton.isEnabled()==false)
			reverseIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		undoButton.setIcon(undoIcon);
		redoButton.setIcon(redoIcon);
		reverseButton.setIcon(reverseIcon);
	}

	/**
	 * Called every time the default button set is enable
	 */
	public void updateDefaultButtonsState() {

	}
}
