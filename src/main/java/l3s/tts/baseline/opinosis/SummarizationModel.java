package l3s.tts.baseline.opinosis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.util.MathUtil;

import cmu.arktweetnlp.Tagger.TaggedToken;
import l3s.tts.configure.Configure;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetPreprocessingUtils;

public class SummarizationModel {
	protected TweetPreprocessingUtils preprocessingUtils;
	protected SimpleDirectedGraph<Node, DefaultWeightedEdge> graph;
	protected HashMap<String, Node> wordNodeMap;
	protected List<Candidate> candidates;
	String mAnchor = "";
    double beforeAttachGain = 0.0;
    double mAnchorPathScore = 0.0;
    private int mAnchorPathLen = 0;
    HashSet<Candidate> shortlisted = new HashSet<Candidate>();
    HashMap<String, Candidate> ccList = new HashMap<String, Candidate>();

	protected Random rand;

	public SummarizationModel() {
		// TODO Auto-generated constructor stub
		wordNodeMap = new HashMap<String, Node>();
		graph = new SimpleDirectedGraph<Node, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		preprocessingUtils = new TweetPreprocessingUtils();
		candidates = new ArrayList<Candidate>();
		// stopWords = preprocessingUtils.getStopWords();

	}

	/**
	 * add a new tweet into graph
	 * 
	 * @param tweet:
	 *            new tweet
	 * @param index:
	 *            index of tweet
	 */
	public void addNewTweet(Tweet tweet, int index) {
		// iterate all tagged words in the tweet
		List<TaggedToken> tokens = tweet.getTaggedTokens();
		
		boolean isPrevNodeNew = true;
		boolean isCurNodeNew = true;
		Node preNode = null;
		Node curNode = null;

		for (int i = 0; i < tokens.size(); i++) {
			StringBuilder builder = new StringBuilder(tokens.get(i).token);
			builder.append("/");
			builder.append(tokens.get(i).tag.toLowerCase());
			String nodeString = builder.toString();
			
			// if graph contained this node, add a new pair of tweetId - word position
			if (wordNodeMap.containsKey(nodeString)) {
				isCurNodeNew = false;

				curNode = wordNodeMap.get(nodeString);
				curNode.addNewTweetPosPair(index, i);

			} else { // if graph doesnt contain this node
				curNode = new Node(rand); // create a new node
				curNode.setNodeName(nodeString);

				curNode.addNewTweetPosPair(index, i); // add a pair of sentence Id and position of word in the node
				graph.addVertex(curNode); // add new node into graph

				wordNodeMap.put(nodeString, curNode);
				isCurNodeNew = true;
			}

			if (isPrevNodeNew || isCurNodeNew) { // if current node or previous node is a new node, add a new edge into
													// graoh
				// check if current node is the first node in the graph
				if (preNode != null && !curNode.equals(preNode) && canAdd(preNode)) {
					graph.addEdge(preNode, curNode);

				}
			} else {
				DefaultWeightedEdge edge = graph.getEdge(preNode, curNode);
				if (edge == null) {
					if (!curNode.equals(preNode) && canAdd(preNode)) {// && !preNode.getNodeName().matches(Configure.ENDTOKENS))
						graph.addEdge(preNode, curNode);

					}
				} else {
					double weight = graph.getEdgeWeight(edge) + tweet.getWeight();
					graph.setEdgeWeight(edge, weight);

				}
			}
			preNode = curNode;
			isPrevNodeNew = isCurNodeNew;

		}
	}
	
	 private boolean canAdd(Node prevVertex) {
	        if (prevVertex.getNodeName().matches(Configure.STOP_ADDING)) {
	            return false;
	        }
	        return true;
	    }

