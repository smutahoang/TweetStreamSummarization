package l3s.tts.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import l3s.tts.baseline.lexrank.LexRank;
import l3s.tts.configure.Configure;
import l3s.tts.evaluation.ROUGE;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetPreprocessingUtils;

public class Inspection {
	static void testRouge() {
		// String groundtruthPath = "C:/Users/Tuan-Anh
		// Hoang/Desktop/tss/groundtruth/london_attack/10/representativeTweets_36.txt";
		String groundtruthPath = "C:/Users/Tuan-Anh Hoang/Desktop/tss/inc/london_attack/33_inc.txt";

		// String generatedPath = "C:/Users/Tuan-Anh
		// Hoang/Desktop/tss/groundtruth/london_attack/10/representativeTweets_37.txt";
		String generatedPath = "C:/Users/Tuan-Anh Hoang/Desktop/tss/inc/london_attack/34_inc.txt";
		// String generatedPath = "C:/Users/Tuan-Anh
		// Hoang/Desktop/tss/80_inc.txt";
		for (int K = 5; K <= 20; K += 5) {
			ROUGE rouge = new ROUGE(1, K, groundtruthPath, generatedPath);
			double precision = rouge.getPrecision();
			double recall = rouge.getRecall();
			double f1Score = rouge.getF1Score();
			System.out.printf(String.format("K = %d prec = %f rec = %f f1 = %f\n", K, precision, recall, f1Score));
			System.out.println("failedTerms:");
			List<String> failedTerms = new ArrayList<String>(rouge.FailedTerms());
			Collections.sort(failedTerms);
			for (String term : failedTerms) {
				System.out.printf("\t%s", term);
			}
			System.out.println();
		}
	}

