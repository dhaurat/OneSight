package com.onesight.uqac.onesight.view;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.google.firebase.auth.FirebaseUser;
import com.onesight.uqac.onesight.R;


public class DeleteAccountAlertFragment extends DialogFragment
{
    private IDeleteAccountAlertListener iDeleteAccountAlertListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_alert_message)
                .setTitle(R.string.dialog_alert_title)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        iDeleteAccountAlertListener.onAccept();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public void setIDeleteAccountAlertListener(
            DeleteAccountAlertFragment.IDeleteAccountAlertListener iDeleteAccountAlertListener)
    {
        this.iDeleteAccountAlertListener = iDeleteAccountAlertListener;
    }

    public interface IDeleteAccountAlertListener {
        void onAccept();
    }
}
