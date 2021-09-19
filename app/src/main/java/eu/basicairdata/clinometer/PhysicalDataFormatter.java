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

    // These values must match with arrays.xml <string-array name="UMAnglesValues">
    public static final int UM_DEGREES          = 0;
    public static final int UM_RADIANS          = 10;
    public static final int UM_PERCENT          = 20;
    public static final int UM_FRACTIONAL       = 30;
    public static final int UM_ENGINEERING_1V   = 40;

    private final ClinometerApplication clinometerApp = ClinometerApplication.getInstance();


    /**
     * It converts a double into its string representation as fraction.
     * The standard tolerance for approximation is 1.0E-2.
     *
     * @param x The double number to convert in fraction
     * @return the fraction string
     */
    static private String convertDecimalToFraction(double x){
        double xx = Math.abs(x);
        boolean isNegative = x != Math.abs(x);
        double tolerance = 1.0E-2;
        double h1 = 1;
        double h2 = 0;
        double k1 = 0;
        double k2 = 1;
        double b = xx;
        do {
            double a = Math.floor(b);
            double aux = h1;
            h1 = a * h1 + h2;
            h2 = aux;
            aux = k1;
            k1 = a * k1 + k2;
            k2 = aux;
            b = 1 / (b - a);
        } while (Math.abs(xx - h1 / k1) > xx * tolerance);

        if (k1 > 1000) return "0";
        if (h1 > 1000) return isNegative ? "<<" : ">>";
        return (isNegative ? "-" : "") + String.format(Locale.getDefault(), "%.0f", h1) + "/"
                + String.format(Locale.getDefault(), "%.0f", k1);
    }


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

        switch (format) {
            case UM_DEGREES:
                physicalData.value = String.format(Locale.getDefault(), "%.1f", number);
                physicalData.um = clinometerApp.getString(R.string.um_degrees);
                break;

            case UM_RADIANS:
                physicalData.value = String.format(Locale.getDefault(), "%.2f", Math.toRadians(number));
                physicalData.um = "";
                break;

            case UM_PERCENT:
                physicalData.um = clinometerApp.getString(R.string.um_percent);

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

            case UM_FRACTIONAL:
                physicalData.value = convertDecimalToFraction((float) Math.tan(Math.toRadians(number)));
                break;

            case UM_ENGINEERING_1V:
                physicalData.um = "";
                float eH;
                if (number == 90) eH = 1000;
                else if (number == -90) eH = -1000;
                else eH = (float) Math.tan(Math.toRadians(number));

                if (eH >= 1000) {
                    physicalData.value = ">>";
                } else if (eH <= -1000) {
                    physicalData.value = "<<";
                } else {
                    if (Math.abs(eH) < 10) physicalData.value = String.format(Locale.getDefault(), "%.3f", eH);
                    else if (Math.abs(eH) < 100) physicalData.value = String.format(Locale.getDefault(), "%.2f", eH);
                    else physicalData.value = String.format(Locale.getDefault(), "%.0f", eH);
                }
                break;
        }
        return (physicalData);
    }
}