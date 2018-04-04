package l3s.tts.baseline.sumblr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;

import l3s.tts.configure.Configure;
import l3s.tts.utils.TimeUtils;
import l3s.tts.utils.Tweet;

public class TCV {

	private class TweetTuple implements Comparable<TweetTuple> {
		private Tweet tweet;
		private double similarity;

		public TweetTuple(Tweet _tweet, double _similarity) {
			tweet = _tweet;
			similarity = _similarity;
		}

		public Tweet getTweet() {
			return tweet;
		}

		public double getSimilarity() {
			return similarity;
		}

		public int compareTo(TweetTuple o) {
			if (similarity < o.getSimilarity()) {
				return -1;
			} else if (similarity > o.getSimilarity()) {
				return 1;
			}
			return 0;
		}

	}

	private int Id;
	private HashMap<String, Double> sumNormalizedTweetVector;
	private HashMap<String, Double> weightedSumTweetVector;
	private int sumTweetTime;
	private int sumTweetSqrTime;
	private int nTweets;
	private HashSet<Tweet> tweets;

	// utility variables
	private double dotProd;
	private double sumSQRWeightedSum;
	private double avgSimilarity;

	/***
	 * Init the TCV from a tweet
	 * 
	 * @param tweet
	 */
	public TCV(Tweet tweet, int _Id, long refTime) {
		this.Id = _Id;
		sumTweetTime = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);
		sumTweetSqrTime = sumTweetTime * sumTweetTime;
		nTweets = 1;
		tweets = new HashSet<Tweet>();
		tweets.add(tweet);
		sumNormalizedTweetVector = new HashMap<String, Double>();
		weightedSumTweetVector = new HashMap<String, Double>();
		double norm = tweet.getNorm();
		double weight = tweet.getWeight();
		dotProd = 0;
		for (Map.Entry<String, Double> pair : tweet.getVector().entrySet()) {
			String word = pair.getKey();
			double value = pair.getValue();
			sumNormalizedTweetVector.put(word, value / norm);
			weightedSumTweetVector.put(word, value * weight);
			dotProd += (value / norm) * (value * weight);
			sumSQRWeightedSum += (value * weight) * (value * weight);
		}
		avgSimilarity = 1;
	}

	/***
	 * 
	 * @return
	 */
	public int getSumTweetTime() {
		return sumTweetSqrTime;
	}

	/***
	 * 
	 * @return
	 */
	public int getSumTweetSqrTime() {
		return sumTweetSqrTime;
	}

	/***
	 * 
	 * @return
	 */
	public double getSumSQRWeightedSum() {
		return sumSQRWeightedSum;
	}

	/***
	 * 
	 * @return
	 */
	public int getNTweets() {
		return nTweets;
	}

	/***
	 * 
	 * @return
	 */
	public int getClusterId() {
		return Id;
	}

	/***
	 * 
	 * @return
	 */
	public HashMap<String, Double> getSumNormalizedTweetVector() {
		return sumNormalizedTweetVector;
	}

	/***
	 * 
	 * @return
	 */
	public HashMap<String, Double> getWeightedSumTweetVector() {
		return weightedSumTweetVector;
	}

	/***
	 * 
	 * @return
	 */
	public HashSet<Tweet> getTweets() {
		return tweets;
	}

	/***
	 * measure input tweet's similarity to centroid
	 * 
	 * @param tweet
	 * @return
	 */
	public double getSimilarity(Tweet tweet) {
		double sim = 0;
		double tweetNorm = 0;
		for (Map.Entry<String, Double> pair : tweet.getVector().entrySet()) {
			String word = pair.getKey();
			double value = pair.getValue();
			if (weightedSumTweetVector.containsKey(word)) {
				sim += pair.getValue() * weightedSumTweetVector.get(word);
			}
			tweetNorm += value * value;
		}
		return sim / Math.sqrt(tweetNorm * sumSQRWeightedSum);
	}

	/***
	 * 
	 * @param other
	 * @return
	 */
	public double getSimilarity(TCV other) {
		double sim = 0;
		for (Map.Entry<String, Double> pair : other.getWeightedSumTweetVector().entrySet()) {
			String word = pair.getKey();
			double value = pair.getValue();
			if (weightedSumTweetVector.containsKey(word)) {
				sim += value * weightedSumTweetVector.get(word);
			}
		}
		return sim / Math.sqrt(sumSQRWeightedSum * other.getSumSQRWeightedSum());
	}

	/***
	 * add a new tweet to this cluster
	 * 
	 * @param tweet
	 */
	public void addTweet(Tweet tweet, long refTime) {
		// update the word maps
		double norm = tweet.getNorm();
		double weight = tweet.getWeight();
		for (Map.Entry<String, Double> pair : tweet.getVector().entrySet()) {
			String word = pair.getKey();
			double value = pair.getValue();
			if (sumNormalizedTweetVector.containsKey(word)) {
				double preNormalizedValue = sumNormalizedTweetVector.get(word);
				sumNormalizedTweetVector.put(word, value / norm + preNormalizedValue);
				double preWeightedSumValue = weightedSumTweetVector.get(word);
				weightedSumTweetVector.put(word, value * weight + preWeightedSumValue);
				dotProd -= preNormalizedValue * preWeightedSumValue;
				dotProd += (value * weight + preWeightedSumValue) * (value * weight + preWeightedSumValue);
				sumSQRWeightedSum -= preWeightedSumValue * preWeightedSumValue;
				sumSQRWeightedSum += (value * weight + preWeightedSumValue) * (value * weight + preWeightedSumValue);
			} else {
				sumNormalizedTweetVector.put(word, value / norm);
				weightedSumTweetVector.put(word, value * weight);
				dotProd += (value / norm) * (value * weight);
				sumSQRWeightedSum += (value * weight) * (value * weight);
			}
		}
		int time = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);

		// update time sums
		sumTweetTime += time;
		sumTweetSqrTime += time * time;

		// update representative tweets
		if (tweets.size() < Configure.NUM_REPRESENTATIVE_TWEETS) {
			tweets.add(tweet);
		} else {
			PriorityQueue<TweetTuple> queue = new PriorityQueue<TweetTuple>();
			queue.add(new TweetTuple(tweet, getSimilarity(tweet)));
			for (Tweet rTweet : tweets) {
				TweetTuple tuple = new TweetTuple(rTweet, getSimilarity(rTweet));
				queue.add(tuple);
			}
			queue.poll();
			tweets.clear();
			while (queue.size() > 0) {
				tweets.add(queue.poll().getTweet());
			}
		}
		// update average similarity
		avgSimilarity = dotProd / (nTweets * Math.sqrt(sumSQRWeightedSum));
	}

	/***
	 * get average similarity of tweets to centroid
	 * 
	 * @return
	 */
	public double getAvgSimilarity() {
		return avgSimilarity;
	}

	/****
	 * 
	 * @param quantile
	 * @return
	 */
	public double getFreshness(double quantile) {
		double muTime = sumTweetTime / nTweets;
		if (nTweets < Configure.MU_THRESHOLD)
			return muTime;
		double sigmaTime = Math.sqrt(sumTweetSqrTime / nTweets - muTime * muTime);
		return muTime + sigmaTime * quantile;
	}

	/***
	 * 
	 * @param other
	 */
	public void merge(TCV other) {
		// vector
		for (Map.Entry<String, Double> pair : other.getSumNormalizedTweetVector().entrySet()) {
			String word = pair.getKey();
			if (sumNormalizedTweetVector.containsKey(word)) {
				double preNormalizedValue = sumNormalizedTweetVector.get(word);
				double newNormalizedValue = preNormalizedValue + pair.getValue();
				double preWeightedSumValue = weightedSumTweetVector.get(word);
				double newWeightedSumValue = preWeightedSumValue + other.getWeightedSumTweetVector().get(word);

				sumNormalizedTweetVector.put(word, newNormalizedValue);
				weightedSumTweetVector.put(word, newWeightedSumValue);
				dotProd -= preNormalizedValue * preWeightedSumValue;
				dotProd += newNormalizedValue * newWeightedSumValue;
				sumSQRWeightedSum -= preWeightedSumValue * preWeightedSumValue;
				sumSQRWeightedSum += newWeightedSumValue * newWeightedSumValue;
			} else {
				sumNormalizedTweetVector.put(word, pair.getValue());
				double w = other.getWeightedSumTweetVector().get(word);
				weightedSumTweetVector.put(word, w);
				dotProd += pair.getValue() * w;
				sumSQRWeightedSum -= w * w;
			}
		}

		// update time
		sumTweetTime += other.getSumTweetTime();
		sumTweetSqrTime += other.getSumTweetSqrTime();

		// update representative tweets
		HashSet<Tweet> allTweets = new HashSet<Tweet>();
		allTweets.addAll(tweets);
		allTweets.addAll(other.getTweets());
		if (allTweets.size() < Configure.NUM_REPRESENTATIVE_TWEETS) {
			tweets = allTweets;
		} else {
			PriorityQueue<TweetTuple> queue = new PriorityQueue<TweetTuple>();
			for (Tweet tweet : allTweets) {
				TweetTuple tuple = new TweetTuple(tweet, getSimilarity(tweet));
				queue.add(tuple);
			}
			queue.poll();
			tweets.clear();
			while (queue.size() > 0) {
				tweets.add(queue.poll().getTweet());
			}
		}
		// update average similarity
		avgSimilarity = dotProd / (nTweets * Math.sqrt(sumSQRWeightedSum));
	}

	/***
	 * 
	 * @param tcvStr
	 */
	public TCV(String tcvStr) {
		try {
			weightedSumTweetVector = new HashMap<String, Double>();
			sumNormalizedTweetVector = new HashMap<String, Double>();
			tweets = new HashSet<Tweet>();
			String[] tokens = tcvStr.split(Configure.TCV_FIELD_SEPARATOR);
			for (String token : tokens) {
				String[] subTokens = token.split(Configure.TCV_KEY_VALUE_SEPARATOR);
				int marker = Integer.parseInt(subTokens[0]);
				if (marker == Configure.TCV_MARKER.ID) {
					Id = Integer.parseInt(subTokens[1]);
				} else if (marker == Configure.TCV_MARKER.NUM_TWEETS) {
					nTweets = Integer.parseInt(subTokens[1]);
				} else if (marker == Configure.TCV_MARKER.SUM_TWEET_TIME) {
					sumTweetTime = Integer.parseInt(subTokens[1]);
				} else if (marker == Configure.TCV_MARKER.SUM_TWEET_SQR_TIME) {
					sumTweetTime = Integer.parseInt(subTokens[1]);
				} else if (marker == Configure.TCV_MARKER.WEIGHTED_SUM_TWEET_VECTOR) {
					weightedSumTweetVector.put(subTokens[1],
							Double.parseDouble(subTokens[2]) / Configure.AMPLIFY_FACTOR);
				} else if (marker == Configure.TCV_MARKER.SUM_NORMALIZED_TWEET_VECTOR) {
					sumNormalizedTweetVector.put(subTokens[1],
							Double.parseDouble(subTokens[2]) / Configure.AMPLIFY_FACTOR);
				} else if (marker == Configure.TCV_MARKER.REPRESENTATIVE_TWEETS) {
					Tweet tweet = new Tweet(subTokens[1]);
					tweets.add(tweet);
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
		StringBuilder strBuilder = new StringBuilder(
				String.format("%s%s%d", Configure.TCV_MARKER.ID, Configure.TCV_KEY_VALUE_SEPARATOR, Id));
		strBuilder.append(String.format("%s%d%s%d", Configure.TCV_FIELD_SEPARATOR, Configure.TCV_MARKER.NUM_TWEETS,
				Configure.TCV_KEY_VALUE_SEPARATOR, nTweets));
		strBuilder.append(String.format("%s%d%s%d", Configure.TCV_FIELD_SEPARATOR, Configure.TCV_MARKER.SUM_TWEET_TIME,
				Configure.TCV_KEY_VALUE_SEPARATOR, sumTweetTime));
		strBuilder.append(String.format("%s%d%s%d", Configure.TCV_FIELD_SEPARATOR,
				Configure.TCV_MARKER.SUM_TWEET_SQR_TIME, sumTweetSqrTime));
		for (Map.Entry<String, Double> pair : weightedSumTweetVector.entrySet()) {
			strBuilder.append(String.format("%s%d%s%s%s%.12f", Configure.TCV_FIELD_SEPARATOR,
					Configure.TCV_MARKER.WEIGHTED_SUM_TWEET_VECTOR, Configure.TCV_KEY_VALUE_SEPARATOR, pair.getKey(),
					Configure.TCV_KEY_VALUE_SEPARATOR, pair.getValue() * Configure.AMPLIFY_FACTOR));
		}
		for (Map.Entry<String, Double> pair : sumNormalizedTweetVector.entrySet()) {
			strBuilder.append(String.format("%s%d%s%s%s%.12f", Configure.TCV_FIELD_SEPARATOR,
					Configure.TCV_MARKER.SUM_NORMALIZED_TWEET_VECTOR, Configure.TCV_KEY_VALUE_SEPARATOR, pair.getKey(),
					Configure.TCV_KEY_VALUE_SEPARATOR, pair.getValue() * Configure.AMPLIFY_FACTOR));
		}
		for (Tweet tweet : tweets) {
			strBuilder.append(String.format("%s%d%s%s", Configure.TCV_FIELD_SEPARATOR,
					Configure.TCV_MARKER.REPRESENTATIVE_TWEETS, Configure.TCV_KEY_VALUE_SEPARATOR, tweet.toString()));
		}
		return strBuilder.toString();
	}
}