	public void findingCandidates() {
		
		Set<Node> nodeList = graph.vertexSet();
		Iterator<Node> iter = nodeList.iterator();
		// iterate all nodes in the graph
		while (iter.hasNext()) {
			Node node = iter.next();
			
			// find a valid starting node to keep going
			if (!node.isVSN()) {
				continue;
			}
			double score = 0.0;
		//	System.out.println("-->"+node.getNodeName());
			this.traverse(node, node.getTweetPosPairs(), node.getNodeName(), score, 1, false, false);
		}
	}
	protected List<Candidate> getFinalSentences() {
        ArrayList<Candidate> temp = new ArrayList<Candidate>();
        ArrayList<Candidate> shortlistedFinal = new ArrayList<Candidate>();
        System.out.println("#candidates: "+shortlisted.size());
        if (this.shortlisted.size() <= 0) {
            return shortlistedFinal;
        }
        temp.addAll(this.removeDuplicates(this.shortlisted, false));
        Collections.sort(temp, new SummarySorter());
        if ((double)temp.size() > Configure.MAX_SUMMARIES) {
            shortlistedFinal.add((Candidate)temp.get(0));
            int i = 1;
            while (i < temp.size() && (double)shortlistedFinal.size() < Configure.MAX_SUMMARIES) {
                Candidate a = (Candidate)temp.get(i - 1);
                Candidate b = (Candidate)temp.get(i);
                shortlistedFinal.add(b);
                ++i;
            }
        } else {
            shortlistedFinal.addAll(temp);
        }
        return shortlistedFinal;
    }
	private boolean traverse(Node x, List<int[]> overlapList, String str, double pathScore, int pathLength,
			boolean isCollapsedCandidate, boolean overlapSame) {
		if (!this.shouldContinueTraverse(x, overlapList, pathLength, pathScore)) {
			return true;
		}
		if (this.isVEN(x, pathLength, isCollapsedCandidate)
				&& this.processVEN(x, pathLength, overlapList, isCollapsedCandidate, str, pathScore)) {
			return true;
		}
		this.processNext(x, str, overlapList, pathScore, pathLength, isCollapsedCandidate);
		return true;
	}

	public boolean isVEN(Node x, int pathLength, boolean isCollapsedCandidate) {
		if (x.isEndToken()) {
			return true;
		}
		if (graph.outDegreeOf(x) <= 0) {
			return true;
		}
		return false;
	}

	public boolean shouldContinueTraverse(Node x, List<int[]> overlapSoFar, int pathLength, double score) {
		if (pathLength >= Configure.P_MAX_SENT_LENGTH) {
			return false;
		}
		if (score == Double.NEGATIVE_INFINITY) {
			return false;
		}
		if (overlapSoFar.size() < Configure.MIN_REDUNDANCY && !x.isEndToken()) {
			return false;
		}
		return true;
	}


	private void processNext(Node x, String str, List<int[]> overlapList, double currentPathScore, int pathLen,
			boolean isCollapsedPath) {
		Set<DefaultWeightedEdge> outgoing = graph.outgoingEdgesOf(x);
		if (outgoing != null && outgoing.size() > 0) {
			Iterator<DefaultWeightedEdge> xEdges = outgoing.iterator();
			boolean doMore = true;
			while (xEdges.hasNext() && doMore) {
				DefaultWeightedEdge xEdge = xEdges.next();
				Node y = graph.getEdgeTarget(xEdge);
				String yNodeName = y.getNodeName();
				List<int[]> currOverlapList = getNodeOverlap(overlapList, y.getTweetPosPairs());
				if (currOverlapList.size() <= 0)
					continue;
				int newPathLen = pathLen + 1;
				double newPathScore = this.computeScore(currentPathScore, currOverlapList, newPathLen);
				if (Configure.IS_COLLAPSE && pathLen >= Configure.ATTACHMENT_AFTER && !isCollapsedPath
						&& currOverlapList.size() <= overlapList.size()
						&& x.getNodeName().matches(Configure.OVERLAP_NODE)) {
					boolean success = doCollapse(x, currOverlapList, newPathScore, currentPathScore, str,
							overlapList, pathLen, isCollapsedPath);
					if (success)
						continue;
					String strTemp = String.valueOf(str) + " " + y.getNodeName();
					doMore = this.traverse(y, currOverlapList, strTemp, newPathScore, newPathLen, isCollapsedPath,
							false);
					continue;
				}
				String strTemp = String.valueOf(str) + " " + yNodeName;
				doMore = this.traverse(y, currOverlapList, strTemp, newPathScore, pathLen + 1, isCollapsedPath, false);
			}
		}
	}
	
