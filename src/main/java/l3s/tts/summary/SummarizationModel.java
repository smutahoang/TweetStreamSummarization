package l3s.tts.summary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.google.common.base.Stopwatch;

import cmu.arktweetnlp.Tagger.TaggedToken;
import l3s.tts.configure.Configure;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetPreprocessingUtils;

public class SummarizationModel {
	protected TweetPreprocessingUtils preprocessingUtils;
	protected SimpleDirectedGraph<Node, DefaultWeightedEdge> graph;
	protected HashMap<String, Node> wordNodeMap;
	protected List<Candidate> candidates;
	protected HashSet<Node> affectedNodesByAdding;
	protected HashSet<Node> affectedNodesByRemoving;
	protected HashSet<String> stopWords;

	protected Random rand;

	public SummarizationModel() {
		// TODO Auto-generated constructor stub
		wordNodeMap = new HashMap<String, Node>();
		graph = new SimpleDirectedGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		preprocessingUtils = new TweetPreprocessingUtils();
		affectedNodesByAdding = new HashSet<Node>();
		affectedNodesByRemoving = new HashSet<Node>();
		candidates = new ArrayList<Candidate>();
		stopWords = preprocessingUtils.getStopWords();
		rand = new Random();
	}

	/**
	 * add a new tweet into graph
	 * 
	 * @param tweet:
	 *            new tweet
	 * @param index:
	 *            index of tweet
	 */
	public void addNewTweet(Tweet tweet, int index) {
		// iterate all tagged words in the tweet
		List<TaggedToken> tokens = tweet.getTaggedTokens();
		boolean isPrevNodeNew = true;
		boolean isCurNodeNew = true;
		Node preNode = null;
		Node curNode = null;

		for (int i = 0; i < tokens.size(); i++) {
			StringBuilder builder = new StringBuilder(tokens.get(i).token);
			builder.append("/");
			builder.append(tokens.get(i).tag.toLowerCase());
			String nodeString = builder.toString();

			// if graph contained this node, add a new pair of tweetId - word position
			if (wordNodeMap.containsKey(nodeString)) {
				isCurNodeNew = false;

				curNode = wordNodeMap.get(nodeString);
				curNode.addNewTweetPosPair(index, i);

			} else { // if graph doesnt contain this node
				curNode = new Node(rand); // create a new node
				curNode.setNodeName(nodeString);

				curNode.addNewTweetPosPair(index, i); // add a pair of sentence Id and position of word in the node
				graph.addVertex(curNode); // add new node into graph

				wordNodeMap.put(nodeString, curNode);
				isCurNodeNew = true;
			}
			affectedNodesByAdding.add(curNode);
			if (isPrevNodeNew || isCurNodeNew) { // if current node or previous node is a new node, add a new edge into
													// graoh
				// check if current node is the first node in the graph
				if (preNode != null && !curNode.equals(preNode)) {// &&
																	// !preNode.getNodeName().matches(Configure.ENDTOKENS))
																	// {
					DefaultWeightedEdge addedEdge = graph.addEdge(preNode, curNode);
					preNode.setWeightOfOutgoingNodes(addedEdge, tweet.getWeight());
				}
			} else {
				DefaultWeightedEdge edge = graph.getEdge(preNode, curNode);
				if (edge == null) {
					if (!curNode.equals(preNode)) {// && !preNode.getNodeName().matches(Configure.ENDTOKENS))
						DefaultWeightedEdge addedEdge = graph.addEdge(preNode, curNode);
						preNode.setWeightOfOutgoingNodes(addedEdge, tweet.getWeight());
					}
				} else {
					double weight = graph.getEdgeWeight(edge) + tweet.getWeight();
					graph.setEdgeWeight(edge, weight);
					preNode.setWeightOfOutgoingNodes(edge, weight);
				}
			}
			preNode = curNode;
			isPrevNodeNew = isCurNodeNew;
			
		}
	}

