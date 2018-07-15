package l3s.tts.baseline.lexrank;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import l3s.tts.configure.Configure;
import l3s.tts.configure.Configure.UpdatingType;
import l3s.tts.utils.KeyValuePair;
import l3s.tts.utils.TimeUtils;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetPreprocessingUtils;
import l3s.tts.utils.TweetStream;

public class LexRank {

	private TweetStream stream;
	private int K;
	private String outputPath;

	private long refTime;
	private long nextUpdate;
	private int nTweets;
	private int currentTimeStep;

	private int startTimeStep;
	private int endTimeStep;
	private LinkedList<Tweet> recentTweets;
	private HashMap<String, Integer> termTweetCount;
	private TweetPreprocessingUtils preprocessingUtils;

	/***
	 * rank tweets by LexRank
	 * 
	 * @param tweets
	 * @return
	 */
	public KeyValuePair[] rank(List<Tweet> tweets, double similarityThreshold, boolean useContinuous) {
		HashMap<Integer, HashMap<Integer, Double>> adjacentTweets = new HashMap<Integer, HashMap<Integer, Double>>();
		double[] sumWeightAdjTweets = new double[tweets.size()];
		for (int i = 0; i < tweets.size(); i++) {
			sumWeightAdjTweets[i] = 0;
		}
		for (int i = 0; i < tweets.size(); i++) {
			Tweet srcTweet = tweets.get(i);
			HashMap<Integer, Double> srcAdjList = new HashMap<Integer, Double>();
			for (int j = i + 1; j < tweets.size(); j++) {
				Tweet desTweet = tweets.get(j);
				double sim = srcTweet.getSimilarity(desTweet);
				if (sim < similarityThreshold) {
					continue;
				}
				srcAdjList.put(j, sim);
				sumWeightAdjTweets[i] += sim;
				if (adjacentTweets.containsKey(j)) {
					adjacentTweets.get(j).put(i, sim);
				} else {
					HashMap<Integer, Double> desAdjList = new HashMap<Integer, Double>();
					desAdjList.put(i, sim);
					adjacentTweets.put(j, desAdjList);
				}
				sumWeightAdjTweets[j] += sim;
			}
			adjacentTweets.put(i, srcAdjList);
		}

		double[] preScores = new double[tweets.size()];
		double[] currScores = new double[tweets.size()];
		for (int i = 0; i < preScores.length; i++) {
			preScores[i] = 1.0 / preScores.length;
		}
		for (int iter = 0; iter < Configure.LEXRANK_NUM_ITERATIONS; iter++) {
			double sum = 0;
			for (int i = 0; i < preScores.length; i++) {
				currScores[i] = 0;
				for (Map.Entry<Integer, Double> pair : adjacentTweets.get(i).entrySet()) {
					int j = pair.getKey();
					if (useContinuous) {
						double sim = pair.getValue();
						currScores[i] += preScores[j] * sim / sumWeightAdjTweets[j];
					} else {
						currScores[i] += preScores[j] / adjacentTweets.get(j).size();
					}
				}
				currScores[i] = Configure.DAMPING_FACTOR / preScores.length
						+ (1 - Configure.DAMPING_FACTOR) * currScores[i];
				sum += currScores[i];
			}
			for (int i = 0; i < preScores.length; i++) {
				preScores[i] = currScores[i] / sum;
			}
		}
		KeyValuePair[] pairs = new KeyValuePair[tweets.size()];
		for (int i = 0; i < preScores.length; i++) {
			pairs[i] = new KeyValuePair(i, preScores[i]);
		}
		Arrays.sort(pairs);
		return pairs;
	}

