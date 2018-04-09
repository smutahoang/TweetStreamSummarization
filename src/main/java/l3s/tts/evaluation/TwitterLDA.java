package l3s.tts.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import l3s.tts.configure.Configure;
import l3s.tts.configure.Configure.UpdatingType;
import l3s.tts.utils.RankingUtils;
import l3s.tts.utils.TimeUtils;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetPreprocessingUtils;
import l3s.tts.utils.TweetStream;

public class TwitterLDA {
	private TweetStream stream;
	private String outputPath;
	private long refTime;
	private long nextUpdate;
	private int currentTimeStep;
	private int nTweets;
	private TweetPreprocessingUtils preprocessingUtils;
	private LinkedList<Tweet> recentTweets;
	private LinkedList<Tweet> allTweets;
	private HashMap<String, Integer> tweetId2Index;

	// data-preporocessing
	private int Term_Min_NTweets = 5;
	private int Tweet_Min_NTerms = 3;

	private int nTopics;

	private int burningPeriod = 50;
	private int maxIteration = 100;
	private int samplingGap = 10;

	private Random rand;
	// hyperparameters
	private double alpha;
	private double sum_alpha;
	private double beta;
	private double sum_beta;

	// data

	private List<Window> windows;
	private String[] tweetVocabulary;
	// parameters
	private double[][] tweetTopics;

	// Gibbs sampling variables
	private int[][] n_zw;
	private int[] sum_nzw;
	private int[][] n_tz;
	private int[] sum_ntz;

	private int[][] final_n_zw;
	private int[] final_sum_nzw;
	private int[][] final_n_tz;
	private int[] final_sum_ntz;

