package l3s.tts.summary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import l3s.tts.configure.Configure;

public class Candidate {
	public static final String SPACE = "\\s+";
	public static final String TAG = "(/[a-z,.:;$]+(\\s+|$))|(/''(\\s+|$))";
	public static final String COLLAPSE = "xx";
	private List<Node> nodeList;
	private String candidateString;
	private boolean isDiscard;
	private boolean isCollapsed;
	private double score;

	public Candidate() {
		// TODO Auto-generated constructor stub
		nodeList = new ArrayList<Node>();
		candidateString = "";
		isDiscard = false;
		isCollapsed = false;
		score = 0;
	}

	public void setCandidateString(String candidateString) {
		this.candidateString = candidateString;
	}

	public void setIsDiscard(boolean isDiscard) {
		this.isDiscard = isDiscard;
	}

	public void setIsCollapse(boolean isCollapse) {
		this.isCollapsed = isCollapse;
	}

	public boolean getIsCollapsed() {
		return isCollapsed;
	}

	public boolean getIsDiscard() {
		return isDiscard;
	}

	public List<Node> getNodeList() {
		return nodeList;
	}

	public void addNode(Node node) {
		nodeList.add(node);
		candidateString += " " + node.getNodeName();
	}

	public void addCordinatingConjunction(String conj) {
		candidateString += conj;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getScore() {
		return score;
	}

	


	public double computeJaccardScore(Candidate other) {
		double result = 0;
		HashSet<Node> union = new HashSet<Node>(this.nodeList);
		HashSet<Node> intersection = new HashSet<Node>(this.nodeList);
		union.addAll(other.getNodeList());
		intersection.retainAll(other.getNodeList());
		result = (double) intersection.size() / union.size();
		return result;
	}

	/**
	 * 
	 * @param other
	 * @param node:
	 *            collapsible node
	 * @param position
	 *            : head or tail of candidate (calculate from the node) = 0 if
	 *            compute from starting point of candidate to the node
	 * @return
	 */
	public double computeJaccardScore(Candidate other, Node node, int position) {
		double result = 0;
		int index1 = this.getNodeList().indexOf(node);
		int index2 = other.getNodeList().indexOf(node);
		HashSet<Node> list1 = new HashSet<Node>();
		HashSet<Node> list2 = new HashSet<Node>();
		// compute the similarity score of the candidate's head
		if (position == 0) {
			for (int i = 0; i < index1; i++)
				list1.add(this.getNodeList().get(i));
			for (int i = 0; i < index2; i++)
				list2.add(other.getNodeList().get(i));
			// compute the similarity score
		} else {
			for (int i = index1; i < this.getNodeList().size(); i++)
				list1.add(this.getNodeList().get(i));
			for (int i = index2; i < other.getNodeList().size(); i++)
				list2.add(other.getNodeList().get(i));
		}

		HashSet<Node> union = new HashSet<Node>(list1);
		HashSet<Node> intersection = new HashSet<Node>(list1);
		union.addAll(list2);
		intersection.retainAll(list2);
		// if(intersection.size() == list1.size()|| intersection.size() == list2.size())
		// return 1;
		result = (double) intersection.size() / union.size();
		return result;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String result = candidateString;
		result = result.replaceAll(TAG, " ");
		result = result.replaceAll(COLLAPSE, "");
		result = result.replaceAll(SPACE, " ");

		return result;
	}

	public String getCan() {
		return candidateString;
	}
}