	/***
	 * extractive summary by LexRank
	 * 
	 * @param tweets
	 * @param edgeSimilarityThreshold
	 * @param useContinuous
	 * @param K
	 * @param summarySimilarityThreshold
	 * @return
	 */
	public List<Tweet> summary(List<Tweet> tweets, double edgeSimilarityThreshold, boolean useContinuous, int K,
			double summarySimilarityThreshold) {
		HashMap<Integer, HashMap<Integer, Double>> adjacentTweets = new HashMap<Integer, HashMap<Integer, Double>>();
		double[] sumWeightAdjTweets = new double[tweets.size()];
		for (int i = 0; i < tweets.size(); i++) {
			sumWeightAdjTweets[i] = 0;
		}
		for (int i = 0; i < tweets.size(); i++) {
			Tweet srcTweet = tweets.get(i);
			HashMap<Integer, Double> srcAdjList = new HashMap<Integer, Double>();
			for (int j = i + 1; j < tweets.size(); j++) {
				Tweet desTweet = tweets.get(j);
				double sim = srcTweet.getSimilarity(desTweet);
				if (sim < edgeSimilarityThreshold) {
					continue;
				}
				srcAdjList.put(j, sim);
				sumWeightAdjTweets[i] += sim;
				if (adjacentTweets.containsKey(j)) {
					adjacentTweets.get(j).put(i, sim);
				} else {
					HashMap<Integer, Double> desAdjList = new HashMap<Integer, Double>();
					desAdjList.put(i, sim);
					adjacentTweets.put(j, desAdjList);
				}
				sumWeightAdjTweets[j] += sim;
			}
			adjacentTweets.put(i, srcAdjList);
		}

		double[] preScores = new double[tweets.size()];
		double[] currScores = new double[tweets.size()];
		for (int i = 0; i < preScores.length; i++) {
			preScores[i] = 1.0 / preScores.length;
		}
		for (int iter = 0; iter < Configure.LEXRANK_NUM_ITERATIONS; iter++) {
			double sum = 0;
			for (int i = 0; i < preScores.length; i++) {
				currScores[i] = 0;
				for (Map.Entry<Integer, Double> pair : adjacentTweets.get(i).entrySet()) {
					int j = pair.getKey();
					if (useContinuous) {
						double sim = pair.getValue();
						currScores[i] += preScores[j] * sim / sumWeightAdjTweets[j];
					} else {
						currScores[i] += preScores[j] / adjacentTweets.get(j).size();
					}
				}
				currScores[i] = Configure.DAMPING_FACTOR / preScores.length
						+ (1 - Configure.DAMPING_FACTOR) * currScores[i];
				sum += currScores[i];
			}

			for (int i = 0; i < preScores.length; i++) {
				preScores[i] = currScores[i] / sum;
			}
		}
		KeyValuePair[] pairs = new KeyValuePair[tweets.size()];
		for (int i = 0; i < preScores.length; i++) {
			pairs[i] = new KeyValuePair(i, preScores[i]);
		}

		Arrays.sort(pairs);
		boolean mark[] = new boolean[tweets.size()];
		for (int i = 0; i < mark.length; i++) {
			mark[i] = false;
		}

		List<Tweet> summary = new ArrayList<Tweet>();
		for (int i = pairs.length - 1; i >= 0; i--) {
			int index = pairs[i].getIntKey();
			if (mark[index]) {
				continue;
			}
			mark[index] = true;
			summary.add(tweets.get(index));
			for (Map.Entry<Integer, Double> pair : adjacentTweets.get(index).entrySet()) {
				int j = pair.getKey();
				double sim = pair.getValue();
				if (sim >= summarySimilarityThreshold) {
					mark[j] = true;
				}
			}
			if (summary.size() >= K) {
				break;
			}
		}

		return summary;
	}

