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
import l3s.tts.utils.IOUtils;
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

	private int burningPeriod = 100;
	private int maxIteration = 500;
	private int samplingGap = 20;

	private Random rand;
	// hyperparameters
	private double alpha;
	private double sum_alpha;
	private double beta;
	private double sum_beta;

	// data

	private List<Step> steps;
	private String[] tweetVocabulary;
	// parameters
	private double[][] tweetTopics;

	// Gibbs sampling variables
	private int[][] n_zs;
	private int[] sum_nzs;
	private int[][] n_tz;
	private int[] sum_ntz;

	private int[][] final_n_zs;
	private int[] final_sum_nzs;
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

		// first tweet
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

	private HashMap<Integer, List<Tweet>> getRawSteps() {
		try {
			HashMap<Integer, List<Tweet>> rawSteps = new HashMap<Integer, List<Tweet>>();
			Tweet tweet = null;
			BufferedWriter bw = new BufferedWriter(new FileWriter("log_groundtruth.csv"));
			BufferedWriter bw_chunk = new BufferedWriter(new FileWriter("chunk_groundtruth.csv"));
			while ((tweet = stream.getTweet()) != null) {
				currentTimeStep = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime,
						Configure.TIME_STEP_WIDTH);
				bw.write(String.format("time=%d\t%s\n", currentTimeStep, tweet.getTweetId()));
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
					rawSteps.put(currentTimeStep, tweets);
					System.out.printf("currentTimeStep = %d\n", currentTimeStep);
					nextUpdate += Configure.TIME_STEP_WIDTH;

					bw_chunk.write(String.format("time=%d\t%s\t%s\t%d\n", currentTimeStep,
							recentTweets.peekFirst().getTweetId(), recentTweets.peekLast().getTweetId(),
							recentTweets.size()));
				}

			}
			bw.close();
			bw_chunk.close();
			return rawSteps;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	private void removeOldTweets() {
		// if (currentTimeStep <= Configure.FORGOTTEN_WINDOW_DISTANCE) {
		// return;
		// }
		// int pastTimeStep = currentTimeStep -
		// Configure.FORGOTTEN_WINDOW_DISTANCE;

		if (currentTimeStep <= 1) {
			return;
		}

		int pastTimeStep = currentTimeStep - 1;
		while (true) {
			if (recentTweets.getFirst().getTimeStep() < pastTimeStep) {
				recentTweets.removeFirst();
			} else {
				break;
			}
		}
	}

	private void preprocess(HashMap<Integer, List<Tweet>> rawSteps) {

		System.out.printf("preprocessing: #raw_windows = %d #tweets = %d\n", rawSteps.size(), allTweets.size());
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

		steps = new ArrayList<Step>();
		HashMap<String, Integer> term2Index = new HashMap<String, Integer>();
		for (Map.Entry<Integer, List<Tweet>> rawStep : rawSteps.entrySet()) {
			int s = rawStep.getKey();
			int nTweets = 0;
			for (Tweet tweet : rawStep.getValue()) {
				if (removedTweets.contains(tweet.getTweetId())) {
					continue;
				}
				nTweets++;
			}
			Step step = new Step();
			step.stepIndex = s;
			System.out.printf("stepIndex = %d\n", s);
			step.tweets = new TweetBoW[nTweets];
			nTweets = 0;
			for (Tweet tweet : rawStep.getValue()) {
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
				step.tweets[nTweets] = new TweetBoW();
				step.tweets[nTweets].tweetID = tweet.getTweetId();
				step.tweets[nTweets].terms = new int[nWwords];
				nWwords = 0;
				HashSet<Integer> uniqueTerms = new HashSet<Integer>();
				for (String term : tweet.getTerms(preprocessingUtils)) {
					if (removedTerms.contains(term)) {
						continue;
					}
					step.tweets[nTweets].terms[nWwords] = term2Index.get(term);
					uniqueTerms.add(step.tweets[nTweets].terms[nWwords]);
					nWwords++;
				}
				step.tweets[nTweets].nUniqueTerms = uniqueTerms.size();
				nTweets++;
			}
			System.out.printf("\t\t\t step-%d: index = %d  #tweets = %d\n", step.stepIndex, steps.size(),
					step.tweets.length);
			steps.add(step);
		}
		// System.exit(-1);
		tweetVocabulary = new String[term2Index.size()];
		for (Map.Entry<String, Integer> pair : term2Index.entrySet()) {
			tweetVocabulary[pair.getValue()] = pair.getKey();
		}
		System.out.printf("\t\t #steps = %d\n", steps.size());
	}

	private void declareFinalCounts() {
		final_n_zs = new int[nTopics][steps.size()];
		final_sum_nzs = new int[steps.size()];
		for (int e = 0; e < steps.size(); e++) {
			for (int z = 0; z < nTopics; z++)
				final_n_zs[z][e] = 0;
			final_sum_nzs[e] = 0;
		}
		final_n_tz = new int[tweetVocabulary.length][nTopics];
		final_sum_ntz = new int[nTopics];
		for (int z = 0; z < nTopics; z++) {
			for (int t = 0; t < tweetVocabulary.length; t++)
				final_n_tz[t][z] = 0;
			final_sum_ntz[z] = 0;
		}
	}

	private void initilize() {
		rand = new Random();

		// init topic for each tweet
		for (int s = 0; s < steps.size(); s++) {
			// tweet
			for (int t = 0; t < steps.get(s).tweets.length; t++) {
				steps.get(s).tweets[t].topic = rand.nextInt(nTopics);
			}
		}
		// declare and initiate counting tables
		n_zs = new int[nTopics][steps.size()];
		sum_nzs = new int[steps.size()];
		for (int s = 0; s < steps.size(); s++) {
			for (int z = 0; z < nTopics; z++)
				n_zs[z][s] = 0;
			sum_nzs[s] = 0;
		}
		n_tz = new int[tweetVocabulary.length][nTopics];
		sum_ntz = new int[nTopics];
		for (int z = 0; z < nTopics; z++) {
			for (int t = 0; t < tweetVocabulary.length; t++)
				n_tz[t][z] = 0;
			sum_ntz[z] = 0;
		}

		// update counting tables
		for (int s = 0; s < steps.size(); s++) {
			// tweet
			for (int t = 0; t < steps.get(s).tweets.length; t++) {
				int z = steps.get(s).tweets[t].topic;
				// window-topic
				n_zs[z][s]++;
				sum_nzs[s]++;
				for (int i = 0; i < steps.get(s).tweets[t].terms.length; i++) {
					int term = steps.get(s).tweets[t].terms[i];
					// word - topic
					n_tz[term][z]++;
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

	private void sampleTweetTopic(int s, int t) {
		// sample the topic for tweet number t of window number w
		// get current topic
		int currz = steps.get(s).tweets[t].topic;
		n_zs[currz][s]--;
		sum_nzs[s]--;
		for (int i = 0; i < steps.get(s).tweets[t].terms.length; i++) {
			int term = steps.get(s).tweets[t].terms[i];
			n_tz[term][currz]--;
			sum_ntz[currz]--;
		}
		double sump = 0;
		double[] p = new double[nTopics];
		for (int z = 0; z < nTopics; z++) {
			p[z] = (n_zs[z][s] + alpha) / (sum_nzs[s] + sum_alpha);
			for (int i = 0; i < steps.get(s).tweets[t].terms.length; i++) {
				int term = steps.get(s).tweets[t].terms[i];
				p[z] = p[z] * (n_tz[term][z] + beta) / (sum_ntz[z] + sum_beta);
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
			steps.get(s).tweets[t].topic = z;
			// window - topic
			n_zs[z][s]++;
			sum_nzs[s]++;
			// topic - word
			for (int i = 0; i < steps.get(s).tweets[t].terms.length; i++) {
				int term = steps.get(s).tweets[t].terms[i];
				n_tz[term][z]++;
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
		for (int s = 0; s < steps.size(); s++) {
			for (int z = 0; z < nTopics; z++)
				final_n_zs[z][s] += n_zs[z][s];
			final_sum_nzs[s] += sum_nzs[s];
		}

		for (int z = 0; z < nTopics; z++) {
			for (int term = 0; term < tweetVocabulary.length; term++)
				final_n_tz[term][z] += n_tz[term][z];
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
			for (int s = 0; s < steps.size(); s++) {
				for (int t = 0; t < steps.get(s).tweets.length; t++) {
					sampleTweetTopic(s, t);
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
		// steps
		for (int s = 0; s < steps.size(); s++) {
			// topic distribution
			steps.get(s).topicDistribution = new double[nTopics];
			for (int z = 0; z < nTopics; z++) {
				steps.get(s).topicDistribution[z] = (final_n_zs[z][s] + alpha) / (final_sum_nzs[s] + sum_alpha);
			}
		}
		// topics
		tweetTopics = new double[nTopics][tweetVocabulary.length];
		for (int z = 0; z < nTopics; z++) {
			for (int term = 0; term < tweetVocabulary.length; term++)
				tweetTopics[z][term] = (final_n_tz[term][z] + beta) / (final_sum_ntz[z] + sum_beta);
		}
	}

	/***
	 * compute likelihood of tweet t in step s given topic z
	 * 
	 * @param s
	 * @param t
	 * @param z
	 * @return
	 */
	private double getTweetLikelihood(int s, int t, int z) {
		double logLikelihood = 0;
		Step window = steps.get(s);
		for (int i = 0; i < steps.get(s).tweets[t].terms.length; i++) {
			int term = window.tweets[t].terms[i];
			logLikelihood = logLikelihood + Math.log10(tweetTopics[z][term]);
		}
		return logLikelihood;
	}

	/***
	 * @param s
	 * @param t
	 * @param z
	 * @return
	 */
	private double getTweetScore(int s, int t, int z) {
		HashSet<Integer> uniqueTerms = new HashSet<Integer>();
		double sum = 0;
		for (int i = 0; i < steps.get(s).tweets[t].terms.length; i++) {
			int term = steps.get(s).tweets[t].terms[i];
			if (uniqueTerms.contains(term)) {
				continue;
			}
			sum += tweetTopics[z][term];
			uniqueTerms.add(term);
		}
		if (uniqueTerms.size() < 7) {
			return 0;
		} else {
			return sum / uniqueTerms.size();
		}
	}

	private void inferTweetTopic() {
		for (int s = 0; s < steps.size(); s++) {
			Step step = steps.get(s);
			for (int t = 0; t < step.tweets.length; t++) {
				double[] posteriors = new double[nTopics];
				for (int z = 0; z < nTopics; z++) {
					posteriors[z] = getTweetLikelihood(s, t, z) + Math.log10(step.topicDistribution[z]);
				}

				step.tweets[t].inferedTopic = -1;
				step.tweets[t].inferedLikelihood = Double.NEGATIVE_INFINITY;
				double sum = 0;
				for (int z = 0; z < nTopics; z++) {
					if (posteriors[z] > step.tweets[t].inferedLikelihood) {
						step.tweets[t].inferedLikelihood = posteriors[z];
						step.tweets[t].inferedTopic = z;
					}
					posteriors[z] = Math.pow(10, posteriors[z]);
					sum += posteriors[z];
				}
				step.tweets[t].inferedPosteriorProb = posteriors[step.tweets[t].inferedTopic] / sum;
			}
		}
	}

	private void outputTopicCoherence(int k) {
		try {
			HashSet<Integer> topTerms = null;
			HashSet<Integer> subTopTerms;
			double coherence = 0;
			for (int z = 0; z < nTopics; z++) {
				topTerms = new HashSet<Integer>(RankingUtils.getIndexTopElements(k, tweetTopics[z]));
				HashMap<Integer, Integer> termTweetCounts = new HashMap<Integer, Integer>();
				HashMap<Integer, HashMap<Integer, Integer>> termPairTweetCount = new HashMap<Integer, HashMap<Integer, Integer>>();
				for (int s = 0; s < steps.size(); s++) {
					for (int t = 0; t < steps.get(s).tweets.length; t++) {
						subTopTerms = new HashSet<Integer>();
						for (int i = 0; i < steps.get(s).tweets[t].terms.length; i++) {
							int term = steps.get(s).tweets[t].terms[i];
							if (topTerms.contains(term)) {
								subTopTerms.add(term);
							}
						}
						for (int u : subTopTerms) {
							if (termTweetCounts.containsKey(u)) {
								termTweetCounts.put(u, termTweetCounts.get(u) + 1);
							} else {
								termTweetCounts.put(u, 1);
							}
							for (int v : subTopTerms) {
								if (v == u) {
									continue;
								}
								if (termPairTweetCount.containsKey(u)) {
									HashMap<Integer, Integer> coAppearances = termPairTweetCount.get(u);
									if (coAppearances.containsKey(v)) {
										coAppearances.put(v, coAppearances.get(v) + 1);
									} else {
										coAppearances.put(v, 1);
									}
								} else {
									HashMap<Integer, Integer> coAppearances = new HashMap<Integer, Integer>();
									coAppearances.put(v, 1);
									termPairTweetCount.put(u, coAppearances);
								}
							}
						}
					}
				}
				double c = 0;
				for (int u : topTerms) {
					int denominator = termTweetCounts.get(u);
					for (int v : topTerms) {
						if (v == u) {
							continue;
						}
						int nominator = 0;
						if (termPairTweetCount.containsKey(u)) {
							if (termPairTweetCount.get(u).containsKey(v)) {
								nominator = termPairTweetCount.get(u).get(v);
							}
						}
						c += Math.log((double) (nominator + 1) / denominator);
					}
				}
				coherence += c;
			}
			coherence /= (k * (k - 1) / 2);
			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/coherence.csv", outputPath), true));
			bw.write(String.format("%d,%f\n", nTopics, coherence));
			bw.close();

		} catch (Exception e) {
			System.out.println("Error in writing out tweet topic top words to file!");
			e.printStackTrace();
			System.exit(0);
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
				for (int term = 0; term < tweetVocabulary.length; term++)
					bw.write("," + tweetTopics[z][term]);
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
					int term = topWords.get(j);
					bw.write(String.format("%s,%f\n", tweetVocabulary[term], tweetTopics[z][term]));
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
		for (int s = 0; s < steps.size(); s++) {
			for (int t = 0; t < steps.get(s).tweets.length; t++) {
				topicTweetCount[steps.get(s).tweets[t].inferedTopic]++;
			}
		}

		String[][] topicTweetIDs = new String[nTopics][];
		double[][] sumProbUniqueWords = new double[nTopics][];
		for (int z = 0; z < nTopics; z++) {
			topicTweetIDs[z] = new String[topicTweetCount[z]];
			sumProbUniqueWords[z] = new double[topicTweetCount[z]];
			topicTweetCount[z] = 0;
		}

		for (int s = 0; s < steps.size(); s++) {
			for (int t = 0; t < steps.get(s).tweets.length; t++) {
				int z = steps.get(s).tweets[t].inferedTopic;
				topicTweetIDs[z][topicTweetCount[z]] = steps.get(s).tweets[t].tweetID;
				sumProbUniqueWords[z][topicTweetCount[z]] = getTweetScore(s, t, z);
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

	private void outputStepTopicDistribution() {
		try {
			String fileName = outputPath + "/stepTopics.csv";
			File file = new File(fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
			for (int s = 0; s < steps.size(); s++) {
				bw.write(String.format("%d", steps.get(s).stepIndex));
				for (int z = 0; z < nTopics; z++)
					bw.write(String.format(",%f", steps.get(s).topicDistribution[z]));
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
		try {
			gibbsSampling();
			inferingModelParameters();
			inferTweetTopic();
			outputTopicCoherence(10);
			outputPath = String.format("%s/%d", outputPath, nTopics);
			IOUtils.mkDir(outputPath);
			outputTweetVocabulary();
			outputTopicWordDistributions();
			outputTweetTopicTopWords(20);
			outputTweetTopicTopTweetsSumProbUniqueWords(200);
			outputStepTopicDistribution();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private double getJCSimilarity(Tweet tweetA, Tweet tweetB) {
		double jc = 0;
		HashSet<String> termsA = new HashSet<String>(tweetA.getTerms(preprocessingUtils));
		HashSet<String> termsB = new HashSet<String>(tweetB.getTerms(preprocessingUtils));

		int nCommons = 0;
		for (String term : termsA) {
			if (termsB.contains(term)) {
				nCommons++;
			}
		}
		jc = ((double) nCommons) / (termsA.size() + termsB.size() - nCommons);
		return jc;
	}

	private void selectRepresentativeTweets(int w, double sumProb) {
		try {
			System.out.printf("[%d]:", w);
			List<Step> window = new ArrayList<Step>();
			for (Step step : steps) {
				if (step.stepIndex <= w - Configure.FORGOTTEN_WINDOW_DISTANCE) {
					continue;
				}
				if (step.stepIndex > w) {
					continue;
				}
				window.add(step);
				System.out.printf("\t%d", step.stepIndex);
			}
			System.out.println();
			int[] topicTweetCount = new int[nTopics];
			for (int i = 0; i < nTopics; i++) {
				topicTweetCount[i] = 0;
			}
			for (Step step : window) {
				for (int t = 0; t < step.tweets.length; t++) {
					if (step.tweets[t].nUniqueTerms >= 5 && step.tweets[t].inferedPosteriorProb >= 0.8) {
						topicTweetCount[step.tweets[t].inferedTopic]++;
					}
				}

			}
			Step[][] stepTweets = new Step[nTopics][];
			int[][] topicTweets = new int[nTopics][];
			for (int z = 0; z < nTopics; z++) {
				stepTweets[z] = new Step[topicTweetCount[z]];
				topicTweets[z] = new int[topicTweetCount[z]];
				topicTweetCount[z] = 0;
			}
			int totalCount = 0;
			for (Step step : window) {
				for (int t = 0; t < step.tweets.length; t++) {
					if (step.tweets[t].nUniqueTerms >= 5 && step.tweets[t].inferedPosteriorProb >= 0.8) {
						int z = step.tweets[t].inferedTopic;
						stepTweets[z][topicTweetCount[z]] = step;
						topicTweets[z][topicTweetCount[z]] = t;
						topicTweetCount[z]++;
						totalCount++;
					}
				}
			}

			double[] windowTopicDistribution = new double[nTopics];
			for (int z = 0; z < nTopics; z++) {
				windowTopicDistribution[z] = ((double) topicTweetCount[z]) / totalCount;
			}

			List<Tweet> representativeTweets = new ArrayList<Tweet>();
			List<Integer> topTopics = RankingUtils.getIndexTopElements(windowTopicDistribution, sumProb);

			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/tweets_%d.txt", outputPath, w)));
			for (int z : topTopics) {
				double[] tweetImportance = new double[topicTweetCount[z]];
				for (int i = 0; i < topicTweetCount[z]; i++) {
					Step step = stepTweets[z][i];
					int t = topicTweets[z][i];
					tweetImportance[i] = getTweetScore(step.stepIndex - 1, t, z);
					// tweetImportance[i] = step.tweets[t].inferedLikelihood /
					// step.tweets[t].terms.length;
				}
				List<Integer> topIndexes = RankingUtils.getIndexTopElements(1, tweetImportance);
				for (int j : topIndexes) {
					Step step = stepTweets[z][j];
					int t = topicTweets[z][j];
					String tweetId = step.tweets[t].tweetID;
					representativeTweets.add(allTweets.get(tweetId2Index.get(tweetId)));
					bw.write(String.format("%d %.2f \t %f \t %s\n", z, windowTopicDistribution[z],
							step.tweets[t].inferedPosteriorProb,
							allTweets.get(tweetId2Index.get(tweetId)).getText().replace("\n", " ")));
				}
			}

			boolean[] mark = new boolean[representativeTweets.size()];
			for (int i = 0; i < mark.length; i++) {
				mark[i] = true;
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(String.format("%s/representativeTweets_%d.txt", outputPath, w)));
			BufferedWriter bw_all = new BufferedWriter(
					new FileWriter(String.format("%s/all_representative_tweets.txt", outputPath), true));
			bw_all.write(String.format("*********** w = %d ***********\n", w));
			for (int i = 0; i < mark.length; i++) {
				if (!mark[i]) {
					continue;
				}
				bw.write(String.format("%s\n", representativeTweets.get(i).getText().replace("\n", " ")));
				bw_all.write(String.format("%s\n", representativeTweets.get(i).getText().replace("\n", " ")));
				for (int j = i + 1; j < mark.length; j++) {
					if (getJCSimilarity(representativeTweets.get(i), representativeTweets.get(j)) >= 0.5) {
						mark[j] = false;
					}
				}
			}
			bw.close();
			bw_all.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void genSummary() {
		try {
			HashMap<Integer, List<Tweet>> rawWindows = getRawSteps();
			/*
			 * for (Map.Entry<Integer, List<Tweet>> window :
			 * rawWindows.entrySet()) { BufferedWriter bw = new
			 * BufferedWriter(new FileWriter( String.format(
			 * "E:/code/java/TweetStreamSummarization/inspection/%d.txt",
			 * window.getKey()))); for (Tweet tweet : window.getValue()) {
			 * bw.write(String.format("%s\t%d\t%s\n", tweet.getTweetId(),
			 * tweet.getPublishedTime(), tweet.getText().replace("\t", " "))); }
			 * bw.close(); }
			 */

			preprocess(rawWindows);
			learnModel();
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(String.format("%s/all_representative_tweets.txt", outputPath)));
			bw.close();
			for (int w = 1; w <= steps.size(); w++) {
				selectRepresentativeTweets(w, 0.9);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
