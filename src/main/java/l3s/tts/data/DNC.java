package l3s.tts.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DNC {
	static class JsonTweet implements Comparable<JsonTweet> {

		private long id;
		private String jsonStr;
		private int batch;

		public JsonTweet(long _id, String _jsonString) {
			id = _id;
			jsonStr = _jsonString;
		}

		public long getId() {
			return id;
		}

		public String getJsonStr() {
			return jsonStr;
		}

		public JsonTweet(long _id, String _jsonString, int _batch) {
			id = _id;
			jsonStr = _jsonString;
			batch = _batch;
		}

		public int getBatch() {
			return batch;
		}

		public int compareTo(JsonTweet o) {
			if (id < o.getId()) {
				return -1;
			} else if (id > o.getId()) {
				return 1;
			} else {
				return 0;
			}
		}

	}

	static void checkOrder() {
		try {
			JsonParser parser = new JsonParser();
			String filename = "/home/hoang/tss/data/harvey_twitter_dataset/02_archive_only/HurricaneHarvey.json";
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			long prevId = -1;
			int nViolations = 0;
			while ((line = br.readLine()) != null) {
				JsonObject jsonTweet = (JsonObject) parser.parse(line);
				if (jsonTweet.has("id")) {
					long Id = jsonTweet.get("id").getAsLong();
					if (Id <= prevId) {
						nViolations++;
					} else {
						prevId = Id;
					}
				}
			}
			br.close();
			System.out.printf("#Violations = %d", nViolations);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	static void getPartialOrdered(int batch, int nLines) {
		try {
			JsonParser parser = new JsonParser();
			List<JsonTweet> tweets = new ArrayList<DNC.JsonTweet>();
			String filename = "/home/hoang/tss/data/metadc993944_dnc/data/data/02_archive_only/DNCinPHL.json";
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			int startIndex = batch * nLines;
			int endIndex = (batch + 1) * nLines;

			int lineIndex = 0;
			while (lineIndex < startIndex) {
				line = br.readLine();
				if (line == null) {
					break;
				}
				lineIndex++;
			}

			while (lineIndex < endIndex) {
				line = br.readLine();
				if (line == null) {
					break;
				}
				lineIndex++;
				JsonObject jsonTweet = (JsonObject) parser.parse(line);
				if (jsonTweet.has("id")) {
					long id = jsonTweet.get("id").getAsLong();
					tweets.add(new JsonTweet(id, line));
				}
			}
			br.close();

			Collections.sort(tweets);
			filename = "/home/hoang/tss/data/metadc993944_dnc/ordered";
			filename = String.format("%s/partial_%d.json", filename, batch);

			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
			for (int i = 0; i < tweets.size(); i++) {
				bw.write(String.format("%s\n", tweets.get(i).getJsonStr()));
			}
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	static void order() {
		try {
			BufferedReader[] br = new BufferedReader[24];
			for (int b = 0; b < 24; b++) {
				String filename = "/home/hoang/tss/data/metadc993944_dnc/ordered";
				filename = String.format("%s/partial_%d.json", filename, b);
				br[b] = new BufferedReader(new FileReader(filename));
			}
			JsonParser parser = new JsonParser();
			Queue<JsonTweet> queue = new PriorityQueue<DNC.JsonTweet>();
			String line = null;
			for (int b = 0; b < 24; b++) {
				line = br[b].readLine();
				if (line == null) {
					continue;
				}
				JsonObject jsonTweet = (JsonObject) parser.parse(line);
				if (jsonTweet.has("id")) {
					long id = jsonTweet.get("id").getAsLong();
					queue.add(new JsonTweet(id, line, b));
				}
			}
			String filename = "/home/hoang/tss/data/metadc993944_dnc/ordered/allTweets.json";
			int nTweets = 0;
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
			while (queue.size() > 0) {
				JsonTweet tweet = queue.poll();
				bw.write(String.format("%s\n", tweet.getJsonStr()));
				nTweets++;

				if (nTweets % 10000 == 0) {
					System.out.printf("[%d] id = %d\n", nTweets, tweet.getId());
				}
				int b = tweet.getBatch();
				line = br[b].readLine();
				if (line == null) {
					continue;
				}
				JsonObject jsonTweet = (JsonObject) parser.parse(line);
				if (jsonTweet.has("id")) {
					long id = jsonTweet.get("id").getAsLong();
					queue.add(new JsonTweet(id, line, b));
				}

			}
			bw.close();
			for (int b = 0; b < 24; b++) {
				br[b].close();
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	static void filterNonEnglishTweets() {
		try {
			JsonParser jsonParser = new JsonParser();
			SimpleDateFormat datetimeParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss +0000 yyyy");
			String filename = "/home/hoang/tss/data/metadc993944_dnc/ordered/allTweets.json";
			BufferedReader br = new BufferedReader(new FileReader(filename));
			filename = "/home/hoang/tss/data/dnc/dnc_2016.txt";
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
			String line = null;
			while ((line = br.readLine()) != null) {
				JsonObject jsonTweet = (JsonObject) jsonParser.parse(line);
				if (!jsonTweet.get("lang").getAsString().equals("en")) {
					continue;
				}
				String tweetId = jsonTweet.get("id").getAsString();
				String userId = jsonTweet.get("user").getAsJsonObject().get("id").getAsString();
				long createdAt = (datetimeParser.parse(jsonTweet.get("created_at").getAsString())).getTime();
				String content = jsonTweet.get("text").getAsString();
				content = content.replace("\n", " ");
				content = content.replace("\r", " ");
				bw.write(String.format("%s\tnull\t%d\t%s\t%s\n", tweetId, createdAt, userId, content));
			}
			br.close();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	static void statByDate() {
		try {
			String filename = "/home/hoang/tss/data/dnc/dnc_2016.txt";
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			HashMap<String, Integer> tweetcounts = new HashMap<String, Integer>();
			while ((line = br.readLine()) != null) {
				long createdAt = Long.parseLong(line.split("\t")[2]);
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(createdAt);
				String dateStr = String.format("%d/%d/%d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1,
						cal.get(Calendar.YEAR));
				if (tweetcounts.containsKey(dateStr)) {
					tweetcounts.put(dateStr, tweetcounts.get(dateStr) + 1);
				} else {
					tweetcounts.put(dateStr, 1);
				}
			}
			br.close();

			filename = "/home/hoang/tss/dnc_2016_tweetcount.csv";
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
			for (String dateStr : tweetcounts.keySet()) {
				String[] tokens = dateStr.split("/");
				bw.write(String.format("%s,%s,%s,%d\n", tokens[0], tokens[1], tokens[2], tweetcounts.get(dateStr)));
			}
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	static void selectTweets() {
		try {
			String filename = "/home/hoang/tss/data/dnc/dnc_2016.txt";
			BufferedReader br = new BufferedReader(new FileReader(filename));
			filename = "/home/hoang/tss/data/dnc/dnc_2016_selected_tweets.txt";
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));

			HashSet<String> selectedDates = new HashSet<String>();
			selectedDates.add("22/7/2016");
			selectedDates.add("23/7/2016");
			selectedDates.add("24/7/2016");
			selectedDates.add("25/7/2016");
			selectedDates.add("26/7/2016");
			selectedDates.add("27/7/2016");
			selectedDates.add("28/7/2016");
			selectedDates.add("29/7/2016");
			selectedDates.add("30/7/2016");

			String line = null;
			while ((line = br.readLine()) != null) {
				long createdAt = Long.parseLong(line.split("\t")[2]);
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(createdAt);
				String dateStr = String.format("%d/%d/%d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1,
						cal.get(Calendar.YEAR));
				if (!selectedDates.contains(dateStr)) {
					continue;
				}
				bw.write(String.format("%s\n", line));
			}
			br.close();
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	static void test() {
		Queue<JsonTweet> queue = new PriorityQueue<DNC.JsonTweet>();
		queue.add(new JsonTweet(4, "4", 4));
		queue.add(new JsonTweet(2, "2", 2));
		queue.add(new JsonTweet(1, "1", 1));
		queue.add(new JsonTweet(5, "5", 5));
		queue.add(new JsonTweet(3, "3", 3));
		while (queue.size() > 0) {
			JsonTweet tweet = queue.poll();
			System.out.printf("id = %d str = %s batch = %d\n", tweet.getId(), tweet.getJsonStr(), tweet.getBatch());
		}

	}

	public static void main(String[] args) {
		// checkOrder();

		// test();
		// System.exit(1);
		// int nLines = 100000;
		// for (int b = 0; b < 24; b++) {
		// System.out.printf("batch = %d\n", b);
		// getPartialOrdered(b, nLines);
		// }

		// order();
		// filterNonEnglishTweets();
		// statByDate();

		selectTweets();
	}
}
