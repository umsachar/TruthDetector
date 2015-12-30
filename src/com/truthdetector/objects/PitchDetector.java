package com.truthdetector.objects;

import java.util.List;

public interface PitchDetector {

	
	/**
	 * Calculate and return an indicator for progress.
	 * @return A value between 0.0 and 100. Indicating the progress made in
	 *         percentage. Or -1 if the task has an indeterminate duration.
	 */
	double progress();

	/**
	 * @return a list of annotated samples
	 */
	List<Annotation> getAnnotations();

	/**
	 * @return the name of the detector possibly with parameters e.g. aubio_YIN
	 */
	String getName();
}
