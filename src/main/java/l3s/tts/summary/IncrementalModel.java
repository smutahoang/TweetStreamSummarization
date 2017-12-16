package l3s.tts.summary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import l3s.tts.configure.Configure;
import l3s.tts.utils.AliasSample;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetStream;

public class IncrementalModel extends TweetGraph {
	TweetStream stream;

	List<Candidate> candidates;

	String outputDir;

	public IncrementalModel(TweetStream stream, String outputDir) {
		super();
		this.stream = stream;
		this.outputDir = outputDir;
		candidates = new ArrayList<Candidate>();

	}

	public void run() {
		Tweet tweet = null;
		int nOfTweets = 0;
		while ((tweet = stream.getTweet()) != null) {
			System.out.println(tweet);
			addNewTweet(tweet, nOfTweets);
			nOfTweets++;
			if (nOfTweets == Configure.TWEET_WINDOW) {
				generateSummary();
				printCandidates();
				System.exit(-1);
			}
			// if it is time to update
			else if (nOfTweets % Configure.TWEET_WINDOW == 0) {
				update();
				reGenerateSummary();
			}
		}
	}

	public void reGenerateSummary() {
		// findingCandidates();

	}

	public void generateSummary() {
		System.out.println("start finding candidates");
		findingCandidates();

	}

	public boolean isTimeToUpdate() {

		return false;
	}

	public void update() {

	}

	public void printCandidates() {
		for (int i = 0; i < candidates.size(); i++) {
			System.out.println(candidates.get(i));
		}
	}

	public void findingCandidates() {
		AliasSample sampler = new AliasSample(new Random());
		Set<Node> nodeList = graph.vertexSet();
		Iterator<Node> iter = nodeList.iterator();

		// iterate all nodes in the graph
		while (iter.hasNext()) {
			Node currentNode = iter.next();
			//System.out.println(currentNode);
			// find a valid starting node to keep going
			if (!currentNode.isVSN())
				continue;
			int i = 0;
			ArrayList<DefaultWeightedEdge> outgoingEdges = new ArrayList<DefaultWeightedEdge>(
					graph.outgoingEdgesOf(currentNode));
			
			double[] weights;
			// for each VSN, we will sample some valid paths
			while (i < Configure.SAMPLE_NUMBER) {// iterate to sample
				Candidate can = new Candidate();
				can.addNode(currentNode);
				while (true) { // find the next node in the path until reaching to the end tokens
				
					weights = getWeightOfOutGoingEdges(outgoingEdges);
					sampler.buildBarChart(weights);
				
					DefaultWeightedEdge nextEdge = outgoingEdges.get(sampler.sample());
					Node nextNode = graph.getEdgeTarget(nextEdge);
					if (nextNode.getNodeName().matches(Configure.ENDTOKENS) || graph.outgoingEdgesOf(nextNode).size() == 0) {
						can.addNode(nextNode);
						if (can.isValidCandidate()) {
							candidates.add(can);
						}
						break;
					}
					
					currentNode = nextNode;
					outgoingEdges = new ArrayList<DefaultWeightedEdge>(graph.outgoingEdgesOf(currentNode));
					
				}
				i++;

			}
		}
	}
	public double[] getWeightOfOutGoingEdges(ArrayList<DefaultWeightedEdge> edges) {
		
		double[] distribution = new double[edges.size()];
		double sum = 0;
		int i = 0;
		for(DefaultWeightedEdge edge: edges){
			distribution[i] = graph.getEdgeWeight(edge);
			sum+=distribution[i];
			i++;
		}
		for(i = 0; i<distribution.length; i++) {
			distribution[i] = distribution[i]/sum;
		}
		return distribution;
	}

