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
				// printCandidates();
				//System.exit(-1);
			} // if it is time to update
			else if (nOfTweets % Configure.TWEET_WINDOW == 0) {
				update();
				reGenerateSummary();
			}

		}
		// generateSummary();

	}

	public void generateSummary() {
		System.out.println(">>>>>>>>>>>>Find valid paths");
		findingCandidates();
		System.out.println(">>>>>>>>>>>>Remove duplicates");
		removeDuplicates();
		System.out.println(">>>>>>>>>>>>Combine valid paths");
		combineTweets();
		ArrayList<Candidate> summary = sortAndGetHighScoreSummaries();
		for (Candidate can : summary)
			System.out.println(can);
		System.out.println("Number of sentences in the summary: " + summary.size());
		affectedNodes.clear();

	}

	public void reGenerateSummary() {
		if (Configure.SIMPLE_UPDATE)
			generateSummary();
		else {
			// get the smallest position node in the list of candidates
			int[] indexOfAffectedNode = new int[affectedNodes.size()];
			// resample old candidates and find a new valid starting nodes
			int size = candidates.size();
			for (int k = 0; k < size; k++) {
				Candidate can = candidates.get(k);
				if (can.getIsCollapse()) {
					candidates.remove(can);
					continue;
				}
				// find the first affected node in a old candidate
				int firstAffectedNode = -1;
				for (int i = 0; i < affectedNodes.size(); i++) {
					int index = can.getNodeList().indexOf(affectedNodes.get(i));
					
					if (index != -1 && index < firstAffectedNode)
						firstAffectedNode = index;
					if(index !=-1 && index+1< indexOfAffectedNode[i]) {
						indexOfAffectedNode[i] = index;
					}
				}
				//
				if (firstAffectedNode != -1) {
					Candidate newCan = new Candidate();
					for (int i = 0; i <= firstAffectedNode; i++) {
						newCan.addNode(can.getNodeList().get(i));
						sampleAValidPath(newCan, can.getNodeList().get(firstAffectedNode));
					}
					candidates.remove(can);
				} 
			}
			// sample for new valid starting nodes
			for(int i = 0; i< indexOfAffectedNode.length; i++) {
				if(indexOfAffectedNode[i] == 0 && affectedNodes.get(i).isVSN()) {
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

	/*public boolean isTimeToUpdate() {

		return false;
	}*/

	public void update() {
		removeOldestTweets();
		reGenerateSummary();
	}

	private void removeOldestTweets() {
		for (int i = 0; i < Configure.NUMBER_OF_REMOVING_TWEETS; i++) {
			Tweet tweet = recentTweets.removeFirst();
			// remove nodes and edges
			List<TaggedToken> tokens = tweet.getTaggedTokens(preprocessingUtils);
			Node source = wordNodeMap.get(tokens.get(0).token + "/" + tokens.get(0).tag);
			int j = 0;
			affectedNodes.add(source);
			wordNodeMap.remove(tokens.get(0).token + "/" + tokens.get(0).tag);
			while (j < tokens.size() - 1) {

				Node target = wordNodeMap.get(tokens.get(j + 1).token + "/" + tokens.get(j + 1).tag);
				affectedNodes.add(target);
				wordNodeMap.remove(tokens.get(j + 1).token + "/" + tokens.get(j + 1).tag);
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
