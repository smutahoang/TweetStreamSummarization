package l3s.tts.runner;

import l3s.tts.configure.Configure;
import l3s.tts.summary.IncrementalModel;
import l3s.tts.utils.TweetStream;

public class TestDiversifiedRanking {
	public static void main(String[] args) {
		new Configure();
		long startTime = System.currentTimeMillis();
		String inputDir = Configure.WORKING_DIRECTORY + "/input";
		String outputDir = Configure.WORKING_DIRECTORY + "/output/inc";

		TweetStream stream = new TweetStream(inputDir);		
		IncrementalModel model = new IncrementalModel(stream, outputDir);
		System.out.println("start");
		model.run();
		System.out.println("done");
		long endTime = System.currentTimeMillis();
		System.out.printf("%s: %dms\n", "Running time", endTime - startTime);
	}

}
