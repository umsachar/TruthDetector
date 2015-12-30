package com.truthdetector.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

public class AndroidPitchDetector implements PitchDetector {

	private String filePath;
	private PitchEstimationAlgorithm algorithm;
	private long lengthInMilliSeconds;
	private List<Annotation> annotations;
	private double progress;
	private final PitchDetectionMode pitchDetectionMode;
	private MediaFormat format;

	private PitchDetectionHandler handler = new PitchDetectionHandler() {

		@Override
		public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
			if (pitchDetectionResult.isPitched()) {
				Annotation annotation = new Annotation(audioEvent.getTimeStamp(), pitchDetectionResult.getPitch(), audioEvent.getdBSPL(), pitchDetectionResult.getProbability());
				annotations.add(annotation);
			}
		}
	};

	private AudioProcessor progressProcessor = new AudioProcessor() {
		public void processingFinished() {
		}

		public boolean process(AudioEvent audioEvent) {
			progress = audioEvent.getProgress();
			return true;
		}
	};

	public AndroidPitchDetector(String filePath, PitchDetectionMode pitchDetectionMode) throws InterruptedException {
		this.filePath = filePath;
		this.pitchDetectionMode = pitchDetectionMode;
		annotations = new ArrayList<Annotation>();
		if (pitchDetectionMode == PitchDetectionMode.TARSOS_MPM) {
			algorithm = PitchEstimationAlgorithm.MPM;
		} else if (pitchDetectionMode == PitchDetectionMode.TARSOS_YIN) {
			algorithm = PitchEstimationAlgorithm.YIN;
		} else if (pitchDetectionMode == PitchDetectionMode.TARSOS_DYNAMIC_WAVELET) {
			algorithm = PitchEstimationAlgorithm.DYNAMIC_WAVELET;
		} else if (pitchDetectionMode == PitchDetectionMode.TARSOS_FFT_YIN) {
			algorithm = PitchEstimationAlgorithm.FFT_YIN;
		} else {
			throw new IllegalArgumentException("Algorithm not recognized, should be MPM, YIN or Dynamic Wavelet, is " + pitchDetectionMode.name());
		}

		lengthInMilliSeconds = calculateLengthInMilliSeconds(filePath);
		Thread t = new Thread(null, AudioProcessing, "AudioProcessing", 256000);
		t.start();
		t.join();
	}

	public Runnable AudioProcessing = new Runnable() {

		public void run() {

			format = getFormat(filePath);
			int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
			int bufferSize = 2048;
			int overlap = 1024;

			AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(filePath, sampleRate, bufferSize, overlap);
			dispatcher.addAudioProcessor(new PitchProcessor(algorithm, sampleRate, bufferSize, handler));
			dispatcher.addAudioProcessor((be.tarsos.dsp.AudioProcessor) progressProcessor);
			dispatcher.run();
		}
	};

	@Override
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	@Override
	public double progress() {
		return progress;
	}

	@Override
	public String getName() {
		return pitchDetectionMode.getParametername();
	}

	public double getLengthIn(TimeUnit unit) {
		return unit.convert(lengthInMilliSeconds, TimeUnit.MILLISECONDS);
	}

	private long calculateLengthInMilliSeconds(String filePath) {
		MediaPlayer mp = new MediaPlayer();
		try {
			mp.setDataSource(filePath);
			mp.prepare();
		} catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
			e.printStackTrace();
		}
		return mp.getDuration();
	}

	public MediaFormat getFormat(String filePath) {
		MediaExtractor extractor = new MediaExtractor();
		try {
			extractor.setDataSource(filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		MediaFormat format = extractor.getTrackFormat(0);
		return format;
	}

}