	public void findingCandidates() {

		Set<Node> nodeList = graph.vertexSet();
		Iterator<Node> iter = nodeList.iterator();
		int vsnCount = 0;
		// iterate all nodes in the graph
		while (iter.hasNext()) {
			Node startNode = iter.next();

			// find a valid starting node to keep going
			if (!startNode.isVSN()) {
				continue;
			}
			vsnCount++;
			System.out.printf("--> %d. VSN: %s\n", vsnCount, startNode.getNodeName());//in the sentences: [", vsnCount, startNode.getNodeName());
			/*for (int[] array : startNode.getTweetPosPairs()) {
				System.out.printf("[%d %d]", array[0], array[1]);
			}
			System.out.printf("]\n");*/
			int i = 0;

			while (i < Configure.SAMPLE_NUMBER) {// iterate to sample
				Candidate can = new Candidate();
				can.addNode(startNode);
				List<int[]> overlap = new ArrayList<int[]>();
				overlap.addAll(startNode.getTweetPosPairs());
				double score = 0;

				sampleAValidPath(can, startNode, overlap, score);
				// System.out.printf("Candidates: %s\n", can.toString());
				i++;

			}
		}
	}

	public void sampleAValidPath(Candidate can, Node startNode, List<int[]> currentOverlap, double currentScore) {

		ArrayList<DefaultWeightedEdge> outgoingEdges = new ArrayList<DefaultWeightedEdge>(
				graph.outgoingEdgesOf(startNode));

		Node currentNode = startNode;
		while (true) { // find the next node in the path until reaching to the end tokens

			if (outgoingEdges.size() == 0)
				return;
			DefaultWeightedEdge nextEdge = currentNode.sampleOutgoingEdges();


			Node nextNode = graph.getEdgeTarget(nextEdge);
			String nameOfNextNode = nextNode.getNodeName().substring(0, nextNode.getNodeName().indexOf("/"));
			
			if (nameOfNextNode.matches(Configure.ENDTOKENS) && Configure.STOP_AT_ENDINGTOKENS) {
				if (!nameOfNextNode.matches(Configure.ENDTOKENS)) { // doesnt have meaning (should add . into candidate if STOP_AT_ENDINGTOKENS = true?)
					can.addNode(nextNode); // we wont add end token at the end of a candidate/a valid path
					//currentOverlap = getOverlapIntersection(currentOverlap, nextNode.getTweetPosPairs());
					//currentScore = computeScore(currentScore, currentOverlap, can.getNodeList().size());
					//currentScore = computeScore(currentScore, currentOverlap, can.getNodeList().size(), graph.getEdgeWeight(nextEdge));
				}
				
				if(can.getNodeList().size() < 5) {
					System.out.printf("\n+ Invalid (length < 5): %s\n", can.getCan());
					break;
				}
				/*if(!can.getCan().contains("/vb")) {
					System.out.printf("+ Invalid (doesnt contain a verb: %s)", can.getCan());
					break;
				}*/
				//if (can.isValidCandidate()) {
					//can.computeAdjustScore(currentScore);
					candidates.add(can);
					//System.out.printf(" + Valid: %s\n", can.getCan());
					System.out.printf("\n+ Valid: ");
					for(int i = 0; i<can.getNodeList().size(); i++) {
						System.out.printf("%s[%d] ", can.getNodeList().get(i).getNodeName(), can.getNodeList().get(i).getTweetPosPairs().size());
					}
					System.out.println();
					// System.out.println(can);
				//	break;
				//} else {
				//	System.out.printf(" + EndButInvalid: %s\n", can.getCan());
					// System.out.println(can);
					break;
				//}
					
				// check if containing a circle in candidate
				

			} else if (graph.outgoingEdgesOf(nextNode).size() > 0) {
				if(isContainingACircle(can, nextNode)) {
					System.out.printf("\n  + Circle: %s \"%s\"\n", can.getCan(), nextNode.getNodeName());
					break;
				}
				can.addNode(nextNode);
				//currentOverlap = getOverlapIntersection(currentOverlap, nextNode.getTweetPosPairs());
				//currentScore = computeScore(currentScore, currentOverlap, can.getNodeList().size());
				//currentScore = computeScore(currentScore, currentOverlap, can.getNodeList().size(), graph.getEdgeWeight(nextEdge));
			}
			if (!shouldContinue(currentOverlap, can.getNodeList().size())) {
				System.out.printf("-->Invalid (length: %d): %s" , can.getNodeList().size(), can);
				break;
			}
			outgoingEdges = new ArrayList<DefaultWeightedEdge>(graph.outgoingEdgesOf(nextNode));
			currentNode = nextNode;
		}
	}

