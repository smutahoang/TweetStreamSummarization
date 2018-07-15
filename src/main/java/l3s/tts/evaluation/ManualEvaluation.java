package l3s.tts.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Random;

import l3s.tts.utils.IOUtils;

public class ManualEvaluation {

	static String rootPath = "C:/Users/Tuan-Anh Hoang/Desktop/tss";

	// static String[] comparativeMethods = new String[] { "lexrank",
	// "opinosis", "online_msc", "sumblr" };
	static String[] comparativeMethods = new String[] { "lexrank", "sumblr" };

	static String reformatTweet(String tweet) {
		tweet = tweet.trim();
		tweet = tweet.replaceAll("\\s+", " ");
		if (tweet.startsWith("RT @") || tweet.startsWith("rt @")) {
			int p = 3;
			while (tweet.charAt(p) != ' ') {
				p++;
			}
			tweet = tweet.substring(p + 1, tweet.length());
		}
		return tweet;
	}

	static void makeHTML(String dataset, int minTimeStep, int maxTimeStep, int sampleGap, int seed, String annotator) {

		try {

			int nTimeSteps = 0;
			int t = minTimeStep;
			while (t <= maxTimeStep) {
				nTimeSteps++;
				t += sampleGap;
			}
			Random rand = new Random(seed);

			int[] topK = new int[] { 5, 10 };

			String[] baseline = new String[comparativeMethods.length * nTimeSteps * topK.length];
			int timeSteps[] = new int[baseline.length];
			int nTweets[] = new int[baseline.length];

			t = minTimeStep;
			int index = 0;
			while (t <= maxTimeStep) {
				for (int m = 0; m < comparativeMethods.length; m++) {
					for (int k = 0; k < topK.length; k++) {
						baseline[index] = comparativeMethods[m];
						timeSteps[index] = t;
						nTweets[index] = topK[k];
						index++;
					}
				}
				t += sampleGap;
			}

			for (int i = 0; i < baseline.length / 2; i++) {
				int p = rand.nextInt(baseline.length);
				int q = rand.nextInt(baseline.length);

				String tempStr = baseline[p];
				baseline[p] = baseline[q];
				baseline[q] = tempStr;

				int tempInt = timeSteps[p];
				timeSteps[p] = timeSteps[q];
				timeSteps[q] = tempInt;

				tempInt = nTweets[p];
				nTweets[p] = nTweets[q];
				nTweets[q] = tempInt;
			}

			String outputPath = String.format("%s/manual/html/%s_%s", rootPath, dataset, annotator);
			IOUtils.mkDir(outputPath);
			BufferedWriter bw_info = new BufferedWriter(
					new FileWriter(String.format("%s/manual/html/%s_%s.csv", rootPath, dataset, annotator)));
			bw_info.write("index,baseline,timeStep,inc_set,topK\n");
			BufferedWriter bw_result = new BufferedWriter(new FileWriter(String.format("%s/result.txt", outputPath)));

			for (int i = 0; i < baseline.length; i++) {
				t = timeSteps[i];
				int p = rand.nextInt(2);
				int K = nTweets[i];

				bw_info.write(String.format("%d,%s,%d,%d,%d\n", i, baseline[i], t, p, K));
				bw_result.write(String.format("%d.html\n", i));

				String filename = String.format("%s/%d.html", outputPath, i);
				BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
				filename = String.format("%s/inc/%s/%d_inc.txt", rootPath, dataset, t);
				BufferedReader inc_br = new BufferedReader(new FileReader(filename));
				filename = String.format("%s/%s/%s/%d_%s.txt", rootPath, baseline[i], dataset, t, baseline[i]);
				BufferedReader cm_br = new BufferedReader(new FileReader(filename));

				bw.write("<html>\n");
				bw.write("\t<head>\n");
				bw.write("\t\t<style>\n");
				bw.write("\t\t\ttable {border-collapse:collapse; table-layout:fixed; width:100%;}\n");
				bw.write("\t\t\ttable td {border:solid 1px #fab; width:100px; word-wrap:break-word;}\n");
				bw.write("\t\t</style>\n");
				bw.write("\t</head>\n");

				bw.write("\t<body>\n");
				bw.write("\t\t<table>\n");
				bw.write("\t\t\t<col width=\"5%\">\n");
				bw.write("\t\t\t<col width=\"47.5%\">\n");
				bw.write("\t\t\t<col width=\"47.5%\">\n");
				bw.write("\t\t\t<tr align=\"center\">\n");
				bw.write("\t\t\t\t<td><b>Tweet number</b></td>\n");
				bw.write("\t\t\t\t<td><b>Set 1</b></td>\n");
				bw.write("\t\t\t\t<td><b>Set 2</b></td>\n");
				bw.write("\t\t\t</tr>\n");

				for (int k = 0; k < K; k++) {
					bw.write("\t\t\t<tr>\n");
					bw.write(String.format("\t\t\t\t<td>%d</td>\n", k));
					if (p == 0) {
						bw.write(String.format("\t\t\t\t<td>%s</td>\n", reformatTweet(inc_br.readLine())));
						bw.write(String.format("\t\t\t\t<td>%s</td>\n", reformatTweet(cm_br.readLine())));
					} else {
						bw.write(String.format("\t\t\t\t<td>%s</td>\n", reformatTweet(cm_br.readLine())));
						bw.write(String.format("\t\t\t\t<td>%s</td>\n", reformatTweet(inc_br.readLine())));
					}
					bw.write("\t\t\t</tr>\n");
				}

				bw.write("\t\t</table>\n");
				bw.write("\t</body>\n");
				bw.write("</html>\n");
				bw.close();

				inc_br.close();
				cm_br.close();
			}
			bw_info.close();
			bw_result.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		String dataset = "travel_ban";

		int minTimeStep = 6;
		int maxTimeStep = 175;
		int sampleGap = 3;
		
		int seed = 1;
		String annotator = "HOANGOANH";
		makeHTML(dataset, minTimeStep, maxTimeStep, sampleGap, seed, annotator);
	}
}
