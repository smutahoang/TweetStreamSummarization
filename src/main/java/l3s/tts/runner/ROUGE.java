package l3s.tts.runner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import l3s.tts.configure.Configure;
import l3s.tts.utils.TweetPreprocessingUtils;

public class ROUGE {
	// private int ngram;
	private Set<String> inferSum;
	private Set<String> generatedSum;
	private TweetPreprocessingUtils tweetPreprocessingUtils;
	private int topK;

	public ROUGE(int ngram, int _topK, File inferPath, File generatedPath) {
		// TODO Auto-generated constructor stub
		// this.ngram = ngram;
		this.topK = _topK;
		inferSum = getNGraph(inferPath, ngram);
		generatedSum = getNGraph(generatedPath, ngram);
		tweetPreprocessingUtils = new TweetPreprocessingUtils();
	}

	private Set<String> getNGraph(File inputFile, int nGram) {
		try {
			BufferedReader buff = null;
			String line = null;
			Set<String> nGramList = new HashSet<String>();
			buff = new BufferedReader(new FileReader(inputFile));
			int nTweets = 0;
			while ((line = buff.readLine()) != null) {
				List<String> terms = tweetPreprocessingUtils.extractTermInTweet(line);
				for (String term : terms) {
					nGramList.add(term);
				}
				nTweets++;
				if (nTweets == topK) {
					break;
				}
			}
			buff.close();
			return nGramList;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	public double getPrecision() {
		Set<String> union = new HashSet<String>();
		union.addAll(inferSum);
		union.addAll(generatedSum);
		int intersection = inferSum.size() + generatedSum.size() - union.size();

		return (double) intersection / generatedSum.size();
	}

	public double getRecall() {
		Set<String> union = new HashSet<String>();
		union.addAll(inferSum);
		union.addAll(generatedSum);
		int intersection = inferSum.size() + generatedSum.size() - union.size();

		return (double) intersection / inferSum.size();
	}

	public double getF1Score() {
		double pre = getPrecision();
		double rec = getRecall();
		double f1 = 2 * pre * rec / (pre + rec);

		return f1;
	}

	public static void main(String[] args) {
		new Configure();
		String groundtruth = Configure.WORKING_DIRECTORY + "/output/evaluation";
		String baselines = Configure.WORKING_DIRECTORY + "/output/baselines";
		File[] baselineFiles = (new File(baselines)).listFiles();
		File[] inferSummaries = (new File(groundtruth)).listFiles();

		try {
			for (int i = 0; i < baselineFiles.length; i++) {
				FileWriter outputFile = new FileWriter(
						Configure.WORKING_DIRECTORY + "/output/rougeScore/" + baselineFiles[i].getName() + ".txt");
				BufferedWriter buff = new BufferedWriter(outputFile);
				buff.write("FileName\tP\tR\tF1\n");
				for (int j = 0; j < inferSummaries.length; j++) {

					if (!inferSummaries[j].getName().endsWith(".txt")) {
						continue;
					}
					File generatedSummary = new File(
							baselineFiles[i].getAbsoluteFile() + "/" + inferSummaries[j].getName());

					ROUGE rouge = new ROUGE(1, 5, inferSummaries[j], generatedSummary);
					buff.write(String.format("%s\t%f\t%f\t%f\n", inferSummaries[j].getName(), rouge.getPrecision(),
							rouge.getRecall(), rouge.getF1Score()));
				}

				buff.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("...done...");
	}
}
