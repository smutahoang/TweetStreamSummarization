package l3s.tts.baseline.sumblr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import l3s.tts.utils.Tweet;

public class TCV {
	private static class MARKER {
		public static int ID = 0;
		public static int SUM_TWEET_VECTOR = 1;
		public static int SUM_TWEET_SQR_VECTOR = 2;
		public static int SUM_TWEET_TIME = 3;
		public static int SUM_TWEET_SQR_TIME = 4;
		public static int NUM_TWEETS = 5;
	}

	private static String FIELD_SEPARATOR = "\t";
	private static String KEY_VALUE_SEPARATOR = ":";
	private static double AMPLIFY_FACTOR = 1000;

	private int Id;
	private HashMap<String, Double> sumTweetVector;
	private HashMap<String, Double> sumTweetSqrVector;
	private int sumTweetTime;
	private int sumTweetSqrTime;
	private int nTweets;
	private HashSet<Tweet> focusTweets;

	/***
	 * Init the TCV from a tweet
	 * 
	 * @param tweet
	 */
	public TCV(Tweet tweet) {

	}

	/***
	 * 
	 * @param tcvStr
	 */
	public TCV(String tcvStr) {
		try {
			sumTweetVector = new HashMap<String, Double>();
			sumTweetSqrVector = new HashMap<String, Double>();
			String[] tokens = tcvStr.split(FIELD_SEPARATOR);
			for (String token : tokens) {
				String[] subTokens = token.split(KEY_VALUE_SEPARATOR);
				int marker = Integer.parseInt(subTokens[0]);
				if (marker == MARKER.ID) {
					Id = Integer.parseInt(subTokens[1]);
				} else if (marker == MARKER.NUM_TWEETS) {
					nTweets = Integer.parseInt(subTokens[1]);
				} else if (marker == MARKER.SUM_TWEET_TIME) {
					sumTweetTime = Integer.parseInt(subTokens[1]);
				} else if (marker == MARKER.SUM_TWEET_SQR_TIME) {
					sumTweetTime = Integer.parseInt(subTokens[1]);
				} else if (marker == MARKER.SUM_TWEET_VECTOR) {
					sumTweetVector.put(subTokens[1], Double.parseDouble(subTokens[2]));
				} else if (marker == MARKER.SUM_TWEET_SQR_VECTOR) {
					sumTweetSqrVector.put(subTokens[1], Double.parseDouble(subTokens[2]));
				} else {
					System.out.printf("ERROR when parsing token = %s\n", token);
					System.exit(-1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/***
	 * return a string that represents the TCV
	 */
	public String toString() {
		StringBuilder strBuilder = new StringBuilder(String.format("%s%s%d", MARKER.ID, KEY_VALUE_SEPARATOR, Id));
		strBuilder.append(String.format("%s%d%s%d", FIELD_SEPARATOR, MARKER.NUM_TWEETS, KEY_VALUE_SEPARATOR, nTweets));
		strBuilder.append(
				String.format("%s%d%s%d", FIELD_SEPARATOR, MARKER.SUM_TWEET_TIME, KEY_VALUE_SEPARATOR, sumTweetTime));
		strBuilder.append(String.format("%s%d%s%d", FIELD_SEPARATOR, MARKER.SUM_TWEET_SQR_TIME, sumTweetSqrTime));
		for (Map.Entry<String, Double> pair : sumTweetVector.entrySet()) {
			strBuilder.append(String.format("%s%d%s%s%s%.12f", FIELD_SEPARATOR, MARKER.SUM_TWEET_VECTOR,
					KEY_VALUE_SEPARATOR, pair.getKey(), KEY_VALUE_SEPARATOR, pair.getValue() * AMPLIFY_FACTOR));
		}
		for (Map.Entry<String, Double> pair : sumTweetSqrVector.entrySet()) {
			strBuilder.append(String.format("%s%d%s%s%s%.12f", FIELD_SEPARATOR, MARKER.SUM_TWEET_SQR_VECTOR,
					KEY_VALUE_SEPARATOR, pair.getKey(), KEY_VALUE_SEPARATOR, pair.getValue() * AMPLIFY_FACTOR));
		}
		return strBuilder.toString();
	}
}
