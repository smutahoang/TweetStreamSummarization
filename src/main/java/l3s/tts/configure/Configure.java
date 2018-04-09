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
	public enum IgnoringType {
		TOPIC_COUNT, NOVELTY
	}

	//
	public static int NUM_IGNORED_TWEETS = 200;

	// constants for directory
	public static String STOPWORD_PATH;
	public static String WORKING_DIRECTORY;

	// constants for building graph
	public static int WINDOW_SIZE = 4; // to form edges of graph
	// constants for random walks
	public static int NUMBER_OF_RANDOM_WALK_AT_EACH_NODE = 4;
	public static int RANDOM_WALK_LENGTH = 20;
	public static double DAMPING_FACTOR = 0.15;

	// constants to select subtopics
	public static int L_EXPANSION = 1;
	public static int TOP_K_PAGERANK = 10;
	public static double MAGINAL_UTILITY = 0.05;
	public static int NUMBER_OF_IGNORED_TOPICS = 3;
	public static double NOVELTY_RATIO = 0.1;// 10%

	// constants to select top tweets for each subtopics
	public static int TWEETS_IN_EACH_SUBTOPIC = 5;
	public static double JACCARD_THRESOLD = 0.3; // to ignore similar tweets for
													// each subtopic
	public static boolean OVERLAPPING_TOPICS = false;
	public static double JACCARD_UTILITY = 0.01; // to remove redundancy in the
													// last step

	// constants for updating model
	public final static UpdatingType updatingType = UpdatingType.PERIOD;
	public final static IgnoringType ignoringType = IgnoringType.NOVELTY;
	public static int TWEET_WINDOW = 6; // update every 1000 tweets
	public final static long TIME_STEP_WIDTH = 60 * 60 * 1000;// 60 mins;
	public static int NUMBER_OF_REMOVING_TWEETS = 100;// for updating step
	public static int FORGOTTEN_WINDOW_DISTANCE = 12;
	public static String DATE_TIME_FORMAT = "EEE MMM dd HH:mm:ss +0000 yyyy";
	// set of stopwords
	public static HashSet<String> stopWords;

	// constants for baseline models

	public static class TCV_MARKER {
		public static int ID = 0;
		public static int SUM_NORMALIZED_TWEET_VECTOR = 1;
		public static int WEIGHTED_SUM_TWEET_VECTOR = 2;
		public static int SUM_TWEET_TIME = 3;
		public static int SUM_TWEET_SQR_TIME = 4;
		public static int NUM_TWEETS = 5;
		public static int REPRESENTATIVE_TWEETS = 6;
	}

	public static String TCV_FIELD_SEPARATOR = "\t";
	public static String TCV_KEY_VALUE_SEPARATOR = ":";

	public static class TWEET_MARKER {
		public static int ID = 0;
		public static int TEXT = 1;
		public static int USER_ID = 2;
		public static int CREATED_AT = 3;
		public static int TERM_TFIDF = 4;
	}

	public static String SUMBLR_TWEET_FIELD_SEPARATOR = " ";
	public static String SUMBLR_TWEET_KEY_VALUE_SEPARATOR = "=";

	public static double SUMBLR_AMPLIFY_FACTOR = 1000;
	public static int SUMBLR_NUM_REPRESENTATIVE_TWEETS = 10;
	public static double SUMBLR_BETA = 0.3;
	public static int SUMBLR_MU_THRESHOLD = 50;
	public static double SUMBLR_MERGE_THRESHOLD = 0.7;
	public static int SUMBLR_MERGE_TRIGGER = 50;
	public static int SUMBLR_NUMBER_INITIAL_TWEETS = 100;
	public static int SUMBLR_NUMBER_INITIAL_CLUSTERS = 10;
	public static int SUMBLR_NUMBER_KMEANS_ITERATIONS = 10;

	public static int LEXRANK_NUM_ITERATIONS = 50;

	// constants for opinosis model

	public enum scoreFunction {
		GAIN_REDUNDANCY_ONLY, GAIN_WEIGHTED_REDUNDANCY_BY_LEVEL, GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL
	}

	public static String TAGGING_MODEL;
	public static int PERMISSABLE_GAP = 3;
	public static int VSN_POS = 15; // pos thresold of a valid starting node
	public static String ENDTOKENS = ".*(/\\.|/,)";
	public static String STOP_ADDING = "(\\./\\.|!/!|\\?/\\?)";
	public static String VSN_TAGs = ".*(/jj|/rb|/prp$|/vbg|/nn|/dt).*";
	public static String VSN_NAME = "^(its/|the/|when/|a/|an/|this/|the/|they/|it/|i/|we/|our/|if/|for/).*";

	public static String VALID_CANDIDATE1 = ".*(/jj)*.*(/nn)+.*(/vb)+.*(/jj)+.*";
	public static String VALID_CANDIDATE2 = ".*(/dt).*";
	public static String VALID_CANDIDATE3 = ".*(/rb)*.*(/jj)+.*(/nn)+.*";
	public static String VALID_CANDIDATE4 = ".*(/prp|/dt)+.*(/vb)+.*(/rb|/jj)+.*(/nn)+.*";
	public static String VALID_CANDIDATE5 = ".*(/jj)+.*(/to)+.*(/vb).*";
	public static String VALID_CANDIDATE6 = ".*(/rb)+.*(/in)+.*(/nn)+.*";
	public static String VALID_CANDIDATE7 = ".*(/to|/vbz|/in|/cc|wdt|/prp|/dt|/,)";
	public static String OVERLAP_NODE = ".*(/vb[a-z]|/in)"; // a node is a
															// collapsible node
															// if it is a verb

	public static int MIN_REDUNDANCY = 2;
	public static int P_MAX_SENT_LENGTH = 18;
	public static double DUPLICATE_THRESOLD = 0.35;
	public static double REMOVING_DUPLICATE_THRESOLD = 0.5;
	public static boolean IS_COLLAPSE = true;
	public static int ATTACHMENT_AFTER = 2;
	public static boolean TURN_ON_DUP_ELIM = true;
	public static boolean NORMALIZE_OVERALLGAIN = true;
	public static int MAX_SUMMARIES = 5;
	public static double DUPLICATE_COLLAPSE_THRESHOLD = 0.5;
	public static scoreFunction SCORING_FUNCTION = scoreFunction.GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL;

	public Configure() {
		// TODO Auto-generated constructor stub
		// WORKING_DIRECTORY =
		// "D:/Alexandria/summarization/TweetStreamSummarization/data";

		WORKING_DIRECTORY = "E:/code/java/TweetStreamSummarization/data";

		STOPWORD_PATH = String.format("%s/stopwords", WORKING_DIRECTORY);

		stopWords = getStopWords();
		TAGGING_MODEL = WORKING_DIRECTORY + "/taggingModel/model.irc.20121211";
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
