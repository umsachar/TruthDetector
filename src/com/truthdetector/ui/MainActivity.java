package com.truthdetector.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.truthdetector.R;
import com.truthdetector.objects.AudioPlayer;
import com.truthdetector.objects.AudioRecorder;
import com.truthdetector.objects.Globals;

public class MainActivity extends Activity implements MarkerView.MarkerListener, WaveformView.WaveformListener {

	private AudioPlayer player;
	private AudioRecorder recorder;
	private boolean isRec;

	private Globals globals = Globals.getInstance();

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setTheme(android.R.style.Theme_Black_NoTitleBar);
		setContentView(R.layout.activity_record);

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
        Log.v("Ringdroid", "EditActivity onConfigurationChanged");
        final int saveZoomLevel = mWaveformView.getZoomLevel();
        super.onConfigurationChanged(newConfig);

        loadGui();

        mHandler.postDelayed(new Runnable() {
                public void run() {
                    mStartMarker.requestFocus();
                    markerFocus(mStartMarker);

                    mWaveformView.setZoomLevel(saveZoomLevel);
                    mWaveformView.recomputeHeights(mDensity);

                    updateDisplay();
                }
            }, 500);
    }

	@Override
	public void waveformTouchStart(float x) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void waveformTouchMove(float x) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void waveformTouchEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void waveformFling(float x) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void waveformDraw() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void waveformZoomIn() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void waveformZoomOut() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markerTouchStart(MarkerView marker, float pos) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markerTouchMove(MarkerView marker, float pos) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markerTouchEnd(MarkerView marker) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markerFocus(MarkerView marker) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markerLeft(MarkerView marker, int velocity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markerRight(MarkerView marker, int velocity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markerEnter(MarkerView marker) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markerKeyUp() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void markerDraw() {
		// TODO Auto-generated method stub
		
	}
}
