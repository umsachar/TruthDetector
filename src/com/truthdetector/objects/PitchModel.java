package com.truthdetector.objects;

import java.util.Arrays;
import java.util.List;

public class PitchModel {
	
	public static final int NUM_PITCH = 128;
	public static final int REF_TIME = 10;
	private static final double BIAS = 0.004;
	
	private int audioTime;
	private double[] probArr; //Made from annotationList
	private double[] normArr; //Normalized arr
	private double[] nFileArr; //Array that goes into nnet
	
	public PitchModel(List<Annotation> annotationList, int timeInSec) {		
		audioTime = timeInSec;
		
		probArr = new double[audioTime * NUM_PITCH];
		normArr = new double[audioTime * NUM_PITCH];
		nFileArr = new double[REF_TIME * NUM_PITCH];
		double maxVol = 0;
		
		for(Annotation annotation : annotationList) {
			double midiKey = annotation.getPitch(PitchUnit.MIDI_KEY);
			double time = annotation.getStart();
			double volume = annotation.getVolume();
			double prob = annotation.getProbability();
			
			int index = (int) (((int)time * NUM_PITCH) + midiKey);
			probArr[index] += volume * prob;
			
			maxVol = Math.max(maxVol, Math.abs(probArr[index]));
		}
		
		for(int i = 0; i < probArr.length; i++)
			normArr[i] = Math.abs(probArr[i]) / maxVol * 0.8 + 0.1;
		
		resize(normArr);
	}
	
	public void resize(double[] input)
    {        
        double[] rawInput = Arrays.copyOf(input, input.length);       

        // YD compensates for the x loop by subtracting the width back out
        int YD = (audioTime / REF_TIME) * PitchModel.NUM_PITCH - PitchModel.NUM_PITCH; 
        int YR = audioTime % REF_TIME;
        int XD = 1;
        int XR = 0;     
        int outOffset= 0;
        int inOffset=  0;
        
        for (int y= REF_TIME, YE= 0; y > 0; y--) {            
            for (int x= PitchModel.NUM_PITCH, XE= 0; x > 0; x--) {
                nFileArr[outOffset++]= rawInput[inOffset];
                inOffset+=XD;
                XE+=XR;
                if (XE >= PitchModel.NUM_PITCH) {
                    XE-= PitchModel.NUM_PITCH;
                    inOffset++;
                }
            }            
            inOffset+= YD;
            YE+= YR;
            if (YE >= REF_TIME) {
                YE -= REF_TIME;     
                inOffset+=PitchModel.NUM_PITCH;
            }
        }               
    }

	public double[] getnFileArr() {
		return nFileArr;
	}
}
