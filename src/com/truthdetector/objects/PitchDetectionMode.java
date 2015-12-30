/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package com.truthdetector.objects;


/**
 * The pitch detection mode defines which algorithm is used to detect pitch.
 * 
 * @author Joren Six
 */
public enum PitchDetectionMode {
	/**
	 * The AUBIO_YIN algorithm.
	 */
	VAMP_YIN("yin"),
	/**
	 * A faster version of AUBIO_YIN: spectral AUBIO_YIN. It should yield very
	 * similar results as AUBIO_YIN, only faster.
	 */
	VAMP_YIN_FFT("yin_fft"),
	
	/**
	 * Melodia monophonic, see http://www.justinsalamon.com/melody-extraction.html
	 */
	VAMP_MELODIA_MONOPHONIC("melodia_monophonic"),
	
	/**
	 * Melodia polyphonic, see Melodia monophonic, see http://www.justinsalamon.com/melody-extraction.html
	 */
	VAMP_MELODIA_POLYPHONIC("melodia_polyphonic"),
	/**
	 * Fast harmonic comb.
	 */
	VAMP_FAST_HARMONIC_COMB("fast_harmonic_comb"),

	/**
	 * Uses a basic estimate of the pitch extracted from the spectrum. The pitch
	 * estimate needs elaboration, just finding the max at the moment but need
	 * to correct for sub-harmonics which are really the fundamental. See
	 * http://www.mazurka.org.uk/software/sv/plugin/MzHarmonicSpectrum/
	 */
	VAMP_MAZURKA_PITCH("mazurka_pitch"),

	/**
	 * Schmitt trigger.
	 */
	VAMP_SCHMITT("schmitt"),
	/**
	 * Spectral comb.
	 */
	VAMP_SPECTRAL_COMB("spectral_comb"),
	
	/**
	 * Spectral comb.
	 */
	VAMP_CONSTANT_Q_200("constantq_200"),
	/**
	 * Spectral comb.
	 */
	VAMP_CONSTANT_Q_400("constantq_400"),

	/**
	 * The IPEM pitch tracker outputs six weighted pitch candidates.
	 */
	IPEM_SIX("ipem_six"),

	/**
	 * The IPEM pitch tracker outputs only one pitch candidates.
	 */
	IPEM_ONE("ipem_one"),

	/**
	 * The pure java YIN implementation of Tarsos.
	 */
	TARSOS_YIN("tarsos_yin"),
	
	/**
	 * The pure java YIN implementation of Tarsos, with parameters set for speed (less accurate).
	 */
	TARSOS_FAST_YIN("tarsos_fast_yin"),

	/**
	 * The pure java MPM (Tartini pitch tracker) implementation of Tarsos.
	 */
	TARSOS_MPM("tarsos_mpm"),
	
	TARSOS_FAST_MPM("tarsos_fast_mpm"),
	
	TARSOS_DYNAMIC_WAVELET("tarsos_dynamic_wavelet"),
	
	SWIPE("swipe"),
	
	POLYPHON("polyphon"),
	
	
	SWIPE_OCTAVE("swipe_octave"), 
	
	TARSOS_FFT_YIN("tarsos_fft_yin");

	/**
	 * The name of the parameter.
	 */
	private final String detectionModeName;

	/**
	 * Initialize a pitch detection mode with a name.
	 * 
	 * @param name
	 *            The name (e.g. command line parameter) for the mode.
	 */
	private PitchDetectionMode(final String name) {
		this.detectionModeName = name;
	}

	/**
	 * @return The name used in the (aubio) command to execute the pitch
	 *         detector.
	 */
	public String getParametername() {
		return this.getDetectionModeName();
	}

	public String getDetectionModeName() {
		return detectionModeName;
	}
}
