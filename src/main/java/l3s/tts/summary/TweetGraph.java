package l3s.tts.summary;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import l3s.tts.configure.Configure;
import l3s.tts.utils.Node;

public class TweetGraph {
	private SimpleDirectedWeightedGraph<Node, DefaultWeightedEdge> directedGraph;
	private Map<String, Long> setOfTweets;
	private HashMap<String, Node> wordNodeMap;
	public TweetGraph() {
		// TODO Auto-generated constructor stub
		directedGraph = new SimpleDirectedWeightedGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		setOfTweets = new HashMap<String, Long>();
		wordNodeMap = new HashMap<String, Node>();
	}

	public SimpleDirectedWeightedGraph<Node, DefaultWeightedEdge> getGraph() {
		return directedGraph;
	}

	public void addNewTweet(int tweetId, String tweet) {
		setOfTweets.put(tweet, System.currentTimeMillis());
		
		String[] words = tweet.split(" ");
		boolean isPrevVertexNew = true;
		boolean isCurVertexNew = true;
		Node preVertex = null;
		Node curVertex = null;
		for (int i = 0; i < words.length; i++) {
			String word = words[i].trim();
			if(word.length() == 0) continue;
			// if graph contained this node
			if(wordNodeMap.containsKey(word)) {
				isCurVertexNew = false;
				
				curVertex = wordNodeMap.get(word);
				curVertex.addNewTweetPosPair(tweetId, i, tweet);
				
			} else {
				curVertex = new Node();
				curVertex.setNodeName(word);
				
				curVertex.addNewTweetPosPair(tweetId, i, tweet);
				directedGraph.addVertex(curVertex);
				wordNodeMap.put(word, curVertex);
				isCurVertexNew = true;
			}
			if(isPrevVertexNew || isCurVertexNew) {
				// stop adding new edge if we reach to an ending token
				if(preVertex!= null &&!curVertex.equals(preVertex)&& !preVertex.getNodeName().matches(Configure.ENDTOKENS)) {
					directedGraph.addEdge(preVertex, curVertex);
				}
			} else {
				DefaultWeightedEdge edge = directedGraph.getEdge(preVertex, curVertex);
				if(edge == null) {
					if(!curVertex.equals(preVertex)&&!preVertex.getNodeName().matches(Configure.ENDTOKENS))
						directedGraph.addEdge(preVertex, curVertex);
				} else {
					double weight = directedGraph.getEdgeWeight(edge) + 1.0;
					directedGraph.setEdgeWeight(edge, weight);
				}
			}
			preVertex = curVertex;
			isPrevVertexNew = isCurVertexNew;
		}
	}
	
	public Set<Node> getNodeList() {
		return directedGraph.vertexSet();
	}
	public Set<DefaultWeightedEdge> getEdgeList() {
		return directedGraph.edgeSet();
	}
	public void printGraph() {
		Set<Node> vertexSet = directedGraph.vertexSet();
		Iterator<Node> iter = vertexSet.iterator();
		int i = 0;
		while(iter.hasNext()) {
			Node n = iter.next();
			Set<DefaultWeightedEdge> edgesOfNode = directedGraph.edgesOf(n);
			for(DefaultWeightedEdge edge: edgesOfNode) {
				Node sourceNode = directedGraph.getEdgeSource(edge);
				Node targetNode = directedGraph.getEdgeTarget(edge);
				if(targetNode.getNodeName().equals(n.getNodeName())) continue;
				double weight = directedGraph.getEdgeWeight(edge); 
				System.out.printf("%d. %s --> %s: %.1f\n", i,  sourceNode, targetNode, weight); 
				i++;
			}
		}
		
	}

}
