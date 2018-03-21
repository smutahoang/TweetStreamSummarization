package l3s.tts.configure;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;



public class Configure {
	
	
	public enum UpdatingType {
		PERIOD, // update every TIME_STEP_WIDTH
		TWEET_COUNT // update every TWEET_WINDOW
	}
	// option to ignore some general subtopic
	public enum IgnoringType{
		TOPIC_COUNT,
		NOVELTY
	}
	// constants for directory
	public static String STOPWORD_PATH;
	public static String WORKING_DIRECTORY;

	//constants for building graph
	public static int WINDOW_SIZE = 4; // to form edges of graph
	//constants for random walks
	public static int NUMBER_OF_RANDOM_WALK_AT_EACH_NODE = 4;
	public static int RANDOM_WALK_LENGTH = 20;
	public static double DAMPING_FACTOR = 0.15;


	
	//constants to select subtopics
	public static int L_EXPANSION = 1;
	public static int TOP_K_PAGERANK = 10;
	public static double MAGINAL_UTILITY = 0.05;
	public static int NUMBER_OF_IGNORED_TOPICS = 3; 
	public static double NOVELTY_RATIO = 0.1;// 10% 
	
	//constants to select top tweets for each subtopics
	public static int TWEETS_IN_EACH_SUBTOPIC = 5;
	public static double JACCARD_THRESOLD = 0.3; // to ignore similar tweets for each subtopic
	public static boolean OVERLAPPING_TOPICS = false;
	public static double JACCARD_UTILITY = 0.01; // to remove redundancy in the last step

	// constants for updating model
	public final static UpdatingType updatingType = UpdatingType.PERIOD;
	public final static IgnoringType ignoringType = IgnoringType.NOVELTY;
	public static int TWEET_WINDOW = 1000; // update every 1000 tweets
	public final static long TIME_STEP_WIDTH = 60 * 60 * 1000;// 60 mins;
	public static int NUMBER_OF_REMOVING_TWEETS = 100;// for updating step
	public static int FORGOTTON_WINDOW_DISTANCE = 12;
	public static String DATE_TIME_FORMAT = "EEE MMM dd HH:mm:ss +0000 yyyy";
	// set of stopwords
	public static HashSet<String> stopWords;
	
	
	public Configure() {
		// TODO Auto-generated constructor stub
		 WORKING_DIRECTORY = "D:/Alexandria/summarization/TweetStreamSummarization/data";

		//WORKING_DIRECTORY = "E:/code/java/TweetStreamSummarization/data";

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
