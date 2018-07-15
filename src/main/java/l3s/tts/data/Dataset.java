package l3s.tts.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;

public class Dataset {
	static void getDataFromAdaptiveFilters(String groundtruthPath, int nTopics, double threshod, String inputPath,
			String outputPath) {
		try {
			String filename = String.format("%s/groundTruth_%d_%.2f.csv", groundtruthPath, nTopics, threshod);
			System.out.printf("filename = %s\n", filename);
			HashSet<String> tweetIds = new HashSet<String>();
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			while ((line = br.readLine()) != null) {
				// System.out.println(line);
				tweetIds.add(line);
			}
			br.close();

			br = new BufferedReader(new FileReader(inputPath));
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath));
			while ((line = br.readLine()) != null) {
				if (tweetIds.contains(line.split("\t")[0])) {
					bw.write(line);
					bw.write("\n");
				}
			}
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String[] args) {
		// String groundtruthPath = "C:/Users/Tuan-Anh
		// Hoang/Desktop/attt/annotation/agreed/london_attack";
		// String outputPath = "C:/Users/Tuan-Anh
		// Hoang/Desktop/tss/dataset/london_attack.txt";

		String groundtruthPath = "C:/Users/Tuan-Anh Hoang/Desktop/attt/annotation/agreed/travel_ban";
		String outputPath = "C:/Users/Tuan-Anh Hoang/Desktop/tss/dataset/travel_ban.txt";

		int nTopics = 200;
		double threshold = 0.8;
		String inputPath = "C:/Users/Tuan-Anh Hoang/Desktop/attt/evaluation/travel_ban/tweetPool.txt";

		getDataFromAdaptiveFilters(groundtruthPath, nTopics, threshold, inputPath, outputPath);
	}
}