	static void testTweetSimilarity() {
		try {
			TweetPreprocessingUtils nlpUtils = new TweetPreprocessingUtils();
			String t1 = "ICYMI, President Trump fired acting AG Sally Yates for refusing to defend his executive order restricting refugees https://t.co/ML…";
			String t2 = "President Trump fires acting Attorney General Sally Yates for refusal to enforce executive order, FOX News reports. https://t.co/…";

			List<String> terms1 = nlpUtils.extractTermInTweet(t1);
			List<String> terms2 = nlpUtils.extractTermInTweet(t2);

			HashSet<String> union = new HashSet<String>();

			union.addAll(terms1);
			union.addAll(terms2);

			double sim = (union.size() > 0) ? (double) (terms1.size() + terms2.size() - union.size()) / union.size()
					: 0;

			System.out.printf("sim = %f\n", sim);
			System.out.printf("#union_terms = %d\n", union.size());

			union = new HashSet<String>(terms1);
			System.out.printf("#terms_1 = %d\n", union.size());
			for (String term : union) {
				System.out.printf("\t%s", term);
			}
			System.out.println();

			union = new HashSet<String>(terms2);
			System.out.printf("#terms_2 = %d\n", union.size());
			for (String term : union) {
				System.out.printf("\t%s", term);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	static double getJaccardScore(List<String> listOfTerms1, List<String> listOfTerms2) {
		HashSet<String> union = new HashSet<String>();

		union.addAll(listOfTerms1);
		union.addAll(listOfTerms2);

		return (union.size() > 0) ? (double) (listOfTerms1.size() + listOfTerms2.size() - union.size()) / union.size()
				: 0;
	}

	static Set<Tweet> removeRedundancyByDiversifiedRanking_Pre(HashSet<Tweet> input) {
		TweetPreprocessingUtils preprocessingUtils = new TweetPreprocessingUtils();
		Set<Tweet> output = new HashSet<Tweet>();

		// compute Jaccard similarity of each tweet with the others
		List<Tweet> listOfTweets = new ArrayList<Tweet>(input);
		HashMap<Tweet, Double> tweetSimilarityMap = new HashMap<Tweet, Double>();
		tweetSimilarityMap.put(listOfTweets.get(0), 0.0);
		for (int i = 0; i < listOfTweets.size() - 1; i++) {
			double sum = 0;
			for (int j = i + 1; j < listOfTweets.size(); j++) {
				double score = getJaccardScore(listOfTweets.get(i).getTerms(preprocessingUtils),
						listOfTweets.get(j).getTerms(preprocessingUtils));
				if (i == 0)
					tweetSimilarityMap.put(listOfTweets.get(j), score);
				else
					tweetSimilarityMap.put(listOfTweets.get(j), score + tweetSimilarityMap.get(listOfTweets.get(j)));
				sum += score;
			}
			double currentScore = tweetSimilarityMap.get(listOfTweets.get(i));
			tweetSimilarityMap.put(listOfTweets.get(i), sum + currentScore);
		}

		// remove redundancy
		double sumOfUtility = 0;
		while (true) {
			// get the best tweet
			double utility = 0;
			Tweet bestTweet = null;
			for (Tweet t : listOfTweets) {
				if (tweetSimilarityMap.get(t) > utility) {
					bestTweet = t;
					utility = tweetSimilarityMap.get(t);
				}
			}
			sumOfUtility += utility;
			// System.out.printf("utility: %f\n", utility);
			if (utility / sumOfUtility < Configure.JACCARD_UTILITY)
				break;

			System.out.printf("[selected_tweets] %s\n", bestTweet.getText());

			output.add(bestTweet);
			listOfTweets.remove(bestTweet);
			// reduce score of remaining tweets
			for (Tweet t : listOfTweets) {
				tweetSimilarityMap.put(t, tweetSimilarityMap.get(t)
						- getJaccardScore(t.getTerms(preprocessingUtils), bestTweet.getTerms(preprocessingUtils)));
				// System.out.println("fff"+tweetSimilarityMap.get(t));
			}
		}
		// System.out.println(output.size());
		return output;
	}

	static List<Tweet> removeRedundancyByDiversifiedRanking(HashSet<Tweet> input) {

		TweetPreprocessingUtils preprocessingUtils = new TweetPreprocessingUtils();

		List<Tweet> output = new ArrayList<Tweet>();
		// compute Jaccard similarity of each tweet with the others
		List<Tweet> listOfTweets = new ArrayList<Tweet>(input);
		int nTweets = listOfTweets.size();
		double[][] similarityMatrix = new double[nTweets][nTweets];
		double[] sumSimmilarity = new double[nTweets];
		for (int i = 0; i < nTweets; i++) {
			sumSimmilarity[i] = 0;
		}
		HashSet<Integer> indexes = new HashSet<Integer>();
		for (int i = 0; i < nTweets; i++) {
			Tweet tweet_i = listOfTweets.get(i);
			for (int j = i + 1; j < nTweets; j++) {
				Tweet tweet_j = listOfTweets.get(j);
				similarityMatrix[i][j] = getJaccardScore(tweet_i.getTerms(preprocessingUtils),
						tweet_j.getTerms(preprocessingUtils));
				similarityMatrix[j][i] = similarityMatrix[i][j];
				sumSimmilarity[i] += similarityMatrix[i][j];
				sumSimmilarity[j] += similarityMatrix[i][j];
			}
			indexes.add(i);
		}

		boolean[] covered = new boolean[nTweets];
		for (int i = 0; i < nTweets; i++) {
			System.out.printf("[%s]\t%s\n", sumSimmilarity[i], listOfTweets.get(i).getText());
			covered[i] = false;
		}

		// remove redundancy
		double sumOfUtility = 0;
		while (true) {
			if (indexes.size() == 0) {
				break;
			}
			// get the best tweet
			double utility = 0;
			int bestIndex = -1;
			for (int index : indexes) {
				if (sumSimmilarity[index] > utility) {
					bestIndex = index;
					utility = sumSimmilarity[index];
				}
			}
			sumOfUtility += utility;
			// System.out.printf("utility: %f\n", utility);
			if (utility / sumOfUtility < Configure.JACCARD_UTILITY)
				break;
			System.out.printf("[selected_tweets] %s\n", listOfTweets.get(bestIndex).getText());
			output.add(listOfTweets.get(bestIndex));
			indexes.remove(bestIndex);
			// reduce score of remaining tweets

			HashSet<Integer> neighbors = new HashSet<Integer>();
			neighbors.add(bestIndex);
			for (int index : indexes) {
				if (covered[index] == true || index == bestIndex) {
					continue;
				}
				if (similarityMatrix[index][bestIndex] >= 0.5) {
					neighbors.add(index);
				}
			}
			indexes.removeAll(neighbors);
			for (int index : neighbors) {
				covered[index] = true;
				for (int rIndex : indexes) {
					sumSimmilarity[rIndex] -= similarityMatrix[index][rIndex];
				}
			}
		}
		// System.out.println(output.size());
		return output;
	}

	static List<Tweet> removeRedundancyByLexRank(HashSet<Tweet> input) {
		TweetPreprocessingUtils preprocessingUtils = new TweetPreprocessingUtils();
		HashMap<String, Integer> termDF = new HashMap<String, Integer>();
		for (Tweet tweet : input) {
			for (String term : tweet.getTerms(preprocessingUtils)) {
				if (termDF.containsKey(term)) {
					termDF.put(term, termDF.get(term) + 1);
				} else {
					termDF.put(term, 1);
				}
			}
		}
		for (Tweet tweet : input) {
			tweet.buildVector(termDF, input.size());
		}
		LexRank lexranker = new LexRank();
		List<Tweet> selectedTwets = lexranker.summary(new ArrayList<Tweet>(input),
				Configure.LEXRANK_MIN_EDGE_SIMILARITY, true, 20, Configure.LEXRANK_MAX_JC_COEFFICIENT);
		return selectedTwets;
	}

	static void testDiversifiedTweetSelection() {
		try {
			String filename = "C:/Users/Tuan-Anh Hoang/Desktop/tss/candidates.csv";
			BufferedReader br = new BufferedReader(new FileReader(filename));
			HashSet<Tweet> tweets = new HashSet<Tweet>();
			String line = null;
			while ((line = br.readLine()) != null) {
				int p = line.indexOf(']');
				Tweet tweet = new Tweet(null, line.substring(p + 2), null, 0);
				tweets.add(tweet);
			}
			br.close();

			removeRedundancyByDiversifiedRanking(tweets);
			System.out.println("******************************************************************");
			List<Tweet> selectedTwets = removeRedundancyByLexRank(tweets);
			for (int i = 0; i < selectedTwets.size(); i++) {
				System.out.printf("[LR_selected_tweets] %s\n", selectedTwets.get(i).getText());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String args[]) {
		new Configure();
		// testRouge();
		testTweetSimilarity();
		// testDiversifiedTweetSelection();
	}
}
