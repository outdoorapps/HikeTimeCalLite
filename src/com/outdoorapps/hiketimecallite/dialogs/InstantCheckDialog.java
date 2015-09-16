package com.outdoorapps.hiketimecallite.dialogs;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.outdoorapps.hiketimecallite.R;

public class InstantCheckDialog extends DialogFragment {

	private AlertDialog dialog;	
	private Activity caller;	
	private InstantCheckDialogListener mListener;
	private InputMethodManager imm;
	private TextView nameView;
	private EditText nameEdit;
	private String initialName, positiveButtonName, negativeButtonName;
	private static String title, name;
	private static ArrayList<String> compareList;

	public interface InstantCheckDialogListener {
        public void onDialogPositiveClick(InstantCheckDialog dialog);
        public void onDialogNegativeClick(InstantCheckDialog dialog);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(mListener==null) // If the listener has not been defined by setListener
        	mListener = (InstantCheckDialogListener) activity;
        caller = activity;
    }
    
    /**
     * Allow listener to be a non-activity class
     * @param mListener
     */
    public void setListener(InstantCheckDialogListener mListener) {
    	this.mListener = mListener;
    }
	
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.instant_check_dialog, null);

		nameView = (TextView) view.findViewById(R.id.new_name);
		nameEdit = (EditText) view.findViewById(R.id.name_input);		
		nameEdit.addTextChangedListener(new TextChangeListener());
		
		imm = (InputMethodManager)caller.getSystemService(Context.INPUT_METHOD_SERVICE);
		
		builder.setView(view)
		.setTitle(title)
		.setPositiveButton(getPositiveButtonName(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {				
				name = nameEdit.getText().toString();
				imm.hideSoftInputFromWindow(nameEdit.getWindowToken(), 0);
				mListener.onDialogPositiveClick(InstantCheckDialog.this);
			}
		})
		.setNegativeButton(getNegativeButtonName(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				imm.hideSoftInputFromWindow(nameEdit.getWindowToken(), 0);
				mListener.onDialogNegativeClick(InstantCheckDialog.this);
			}
		});
		
		dialog = builder.create();	
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		dialog.show();
		nameEdit.setText(initialName);
		return dialog;
	}
	
	public void setCompareList(ArrayList<String> compareListInput, boolean caseSensitive) {
		if(compareListInput==null)
			compareList = new ArrayList<String>();
		else {
			if(caseSensitive)
				compareList = compareListInput;
			else {
				compareList = new ArrayList<String>();
				for(int i=0;i<compareListInput.size();i++) {
					String name = compareListInput.get(i);
					compareList.add(name.toLowerCase(Locale.getDefault()));
				}
			}
		}
	}

	public void setInitialName(String initialName) {
		this.initialName = initialName;
	}
	
	public String getName() {
		return name;
	}
	
	public void setTitle(String titleInput) {
		title = titleInput;
	}
	
	private class TextChangeListener implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			// name check
			String currentName = s.toString();
			if(currentName.equals("")) {
				dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
				nameView.setText(R.string.new_name);
			}
			else {
				String currentNameLowerCase = currentName.toLowerCase(Locale.getDefault());
				if(compareList.contains(currentNameLowerCase)) {
					dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
					nameView.setText(R.string.new_name_used);
				}
				else {
					dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
					nameView.setText(R.string.new_name);
				}
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}

	}
	
	public void setPositiveButtonName(String positiveButtonName) {
		this.positiveButtonName = positiveButtonName;
	}

	public void setNegativeButtonName(String negativeButtonName) {
		this.negativeButtonName = negativeButtonName;
	}

	private String getPositiveButtonName() {
		if(positiveButtonName==null)
			return getString(R.string.ok);
		else
			return positiveButtonName;
	}
	
	private String getNegativeButtonName() {
		if(negativeButtonName==null)
			return getString(R.string.cancel);
		else
			return negativeButtonName;
	}
}
