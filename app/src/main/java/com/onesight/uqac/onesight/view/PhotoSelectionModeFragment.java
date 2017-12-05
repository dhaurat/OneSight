package com.onesight.uqac.onesight.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.app.AlertDialog;

import com.onesight.uqac.onesight.R;

public class PhotoSelectionModeFragment extends DialogFragment {

        private String[] picMode = {"", ""};

        private IPicModeSelectListener iPicModeSelectListener;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            if (isAdded())
            {
                picMode[0] = getResources().getString(R.string.camera);
                picMode[1] = getResources().getString(R.string.gallery);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(picMode, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (iPicModeSelectListener != null)
                        iPicModeSelectListener.onPicModeSelected(picMode[which]);
                }
            });
            return builder.create();
        }

        public void setIPicModeSelectListener(IPicModeSelectListener iPicModeSelectListener) {
            this.iPicModeSelectListener = iPicModeSelectListener;
        }

        public interface IPicModeSelectListener {
            void onPicModeSelected(String mode);
        }
}
