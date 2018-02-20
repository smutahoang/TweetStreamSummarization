package l3s.tts.summary;

import java.util.ArrayList;
import java.util.List;

public class RandomWalk {
	// private Node startingNode;
	private List<Node> visitedNodes;

	public RandomWalk(Node startingNode) {
		// this.startingNode = _startingNode;
		visitedNodes = new ArrayList<Node>();
		visitedNodes.add(startingNode);
	}

	public Node getStartingNode() {
		return visitedNodes.get(0);
	}

	public int length() {
		return visitedNodes.size();
	}

	public void addVisitedNode(Node node) {
		visitedNodes.add(node);
	}

	public List<Node> getVisitedNodes() {
		return visitedNodes;
	}
}
