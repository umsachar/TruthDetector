package com.truthdetector.ui;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import android.app.Activity;
import android.content.res.Configuration;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.truthdetector.R;
import com.truthdetector.objects.AudioPlayer;
import com.truthdetector.objects.AudioRecorder;
import com.truthdetector.objects.Globals;

public class MainActivity extends Activity implements MarkerView.MarkerListener, WaveformView.WaveformListener {

	private AudioPlayer player;
	private AudioRecorder recorder;
	private boolean isRec;
	private boolean mIsPlaying;

	private WaveformView mWaveformView;
	private MarkerView mStartMarker;
	private MarkerView mEndMarker;
	private TextView mInfo;
	private ImageButton mPlayButton;
	private boolean mKeyDown;
	private String mCaption = "";
	private int mWidth;
	private int mMaxPos;
	private int mStartPos;
	private int mEndPos;
	private boolean mStartVisible;
	private boolean mEndVisible;
	private int mOffset;
	private int mOffsetGoal;
	private int mFlingVelocity;
	private int mPlayStartMsec;
	private int mPlayEndMsec;
	private Handler mHandler;
	private MediaPlayer mPlayer;
	private boolean mTouchDragging;
	private float mTouchStart;
	private int mTouchInitialOffset;
	private int mTouchInitialStartPos;
	private int mTouchInitialEndPos;
	private long mWaveformTouchStartMsec;
	private float mDensity;
	private int mMarkerLeftInset;
	private int mMarkerRightInset;
	private int mMarkerTopOffset;
	private int mMarkerBottomOffset;

	private Globals globals = Globals.getInstance();

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setTheme(android.R.style.Theme_Black_NoTitleBar);
		setContentView(R.layout.activity_main);

		/*
		 * Make audio player ImageButton playButton = (ImageButton)
		 * findViewById(R.id.play); ImageButton rewButton = (ImageButton)
		 * findViewById(R.id.rec); SeekBar seekbar = (SeekBar)
		 * findViewById(R.id.seekBar); TextView time = (TextView)
		 * findViewById(R.id.info); player = new
		 * AudioPlayer(globals.getAbsFilePath(), playButton, rewButton, seekbar,
		 * time);
		 */

		// Make audio recorder
		TextView recTime = (TextView) findViewById(R.id.recTime);
		recorder = new AudioRecorder(globals.getAbsFilePath(), recTime);
		recorder.initiateRecorder();
		isRec = false;