	private List<Node> getNodeList(String sent) {
        String[] tokens = sent.split("\\s+");
        ArrayList<Node> l = new ArrayList<Node>();
        String[] arrstring = tokens;
        int n = arrstring.length;
        int n2 = 0;
        while (n2 < n) {
            Node n3;
            String token = arrstring[n2];
            if (token.matches(".*(/nn|/jj|/vb[a-s]).*") && (n3 = wordNodeMap.get(token)) != null) {
                l.add(n3);
            }
            ++n2;
        }
        return l;
    }
	
	private Candidate remove(Candidate currSentence, Candidate best) {
        
        if (best.gain < currSentence.gain && best.level <= currSentence.level) {
            best.discard = true;
            best = currSentence;
        } else {
            currSentence.discard = true;
        }
        return best;
    }
	
	public double computeCandidateSimScore(Candidate s1, Candidate s2) {
        List<Node> l1 = s1.theNodeList;
        List<Node> l2 = s2.theNodeList;
        HashSet<Node> union = new HashSet<Node>(l1);
        HashSet<Node> intersect = new HashSet<Node>(l1);
        union.addAll(l2);
        intersect.retainAll(l2);
        double overlap = (double)intersect.size() / (double)union.size();
        return overlap;
    }
	private HashSet<Candidate> removeDuplicates(HashSet<Candidate> set, boolean isIntermediate) {
        HashSet<Candidate> finalSentences = new HashSet<Candidate>();
        if (Configure.TURN_ON_DUP_ELIM) {
            ArrayList<Candidate> list = new ArrayList<Candidate>(set);
            int i = 0;
            while (i < list.size()) {
                Candidate info = list.get(i);
                info.discard = false;
                List<Node> nl = this.getNodeList(info.sent);
                info.theNodeList = nl;
                ++i;
            }
          
            int a = 0;
            while (a < list.size()) {
                if (!list.get((int)a).discard) {
                    
                    Candidate best = list.get(a);
                    int b = 0;
                    while (b < list.size()) {
                        if (!list.get((int)b).discard && a != b) {
                            Candidate currSentence = list.get(b);
                            double overlap = this.computeCandidateSimScore(currSentence, best);
                            if (isIntermediate) {
                                if (overlap > Configure.DUPLICATE_COLLAPSE_THRESHOLD) {
                                    best = this.remove(currSentence, best);
                                }
                            } else if (overlap > Configure.DUPLICATE_THRESOLD) {
                                best = this.remove(currSentence, best);
                            }
                        }
                        ++b;
                    }
                    finalSentences.add(best);
                    best.discard = true;
                }
                ++a;
            }
        } else {
            finalSentences = set;
        }
        return finalSentences;
    }
	 private boolean processFound() {
	        boolean success = false;
	        Collection<Candidate> temp = this.ccList.values();
	        HashSet<Candidate> collapsed = new HashSet<Candidate>(temp);
	        collapsed = this.removeDuplicates(collapsed, true);
	        int i = 0;
	        if (collapsed.size() > 1) {
	            double overallgains = 0.0;
	            double allscores = this.mAnchorPathScore;
	            double allgains = this.beforeAttachGain;
	            int alllevels = this.mAnchorPathLen;
	            StringBuffer buffer = new StringBuffer(this.mAnchor);
	            ArrayList<int[]> sentList = new ArrayList<int[]>();
	            for (Candidate theInfo : collapsed) {
	                overallgains += theInfo.gain;
	                allgains += theInfo.localgain;
	                allscores += theInfo.rawscore;
	                alllevels += theInfo.level;
	                sentList.addAll(theInfo.sentList);
	                if (i > 0 && i == collapsed.size() - 1) {
	                    buffer.append(" and ");
	                } else if (i > 0) {
	                    buffer.append(" , ");
	                } else {
	                    buffer.append(" ");
	                }
	                buffer.append(theInfo.sent);
	                ++i;
	            }
	            if (this.ccList.size() > 1) {
	                double overallGain = overallgains / (double)this.ccList.size();
	                Candidate can = new Candidate(overallGain, buffer.toString(), sentList, alllevels);
	                this.shortlisted.add(can);
	                //System.out.println(can);
	                success = true;
	            }
	        }
	        this.ccList.clear();
	        this.mAnchor = "";
	        this.beforeAttachGain = 0.0;
	        this.mAnchorPathScore = 0.0;
	        this.mAnchorPathLen = 0;
	        return success;
	    }
	
