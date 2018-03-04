package l3s.tts.summary;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import l3s.tts.configure.Configure;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetStream;

public class IncrementalModel extends SummarizationModel {
	private TweetStream stream;
	private Formatter format;
	private LinkedList<Tweet> recentTweets;

	public IncrementalModel() {
		super();
	}

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
			// if (tweet.isReTweet())
			// continue; // ignore retweets

			recentTweets.add(tweet);
			// tweet.setTweetId(nOfTweets);
			addNewTweet(tweet);
			nOfTweets++;

			if (nOfTweets == Configure.TWEET_WINDOW) {
				System.out.printf("\n-----NUMBER OF NEW NODES: %d\n", newNodes.size());
				System.out.printf("\n-----NUMBER OF NODES IN THE GRAPH: %d\n", wordNodeMap.size());
				endTime = System.currentTimeMillis();
				System.out.printf("Time for reading tweets: %d\n", (endTime - startTime));
				generateSummary();

				startTime = System.currentTimeMillis();

			} // if it is time to update
			else if (nOfTweets % Configure.TWEET_WINDOW == 0) {
				System.out.printf("\n-----NUMBER OF NEW NODES: %d\n", newNodes.size());
				System.out.printf("\n-----NUMBER OF NODES IN THE GRAPH: %d\n", wordNodeMap.size());
				endTime = System.currentTimeMillis();
				System.out.printf("Time for reading tweets: %d\n", (endTime - startTime));
				update();

				startTime = System.currentTimeMillis();

			}

		}
		format.close();

	}

	public void update() {
		long time1 = System.currentTimeMillis();
		removeOldestTweets();

		long time2 = System.currentTimeMillis();
		buildAliasSampler();

		long time3 = System.currentTimeMillis();
		updateRandomWalks();

		long time4 = System.currentTimeMillis();
		// resetPageRank();

		long time5 = System.currentTimeMillis();
		computePageRank();

		List<Node> topSubtopicsbyPageRank = getKSubtopicsBasedOnPagerank();
		printTopNodesByPagerank(topSubtopicsbyPageRank);

		long time6 = System.currentTimeMillis();
		getSubtopics();

		long time7 = System.currentTimeMillis();
		List<String> summary = getTopKTweetsForEachSubtopicAsASummary();
		// printSummary(summary);
		long time8 = System.currentTimeMillis();

		subtopics.clear();
		affectedNodesByAdding.clear();
		affectedNodesByRemoving.clear();
		newNodes.clear();

		System.out.printf(">>>>>>>>>>>TIME FOR REMOVING OLDEST TWEETS: %d\n", (time2 - time1));
		System.out.printf(">>>>>>>>>>>TIME FOR BUILDING ALIAS SAMPLER: %d\n", (time3 - time2));
		System.out.printf(">>>>>>>>>>>TIME FOR UPDATE RANDOM WALK: %d\n", (time4 - time3));
		System.out.printf(">>>>>>>>>>>TIME FOR RESET PAGERANK: %d\n", (time5 - time4));
		System.out.printf(">>>>>>>>>>>TIME FOR COMPUTING PAGERANK OF ALL NODES: %d\n", (time6 - time5));
		System.out.printf(">>>>>>>>>>>TIME FOR GETTING SUBTOPICS: %d\n", (time7 - time6));
		System.out.printf(">>>>>>>>>>>TIME FOR GENERATING SUMMARY: %d\n", (time8 - time7));
	}

	public void removeOldestTweets() {
		int numberOfRemovedNodes = 0;
		for (int i = 0; i < Configure.NUMBER_OF_REMOVING_TWEETS; i++) {
			Tweet tweet = recentTweets.removeFirst();

			List<String> terms = tweet.getTerms(preprocessingUtils);
			for (int j = 0; j < terms.size(); j++) {
				Node source = wordNodeMap.get(terms.get(j));
				if (source == null)
					continue;
				// removing the tweets
				source.removeTweet(tweet);
				affectedNodesByRemoving.add(source);
				// removing the edges
				// System.out.println(terms.get(j) +"\t" +
				// source.getNodeName());
				for (int k = 1; k < Configure.WINDOW_SIZE; k++) {
					if (j + k >= terms.size())
						break;

					//Node target = wordNodeMap.get(terms.get(j)); --> bug
					Node target = wordNodeMap.get(terms.get(j + k));
					DefaultWeightedEdge edge = graph.getEdge(source, target);
					double weight = graph.getEdgeWeight(edge);
					if (weight == 1) {
						graph.removeEdge(edge);
						source.setWeightOfOutgoingNodes(edge, 0); // update weight for outgoing edges of source node
					} else {
						graph.setEdgeWeight(edge, weight - tweet.getWeight());
						source.setWeightOfOutgoingNodes(edge, weight - tweet.getWeight()); // update weight for outgoing
																							// edges of source node
					}

				}
				// removing terms if need
				if (graph.edgesOf(source).size() == 0) {
					graph.removeVertex(source);
					wordNodeMap.remove(terms.get(j));
					if (terms.get(j).equals("naacp")) {
						System.err.println("naacp is deleted!");
						System.err.println(tweet.getText());
						// System.exit(-1);
					}
					numberOfRemovedNodes++;
				}
			}
		}
		/*
		 * System.out.printf("\n.................NUMBER OF NODES REMOVED: %d\n",
		 * numberOfRemovedNodes); System.out.printf(
		 * "\n.................NUMBER OF AFFECTED NODES BY REMOVING: %d\n",
		 * affectedNodesByRemoving.size());
		 */
	}

	public void reWalk(HashSet<Integer> affectedWalk) {
		for (int w : affectedWalk) {
			// remove the walk
			RandomWalk walk = randomWalks.get(w);
			for (Node node : walk.getVisitedNodes()) {
				if (!wordNodeMap.containsKey(node.getNodeName())) {
					continue;
				}
				node.removeWalk(w);
			}
			// sample the walk again
			Node node = walk.getStartingNode();
			if (!wordNodeMap.containsKey(node.getNodeName())) {
				continue;
			}
			RandomWalk newWalk = new RandomWalk(node);
			node.addVisit(walkId);
			keepWalking(newWalk, w, node);
			randomWalks.put(w, newWalk);
		}
	}

	public void updateRandomWalks() {
		HashSet<Integer> affectedWalk = new HashSet<Integer>();

		// affected node by adding
		Iterator<Node> iter = affectedNodesByAdding.iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			HashMap<Integer, Integer> visitedWalks = node.getVisistedWalk();
			for (Map.Entry<Integer, Integer> pair : visitedWalks.entrySet()) {
				affectedWalk.add(pair.getKey());
			}
		}

		// affected nodes by removing
		iter = affectedNodesByRemoving.iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			if (affectedNodesByAdding.contains(node)) {
				continue;
			}
			HashMap<Integer, Integer> visitedWalks = node.getVisistedWalk();
			for (Map.Entry<Integer, Integer> pair : visitedWalks.entrySet()) {
				affectedWalk.add(pair.getKey());
			}
		}

		// rewalk the affected walks
		reWalk(affectedWalk);

		// new nodes
		iter = newNodes.iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			for (int i = 0; i < Configure.NUMBER_OF_RANDOM_WALK_AT_EACH_NODE; i++) {
				RandomWalk newWalk = new RandomWalk(node);
				node.addVisit(walkId);
				keepWalking(newWalk, walkId, node);
				randomWalks.put(walkId, newWalk);
				walkId++;
			}
		}
	}

	public void generateSummary() {
		long time1 = System.currentTimeMillis();
		buildAliasSampler();

		long time2 = System.currentTimeMillis();
		sampleAllWalks();
		// printSegments();

		// long time3 = System.currentTimeMillis();
		// resetPageRank();

		long time4 = System.currentTimeMillis();
		computePageRank();
		long time5 = System.currentTimeMillis();
		// print top k based on pagerank
		List<Node> topSubtopicsbyPageRank = getKSubtopicsBasedOnPagerank();
		printTopNodesByPagerank(topSubtopicsbyPageRank);
		// printPageRank();

		time5 = System.currentTimeMillis();
		getSubtopics();

		long time6 = System.currentTimeMillis();
		List<String> summary = getTopKTweetsForEachSubtopicAsASummary();
		// printSummary(summary);
		long time7 = System.currentTimeMillis();
		subtopics.clear();
		affectedNodesByAdding.clear();
		affectedNodesByRemoving.clear();
		newNodes.clear();

		System.out.printf(">>>>>>>>>>>TIME FOR BUILDING ALIAS SAMPLER: %d\n", (time2 - time1));
		System.out.printf(">>>>>>>>>>>TIME FOR RANDOM WALK AND STORING SEGMENTS: %d\n", (time4 - time2));
		System.out.printf(">>>>>>>>>>>TIME FOR COMPUTING PAGERANK OF ALL NODES: %d\n", (time5 - time4));
		System.out.printf(">>>>>>>>>>>TIME FOR GETTING SUBTOPICS: %d\n", (time6 - time5));
		System.out.printf(">>>>>>>>>>>TIME FOR GENERATING SUMMARY: %d\n", (time7 - time6));

	}

	public void printTopNodesByPagerank(List<Node> nodeList) {
		System.out.println(".....................TOP NODES BY PAGERANK.............\n");
		for (int i = nodeList.size() - 1; i >= 0; i--) {
			System.out.printf("Pagerank: %f, %s\n", nodeList.get(i).getPageRank(), nodeList.get(i).getNodeName());
		}
		System.out.println(".......................................................\n");
	}

	public void resetPageRank() {
		Set<Node> nodesInGraph = graph.vertexSet();
		Iterator<Node> iter = nodesInGraph.iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			node.resetSegments();
			node.updatePageRank(0);
		}
	}

	public void printSummary(List<String> listOfTweets) {
		System.out.println("...................FINAL SUMMARY....................");
		for (int i = 0; i < listOfTweets.size(); i++) {
			System.out.println(listOfTweets.get(i));
		}
		System.out.println("....................................................");
	}

	public void printPageRank() {
		Iterator<Node> iter = wordNodeMap.values().iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			System.out.printf("%s: %f\n", node.getNodeName(), node.getPageRank());
		}
	}

	public void printRandomWalks() {
		for (int i = 0; i < randomWalks.size(); i++) {
			System.out.printf("%d. ", i);
			// for (int j = 0; j < randomWalks.get(i).size(); j++) {
			// System.out.printf(" %s ", randomWalks.get(i).get(j));
			// }
			System.out.println();
		}
	}

	public void testAlias() {
		String nodeName = "the/dt";
		Node corresNode = wordNodeMap.get(nodeName);

		HashMap<DefaultWeightedEdge, Double> edgeWeightMap = corresNode.getWeightsOfOutgoingNodes();
		for (Map.Entry<DefaultWeightedEdge, Double> entry : edgeWeightMap.entrySet()) {
			DefaultWeightedEdge edge = entry.getKey();
			System.out.println(graph.getEdgeSource(edge) + "-->" + graph.getEdgeTarget(edge) + ":" + entry.getValue());
		}
		for (int i = 0; i < 1000; i++) {
			DefaultWeightedEdge nextNode = corresNode.sampleOutgoingEdges();
			System.out.println(graph.getEdgeTarget(nextNode));
		}
		System.exit(-1);
	}

}
