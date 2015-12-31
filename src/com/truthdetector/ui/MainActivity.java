package com.truthdetector.ui;

import android.app.Activity;
import android.content.res.Configuration;
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
		

		// mWaveformView.setSoundFile(mSoundFile);

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
        int xWhole = (int)x;
        int xFrac = (int)(100 * (x - xWhole) + 0.5);

        if (xFrac >= 100) {
            xWhole++; //Round up
            xFrac -= 100; //Now we need the remainder after the round up
            if (xFrac < 10) {
                xFrac *= 10; //we need a fraction that is 2 digits long
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
    
    
}
