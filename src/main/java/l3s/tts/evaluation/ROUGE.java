package l3s.tts.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
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

	public ROUGE(int ngram, int _topK, String inferPath, String generatedPath) {
		// TODO Auto-generated constructor stub
		// this.ngram = ngram;
		this.topK = _topK;
		tweetPreprocessingUtils = new TweetPreprocessingUtils();
		inferSum = getNGrams(inferPath, Integer.MAX_VALUE, ngram);
		generatedSum = getNGrams(generatedPath, topK, ngram);
	}

	private Set<String> getNGrams(String inputFile, int nTweets, int nGram) {
		try {
			BufferedReader buff = null;
			String line = null;
			Set<String> nGramList = new HashSet<String>();
			buff = new BufferedReader(new FileReader(inputFile));
			while ((line = buff.readLine()) != null) {
				List<String> terms = tweetPreprocessingUtils.extractTermInTweet(line);
				for (String term : terms) {
					nGramList.add(term);
				}
				nTweets--;

				// System.out.printf("line = %s\n", line);
				// for (String term : terms) {
				// System.out.printf("term = %s\n", term);
				// }
				// System.out.println("*******************************************");

				if (nTweets == 0) {
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

	public Set<String> FailedTerms() {
		Set<String> failedTerms = new HashSet<String>();
		for (String term : generatedSum) {
			if (!inferSum.contains(term)) {
				failedTerms.add(term);
			}
		}
		return failedTerms;
	}

	public Set<String> missedTerms() {
		Set<String> missedTerms = new HashSet<String>();
		for (String term : inferSum) {
			if (!generatedSum.contains(term)) {
				missedTerms.add(term);
			}
		}
		return missedTerms;
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
		try {

			String inferPath = "C:/Users/Tuan-Anh Hoang/Desktop/tss/groundtruth/representativeTweets_22.txt";
			String generatedPath = "C:/Users/Tuan-Anh Hoang/Desktop/tss/inc/22_inc.txt";

			ROUGE rouge = new ROUGE(1, 10, inferPath, generatedPath);
			System.out.printf("prec = %f rec = %f f1Score = %f\n", rouge.getPrecision(), rouge.getRecall(),
					rouge.getF1Score());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
