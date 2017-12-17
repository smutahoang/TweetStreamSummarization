package l3s.tts.summary;

import java.util.ArrayList;

import l3s.tts.configure.Configure;
import l3s.tts.utils.Tweet;
import l3s.tts.utils.TweetStream;

public class IncrementalModel extends TweetGraph {
	TweetStream stream;
	String outputDir;

	public IncrementalModel(TweetStream stream, String outputDir) {
		super();
		this.stream = stream;
		this.outputDir = outputDir;
		candidates = new ArrayList<Candidate>();

	}

	public void run() {
		Tweet tweet = null;
		int nOfTweets = 0;
		while ((tweet = stream.getTweet()) != null) {
			if (tweet.isReTweet())
				continue; // ignore retweets

			addNewTweet(tweet, nOfTweets);
			nOfTweets++;

			if (nOfTweets == Configure.TWEET_WINDOW) {
				generateSummary();
				// printCandidates();
				System.exit(-1);
			} // if it is time to update
			else if (nOfTweets % Configure.TWEET_WINDOW == 0) {
				update();
				reGenerateSummary();
			}

		}
		// generateSummary();

	}
	public void generateSummary() {
		System.out.println(">>>>>>>>>>>>Find valid paths");
		findingCandidates();
		System.out.println(">>>>>>>>>>>>Remove duplicates");
		removeDuplicates();
		System.out.println(">>>>>>>>>>>>Combine valid paths");
		combineTweets();
		ArrayList<Candidate> summary = sortAndGetHighScoreSummaries();
		for(Candidate can: summary)
			System.out.println(can);
		System.out.println("Number of sentences in the summary: "+summary.size());
	}
	public void reGenerateSummary() {
		// findingCandidates();

	}

	public boolean isTimeToUpdate() {

		return false;
	}

	public void update() {

	}

}
