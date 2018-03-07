package l3s.tts.summary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.jgrapht.graph.DefaultWeightedEdge;

import l3s.tts.utils.AliasSample;
import l3s.tts.utils.Tweet;

public class Node {

	private String nodeName;
	private double pageRank;
	private int numberofVisits;
	private double utility;
	private HashMap<Integer, Integer> walkVisitMap;// map from walkId to
													// #visits
	private HashMap<DefaultWeightedEdge, Double> weightsOfOutgoingEdges;
	private ArrayList<DefaultWeightedEdge> outgoingEdgeIndexes;

	private AliasSample alias;
	private HashSet<Tweet> listOfTweets;

	public Node(Random rand) {
		// TODO Auto-generated constructor stub

		weightsOfOutgoingEdges = new HashMap<DefaultWeightedEdge, Double>();
		alias = new AliasSample(rand);
		numberofVisits = 0;
		pageRank = 0;
		utility = 0;
		listOfTweets = new HashSet<Tweet>();
		walkVisitMap = new HashMap<Integer, Integer>();

	}
	public void setUtility(double utility) {
		this.utility = utility;
	}
	
	public double getUtility() {
		return utility;
	}
	public void addTweet(Tweet tweet) {
		listOfTweets.add(tweet);
	}

	public HashMap<DefaultWeightedEdge, Double> getWeightsOfOutgoingNodes() {
		return weightsOfOutgoingEdges;
	}

	public void setWeightOfOutgoingNodes(DefaultWeightedEdge edge, double weight) {

		if (weight == 0)
			weightsOfOutgoingEdges.remove(edge);
		else
			weightsOfOutgoingEdges.put(edge, weight);

	}

	public void updateAliasSampler() {
		outgoingEdgeIndexes = new ArrayList<DefaultWeightedEdge>();
		ArrayList<Double> weights = new ArrayList<Double>();

		for (Map.Entry<DefaultWeightedEdge, Double> pair : weightsOfOutgoingEdges.entrySet()) {
			outgoingEdgeIndexes.add(pair.getKey());
			weights.add(pair.getValue());
		}

		double[] distribution = new double[weights.size()];
		double sum = 0;
		for (int i = 0; i < weights.size(); i++) {
			sum += weights.get(i);
		}

		for (int i = 0; i < weights.size(); i++) {
			distribution[i] = weights.get(i) / sum;
		}
		alias.buildBarChart(distribution);
	}

	public DefaultWeightedEdge sampleOutgoingEdges() {
		int i = alias.sample();
		return outgoingEdgeIndexes.get(i);
	}

	public void resetSegments() {
		walkVisitMap.clear();
		numberofVisits = 0;
	}

	public void updatePageRank(double pageRank) {
		this.pageRank = pageRank;
	}

	public double getPageRank() {
		return pageRank;
	}

	public AliasSample getAlias() {
		return alias;
	}

	public HashSet<Tweet> getTweets() {
		return listOfTweets;
	}

	public void removeTweet(Tweet tweet) {
		listOfTweets.remove(tweet);
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String toString() {
		/*
		 * String s = nodeName+"{"; for (int i = 0; i<tweetPosPairs.size(); i++)
		 * { s+="{"+ tweetPosPairs.get(i)[0]+", " + tweetPosPairs.get(i)[1]
		 * +"}"; } s += "}"; return s;
		 */
		return nodeName;
	}

	public void increaseVisits() {
		numberofVisits++;
	}

	public int getNumberOfVisits() {
		return numberofVisits;
	}

	public void addVisit(int walkId) {
		if (walkVisitMap.containsKey(walkId)) {
			walkVisitMap.put(walkId, walkVisitMap.get(walkId) + 1);
		} else {
			walkVisitMap.put(walkId, 1);
		}
		numberofVisits++;
	}

	public void removeWalk(int walkId) {
		if (walkVisitMap.containsKey(walkId)) {
			numberofVisits -= walkVisitMap.remove(walkId);
		}
	}

	public HashMap<Integer, Integer> getVisistedWalk() {
		return walkVisitMap;
	}
}
