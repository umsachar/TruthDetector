package com.truthdetector.objects;

import java.io.File;

public class Globals {
	
	private final static String EXT = ".wav";

	private static Globals instance = new Globals();

	private Globals() {

	}

	public static Globals getInstance() {
		return instance;
	}

	private String filePath;
	private String fileName;
	private String dataFileName = "nnet_data.data";
	private String netFileName = "nnetwork.nnet";

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public static void setInstance(Globals instance) {
		Globals.instance = instance;
	}
	
	public String getAbsFilePath() {
		return filePath + fileName + EXT;
	}

	public String getNewFileName() {
		File savePath = new File(getAbsFilePath());
		int maxIndex = 0;
		for (final File file : savePath.listFiles()) {
			String currName = file.getName();
			currName = currName.substring(0, currName.length() - 4);

			try {
				String[] arr = currName.split("_");
				int index = Integer.parseInt(arr[1]);
				maxIndex = Math.max(index + 1, maxIndex);
			} catch (Exception e) {

			}
		}
		return "record_" + maxIndex;
	}
	
	public void saveFile(String fName) {
		String oldPath = getAbsFilePath();
		fileName = fName;
		String newPath = getAbsFilePath();
		File file = new File(oldPath);
		file.renameTo(new File(newPath));
	}
	
	public String getNetSavePath() {
		return filePath + netFileName;
	}
	
	public String getDataSavePath() {
		return filePath + dataFileName;
	}
}
