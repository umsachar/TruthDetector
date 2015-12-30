package com.truthdetector.objects;

public class Annotation {
	
	private double timeStamp;
	private Pitch pitch;
	private double volume;
	private double probability;

	public Annotation(double timeStamp, float hertz, double volume, float probability) {
		this.timeStamp = timeStamp;
		pitch = new Pitch(hertz);
		this.volume = volume;
		this.probability = probability;
	}

	public double getPitch(PitchUnit pUnit) {
		return pitch.getPitch(pUnit);
	}

	public double getStart() {
		return timeStamp;
	}
	
	public double getVolume() {
		return volume;
	}

	public double getProbability() {
		return probability;
	}
	
	@Override
	public String toString() {
		return String.format("[%.2f, %d, %.1f, %.2f]", timeStamp, (int)pitch.getPitch(PitchUnit.MIDI_KEY), volume, probability);
	}
}