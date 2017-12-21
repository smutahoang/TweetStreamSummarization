package l3s.tts.summary;

import java.util.ArrayList;
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
	// private String outputDir;
	private LinkedList<Tweet> recentTweets;

	public IncrementalModel(TweetStream stream, String outputDir) {
		super();
		this.stream = stream;
		// this.outputDir = outputDir;
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
			// System.out.println("Tweet: " + tweet.getText());
			tweet.getTaggedTokens(preprocessingUtils);
			// System.out.println("Tweet: " + tweet.getText()+"\n");
			recentTweets.add(tweet);
			tweet.setTweetId(nOfTweets);
			addNewTweet(tweet, nOfTweets);
			nOfTweets++;
			
			if (nOfTweets == Configure.TWEET_WINDOW) {
				//printGraph();
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
		affectedNodesByAdding.clear();

	}

	public void reGenerateSummary() {
		if (Configure.SIMPLE_UPDATE)
			generateSummary();
		else {
			// get the smallest position of a node in the list of candidates

			
			HashSet<Node> existedValidStartingNodes = new HashSet<Node>();
			
			// re-sample old candidates and find new valid starting nodes
			for(int i =0; i<candidates.size(); i++) {
				Candidate can = candidates.get(i);
				can.setIsDiscard(false);
				if (can.getIsCollapsed()) {
					candidates.remove(can);
					i--;
					continue;
				}
				List<Node> nodesOfCandidate =can.getNodeList();
				for(int j = 0; j<nodesOfCandidate.size(); j++) {
					if(affectedNodesByAdding.contains(nodesOfCandidate.get(j)) || affectedNodesByRemoving.contains(nodesOfCandidate.get(j))) {
						if(j == 0)
							existedValidStartingNodes.add(nodesOfCandidate.get(j));
						Candidate newCan = new Candidate();
						for(int k = 0; k<=j; k++) {
							newCan.addNode(can.getNodeList().get(k));
						}
						sampleAValidPath(newCan, nodesOfCandidate.get(j));
						candidates.remove(can);
						i--;
						break;	
					}
					
				}
			}
			Iterator<Node> iter = affectedNodesByAdding.iterator();
			while(iter.hasNext()) {
				Node node = iter.next();
				if(!existedValidStartingNodes.contains(node)&& node.isVSN()) {
					Candidate newCan = new Candidate();
					newCan.addNode(node);
					//System.out.println("Re-generating: "+newCan.toString());
					sampleAValidPath(newCan, node);
				}
			}
			
			
			System.out.println("\n>>>>>>>>>>>>Remove duplicates");
			removeDuplicates();

			System.out.println("\n>>>>>>>>>>>>Combine valid paths");
			combineTweets();

			System.out.println("\n>>>>>>>>>>>>Sort and get final paths");
			ArrayList<Candidate> summary = sortAndGetHighScoreSummaries();
			for (Candidate can : summary)
				System.out.println(can);
			System.out.println("\n\nNumber of sentences in the summary: " + summary.size());
		}

	}

	/*
	 * public boolean isTimeToUpdate() {
	 * 
	 * return false; }
	 */

	public void update() {
		System.out.println(">>>>>>>>>>>>>Removing oldest tweets");
		removeOldestTweets();
		System.out.println(">>>>>>>>>>>>>Re-generating");
		reGenerateSummary();
	}

	private void removeOldestTweets() {

		for (int i = 0; i < Configure.NUMBER_OF_REMOVING_TWEETS; i++) {
			Tweet tweet = recentTweets.removeFirst();
			// remove nodes and edges
			List<TaggedToken> tokens = tweet.getTaggedTokens(preprocessingUtils);

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
					graph.removeEdge(edge);
					source.setWeightOfOutgoingNodes(edge, 0);
					//System.out.println("remove an outgoing node of:"+source);
				} else {
					graph.setEdgeWeight(edge, weight);
					source.setWeightOfOutgoingNodes(edge, weight);
					//System.out.println("remove an outgoing node of: "+ source);
				}
				
				// remove pairs of tweetId - position out of the information of the node\
				
				source.removeTweetPosPair(tweet.getTweetId(), j); // should re-factor this function
				/*if(source.getTweetPosPairs().size() == 0) {
					wordNodeMap.remove(nodeString);
					graph.removeVertex(source);
				}*/
				j++;
				source = target;
			}
			target.removeTweetPosPair(tweet.getTweetId(), j); // should re-factor this function
			/*if(target.getTweetPosPairs().size() == 0) {
				wordNodeMap.remove(nodeString);
				graph.removeVertex(target);
			}*/
		}

	}
}