		// Set record button
		final ImageButton recButton = (ImageButton) findViewById(R.id.rec);
		recButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isRec) {
					recorder.record(MainActivity.this);
					recButton.setImageResource(R.drawable.stop);
					setRecoded(false);
					player.destroyPlayer();
				} else {
					recorder.stopRecording();
					recButton.setImageResource(R.drawable.record);
					setRecoded(true);
					recorder.destroyRecorder();
					recorder.initiateRecorder();
					player.initiatePlayer();
				}
				isRec = !isRec;
			}
		});

		Button detectButton = (Button) findViewById(R.id.detect);
		detectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		
		mWaveformView = (WaveformView)findViewById(R.id.waveform);
        mWaveformView.setListener(this);

        mInfo = (TextView)findViewById(R.id.info);
        mInfo.setText(mCaption);

        mMaxPos = 0;

        try {
			readFile(new File(globals.getAbsFilePath()), mWaveformView);
		} catch (IOException e) {
			e.printStackTrace();
		}

        mStartMarker = (MarkerView)findViewById(R.id.startmarker);
        mStartMarker.setListener(this);
        mStartMarker.setAlpha(1f);
        mStartMarker.setFocusable(true);
        mStartMarker.setFocusableInTouchMode(true);
        mStartVisible = true;

        mEndMarker = (MarkerView)findViewById(R.id.endmarker);
        mEndMarker.setListener(this);
        mEndMarker.setAlpha(1f);
        mEndMarker.setFocusable(true);
        mEndMarker.setFocusableInTouchMode(true);
        mEndVisible = true;

        updateDisplay();
	}

	private void setRecoded(boolean enabled) {

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final int saveZoomLevel = mWaveformView.getZoomLevel();
		super.onConfigurationChanged(newConfig);

		loadGui();

		new Handler().postDelayed(new Runnable() {
			public void run() {
				mStartMarker.requestFocus();
				markerFocus(mStartMarker);

				mWaveformView.setZoomLevel(saveZoomLevel);
				mWaveformView.recomputeHeights(mDensity);

				updateDisplay();
			}
		}, 500);
	}

	//
	// WaveformListener
	//

	/**
	 * Every time we get a message that our waveform drew, see if we need to
	 * animate and trigger another redraw.
	 */
	public void waveformDraw() {
		mWidth = mWaveformView.getMeasuredWidth();
		if (mOffsetGoal != mOffset && !mKeyDown)
			updateDisplay();
		else if (mIsPlaying) {
			updateDisplay();
		} else if (mFlingVelocity != 0) {
			updateDisplay();
		}
	}

	public void waveformTouchStart(float x) {
		mTouchDragging = true;
		mTouchStart = x;
		mTouchInitialOffset = mOffset;
		mFlingVelocity = 0;
		mWaveformTouchStartMsec = getCurrentTime();
	}

	public void waveformTouchMove(float x) {
		mOffset = trap((int) (mTouchInitialOffset + (mTouchStart - x)));
		updateDisplay();
	}

	public void waveformTouchEnd() {
		mTouchDragging = false;
		mOffsetGoal = mOffset;

		long elapsedMsec = getCurrentTime() - mWaveformTouchStartMsec;
		if (elapsedMsec < 300) {
			if (mIsPlaying) {
				int seekMsec = mWaveformView.pixelsToMillisecs((int) (mTouchStart + mOffset));
				if (seekMsec >= mPlayStartMsec && seekMsec < mPlayEndMsec) {
					mPlayer.seekTo(seekMsec);
				} else {
					handlePause();
				}
			} else {
				onPlay((int) (mTouchStart + mOffset));
			}
		}
	}

	public void waveformFling(float vx) {
		mTouchDragging = false;
		mOffsetGoal = mOffset;
		mFlingVelocity = (int) (-vx);
		updateDisplay();
	}

	public void waveformZoomIn() {
		mWaveformView.zoomIn();
		mStartPos = mWaveformView.getStart();
		mEndPos = mWaveformView.getEnd();
		mMaxPos = mWaveformView.maxPos();
		mOffset = mWaveformView.getOffset();
		mOffsetGoal = mOffset;
		updateDisplay();
	}

	public void waveformZoomOut() {
		mWaveformView.zoomOut();
		mStartPos = mWaveformView.getStart();
		mEndPos = mWaveformView.getEnd();
		mMaxPos = mWaveformView.maxPos();
		mOffset = mWaveformView.getOffset();
		mOffsetGoal = mOffset;
		updateDisplay();
	}

	//
	// MarkerListener
	//

	public void markerDraw() {
	}

	public void markerTouchStart(MarkerView marker, float x) {
		mTouchDragging = true;
		mTouchStart = x;
		mTouchInitialStartPos = mStartPos;
		mTouchInitialEndPos = mEndPos;
	}

	public void markerTouchMove(MarkerView marker, float x) {
		float delta = x - mTouchStart;

		if (marker == mStartMarker) {
			mStartPos = trap((int) (mTouchInitialStartPos + delta));
			mEndPos = trap((int) (mTouchInitialEndPos + delta));
		} else {
			mEndPos = trap((int) (mTouchInitialEndPos + delta));
			if (mEndPos < mStartPos)
				mEndPos = mStartPos;
		}

		updateDisplay();
	}

	public void markerTouchEnd(MarkerView marker) {
		mTouchDragging = false;
		if (marker == mStartMarker) {
			setOffsetGoalStart();
		} else {
			setOffsetGoalEnd();
		}
	}

	public void markerLeft(MarkerView marker, int velocity) {
		mKeyDown = true;

		if (marker == mStartMarker) {
			int saveStart = mStartPos;
			mStartPos = trap(mStartPos - velocity);
			mEndPos = trap(mEndPos - (saveStart - mStartPos));
			setOffsetGoalStart();
		}

		if (marker == mEndMarker) {
			if (mEndPos == mStartPos) {
				mStartPos = trap(mStartPos - velocity);
				mEndPos = mStartPos;
			} else {
				mEndPos = trap(mEndPos - velocity);
			}

			setOffsetGoalEnd();
		}

		updateDisplay();
	}

	public void markerRight(MarkerView marker, int velocity) {
		mKeyDown = true;

		if (marker == mStartMarker) {
			int saveStart = mStartPos;
			mStartPos += velocity;
			if (mStartPos > mMaxPos)
				mStartPos = mMaxPos;
			mEndPos += (mStartPos - saveStart);
			if (mEndPos > mMaxPos)
				mEndPos = mMaxPos;

			setOffsetGoalStart();
		}

		if (marker == mEndMarker) {
			mEndPos += velocity;
			if (mEndPos > mMaxPos)
				mEndPos = mMaxPos;

			setOffsetGoalEnd();
		}

		updateDisplay();
	}

	public void markerEnter(MarkerView marker) {
	}

	public void markerKeyUp() {
		mKeyDown = false;
		updateDisplay();
	}

	public void markerFocus(MarkerView marker) {
		mKeyDown = false;
		if (marker == mStartMarker) {
			setOffsetGoalStartNoUpdate();
		} else {
			setOffsetGoalEndNoUpdate();
		}

		// Delay updaing the display because if this focus was in
		// response to a touch event, we want to receive the touch
		// event too before updating the display.
		mHandler.postDelayed(new Runnable() {
			public void run() {
				updateDisplay();
			}
		}, 100);
	}

	//
	// Internal methods
	//

	/**
	 * Called from both onCreate and onConfigurationChanged (if the user
	 * switched layouts)
	 */
	private void loadGui() {
		// Inflate our UI from its XML layout description.
		setContentView(R.layout.activity_load);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mDensity = metrics.density;

		mMarkerLeftInset = (int) (46 * mDensity);
		mMarkerRightInset = (int) (48 * mDensity);
		mMarkerTopOffset = (int) (10 * mDensity);
		mMarkerBottomOffset = (int) (10 * mDensity);

		mPlayButton = (ImageButton) findViewById(R.id.play);
		mPlayButton.setOnClickListener(new OnClickListener() {
			public void onClick(View sender) {
				onPlay(mStartPos);
			}
		});

		enableDisableButtons();

		mWaveformView = (WaveformView) findViewById(R.id.waveform);
		mWaveformView.setListener(this);

		mInfo = (TextView) findViewById(R.id.info);
		mInfo.setText(mCaption);

		mMaxPos = 0;

		try {
			readFile(new File(globals.getAbsFilePath()), mWaveformView);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mStartMarker = (MarkerView) findViewById(R.id.startmarker);
		mStartMarker.setListener(this);
		mStartMarker.setAlpha(1f);
		mStartMarker.setFocusable(true);
		mStartMarker.setFocusableInTouchMode(true);
		mStartVisible = true;

		mEndMarker = (MarkerView) findViewById(R.id.endmarker);
		mEndMarker.setListener(this);
		mEndMarker.setAlpha(1f);
		mEndMarker.setFocusable(true);
		mEndMarker.setFocusableInTouchMode(true);
		mEndVisible = true;

		updateDisplay();
	}

	private synchronized void updateDisplay() {
		if (mIsPlaying) {
			int now = mPlayer.getCurrentPosition();
			int frames = mWaveformView.millisecsToPixels(now);
			mWaveformView.setPlayback(frames);
			setOffsetGoalNoUpdate(frames - mWidth / 2);
			if (now >= mPlayEndMsec) {
				handlePause();
			}
		}

		if (!mTouchDragging) {
			int offsetDelta;

			if (mFlingVelocity != 0) {
				offsetDelta = mFlingVelocity / 30;
				if (mFlingVelocity > 80) {
					mFlingVelocity -= 80;
				} else if (mFlingVelocity < -80) {
					mFlingVelocity += 80;
				} else {
					mFlingVelocity = 0;
				}

				mOffset += offsetDelta;

				if (mOffset + mWidth / 2 > mMaxPos) {
					mOffset = mMaxPos - mWidth / 2;
					mFlingVelocity = 0;
				}
				if (mOffset < 0) {
					mOffset = 0;
					mFlingVelocity = 0;
				}
				mOffsetGoal = mOffset;
			} else {
				offsetDelta = mOffsetGoal - mOffset;

				if (offsetDelta > 10)
					offsetDelta = offsetDelta / 10;
				else if (offsetDelta > 0)
					offsetDelta = 1;
				else if (offsetDelta < -10)
					offsetDelta = offsetDelta / 10;
				else if (offsetDelta < 0)
					offsetDelta = -1;
				else
					offsetDelta = 0;

				mOffset += offsetDelta;
			}
		}

		mWaveformView.setParameters(mStartPos, mEndPos, mOffset);
		mWaveformView.invalidate();

		mStartMarker.setContentDescription(getResources().getText(R.string.start_marker) + " " + formatTime(mStartPos));
		mEndMarker.setContentDescription(getResources().getText(R.string.end_marker) + " " + formatTime(mEndPos));

		int startX = mStartPos - mOffset - mMarkerLeftInset;
		if (startX + mStartMarker.getWidth() >= 0) {
			if (!mStartVisible) {
				// Delay this to avoid flicker
				mHandler.postDelayed(new Runnable() {
					public void run() {
						mStartVisible = true;
						mStartMarker.setAlpha(1f);
					}
				}, 0);
			}
		} else {
			if (mStartVisible) {
				mStartMarker.setAlpha(0f);
				mStartVisible = false;
			}
			startX = 0;
		}

		int endX = mEndPos - mOffset - mEndMarker.getWidth() + mMarkerRightInset;
		if (endX + mEndMarker.getWidth() >= 0) {
			if (!mEndVisible) {
				// Delay this to avoid flicker
				mHandler.postDelayed(new Runnable() {
					public void run() {
						mEndVisible = true;
						mEndMarker.setAlpha(1f);
					}
				}, 0);
			}
		} else {
			if (mEndVisible) {
				mEndMarker.setAlpha(0f);
				mEndVisible = false;
			}
			endX = 0;
		}

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(startX, mMarkerTopOffset, -mStartMarker.getWidth(), -mStartMarker.getHeight());
		mStartMarker.setLayoutParams(params);

		params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(endX, mWaveformView.getMeasuredHeight() - mEndMarker.getHeight() - mMarkerBottomOffset, -mStartMarker.getWidth(), -mStartMarker.getHeight());
		mEndMarker.setLayoutParams(params);
	}

	private long getCurrentTime() {
		return System.nanoTime() / 1000000;
	}

	private int trap(int pos) {
		if (pos < 0)
			return 0;
		if (pos > mMaxPos)
			return mMaxPos;
		return pos;
	}

	private synchronized void onPlay(int startPosition) {
		if (mIsPlaying) {
			handlePause();
			return;
		}

		if (mPlayer == null) {
			// Not initialized yet
			return;
		}

		try {
			mPlayStartMsec = mWaveformView.pixelsToMillisecs(startPosition);
			if (startPosition < mStartPos) {
				mPlayEndMsec = mWaveformView.pixelsToMillisecs(mStartPos);
			} else if (startPosition > mEndPos) {
				mPlayEndMsec = mWaveformView.pixelsToMillisecs(mMaxPos);
			} else {
				mPlayEndMsec = mWaveformView.pixelsToMillisecs(mEndPos);
			}
			mPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					handlePause();
				}
			});
			mIsPlaying = true;

			mPlayer.seekTo(mPlayStartMsec);
			mPlayer.start();
			updateDisplay();
			enableDisableButtons();
		} catch (Exception e) {
			return;
		}
	}

	private void setOffsetGoalStart() {
		setOffsetGoal(mStartPos - mWidth / 2);
	}

	private void setOffsetGoalStartNoUpdate() {
		setOffsetGoalNoUpdate(mStartPos - mWidth / 2);
	}

	private void setOffsetGoalEnd() {
		setOffsetGoal(mEndPos - mWidth / 2);
	}

	private void setOffsetGoalEndNoUpdate() {
		setOffsetGoalNoUpdate(mEndPos - mWidth / 2);
	}

	private void setOffsetGoal(int offset) {
		setOffsetGoalNoUpdate(offset);
		updateDisplay();
	}

	private void setOffsetGoalNoUpdate(int offset) {
		if (mTouchDragging) {
			return;
		}

		mOffsetGoal = offset;
		if (mOffsetGoal + mWidth / 2 > mMaxPos)
			mOffsetGoal = mMaxPos - mWidth / 2;
		if (mOffsetGoal < 0)
			mOffsetGoal = 0;
	}

	private String formatTime(int pixels) {
		if (mWaveformView != null && mWaveformView.isInitialized()) {
			return formatDecimal(mWaveformView.pixelsToSeconds(pixels));
		} else {
			return "";
		}
	}

	private String formatDecimal(double x) {
		int xWhole = (int) x;
		int xFrac = (int) (100 * (x - xWhole) + 0.5);

		if (xFrac >= 100) {
			xWhole++; // Round up
			xFrac -= 100; // Now we need the remainder after the round up
			if (xFrac < 10) {
				xFrac *= 10; // we need a fraction that is 2 digits long
			}
		}

		if (xFrac < 10)
			return xWhole + ".0" + xFrac;
		else
			return xWhole + "." + xFrac;
	}

	private synchronized void handlePause() {
		if (mPlayer != null && mPlayer.isPlaying()) {
			mPlayer.pause();
		}
		mWaveformView.setPlayback(-1);
		mIsPlaying = false;
		enableDisableButtons();
	}

	private void enableDisableButtons() {
		if (mIsPlaying) {
			mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
			mPlayButton.setContentDescription(getResources().getText(R.string.stop));
		} else {
			mPlayButton.setImageResource(android.R.drawable.ic_media_play);
			mPlayButton.setContentDescription(getResources().getText(R.string.play));
		}
	}

	private void readFile(File inputFile, WaveformView waveview) throws java.io.FileNotFoundException, java.io.IOException {
		int samplesPerFrame = 1024;
		MediaExtractor extractor = new MediaExtractor();
		MediaFormat format = null;
		int i;
		long fileSize = inputFile.length();
		String[] components = inputFile.getPath().split("\\.");

		extractor.setDataSource(inputFile.getPath());
		int numTracks = extractor.getTrackCount();
		// find and select the first audio track present in the file.
		for (i = 0; i < numTracks; i++) {
			format = extractor.getTrackFormat(i);
			if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
				extractor.selectTrack(i);
				break;
			}
		}
		int numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
		int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
		// Expected total number of samples per channel.
		int expectedNumSamples = (int) ((format.getLong(MediaFormat.KEY_DURATION) / 1000000.f) * sampleRate + 0.5f);

		MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
		codec.configure(format, null, null, 0);
		codec.start();

		int decodedSamplesSize = 0; // size of the output buffer containing
									// decoded samples.
		byte[] decodedSamples = null;
		ByteBuffer[] inputBuffers = codec.getInputBuffers();
		ByteBuffer[] outputBuffers = codec.getOutputBuffers();
		int sample_size;
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		long presentation_time;
		int tot_size_read = 0;
		boolean done_reading = false;

		// Set the size of the decoded samples buffer to 1MB (~6sec of a stereo
		// stream at 44.1kHz).
		// For longer streams, the buffer size will be increased later on,
		// calculating a rough
		// estimate of the total size needed to store all the samples in order
		// to resize the buffer
		// only once.
		ByteBuffer decodedBytes = ByteBuffer.allocate(1 << 20);
		Boolean firstSampleData = true;
		while (true) {
			// read data from file and feed it to the decoder input buffers.
			int inputBufferIndex = codec.dequeueInputBuffer(100);
			if (!done_reading && inputBufferIndex >= 0) {
				sample_size = extractor.readSampleData(inputBuffers[inputBufferIndex], 0);
				if (firstSampleData && format.getString(MediaFormat.KEY_MIME).equals("audio/mp4a-latm") && sample_size == 2) {
					// For some reasons on some devices (e.g. the Samsung S3)
					// you should not
					// provide the first two bytes of an AAC stream, otherwise
					// the MediaCodec will
					// crash. These two bytes do not contain music data but
					// basic info on the
					// stream (e.g. channel configuration and sampling
					// frequency), and skipping them
					// seems OK with other devices (MediaCodec has already been
					// configured and
					// already knows these parameters).
					extractor.advance();
					tot_size_read += sample_size;
				} else if (sample_size < 0) {
					// All samples have been read.
					codec.queueInputBuffer(inputBufferIndex, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
					done_reading = true;
				} else {
					presentation_time = extractor.getSampleTime();
					codec.queueInputBuffer(inputBufferIndex, 0, sample_size, presentation_time, 0);
					extractor.advance();
					tot_size_read += sample_size;
				}
				firstSampleData = false;
			}

			// Get decoded stream from the decoder output buffers.
			int outputBufferIndex = codec.dequeueOutputBuffer(info, 100);
			if (outputBufferIndex >= 0 && info.size > 0) {
				if (decodedSamplesSize < info.size) {
					decodedSamplesSize = info.size;
					decodedSamples = new byte[decodedSamplesSize];
				}
				outputBuffers[outputBufferIndex].get(decodedSamples, 0, info.size);
				outputBuffers[outputBufferIndex].clear();
				// Check if buffer is big enough. Resize it if it's too small.
				if (decodedBytes.remaining() < info.size) {
					// Getting a rough estimate of the total size, allocate 20%
					// more, and
					// make sure to allocate at least 5MB more than the initial
					// size.
					int position = decodedBytes.position();
					int newSize = (int) ((position * (1.0 * fileSize / tot_size_read)) * 1.2);
					if (newSize - position < info.size + 5 * (1 << 20)) {
						newSize = position + info.size + 5 * (1 << 20);
					}
					ByteBuffer newDecodedBytes = null;
					// Try to allocate memory. If we are OOM, try to run the
					// garbage collector.
					int retry = 10;
					while (retry > 0) {
						try {
							newDecodedBytes = ByteBuffer.allocate(newSize);
							break;
						} catch (OutOfMemoryError oome) {
							// setting android:largeHeap="true" in <application>
							// seem to help not
							// reaching this section.
							retry--;
						}
					}
					if (retry == 0) {
						// Failed to allocate memory... Stop reading more data
						// and finalize the
						// instance with the data decoded so far.
						break;
					}
					// ByteBuffer newDecodedBytes =
					// ByteBuffer.allocate(newSize);
					decodedBytes.rewind();
					newDecodedBytes.put(decodedBytes);
					decodedBytes = newDecodedBytes;
					decodedBytes.position(position);
				}
				decodedBytes.put(decodedSamples, 0, info.size);
				codec.releaseOutputBuffer(outputBufferIndex, false);
			} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				outputBuffers = codec.getOutputBuffers();
			} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// Subsequent data will conform to new format.
				// We could check that codec.getOutputFormat(), which is the new
				// output format,
				// is what we expect.
			}
			if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 || (decodedBytes.position() / (2 * numChannels)) >= expectedNumSamples) {
				// We got all the decoded data from the decoder. Stop here.
				// Theoretically dequeueOutputBuffer(info, ...) should have set
				// info.flags to
				// MediaCodec.BUFFER_FLAG_END_OF_STREAM. However some phones
				// (e.g. Samsung S3)
				// won't do that for some files (e.g. with mono AAC files), in
				// which case subsequent
				// calls to dequeueOutputBuffer may result in the application
				// crashing, without
				// even an exception being thrown... Hence the second check.
				// (for mono AAC files, the S3 will actually double each sample,
				// as if the stream
				// was stereo. The resulting stream is half what it's supposed
				// to be and with a much
				// lower pitch.)
				break;
			}
		}
		int numSamples = decodedBytes.position() / (numChannels * 2); // One
																		// sample
																		// = 2
																		// bytes.
		decodedBytes.rewind();
		decodedBytes.order(ByteOrder.LITTLE_ENDIAN);
		ShortBuffer shortDecodedSamples = decodedBytes.asShortBuffer();
		int avgBitRate = (int) ((fileSize * 8) * ((float) sampleRate / numSamples) / 1000);

		extractor.release();
		extractor = null;
		codec.stop();
		codec.release();
		codec = null;

		// Temporary hack to make it work with the old version.
		int numFrames = numSamples / samplesPerFrame;
		if (numSamples % samplesPerFrame != 0) {
			numFrames++;
		}
		int[] frameGains = new int[numFrames];
		int j;
		int gain, value;
		int frameLens = (int) ((1000 * avgBitRate / 8) * (samplesPerFrame / sampleRate));
		for (i = 0; i < numFrames; i++) {
			gain = -1;
			for (j = 0; j < samplesPerFrame; j++) {
				value = 0;
				for (int k = 0; k < numChannels; k++) {
					if (shortDecodedSamples.remaining() > 0) {
						value += java.lang.Math.abs(shortDecodedSamples.get());
					}
				}
				value /= numChannels;
				if (gain < value) {
					gain = value;
				}
			}
			frameGains[i] = (int) Math.sqrt(gain); // here gain = sqrt(max
													// value of 1st channel)...
		}
		shortDecodedSamples.rewind();
		// DumpSamples(); // Uncomment this line to dump the samples in a TSV
		// file.
		

		waveview.setSoundFile(sampleRate, samplesPerFrame, numFrames, frameGains);
	}
}
