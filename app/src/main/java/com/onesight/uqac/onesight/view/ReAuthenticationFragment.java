package com.onesight.uqac.onesight.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.onesight.uqac.onesight.R;

public class ReAuthenticationFragment extends DialogFragment
{
    private IReAuthenticationListener iReAuthenticationListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater li = LayoutInflater.from(getActivity());

        View promptsView = li.inflate(R.layout.password_prompt, null);

        final EditText userInput = promptsView
                .findViewById(R.id.password_prompt);

        builder.setView(promptsView)
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (iReAuthenticationListener != null)
                                {
                                    iReAuthenticationListener.
                                            onPasswordEntered(userInput.getText().toString());
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                iReAuthenticationListener.onPasswordEntered(null);
                                Log.d("AuthenticationFragment",
                                        "Cancelled re-authentication.");
                            }
                        });

        return builder.create();
    }

    public void setIReAuthenticationListener(
            ReAuthenticationFragment.IReAuthenticationListener iReAuthenticationListener)
    {
        this.iReAuthenticationListener = iReAuthenticationListener;
    }

    public interface IReAuthenticationListener {
        void onPasswordEntered(String userInput);
    }
}