	/***
	 * extractive summary by LexRank
	 * 
	 * @param tweets
	 * @param edgeSimilarityThreshold
	 * @param useContinuous
	 * @param K
	 * @param summarySimilarityThreshold
	 * @return
	 */
	public List<Tweet> efficientSummary(List<Tweet> tweets, double edgeSimilarityThreshold, boolean useContinuous,
			int K, double summarySimilarityThreshold) {
		HashMap<Integer, HashMap<Integer, Double>> adjacentTweets = new HashMap<Integer, HashMap<Integer, Double>>();
		double[] sumWeightAdjTweets = new double[tweets.size()];
		double[] tweetNorm = new double[tweets.size()];
		for (int i = 0; i < tweets.size(); i++) {
			sumWeightAdjTweets[i] = 0;
			tweetNorm[i] = tweets.get(i).getNorm();
		}
		for (int i = 0; i < tweets.size(); i++) {
			Tweet srcTweet = tweets.get(i);
			HashMap<Integer, Double> srcAdjList = new HashMap<Integer, Double>();
			for (int j = i + 1; j < tweets.size(); j++) {
				Tweet desTweet = tweets.get(j);
				double sim = srcTweet.getDotProduct(desTweet) / (tweetNorm[i] * tweetNorm[j]);
				if (sim < edgeSimilarityThreshold) {
					continue;
				}
				srcAdjList.put(j, sim);
				sumWeightAdjTweets[i] += sim;
				if (adjacentTweets.containsKey(j)) {
					adjacentTweets.get(j).put(i, sim);
				} else {
					HashMap<Integer, Double> desAdjList = new HashMap<Integer, Double>();
					desAdjList.put(i, sim);
					adjacentTweets.put(j, desAdjList);
				}
				sumWeightAdjTweets[j] += sim;
			}
			adjacentTweets.put(i, srcAdjList);
		}

		double[] preScores = new double[tweets.size()];
		double[] currScores = new double[tweets.size()];
		for (int i = 0; i < preScores.length; i++) {
			preScores[i] = 1.0 / preScores.length;
		}
		for (int iter = 0; iter < Configure.LEXRANK_NUM_ITERATIONS; iter++) {
			double sum = 0;
			for (int i = 0; i < preScores.length; i++) {
				currScores[i] = 0;
				for (Map.Entry<Integer, Double> pair : adjacentTweets.get(i).entrySet()) {
					int j = pair.getKey();
					if (useContinuous) {
						double sim = pair.getValue();
						currScores[i] += preScores[j] * sim / sumWeightAdjTweets[j];
					} else {
						currScores[i] += preScores[j] / adjacentTweets.get(j).size();
					}
				}
				currScores[i] = Configure.DAMPING_FACTOR / preScores.length
						+ (1 - Configure.DAMPING_FACTOR) * currScores[i];
				sum += currScores[i];
			}

			for (int i = 0; i < preScores.length; i++) {
				preScores[i] = currScores[i] / sum;
			}
		}
		KeyValuePair[] pairs = new KeyValuePair[tweets.size()];
		for (int i = 0; i < preScores.length; i++) {
			pairs[i] = new KeyValuePair(i, preScores[i]);
		}

		Arrays.sort(pairs);
		boolean mark[] = new boolean[tweets.size()];
		for (int i = 0; i < mark.length; i++) {
			mark[i] = false;
		}

		List<Tweet> summary = new ArrayList<Tweet>();
		for (int i = pairs.length - 1; i >= 0; i--) {
			int index = pairs[i].getIntKey();
			if (mark[index]) {
				continue;
			}
			mark[index] = true;
			summary.add(tweets.get(index));
			for (Map.Entry<Integer, Double> pair : adjacentTweets.get(index).entrySet()) {
				int j = pair.getKey();
				double sim = pair.getValue();
				if (sim >= summarySimilarityThreshold) {
					mark[j] = true;
				}
			}
			if (summary.size() >= K) {
				break;
			}
		}

		return summary;
	}

	/***
	 * 
	 */
	public LexRank() {

	}

	/***
	 * 
	 * @param _stream
	 */
	public LexRank(TweetStream _stream, int _K, String _outputPath) {
		stream = _stream;
		K = _K;
		outputPath = _outputPath;

		preprocessingUtils = new TweetPreprocessingUtils();
		recentTweets = new LinkedList<Tweet>();
		termTweetCount = new HashMap<String, Integer>();
		// fist tweet
		Tweet tweet = stream.getTweet();
		nTweets = 1;
		refTime = tweet.getPublishedTime();
		nextUpdate = refTime + Configure.TIME_STEP_WIDTH;

		currentTimeStep = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);
		tweet.setTimeStep(currentTimeStep);

