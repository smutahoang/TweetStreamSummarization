package l3s.tts.runner;

import l3s.tts.baseline.sumblr.Sumblr;
import l3s.tts.configure.Configure;
import l3s.tts.utils.TweetStream;

public class TestSumblr {
	public static void main(String[] args) {
		new Configure();
		long startTime = System.currentTimeMillis();
		String inputDir = Configure.WORKING_DIRECTORY + "\\input";

		TweetStream stream = new TweetStream(inputDir);
		Sumblr model = new Sumblr(stream);
		System.out.println("start");
		model.process();
		System.out.println("done");
		long endTime = System.currentTimeMillis();
		System.out.printf("%s: %dms\n", "Running time", endTime - startTime);
	}

}
