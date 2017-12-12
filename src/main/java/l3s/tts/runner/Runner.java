package l3s.tts.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import l3s.tts.configure.Configure;
import l3s.tts.summary.TweetGraph;
import l3s.tts.summary.TweetSummarization;

public class Runner {

	public static void main(String[] args) {
		new Configure();
		long startTime = System.currentTimeMillis();
		String inputDir = Configure.WORKING_DIRECTORY + "//input";
		String outputDir = Configure.WORKING_DIRECTORY + "//output";

		run(inputDir, outputDir);

		long endTime = System.currentTimeMillis();
		System.out.printf("%s: %dms", "Running time", endTime - startTime);
	}

	public static void run(String inputDir, String outputDir) {
		File inputFolder = new File(inputDir);

		for (File inputFile : inputFolder.listFiles()) {
			System.out.printf("-->File: %s\n", inputFile.getAbsolutePath());
			TweetGraph graph = new TweetGraph();
			start(graph, inputFile, outputDir);

		}
	}

	public static void start(TweetGraph graph, File inputFile, String outputDir) {
		try {
			BufferedReader buff = new BufferedReader(new FileReader(inputFile));
			String line;
			int tweetId = 0;
			// iterate each line in the file
			while ((line = buff.readLine()) != null) {
				tweetId++;
				//if (!(tweetId % Configure.TWEET_WINDOW == 0)) {
					// grow graph					
				graph.addNewTweet(tweetId, line.toLowerCase());
//				
				
			}

			TweetSummarization summarizer = new TweetSummarization(graph);
			summarizer.findingCandidates();

			summarizer.removeDuplicates();
			
			summarizer.combineTweets();
			
			summarizer.generateSummary();
			System.out.println("done");
			buff.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