	/*
	 * public void removeDiscard() { for(int i=0 ; i<candidates.size(); i++) {
	 * if(candidates.get(i).getIsDiscard()) { candidates.remove(i); i--; } } }
	 * 
	 * 
	 * public void generateSummary() { Collections.sort(candidates, new
	 * Comparator<Candidate>() {
	 * 
	 * public int compare(Candidate o1, Candidate o2) { // TODO Auto-generated
	 * method stub double score1 = o1.getScore(); double score2 = o2.getScore(); if
	 * (score1 < score2) return -1; else if (score1 > score2) return 1; else return
	 * 0;
	 * 
	 * }
	 * 
	 * }); int count = 0, i = 0; while (count < Configure.MAX_SUMMARIES && i <
	 * candidates.size()) { if (!candidates.get(i).getIsDiscard()) {
	 * System.out.println(candidates.get(i)); count++; } i++; } }
	 * 
	 *//**
		 * compute score of a path and combine different paths
		 *//*
			 * public void combineTweets() {
			 * 
			 * int numOfCandidates = candidates.size(); // iterate all candidates for (int j
			 * = 0; j < numOfCandidates; j++) { if (candidates.get(j).getIsDiscard())
			 * continue; // get nodes of current candidates List<Node> currNodeList =
			 * candidates.get(j).getNodeList(); List<Candidate> ccCandidate = new
			 * ArrayList<Candidate>();
			 * 
			 * int i = 0; // iterate all nodes of the jth candidate for (i = 0; i <
			 * currNodeList.size(); i++) { Node node = currNodeList.get(i);
			 * 
			 * if (!node.isCollapse() || i == currNodeList.size() - 1) continue;
			 * System.out.println(node.getNodeName()); // if this sentence contain a
			 * collapsible node, we will find a way to combine // aspects of prefixes
			 * ccCandidate.add(candidates.get(j)); for (int k = j + 1; k < numOfCandidates;
			 * k++) { List<Node> kCanNodeList = candidates.get(k).getNodeList(); if
			 * (!kCanNodeList.contains(node) || !candidates.get(k).getIsCollapse() ||
			 * candidates.get(k).getIsDiscard()) continue;
			 * 
			 * if (candidates.get(k).getNodeList().indexOf(node) ==
			 * candidates.get(k).getNodeList().size()) continue; double prefixOverlap =
			 * candidates.get(j).computeJaccardScore(candidates.get(k), node, 0);
			 * 
			 * if (prefixOverlap >= Configure.DUPLICATE_PREFIX_THRESOLD &&
			 * isCombined(ccCandidate, candidates.get(k), node)) {
			 * ccCandidate.add(candidates.get(k)); } candidates.get(k).setIsDiscard(true);
			 * 
			 * } if (ccCandidate.size() == 1) continue;
			 * 
			 * candidates.get(j).setIsDiscard(true); Candidate cc = new Candidate(); double
			 * score = candidates.get(j).computeScore(); for (int t = 0; t <
			 * currNodeList.size(); t++) cc.addNode(currNodeList.get(t)); for (int t = 1; t
			 * < ccCandidate.size(); t++) { score += ccCandidate.get(t).computeScore(); int
			 * index = ccCandidate.get(t).getNodeList().indexOf(node); if (t ==
			 * ccCandidate.size() - 1) cc.addCordinatingConjunction(" ----- and---- "); else
			 * cc.addCordinatingConjunction(" -----, ------"); for (int h = index + 1; h <
			 * ccCandidate.get(t).getNodeList().size(); h++) {
			 * cc.addNode(ccCandidate.get(t).getNodeList().get(h)); } } cc.setScore(score /
			 * ccCandidate.size()); cc.setIsCollapse(true); candidates.add(cc);
			 * 
			 * break;
			 * 
			 * }
			 * 
			 * } }
			 * 
			 * public boolean isCombined(List<Candidate> candidates, Candidate can, Node n)
			 * { for (int i = 0; i < candidates.size(); i++) { if
			 * (candidates.get(i).computeJaccardScore(can, n, 1) >
			 * Configure.DUPLICATE_SUFFIX_THRESOLD) { return false; } }
			 * 
			 * return true; }
			 * 
			 * public void removeDuplicates() { for (int i = 0; i < candidates.size(); i++)
			 * { if (candidates.get(i).getIsDiscard()) continue; for (int j = i + 1; j <
			 * candidates.size(); j++) { if (candidates.get(j).getIsDiscard()) continue; if
			 * (candidates.get(i).computeJaccardScore(candidates.get(j)) >
			 * Configure.DUPLICATE_THRESOLD) { if (candidates.get(i).getScore() >
			 * candidates.get(j).getScore()) candidates.get(j).setIsDiscard(true); else
			 * candidates.get(i).setIsDiscard(true); } } } }
			 * 
			 * public DefaultWeightedEdge sampleMultimomial(Set<DefaultWeightedEdge>
			 * outgoingEdges, Random rand) { List<DefaultWeightedEdge> edges = new
			 * ArrayList<DefaultWeightedEdge>(outgoingEdges); double[] distribution = new
			 * double[edges.size()]; double sumOfWeights = 0;
			 * 
			 * for (int i = 0; i < edges.size(); i++) { distribution[i] =
			 * graph.getGraph().getEdgeWeight(edges.get(i)); sumOfWeights = sumOfWeights +
			 * distribution[i]; } double sum = 0; double x = rand.nextDouble(); for (int i =
			 * 0; i < edges.size(); i++) { sum += distribution[i] / sumOfWeights; if (sum >=
			 * x) return edges.get(i); } return edges.get(edges.size() - 1); }
			 * 
			 * public List<Candidate> getCandidates() { return candidates; }
			 */
}
