package l3s.tts.configure;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;

public class Configure {
	public enum scoreFunction {
		GAIN_REDUNDANCY_ONLY, GAIN_WEIGHTED_REDUNDANCY_BY_LEVEL, GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL
	}

	public static String STOPWORD_PATH;
	public static String WORKING_DIRECTORY;

	public static int TWEETS_IN_EACH_SUBTOPIC = 5;

	public static int WINDOW_SIZE = 4;
	public static int NUMBER_OF_RANDOM_WALK_AT_EACH_NODE = 4;
	public static int RANDOM_WALK_LENGTH = 20;
	public static double DAMPING_FACTOR = 0.15;
	public static int L_EXPANSION = 1;
	public static double MAGINAL_UTILITY = 0.015;
	public static int MAX_SUMMARIES = 5;
	public static double JACCARD_THRESOLD = 0.5;
	public static int TOP_K = 10;

	public static boolean STOP_AT_ENDINGTOKENS = true;
	public static int NUMBER_OF_REMOVING_TWEETS = 200;
	public static boolean SIMPLE_UPDATE = false;

	public static String DATE_TIME_FORMAT = "EEE MMM dd HH:mm:ss +0000 yyyy";

	public static HashSet<String> stopWords;

	// minutes
	public static int TWEET_WINDOW = 1000;

	public static scoreFunction SCORING_FUNCTION = scoreFunction.GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL;

	public Configure() {
		// TODO Auto-generated constructor stub
		// WORKING_DIRECTORY =
		// "D:/Alexandria/summarization/TweetStreamSummarization/data";

		WORKING_DIRECTORY = "D:\\Alexandria\\summarization\\TweetStreamSummarization\\data";

		STOPWORD_PATH = String.format("%s/stopwords", WORKING_DIRECTORY);

		stopWords = getStopWords();

	}

	// public HashSet<String> getStopWords()
	public HashSet<String> getStopWords() {
		try {
			stopWords = new HashSet<String>();
			BufferedReader br;
			String line = null;

			br = new BufferedReader(
					new FileReader(String.format("%s/common-english-adverbs.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(
					new FileReader(String.format("%s/common-english-prep-conj.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(
					new FileReader(String.format("%s/common-english-words.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(
					new FileReader(String.format("%s/smart-common-words.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(new FileReader(String.format("%s/mysql-stopwords.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(new FileReader(String.format("%s/twitter-slang.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(new FileReader(String.format("%s/shorthen.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			addMoreStopWords();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return stopWords;
	}

	/***
	 * more stop-words found while conducting experiments
	 */
	private void addMoreStopWords() {
		// words due to truncated tweets
		stopWords.add("//t");
		stopWords.add("http");
		stopWords.add("https");
	}
}
