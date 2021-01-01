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

    private float[] Sample;
    private int CurrentSample = -1;
    private int Loaded = 0;

    private float MeanValue = 0;
    private float Variance = 0;
    private float StdDeviation = 0;
    private float Tolerance = 0;        // Confidence 95%

    public MeanVariance(int size) {
        Sample = new float[size];
    }

    public void LoadSample(float sample) {
        CurrentSample++;
        if (CurrentSample == Sample.length) {
            CurrentSample = 0;
            //Log.d("MeanVariance", (String.format("MeanVariance: Mean=%+1.4f Uncertainty=%+1.4f (Std Deviation=%+1.4f)", MeanValue, Tolerance, StdDeviation)));
        }

        Sample[CurrentSample] = sample;
        Loaded++;

        Calculate();
    }

    public void Reset() {
        Loaded = 0;
        CurrentSample = -1;

        MeanValue = 0;
        Variance = 0;
        StdDeviation = 0;
        Tolerance = 0;

        Arrays.fill(Sample, 0);
    }

    public void Reset(float values) {
        Loaded = 0;
        CurrentSample = -1;

        Arrays.fill(Sample, values);
        Calculate();
    }

    public boolean isLoaded() {
        return (Loaded >= Sample.length);
    }

    public float percentLoaded() {
        return (Math.min(100 * Loaded / Sample.length, 100));
    }

    public boolean isReady() {
        return (Loaded > 10);
    }

    public float getMeanValue() {
        return MeanValue;
    }

    public float getVariance() {
        return Variance;
    }

    public float getStdDeviation() {
        return StdDeviation;
    }

    public float getTolerance() {
        return Tolerance;
    }

    private void Calculate() {
        int nsamples = Math.min(Sample.length, Loaded);
        if (nsamples > 0) {

            // ------ Mean value
            double mv = 0;
            for (int i = 0; i < nsamples; i++) {
                mv += Sample[i];
            }
            mv /= nsamples;
            MeanValue = (float) mv;

            // ------ Variance
            double var = 0;
            for (int i = 0; i < nsamples; i++) {
                var += (Sample[i] - mv) * (Sample[i] - mv);
            }
            Variance /= nsamples;

            // ------ Standard Deviation
            StdDeviation = (float) Math.sqrt(var);

            // ------ Uncertainty (confidence 95%)
            Tolerance = (float) (1.96d * StdDeviation / Math.sqrt(nsamples));
        } else {
            Reset();
        }
    }


    public float getMeanValue(int number_of_last_samples) {
        int nsamples = Math.min(Sample.length, Loaded);
        if (number_of_last_samples <= 0) return MeanValue;
        if (number_of_last_samples >= nsamples) return 0;
        if (nsamples > 0) {
            // ------ Mean value
            double mv = 0;
            int index = (Loaded - number_of_last_samples) % nsamples;
            Log.d("MeanVariance", ("MeanVariance: Loaded="+ Loaded + " Start=" + index));

            for (int i = 0; i < number_of_last_samples; i++) {
                mv += Sample[index];
                index++;
                if (index == nsamples) index = 0;
            }
            mv /= number_of_last_samples;

            Log.d("MeanVariance", (String.format("MeanVariance: Mean=%+1.4f Uncertainty=%+1.4f (Std Deviation=%+1.4f)", MeanValue, Tolerance, StdDeviation)));
            return (float) mv;
        }
        return 0;
    }


    public float getTolerance(int number_of_last_samples) {
        int nsamples = Math.min(Sample.length, Loaded);
        if (number_of_last_samples <= 0) return Tolerance;
        if (number_of_last_samples >= nsamples) return 0;
        if (nsamples > 0) {
            // ------ Mean value
            double mv = 0;
            int index = (Loaded - number_of_last_samples) % nsamples;
            for (int i = 0; i < number_of_last_samples; i++) {
                mv += Sample[index];
                index++;
                if (index == nsamples) index = 0;
            }
            mv /= number_of_last_samples;

            // ------ Variance
            double var = 0;
            index = (Loaded - number_of_last_samples) % nsamples;
            for (int i = 0; i < number_of_last_samples; i++) {
                var += (Sample[index] - mv) * (Sample[index] - mv);
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
