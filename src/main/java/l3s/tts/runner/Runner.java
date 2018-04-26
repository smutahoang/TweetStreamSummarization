package l3s.tts.runner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import l3s.tts.configure.Configure;
import l3s.tts.evaluation.ROUGE;

public class Runner {

	static void evaluation() {
		try {
			new Configure();
			List<String> models = new ArrayList<String>();
			// models.add("lexrank");
			// models.add("sumblr");
			// models.add("opinosis");
			models.add("inc");
			for (String model : models) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(
						String.format("%s/output/evaluation/%s.csv", Configure.WORKING_DIRECTORY, model)));
				for (int t = 1; t <= 150; t++) {
					String groundtruthPath = String.format("%s/output/groundtruth/representativeTweets_%d.txt",
							Configure.WORKING_DIRECTORY, t);
					String generatedPath = String.format("%s/output/%s/%d_%s.txt", Configure.WORKING_DIRECTORY, model,
							t, model);
					bw.write(String.format("%d", t));
					for (int K = 5; K <= 10; K += 5) {
						ROUGE rouge = new ROUGE(1, K, groundtruthPath, generatedPath);
						double f1Score = rouge.getF1Score();
						bw.write(String.format(",%f", f1Score));

						System.out.printf("model = %s time = %d nTweets = %d f1 = %f\n", model, t, K, f1Score);
					}
					bw.write("\n");
				}
				bw.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

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
		} else if (option.equals("evaluation")) {
			evaluation();
		} else {
			// do nothing for now
		}
	}
}
