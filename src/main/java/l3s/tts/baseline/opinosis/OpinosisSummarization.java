package l3s.tts.baseline.opinosis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import cmu.arktweetnlp.Tagger.TaggedToken;
import l3s.tts.configure.Configure;
import l3s.tts.configure.Configure.UpdatingType;
import l3s.tts.utils.TimeUtils;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetStream;

public class OpinosisSummarization extends SummarizationModel {
	private TweetStream stream;
	private LinkedList<Tweet> recentTweets;
	private long nextUpdate;
	private int currentTime;
	private long refTime;
	private int nOfTweets;
	private static String outputPath = Configure.WORKING_DIRECTORY +"/output/baselines/opinosis";

	public OpinosisSummarization(TweetStream stream) {
		this.stream = stream;
		recentTweets = new LinkedList<Tweet>();

		// first tweet

		Tweet tweet = stream.getTweet();
		tweet.tagTokens(preprocessingUtils);
		recentTweets.add(tweet);
		tweet.setIndex(0);
		addNewTweet(tweet, 0);
		refTime = tweet.getPublishedTime();
		currentTime = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);
		currentTime = TimeUtils.getElapsedTime(tweet.getPublishedTime(), tweet.getPublishedTime(),
				Configure.TIME_STEP_WIDTH);
		tweet.setTimeStep(currentTime);
		nextUpdate = tweet.getPublishedTime() + Configure.TIME_STEP_WIDTH;
		nOfTweets = 1;
	}

	/**
	 * read tweets from stream and start generate summary
	 */
	public void run() {
		Tweet tweet = null;

		while ((tweet = stream.getTweet()) != null) {
			tweet.tagTokens(preprocessingUtils);
			recentTweets.add(tweet);
			tweet.setIndex(nOfTweets);
			addNewTweet(tweet, nOfTweets);
			nOfTweets++;
			currentTime = TimeUtils.getElapsedTime(tweet.getPublishedTime(), refTime, Configure.TIME_STEP_WIDTH);
			tweet.setTimeStep(currentTime);
			if ((Configure.updatingType == UpdatingType.TWEET_COUNT && nOfTweets % Configure.TWEET_WINDOW == 0)
					|| (Configure.updatingType == UpdatingType.PERIOD && tweet.getPublishedTime() >= nextUpdate)) {
				genSummary();
				System.out.println("number of tweets: " + nOfTweets);
				nextUpdate += Configure.TIME_STEP_WIDTH;
			}

		}

	}

	public void genSummary() {

		// System.out.println("number of nodes: " + graph.vertexSet().size());
		// System.out.println("number of edges: " + graph.edgeSet().size());
		
		
		try {
			FileWriter file = new FileWriter(String.format("%s/representativeTweets_%d.txt", outputPath, currentTime));
			BufferedWriter buff = new BufferedWriter(file);
			removeOldTweets();
			findingCandidates();
			
			System.out.println("____________________________________");
			List<Candidate> theSentenceInfos = getFinalSentences();
			for (Candidate info : theSentenceInfos) {
				info.sent = info.sent.replaceAll("(/[a-z,.;$]+(\\s+|$))", " ");
				info.sent = info.sent.replaceAll("xx", "");
				info.sent = String.valueOf(info.sent) + " .";
				info.sent = info.sent.replaceAll("\\s+", " ");
				System.out.println(info.sent);
				buff.write(String.format("%s\n", info.sent));
			}
			System.out.println("____________________________________");
			clearOldData();
			buff.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		

	}

	private void clearOldData() {
		shortlisted.clear();
		candidates.clear();
	}

	private void removeOldTweets() {
		
		if (currentTime <= Configure.FORGOTTEN_WINDOW_DISTANCE) {
			return;
		}
		System.out.println("--> removing");
		int lastTimeStep = currentTime - Configure.FORGOTTEN_WINDOW_DISTANCE;
		int count = 0;
		while (true) {
			if (recentTweets.getFirst().getTimeStep() <= lastTimeStep) {
				Tweet tweet = recentTweets.removeFirst();
				List<TaggedToken> tokens = tweet.getTaggedTokens();

				int j = 0;
				StringBuilder builder = new StringBuilder(tokens.get(j).token);
				builder.append("/");
				builder.append(tokens.get(j).tag.toLowerCase());
				String nodeString = builder.toString();

				Node source = wordNodeMap.get(nodeString);

				Node target = null;

				while (j < tokens.size()) {
					source.removeTweetPosPair(tweet.getIndex(), j);
					if (source.getTweetPosPairs().size() == 0) {
						graph.removeVertex(source);
						wordNodeMap.remove(nodeString);
						count++;
					}

					j++;
					if (j == tokens.size())
						break;
					builder = new StringBuilder(tokens.get(j).token);
					builder.append("/");
					builder.append(tokens.get(j).tag.toLowerCase());
					nodeString = builder.toString();
					target = wordNodeMap.get(nodeString);
					DefaultWeightedEdge edge = graph.getEdge(source, target);

					double weight = graph.getEdgeWeight(edge) - tweet.getWeight();
					if (weight == 0)
						graph.removeEdge(edge);
					else
						graph.setEdgeWeight(edge, weight);
					source = target;
				}

			} else {
				break;
			}
			// System.out.println("number of removed nodes: " + count);
		}

	}

}
