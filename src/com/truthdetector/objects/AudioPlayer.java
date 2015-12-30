package com.truthdetector.objects;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class AudioPlayer {

	private String filePath;
	private MediaPlayer player;
	private ImageButton playButton;
	private ImageButton rewButton;
	private SeekBar seekbar;
	private TextView time;
	private boolean isPlaying;
	private Handler handler;
	
	public AudioPlayer(String filePath, ImageButton pButton, ImageButton rButton, SeekBar seekbar, TextView time) {
		this.filePath = filePath;
		this.playButton = pButton;
		this.rewButton = rButton;
		this.seekbar = seekbar;
		this.time = time;
		this.isPlaying = false;
		handler = new Handler();
		
		playButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isPlaying) {
					player.start();
					playButton.setImageResource(android.R.drawable.ic_media_pause);
				} else {
					player.pause();
					playButton.setImageResource(android.R.drawable.ic_media_play);
				}
				isPlaying = !isPlaying;
			}
		});
		
		rewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				player.seekTo(0);
			}
		});
		
		seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (player != null && fromUser) {
					player.seekTo(progress);
				}
			}
		});
	}
	
	public void initiatePlayer() {
		player = new MediaPlayer();

		player.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				player.seekTo(0);
				playButton.setImageResource(android.R.drawable.ic_media_play);
				isPlaying = false;
			}

		});
		try {
			player.setDataSource(filePath);
			player.prepare();
		} catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
			e.printStackTrace();
		}

		seekUpdation();
	}
	
	public void destroyPlayer() {
		if(player != null) {
			if (player.isPlaying())
				player.stop();
			player.release();
			player = null;
		}
	}
	
	private void seekUpdation() {
		seekbar.setMax(player.getDuration());
		seekbar.setProgress(player.getCurrentPosition());

		String timeStr = new SimpleDateFormat("mm:ss").format(new Date(player.getCurrentPosition())) + "/" + new SimpleDateFormat("mm:ss").format(new Date(player.getDuration()));
		time.setText(timeStr);

		handler.postDelayed(run, 100);
	}
	
	Runnable run = new Runnable() {
		@Override
		public void run() {
			if (player != null)
				seekUpdation();
		}
	};
}
