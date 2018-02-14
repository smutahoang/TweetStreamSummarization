package l3s.tts.summary;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;

import l3s.tts.configure.Configure;
import l3s.tts.utils.KeyValue_Pair;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetPreprocessingUtils;

public class SummarizationModel {
	protected TweetPreprocessingUtils preprocessingUtils;
	protected SimpleGraph<Node, DefaultWeightedEdge> graph;
	protected HashMap<String, Node> wordNodeMap;
	protected List<Candidate> candidates;
	protected HashSet<Node> affectedNodesByAdding;
	protected HashSet<Node> affectedNodesByRemoving;
	protected HashSet<Node> newNodes;
	protected List<ArrayList<Node>> segments;
	protected Set<Node> subtopics;

	protected Random rand;

	public SummarizationModel() {
		// TODO Auto-generated constructor stub
		wordNodeMap = new HashMap<String, Node>();
		graph = new SimpleGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		preprocessingUtils = new TweetPreprocessingUtils();
		affectedNodesByAdding = new HashSet<Node>();
		affectedNodesByRemoving = new HashSet<Node>();
		newNodes = new HashSet<Node>();
		candidates = new ArrayList<Candidate>();

		segments = new ArrayList<ArrayList<Node>>();

		subtopics = new HashSet<Node>();
		rand = new Random();
	}

	public void getSubtopics() {
		HashSet<Node> nodeSet = new HashSet<Node>();
		nodeSet.addAll(wordNodeMap.values());
		Scanner scan = new Scanner(System.in);
		double utility = Double.MAX_VALUE;
		double sumOfUtility = 0;
		HashSet<Node> coveredSet = new HashSet<Node>();
		while (utility/sumOfUtility > Configure.MAGINAL_UTILITY) {
			Iterator<Node> iter = nodeSet.iterator();
			Node bestNode = null;
			double max = 0;
			// iterate all node that havent added into subtopic set to find a node with the highest coverage
			HashSet<Node> maxCoveredSetOfCurrNode = new HashSet<Node>();
			while (iter.hasNext()) {
				// find node with the highest coverage
				Node node = iter.next();
				double coverageScore = 0;
				List<Node> currSet = new ArrayList<Node>();
				currSet.add(node);
				
				// get expansion set of the current node
				HashSet<Node> expansionSetOfCurrNode = new HashSet<Node>();
				for (int i = 0; i < Configure.L_EXPANSION; i++) {
					// get ith-expansion set 
					Set<Node> expansionSet = new HashSet<Node>();
					for (Node nodeInCurrSet : currSet) {
						List<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>(
								graph.edgesOf(nodeInCurrSet));
						// get all neighbors
						for (int j = 0; j < edges.size(); j++) {
							Node n = graph.getEdgeSource(edges.get(j));
							if(n.equals(nodeInCurrSet))
								n = graph.getEdgeTarget(edges.get(j));
							if (!subtopics.contains(n)&&!coveredSet.contains(n)) {
								expansionSet.add(n);
								coverageScore += n.getPageRank();

							}

						}
					}
					currSet = new ArrayList<Node>(expansionSet);
					expansionSetOfCurrNode.addAll(expansionSet);
				}
				if (coverageScore > max) {
					max = coverageScore;
					bestNode = node;
					maxCoveredSetOfCurrNode = expansionSetOfCurrNode;
				}
			}
			if(utility < Configure.MAGINAL_UTILITY) break;
			utility = max;
			sumOfUtility+=utility;
			subtopics.add(bestNode);
			nodeSet.remove(bestNode);
			coveredSet.addAll(maxCoveredSetOfCurrNode);
			System.out.printf("Utility: %f, %s\n", utility, bestNode.getNodeName());

		}
	}

