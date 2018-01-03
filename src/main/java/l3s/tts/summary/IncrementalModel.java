package l3s.tts.summary;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import cmu.arktweetnlp.Tagger.TaggedToken;
import l3s.tts.configure.Configure;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetStream;

public class IncrementalModel extends SummarizationModel {
	private TweetStream stream;
	private Formatter format;
	private LinkedList<Tweet> recentTweets;

	public IncrementalModel(TweetStream stream, String outputDir) {
		super();
		this.stream = stream;
		openFile(outputDir);
		recentTweets = new LinkedList<Tweet>();

	}
	public void openFile(String output) {
		try {
			format = new Formatter(output);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * read tweets from stream and start generate summary
	 */
	public void run() {
		Tweet tweet = null;
		int nOfTweets = 0;
		long startTime = System.currentTimeMillis();
		long endTime;
		
		
		while ((tweet = stream.getTweet()) != null) {
			if (tweet.isReTweet())
				continue; // ignore retweets

			tweet.tagTokens(preprocessingUtils);
			recentTweets.add(tweet);
			tweet.setTweetId(nOfTweets);
			addNewTweet(tweet, nOfTweets);
			nOfTweets++;

			if (nOfTweets == Configure.TWEET_WINDOW) {
				endTime = System.currentTimeMillis();
				System.out.printf("Time for reading tweets: %d\n", (endTime - startTime));
				generateSummary();
				startTime = System.currentTimeMillis();

			} // if it is time to update
			else if (nOfTweets % Configure.TWEET_WINDOW == 0) {
				endTime = System.currentTimeMillis();
				System.out.printf("Time for reading tweets: %d\n", (endTime - startTime));
				update();
					startTime = System.currentTimeMillis();
			}

		}
		format.close();

	}

	public void generateSummary() {
		long time1, time2, time3, time4, time5;
		time1 = System.currentTimeMillis();
		buildAliasSampler();
		time2 = System.currentTimeMillis();

		System.out.println("\n>>>>>>>>>>>>Find valid paths");
		findingCandidates();
		time3 = System.currentTimeMillis();
		System.out.println(".......................GRAPH......................");
		printGraph();
		System.out.println(".......................LIST OF CANDIDATES.......................\n");
		printCandidates();
		
		System.out.println("................................................................");

		// System.out.println("\n>>>>>>>>>>>>Remove duplicates");

		// removeDuplicates();
		// time2 = System.currentTimeMillis();
		// System.out.printf("--------->Time for removing candidates: %d\n", (time2 -
		// time1));

		System.out.println("\n>>>>>>>>>>>>Combine valid paths");
		combineTweets();
		time4 = System.currentTimeMillis();

		System.out.println("\n>>>>>>>>>>>>Sort and get final paths");
		ArrayList<Candidate> summary = sortAndGetHighScoreSummaries();
		time5 = System.currentTimeMillis();

		System.out.println(".......................LIST OF CANDIDATES (BEFORE REMOVING AND SORTING).......................\n");
		printCandidates();

		System.out.println("................................................................");
		System.out.printf("--------->Time for building alias: %d\n", (time2 - time1));
		System.out.printf("--------->Time for finding candidates: %d\n", (time3 - time2));
		System.out.printf("--------->Time for combining candidates: %d\n", (time4 - time3));
		System.out.printf("--------->Time for sorting and getting high score candidates: %d\n", (time5 - time4));
		System.out.println("\n-----------------------FINAL SUMMARY-----------------------");
		for (Candidate can : summary)
			System.out.println(can);
		System.out.println("\nNumber of sentences in the summary: " + summary.size());
		System.out.println("...........................................................\n");

		affectedNodesByAdding.clear();
		affectedNodesByRemoving.clear();

	}

	public void reGenerateSummary() {

		long time1, time2, time3, time4;
		time1 = System.currentTimeMillis();

		if (Configure.SIMPLE_UPDATE) {
			generateSummary();
		} else {
			// get the smallest position of a node in the list of candidates
			HashSet<Node> existedValidStartingNodes = new HashSet<Node>();
			int removedCollapsedCan = 0;
			int removedCanInReOAdd = 0;
			int nOfOldCandidates = candidates.size();
			// re-sample old candidates and find new valid starting nodes
			for (int i = 0; i < nOfOldCandidates; i++) {
				Candidate can = candidates.get(i);
				can.setIsDiscard(false);
				if (can.getIsCollapsed()) {

					candidates.remove(can);
					removedCollapsedCan++;
					i--;
					nOfOldCandidates--;
					continue;
				}
				List<Node> nodesOfCandidate = can.getNodeList();
				for (int j = 0; j < nodesOfCandidate.size(); j++) {
					if (affectedNodesByAdding.contains(nodesOfCandidate.get(j))
							|| affectedNodesByRemoving.contains(nodesOfCandidate.get(j))) {
						if (j == 0)
							existedValidStartingNodes.add(nodesOfCandidate.get(j));

						// int loop = 0;
						// while (loop < Configure.SAMPLE_NUMBER) {
						Candidate newCan = new Candidate();
						List<int[]> currentOverlap = new ArrayList<int[]>();
						double currentScore = 0;
						currentOverlap.addAll(nodesOfCandidate.get(0).getTweetPosPairs());
						newCan.addNode(nodesOfCandidate.get(0));
						System.out.printf("--> VSN: %s in the sentences: [", nodesOfCandidate.get(0).getNodeName());
						for (int[] array : nodesOfCandidate.get(0).getTweetPosPairs()) {
							System.out.printf("[%d %d]", array[0], array[1]);
						}
						System.out.printf("]\n");
						for (int k = 1; k <= j; k++) {
							Node node = nodesOfCandidate.get(k);
							newCan.addNode(node);
							currentOverlap = getOverlapIntersection(currentOverlap, node.getTweetPosPairs());
							currentScore = computeScore(currentScore, currentOverlap, can.getNodeList().size());
						}
						sampleAValidPath(newCan, nodesOfCandidate.get(j), currentOverlap, currentScore);
						// loop++;
						// }

						removedCanInReOAdd++;
						candidates.remove(can);
						i--;
						nOfOldCandidates--;
						break;
					}

				}

			}
			int newValidStartingNode = 0;
			Iterator<Node> iter = affectedNodesByAdding.iterator();
			while (iter.hasNext()) {
				Node node = iter.next();
				if (!existedValidStartingNodes.contains(node) && node.isVSN()) {
					int loop = 0;
					newValidStartingNode++;

					System.out.printf("--> VSN: %s in the sentences: [", node.getNodeName());
					for (int[] array : node.getTweetPosPairs()) {
						System.out.printf("[%d %d]", array[0], array[1]);
					}
					System.out.printf("]\n");
					while (loop < Configure.SAMPLE_NUMBER) {
						Candidate newCan = new Candidate();
						newCan.addNode(node);
						List<int[]> currentOverlap = new ArrayList<int[]>();
						double currentScore = 0;
						currentOverlap.addAll(node.getTweetPosPairs());
						sampleAValidPath(newCan, node, currentOverlap, currentScore);
						loop++;
					}
				}
			}

			System.out.println(".......................LIST OF CANDIDATES.......................\n");
			printCandidates();
			System.out.println(".......................GRAPH......................");
			printGraph();
			System.out.println("................................................................");
			System.out.printf("+++++The total number of candidates: %d\n", candidates.size());
			System.out.printf("+++++The number of collapsed candidates removed: %d\n", removedCollapsedCan);
			System.out.printf("+++++The number of candidates in adding or removing list that is removed: %d\n",
					removedCanInReOAdd);
			System.out.printf("+++++The number of new valid starting nodes: %d\n", newValidStartingNode);

			time2 = System.currentTimeMillis();

			// System.out.println("\n>>>>>>>>>>>>Remove duplicates");
			// removeDuplicates();
			// time1 = System.currentTimeMillis();
			// System.out.printf("--------->Time for removing: %d\n", (time1 - time2));

			System.out.println("\n>>>>>>>>>>>>Combine valid paths");
			combineTweets();
			time3 = System.currentTimeMillis();

			System.out.println("\n>>>>>>>>>>>>Sort and get final paths");
			ArrayList<Candidate> summary = sortAndGetHighScoreSummaries();
			time4 = System.currentTimeMillis();

			System.out.println(".......................LIST OF CANDIDATES (BEFORE REMOVING AND SORTING).......................\n");
			printCandidates();
			System.out.println("................................................................");

			System.out.printf("--------->Time for resampling: %d\n", (time2 - time1));
			System.out.printf("--------->Time for combining: %d\n", (time3 - time2));
			System.out.printf("--------->Time for sorting and getting final candidates: %d\n", (time4 - time3));
			System.out.println("\n-----------------------FINAL SUMMARY-----------------------");
			for (Candidate can : summary)
				System.out.println(can);
			System.out.println("\n\nNumber of sentences in the summary: " + summary.size());
			affectedNodesByAdding.clear();
			affectedNodesByRemoving.clear();
			System.out.println("...........................................................\n");
		}

	}

	/*
	 * public boolean isTimeToUpdate() {
	 * 
	 * return false; }
	 */

	public void update() {
		System.out.printf("\n>>>>>>>>>>>>Removing oldest tweets");
		removeOldestTweets();
		System.out.printf("\n>>>>>>>>>>>>resample");
		buildAliasSampler();
		System.out.printf("\n>>>>>>>>>>>>Re-generating");

		reGenerateSummary();

	}

	private void removeOldestTweets() {

		for (int i = 0; i < Configure.NUMBER_OF_REMOVING_TWEETS; i++) {
			Tweet tweet = recentTweets.removeFirst();
			// remove nodes and edges
			List<TaggedToken> tokens = tweet.getTaggedTokens();

			int j = 0;
			StringBuilder builder = new StringBuilder(tokens.get(j).token);
			builder.append("/");
			builder.append(tokens.get(j).tag.toLowerCase());
			String nodeString = builder.toString();

			Node source = wordNodeMap.get(nodeString);
			if (source != null)
				affectedNodesByRemoving.add(source);
			Node target = null;
			while (j < tokens.size() - 1) {

				builder = new StringBuilder(tokens.get(j + 1).token);
				builder.append("/");
				builder.append(tokens.get(j + 1).tag.toLowerCase());
				nodeString = builder.toString();
				target = wordNodeMap.get(nodeString);
				if (target != null)
					affectedNodesByRemoving.add(target);

				DefaultWeightedEdge edge = graph.getEdge(source, target);
				double weight = graph.getEdgeWeight(edge);
				weight = weight - tweet.getWeight();
				if (weight == 0) {
					//if (edge != null) {
						source.setWeightOfOutgoingNodes(edge, 0);
						graph.removeEdge(edge);
					//}
					// System.out.println("remove an outgoing node of:"+source);
				} else {
					graph.setEdgeWeight(edge, weight);
					source.setWeightOfOutgoingNodes(edge, weight);
					// System.out.println("remove an outgoing node of: "+ source);
				}

				// remove pairs of tweetId - position out of the information of the node
				source.removeTweetPosPair(tweet.getTweetId(), j); // should re-factor this function

				/*if (source.getTweetPosPairs().size() == 0) {
					wordNodeMap.remove(nodeString);
					graph.removeVertex(source);
				}*/

				j++;
				source = target;
			}
			target.removeTweetPosPair(tweet.getTweetId(), j); // should re-factor this function

			/*if (target.getTweetPosPairs().size() == 0) {
				wordNodeMap.remove(nodeString);
				graph.removeVertex(target);
			}*/

		}

	}
}
