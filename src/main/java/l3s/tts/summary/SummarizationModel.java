package l3s.tts.summary;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.w3c.dom.NodeList;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
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
	protected HashMap<Integer, RandomWalk> randomWalks;
	protected int walkId;
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

		randomWalks = new HashMap<Integer, RandomWalk>();
		walkId = 0;

		subtopics = new HashSet<Node>();
		rand = new Random();
	}

	public List<Node> getKSubtopicsBasedOnPagerank() {
		List<Node> topNodes = new ArrayList<Node>();

		Iterator<Node> iter = graph.vertexSet().iterator();
		PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
		while (iter.hasNext()) {
			Node node = iter.next();
			String text = node.getNodeName();
			Double score = node.getPageRank();
			if (queue.size() < Configure.TOP_K) {
				queue.add(new KeyValue_Pair(node.getNodeName(), score));
			} else {
				KeyValue_Pair head = queue.peek();

				if (head.getDoubleValue() < score) {
					queue.poll();
					queue.add(new KeyValue_Pair(text, score));
				}
			}
		}
		while (!queue.isEmpty()) {
			String text = queue.poll().getStrKey();
			topNodes.add(wordNodeMap.get(text));
		}
		return topNodes;
	}

	public void getSubtopics() {

		HashSet<Node> nodeSet = new HashSet<Node>();
		nodeSet.addAll(wordNodeMap.values()); // set of nodes that havent added
												// into sub-topic set
		// Scanner scan = new Scanner(System.in);
		double utility = Double.MAX_VALUE;
		double sumOfUtility = 0;
		HashSet<Node> coveredSet = new HashSet<Node>();
		while (true) {
			Iterator<Node> iter = nodeSet.iterator();
			Node bestNode = null;
			double max = 0;
			// iterate all node that havent added into subtopic set to find a
			// node with the highest coverage
			HashSet<Node> coveredSetbyBestNode = new HashSet<Node>();
			while (iter.hasNext()) {
				// find node with the highest coverage
				Node node = iter.next();
				double coverageScore = 0;

				HashMap<Node, Integer> coveredNodes = new HashMap<Node, Integer>();
				coveredNodes.put(node, 0);

				Queue<Node> queue = new LinkedList<Node>();
				queue.add(node);
				coverageScore += node.getPageRank();

				while (queue.size() > 0) {
					Node currentNode = queue.remove();
					int currentLevel = coveredNodes.get(currentNode);
					if (currentLevel == Configure.L_EXPANSION) {
						continue;
					}
					// get all neighbors of the current node
					List<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>(graph.edgesOf(currentNode));
					for (int j = 0; j < edges.size(); j++) {
						Node n = graph.getEdgeSource(edges.get(j));
						if (n == currentNode) {
							n = graph.getEdgeTarget(edges.get(j));
						}
						if (subtopics.contains(n)) {
							continue;
						}
						if (coveredSet.contains(n)) {
							continue;
						}
						if (coveredNodes.containsKey(n)) {
							continue;
						} else {
							coveredNodes.put(n, currentLevel + 1);
							queue.add(n);
							coverageScore += n.getPageRank();
						}

					}
				}
				if (coverageScore > max) {
					max = coverageScore;
					bestNode = node;
					coveredSetbyBestNode.addAll(coveredNodes.keySet());
				}
			}
			utility = max;

			if (utility / sumOfUtility < Configure.MAGINAL_UTILITY)
				break;
			sumOfUtility += utility;
			subtopics.add(bestNode);

			nodeSet.remove(bestNode);

			System.out.printf("Utility: %f, %s\n", utility, bestNode.getNodeName());

			Set<DefaultWeightedEdge> edges = graph.edgesOf(bestNode);
			Iterator<DefaultWeightedEdge> iter1 = edges.iterator();
			System.out.printf("CoveredSet: %d\n", coveredSet.size());
			/*
			 * while(iter1.hasNext()) { DefaultWeightedEdge edge = iter1.next(); Node node =
			 * graph.getEdgeSource(edge); if(node.equals(bestNode)) node =
			 * graph.getEdgeTarget(edge);
			 * 
			 * //if(!coveredSet.contains(node)) System.out.printf("%s, ",
			 * node.getNodeName()); }
			 */

			coveredSet.addAll(coveredSetbyBestNode);
			coveredSet.add(bestNode);
		}
	}

	public void efficientGetSubtopics() {
		// base on ALGORITHM 2 in "Diversified Recommendation on Graphs:
		// Pitfalls, Measures, and Algorithms"
		HashMap<Node, Set<Node>> nodeNeighborMap = new HashMap<Node, Set<Node>>();

		Set<Node> coveredNodes = new HashSet<Node>();

		Set<Node> allNodes = new HashSet<Node>(wordNodeMap.values());
		Iterator<Node> iter = allNodes.iterator();
		Node bestNode = null;

		// compute initial utility of each node and get the node with highest utility
		while (iter.hasNext()) {
			Node node = iter.next();
			// save all neighbors of the current node
			Set<Node> neighbors = new HashSet<Node>();
			double utility = getNeighborsOfANode(node, neighbors);
			node.setUtility(utility);

			nodeNeighborMap.put(node, neighbors);
			if (bestNode == null || (bestNode != null && bestNode.getUtility() < node.getUtility()))
				bestNode = node;
		}

		double sumOfUtility = 0;
		coveredNodes.add(bestNode);
		while (true) {
			double utility = bestNode.getUtility();
			sumOfUtility += utility;
			if (utility / sumOfUtility < Configure.MAGINAL_UTILITY)
				break;

			subtopics.add(bestNode);
			allNodes.remove(bestNode);
			coveredNodes.add(bestNode);
			System.out.printf("Utilitly: %f, %s\n", bestNode.getUtility(), bestNode.getNodeName());

			Iterator<Node> neighborsOfBestNode = nodeNeighborMap.get(bestNode).iterator();

			// update utility
			while (neighborsOfBestNode.hasNext()) {
				Node currNode = neighborsOfBestNode.next();
				if (!coveredNodes.contains(currNode)) {
					Iterator<Node> neighborsOfCurrNode = nodeNeighborMap.get(currNode).iterator();
					while (neighborsOfCurrNode.hasNext()) {
						Node node = neighborsOfCurrNode.next();
						node.setUtility(node.getUtility() - currNode.getPageRank() );
						

					}
					coveredNodes.add(currNode);
				}
			}

			// get the best node
			Iterator<Node> iterAllCurrentNodes = allNodes.iterator();
			bestNode = null;
			while (iterAllCurrentNodes.hasNext()) {
				Node node = iterAllCurrentNodes.next();
				if (bestNode == null || (bestNode != null && bestNode.getUtility() < node.getUtility()))
					bestNode = node;
			}

		}
	}

	public double getNeighborsOfANode(Node node, Set<Node> neighbors) {
		HashMap<Node, Integer> neighborsLevelMap = new HashMap<Node, Integer>();
		Queue<Node> queue = new LinkedList<Node>();

		neighborsLevelMap.put(node, 0);
		queue.add(node);

		double utility = 0;
		while (queue.size() > 0) {
			Node head = queue.poll();
			int currentLevel = neighborsLevelMap.get(head);
			if (currentLevel == Configure.L_EXPANSION)
				continue;
			// get all neighbors
			ArrayList<DefaultWeightedEdge> outgoingEdges = new ArrayList<DefaultWeightedEdge>(graph.edgesOf(head));
			for (int i = 0; i < outgoingEdges.size(); i++) {
				Node outgoingNode = graph.getEdgeSource(outgoingEdges.get(i));
				if (head.equals(outgoingNode))
					outgoingNode = graph.getEdgeTarget(outgoingEdges.get(i));
				if (neighborsLevelMap.containsKey(outgoingNode))
					continue;
				neighborsLevelMap.put(outgoingNode, currentLevel + 1);
				utility += outgoingNode.getPageRank();
			}
		}
		// node.setUtility(utility);
		neighbors = neighborsLevelMap.keySet();
		// neighbors.remove(node);
		return utility;
	}

	// get top k tweets for each subtopics
	public List<String> getTopKTweetsForEachSubtopicAsASummary(Set<Node> subtopics) {
		Iterator<Node> iter = subtopics.iterator();
		List<String> topTweets = new ArrayList<String>();
		// topTweets: save all tweets in the final summary

		HashMap<String, Tweet> textTweetMap = new HashMap<String, Tweet>();
		HashMap<String, Tweet> topTweetMap = new HashMap<String, Tweet>();

		// HashSet<List<String>> topTweetSet = new HashSet<List<String>>();

		// for each node, iterate all tweets that contains the node
		while (iter.hasNext()) {
			Node node = iter.next();

			System.out.printf("\n>>>>>>>>>>>>>>>%s\n", node.getNodeName());

			HashMap<Tweet, Double> importantTweets = new HashMap<Tweet, Double>(); // get
																					// tweets
																					// containing
																					// the
																					// node
			for (Tweet t : node.getTweets()) {
				HashSet<String> terms = new HashSet<String>();

				terms.addAll(t.getTerms(preprocessingUtils));

				// if(!isRepresentativeTweetOfTheTopic(terms, node))
				// continue;

				// if(topTweetSet.add(t.getTerms(preprocessingUtils))) {
				if (shouldAddANewTweet(t, new HashSet<Tweet>(topTweetMap.values()))) {
					importantTweets.put(t, computeTweetScore(terms));
					textTweetMap.put(t.getText(), t);
				}
			}

			PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
			HashMap<String, Tweet> queueTweetMap = new HashMap<String, Tweet>();
			for (Map.Entry<Tweet, Double> tweet : importantTweets.entrySet()) {
				String text = tweet.getKey().getText();
				Double score = tweet.getValue();
				if (!shouldAddANewTweet(tweet.getKey(), new HashSet<Tweet>(queueTweetMap.values())))
					continue;
				if (queue.size() < Configure.TWEETS_IN_EACH_SUBTOPIC) {
					queue.add(new KeyValue_Pair(text, score));

					queueTweetMap.put(text, tweet.getKey());
				} else {
					KeyValue_Pair head = queue.peek();

					if (head.getDoubleValue() < score) {
						KeyValue_Pair removedTweet = queue.poll();
						queue.add(new KeyValue_Pair(text, score));

						queueTweetMap.remove(removedTweet.getStrKey());
						queueTweetMap.put(text, tweet.getKey());
					}
				}
			}
			while (!queue.isEmpty()) {
				String text = queue.poll().getStrKey();
				topTweets.add(text);
				topTweetMap.put(text, textTweetMap.get(text));
				System.out.println(text);
			}
		}

		return topTweets;

	}

	private boolean isRepresentativeTweetOfTheTopic(HashSet<String> terms, Node node) {
		Iterator<String> iter = terms.iterator();
		while (iter.hasNext()) {
			Node currNode = wordNodeMap.get(iter.next());
			if (currNode.getPageRank() > node.getPageRank())
				return false;

		}
		return true;
	}

	// check if we can add t into the set of existed tweets
	private boolean shouldAddANewTweet(Tweet newTweet, Set<Tweet> setOfExistedTweets) {
		List<String> termsOfNewTweet = newTweet.getTerms(preprocessingUtils);
		Iterator<Tweet> iter = setOfExistedTweets.iterator();
		while (iter.hasNext()) {
			List<String> termsOfCurrentTweet = iter.next().getTerms(preprocessingUtils);
			if (getJaccardScore(termsOfNewTweet, termsOfCurrentTweet) > Configure.JACCARD_THRESOLD) {
				return false;
			}
		}
		return true;
	}

	private double getJaccardScore(List<String> listOfTerms1, List<String> listOfTerms2) {
		HashSet<String> union = new HashSet<String>();

		union.addAll(listOfTerms1);
		union.addAll(listOfTerms2);

		return (union.size() > 0) ? (double) (listOfTerms1.size() + listOfTerms2.size() - union.size()) / union.size()
				: 0;
	}

	public double computeTweetScore(HashSet<String> terms) {
		// List<String> terms = tweet.getTerms(preprocessingUtils);
		double score = 0;
		Iterator<String> iter = terms.iterator();
		while (iter.hasNext()) {
			String nextString = iter.next();
			if (!wordNodeMap.containsKey(nextString)) {
				System.err.println("err!! Doesnt contain node in the graph: " + nextString);
			}
			Node node = wordNodeMap.get(nextString);
			score += node.getPageRank();
		}
		return score;
	}

	public void addNewTweet(Tweet tweet) {

		List<String> terms = tweet.getTerms(preprocessingUtils);
		/*
		 * if(terms.get(0).startsWith("@")) System.out.printf("Tweet: %s\n",
		 * tweet.getText());
		 */
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
			// System.out.printf("\n>>>new node: %s\n", currNode);
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
					// System.out.printf("\n>>>new node: %s\n", nextNode);
					newNodes.add(nextNode);
				}
				DefaultWeightedEdge edge = graph.getEdge(currNode, nextNode);
				// System.out.println(currNode + "\t" + nextNode);

				if (edge == null) {
					if (currNode != nextNode) { // should we accept a node
												// pointing to itself
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

	public void sampleAllWalks() {
		Set<Node> nodeSet = graph.vertexSet();
		Iterator<Node> iter = nodeSet.iterator();
		while (iter.hasNext()) {
			Node node = iter.next();
			for (int i = 0; i < Configure.NUMBER_OF_RANDOM_WALK_AT_EACH_NODE; i++) {
				RandomWalk walk = new RandomWalk(node);
				node.addVisit(walkId);
				keepWalking(walk, walkId, node);
				randomWalks.put(walkId, walk);
				walkId++;
			}
		}
	}

	public void keepWalking(RandomWalk walk, int w, Node node) {
		Node currNode = node;
		// int length = ;
		while (walk.length() < Configure.RANDOM_WALK_LENGTH) {
			double x = rand.nextDouble();
			if (x < Configure.DAMPING_FACTOR || currNode.getWeightsOfOutgoingNodes().size() == 0) {
				break;
			}

			DefaultWeightedEdge outgoingEdge = currNode.sampleOutgoingEdges();
			Node nextNode = graph.getEdgeTarget(outgoingEdge);
			if (nextNode.equals(currNode)) {
				nextNode = graph.getEdgeSource(outgoingEdge);
			}
			nextNode.addVisit(w);
			walk.addVisitedNode(nextNode);
			currNode = nextNode;
		}
		// return walk;
	}

	public void computePageRank() {
		int nTotalVisits = 0;
		Iterator<Node> iter = graph.vertexSet().iterator();
		while (iter.hasNext()) {
			nTotalVisits += iter.next().getNumberOfVisits();
		}

		// iter all nodes in the graph and compute PageRank
		iter = graph.vertexSet().iterator();

		while (iter.hasNext()) {
			Node node = iter.next();

			double pageRankScore = ((double) node.getNumberOfVisits()) / nTotalVisits;
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
			if (affectedNodesByAdding.contains(node)) {
				continue;
			}
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
