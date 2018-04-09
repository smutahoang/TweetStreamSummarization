package l3s.tts.runner;

import l3s.tts.baseline.opinosis.OpinosisSummarization;
import l3s.tts.configure.Configure;
import l3s.tts.utils.TweetStream;

public class TestOpinosis {
	public static void main(String[] args) {
		new Configure();
		long startTime = System.currentTimeMillis();
		String inputDir = Configure.WORKING_DIRECTORY + "\\input";
		
		TweetStream stream = new TweetStream(inputDir);
		OpinosisSummarization model = new OpinosisSummarization(stream);
		System.out.println("start");
		model.run();
		
		long endTime = System.currentTimeMillis();
		System.out.printf("%s: %dms\n", "Running time", endTime - startTime);
	}

	
}