		List<String> terms = tweet.getTerms(preprocessingUtils);
		for (String term : terms) {
			if (termTweetCount.containsKey(term)) {
				termTweetCount.put(term, 1 + termTweetCount.get(term));
			} else {
				termTweetCount.put(term, 1);
			}
		}
		recentTweets.add(tweet);
		startTimeStep = 0;
		endTimeStep = Integer.MAX_VALUE;
	}

	public LexRank(TweetStream _stream, int _K, int _startTimeStep, int _endTimeStep, String _outputPath) {
		stream = _stream;
		K = _K;
		outputPath = _outputPath;

		preprocessingUtils = new TweetPreprocessingUtils();
		recentTweets = new LinkedList<Tweet>();
		termTweetCount = new HashMap<String, Integer>();
		// fist tweet
		Tweet tweet = stream.getTweet();
		nTweets = 1;
		refTime = tweet.getPublishedTime();
		nextUpdate = refTime + Configure.TIME_STEP_WIDTH;

		currentTimeStep = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);
		tweet.setTimeStep(currentTimeStep);

		List<String> terms = tweet.getTerms(preprocessingUtils);
		for (String term : terms) {
			if (termTweetCount.containsKey(term)) {
				termTweetCount.put(term, 1 + termTweetCount.get(term));
			} else {
				termTweetCount.put(term, 1);
			}
		}
		recentTweets.add(tweet);
		startTimeStep = _startTimeStep;
		endTimeStep = _endTimeStep;
	}

	public void process() {
		Tweet tweet = null;
		while ((tweet = stream.getTweet()) != null) {
			if (tweet.getTerms(preprocessingUtils).size() < 1) {
				continue;
			}
			nTweets++;
			currentTimeStep = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);
			tweet.setTimeStep(currentTimeStep);
			List<String> terms = tweet.getTerms(preprocessingUtils);
			for (String term : terms) {
				if (termTweetCount.containsKey(term)) {
					termTweetCount.put(term, 1 + termTweetCount.get(term));
				} else {
					termTweetCount.put(term, 1);
				}
			}
			recentTweets.add(tweet);
			if ((Configure.updatingType == UpdatingType.TWEET_COUNT && nTweets % Configure.TWEET_WINDOW == 0)
					|| (Configure.updatingType == UpdatingType.PERIOD && tweet.getPublishedTime() >= nextUpdate)) {
				genSummary();
				nextUpdate += Configure.TIME_STEP_WIDTH;
			}
		}
	}

	private void genSummary() {
		removeOldTweets();
		if (currentTimeStep < startTimeStep) {
			return;
		}
		if (currentTimeStep >= endTimeStep) {
			System.out.println("FINISHED!!!!");
			System.exit(1);
		}
		List<Tweet> tweets = new ArrayList<Tweet>();
		for (Tweet tweet : recentTweets) {
			tweet.buildVector(termTweetCount, recentTweets.size());
			tweets.add(tweet);
		}
		List<Tweet> selectedTwets = efficientSummary(tweets, Configure.LEXRANK_MIN_EDGE_SIMILARITY, true, K,
				Configure.LEXRANK_MAX_JC_COEFFICIENT);
		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(String.format("%s/%d_lexrank.txt", outputPath, currentTimeStep)));
			for (Tweet tweet : selectedTwets) {
				bw.write(String.format("%s\n", tweet.getText().replace("\n", " ")));
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.printf("******************time = %d***********\n", currentTimeStep);
		for (Tweet tweet : selectedTwets) {
			System.out.printf("[selected tweet] %s\n", tweet.getText());
		}
	}

	private void removeOldTweets() {
		if (currentTimeStep <= Configure.FORGOTTEN_WINDOW_DISTANCE) {
			return;
		}
		int lastTimeStep = currentTimeStep - Configure.FORGOTTEN_WINDOW_DISTANCE;
		while (true) {
			if (recentTweets.getFirst().getTimeStep() < lastTimeStep) {
				Tweet tweet = recentTweets.removeFirst();
				List<String> terms = tweet.getTerms(preprocessingUtils);
				for (String term : terms) {
					int count = termTweetCount.get(term) - 1;
					if (count > 0) {
						termTweetCount.put(term, count);
					} else {
						termTweetCount.remove(term);
					}
				}
			} else {
				break;
			}
		}
	}
}
