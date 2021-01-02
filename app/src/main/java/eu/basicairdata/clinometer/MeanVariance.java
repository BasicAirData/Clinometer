/*
 * MeanVariance - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 3/6/2020
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

import android.util.Log;

import java.util.Arrays;

public class MeanVariance {

    private float[] sample;
    private int currentSample = -1;
    private int loaded = 0;

    private float meanValue = 0;
    private float variance = 0;
    private float stdDeviation = 0;
    private float tolerance = 0;        // Confidence 95%


    public MeanVariance(int size) {
        sample = new float[size];
    }


    public void loadSample(float sample) {
        currentSample++;
        if (currentSample == this.sample.length) {
            currentSample = 0;
            //Log.d("MeanVariance", (String.format("MeanVariance: Mean=%+1.4f Uncertainty=%+1.4f (Std Deviation=%+1.4f)", MeanValue, Tolerance, StdDeviation)));
        }

        this.sample[currentSample] = sample;
        loaded++;

        calculate();
    }


    public void reset() {
        loaded = 0;
        currentSample = -1;

        meanValue = 0;
        variance = 0;
        stdDeviation = 0;
        tolerance = 0;

        Arrays.fill(sample, 0);
    }


    public void reset(float values) {
        loaded = 0;
        currentSample = -1;

        Arrays.fill(sample, values);
        calculate();
    }


    public boolean getLoaded() {
        return (loaded >= sample.length);
    }


    public float percentLoaded() {
        return (Math.min(100 * loaded / sample.length, 100));
    }


    public boolean isReady() {
        return (loaded > 10);
    }


    public float getMeanValue() {
        return meanValue;
    }


    public float getVariance() {
        return variance;
    }


    public float getStdDeviation() {
        return stdDeviation;
    }


    public float getTolerance() {
        return tolerance;
    }


    private void calculate() {
        int nsamples = Math.min(sample.length, loaded);
        if (nsamples > 0) {

            // ------ Mean value
            double mv = 0;
            for (int i = 0; i < nsamples; i++) {
                mv += sample[i];
            }
            mv /= nsamples;
            meanValue = (float) mv;

            // ------ Variance
            double var = 0;
            for (int i = 0; i < nsamples; i++) {
                var += (sample[i] - mv) * (sample[i] - mv);
            }
            variance /= nsamples;

            // ------ Standard Deviation
            stdDeviation = (float) Math.sqrt(var);

            // ------ Uncertainty (confidence 95%)
            tolerance = (float) (1.96d * stdDeviation / Math.sqrt(nsamples));
        } else {
            reset();
        }
    }


    public float getMeanValue(int number_of_last_samples) {
        int nsamples = Math.min(sample.length, loaded);
        if (number_of_last_samples <= 0) return meanValue;
        if (number_of_last_samples >= nsamples) return 0;
        if (nsamples > 0) {
            // ------ Mean value
            double mv = 0;
            int index = (loaded - number_of_last_samples) % nsamples;
            Log.d("MeanVariance", ("MeanVariance: Loaded="+ loaded + " Start=" + index));

            for (int i = 0; i < number_of_last_samples; i++) {
                mv += sample[index];
                index++;
                if (index == nsamples) index = 0;
            }
            mv /= number_of_last_samples;

            Log.d("MeanVariance", (String.format("MeanVariance: Mean=%+1.4f Uncertainty=%+1.4f (Std Deviation=%+1.4f)", meanValue, tolerance, stdDeviation)));
            return (float) mv;
        }
        return 0;
    }


    public float getTolerance(int number_of_last_samples) {
        int nsamples = Math.min(sample.length, loaded);
        if (number_of_last_samples <= 0) return tolerance;
        if (number_of_last_samples >= nsamples) return 0;
        if (nsamples > 0) {
            // ------ Mean value
            double mv = 0;
            int index = (loaded - number_of_last_samples) % nsamples;
            for (int i = 0; i < number_of_last_samples; i++) {
                mv += sample[index];
                index++;
                if (index == nsamples) index = 0;
            }
            mv /= number_of_last_samples;

            // ------ Variance
            double var = 0;
            index = (loaded - number_of_last_samples) % nsamples;
            for (int i = 0; i < number_of_last_samples; i++) {
                var += (sample[index] - mv) * (sample[index] - mv);
                index++;
                if (index == nsamples) index = 0;
            }
            var /= number_of_last_samples;

            // ------ Standard Deviation
            double stddv = (float) Math.sqrt(var);

            // ------ Uncertainty (confidence 95%)
            return (float) (1.96d * stddv / Math.sqrt(number_of_last_samples));
        }
        return 0;
    }
}
