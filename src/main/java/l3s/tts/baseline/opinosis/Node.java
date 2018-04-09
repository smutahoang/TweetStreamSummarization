package l3s.tts.baseline.opinosis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.jgrapht.graph.DefaultWeightedEdge;

import l3s.tts.configure.Configure;


public class Node {

	private String nodeName;
	private List<int[]> tweetPosPairs;// set of pairs of sentence Index and word's position
	private int sumPos;

	public void removeTweetPosPair(int tweetId, int position) {
		for(int i = 0; i<tweetPosPairs.size(); i++) {
			if(tweetPosPairs.get(i)[0] == tweetId && tweetPosPairs.get(i)[1] == position) {
				tweetPosPairs.remove(i);
				sumPos = sumPos - position;
				break;
			}
		}
	}
	
	public static double getSetenceJaccardOverlap(List<int[]> l1, List<int[]> l2) {
        int last = 0;
        int intersect = 0;
        HashSet<Integer> union = new HashSet<Integer>();
        int i = 0;
        while (i < l1.size()) {
            int elem1 = l1.get(i)[0];
            union.add(elem1);
            int j = last;
            while (j < l2.size()) {
                int elem2 = l2.get(j)[0];
                union.add(elem2);
                if (elem2 == elem1) {
                    ++intersect;
                    last = j + 1;
                    break;
                }
                if (elem2 > elem1) break;
                ++j;
            }
            ++i;
        }
        double overlap = (double)intersect / (double)union.size();
        return overlap;
    }
	public Node(Random rand) {
		// TODO Auto-generated constructor stub
		sumPos = 0;
		tweetPosPairs = new ArrayList<int[]>();
		
		
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
	
	public boolean isEndToken() {
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
