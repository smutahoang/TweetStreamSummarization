package l3s.tts.summary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.jgrapht.graph.DefaultWeightedEdge;

import l3s.tts.utils.AliasSample;
import l3s.tts.utils.Tweet;

public class Node {

	private String nodeName;
	private double pageRank;
	private int numberOfSegment;
	private HashMap<DefaultWeightedEdge, Double> weightsOfOutgoingNodes;
	private AliasSample alias;
	private HashSet<Tweet> listOfTweets;
	
	public void addTweet(Tweet tweet) {
		listOfTweets.add(tweet);
	}
	
	public HashMap<DefaultWeightedEdge, Double> getWeightsOfOutgoingNodes() {
		return weightsOfOutgoingNodes;
	}
	public void setWeightOfOutgoingNodes (DefaultWeightedEdge edge, double weight) {

		if(weight == 0)
			weightsOfOutgoingNodes.remove(edge);
		else weightsOfOutgoingNodes.put(edge, weight);
		
		
	}
	public void updateAliasSampler() {
		ArrayList<Double> weights = new ArrayList<Double>(weightsOfOutgoingNodes.values());
		double[] distribution = new double[weights.size()];
		double sum = 0;
		for(int i = 0; i<weights.size(); i++) {
			sum += weights.get(i);
		}
		
		for(int i = 0; i<weights.size(); i++) {
			distribution[i] = weights.get(i)/sum;
		}
		alias.buildBarChart(distribution);
	}
	
	
	public DefaultWeightedEdge sampleOutgoingEdges() {
		
		ArrayList<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>(weightsOfOutgoingNodes.keySet());
		int i = alias.sample();
		
		return edges.get(i);
		
	}
	public void increaseSegments() {
		numberOfSegment++;
	}
	public void resetSegments() {
		numberOfSegment = 0;
	}
	public int getSegments() {
		return numberOfSegment;
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
	public Node(Random rand) {
		// TODO Auto-generated constructor stub
		
		weightsOfOutgoingNodes = new HashMap<DefaultWeightedEdge, Double>();
		alias = new AliasSample(rand);
		numberOfSegment = 0;
		pageRank = 0;
		listOfTweets = new HashSet<Tweet>();
		
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
		/*String s = nodeName+"{";
		for (int i = 0; i<tweetPosPairs.size(); i++) {
			s+="{"+ tweetPosPairs.get(i)[0]+", " + tweetPosPairs.get(i)[1] +"}";
		}
		s += "}";
		return s;*/
		return nodeName;
	}
	
}
