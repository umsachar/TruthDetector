package com.truthdetector.ui;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

import com.truthdetector.R;
import com.truthdetector.objects.Globals;

public class LoadActivity extends Activity {

	private final int NUM_TASKS = 3;

	private TextView pText;
	private ProgressBar pBar;
	private Button startButton;
	private int onTask;
	private Handler handler = new Handler();
	private Globals globals = Globals.getInstance();

	private String[] taskInfo = { "Setting up file paths...", "Loading FFMPEG executable...", "Finished" };

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_load);

		pText = (TextView) findViewById(R.id.progressText);
		pBar = (ProgressBar) findViewById(R.id.progressBar);
		pBar.setMax(NUM_TASKS);
		
		pBar.setProgress(1);
		pText.setText("Hi");

		startButton = (Button) findViewById(R.id.startButton);
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent rec = new Intent(LoadActivity.this, MainActivity.class);
				startActivity(rec);
			}
		});
		startButton.setVisibility(View.INVISIBLE);

		onTask = 0;
		animate();
	}

	public void animate() {
	
		new Thread(new Runnable() {
			public void run() {
				globals.setFilePath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/truthdetector/");
				File savePath = new File(globals.getFilePath());
				if (!savePath.exists())
					savePath.mkdir();
				globals.setFileName("temp");
				onTask++;
				updateProgressBar();
				
				new AndroidFFMPEGLocator(LoadActivity.this);
				onTask++;
				updateProgressBar();
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				exitLoading();
			}
		}).start();
	}
	
	private void updateProgressBar() {
		handler.post(new Runnable() {
			public void run() {
				pBar.setProgress(onTask);
				pText.setText(taskInfo[onTask]);
			}
		});
	}
	
	private void exitLoading() {
		handler.post(new Runnable() {
			public void run() {
				pBar.setVisibility(View.INVISIBLE);
				pText.setVisibility(View.INVISIBLE);
				startButton.setVisibility(View.VISIBLE);
			}
		});
	}
}
