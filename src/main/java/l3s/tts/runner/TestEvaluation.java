package l3s.tts.runner;

import l3s.tts.configure.Configure;
import l3s.tts.evaluation.TwitterLDA;
import l3s.tts.utils.TweetStream;

public class TestEvaluation {
	public static void main(String[] args) {
		new Configure();
		long startTime = System.currentTimeMillis();
		String inputDir = Configure.WORKING_DIRECTORY + "/input";
		String outputDir = Configure.WORKING_DIRECTORY + "/output/evaluation";

		TweetStream stream = new TweetStream(inputDir);
		TwitterLDA evaluator = new TwitterLDA(stream, 10, outputDir);
		System.out.println("start");
		evaluator.genSummary();
		System.out.println("done");
		long endTime = System.currentTimeMillis();
		System.out.printf("%s: %dms\n", "Running time", endTime - startTime);
	}

}
