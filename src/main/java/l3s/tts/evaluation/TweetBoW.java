package l3s.tts.evaluation;

public class TweetBoW {
	public String tweetID;
	// public String userID;
	public int[] terms;
	public int nUniqueTerms;
	public int topic;

	public int inferedTopic;
	public double inferedLikelihood;
	public double inferedPosteriorProb; // i.e., prob{inferedTopic|words)
}
