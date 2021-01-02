/*
 * FragmentAboutDialog - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 2/6/2020
 *
 * This file is part of BasicAirData Clinometer for Android.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.basicairdata.clinometer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class FragmentAboutDialog extends DialogFragment {

    TextView tvVersion;
    TextView tvDescription;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);

        AlertDialog.Builder createAboutAlert = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_about_dialog, null);

        tvVersion = (TextView) view.findViewById(R.id.id_about_textView_Version);
        String versionName = BuildConfig.VERSION_NAME;
        tvVersion.setText(getString(R.string.about_version) + " " + versionName);

        tvDescription = (TextView) view.findViewById(R.id.id_about_textView_description);

        tvDescription.setText(getString(R.string.about_description_googleplaystore));

        createAboutAlert.setView(view).setPositiveButton(R.string.about_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {}
        });


        createAboutAlert.setView(view).setNegativeButton(R.string.about_rate_this_app, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                boolean marketfailed = false;
                try {
                    getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
                } catch (Throwable e) {
                    // Unable to start the Google Play app for rating
                    marketfailed = true;
                }
                if (marketfailed) {
                    try {               // Try with the web browser
                        getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
                    } catch (Throwable e) {
                        // Unable to start also the browser for rating
                        Toast.makeText(getContext(), getString(R.string.about_unable_to_rate), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        return createAboutAlert.create();
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
}