	public HashMap<Tweet, Double> getListOfTweetsCoveringSubtopics() {
		Iterator<Node> iter = subtopics.iterator();
		HashMap<Tweet, Double> importantTweets = new HashMap<Tweet, Double>();
		while (iter.hasNext()) {
			Node node = iter.next();
			for (Tweet t : node.getTweets()) {
				importantTweets.put(t, computeTweetScore(t));
			}
		}
		return importantTweets;
	}
	// get top k tweets with the highest score that cover one of the keywords
	public List<String> getTopKTweetsInSummary() {
		Iterator<Node> iter = subtopics.iterator();
		// importantTweets: save all tweets in the final summary
		HashMap<Tweet, Double> importantTweets = new HashMap<Tweet, Double>();
		HashSet<List<String>> tweets = new HashSet<List<String>>(); // avoid adding similar sentences in the final summary
		while (iter.hasNext()) {
			Node node = iter.next();
			for (Tweet t : node.getTweets()) {
				List<String> terms = t.getTerms(preprocessingUtils);
				if(tweets.add(terms))
					importantTweets.put(t, computeTweetScore(t));
			}
		}

		PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
		for (Map.Entry<Tweet, Double> tweet : importantTweets.entrySet()) {
			String text = tweet.getKey().getText();
			Double score = tweet.getValue();
			if (queue.size() < Configure.MAX_SUMMARIES) {
				queue.add(new KeyValue_Pair(text, score));
			} else {
				KeyValue_Pair head = queue.peek();

				if (head.getDoubleValue() < score) {
					queue.poll();
					queue.add(new KeyValue_Pair(text, score));
				}
			}
		}

		List<String> topTweets = new ArrayList<String>();
		while (!queue.isEmpty()) {
			topTweets.add(queue.poll().getStrKey());
		}
		return topTweets;

	}

	// get top k tweets for each subtopics
	public List<String> getTopKTweetsForEachSubtopicAsASummary() {
		Iterator<Node> iter = subtopics.iterator();
		List<String> topTweets = new ArrayList<String>();
		// importantTweets: save all tweets in the final summary
		
		HashSet<List<String>> tweets = new HashSet<List<String>>(); // avoid adding similar sentences in the final summary
		
		// for each node, iterate all tweets that contains the node
		while (iter.hasNext()) {
			Node node = iter.next();
			HashMap<Tweet, Double> importantTweets = new HashMap<Tweet, Double>();
			for (Tweet t : node.getTweets()) {
				List<String> terms = t.getTerms(preprocessingUtils);
				if(tweets.add(terms))
					importantTweets.put(t, computeTweetScore(t));
			}
			PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
			for (Map.Entry<Tweet, Double> tweet : importantTweets.entrySet()) {
				String text = tweet.getKey().getText();
				Double score = tweet.getValue();
				if (queue.size() < Configure.TWEETS_IN_EACH_SUBTOPIC) {
					queue.add(new KeyValue_Pair(text, score));
				} else {
					KeyValue_Pair head = queue.peek();

					if (head.getDoubleValue() < score) {
						queue.poll();
						queue.add(new KeyValue_Pair(text, score));
					}
				}
			}
			while (!queue.isEmpty()) {
				topTweets.add(queue.poll().getStrKey());
				
			}
		}

		

		
		
		return topTweets;

	}
	public double computeTweetScore(Tweet tweet) {
		List<String> terms = tweet.getTerms(preprocessingUtils);
		double score = 0;
		for (int i = 0; i < terms.size(); i++) {
			Node node = wordNodeMap.get(terms.get(i));
			score += node.getPageRank();
		}
		return score;
	}

	public void addNewTweet(Tweet tweet) {
		
		List<String> terms = tweet.getTerms(preprocessingUtils);
		/*if(terms.get(0).startsWith("@"))
			System.out.printf("Tweet: %s\n", tweet.getText());*/
		Node currNode = null;
		Node nextNode = null;
		if (wordNodeMap.containsKey(terms.get(0))) {
			currNode = wordNodeMap.get(terms.get(0));
			affectedNodesByAdding.add(currNode);
		} else {
			currNode = new Node(rand);
			currNode.setNodeName(terms.get(0));
			wordNodeMap.put(terms.get(0), currNode);
			graph.addVertex(currNode);
			newNodes.add(currNode);
			//System.out.printf("\n>>>new node: %s\n", currNode);
		}
		for (int i = 0; i < terms.size(); i++) {

			currNode = wordNodeMap.get(terms.get(i));
			currNode.addTweet(tweet);

			for (int k = 1; k < Configure.WINDOW_SIZE; k++) {
				if (i + k >= terms.size())
					break;
				if (wordNodeMap.containsKey(terms.get(i + k))) {
					nextNode = wordNodeMap.get(terms.get(i + k));
					affectedNodesByAdding.add(nextNode);
				} else {
					nextNode = new Node(rand);
					nextNode.setNodeName(terms.get(i + k));
					wordNodeMap.put(terms.get(i + k), nextNode);
					graph.addVertex(nextNode);
			//		System.out.printf("\n>>>new node: %s\n", nextNode);
					newNodes.add(nextNode);
				}
				DefaultWeightedEdge edge = graph.getEdge(currNode, nextNode);
				// System.out.println(currNode + "\t" + nextNode);

				if (edge == null) {
					if (currNode != nextNode) {
						DefaultWeightedEdge addedEdge = graph.addEdge(currNode, nextNode);
						graph.setEdgeWeight(addedEdge, tweet.getWeight());
						currNode.setWeightOfOutgoingNodes(addedEdge, tweet.getWeight());
					}
				} else {
					double weight = graph.getEdgeWeight(edge) + tweet.getWeight();
					graph.setEdgeWeight(edge, weight);
					currNode.setWeightOfOutgoingNodes(edge, weight);
				}
			}
		}

	}

