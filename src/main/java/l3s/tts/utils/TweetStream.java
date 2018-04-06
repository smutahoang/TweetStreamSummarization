package l3s.tts.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * 
 * @author Huyen Nguyen
 *
 */
public class TweetStream {
	private BufferedReader buff;
	private File[] fileList;
	private int currentFile;

	public void openFile(File file) {

		try {
			buff = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public TweetStream(String streamPath) {
		File streamFolder = new File(streamPath);
		fileList = streamFolder.listFiles();
		int currentFile = 0;
		openFile(fileList[currentFile]);
	}

	/***
	 * @author: Tuan-Anh check if tweet is valid by manually identified rules
	 * 
	 * @param text
	 * @return
	 */
	private boolean isValidTweet(String text) {
		if (text.startsWith("I liked a @YouTube video")) {
			return false;
		}
		return true;
	}

	public Tweet getTweet() {
		Tweet tweet = null;
		String line;
		try {
			line = buff.readLine();
			while (true) {
				if (line == null) {
					buff.close();
					currentFile++;

					if (currentFile == fileList.length)
						return null;
					else {
						openFile(fileList[currentFile]);
						line = buff.readLine();
						continue;
					}
				}

				String[] text = line.split("\t");
				String tweetId = text[0];
				String userId = text[3];
				long createdAt = Long.parseLong(text[2]);
				String content = text[4];

				if (!isValidTweet(content)) {
					line = buff.readLine();
					continue;
				}
				tweet = new Tweet(tweetId, content, userId, createdAt);
				return tweet;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

		return tweet;
	}

}
