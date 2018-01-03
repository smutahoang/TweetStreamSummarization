package l3s.tts.utils;

import java.util.HashSet;
import java.util.List;

import cmu.arktweetnlp.Tagger.TaggedToken;
import l3s.tts.summary.Candidate;
import l3s.tts.summary.Node;

public class PrintData {
	static TweetPreprocessingUtils preProcessingUtils = new TweetPreprocessingUtils();
	// get all candidates containing a node
	public static HashSet<Candidate> getCandidatesContainingNode(List<Candidate> cans, Node node) {
		HashSet<Candidate> result = new HashSet<Candidate>();
		for (int i = 0; i < cans.size(); i++) {
			if (cans.get(i).getNodeList().contains(node)) {
				result.add(cans.get(i));
				System.out.printf("-->%s\n", cans.get(i));
			}
		}

		return result;

	}
	// get all tweets contain nodes
	public static HashSet<Tweet> getTweetsContainingNode(List<Tweet> tweets, Node node) {
		HashSet<Tweet> result = new HashSet<Tweet>();
		String tagNode = node.getNodeName().substring(node.getNodeName().indexOf("/")+1);
		String tokenNode = node.getNodeName().substring(0, node.getNodeName().indexOf("/"));
		for(int i = 0; i<tweets.size(); i++) {
			List<TaggedToken> taggedTokens = tweets.get(i).getTaggedTokens();
			for(int j= 0; j<taggedTokens.size(); j++) {
				if(taggedTokens.get(j).tag.equals(tagNode) && taggedTokens.get(j).token.equals(tokenNode)) {
					result.add(tweets.get(i));
					System.out.printf("-->%s\n", tweets.get(i));
					break;
				}
			}
		}
		
		return result;
	}
}
