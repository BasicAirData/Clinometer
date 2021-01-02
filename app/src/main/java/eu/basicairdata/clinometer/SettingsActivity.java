/*
 * SettingsActivity - Java Class for Android
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

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelayout_settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {

        Preference preferenceAbout;
        Preference preferenceCalibration;
        Preference preferenceResetCalibration;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            preferenceAbout = findPreference("prefAbout");
            preferenceAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    FragmentAboutDialog aboutDialog = new FragmentAboutDialog();

                    aboutDialog.show(fm, "");
                    return false;
                }
            });

            preferenceResetCalibration = findPreference("prefResetCalibration");
            preferenceResetCalibration.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preferences.contains("prefCalibrationAngle0")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage(getResources().getString(R.string.reset_calibration_confirmation));
                        builder.setIcon(android.R.drawable.ic_menu_info_details);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.remove("prefCalibrationAngle0");
                                editor.remove("prefCalibrationAngle1");
                                editor.remove("prefCalibrationAngle2");
                                editor.remove("prefCalibrationGain0");
                                editor.remove("prefCalibrationGain1");
                                editor.remove("prefCalibrationGain2");
                                editor.remove("prefCalibrationOffset0");
                                editor.remove("prefCalibrationOffset1");
                                editor.remove("prefCalibrationOffset2");
                                editor.remove("prefCalibrationTime");
                                editor.commit();

                                updatePreferences();
                            }
                        });
                        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                    return false;
                }
            });
        }


        @Override
        public void onResume() {
            super.onResume();
            updatePreferences();
        }


        private void updatePreferences() {
            preferenceCalibration = findPreference("prefCalibration");

            SimpleDateFormat dfdt = new SimpleDateFormat("dd LLL yyyy HH:mm");        // date and time formatter for timestamp
            //dfdtGPX.setTimeZone(TimeZone.getTimeZone("GMT"));

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (preferences.contains("prefCalibrationAngle0")) {
                preferenceCalibration.setSummary(getResources().getString(R.string.pref_calibration_summary_calibrated) + " ("
                                + dfdt.format(preferences.getLong("prefCalibrationTime", 0)) + ")");
//                preferenceCalibration.setSummary(getResources().getString(R.string.pref_calibration_summary_calibrated)
//                        + String.format(" (%1.2f°; %1.2f°; %1.2f°)", preferences.getFloat("prefCalibrationAngle0", 0),
//                        preferences.getFloat("prefCalibrationAngle1", 0), preferences.getFloat("prefCalibrationAngle2", 0)));
                preferenceResetCalibration.setEnabled(true);
            } else {
                preferenceCalibration.setSummary(getResources().getString(R.string.pref_calibration_summary_notcalibrated));
                preferenceResetCalibration.setEnabled(false);
            }
        }
    }
}