	// new test using the weights in the edges
	public double computeScore(double currentScore, List<int[]> currentOverlap, int pathLength, double weight) {
		double score = 0;
		switch (Configure.SCORING_FUNCTION) {
		case GAIN_REDUNDANCY_ONLY:
			score = currentScore  + weight;
			break;
		case GAIN_WEIGHTED_REDUNDANCY_BY_LEVEL:
			score = currentScore  + weight;
			break;
		case GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL:
			score = currentScore + weight; // log_2(pathLength)
			break;
		}
		System.out.printf("[s: %.1f, %d, %.1f], ", currentScore, currentOverlap.size(), weight);
		return score;
	}
	
	// original solution
	public double computeScore(double currentScore, List<int[]> currentOverlap, int pathLength) {
	
		double score = 0;
		switch (Configure.SCORING_FUNCTION) {
		case GAIN_REDUNDANCY_ONLY:
			score = currentScore + currentOverlap.size();
			break;
		case GAIN_WEIGHTED_REDUNDANCY_BY_LEVEL:
			score = currentScore + pathLength * currentOverlap.size();
			break;
		case GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL:
			score = currentScore + (Math.log(pathLength) / Math.log(2)) * currentOverlap.size(); // log_2(pathLength)
			break;
		}
		System.out.printf("score: %f, ", currentScore);
		return score;
	}
	
	// compute score after finding all candidates
	public void computeCandidateScores() {
		for(int i = 0; i<candidates.size(); i++) {
			candidates.get(i).computeScore();
		}
	}

	public void removeDuplicates() {
		int count = 0;
		for (int i = 0; i < candidates.size(); i++) {
			if (candidates.get(i).getIsDiscard())
				continue;
			for (int j = i + 1; j < candidates.size(); j++) {
				if (candidates.get(j).getIsDiscard())
					continue;
				if (candidates.get(i).computeJaccardScore(candidates.get(j)) >= Configure.REMOVING_DUPLICATE_THRESOLD) {
					if (candidates.get(i).getScore() > candidates.get(j).getScore()) {
						candidates.get(j).setIsDiscard(true);
						count++;
						// System.out.println("--> remove: "+ candidates.get(j));

					} else {
						candidates.get(i).setIsDiscard(true);
						count++;
						// System.out.println("--> remove: " + candidates.get(i));
						break;
					}
				}
			}
		}
		System.out.printf("-------------------->number of removed candidates: %d\n", count);
	}

