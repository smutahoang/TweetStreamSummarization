package l3s.tts.baseline.sumblr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import l3s.tts.configure.Configure;
import l3s.tts.utils.Tweet;

public class KMeans {

	private static HashMap<Integer, HashMap<String, Double>> findMeans(List<Tweet> tweets, int[] membership,
			int[] nMembers) {
		HashMap<Integer, HashMap<String, Double>> means = new HashMap<Integer, HashMap<String, Double>>();
		for (int i = 0; i < tweets.size(); i++) {
			Tweet tweet = tweets.get(i);
			int c = membership[i];
			if (means.containsKey(c)) {
				HashMap<String, Double> vector = means.get(c);
				for (Map.Entry<String, Double> pair : tweet.getVector().entrySet()) {
					String term = pair.getKey();
					double value = pair.getValue();
					if (vector.containsKey(term)) {
						vector.put(term, vector.get(term) + value / nMembers[c]);
					} else {
						vector.put(term, value / nMembers[c]);
					}
				}
			} else {
				HashMap<String, Double> vector = new HashMap<String, Double>();
				for (Map.Entry<String, Double> pair : tweet.getVector().entrySet()) {
					String term = pair.getKey();
					double value = pair.getValue();
					vector.put(term, value / nMembers[c]);
				}
				means.put(c, vector);
			}
		}
		return means;
	}

	public static int[] cluster(List<Tweet> tweets, int nClusters) {
		int[] membership = new int[tweets.size()];
		int[] nMembers = new int[nClusters];
		for (int c = 0; c < nMembers.length; c++) {
			nMembers[c] = 0;
		}
		Random rand = new Random();
		for (int i = 0; i < tweets.size(); i++) {
			int c = rand.nextInt(nClusters);
			membership[i] = c;
			nMembers[c]++;
		}

		for (int iter = 0; iter < Configure.SUMBLR_NUMBER_KMEANS_ITERATIONS; iter++) {
			HashMap<Integer, HashMap<String, Double>> means = findMeans(tweets, membership, nMembers);
			for (int c = 0; c < nMembers.length; c++) {
				nMembers[c] = 0;
			}

			for (int i = 0; i < tweets.size(); i++) {
				Tweet tweet = tweets.get(i);
				int index = -1;
				double distance = Double.POSITIVE_INFINITY;
				for (int c = 0; c < nMembers.length; c++) {
					HashMap<String, Double> vector = means.get(c);
					double d = 0;
					for (Map.Entry<String, Double> pair : tweet.getVector().entrySet()) {
						String term = pair.getKey();
						double value = pair.getValue();
						if (vector.containsKey(term)) {
							d += Math.pow(value - vector.get(term), 2);
						} else {
							d += value * value;
						}
					}
					for (Map.Entry<String, Double> pair : vector.entrySet()) {
						String term = pair.getKey();
						double value = pair.getValue();
						if (tweet.getVector().containsKey(term)) {
							continue;
						} else {
							d += value * value;
						}
					}
					if (d < distance) {
						distance = d;
						index = c;
					}
				}
				membership[i] = index;
				nMembers[index]++;
			}
		}

		return membership;
	}
}
