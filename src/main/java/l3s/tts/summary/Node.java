package l3s.tts.summary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jgrapht.graph.DefaultWeightedEdge;

import l3s.tts.configure.Configure;
import l3s.tts.utils.AliasSample;

public class Node {

	private String nodeName;
	private List<int[]> tweetPosPairs;// set of pairs of sentence Index and word's position
	private int sumPos;
	private HashMap<DefaultWeightedEdge, Double> weightsOfOutgoingNodes;
	private AliasSample alias;
	
	public void removeTweetPosPair(int tweetId, int position) {
		for(int i = 0; i<tweetPosPairs.size(); i++) {
			if(tweetPosPairs.get(i)[0] == tweetId && tweetPosPairs.get(i)[1] == position) {
				tweetPosPairs.remove(i);
				sumPos = sumPos - position;
				break;
			}
		}
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
	
	public AliasSample getAlias() {
		return alias;
	}
	public Node(Random rand) {
		// TODO Auto-generated constructor stub
		sumPos = 0;
		tweetPosPairs = new ArrayList<int[]>();
		weightsOfOutgoingNodes = new HashMap<DefaultWeightedEdge, Double>();
		alias = new AliasSample(rand);
		
	}
	public String getNodeName() {
		return nodeName;
	}
	public List<int[]> getTweetPosPairs() {
		return tweetPosPairs;
	}

	public void addNewTweetPosPair(int tweetId, int position) {
		tweetPosPairs.add(new int[] {tweetId, position});
		sumPos += position;
	}
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	public double getAveragePos() {
		return (double)sumPos/tweetPosPairs.size();
	}
	
	public boolean isVSN() {
		//String tagName = nodeName.substring(nodeName.indexOf("/"));
		if (getAveragePos() <= Configure.VSN_POS && (nodeName.matches(Configure.VSN_TAGs)||nodeName.matches(Configure.VSN_NAME))) {
            return true;
        }
        return false;

	}
	
	public boolean isEndTokenInNodeName() {
		return nodeName.matches(Configure.ENDTOKENS);
	}
	
	public boolean isCollapse() {
		return nodeName.matches(Configure.OVERLAP_NODE);
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
