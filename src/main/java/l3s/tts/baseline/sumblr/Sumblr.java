package l3s.tts.baseline.sumblr;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import l3s.tts.baseline.lexrank.LexRank;
import l3s.tts.configure.Configure;
import l3s.tts.configure.Configure.UpdatingType;
import l3s.tts.utils.KeyValuePair;
import l3s.tts.utils.TimeUtils;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetPreprocessingUtils;
import l3s.tts.utils.TweetStream;

public class Sumblr {

	private HashMap<Integer, TCV> clusters;
	private TweetStream stream;
	private String outputPath;

	private long refTime;
	private int nClusters;
	private long nextUpdate;
	private int nTweets;
	private int currentTimeStep;
	private HashMap<String, Integer> termTweetCount;
	private TweetPreprocessingUtils preprocessingUtils;
	private LexRank lexRanker;

	// utility variable
	private List<Tweet> firstTweets;
	double quantile = Util.getQuantile(0.95);

	private class ClusterPair implements Comparable<ClusterPair> {
		private int cId1;
		private int cId2;
		private double similarity;

		public ClusterPair(int _cId1, int _cId2, double _similarity) {
			cId1 = _cId1;
			cId2 = _cId2;
			similarity = _similarity;
		}

		public int getClusterId1() {
			return cId1;
		}

		public int getClusterId2() {
			return cId2;
		}

		public double getSimilarity() {
			return similarity;
		}

		public int compareTo(ClusterPair o) {
			if (similarity < o.getSimilarity()) {
				return 1;
			} else if (similarity > o.getSimilarity()) {
				return -1;
			} else {
				return 0;
			}
		}

	}

	public Sumblr(TweetStream _stream, String _outputPath) {
		stream = _stream;
		outputPath = _outputPath;

		clusters = new HashMap<Integer, TCV>();
		preprocessingUtils = new TweetPreprocessingUtils();
		termTweetCount = new HashMap<String, Integer>();
		firstTweets = new ArrayList<Tweet>();
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
		firstTweets.add(tweet);
		initClusters();
		lexRanker = new LexRank();
	}

	private void initClusters() {
		System.out.println("initializing clusters");
		Tweet tweet = null;
		while ((tweet = stream.getTweet()) != null) {
			nTweets++;
			firstTweets.add(tweet);
			tweet.getTerms(preprocessingUtils);
			List<String> terms = tweet.getTerms(preprocessingUtils);
			for (String term : terms) {
				if (termTweetCount.containsKey(term)) {
					termTweetCount.put(term, 1 + termTweetCount.get(term));
				} else {
					termTweetCount.put(term, 1);
				}
			}
			if ((Configure.updatingType == UpdatingType.TWEET_COUNT && nTweets % Configure.TWEET_WINDOW == 0)
					|| (Configure.updatingType == UpdatingType.PERIOD && tweet.getPublishedTime() > nextUpdate)) {
				break;
			}
		}
		for (int i = 0; i < firstTweets.size(); i++) {
			firstTweets.get(i).buildVector(termTweetCount, firstTweets.size());
		}
		int[] membership = KMeans.cluster(firstTweets, Configure.SUMBLR_NUMBER_INITIAL_CLUSTERS);
		for (int i = 0; i < membership.length; i++) {
			tweet = firstTweets.get(i);
			int c = membership[i];
			if (clusters.containsKey(c)) {
				clusters.get(c).addTweet(tweet, refTime);
			} else {
				TCV cluster = new TCV(tweet, c, refTime);
				clusters.put(c, cluster);
			}
		}
		nClusters = Configure.SUMBLR_NUMBER_INITIAL_CLUSTERS;
	}

	private TCV chooseCluster(Tweet tweet) {
		TCV cCluster = null;
		double similarity = Double.NEGATIVE_INFINITY;
		for (TCV cluster : clusters.values()) {
			double s = cluster.getSimilarity(tweet);
			if (s > similarity) {
				similarity = s;
				cCluster = cluster;
			}
		}
		if (cCluster == null) {
			System.out.println("*****************************");
			System.out.printf("tweet = %s\n", tweet.getText());
			for (String term : tweet.getVector().keySet()) {
				System.out.printf("\t\tterm = %s value = %f\n", tweet.getVector().get(term));
			}
			for (TCV cluster : clusters.values()) {
				double s = cluster.getSimilarity(tweet);
				System.out.printf("cluster = %d sumSQRWeightedSum = %f sim = %f\n", cluster.getClusterId(),
						cluster.getSumSQRWeightedSum(), s);
			}
		}
		if (similarity >= Configure.SUMBLR_BETA * cCluster.getAvgSimilarity()) {
			/*
			 * System.out.printf(
			 * "sim = %f \t beta*avgSim = %f avgSim = %f nTweets = %d\n",
			 * similarity, Configure.SUMBLR_BETA * cCluster.getAvgSimilarity(),
			 * cCluster.getAvgSimilarity(), cCluster.getNTweets());
			 */
			return cCluster;

		} else {
			return null;
		}
	}

