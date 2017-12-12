package l3s.tts.summary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import l3s.tts.configure.Configure;
import l3s.tts.utils.Candidate;
import l3s.tts.utils.Node;

public class TweetSummarization {
	TweetGraph graph;
	List<Candidate> candidates;

	public TweetSummarization() {
		// TODO Auto-generated constructor stub
		graph = new TweetGraph();
		candidates = new ArrayList<Candidate>();
	}

	public TweetSummarization(TweetGraph graph) {
		this.graph = graph;
		candidates = new ArrayList<Candidate>();
	}

	public void findingCandidates() {
		Set<Node> nodeList = graph.getGraph().vertexSet();
		Iterator<Node> iter = nodeList.iterator();
		// iterate all nodes in the graph
		while (iter.hasNext()) {
			Node currentNode = iter.next();
			// find a valid starting node to keep going
			if (!currentNode.isVSN())
				continue;

			Random rand = new Random();
			int i = 0;
			Set<DefaultWeightedEdge> outgoingEdges = graph.getGraph().outgoingEdgesOf(currentNode);
			// for each VSN, we will sample some valid paths
			while (i < Configure.SAMPLE_NUMBER && i < outgoingEdges.size()) {// iterate to sampling
				Candidate can = new Candidate();
				can.addNode(currentNode);
				while (true) { // find the next node in the path until reaching to the end tokens

					DefaultWeightedEdge nextEdge = sampleMultimomial(outgoingEdges, rand);
					Node nextNode = graph.getGraph().getEdgeTarget(nextEdge);
					if (nextNode.getNodeName().matches(Configure.ENDTOKENS) && graph.getGraph().outgoingEdgesOf(nextNode).size() == 0) {
						if (can.isValidCandidate()) {
							candidates.add(can);
						}
						break;
					}
					can.addNode(nextNode);
					currentNode = nextNode;
					outgoingEdges = graph.getGraph().outgoingEdgesOf(currentNode);
					if (outgoingEdges.size() == 0) {
						break;
					}
					
				}
				i++;
				
			}
		}
	}
	
	/*public void removeDiscard() {
		for(int i=0 ; i<candidates.size(); i++) {
			if(candidates.get(i).getIsDiscard()) {
				candidates.remove(i);
				i--;
			}
		}
	}*/
	
	public void generateSummary() {
		 Collections.sort(candidates, new Comparator<Candidate> (){

			public int compare(Candidate o1, Candidate o2) {
				// TODO Auto-generated method stub
				double score1 = o1.getScore();
				double score2 = o2.getScore();
				if(score1 < score2) return -1;
				else if(score1 > score2) return 1;
				else return 0;
				
			}
			 
		 });
		 int count = 0, i = 0;
		 while(count<Configure.MAX_SUMMARIES && i<candidates.size()) {
			 if(!candidates.get(i).getIsDiscard()) {
				 System.out.println(candidates.get(i));
				 count++;
			 }
			 i++;
		 }
	}

	/**
	 * compute score of a path and combine different paths
	 */
	public void combineTweets() {
		
		int numOfCandidates = candidates.size();
		// iterate all candidates
		for (int j = 0; j < numOfCandidates; j++) {
			if(candidates.get(j).getIsDiscard()) continue;
			// get nodes of current candidates
			List<Node> currNodeList = candidates.get(j).getNodeList();
			List<Candidate> ccCandidate = new ArrayList<Candidate>();
			
			int i = 0;
			// iterate all nodes of the jth candidate
			for (i = 0; i < currNodeList.size(); i++) {
				Node node = currNodeList.get(i);

				if (!node.isCollapse() || i == currNodeList.size() - 1 )
					continue;
				// if this sentence contain a collapsible node, we will find a way to combine
				// aspects of prefixes
				ccCandidate.add(candidates.get(j));
				for (int k = j + 1; k < numOfCandidates; k++) {
					List<Node> kCanNodeList = candidates.get(k).getNodeList();
				if (!kCanNodeList.contains(node) || !candidates.get(k).getIsCollapse() || candidates.get(k).getIsDiscard())
						continue;

					if (candidates.get(k).getNodeList().indexOf(node) == candidates.get(k).getNodeList().size())
						continue;
					double prefixOverlap = candidates.get(j).computeJaccardScore(candidates.get(k), node, 0);
					
					if (prefixOverlap >= Configure.DUPLICATE_PREFIX_THRESOLD && isCombined(ccCandidate, candidates.get(k), node)) {
						ccCandidate.add(candidates.get(k));
					}
					candidates.get(k).setIsDiscard(true);;
				}
				if (ccCandidate.size() == 1)
					continue;

				candidates.get(j).setIsDiscard(true);
				Candidate cc = new Candidate();
				double score = candidates.get(j).computeScore();
				for (int t = 0; t < currNodeList.size(); t++)
					cc.addNode(currNodeList.get(t));
				for (int t = 1; t < ccCandidate.size(); t++) {
					score += ccCandidate.get(t).computeScore();
					int index = ccCandidate.get(t).getNodeList().indexOf(node);
					if (t == ccCandidate.size() - 1)
						cc.addCordinatingConjunction(" ----- and---- ");
					else
						cc.addCordinatingConjunction( " -----, ------");
					for (int h = index + 1; h < ccCandidate.get(t).getNodeList().size(); h++) {
						cc.addNode(ccCandidate.get(t).getNodeList().get(h));
					}
				}
				cc.setScore(score/ccCandidate.size());
				cc.setIsCollapse(true);
				candidates.add(cc);
				

				break;

			}
			
		}
	}
	public boolean isCombined(List<Candidate> candidates, Candidate can, Node n) {
		for(int i = 0; i<candidates.size(); i++) {			
			if(candidates.get(i).computeJaccardScore(can, n, 1) > Configure.DUPLICATE_SUFFIX_THRESOLD) {
				return false;
			}
		}
	
		return true;
	}

	
	public void removeDuplicates() {
		for(int i = 0; i<candidates.size(); i++) {
			if(candidates.get(i).getIsDiscard()) continue;
			for(int j = i+1; j<candidates.size(); j++) {
				if(candidates.get(j).getIsDiscard()) continue;
				if(candidates.get(i).computeJaccardScore(candidates.get(j))>Configure.DUPLICATE_THRESOLD) {
					if(candidates.get(i).computeScore()>candidates.get(j).computeScore())
						candidates.get(j).setIsDiscard(true);
					else
						candidates.get(i).setIsDiscard(true);
				}
			}
		}
	}

	public DefaultWeightedEdge sampleMultimomial(Set<DefaultWeightedEdge> outgoingEdges, Random rand) {
		List<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>(outgoingEdges);
		double[] distribution = new double[edges.size()];
		double sumOfWeights = 0;

		for (int i = 0; i < edges.size(); i++) {
			distribution[i] = graph.getGraph().getEdgeWeight(edges.get(i));
			sumOfWeights = sumOfWeights + distribution[i];
		}
		double sum = 0;
		double x = rand.nextDouble();
		for (int i = 0; i < edges.size(); i++) {
			sum += distribution[i] / sumOfWeights;
			if (sum >= x)
				return edges.get(i);
		}
		return edges.get(edges.size() - 1);
	}

	public List<Candidate> getCandidates() {
		return candidates;
	}
}
