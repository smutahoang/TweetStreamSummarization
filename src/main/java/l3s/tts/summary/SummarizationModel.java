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
	protected List<Node> subtopics;

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

		subtopics = new ArrayList<Node>();
		rand = new Random();
	}

	public List<Node> getKSubtopicsBasedOnPagerank(int k) {
		List<Node> topNodes = new ArrayList<Node>();

		PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
		for (Node node : graph.vertexSet()) {

			String text = node.getNodeName();
			Double score = node.getPageRank();
			if (queue.size() < k) {
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
		double utility = 0;
		double sumOfUtility = 0;
		HashSet<Node> coveredSet = new HashSet<Node>();
		while (true) {

			Node bestNode = null;
			double max = 0;
			// iterate all node that havent added into subtopic set to find a
			// node with the highest coverage
			HashSet<Node> coveredSetbyBestNode = new HashSet<Node>();
			for (Node node : nodeSet) {
				// find node with the highest coverage

				double coverageScore = 0;

				HashMap<Node, Integer> coveredNodes = new HashMap<Node, Integer>();
				coveredNodes.put(node, 0);

				Queue<Node> queue = new LinkedList<Node>();
				queue.add(node);
				// coverageScore += node.getPageRank();

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
					coveredSetbyBestNode.addAll(coveredNodes.keySet()); // -->
																		// bug
					coveredSetbyBestNode = new HashSet<Node>(coveredNodes.keySet());

				}
			}
			utility = max;

			if (utility / sumOfUtility < Configure.MAGINAL_UTILITY)
				break;
			sumOfUtility += utility;
			subtopics.add(bestNode);

			nodeSet.remove(bestNode);

			System.out.printf("Utility: %f, %s\n", utility, bestNode.getNodeName());

			// Set<DefaultWeightedEdge> edges = graph.edgesOf(bestNode);
			// Iterator<DefaultWeightedEdge> iter1 = edges.iterator();
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

	public void checkIntersection() {
		// base on ALGORITHM 2 in "Diversified Recommendation on Graphs:
		// Pitfalls, Measures, and Algorithms"
		HashMap<Node, Set<Node>> nodeNeighborMap = new HashMap<Node, Set<Node>>();
		HashMap<Node, Boolean> nodeStatusMap = new HashMap<Node, Boolean>();

		Set<Tweet> tweetsOfTopK = new HashSet<Tweet>();
		Set<Tweet> remainingTweets = new HashSet<Tweet>();
		Set<Node> allNodes = new HashSet<Node>(wordNodeMap.values());

		// get top k by pagerank and remove
		// List<Node> topKPagerank =
		// getKSubtopicsBasedOnPagerank(Configure.NUMBER_OF_IGNORED_TOPICS);

		Node bestNode = null;

		// compute initial utility of each node and get the node with highest
		// utility
		for (Node node : allNodes) {

			// save all neighbors of the current node
			Set<Node> neighbors = getNeighborsOfAndUtilityANode(node);

			nodeNeighborMap.put(node, neighbors);

			nodeStatusMap.put(node, false);

			if (bestNode == null || (bestNode != null && bestNode.getUtility() < node.getUtility())) {
				bestNode = node;
			}

		}
		Set<Node> removedNode = new HashSet<Node>();
		double sumOfUtility = 0;
		int numberOfIgnoredNodes = 1;
		int count = 0;
		while (true) {
			allNodes.remove(bestNode);

			System.out.printf("removed: %s, %d (#tweets)\n", bestNode, bestNode.getTweets().size());
			if (numberOfIgnoredNodes > Configure.NUMBER_OF_IGNORED_TOPICS) {
				// if(count<4) {
				remainingTweets.addAll(bestNode.getTweets());
				count++;
				// }
				double utility = bestNode.getUtility();
				sumOfUtility += utility;
				if (utility / sumOfUtility < Configure.MAGINAL_UTILITY)
					break;

				subtopics.add(bestNode);

				System.out.printf("Utilitly: %f, %s\n", bestNode.getUtility(), bestNode.getNodeName());

				// System.out.println(nodeNeighborMap.get(bestNode).size());

				// update utility
				for (Node currNode : nodeNeighborMap.get(bestNode)) {
					if (currNode == bestNode) {
						continue;
					}
					if (nodeStatusMap.get(bestNode) == false)
						currNode.setUtility(currNode.getUtility() - bestNode.getPageRank() - currNode.getPageRank());

					if (nodeStatusMap.get(currNode) == false) {
						for (Node node : nodeNeighborMap.get(currNode)) {
							if (node == currNode) {
								continue;
							}
							node.setUtility(node.getUtility() - currNode.getPageRank());
						}
						nodeStatusMap.put(currNode, true);
					}
				}
				nodeStatusMap.put(bestNode, true);
			} else {
				tweetsOfTopK.addAll(bestNode.getTweets());
				removedNode.add(bestNode);
			}
			numberOfIgnoredNodes++;
			// get the best node
			bestNode = null;
			for (Node node : allNodes) {

				if (bestNode == null || (bestNode != null && bestNode.getUtility() < node.getUtility()))
					bestNode = node;
			}
		}
		System.out.println(".................................................");
		System.out.printf("Top k: %d\n", tweetsOfTopK.size());
		System.out.printf("4 final terms: %d\n", remainingTweets.size());
		Set<Tweet> union = new HashSet<Tweet>();
		union.addAll(tweetsOfTopK);
		union.addAll(remainingTweets);
		int intersection = (tweetsOfTopK.size() + remainingTweets.size() - union.size());
		System.out.printf("union: %d, intersection: %d, %f, %f\n", union.size(), intersection,
				(double) intersection / tweetsOfTopK.size(), (double) intersection / remainingTweets.size());
		System.out.println(".................................................");

		subtopics.addAll(removedNode);
		// ==> complexity: O(n*d*d+ k*n+ k*d*d)
		// ==> old complexity: O(k*n*d*d)
	}

	public void efficientGetSubtopics() {
		// base on ALGORITHM 2 in "Diversified Recommendation on Graphs:
		// Pitfalls, Measures, and Algorithms"
		HashMap<Node, Set<Node>> nodeNeighborMap = new HashMap<Node, Set<Node>>();
		HashMap<Node, Boolean> nodeStatusMap = new HashMap<Node, Boolean>();

		Set<Node> allNodes = new HashSet<Node>(wordNodeMap.values());

		// get top k by pagerank and remove
		// List<Node> topKPagerank =
		// getKSubtopicsBasedOnPagerank(Configure.NUMBER_OF_IGNORED_TOPICS);

		Node bestNode = null;

		// compute initial utility of each node and get the node with highest
		// utility
		for (Node node : allNodes) {

			// save all neighbors of the current node
			Set<Node> neighbors = getNeighborsOfAndUtilityANode(node);

			nodeNeighborMap.put(node, neighbors);

			nodeStatusMap.put(node, false);

			if (bestNode == null || (bestNode != null && bestNode.getUtility() < node.getUtility())) {
				bestNode = node;
			}

		}

		double sumOfUtility = 0;
		int numberOfIgnoredNodes = 1;
		while (true) {
			allNodes.remove(bestNode);
			System.out.println("removed: " + bestNode);
			if (numberOfIgnoredNodes > Configure.NUMBER_OF_IGNORED_TOPICS) {
				double utility = bestNode.getUtility();
				sumOfUtility += utility;
				if (utility / sumOfUtility < Configure.MAGINAL_UTILITY)
					break;

				subtopics.add(bestNode);

				System.out.printf("Utilitly: %f, %s\n", bestNode.getUtility(), bestNode.getNodeName());

				System.out.println(nodeNeighborMap.get(bestNode).size());

				// update utility
				for (Node currNode : nodeNeighborMap.get(bestNode)) {
					if (currNode == bestNode) {
						continue;
					}
					if (nodeStatusMap.get(bestNode) == false)
						currNode.setUtility(currNode.getUtility() - bestNode.getPageRank() - currNode.getPageRank());

					if (nodeStatusMap.get(currNode) == false) {
						for (Node node : nodeNeighborMap.get(currNode)) {
							if (node == currNode) {
								continue;
							}
							node.setUtility(node.getUtility() - currNode.getPageRank());
						}
						nodeStatusMap.put(currNode, true);
					}
				}
				nodeStatusMap.put(bestNode, true);

			}
			numberOfIgnoredNodes++;
			// get the best node
			bestNode = null;
			for (Node node : allNodes) {

				if (bestNode == null || (bestNode != null && bestNode.getUtility() < node.getUtility()))
					bestNode = node;
			}
		}

		// ==> complexity: O(n*d*d+ k*n+ k*d*d)
		// ==> old complexity: O(k*n*d*d)
	}

	public Set<Node> getNeighborsOfAndUtilityANode(Node node) {
		HashMap<Node, Integer> coveredNodes = new HashMap<Node, Integer>();
		coveredNodes.put(node, 0);
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(node);
		double utility = node.getPageRank();

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

				if (coveredNodes.containsKey(n)) {
					continue;
				} else {
					coveredNodes.put(n, currentLevel + 1);
					queue.add(n);
					utility += n.getPageRank();
				}

			}
		}

		node.setUtility(utility);
		Set<Node> result = coveredNodes.keySet();
		// result.remove(node);
		return result;
	}

	public List<String> getTopKDiversifiedTweetsByReducingImportantScore(List<Node> subtopics) {
		List<String> topTweets = new ArrayList<String>();

		HashMap<String, Tweet> textTweetMap = new HashMap<String, Tweet>();
		HashMap<String, Tweet> topTweetMap = new HashMap<String, Tweet>();

		// for each subtopic
		for (Node node : subtopics) {

			System.out.printf("\n>>>>>>>>>>>>>>%s\n", node.getNodeName());

			HashMap<Tweet, Double> tweetPagerankMap = new HashMap<Tweet, Double>();
			for (Tweet t : node.getTweets()) {
				HashSet<String> terms = new HashSet<String>();
				terms.addAll(t.getTerms(preprocessingUtils));
				tweetPagerankMap.put(t, computeTweetScore(terms));
				// textTweetMap.put(t.getText(), t);
			}

			PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
			HashMap<String, Tweet> queueTweetMap = new HashMap<String, Tweet>();
			for (Map.Entry<Tweet, Double> tweet : tweetPagerankMap.entrySet()) {
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
				reduceScoreOfTerms(queueTweetMap.get(text));
				System.out.println(text);
			}
		}

		return topTweets;
	}

	private void reduceScoreOfTerms(Tweet tweet) {
		List<String> terms = tweet.getTerms(preprocessingUtils);
		for (int i = 0; i < terms.size(); i++) {
			Node node = wordNodeMap.get(terms.get(i));
			double pagerank = node.getPageRank();
			// node.updatePageRank(pagerank * pagerank);
			node.updatePageRank(pagerank * 1);
		}
	}

	public void checkOverlapping() {

		System.out.println(
				"Node: #tweets| coveredSet: #tweets| intersection| uinion| intersec/node| (#tweetOfNode - intersec)/union");
		System.out.println("------------------------------------------------------------------------------------");
		Set<Tweet> union = new HashSet<Tweet>();
		StringBuffer buffer = new StringBuffer("{");
		for (int i = 0; i < subtopics.size(); i++) {

			buffer.append(subtopics.get(i).getNodeName());
			buffer.append(", ");
			int beforeAdding = union.size();
			union.addAll(subtopics.get(i).getTweets());
			// if(i == 0) continue;
			Set<Tweet> tweetOfI = subtopics.get(i).getTweets();
			int intersection = beforeAdding + tweetOfI.size() - union.size();

			System.out.printf("%s: %d| %s}: %d|   %d, %d, %f, %f\n", subtopics.get(i).getNodeName(), tweetOfI.size(),
					buffer.toString(), beforeAdding, intersection, union.size(),
					(double) intersection / tweetOfI.size(), (double) (tweetOfI.size() - intersection) / union.size());
		}
		System.out.println("------------------------------------------------------------------------------------");
	}

	public List<String> getTopKTweetsWithoutRedundancy(List<Node> subtopics) {
		HashSet<String> coveredTweets = new HashSet<String>();

		List<String> topTweets = new ArrayList<String>();
		// topTweets: save all tweets in the final summary

		HashMap<String, Tweet> textTweetMap = new HashMap<String, Tweet>();
		HashMap<String, Tweet> topTweetMap = new HashMap<String, Tweet>();

		// HashSet<List<String>> topTweetSet = new HashSet<List<String>>();

		// for each node, iterate all tweets that contains the node
		for (Node node : subtopics) {

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
				if (!coveredTweets.contains(t.getText())) {
					importantTweets.put(t, computeTweetScore(terms));
					textTweetMap.put(t.getText(), t);
					coveredTweets.add(t.getText());
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

	// get top k tweets for each subtopics
	public List<String> getTopKDiversifiedTweetsBasedOnJaccardScore(List<Node> subtopics) {

		List<String> topTweets = new ArrayList<String>();
		// topTweets: save all tweets in the final summary

		HashMap<String, Tweet> textTweetMap = new HashMap<String, Tweet>();
		HashMap<String, Tweet> topTweetMap = new HashMap<String, Tweet>();

		// HashSet<List<String>> topTweetSet = new HashSet<List<String>>();

		// for each node, iterate all tweets that contains the node
		for (Node node : subtopics) {

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

		for (String term : terms) {
			Node currNode = wordNodeMap.get(term);
			if (currNode.getPageRank() > node.getPageRank())
				return false;

		}
		return true;
	}

	// getTopKTweetsByGettingHighestJaccardScores
	public List<String> getTopKDiversifiedTweetsWithHigestJaccardScores(List<Node> subtopics) {

		List<String> topTweets = new ArrayList<String>();

		// for each node, iterate all tweets that contains the node
		for (Node node : subtopics) {
			System.out.printf(">>>>>>>>>Node: %s\n", node.getNodeName());
			List<Tweet> tweetsOfNode = new ArrayList<Tweet>(node.getTweets());
			HashMap<Tweet, Double> tweetSimilarityMap = new HashMap<Tweet, Double>();
			tweetSimilarityMap.put(tweetsOfNode.get(0), 0.0);
			for (int i = 0; i < tweetsOfNode.size(); i++) {
				double sum = 0;
				for (int j = i + 1; j < tweetsOfNode.size(); j++) {
					double score = getJaccardScore(tweetsOfNode.get(i).getTerms(preprocessingUtils),
							tweetsOfNode.get(j).getTerms(preprocessingUtils));
					tweetSimilarityMap.put(tweetsOfNode.get(j), score);
					sum += score;
				}
				double currentScore = tweetSimilarityMap.get(tweetsOfNode.get(i));
				tweetSimilarityMap.put(tweetsOfNode.get(i), currentScore + sum);
			}

			// get top K tweets of the subtopic
			PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
			//HashMap<String, Tweet> queueTweetMap = new HashMap<String, Tweet>();
			for (Map.Entry<Tweet, Double> tweet : tweetSimilarityMap.entrySet()) {
				String text = tweet.getKey().getText();
				Double score = tweet.getValue();
				if (queue.size() < Configure.TWEETS_IN_EACH_SUBTOPIC) {
					queue.add(new KeyValue_Pair(text, score));

					//queueTweetMap.put(text, tweet.getKey());
				} else {
					KeyValue_Pair head = queue.peek();

					if (head.getDoubleValue() < score) {
						KeyValue_Pair removedTweet = queue.poll();
						queue.add(new KeyValue_Pair(text, score));

						/*queueTweetMap.remove(removedTweet.getStrKey());
						queueTweetMap.put(text, tweet.getKey());*/
					}
				}
			}
			while (!queue.isEmpty()) {
				String text = queue.poll().getStrKey();
				topTweets.add(text);
				
				System.out.println(text);
			}
		}

		return topTweets;

	}

	// check if we can add t into the set of existed tweets
	private boolean shouldAddANewTweet(Tweet newTweet, Set<Tweet> setOfExistedTweets) {
		List<String> termsOfNewTweet = newTweet.getTerms(preprocessingUtils);

		for (Tweet tweet : setOfExistedTweets) {
			List<String> termsOfCurrentTweet = tweet.getTerms(preprocessingUtils);
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

		for (String nextString : terms) {

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

		for (Node node : nodeSet) {

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

		for (Node node : graph.vertexSet()) {
			nTotalVisits += node.getNumberOfVisits();
		}

		// iter all nodes in the graph and compute PageRank

		for (Node node : graph.vertexSet()) {

			double pageRankScore = ((double) node.getNumberOfVisits()) / nTotalVisits;
			node.updatePageRank(pageRankScore);
		}
	}

	public void buildAliasSampler() {

		for (Node node : affectedNodesByAdding) {

			node.updateAliasSampler();
		}

		for (Node node : affectedNodesByRemoving) {

			if (affectedNodesByAdding.contains(node)) {
				continue;
			}
			node.updateAliasSampler();
		}

		for (Node node : newNodes) {

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

		int i = 0;
		for (Node n : vertexSet) {

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