	// check if adding s create a circle or not
	public boolean isContainingACircle(Candidate can, Node node) {
		
		String nodeName = node.getNodeName();
		String canString = can.getCan();
		String nodeString = nodeName.substring(0, nodeName.indexOf("/"));
		if(canString.contains(nodeName) && !stopWords.contains(nodeString))
			return true;
		return false;
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

	public List<int[]> getOverlapIntersection(List<int[]> left, List<int[]> right) {
		List<int[]> result = new ArrayList<int[]>();
		int pointer = 0;
		int i = 0;
		while (i < left.size()) {
			int[] eleft = left.get(i);
			if (pointer > right.size())
				break;
			int j = pointer;
			while (j < right.size()) {
				int[] eright = right.get(j);
				if (eright[0] == eleft[0]) {
					if (eright[1] > eleft[1] && Math.abs(eright[1] - eleft[1]) <= Configure.PERMISSABLE_GAP) {
						result.add(eright);
						pointer = j + 1;
						break;
					}
					//eright[1] = eleft[1];
				} else if (eright[0] > eleft[0])
					break;
				++j;
			}
			++i;
		}
		//System.out.printf("%d, ", left.size());
		return result;
	}

	public void printCandidates(Formatter format) {
		for (int i = 0; i < candidates.size(); i++) {
			format.format("%d. %s\n", i + 1, candidates.get(i));
		}

	}

	public void printCandidates() {
		int count = 0;
		for(int i = 0; i<candidates.size(); i++) {
			if(!candidates.get(i).getIsDiscard()) {
				System.out.printf("%d. %f, %s\n", (i+1), candidates.get(i).getScore(), candidates.get(i));
				count++;
			}
		}
		System.out.printf("--> %d candidates\n", count);
	}
	// compute score of a path and combine different paths

	public void combineTweets() {

		int numOfCandidates = candidates.size(); // iterate all candidates
		for (int j = 0; j < numOfCandidates; j++) {
			if (candidates.get(j).getIsDiscard())
				continue; // get nodes of current candidates
			List<Node> currNodeList = candidates.get(j).getNodeList();
			List<Candidate> ccCandidate = new ArrayList<Candidate>();

			int i;// iterate all nodes of the jth candidate
			for (i = 0; i < currNodeList.size(); i++) {
				Node node = currNodeList.get(i);

				if (!node.isCollapse() || i == currNodeList.size() - 1)
					continue;
				// if this sentence contain a collapsible node, we will find a way to combine
				// aspects of prefixes
				ccCandidate.add(candidates.get(j));

				for (int k = j + 1; k < numOfCandidates; k++) {
					List<Node> kCanNodeList = candidates.get(k).getNodeList();
					if (!kCanNodeList.contains(node) || candidates.get(k).getIsCollapsed()
							|| candidates.get(k).getIsDiscard())
						continue;

					if (candidates.get(k).getNodeList().indexOf(node) == candidates.get(k).getNodeList().size())
						continue;
					double prefixOverlap = candidates.get(j).computeJaccardScore(candidates.get(k), node, 0);

					if (prefixOverlap >= Configure.DUPLICATE_PREFIX_THRESOLD) {
						if (isCombined(ccCandidate, candidates.get(k), node)) {
							ccCandidate.add(candidates.get(k));
						}
						candidates.get(k).setIsDiscard(true);
					}

				}
				if (ccCandidate.size() == 1)
					break;

				candidates.get(j).setIsDiscard(true);
				Candidate cc = new Candidate();
				double score = candidates.get(j).getScore();

				for (int t = 0; t < currNodeList.size(); t++)
					cc.addNode(currNodeList.get(t));
				for (int t = 1; t < ccCandidate.size(); t++) {

					score += ccCandidate.get(t).getScore();
					int index = ccCandidate.get(t).getNodeList().indexOf(node);
					if (t == ccCandidate.size() - 1)
						cc.addCordinatingConjunction(" ----- and---- ");
					else
						cc.addCordinatingConjunction(" -----, ------");
					for (int h = index + 1; h < ccCandidate.get(t).getNodeList().size(); h++) {
						cc.addNode(ccCandidate.get(t).getNodeList().get(h));
					}
				}
				cc.setScore(score / ccCandidate.size());
				cc.setIsCollapse(true);
				candidates.add(cc);

				break;

			}

		}
	}

	
	// sort and get highest score candidates
	public ArrayList<Candidate> sortAndGetHighScoreSummaries() {
		ArrayList<Candidate> summary = new ArrayList<Candidate>();
		// can be improved
		Collections.sort(candidates, new Comparator<Candidate>() {

			public int compare(Candidate o1, Candidate o2) { // TODO Auto-generated method stub
				double score1 = o1.getScore();
				double score2 = o2.getScore();
				if (score1 < score2)
					return -1;
				else if (score1 > score2)
					return 1;
				else
					return 0;

			}

		});
		int count = 0, i = 0;
		// get Max_Summaries candidates
		while (count < Configure.MAX_SUMMARIES && i < candidates.size()) {
			if (!candidates.get(i).getIsDiscard()) {
				int j;
				// ignore a candidate if there existed a similar candidate in the chosen summary
				for (j = 0; j < summary.size(); j++) {
					if (candidates.get(i).computeJaccardScore(summary.get(j)) > Configure.DUPLICATE_THRESOLD) {
						i++;
						break;
					}
				}
				if (j == summary.size()) {
					summary.add(candidates.get(i));
					count++;
				}
			}
			i++;
			if (count == Configure.MAX_SUMMARIES)
				break;

		}
		return summary;
	}

	public boolean isCombined(List<Candidate> candidates, Candidate can, Node n) {
		for (int i = 0; i < candidates.size(); i++) {
			if (candidates.get(i).computeJaccardScore(can, n, 1) > Configure.DUPLICATE_SUFFIX_THRESOLD) {
				return false;
			}
		}

		return true;
	}

	public boolean shouldContinue(List<int[]> currentOverlap, int pathLength) {
		if (pathLength >= Configure.P_MAX_SENT_LENGTH) {
			return false;
		}

		if (currentOverlap.size() < Configure.MIN_REDUNDANCY) {// && !this.isEndToken(x)) {
			return false;
		}
		return true;
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
