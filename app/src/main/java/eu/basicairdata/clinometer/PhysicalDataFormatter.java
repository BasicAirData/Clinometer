/*
 * PhysicalDataFormatter - Java Class for Android
 * Created by G.Capelli on 18/9/2021
 * This file is part of BasicAirData Clinometer
 *
 * Copyright (C) 2011 BasicAirData
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

import java.util.Locale;

/**
 * A helper Class for the formatting of the physical data.
 * It returns the data formatted basing on the given criteria and on the Preferences.
 */
class PhysicalDataFormatter {

    public static final int UM_DEGREES                    = 0;
    public static final int UM_RADIANS                    = 10;
    public static final int UM_PERCENT                    = 20;

    private final ClinometerApplication clinometerApp = ClinometerApplication.getInstance();

    /**
     * It returns a PhysicalData formatted basing on the given criteria and on the Preferences.
     *
     * @param number The float number to format as Physical Data
     * @return The Physical Data containing number and unit of measurement
     */
    public PhysicalData format(float number) {
        PhysicalData physicalData = new PhysicalData();
        physicalData.value = "";
        physicalData.um = "";

        int format = clinometerApp.getPrefUM();

        physicalData.um = clinometerApp.getString(R.string.um_percent);

        switch (format) {
            case UM_DEGREES:
                physicalData.value = String.format(Locale.getDefault(), "%.1f", number);
                physicalData.um = clinometerApp.getString(R.string.um_degrees);
                break;

            case UM_RADIANS:
                physicalData.value = String.format(Locale.getDefault(), "%.2f", Math.toRadians(number));
                physicalData.um = clinometerApp.getString(R.string.um_radians);
                break;

            case UM_PERCENT:
                float percent;
                if (number == 90) percent = 1000;
                else if (number == -90) percent = -1000;
                else percent = (float) Math.tan(Math.toRadians(number)) * 100.0f;

                if (percent >= 1000) {
                    physicalData.value = ">>";
                    physicalData.um = "";
                } else if (percent <= -1000) {
                    physicalData.value = "<<";
                    physicalData.um = "";
                } else {
                    if (Math.abs(percent) < 100)
                        physicalData.value = String.format(Locale.getDefault(), "%.1f", percent);
                    else physicalData.value = String.format(Locale.getDefault(), "%.0f", percent);
                }
                break;
        }
        return (physicalData);
    }
}