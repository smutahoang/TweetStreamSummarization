package l3s.tts.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class dataset {
	static class Tweet implements Comparable<Tweet> {
		private long tweetId;
		private String info;

		public Tweet(long _tweetId, String _info) {
			tweetId = _tweetId;
			info = _info;
		}

		public long getTweetId() {
			return tweetId;
		}

		public String getInfo() {
			return info;
		}

		public int compareTo(Tweet o) {
			if (tweetId < o.getTweetId()) {
				return -1;
			} else if (tweetId == o.getTweetId()) {
				return 1;
			} else {
				return 0;
			}
		}

	}

	static void getDatasets() {
		try {
			List<String> models = new ArrayList<String>();
			models.add("graph");
			models.add("ps");
			models.add("kw");

			HashMap<String, String> events = new HashMap<String, String>();
			events.put("london_attack", "londonAttack_2017-03-22");
			events.put("united_airline_scandal", "usScandal_2017-04-10");

			String atttResultPath = "C:/Users/Tuan-Anh Hoang/Desktop/attt";
			String outputPath = "E:/code/java/TweetStreamSummarization/data/input";

			for (String e : events.keySet()) {
				HashSet<String> tweetIds = new HashSet<String>();
				BufferedReader br = new BufferedReader(new FileReader(
						String.format("%s/manual/agreed/%s/groundTruth_30_0.80.csv", atttResultPath, e)));
				String line = null;
				while ((line = br.readLine()) != null) {
					tweetIds.add(line);
				}
				br.close();

				List<Tweet> tweets = new ArrayList<dataset.Tweet>();
				for (String m : models) {
					br = new BufferedReader(new FileReader(
							String.format("%s/%s/%s_%sFilteredTweets.txt", atttResultPath, e, events.get(e), m)));
					while ((line = br.readLine()) != null) {
						String id = line.split("\t")[0];
						if (tweetIds.contains(id)) {
							tweets.add(new Tweet(Long.parseLong(id), line));
							tweetIds.remove(id);
						}
					}
					br.close();
				}

				Collections.sort(tweets);

				BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s/%s.txt", outputPath, e)));
				for (Tweet tweet : tweets) {
					bw.write(String.format("%s\n", tweet.getInfo()));
				}
				bw.close();

			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String[] args) {
		getDatasets();
	}
}