	public void process() {
		System.out.println("tracking");
		Tweet tweet = null;
		while ((tweet = stream.getTweet()) != null) {
			if (tweet.getTerms(preprocessingUtils).size() < 1) {
				continue;
			}			
			nTweets++;			
			List<String> terms = tweet.getTerms(preprocessingUtils);
			for (String term : terms) {
				if (termTweetCount.containsKey(term)) {
					termTweetCount.put(term, 1 + termTweetCount.get(term));
				} else {
					termTweetCount.put(term, 1);
				}
			}
			currentTimeStep = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);
			tweet.buildVector(termTweetCount, nTweets);
			TCV cluster = chooseCluster(tweet);
			if (cluster != null) {
				cluster.addTweet(tweet, refTime);
			} else {
				cluster = new TCV(tweet, nClusters, refTime);
				clusters.put(nClusters, cluster);
				nClusters++;
			}
			if ((Configure.updatingType == UpdatingType.TWEET_COUNT && nTweets % Configure.TWEET_WINDOW == 0)
					|| (Configure.updatingType == UpdatingType.PERIOD && tweet.getPublishedTime() >= nextUpdate)) {
				genSummary();
				nextUpdate += Configure.TIME_STEP_WIDTH;
			}
		}
	}

	private void genSummary() {
		System.out.println("generating summary");
		deleteOutdateClusters();
		mergeClusters();
		/*
		 * for (TCV c : clusters.values()) { System.out.printf(
		 * "cluster_%d avgSim = %f nTweets = %d\n", c.getClusterId(),
		 * c.getAvgSimilarity(), c.getNTweets()); }
		 */

		List<Tweet> tweets = new ArrayList<Tweet>();
		List<Integer> tweetCluster = new ArrayList<Integer>();
		int maxSize = -1;
		for (TCV cluster : clusters.values()) {
			for (Tweet tweet : cluster.getTweets()) {
				tweets.add(tweet);
				tweetCluster.add(cluster.getClusterId());
			}
			if (cluster.getNTweets() > maxSize) {
				maxSize = cluster.getNTweets();
			}
		}
		System.out.printf(" -------------- #tweets = %d\n", tweets.size());

		KeyValuePair[] rankedList = lexRanker.rank(tweets, Configure.LEXRANK_MIN_EDGE_SIMILARITY, true);

		List<Integer> selectedIndexes = new ArrayList<Integer>();
		HashSet<Integer> candidates = new HashSet<Integer>();
		double[] scores = new double[tweets.size()];
		double[] avgSim = new double[tweets.size()];
		for (int r = 0; r < rankedList.length; r++) {
			candidates.add(r);
			int index = rankedList[r].getIntKey();
			double score = rankedList[r].getDoubleValue();
			int c = tweetCluster.get(index);
			scores[index] = Configure.SUMBLR_LAMBDA * ((double) clusters.get(c).getNTweets() / maxSize) * score;
			avgSim[index] = 0;
		}

		int nSelected = 0;
		while (nSelected < 20) {
			int maxIndex = -1;
			double maxScore = Double.NEGATIVE_INFINITY;
			int maxRank = -1;
			for (int r : candidates) {
				int index = rankedList[r].getIntKey();
				double score = scores[index] - (1 - Configure.SUMBLR_LAMBDA) * avgSim[index];
				if (score > maxScore) {
					maxIndex = index;
					maxScore = score;
					maxRank = r;
				}
			}
			selectedIndexes.add(maxIndex);
			candidates.remove(maxRank);
			Tweet addedTweet = tweets.get(maxIndex);
			for (int r : candidates) {
				int index = rankedList[r].getIntKey();
				double sim = tweets.get(index).getSimilarity(addedTweet);
				avgSim[index] = (avgSim[index] * nSelected + sim) / (nSelected + 1);
			}
			nSelected++;
		}

		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(String.format("%s/%d_sumblr.txt", outputPath, currentTimeStep)));
			for (int index : selectedIndexes) {
				Tweet tweet = tweets.get(index);
				bw.write(String.format("%s\n", tweet.getText().replace("\n", " ")));
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// System.out.printf("******************time = %d***********\n",
		// currentTimeStep);
		// for (Tweet tweet : selectedTwets) {
		// System.out.printf("[selected tweet] %s\n", tweet.getText());
		// }
	}

	/***
	 * remove outdated clusters
	 */
	private void deleteOutdateClusters() {
		System.out.println("deleting outdate clusters");
		long outdatedThreshold = currentTimeStep - Configure.FORGOTTEN_WINDOW_DISTANCE;
		List<Integer> removeIDs = new ArrayList<Integer>();
		for (TCV cluster : clusters.values()) {
			// Try to forget old clusters
			double freshness = cluster.getFreshness(quantile);
			if (freshness < outdatedThreshold)
				removeIDs.add(cluster.getClusterId());
		}
		for (Integer id : removeIDs)
			clusters.remove(id);
		System.out.printf("%d clusters removed, %d clusters left\n", removeIDs.size(), clusters.size());
	}

	/****
	 * merge cluster
	 */
	private void mergeClusters() {
		System.out.println("merging clusters");
		if (clusters.size() < Configure.SUMBLR_MERGE_TRIGGER) {
			return;
		}
		HashMap<Integer, Integer> merge = new HashMap<Integer, Integer>();
		HashMap<Integer, HashSet<Integer>> subClusters = new HashMap<Integer, HashSet<Integer>>();
		List<Integer> clusterIndexes = new ArrayList<Integer>(clusters.keySet());
		PriorityQueue<ClusterPair> queue = new PriorityQueue<Sumblr.ClusterPair>();
		for (int i = 0; i < clusterIndexes.size(); i++) {
			TCV cluster1 = clusters.get(clusterIndexes.get(i));
			for (int j = i + 1; j < clusterIndexes.size(); j++) {
				TCV cluster2 = clusters.get(clusterIndexes.get(j));
				double similarity = cluster1.getSimilarity(cluster2);
				// System.out.printf("cluster_1 = %d cluster_2 = %d sim = %f\n",
				// i, j, similarity);
				ClusterPair pair = new ClusterPair(clusterIndexes.get(i), clusterIndexes.get(j), similarity);
				queue.add(pair);
			}
		}
		int nRemovedClusters = 0;
		while (queue.size() > 0) {
			if (nRemovedClusters >= clusters.size() * (1 - Configure.SUMBLR_MERGE_THRESHOLD)) {
				return;
			}

			ClusterPair pair = queue.remove();
			int cId1 = pair.getClusterId1();
			int cId2 = pair.getClusterId2();

			boolean flag1 = merge.containsKey(cId1);
			boolean flag2 = merge.containsKey(cId2);

			// System.out.printf("cluster_1 = %d \t cluster_2 = %d\n", cId1,
			// cId2);

			if (!flag1 && !flag2) {
				clusters.get(cId1).merge(clusters.get(cId2));
				clusters.remove(cId2);
				// System.out.printf("\t\tfalse - false: removed %d\n", cId2);
				nRemovedClusters++;
				merge.put(cId1, cId1);
				merge.put(cId2, cId1);
				HashSet<Integer> subs = new HashSet<Integer>();
				subs.add(cId1);
				subs.add(cId2);
				subClusters.put(cId1, subs);
			} else if (flag1 && !flag2) {
				clusters.get(merge.get(cId1)).merge(clusters.get(cId2));
				clusters.remove(cId2);
				// System.out.printf("\t\ttrue - false: removed %d\n", cId2);
				nRemovedClusters++;
				merge.put(cId2, merge.get(cId1));
				subClusters.get(merge.get(cId1)).add(cId2);
			} else if (!flag1 && flag2) {
				clusters.get(merge.get(cId2)).merge(clusters.get(cId1));
				clusters.remove(cId1);
				// System.out.printf("\t\tfalse - true: removed %d\n", cId1);
				nRemovedClusters++;
				merge.put(cId1, merge.get(cId2));
				subClusters.get(merge.get(cId2)).add(cId1);
			} else {
				// System.out.printf("\t\ttrue-true: merge1 = %d merge2 = %d\n",
				// merge.get(cId1), merge.get(cId2));
				if (merge.get(cId1).intValue() == merge.get(cId2).intValue()) {
					// System.out.println("\t\t\tcontinue");
					continue;
				}
				clusters.get(merge.get(cId1)).merge(clusters.get(merge.get(cId2)));
				clusters.remove(merge.get(cId2));
				// System.out.printf("\t\t\tremoved %d\n", merge.get(cId2));
				nRemovedClusters++;
				for (int c : subClusters.get(merge.get(cId2))) {
					merge.put(c, merge.get(cId1));
					subClusters.get(merge.get(cId1)).add(c);
				}

			}
		}
		System.out.printf("\t\t%d clusters left\n", clusters.size());
	}
}