	public TwitterLDA(TweetStream _stream, int _nTopics, String _outputPath) {
		stream = _stream;
		outputPath = _outputPath;
		nTopics = _nTopics;
		preprocessingUtils = new TweetPreprocessingUtils();
		recentTweets = new LinkedList<Tweet>();
		allTweets = new LinkedList<Tweet>();
		tweetId2Index = new HashMap<String, Integer>();
		Tweet tweet = stream.getTweet();
		nTweets = 1;
		refTime = tweet.getPublishedTime();
		nextUpdate = refTime + Configure.TIME_STEP_WIDTH;
		currentTimeStep = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);
		tweet.setTimeStep(currentTimeStep);
		recentTweets.add(tweet);
		tweetId2Index.put(tweet.getTweetId(), allTweets.size());
		allTweets.add(tweet);
	}

	private HashMap<Integer, List<Tweet>> getRawWindows() {
		HashMap<Integer, List<Tweet>> windows = new HashMap<Integer, List<Tweet>>();
		Tweet tweet = null;
		while ((tweet = stream.getTweet()) != null) {
			currentTimeStep = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);
			tweet.setTimeStep(currentTimeStep);
			recentTweets.add(tweet);
			tweetId2Index.put(tweet.getTweetId(), allTweets.size());
			allTweets.add(tweet);
			nTweets++;
			if ((Configure.updatingType == UpdatingType.TWEET_COUNT && nTweets % Configure.TWEET_WINDOW == 0)
					|| (Configure.updatingType == UpdatingType.PERIOD && tweet.getPublishedTime() >= nextUpdate)) {
				removeOldTweets();
				List<Tweet> tweets = new ArrayList<Tweet>();
				for (Tweet rTweet : recentTweets) {
					tweets.add(rTweet);
				}
				windows.put(currentTimeStep, tweets);
				nextUpdate += Configure.TIME_STEP_WIDTH;
			}
		}
		return windows;
	}

	private void removeOldTweets() {
		if (currentTimeStep <= Configure.FORGOTTEN_WINDOW_DISTANCE) {
			return;
		}
		int pastTimeStep = currentTimeStep - Configure.FORGOTTEN_WINDOW_DISTANCE;
		while (true) {
			if (recentTweets.getFirst().getTimeStep() <= pastTimeStep) {
				recentTweets.removeFirst();
			} else {
				break;
			}
		}
	}

	private void preprocess(HashMap<Integer, List<Tweet>> rawWindows) {

		System.out.printf("preprocessing: #raw_windows = %d #tweets = %d\n", rawWindows.size(), allTweets.size());
		HashMap<String, Integer> termNTweets = new HashMap<String, Integer>();
		for (Tweet tweet : allTweets) {
			List<String> terms = tweet.getTerms(preprocessingUtils);
			for (String term : terms) {
				if (termNTweets.containsKey(term)) {
					termNTweets.put(term, 1 + termNTweets.get(term));
				} else {
					termNTweets.put(term, 1);
				}
			}
		}

		HashSet<String> removedTerms = new HashSet<String>();
		HashSet<String> removedTweets = new HashSet<String>();

		while (true) {
			boolean flag = true;

			nTweets = 0;
			for (Tweet tweet : allTweets) {
				String tId = tweet.getTweetId();
				if (removedTweets.contains(tId)) {
					continue;
				}
				List<String> terms = tweet.getTerms(preprocessingUtils);
				int nTerms = 0;
				for (String term : terms) {
					if (removedTerms.contains(term)) {
						continue;
					}
					if (termNTweets.get(term) >= Term_Min_NTweets) {
						nTerms++;
					} else {
						removedTerms.add(term);
						flag = false;
					}
				}
				if (nTerms < Tweet_Min_NTerms) {
					for (String term : terms) {
						termNTweets.put(term, termNTweets.get(term) - 1);
					}
					removedTweets.add(tId);
					flag = false;
				} else {
					nTweets++;
				}
			}
			if (flag)
				break;
		}

		System.out.printf("\t\t #removedTweets = %d\n", removedTweets.size());
		System.out.printf("\t\t #removedTerms = %d\n", removedTerms.size());

		windows = new ArrayList<Window>();
		HashMap<String, Integer> term2Index = new HashMap<String, Integer>();
		for (Map.Entry<Integer, List<Tweet>> rawWindow : rawWindows.entrySet()) {
			int w = rawWindow.getKey();
			int nTweets = 0;
			for (Tweet tweet : rawWindow.getValue()) {
				if (removedTweets.contains(tweet.getTweetId())) {
					continue;
				}
				nTweets++;
			}
			Window window = new Window();
			window.windowId = w;
			window.tweets = new TweetBoW[nTweets];
			nTweets = 0;
			for (Tweet tweet : rawWindow.getValue()) {
				if (removedTweets.contains(tweet.getTweetId())) {
					continue;
				}
				int nWwords = 0;
				for (String term : tweet.getTerms(preprocessingUtils)) {
					if (removedTerms.contains(term)) {
						continue;
					}
					if (!term2Index.containsKey(term)) {
						term2Index.put(term, term2Index.size());
					}
					nWwords++;
				}
				window.tweets[nTweets] = new TweetBoW();
				window.tweets[nTweets].tweetID = tweet.getTweetId();
				window.tweets[nTweets].terms = new int[nWwords];
				nWwords = 0;
				for (String term : tweet.getTerms(preprocessingUtils)) {
					if (removedTerms.contains(term)) {
						continue;
					}
					window.tweets[nTweets].terms[nWwords] = term2Index.get(term);
					nWwords++;
				}
				nTweets++;
			}
			System.out.printf("\t\t\t window-%d: #tweets = %d\n", windows.size(), window.tweets.length);
			windows.add(window);
		}
		tweetVocabulary = new String[term2Index.size()];
		for (Map.Entry<String, Integer> pair : term2Index.entrySet()) {
			tweetVocabulary[pair.getValue()] = pair.getKey();
		}
		System.out.printf("\t\t #windows = %d\n", windows.size());
	}

	private void declareFinalCounts() {
		final_n_zw = new int[nTopics][windows.size()];
		final_sum_nzw = new int[windows.size()];
		for (int e = 0; e < windows.size(); e++) {
			for (int z = 0; z < nTopics; z++)
				final_n_zw[z][e] = 0;
			final_sum_nzw[e] = 0;
		}
		final_n_tz = new int[tweetVocabulary.length][nTopics];
		final_sum_ntz = new int[nTopics];
		for (int z = 0; z < nTopics; z++) {
			for (int w = 0; w < tweetVocabulary.length; w++)
				final_n_tz[w][z] = 0;
			final_sum_ntz[z] = 0;
		}
	}

	private void initilize() {
		rand = new Random();

		// init topic for each tweet
		for (int w = 0; w < windows.size(); w++) {
			// tweet
			for (int t = 0; t < windows.get(w).tweets.length; t++) {
				windows.get(w).tweets[t].topic = rand.nextInt(nTopics);
			}
		}
		// declare and initiate counting tables
		n_zw = new int[nTopics][windows.size()];
		sum_nzw = new int[windows.size()];
		for (int w = 0; w < windows.size(); w++) {
			for (int z = 0; z < nTopics; z++)
				n_zw[z][w] = 0;
			sum_nzw[w] = 0;
		}
		n_tz = new int[tweetVocabulary.length][nTopics];
		sum_ntz = new int[nTopics];
		for (int z = 0; z < nTopics; z++) {
			for (int t = 0; t < tweetVocabulary.length; t++)
				n_tz[t][z] = 0;
			sum_ntz[z] = 0;
		}

		// update counting tables
		for (int w = 0; w < windows.size(); w++) {
			// tweet
			for (int t = 0; t < windows.get(w).tweets.length; t++) {
				int z = windows.get(w).tweets[t].topic;
				// window-topic
				n_zw[z][w]++;
				sum_nzw[w]++;
				for (int i = 0; i < windows.get(w).tweets[t].terms.length; i++) {
					int word = windows.get(w).tweets[t].terms[i];
					// word - topic
					n_tz[word][z]++;
					sum_ntz[z]++;
				}
			}
		}
	}

	// sampling
	private void setPriors() {
		// window topic prior
		alpha = 50.0 / nTopics;
		sum_alpha = 50;

		// topic tweet word prior
		beta = 0.01;
		sum_beta = 0.01 * tweetVocabulary.length;
	}

	private void sampleTweetTopic(int w, int t) {
		// sample the topic for tweet number t of window number w
		// get current topic
		int currz = windows.get(w).tweets[t].topic;
		n_zw[currz][w]--;
		sum_nzw[w]--;
		for (int i = 0; i < windows.get(w).tweets[t].terms.length; i++) {
			int word = windows.get(w).tweets[t].terms[i];
			n_tz[word][currz]--;
			sum_ntz[currz]--;
		}
		double sump = 0;
		double[] p = new double[nTopics];
		for (int z = 0; z < nTopics; z++) {
			p[z] = (n_zw[z][w] + alpha) / (sum_nzw[w] + sum_alpha);
			for (int i = 0; i < windows.get(w).tweets[t].terms.length; i++) {
				int word = windows.get(w).tweets[t].terms[i];
				p[z] = p[z] * (n_tz[word][z] + beta) / (sum_ntz[z] + sum_beta);
			}
			// cumulative
			p[z] = sump + p[z];
			sump = p[z];
		}
		sump = rand.nextDouble() * sump;
		for (int z = 0; z < nTopics; z++) {
			if (sump > p[z])
				continue;
			// the topic
			windows.get(w).tweets[t].topic = z;
			// window - topic
			n_zw[z][w]++;
			sum_nzw[w]++;
			// topic - word
			for (int i = 0; i < windows.get(w).tweets[t].terms.length; i++) {
				int word = windows.get(w).tweets[t].terms[i];
				n_tz[word][z]++;
				sum_ntz[z]++;
			}
			return;
		}
		System.out.println("bug in sampleTweetTopic");
		for (int z = 0; z < nTopics; z++) {
			System.out.print(p[z] + " ");
		}
		System.exit(-1);
	}

	private void updateFinalCounts() {
		for (int w = 0; w < windows.size(); w++) {
			for (int z = 0; z < nTopics; z++)
				final_n_zw[z][w] += n_zw[z][w];
			final_sum_nzw[w] += sum_nzw[w];
		}

		for (int z = 0; z < nTopics; z++) {
			for (int word = 0; word < tweetVocabulary.length; word++)
				final_n_tz[word][z] += n_tz[word][z];
			final_sum_ntz[z] += sum_ntz[z];
		}
	}

	private void gibbsSampling() {
		System.out.println("Runing Gibbs sampling");
		System.out.print("Setting prios ...");
		setPriors();
		System.out.println(" Done!");
		declareFinalCounts();
		System.out.print("Initializing ... ");
		initilize();
		System.out.println("... Done!");
		for (int iter = 0; iter < burningPeriod + maxIteration; iter++) {
			System.out.print("iteration " + iter);
			// topic
			for (int w = 0; w < windows.size(); w++) {
				for (int t = 0; t < windows.get(w).tweets.length; t++) {
					sampleTweetTopic(w, t);
				}
			}

			System.out.println(" done!");
			if (samplingGap <= 0)
				continue;
			if (iter < burningPeriod)
				continue;
			if ((iter - burningPeriod) % samplingGap == 0) {
				updateFinalCounts();
			}
		}
		if (samplingGap <= 0)
			updateFinalCounts();
	}

	// inference
	private void inferingModelParameters() {
		// windows
		for (int w = 0; w < windows.size(); w++) {
			// topic distribution
			windows.get(w).topicDistribution = new double[nTopics];
			for (int z = 0; z < nTopics; z++) {
				windows.get(w).topicDistribution[z] = (final_n_zw[z][w] + alpha) / (final_sum_nzw[w] + sum_alpha);
			}
		}
		// topics
		tweetTopics = new double[nTopics][tweetVocabulary.length];
		for (int z = 0; z < nTopics; z++) {
			for (int w = 0; w < tweetVocabulary.length; w++)
				tweetTopics[z][w] = (final_n_tz[w][z] + beta) / (final_sum_ntz[z] + sum_beta);
		}
	}

	/***
	 * compute likelihood of tweet t in window w given topic z
	 * 
	 * @param w
	 * @param t
	 * @param z
	 * @return
	 */
	private double getTweetLikelihood(int w, int t, int z) {
		double logLikelihood = 0;
		for (int i = 0; i < windows.get(w).tweets[t].terms.length; i++) {
			int word = windows.get(w).tweets[t].terms[i];
			logLikelihood = logLikelihood + Math.log10(tweetTopics[z][word]);
		}
		return logLikelihood;
	}

	/***
	 * @param w
	 * @param t
	 * @param z
	 * @return
	 */
	private double getTweetSumProbUniqueWord(int w, int t, int z) {
		HashSet<Integer> uniqueWords = new HashSet<Integer>();
		for (int i = 0; i < windows.get(w).tweets[t].terms.length; i++) {
			int word = windows.get(w).tweets[t].terms[i];
			uniqueWords.add(word);
		}
		double sum = 0;
		for (int word : uniqueWords) {
			// probability that word i is generated by topic z
			sum += tweetTopics[z][word];
		}
		return sum;
	}

	private void inferTweetTopic() {
		for (int w = 0; w < windows.size(); w++) {
			for (int t = 0; t < windows.get(w).tweets.length; t++) {

				double[] posteriors = new double[nTopics];
				for (int z = 0; z < nTopics; z++) {
					posteriors[z] = getTweetLikelihood(w, t, z) + Math.log10(windows.get(w).topicDistribution[z]);
				}

				windows.get(w).tweets[t].inferedTopic = -1;
				windows.get(w).tweets[t].inferedLikelihood = Double.NEGATIVE_INFINITY;
				double sum = 0;
				for (int z = 0; z < nTopics; z++) {
					if (posteriors[z] > windows.get(w).tweets[t].inferedLikelihood) {
						windows.get(w).tweets[t].inferedLikelihood = posteriors[z];
						windows.get(w).tweets[t].inferedTopic = z;
					}
					posteriors[z] = Math.pow(10, posteriors[z]);
					sum += posteriors[z];
				}
				windows.get(w).tweets[t].inferedPosteriorProb = posteriors[windows.get(w).tweets[t].inferedTopic] / sum;
			}
		}
	}

	private void outputTopicWordDistributions() {
		try {
			String fileName = outputPath + "/topicWordDistributions.csv";
			File file = new File(fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
			for (int z = 0; z < nTopics; z++) {
				bw.write("" + z);
				for (int w = 0; w < tweetVocabulary.length; w++)
					bw.write("," + tweetTopics[z][w]);
				bw.write("\n");
			}
			bw.close();
		} catch (Exception e) {
			System.out.println("Error in writing out tweet topics to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void outputTweetVocabulary() {
		try {
			String fileName = outputPath + "/words.csv";
			File file = new File(fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
			for (int i = 0; i < tweetVocabulary.length; i++) {
				bw.write(String.format("%d,%s\n", i, tweetVocabulary[i]));
			}
			bw.close();
		} catch (Exception e) {
			System.out.println("Error in writing out tweet vocabulary to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void outputTweetTopicTopWords(int k) {
		try {
			String fileName = outputPath + "/tweetTopicTopWords.csv";
			File file = new File(fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
			List<Integer> topWords = null;
			for (int z = 0; z < nTopics; z++) {
				bw.write(String.format("***[[TOPIC-%d]]***\n", z));
				topWords = RankingUtils.getIndexTopElements(k, tweetTopics[z]);
				for (int j = topWords.size() - 1; j >= 0; j--) {
					int w = topWords.get(j);
					bw.write(String.format("%s,%f\n", tweetVocabulary[w], tweetTopics[z][w]));
				}
			}

			bw.close();
		} catch (Exception e) {
			System.out.println("Error in writing out tweet topic top words to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void outputTweetTopicTopTweetsSumProbUniqueWords(int k) {
		int[] topicTweetCount = new int[nTopics];

		for (int z = 0; z < nTopics; z++)
			topicTweetCount[z] = 0;
		for (int w = 0; w < windows.size(); w++) {
			for (int t = 0; t < windows.get(w).tweets.length; t++) {
				topicTweetCount[windows.get(w).tweets[t].inferedTopic]++;
			}
		}

		String[][] topicTweetIDs = new String[nTopics][];
		double[][] sumProbUniqueWords = new double[nTopics][];
		for (int z = 0; z < nTopics; z++) {
			topicTweetIDs[z] = new String[topicTweetCount[z]];
			sumProbUniqueWords[z] = new double[topicTweetCount[z]];
			topicTweetCount[z] = 0;
		}

		for (int w = 0; w < windows.size(); w++) {
			for (int t = 0; t < windows.get(w).tweets.length; t++) {
				int z = windows.get(w).tweets[t].inferedTopic;
				topicTweetIDs[z][topicTweetCount[z]] = windows.get(w).tweets[t].tweetID;
				sumProbUniqueWords[z][topicTweetCount[z]] = getTweetSumProbUniqueWord(w, t, z);
				topicTweetCount[z]++;
			}
		}

		try {
			String fileName = outputPath + "/topicTopTweetsBySumProbUniqueWords.csv";
			File file = new File(fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
			List<Integer> topTweets = null;
			for (int z = 0; z < nTopics; z++) {
				bw.write(String.format("***[[TOPIC-%d]]***\n", z));
				topTweets = RankingUtils.getIndexTopElements(k, sumProbUniqueWords[z]);
				for (int j = topTweets.size() - 1; j >= 0; j--) {
					int t = topTweets.get(j);
					bw.write(String.format("%s,%f,%s\n", topicTweetIDs[z][t], sumProbUniqueWords[z][t],
							allTweets.get(tweetId2Index.get(topicTweetIDs[z][t])).getText().replace("\n", " ")));
				}
			}
			bw.close();
		} catch (Exception exception) {
			System.out.println("Error in writing out tweet topic top tweets to file!");
			exception.printStackTrace();
			System.exit(0);
		}
	}

	private void outputWindowTopicDistribution() {
		try {
			String fileName = outputPath + "/windowTopics.csv";
			File file = new File(fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
			for (int e = 0; e < windows.size(); e++) {
				bw.write(String.format("%d", windows.get(e).windowId));
				for (int z = 0; z < nTopics; z++)
					bw.write(String.format(",%f", windows.get(e).topicDistribution[z]));
				bw.write("\n");
			}
			bw.close();
		} catch (Exception e) {
			System.out.println("Error in writing out window topic distributions to file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void learnModel() {
		gibbsSampling();
		inferingModelParameters();
		inferTweetTopic();
		outputTweetVocabulary();
		outputTopicWordDistributions();
		outputTweetTopicTopWords(20);
		outputTweetTopicTopTweetsSumProbUniqueWords(200);
		outputWindowTopicDistribution();
	}

	private void selectRepresentativeTweets(int w, double sumProb) {
		try {
			int[] topicTweetCount = new int[nTopics];
			for (int i = 0; i < nTopics; i++) {
				topicTweetCount[i] = 0;
			}
			for (int t = 0; t < windows.get(w).tweets.length; t++) {
				if (windows.get(w).tweets[t].inferedPosteriorProb >= 0.8) {
					topicTweetCount[windows.get(w).tweets[t].inferedTopic]++;
				}
			}
			int[][] topicTweets = new int[nTopics][];
			for (int i = 0; i < nTopics; i++) {
				topicTweets[i] = new int[topicTweetCount[i]];
				topicTweetCount[i] = 0;
			}
			for (int t = 0; t < windows.get(w).tweets.length; t++) {
				if (windows.get(w).tweets[t].inferedPosteriorProb >= 0.8) {
					int z = windows.get(w).tweets[t].inferedTopic;
					topicTweets[z][topicTweetCount[z]] = t;
					topicTweetCount[z]++;
				}
			}

			List<Integer> topTopics = RankingUtils.getIndexTopElements(windows.get(w).topicDistribution, sumProb);
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(String.format("%s/representativeTweets_%d.txt", outputPath, w)));
			for (int z : topTopics) {
				double[] sumProbUniqueWords = new double[topicTweetCount[z]];
				for (int i = 0; i < topicTweetCount[z]; i++) {
					int t = topicTweets[z][i];
					sumProbUniqueWords[i] = getTweetSumProbUniqueWord(w, t, z);
				}
				List<Integer> topIndexes = RankingUtils.getIndexTopElements(1, sumProbUniqueWords);
				for (int j : topIndexes) {
					String tweetId = windows.get(w).tweets[j].tweetID;
					// double p = windows.get(w).tweets[j].inferedPosteriorProb;
					/*
					 * bw.write(String.format("%d\t%d\t%f\t%f\t%s\n", w, z,
					 * windows.get(w).topicDistribution[z], p,
					 * allTweets.get(tweetId2Index.get(tweetId)).getText().
					 * replace("\n", " ")));
					 */
					bw.write(String.format("%s\n",
							allTweets.get(tweetId2Index.get(tweetId)).getText().replace("\n", " ")));
				}
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void genSummary() {
		HashMap<Integer, List<Tweet>> rawWindows = getRawWindows();
		preprocess(rawWindows);
		learnModel();
		for (int w = 0; w < windows.size(); w++) {
			selectRepresentativeTweets(w, 0.9);
		}
	}
}
