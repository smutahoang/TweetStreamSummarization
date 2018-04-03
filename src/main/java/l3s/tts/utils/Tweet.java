package l3s.tts.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cmu.arktweetnlp.Tagger.TaggedToken;
import l3s.tts.configure.Configure;

public class Tweet {

	private int tweetId;
	private String text;
	private String userId;
	private long createdAt;
	private double weight;
	private int windowId;
	private List<TaggedToken> taggedTokens;
	private List<String> terms;
	private HashMap<String, Double> vector;// for sumblr baseline

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getWeight() {
		return weight;
	}

	public List<TaggedToken> getTaggedTokens() {
		return taggedTokens;
	}

	public static String RETWEET = "RT @";

	public Tweet(String text, String userId, long createdAt) {
		// TODO Auto-generated constructor stub
		// this.tweetId = tweetId;
		this.userId = userId;
		this.text = text;
		this.createdAt = createdAt;
		this.weight = 1.0;
		taggedTokens = null;
	}

	public int getTweetId() {
		return tweetId;
	}

	public void setTweetId(int tweetId) {
		this.tweetId = tweetId;
	}

	public String getText() {
		return text;
	}

	public String getUserId() {
		return userId;
	}

	public long getPublishedTime() {
		return createdAt;
	}

	public boolean isReTweet() {
		return text.trim().startsWith(RETWEET);
	}

	public int getWindowId() {
		return windowId;
	}

	public void setWindowId(int windowId) {
		this.windowId = windowId;
	}

	public List<String> getTerms(TweetPreprocessingUtils preprocessingUtils) {
		if (terms == null) {
			terms = preprocessingUtils.extractTermInTweet(text);
		}
		return terms;
	}

	public void print() {
		System.out.printf("time = %d user = %s tweetId = %s text = %s\n", createdAt, userId, tweetId, text);
	}

	public double getNorm() {
		double norm = 0;
		for (Map.Entry<String, Double> pair : vector.entrySet()) {
			norm += Math.pow(pair.getValue(), 2);
		}
		return Math.sqrt(norm);
	}

	public HashMap<String, Double> getVector() {
		return vector;
	}

	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(
				String.format("%s%s%d", Configure.TWEET_MARKER.ID, Configure.TWEET_KEY_VALUE_SEPARATOR, tweetId));
		strBuilder.append(String.format("%s%s%s%s", Configure.TWEET_FIELD_SEPARATOR, Configure.TWEET_MARKER.TEXT,
				Configure.TWEET_KEY_VALUE_SEPARATOR, text));
		strBuilder.append(String.format("%s%s%s%s", Configure.TWEET_FIELD_SEPARATOR, Configure.TWEET_MARKER.USER_ID,
				Configure.TWEET_KEY_VALUE_SEPARATOR, userId));
		strBuilder.append(String.format("%s%s%s%d", Configure.TWEET_FIELD_SEPARATOR, Configure.TWEET_MARKER.CREATED_AT,
				Configure.TWEET_KEY_VALUE_SEPARATOR, createdAt));
		for (Map.Entry<String, Double> pair : vector.entrySet()) {
			strBuilder.append(String.format("%s%s%s%s%s%.12f", Configure.TWEET_FIELD_SEPARATOR,
					Configure.TWEET_MARKER.TERM_TFIDF, Configure.TWEET_KEY_VALUE_SEPARATOR, pair.getKey(),
					Configure.TWEET_KEY_VALUE_SEPARATOR, pair.getValue() * Configure.AMPLIFY_FACTOR));
		}

		return strBuilder.toString();
	}

	public Tweet(String tweetStr) {
		try {
			vector = new HashMap<String, Double>();
			String[] tokens = tweetStr.split(Configure.TWEET_FIELD_SEPARATOR);
			for (String token : tokens) {
				String[] subTokens = token.split(Configure.TWEET_KEY_VALUE_SEPARATOR);
				int marker = Integer.parseInt(subTokens[0]);
				if (marker == Configure.TWEET_MARKER.ID) {
					tweetId = Integer.parseInt(subTokens[1]);
				} else if (marker == Configure.TWEET_MARKER.USER_ID) {
					userId = subTokens[1];
				} else if (marker == Configure.TWEET_MARKER.TEXT) {
					text = subTokens[1];
				} else if (marker == Configure.TWEET_MARKER.CREATED_AT) {
					createdAt = Long.parseLong(subTokens[1]);
				} else if (marker == Configure.TWEET_MARKER.TERM_TFIDF) {
					vector.put(subTokens[1], Double.parseDouble(subTokens[1]) / Configure.AMPLIFY_FACTOR);
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

}
