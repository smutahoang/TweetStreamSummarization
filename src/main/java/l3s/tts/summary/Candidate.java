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

	/**
	 * 
	 * @return score of the path
	 */
	public void computeScore1() {

		if (nodeList.size() != 0) {
			List<int[]> overlap = nodeList.get(0).getTweetPosPairs();

			switch (Configure.SCORING_FUNCTION) {
			case GAIN_REDUNDANCY_ONLY:
				for (int i = 1; i < nodeList.size(); i++) {
					overlap = getOverlapIntersection(overlap, nodeList.get(i).getTweetPosPairs());
					score = score + (double) overlap.size();
				}
				break;
			case GAIN_WEIGHTED_REDUNDANCY_BY_LEVEL:
				for (int i = 1; i < nodeList.size(); i++) {
					overlap = getOverlapIntersection(overlap, nodeList.get(i).getTweetPosPairs());
					score = score + (double) (overlap.size() * (i + 1));
				}
				break;
			case GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL:
				for (int i = 1; i < nodeList.size(); i++) {
					overlap = getOverlapIntersection(overlap, nodeList.get(i).getTweetPosPairs());
					score = score + (double) overlap.size() * Math.log(i + 1);
				}
				break;
			default:
				break;
			}
		}

	}

	// compute score of a path according to the Filippova paper
	public double computeScore() {
		score = 0;
		for (int i = 0; i < nodeList.size() - 1; i++) {
			int freqOfINode = nodeList.get(i).getTweetPosPairs().size();
			int freqOfIPlus1Node = nodeList.get(i + 1).getTweetPosPairs().size();
			score += (freqOfINode + freqOfIPlus1Node) / (getOverlappingDistance(nodeList.get(i).getTweetPosPairs(),
					nodeList.get(i + 1).getTweetPosPairs()) * freqOfINode * freqOfIPlus1Node);
		}
		//score = score/Math.log(nodeList.size());
		return score;
	}

	// get overlapping distance between two nodes
	public static double getOverlappingDistance(List<int[]> left, List<int[]> right) {
		double diff = 0;
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
					if (eright[1] > eleft[1]) {
						diff += (double) 1 / (eright[1] - eleft[1]);
						//System.out.println(eright[1] + "\t" + eleft[1]);
						pointer = j;
						break;
					}

				} else if (eright[0] > eleft[0])
					break;
				++j;
			}
			++i;
		}
		return diff;
	}

	public void computeAdjustScore(double score) {
		// if(nodeList.size() == 1) this.score = 0;
		if (nodeList.size() != 0) {
			this.score = score / nodeList.size();
		}
	}

	/**
	 * @param left:
	 *            overlapping list of left node
	 * @param right:
	 *            overlapping list of right node
	 * @return result: intersection of 2 overlapping list
	 */
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
					// eright[1] = eleft[1];
				} else if (eright[0] > eleft[0])
					break;
				++j;
			}
			++i;
		}
		return result;
	}

	public boolean isValidCandidate() {
		boolean isGood = false;
		candidateString = candidateString.trim();
		if (candidateString.matches(Configure.VALID_CANDIDATE1)) {
			isGood = true;
		} else if (!candidateString.matches(Configure.VALID_CANDIDATE2)
				&& candidateString.matches(Configure.VALID_CANDIDATE3)) {
			isGood = true;
		} else if (candidateString.matches(Configure.VALID_CANDIDATE4)) {
			isGood = true;
		} else if (candidateString.matches(Configure.VALID_CANDIDATE5)) {
			isGood = true;
		} else if (candidateString.matches(Configure.VALID_CANDIDATE6)) {
			isGood = true;
		}
		// conditions to remove some invalid candidates (ending with to, in, verb,
		// coordinating conjunction, Wh-determiner,
		// personal pronoun, determiner(a, an , the), ,)
		if (candidateString.contains(" ")) {
			String last = candidateString.substring(candidateString.lastIndexOf(32), candidateString.length());
			if (last.matches(Configure.VALID_CANDIDATE7)) {
				isGood = false;
			}
		}
		return isGood;
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
