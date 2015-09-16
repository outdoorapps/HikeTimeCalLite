package com.outdoorapps.hiketimecallite.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;

import com.outdoorapps.hiketimecallite.R;

public class EULADialog extends DialogFragment {
	
	public EULADialogListener mListener;
	
	public interface EULADialogListener {
        public void onDialogPositiveClick(EULADialog dialog);
        public void onDialogNegativeClick(EULADialog dialog);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (EULADialogListener) activity;
    }
    
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.eula_dialog, null);		
		builder.setTitle(R.string.eula)
		.setView(view)
		.setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				mListener.onDialogPositiveClick(EULADialog.this);
			}
		})
		.setNegativeButton(R.string.do_not_agree, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				mListener.onDialogNegativeClick(EULADialog.this);
			}
		});
		return builder.create();
	}
}
