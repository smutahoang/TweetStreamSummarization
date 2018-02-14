package l3s.tts.summary;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
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
		updateRandomWalkSegments();
		
		long time4 = System.currentTimeMillis();
		resetPageRank();
		
		long time5 = System.currentTimeMillis();
		computePageRank();
		
		long time6 = System.currentTimeMillis();
		getSubtopics();
		
		long time7 = System.currentTimeMillis();
		List<String> summary = getTopKTweetsForEachSubtopicAsASummary();
		printSummary(summary);
		long time8 = System.currentTimeMillis();
		
		subtopics.clear();
		affectedNodesByAdding.clear();
		affectedNodesByRemoving.clear();
		newNodes.clear();
		
		System.out.printf(">>>>>>>>>>>TIME FOR REMOVING OLDEST TWEETS: %d\n", (time2 - time1));
		System.out.printf(">>>>>>>>>>>TIME FOR BUILDING ALIAS SAMPLER: %d\n", (time3 - time2));
		System.out.printf(">>>>>>>>>>>TIME FOR UPDATE RANDOM WALK: %d\n", (time4 - time3));
		System.out.printf(">>>>>>>>>>>TIME FOR RESET PAGERANK: %d\n", (time5 - time4) );
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
				
				affectedNodesByRemoving.add(source);
			//	System.out.println(terms.get(j) +"\t" + source.getNodeName());
			
				source.removeTweet(tweet);
				for (int k = 1; k < Configure.WINDOW_SIZE; k++) {
					if (j + k < terms.size()) {
						Node target = wordNodeMap.get(terms.get(k));
						DefaultWeightedEdge edge = graph.getEdge(source, target);
						double weight = graph.getEdgeWeight(edge);
						if (weight == 1) {
							graph.removeEdge(edge);
						} else {
							graph.setEdgeWeight(edge, weight - tweet.getWeight());
						}

					}
				}
				if (graph.edgesOf(source).size() == 0) {
					graph.removeVertex(source);
					wordNodeMap.remove(terms.get(j));
					numberOfRemovedNodes++;
				}
			}
		}
		/*System.out.printf("\n.................NUMBER OF NODES REMOVED: %d\n", numberOfRemovedNodes);
		System.out.printf("\n.................NUMBER OF AFFECTED NODES BY REMOVING: %d\n", affectedNodesByRemoving.size());*/
	}

	public void updateRandomWalkSegments() {
		for (int i = 0; i < segments.size(); i++) {
			ArrayList<Node> seg = segments.get(i);
			ArrayList<Node> newSeg = new ArrayList<Node>();
			for (int j = 0; j < seg.size(); j++) {
				newSeg.add(seg.get(j));
				if (affectedNodesByAdding.contains(seg.get(j)) || affectedNodesByRemoving.contains(seg.get(j))) {
					if (graph.containsVertex(seg.get(j))) {
						randomWalk(newSeg, seg.get(j));
						segments.remove(i);
						break;
					} else {
						if(j == 0) segments.remove(i);
						else {
							
							randomWalk(newSeg, newSeg.get(j-1));
							segments.remove(i);
						}
						break;
					}
				}

			}
		}
		Iterator<Node> iter = newNodes.iterator();
		while(iter.hasNext()) {
			
			Node node = iter.next();
			for(int i = 0; i<Configure.NUMBER_OF_RANDOM_WALK_AT_EACH_NODE; i++) {
				ArrayList<Node> newSeg = new ArrayList<Node>();
				newSeg.add(node);
				randomWalk(newSeg, node);
			}
		}
		
	}

	public void generateSummary() {
		long time1 = System.currentTimeMillis();
		buildAliasSampler();
		
		long time2 = System.currentTimeMillis();
		saveRandomWalkSegments();
		//printSegments();
		
		//long time3 = System.currentTimeMillis();
		//resetPageRank();
		
		long time4 = System.currentTimeMillis();
		computePageRank();
		//printPageRank();
		
		long time5= System.currentTimeMillis();
		getSubtopics();
		
		long time6 = System.currentTimeMillis();
		List<String> summary = getTopKTweetsForEachSubtopicAsASummary();
		printSummary(summary);
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

	public void resetPageRank() {
		Set<Node> nodesInGraph = graph.vertexSet();
		Iterator<Node> iter = nodesInGraph.iterator();
		while(iter.hasNext()) {
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

	public void printSegments() {
		for (int i = 0; i < segments.size(); i++) {
			System.out.printf("%d. ", i);
			for (int j = 0; j < segments.get(i).size(); j++) {
				System.out.printf(" %s ", segments.get(i).get(j));
			}
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
