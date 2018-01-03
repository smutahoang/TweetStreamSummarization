package l3s.tts.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class AliasSample {
	private double prob[];
	private int alias[];
	private Random rand;
	public AliasSample(Random rand) {
		// TODO Auto-generated constructor stub
		this.rand = rand;
		
	}
	
	public double[] normalize(int[] weight) {
		double[] probabilities = new double[weight.length];
		double sum = 0;
		for(int i = 0; i<weight.length; i++) {
			sum+=weight[i];
		}
		for(int i = 0; i<weight.length; i++) {
			probabilities[i] = (double)weight[i]/sum;
		}
		return probabilities;
	}
	
	
	public void buildBarChart(double[] weights) {
		prob = new double[weights.length];
		alias = new int[weights.length];
		double[] probabilities = weights;
		//probabilities = normalize(weight);
		Deque<Integer> lProb = new ArrayDeque<Integer> ();
		Deque<Integer> hProb = new ArrayDeque<Integer>();
		double average = (double)1/probabilities.length;
		
		for(int i = 0; i<probabilities.length; i++) {
			if(probabilities[i] < average) 
				lProb.add(i);
			else if(probabilities[i] > average)
				hProb.add(i);
			else {
				
				alias[i] = i;
			}
			prob[i] = average;
		}
		
		//
		int lIndex, hIndex;
		while(!lProb.isEmpty() && !hProb.isEmpty()) {
			lIndex = lProb.removeLast();
			hIndex = hProb.removeLast();
			prob[lIndex] = probabilities[lIndex];
			alias[lIndex] = hIndex;
			double remain = probabilities[hIndex] - (average - probabilities[lIndex]);
			if(remain > average) {
				probabilities[hIndex] = remain;
				hProb.add(hIndex);
			} else {
				lProb.add(hIndex);
				probabilities[hIndex] = remain;
			}
			
		}
	}
	public int[] getAlias() {
		return alias;
	}
	public double[] getProb() {
		return prob;
	}
	public int sample() {
		
		int column = rand.nextInt(alias.length);
		//System.out.print(column);
		double x = rand.nextDouble();
		if(alias.length * prob[column] < x) {
			///System.out.print(prob[alias[column]] +" ");
			return alias[column];
		}
		else {
			//System.out.print(prob[column] +" ");
			return column;
		}
	}
	public static void main(String[] args) {
		AliasSample aliasSample = new AliasSample(new Random());
		double[] weight = {0.4, 0.15, 0.1, 0.35};
		aliasSample.buildBarChart(weight);
		int[] alias = aliasSample.getAlias();
		for(int i = 0; i<alias.length; i++)
			System.out.print(alias[i]+"\t");
		System.out.println();
		double[] prob = aliasSample.getProb();
		for(int i = 0; i<prob.length; i++)
			System.out.print(prob[i]+"\t");
		for(int i = 0; i<100; i++) {
			System.out.println(aliasSample.sample());
		}
	}
}
