package l3s.tts.runner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import l3s.tts.configure.Configure;

public class ROUGE {
	private int ngram;
	private Set<String> inferSum;
	private Set<String> generatedSum;

	public ROUGE(int ngram, File inferPath, File generatedPath) {
		// TODO Auto-generated constructor stub
		this.ngram = ngram;
		inferSum = getNGraph(inferPath, ngram);
		generatedSum = getNGraph(generatedPath, ngram);

	}

	private Set<String> getNGraph(File inputFile, int nGram) {
		BufferedReader buff = null;
		String line;
		Set<String> nGramList = new HashSet<String>();
		try {
			buff = new BufferedReader(new FileReader(inputFile));
			while ((line = buff.readLine()) != null) {
				String[] words = line.split(" ");

				for (int i = 0; i < words.length - 1; i++) {
					StringBuilder builder = new StringBuilder();
					builder.append(words[i]);
					for (int j = i + 1; j < i + nGram; j++) {
						builder.append(" ");
						builder.append(words[j]);
					}
					nGramList.add(builder.toString());
				}
			}

			buff.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		return nGramList;
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
		String baselines = Configure.WORKING_DIRECTORY+"/output/baselines";
		File[] baselineFiles = (new File(baselines)).listFiles();
		File[] inferSummaries = (new File(groundtruth)).listFiles();
		

		try {
			for (int i = 0; i < baselineFiles.length; i++) {
				FileWriter outputFile = new FileWriter(Configure.WORKING_DIRECTORY+"/output/rougeScore/"+baselineFiles[i].getName()+".txt");
				BufferedWriter buff = new BufferedWriter(outputFile);
				buff.write("FileName\tP\tR\tF1\n");
				for(int j = 0; j<inferSummaries.length; j++) {
					
					if(!inferSummaries[j].getName().endsWith(".txt")) {
						continue;
					}
					File generatedSummary = new File(baselineFiles[i].getAbsoluteFile()+"/"+inferSummaries[j].getName());
					
					ROUGE rouge = new ROUGE(1, inferSummaries[j], generatedSummary);
					buff.write(String.format("%s\t%f\t%f\t%f\n", inferSummaries[j].getName(), rouge.getPrecision(), rouge.getRecall(), rouge.getF1Score()));
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
