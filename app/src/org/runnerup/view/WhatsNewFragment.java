package org.runnerup.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import org.runnerup.R;

public class WhatsNewFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.whatsnew, null);
        WebView wv = (WebView) view.findViewById(R.id.web_view1);
        builder.setTitle("What's new");
        builder.setView(view);
        builder.setPositiveButton("Rate RunnerUp", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Uri uri = Uri.parse("market://details?id=" + getActivity().getPackageName());
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        wv.loadUrl("file:///android_asset/changes.html");

        return builder.create();
    }
}
