package com.truthdetector.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class ErrorDialog {

	public static void newErrorDialog(Context context, String title, String message) {
		new AlertDialog.Builder(context).setTitle(title).setMessage(message).setNeutralButton("OK", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				System.exit(0);
			}
		}).setIcon(android.R.drawable.ic_dialog_alert).show();
	}
}
