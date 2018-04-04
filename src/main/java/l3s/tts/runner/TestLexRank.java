package l3s.tts.runner;

import l3s.tts.baseline.lexrank.LexRank;
import l3s.tts.configure.Configure;
import l3s.tts.summary.IncrementalModel;
import l3s.tts.utils.TweetStream;

public class TestLexRank {
	public static void main(String[] args) {
		new Configure();
		long startTime = System.currentTimeMillis();
		String inputDir = Configure.WORKING_DIRECTORY + "\\input";		

		TweetStream stream = new TweetStream(inputDir);
		LexRank model = new LexRank(stream);
		System.out.println("start");
		model.process();
		System.out.println("done");
		long endTime = System.currentTimeMillis();
		System.out.printf("%s: %dms\n", "Running time", endTime - startTime);
	}

}