	public void saveRandomWalkSegments() {
		Set<Node> nodeSet = graph.vertexSet();
		Iterator<Node> iter = nodeSet.iterator();
		while (iter.hasNext()) {
			Node node = iter.next();

			int i = 0;
			while (i < Configure.NUMBER_OF_RANDOM_WALK_AT_EACH_NODE) {

				ArrayList<Node> seg = new ArrayList<Node>();
				seg.add(node);
				randomWalk(seg, node);

				i++;
			}
		}
	}

	public void randomWalk(ArrayList<Node> seg, Node node) {
		Node currNode = node;

		int length = seg.size();
		while (length < Configure.RANDOM_WALK_LENGTH) {
			double x = rand.nextDouble();
			if (x < Configure.DAMPING_FACTOR || currNode.getWeightsOfOutgoingNodes().size() == 0) {
				break;
			}

			DefaultWeightedEdge outgoingEdge = currNode.sampleOutgoingEdges();
			Node nextNode = graph.getEdgeTarget(outgoingEdge);
			seg.add(nextNode);
			currNode = nextNode;
		}
		segments.add(seg);
	}

	public void computePageRank() {
		for (int i = 0; i < segments.size(); i++) {
			for (int j = 0; j < segments.get(i).size(); j++) {
				Node node = segments.get(i).get(j);
				if (segments.get(i).indexOf(node) == j) // only increase in the first appearing of the node in the
														// segment
					node.increaseSegments();
			}
		}

		// iter all nodes in the graph and compute PageRank
		Iterator<Node> iter = wordNodeMap.values().iterator();

		while (iter.hasNext()) {
			Node node = iter.next();

			double pageRankScore = node.getSegments() / (wordNodeMap.values().size()
					* Configure.NUMBER_OF_RANDOM_WALK_AT_EACH_NODE / Configure.DAMPING_FACTOR);
			//System.out.println(node + "\t" + node.getSegments() + "\t"
			//		+ wordNodeMap.size() * Configure.NUMBER_OF_RANDOM_WALK_AT_EACH_NODE);
			node.updatePageRank(pageRankScore);
		}
	}

	public void buildAliasSampler() {
		Iterator<Node> iter = affectedNodesByAdding.iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			node.updateAliasSampler();
		}
		iter = affectedNodesByRemoving.iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			node.updateAliasSampler();
		}
		iter = newNodes.iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			node.updateAliasSampler();
		}
	}

	public double[] getWeightOfOutgoingEdges(ArrayList<DefaultWeightedEdge> edges) {

		double[] distribution = new double[edges.size()];
		double sum = 0;
		int i = 0;
		for (DefaultWeightedEdge edge : edges) {
			distribution[i] = graph.getEdgeWeight(edge);
			sum += distribution[i];
			i++;
		}
		for (i = 0; i < distribution.length; i++) {
			distribution[i] = distribution[i] / sum;
		}
		return distribution;
	}

	public void printCandidates(Formatter format) {
		for (int i = 0; i < candidates.size(); i++) {
			format.format("%d. %s\n", i + 1, candidates.get(i));
		}

	}

	public void printCandidates() {
		int count = 0;
		for (int i = 0; i < candidates.size(); i++) {
			if (!candidates.get(i).getIsDiscard()) {
				System.out.printf("%d. %f, %s\n", (i + 1), candidates.get(i).getScore(), candidates.get(i));
				count++;
			}
		}
		System.out.printf("--> %d candidates\n", count);
	}

	public void printGraph() {
		Set<Node> vertexSet = graph.vertexSet();
		Iterator<Node> iter = vertexSet.iterator();
		int i = 0;
		while (iter.hasNext()) {
			Node n = iter.next();
			Set<DefaultWeightedEdge> edgesOfNode = graph.edgesOf(n);
			for (DefaultWeightedEdge edge : edgesOfNode) {
				Node sourceNode = graph.getEdgeSource(edge);
				Node targetNode = graph.getEdgeTarget(edge);
				if (targetNode.getNodeName().equals(n.getNodeName()))
					continue;
				double weight = graph.getEdgeWeight(edge);
				System.out.printf("%d. %s --> %s: %.1f\n", i, sourceNode.getNodeName(), targetNode.getNodeName(),
						weight);
				i++;
			}
		}

	}

}