	private boolean doCollapse(Node x, List<int[]> YintersectX, double pathscore, double prevPathScore, String str, List<int[]> overlapList, int level, boolean concatOn) {
        this.mAnchor = str;
        this.mAnchorPathScore = prevPathScore;
        this.mAnchorPathLen = level;
        Set<DefaultWeightedEdge> edges = graph.outgoingEdgesOf(x);
        if (edges != null && edges.size() > 1) {
            for (DefaultWeightedEdge cEdgeOfX : edges) {
                Node cY = graph.getEdgeTarget(cEdgeOfX);
                String cYNodeName = cY.getNodeName();
                List<int[]> cYintersectX = this.getNodeOverlap(overlapList, cY.getTweetPosPairs());
                int newLevel = level + 1;
                double newPathScore = this.computeScore(pathscore, cYintersectX, newLevel);
                if (cYintersectX.size() < Configure.MIN_REDUNDANCY) continue;
                this.traverse(cY, cYintersectX, "xx " + cYNodeName, newPathScore, newLevel, true, false);
            }
        }
        concatOn = false;
        return this.processFound();
    }
	
	public List<int[]> getNodeOverlap(List<int[]> left, List<int[]> right) {
        ArrayList<int[]> l3 = new ArrayList<int[]>();
       
        int pointer = 0;
        int i = 0;
        while (i < left.size()) {
            int[] eleft = left.get(i);
            if (pointer > right.size()) break;
            int j = pointer;
            while (j < right.size()) {
                int[] eright = right.get(j);
                if (eright[0] == eleft[0]) {
                    if (eright[1] > eleft[1] && Math.abs(eright[1] - eleft[1]) <= Configure.PERMISSABLE_GAP) {
                        l3.add(eright);
                        pointer = j + 1;
                        break;
                    }
                    eright[1]= eleft[1];
                } else if (eright[0] > eleft[0]) break;
                ++j;
            }
            ++i;
        }
      
        return l3;
    }
	
