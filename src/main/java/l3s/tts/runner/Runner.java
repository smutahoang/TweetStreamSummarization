package l3s.tts.runner;

public class Runner {

	public static void main(String[] args) {
		String option = args[0];
		if (option.equals("lexrank")) {
			TestLexRank.main(null);
		} else if (option.equals("opinosis")) {
			TestOpinosis.main(null);
		} else if (option.equals("sumblr")) {
			TestSumblr.main(null);
		} else if (option.equals("inc")) {
			TestDiversifiedRanking.main(null);
		} else if (option.equals("groundtruth")) {
			TestGroundtruthGeneration.main(null);
		} else {
			// do nothing for now
		}
	}
}
