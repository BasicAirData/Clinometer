/*
 * SettingsActivity - Java Class for Android
 * Created by G.Capelli on 2/6/2020
 * This file is part of BasicAirData Clinometer
 *
 * Copyright (C) 2020 BasicAirData
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

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static eu.basicairdata.clinometer.ClinometerApplication.CAMERA_REQUEST_CODE;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_ABOUT;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_ANGLE_0;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_ANGLE_1;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_ANGLE_2;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_GAIN_0;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_GAIN_1;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_GAIN_2;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_OFFSET_0;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_OFFSET_1;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_OFFSET_2;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_RESET;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_TIME;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CAMERA;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CAMERA_EXPOSURE_COMPENSATION;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CAMERA_PERMISSION;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_COLOR_THEME;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_KEEP_SCREEN_ON;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_ONLINE_HELP;


public class SettingsActivity extends AppCompatActivity {

    private ClinometerApplication clinometerApplication;
    SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        clinometerApplication = ClinometerApplication.getInstance();

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelayout_settings, settingsFragment)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        clinometerApplication.scanCameras();
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_PREF_KEEP_SCREEN_ON, true)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            Log.w("SettingsActivity", "onRequestPermissionsResult()");
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                clinometerApplication.scanCameras();
                settingsFragment.updatePreferences();
            }
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {

        private ClinometerApplication clinometerApplication;

        SharedPreferences preferences;
        SharedPreferences.OnSharedPreferenceChangeListener listener;

        ListPreference preferenceCameraToUse;
        ListPreference preferenceColorTheme;
        SeekBarPreference preferenceExposureCompensation;
        Preference preferenceCalibration;
        Preference preferenceAbout;
        Preference preferenceOnlineHelp;
        Preference preferenceCameraPermission;
        Preference preferenceResetCalibration;


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            clinometerApplication = ClinometerApplication.getInstance();
            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    Log.d("SettingsActivity", "onSharedPreferenceChanged");
                    if (key.equals(KEY_PREF_CAMERA)) {
                        clinometerApplication.setSelectedCamera(Integer.parseInt(preferenceCameraToUse.getValue()));
                        setupCameraPreference();
                        setupCompensationPreference();
                    }
                    if (key.equals(KEY_PREF_KEEP_SCREEN_ON)) {
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(KEY_PREF_KEEP_SCREEN_ON, true)) getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        else getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    if (key.equals(KEY_PREF_COLOR_THEME)) {
                        boolean isDarkMode = prefs.getBoolean(KEY_PREF_COLOR_THEME, true);
                        // Apply dark or light theme based on switch state
                        if (isDarkMode) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Force dark theme
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Force light theme
                        }

                        // Apply the correct theme based on the preference before setting content view
                        if (isDarkMode) {
                            getActivity().setTheme(R.style.AppTheme); // Your dark theme
                        } else {
                            getActivity().setTheme(R.style.AppTheme_Light); // Your light theme
                        }
                        // Recreate activity to apply the theme change
                        getActivity().recreate();
                        getActivity().finish();
                    }
                }
            };

            preferenceCameraToUse = findPreference(KEY_PREF_CAMERA);




            preferenceCameraPermission = findPreference(KEY_PREF_CAMERA_PERMISSION);
            preferenceCameraPermission.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                    } else {
                        setupCameraPreference();
                        setupCompensationPreference();
                    }
                    return false;
                }
            });

            preferenceExposureCompensation = findPreference(KEY_PREF_CAMERA_EXPOSURE_COMPENSATION);
            preferenceCalibration = findPreference(KEY_PREF_CALIBRATION);

            preferenceAbout = findPreference(KEY_PREF_ABOUT);
            preferenceAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    FragmentAboutDialog aboutDialog = new FragmentAboutDialog();

                    aboutDialog.show(fm, "");
                    return false;
                }
            });

            preferenceOnlineHelp = findPreference(KEY_PREF_ONLINE_HELP);
            preferenceOnlineHelp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        // Opens the default browser and shows the Getting Started Guide page
                        String url = "https://www.basicairdata.eu/projects/android/android-clinometer/";
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    } catch (Exception e){
                        Toast toast = Toast.makeText(getActivity().getApplicationContext(), R.string.toast_no_browser_installed, Toast.LENGTH_LONG);
                        toast.show();
                    }
                    return false;
                }
            });

            preferenceResetCalibration = findPreference(KEY_PREF_CALIBRATION_RESET);
            preferenceResetCalibration.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preferences.contains(KEY_PREF_CALIBRATION_ANGLE_0)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage(getResources().getString(R.string.dialog_reset_calibration_confirmation));
                        builder.setIcon(android.R.drawable.ic_menu_info_details);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.remove(KEY_PREF_CALIBRATION_ANGLE_0);
                                editor.remove(KEY_PREF_CALIBRATION_ANGLE_1);
                                editor.remove(KEY_PREF_CALIBRATION_ANGLE_2);
                                editor.remove(KEY_PREF_CALIBRATION_GAIN_0);
                                editor.remove(KEY_PREF_CALIBRATION_GAIN_1);
                                editor.remove(KEY_PREF_CALIBRATION_GAIN_2);
                                editor.remove(KEY_PREF_CALIBRATION_OFFSET_0);
                                editor.remove(KEY_PREF_CALIBRATION_OFFSET_1);
                                editor.remove(KEY_PREF_CALIBRATION_OFFSET_2);
                                editor.remove(KEY_PREF_CALIBRATION_TIME);
                                for (int i = 0; i < 7; i++) {
                                    editor.remove("prefCalibrationRawMean_0_" + i);
                                    editor.remove("prefCalibrationRawMean_1_" + i);
                                    editor.remove("prefCalibrationRawMean_2_" + i);
                                }
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
        public void onPause() {
            super.onPause();
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void onResume() {
            super.onResume();
            preferences.registerOnSharedPreferenceChangeListener(listener);
            updatePreferences();
        }


        private void updatePreferences() {
            SimpleDateFormat dfdt = new SimpleDateFormat("dd LLL yyyy HH:mm");        // date and time formatter for timestamp
            //dfdtGPX.setTimeZone(TimeZone.getTimeZone("GMT"));

            if (preferences.contains(KEY_PREF_CALIBRATION_ANGLE_0)) {
                preferenceCalibration.setSummary(getResources().getString(R.string.pref_calibration_summary_calibrated) + " ("
                                + dfdt.format(preferences.getLong(KEY_PREF_CALIBRATION_TIME, 0)) + ")");
                preferenceResetCalibration.setEnabled(true);
            } else {
                preferenceCalibration.setSummary(getResources().getString(R.string.pref_calibration_summary_notcalibrated));
                preferenceResetCalibration.setEnabled(false);
            }
            setupCameraPreference();
            setupCompensationPreference();
        }


        private void setupCameraPreference() {
            preferenceCameraToUse.setVisible(true);
            preferenceCameraPermission.setVisible(false);
            preferenceExposureCompensation.setVisible(true);

            if (!clinometerApplication.hasCamera()) {
                // The Device has NO Cameras
                preferenceCameraToUse.setEnabled(false);
                preferenceCameraToUse.setSummary(getString(R.string.pref_cameramode_no_cameras));
                preferenceExposureCompensation.setEnabled(false);
                preferenceExposureCompensation.setVisible(false);
            } else {
                if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    // Camera present
                    ArrayList<CameraInformation> ci = clinometerApplication.getListOfCameraInformation();
                    if (ci.isEmpty()) {
                        // The Device has NO Cameras
                        preferenceCameraToUse.setEnabled(false);
                        preferenceCameraToUse.setSummary(getString(R.string.pref_cameramode_no_cameras));
                        preferenceExposureCompensation.setEnabled(false);
                        return;
                    }
                    List<String> ci_entries = new ArrayList<>();
                    List<String> ci_values = new ArrayList<>();
                    for (CameraInformation c : ci) {
                        ci_entries.add(c.description + (c.horizontalViewAngle < 5.0f ? "" : " (" + String.format("%.0f", c.horizontalViewAngle) + "°)"));
                        ci_values.add(String.valueOf(c.id));
                    }

                    preferenceCameraToUse.setEntries(ci_entries.toArray(new CharSequence[ci_entries.size()]));
                    preferenceCameraToUse.setEntryValues(ci_values.toArray(new CharSequence[ci_values.size()]));
                    preferenceCameraToUse.setEnabled(true);
                } else {
                    // Camera present, but no camera permission granted
                    preferenceCameraToUse.setVisible(false);
                    preferenceCameraPermission.setVisible(true);
                    preferenceExposureCompensation.setVisible(false);
                }
            }
        }


        private void setupCompensationPreference() {
            if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                ArrayList<CameraInformation> ci = clinometerApplication.getListOfCameraInformation();

                if (!ci.isEmpty()) {
                    CameraInformation selCameraInformation = ci.get(Integer.parseInt(preferenceCameraToUse.getValue()));
                    int compensationRange = selCameraInformation.maxExposureCompensation - selCameraInformation.minExposureCompensation;
                    int compensation = preferenceExposureCompensation.getValue();

                    Log.d("SettingsActivity", "Selected Camera " + selCameraInformation.id + " (compensation Range = " + compensationRange + ")");

                    if (compensationRange > 0) {
                        // Exposure Compensation feature is present
                        preferenceExposureCompensation.setEnabled(true);

                        preferenceExposureCompensation.setMin(selCameraInformation.minExposureCompensation);
                        preferenceExposureCompensation.setMax(selCameraInformation.maxExposureCompensation);

                        // Check if the Exposure Compensation set is out of range. In case modify it
                        if (compensation > selCameraInformation.maxExposureCompensation)
                            preferenceExposureCompensation.setValue(selCameraInformation.maxExposureCompensation);
                        if (compensation < selCameraInformation.minExposureCompensation)
                            preferenceExposureCompensation.setValue(selCameraInformation.minExposureCompensation);
                        if (compensation != preferenceExposureCompensation.getValue()) {
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt(KEY_PREF_CAMERA_EXPOSURE_COMPENSATION, preferenceExposureCompensation.getValue());
                            editor.commit();
                        }
                    } else {
                        preferenceExposureCompensation.setMin(-1);
                        preferenceExposureCompensation.setMax(1);
                        preferenceExposureCompensation.setValue(0);
                        preferenceExposureCompensation.setEnabled(false);
                    }
                }
            }
        }


    }
}