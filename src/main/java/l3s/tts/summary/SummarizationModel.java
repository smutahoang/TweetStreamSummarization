package l3s.tts.summary;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import l3s.tts.configure.Configure;
import l3s.tts.configure.Configure.IgnoringType;
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

	public void efficientGetSubtopics() {
		// base on ALGORITHM 2 in "Diversified Recommendation on Graphs:
		// Pitfalls, Measures, and Algorithms"
		HashMap<Node, Set<Node>> nodeNeighborMap = new HashMap<Node, Set<Node>>();
		HashMap<Node, Boolean> nodeStatusMap = new HashMap<Node, Boolean>(); // node's neighbors are already consider or
																				// not
		Set<Node> uncoveredNodes = new HashSet<Node>(wordNodeMap.values());

		// Initialization: compute utility of every node
		for (Node node : uncoveredNodes) {
			Set<Node> neighbors = getNeighborsOfAndUtilityANode(node);
			nodeNeighborMap.put(node, neighbors);
			nodeStatusMap.put(node, false);
		}

		double sumOfUtility = 0;
		int numberOfIgnoredNodes = 1;
		double novelty = 0;

		Set<Tweet> coveredTweetsByFirstSet = new HashSet<Tweet>();// set of tweets covered by ignored top topics
		Set<Tweet> coveredTweetsBySecondSet = new HashSet<Tweet>();
		Node bestNode = getBestNode(uncoveredNodes);

		while (true) {
			uncoveredNodes.remove(bestNode);
			System.out.printf("removed: %s\t ", bestNode);
			// ignore some subtopics
			if (ignoreSubtopic(coveredTweetsByFirstSet, bestNode, numberOfIgnoredNodes)) {
				numberOfIgnoredNodes++;
				bestNode = getBestNode(uncoveredNodes);
				continue;
			}
			// start getting aspects
			double utility = bestNode.getUtility();
			sumOfUtility += utility;
			if (utility / sumOfUtility < Configure.MAGINAL_UTILITY)
				break;

			subtopics.add(bestNode);
			System.out.printf("Utilitly: %f\t", bestNode.getUtility());
			System.out.printf("#neighbors: %d\n", nodeNeighborMap.get(bestNode).size());
			// update utility
			updateUtility(nodeNeighborMap, nodeStatusMap, bestNode);

			coveredTweetsBySecondSet.addAll(bestNode.getTweets());
			// get the next best node
			bestNode = getBestNode(uncoveredNodes);

		}
		checkIntersectionOf2Set(coveredTweetsByFirstSet, coveredTweetsBySecondSet);
		// ==> complexity: O(n*d*d+ k*n+ k*d*d)
		// ==> old complexity: O(k*n*d*d)
	}

	private boolean ignoreSubtopic(Set<Tweet> coveredTweets, Node bestNode, int numberOfIgnoredNodes) {
		if (Configure.ignoringType == IgnoringType.TOPIC_COUNT) {
			if (numberOfIgnoredNodes <= Configure.NUMBER_OF_IGNORED_TOPICS)
				return true;
		}
		if (Configure.ignoringType == IgnoringType.NOVELTY) {
			double novelty = computeNovelty(coveredTweets, bestNode.getTweets());
			if (novelty > Configure.NOVELTY_RATIO) {
				System.out.printf("Novelty: %f\n", novelty);
				numberOfIgnoredNodes++;
				coveredTweets.addAll(bestNode.getTweets());
				return true;
			}

		}
		return false;
	}

	private void checkIntersectionOf2Set(Set<Tweet> set1, Set<Tweet> set2) {
		Set<Tweet> union = new HashSet<Tweet>();
		union.addAll(set1);
		union.addAll(set2);
		int intersection = set1.size() + set2.size() - union.size();
		System.out.printf("\nfirst set: %d, second set: %d, intersection/set1: %f, intersection/set2: %f\n",
				set1.size(), set2.size(), (double) intersection / set1.size(), (double) intersection / set2.size());
	}

	private void updateUtility(HashMap<Node, Set<Node>> nodeNeighborMap, HashMap<Node, Boolean> nodeStatusMap,
			Node bestNode) {
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

	private Node getBestNode(Set<Node> uncoveredNodes) {
		Node bestNode = null;
		for (Node node : uncoveredNodes) {
			if (bestNode == null || (bestNode != null && bestNode.getUtility() < node.getUtility()))
				bestNode = node;

		}
		return bestNode;
	}

	public double computeNovelty(Set<Tweet> existedTweets, Set<Tweet> newTweets) {
		double novelty = 0;
		Set<Tweet> union = new HashSet<Tweet>();
		union.addAll(existedTweets);
		union.addAll(newTweets);
		int intersection = existedTweets.size() + newTweets.size() - union.size();
		novelty = (double) (newTweets.size() - intersection) / union.size();
		return novelty;
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

	public Set<Tweet> getTopKDiversifiedTweets(List<Node> subtopics) {

		Set<String> coveredTweets = new HashSet<String>();// set of all tweets of considered topics
		HashMap<Tweet, List<String>> union = new HashMap<Tweet, List<String>>(); // union of important tweets of each
																					// topics
		HashMap<String, Tweet> textTweetMap = new HashMap<String, Tweet>();
		for (Node node : subtopics) {

			Set<Tweet> tweetsOfNode = node.getTweets();

			PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
			for (Tweet tweet : tweetsOfNode) {
				HashSet<String> terms = new HashSet<String>();
				terms.addAll(tweet.getTerms(preprocessingUtils));

				String text = tweet.getUserId();
				double pageRank = computeTweetScore(terms);
				textTweetMap.put(text, tweet);

				// option allow get a tweet that is already chosen by another topic
				if (Configure.OVERLAPPING_TOPICS == false && coveredTweets.contains(text))
					continue;
				coveredTweets.add(text);
				queue.add(new KeyValue_Pair(text, pageRank * (-1)));

			}
			System.out.printf("\n>>>>>Node: %s\n", node.getNodeName());
			// get top K tweets of the current subtopic
			int count = 0;
			while (count < Configure.TWEETS_IN_EACH_SUBTOPIC && queue.size() > 0) {
				KeyValue_Pair maxKey = queue.poll();
				Tweet currentTweet = textTweetMap.get(maxKey.getStrKey());
				if (shouldAddANewTweet(currentTweet, union.keySet())) {
					union.put(currentTweet, currentTweet.getTerms(preprocessingUtils));
					System.out.println(currentTweet.getText());
					count++;
				}
			}

		}
		System.out.println("\n>>>>>>>>>>>>FINAL RESULT>>>>>>>>>>>>>>>>>>>>");
		// remove redundancy of the union set
		Set<Tweet> result = removeRedundancyByDiversifiedRanking(union);
		return null;
	}

	private Set<Tweet> removeRedundancyByDiversifiedRanking(HashMap<Tweet, List<String>> input) {
		Set<Tweet> output = new HashSet<Tweet>();

		// compute Jaccard similarity of each tweet with the others
		List<Tweet> listOfTweets = new ArrayList<Tweet>(input.keySet());
		HashMap<Tweet, Double> tweetSimilarityMap = new HashMap<Tweet, Double>();
		tweetSimilarityMap.put(listOfTweets.get(0), 0.0);
		for (int i = 0; i < listOfTweets.size()-1; i++) {
			double sum = 0;
			for (int j = i + 1; j < listOfTweets.size(); j++) {
				double score = getJaccardScore(input.get(listOfTweets.get(i)), input.get(listOfTweets.get(j)));
				
				if (i == 0)
					tweetSimilarityMap.put(listOfTweets.get(j), score);
				else
					tweetSimilarityMap.put(listOfTweets.get(j), score + tweetSimilarityMap.get(listOfTweets.get(j)));
				sum += score;
			}
			double currentScore = tweetSimilarityMap.get(listOfTweets.get(i));
			tweetSimilarityMap.put(listOfTweets.get(i), sum + currentScore);
			
		}
		
		// remove redundancy
		double sumOfUtility = 0;
		
		while(true) {
			// get the best tweet
			double utility = 0;
			

			Tweet bestTweet = null;
			for(Tweet t: listOfTweets) {
				if(tweetSimilarityMap.get(t) > utility) {
					bestTweet = t;
					utility = tweetSimilarityMap.get(t);
				}
			}
			sumOfUtility +=utility;
		//	System.out.printf("utility: %f\n", utility);
			if(utility/sumOfUtility < 0.01)
				break;
			
			System.out.println(bestTweet.getText());
			output.add(bestTweet);
			listOfTweets.remove(bestTweet);
			//reduce score of remaining tweets
			for(Tweet t: listOfTweets) {
				tweetSimilarityMap.put(t, tweetSimilarityMap.get(t) - getJaccardScore(input.get(t), input.get(bestTweet)));
				//System.out.println("fff"+tweetSimilarityMap.get(t));
			}
		}
		System.out.println(output.size());
		return output;
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
			// HashMap<String, Tweet> queueTweetMap = new HashMap<String, Tweet>();
			for (Map.Entry<Tweet, Double> tweet : tweetSimilarityMap.entrySet()) {
				String text = tweet.getKey().getText();
				Double score = tweet.getValue();
				if (queue.size() < Configure.TWEETS_IN_EACH_SUBTOPIC) {
					queue.add(new KeyValue_Pair(text, score));

					// queueTweetMap.put(text, tweet.getKey());
				} else {
					KeyValue_Pair head = queue.peek();

					if (head.getDoubleValue() < score) {
						KeyValue_Pair removedTweet = queue.poll();
						queue.add(new KeyValue_Pair(text, score));

						/*
						 * queueTweetMap.remove(removedTweet.getStrKey()); queueTweetMap.put(text,
						 * tweet.getKey());
						 */
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