	public double computeAdjustedScore(double score, int level) {
        double oGain = score;
        if (Configure.NORMALIZE_OVERALLGAIN) {
            oGain /= (double)level;
        }
        return oGain;
    }
	
	
	 public boolean isValidCandidate(String str) {
	        boolean isGood = false;
	        if (str.matches(Configure.VALID_CANDIDATE1)) {
	            isGood = true;
	        } else if (!str.matches(Configure.VALID_CANDIDATE2) && str.matches(Configure.VALID_CANDIDATE3)) {
	            isGood = true;
	        } else if (str.matches(Configure.VALID_CANDIDATE4)) {
	            isGood = true;
	        } else if (str.matches(Configure.VALID_CANDIDATE5)) {
	            isGood = true;
	        } else if (str.matches(Configure.VALID_CANDIDATE6)) {
	            isGood = true;
	        }
	        String last = str.substring(str.lastIndexOf(32), str.length());
	        if (last.matches(Configure.VALID_CANDIDATE7)) {
	            isGood = false;
	        }
	        return isGood;
	    }
	boolean processVEN(Node x, int pathLength, List<int[]> theNodeList, boolean isCollapsedCandidate, String str,
			double pathScore) {
		String theCandidateStr = str;
		int thePathLen = pathLength;
		double theScore = pathScore;
		if (x.isEndToken()) {
			theCandidateStr = theCandidateStr.substring(0, theCandidateStr.lastIndexOf(" "));
			thePathLen = pathLength - 1;
		}
		double theAdjustedScore = computeAdjustedScore(theScore, thePathLen);
		if (this.isValidCandidate(String.valueOf(this.mAnchor) + " " + theCandidateStr)) {
			if (!isCollapsedCandidate) {
				Candidate can = new Candidate(theAdjustedScore, theCandidateStr, theNodeList, thePathLen);
				this.shortlisted.add(can);
				
				// print can
				//System.out.println("--> "+can.toString());
			} else {
				Candidate cc = this.ccList.get(theCandidateStr);
				int ccPathLength = thePathLen - this.mAnchorPathLen;
				double ccPathScore = theScore - this.mAnchorPathScore;
				if (cc != null) {
					cc.gain = Math.max(cc.gain, theAdjustedScore);
				} else {
					cc = new Candidate(theAdjustedScore, theCandidateStr, theNodeList, ccPathLength, ccPathScore,
							0.0 - this.beforeAttachGain);
					this.ccList.put(theCandidateStr, cc);
				}
				return true;
			}
		}
		return false;
	}


	// original solution
	public double computeScore(double currentScore, List<int[]> currentOverlap, int pathLength) {

		double score = 0;
		switch (Configure.SCORING_FUNCTION) {
		case GAIN_REDUNDANCY_ONLY:
			score = currentScore + currentOverlap.size();
			break;
		case GAIN_WEIGHTED_REDUNDANCY_BY_LEVEL:
			score = currentScore + pathLength * currentOverlap.size();
			break;
		case GAIN_WEIGHTED_REDUNDANCY_BY_LOG_LEVEL:
			//score = pathLength > 1 ? currentScore + (double)overlapSize * MathUtil.getLog2(pathLength) : currentScore + (double)overlapSize;
			score = currentScore + (Math.log(pathLength) / Math.log(2)) * currentOverlap.size(); // log_2(pathLength)
			break;
		}
		//System.out.printf("score: %f, ", currentScore);
		return score;
	}


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
		// System.out.printf("%d, ", left.size());
		return result;
	}

	public void printCandidates(Formatter format) {
		for (int i = 0; i < candidates.size(); i++) {
			format.format("%d. %s\n", i + 1, candidates.get(i));
		}

	}


	public boolean shouldContinue(List<int[]> currentOverlap, int pathLength) {
		if (pathLength >= Configure.P_MAX_SENT_LENGTH) {
			return false;
		}

		if (currentOverlap.size() < Configure.MIN_REDUNDANCY) {// && !this.isEndToken(x)) {
			return false;
		}
		return true;
	}

	public void printGraph() {
		Set<Node> vertexSet = graph.vertexSet();
		Iterator<Node> iter = vertexSet.iterator();
		int i = 0;
		while (iter.hasNext()) {
			Node n = iter.next();
			Set<DefaultWeightedEdge> edgesOfNode = graph.edgesOf(n);
			for (DefaultWeightedEdge edge : edgesOfNode) {
				Node sourceNode = graph.getEdgeSource(edge);
				Node targetNode = graph.getEdgeTarget(edge);
				if (targetNode.getNodeName().equals(n.getNodeName()))
					continue;
				double weight = graph.getEdgeWeight(edge);
				System.out.printf("%d. %s --> %s: %.1f\n", i, sourceNode.getNodeName(), targetNode.getNodeName(),
						weight);
				i++;
			}
		}

	}

}
