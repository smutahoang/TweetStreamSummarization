package l3s.tts.configure;

public class Configure {
	public enum scoreFunction {
		GAIN_REDUNDANCY_ONLY, GAIN_WEIGHTED_REDUNDANCY_BY_LEVEL, GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL
	}

	public static String STOPWORD_PATH;
	public static String WORKING_DIRECTORY;
	public static String PROPERTIES_PATH;
	public static String TAGGING_MODEL;
	public static int MAX_SUMMARIES = 5;
	public static int PERMISSABLE_GAP = 5;
	public static int SAMPLE_NUMBER = 4;
	public static int VSN_POS = 15; // pos thresold of a valid starting node
	public static boolean STOP_AT_ENDINGTOKENS = true;
	public static int NUMBER_OF_REMOVING_TWEETS = 2;
	public static boolean SIMPLE_UPDATE = false;
	public static String ENDTOKENS = "\\?|(\\.){1,4}|(\\!){1,4}";
	public static String VSN_TAGs = ".*(/jj|/rb|/prp$|/vbg|/nn|/dt).*";
	public static String VSN_NAME = "^(its/|the/|when/|a/|an/|this/|the/|they/|it/|i/|we/|our/|if/|for/).*";
	public static String DATE_TIME_FORMAT = "EEE MMM dd HH:mm:ss +0000 yyyy";

	public static String VALID_CANDIDATE1 = ".*(/jj)*.*(/nn)+.*(/vb)+.*(/jj)+.*";
	public static String VALID_CANDIDATE2 = ".*(/dt).*";
	public static String VALID_CANDIDATE3 = ".*(/rb)*.*(/jj)+.*(/nn)+.*";
	public static String VALID_CANDIDATE4 = ".*(/prp|/dt)+.*(/vb)+.*(/rb|/jj)+.*(/nn)+.*";
	public static String VALID_CANDIDATE5 = ".*(/jj)+.*(/to)+.*(/vb).*";
	public static String VALID_CANDIDATE6 = ".*(/rb)+.*(/in)+.*(/nn)+.*";
	public static String VALID_CANDIDATE7 = ".*(/to|/vbz|/in|/cc|wdt|/prp|/dt|/,)";
	public static String OVERLAP_NODE = ".*(/vb[a-z]|/in)"; // a node is a collapsible node if it is a verb

	public static int MIN_REDUNDANCY = 2;
	public static int P_MAX_SENT_LENGTH = 18;
	public static double DUPLICATE_PREFIX_THRESOLD = 0.4;
	public static double DUPLICATE_SUFFIX_THRESOLD = 0.4;
	public static double DUPLICATE_THRESOLD = 0.3;
	public static boolean IS_COLLAPSE = true ;
//	public static long TIME_STEP_WIDTH = 10 * 60 * 1000; // Update every ten minutes
	public static int TWEET_WINDOW = 1000;

	public static scoreFunction SCORING_FUNCTION = scoreFunction.GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL;

	public Configure() {
		// TODO Auto-generated constructor stub
		WORKING_DIRECTORY = "D:\\Alexandria\\summarization\\TweetStreamSummarization\\data";
		TAGGING_MODEL = WORKING_DIRECTORY+"//taggingModel//model.irc.20121211";
		STOPWORD_PATH = String.format("%s/stopwords", WORKING_DIRECTORY);
		PROPERTIES_PATH = String.format("%s/summrary.properties", WORKING_DIRECTORY);
	}
}
