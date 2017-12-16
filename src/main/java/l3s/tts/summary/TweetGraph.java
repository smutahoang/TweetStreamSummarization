package l3s.tts.summary;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import cmu.arktweetnlp.Tagger.TaggedToken;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetPreprocessingUtils;

public class TweetGraph {
	private TweetPreprocessingUtils preprocessingUtils;
	SimpleDirectedGraph<Node, DefaultWeightedEdge> graph;
	HashMap<String, Node> wordNodeMap;
	
	public TweetGraph() {
		// TODO Auto-generated constructor stub
		wordNodeMap = new HashMap<String, Node>();
		graph = new SimpleDirectedGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		preprocessingUtils = new TweetPreprocessingUtils();
	}
	/**
	 * add a new tweet into graph
	 * @param tweet: new tweet
	 * @param index: index of tweet
	 */
	public void addNewTweet(Tweet tweet, int index) {
		// iterate all tagged words in the tweet
		List<TaggedToken> tokens = tweet.getTaggedTokens(preprocessingUtils);
		boolean isPrevNodeNew = true;
		boolean isCurNodeNew = true;
		Node preNode = null;
		Node curNode = null;
		for (int i = 0; i < tokens.size(); i++) {
			
			// if graph contained this node
			if(wordNodeMap.containsKey(tokens.get(i).token+"/"+tokens.get(i).tag)) {
				isCurNodeNew = false;
				
				curNode = wordNodeMap.get(tokens.get(i).token+"/"+tokens.get(i).tag);
				curNode.addNewTweetPosPair(index, i);
				
			} else { // if graph doesnt contain this node
				curNode = new Node(); // create a new node
				curNode.setNodeName(tokens.get(i).token+"/"+tokens.get(i).tag);
				
				curNode.addNewTweetPosPair(index, i); // add a pair of sentence Id and position of word in the node
				graph.addVertex(curNode); // add new node into graph
				wordNodeMap.put(tokens.get(i).token+"/"+tokens.get(i).tag, curNode);
				isCurNodeNew = true;
			}
			if(isPrevNodeNew || isCurNodeNew) { // if current node or previous node is a new node, add a new edge into graoh
				// check if current node is the first node in the graph
				if(preNode!= null &&!curNode.equals(preNode)) {//&& !preNode.getNodeName().matches(Configure.ENDTOKENS)) {
					graph.addEdge(preNode, curNode);
				}
			} else {
				DefaultWeightedEdge edge = graph.getEdge(preNode, curNode);
				if(edge == null) {
					if(!curNode.equals(preNode))// && !preNode.getNodeName().matches(Configure.ENDTOKENS))
						graph.addEdge(preNode, curNode);
				} else {
					double weight = graph.getEdgeWeight(edge) + 1.0;
					graph.setEdgeWeight(edge, weight);
				}
			}
			preNode = curNode;
			isPrevNodeNew = isCurNodeNew;
		}
	}
	
	public void printGraph() {
		Set<Node> vertexSet = graph.vertexSet();
		Iterator<Node> iter = vertexSet.iterator();
		int i = 0;
		while(iter.hasNext()) {
			Node n = iter.next();
			Set<DefaultWeightedEdge> edgesOfNode = graph.edgesOf(n);
			for(DefaultWeightedEdge edge: edgesOfNode) {
				Node sourceNode = graph.getEdgeSource(edge);
				Node targetNode = graph.getEdgeTarget(edge);
				if(targetNode.getNodeName().equals(n.getNodeName())) continue;
				double weight = graph.getEdgeWeight(edge); 
				System.out.printf("%d. %s --> %s: %.1f\n", i,  sourceNode, targetNode, weight); 
				i++;
			}
		}
		
	}
}
