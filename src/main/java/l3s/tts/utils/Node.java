package l3s.tts.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import l3s.tts.configure.Configure;

public class Node {
	private String nodeName;
	private List<int[]> tweetPosPairs;// set of pairs of sentence Index and word's position
	private int sumPos;
	private HashSet<String> tweets = new HashSet<String>();// list of tweets that contain this word
	// how to save all paths going through the node for re-sample
	public Node() {
		// TODO Auto-generated constructor stub
		sumPos = 0;
		tweetPosPairs = new ArrayList<int[]>();
	}
	public String getNodeName() {
		return nodeName;
	}
	public List<int[]> getSenPosPairs() {
		return tweetPosPairs;
	}

	public void addNewTweetPosPair(int tweetId, int position, String tweet) {
		tweetPosPairs.add(new int[] {tweetId, position});
		tweets.add(tweet);
		sumPos += position;
	}
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	public double getAveragePos() {
		return (double)sumPos/tweets.size();
	}
	
	public boolean isVSN() {
		String tagName = nodeName.substring(nodeName.indexOf("/"));
		if (getAveragePos() <= Configure.VSN_POS && ((new HashSet<String>(Configure.VSN_POS)).contains(tagName)||nodeName.matches(Configure.VSN_NAME))) {
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
		String s = nodeName +"{";
		for (int i = 0; i<tweetPosPairs.size(); i++) {
			s+="{"+ tweetPosPairs.get(i)[0]+", " + tweetPosPairs.get(i)[1] +"}";
		}
		s += "}";
		return s;
	}
	
}
