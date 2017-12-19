package l3s.tts.summary;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import cmu.arktweetnlp.Tagger.TaggedToken;
import l3s.tts.configure.Configure;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetStream;

public class IncrementalModel extends SummarizationModel {
	private TweetStream stream;
	private String outputDir;
	private LinkedList<Tweet> recentTweets;

	public IncrementalModel(TweetStream stream, String outputDir) {
		super();
		this.stream = stream;
		this.outputDir = outputDir;
		recentTweets = new LinkedList<Tweet>();

	}

	/**
	 * read tweets from stream and start generate summary
	 */
	public void run() {
		Tweet tweet = null;
		int nOfTweets = 0;
		while ((tweet = stream.getTweet()) != null) {
			if (tweet.isReTweet())
				continue; // ignore retweets
			recentTweets.add(tweet);
			addNewTweet(tweet, nOfTweets);
			nOfTweets++;

			if (nOfTweets == Configure.TWEET_WINDOW) {
				generateSummary();

			} // if it is time to update
			else if (nOfTweets % Configure.TWEET_WINDOW == 0) {
				update();

			}

		}
		// generateSummary();

	}

	public void generateSummary() {
		System.out.println("\n>>>>>>>>>>>>Find valid paths");
		findingCandidates();

		System.out.println("\n>>>>>>>>>>>>Remove duplicates");
		removeDuplicates();

		System.out.println("\n>>>>>>>>>>>>Combine valid paths");
		combineTweets();

		System.out.println("\n>>>>>>>>>>>>Sort and get final paths");
		ArrayList<Candidate> summary = sortAndGetHighScoreSummaries();
		for (Candidate can : summary)
			System.out.println(can);
		System.out.println("\nNumber of sentences in the summary: " + summary.size());
		affectedNodes.clear();

	}

	public void reGenerateSummary() {
		if (Configure.SIMPLE_UPDATE)
			generateSummary();
		else {
			// get the smallest position of a node in the list of candidates

			int[] indexOfAffectedNode = new int[affectedNodes.size()];

			// re-sample old candidates and find new valid starting nodes

			System.out.println("current candidates: " + candidates.size());
			for (int k = 0; k < candidates.size(); k++) {
				Candidate can = candidates.get(k);
				can.setIsDiscard(false);
				if (can.getIsCollapsed()) {
					candidates.remove(can);
					k--;
					continue;
				}

				int firstAffectedNode = -1;
				for (int i = 0; i < affectedNodes.size(); i++) {
					int index = can.getNodeList().indexOf(affectedNodes.get(i));
					// find the first affected node in an old candidate
					if (index != -1 && index < firstAffectedNode)
						firstAffectedNode = index;
					// mark affected nodes that is a valid starting node in one of old candidates
					if (index == 0) {
						indexOfAffectedNode[i] = 1;
					}
				}
				// sample a candidate again from the first affected node
				if (firstAffectedNode != -1) {
					Candidate newCan = new Candidate();
					for (int i = 0; i <= firstAffectedNode; i++) {
						newCan.addNode(can.getNodeList().get(i));

					}
					sampleAValidPath(newCan, newCan.getNodeList().get(firstAffectedNode));
					candidates.remove(can);
					k--;

				}
			}
			
			// sample candidates for new valid starting nodes
			for (int i = 0; i < indexOfAffectedNode.length; i++) {
				
				if (indexOfAffectedNode[i] == 0 && affectedNodes.get(i).isVSN()) {
					Candidate can = new Candidate();
					can.addNode(affectedNodes.get(i));
					sampleAValidPath(can, affectedNodes.get(i));
				}
			}
			removeDuplicates();
			combineTweets();
			ArrayList<Candidate> summary = sortAndGetHighScoreSummaries();
			for (Candidate can : summary)
				System.out.println(can);
			System.out.println("Number of sentences in the summary: " + summary.size());
		}

	}

	/*
	 * public boolean isTimeToUpdate() {
	 * 
	 * return false; }
	 */

	public void update() {
		
		removeOldestTweets();
		
		reGenerateSummary();
	}

	private void removeOldestTweets() {

		for (int i = 0; i < Configure.NUMBER_OF_REMOVING_TWEETS; i++) {
			Tweet tweet = recentTweets.removeFirst();
			// remove nodes and edges
			List<TaggedToken> tokens = tweet.getTaggedTokens(preprocessingUtils);
			
			int j = 0;
			

			Node source = wordNodeMap.remove(tokens.get(0).token + "/" + tokens.get(0).tag.toLowerCase());
			if(source != null)
				affectedNodes.add(source);
			while (j < tokens.size() - 1) {
				
				Node target = wordNodeMap.remove(tokens.get(j + 1).token + "/" + tokens.get(j + 1).tag.toLowerCase());
				if(target !=null)
					affectedNodes.add(target);
					
				
				DefaultWeightedEdge edge = graph.getEdge(source, target);
				double weight = graph.getEdgeWeight(edge);
				weight = weight - 1;
				if (weight == 0)
					graph.removeEdge(edge);
				else
					graph.setEdgeWeight(edge, weight);
				j++;
				source = target;
			}
		}

	}
}
