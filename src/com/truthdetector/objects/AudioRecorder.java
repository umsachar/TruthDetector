package com.truthdetector.objects;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

public class AudioRecorder {

	private final int SAMPLE_RATE = 44100;
	private final int WAV_HEADER_LENGTH = 44;

	private String savePath;
	private AudioRecord aRecorder;
	private int bufferSize;
	private boolean recording;
	private TextView recTime;

	public AudioRecorder(String savePath, TextView recTime) {
		this.savePath = savePath;
		this.recording = false;
		this.recTime = recTime;
	}

	public void initiateRecorder() {
		int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
		int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

		bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfiguration, audioEncoding) * 2;
		aRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, channelConfiguration, audioEncoding, bufferSize);
		
		recTime.setText("");
	}

	public void destroyRecorder() {
		if (aRecorder != null) {
			aRecorder.release();
			aRecorder = null;
		}

		recTime.setText("");
	}

	public void record(final Activity activity) {

		recording = true;
		
		new Thread(new Runnable() {

			public void run() {
				byte[] tempBuffer = new byte[bufferSize];

				File outFile = new File(savePath);
				if (outFile.exists())
					outFile.delete();

				try {
					outFile.createNewFile();
					FileOutputStream outStream = new FileOutputStream(outFile);
					outStream.write(createHeader(0));
					final Date start = new Date();
					aRecorder.startRecording();

					while (recording) {
						aRecorder.read(tempBuffer, 0, bufferSize);
						outStream.write(tempBuffer);

						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								String time = new SimpleDateFormat("mm:ss").format(new Date().getTime() - start.getTime());
								recTime.setText(time);
							}
						});
					}
					aRecorder.stop();
					outStream.close();
				} catch (Exception e) {

				}

				finalizeWriteToFile();

			}
		}).start();
	}
	
	public void stopRecording() {
		recording = false;
	}

	/**
	 * End the recording, saving and finalising the file
	 */
	public void finalizeWriteToFile() {
		File outFile = new File(savePath);

		if (recording == false && outFile != null) {
			appendHeader(outFile);

			Intent scanWav = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			scanWav.setData(Uri.fromFile(outFile));
		}
	}

	/**
	 * Creates a valid WAV header for the given bytes, using the class-wide
	 * sample rate
	 * 
	 * @param bytes
	 *            The sound data to be appraised
	 * @return The header, ready to be written to a file
	 */
	public byte[] createHeader(int bytesLength) {

		int totalLength = bytesLength + 4 + 24 + 8;
		byte[] lengthData = intToBytes(totalLength);
		byte[] samplesLength = intToBytes(bytesLength);
		byte[] sampleRateBytes = intToBytes(SAMPLE_RATE);
		byte[] bytesPerSecond = intToBytes(SAMPLE_RATE * 2);

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			out.write(new byte[] { 'R', 'I', 'F', 'F' });
			out.write(lengthData);
			out.write(new byte[] { 'W', 'A', 'V', 'E' });

			out.write(new byte[] { 'f', 'm', 't', ' ' });
			out.write(new byte[] { 0x10, 0x00, 0x00, 0x00 }); // 16 bit chunks
			out.write(new byte[] { 0x01, 0x00, 0x01, 0x00 }); // mono
			out.write(sampleRateBytes); // sampling rate
			out.write(bytesPerSecond); // bytes per second
			out.write(new byte[] { 0x02, 0x00, 0x10, 0x00 }); // 2 bytes per
																// sample

			out.write(new byte[] { 'd', 'a', 't', 'a' });
			out.write(samplesLength);
		} catch (IOException e) {
			Log.e("Create WAV", e.getMessage());
		}

		return out.toByteArray();
	}

	/**
	 * Turns an integer into its little-endian four-byte representation
	 * 
	 * @param in
	 *            The integer to be converted
	 * @return The bytes representing this integer
	 */
	public static byte[] intToBytes(int in) {
		byte[] bytes = new byte[4];
		for (int i = 0; i < 4; i++) {
			bytes[i] = (byte) ((in >>> i * 8) & 0xFF);
		}
		return bytes;
	}

	/**
	 * Appends a WAV header to a file containing raw audio data. Uses different
	 * strategies depending on amount of free disk space.
	 * 
	 * @param file
	 *            The file containing 16-bit little-endian PCM data.
	 */
	public void appendHeader(File file) {

		int bytesLength = (int) file.length();
		byte[] header = createHeader(bytesLength - WAV_HEADER_LENGTH);

		try {
			RandomAccessFile ramFile = new RandomAccessFile(file, "rw");
			ramFile.seek(0);
			ramFile.write(header);
			ramFile.close();
		} catch (FileNotFoundException e) {
			Log.e("Hertz", "Tried to append header to invalid file: " + e.getLocalizedMessage());
			return;
		} catch (IOException e) {
			Log.e("Hertz", "IO Error during header append: " + e.getLocalizedMessage());
			return;
		}
	}
}