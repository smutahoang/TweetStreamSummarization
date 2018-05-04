package l3s.tts.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

public class RankingUtils {

	public static List<Integer> getIndexTopElements(int k, double[] array) {
		PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
		for (int i = 0; i < array.length; i++) {
			if (queue.size() < k) {
				queue.add(new KeyValue_Pair(i, array[i]));
			} else {
				KeyValue_Pair head = queue.peek();
				if (head.getDoubleValue() < array[i]) {
					queue.poll();
					queue.add(new KeyValue_Pair(i, array[i]));
				}
			}
		}

		List<Integer> topWords = new ArrayList<Integer>();
		while (!queue.isEmpty()) {
			topWords.add(topWords.size(), queue.poll().getIntKey());
		}
		return topWords;
	}

	public static List<Integer> getIndexTopElements(int k, List<Double> array) {
		PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
		for (int i = 0; i < array.size(); i++) {
			if (queue.size() < k) {
				queue.add(new KeyValue_Pair(i, array.get(i)));
			} else {
				KeyValue_Pair head = queue.peek();
				if (head.getDoubleValue() < array.get(i)) {
					queue.poll();
					queue.add(new KeyValue_Pair(i, array.get(i)));
				}
			}
		}

		List<Integer> topWords = new ArrayList<Integer>();
		while (!queue.isEmpty()) {
			topWords.add(topWords.size(), queue.poll().getIntKey());
		}
		return topWords;
	}

	public static List<Integer> getIndexTopElements(double[] array, double threshold) {
		PriorityBlockingQueue<KeyValue_Pair> queue = new PriorityBlockingQueue<KeyValue_Pair>();
		for (int i = 0; i < array.length; i++) {
			queue.add(new KeyValue_Pair(i, array[i]));
		}

		KeyValue_Pair[] sortedArray = new KeyValue_Pair[array.length];
		int i = 0;
		while (!queue.isEmpty()) {
			sortedArray[i] = queue.poll();
			i++;
		}
		i = i - 1;
		double s = 0;
		List<Integer> topWords = new ArrayList<Integer>();
		while (s < threshold) {
			topWords.add(sortedArray[i].getIntKey());
			s += sortedArray[i].getDoubleValue();
			i--;
			if (i < 0)
				break;
		}
		return topWords;
	}

	public static void main(String[] args) {

		double[] a = new double[] { 0.1, 0.3, 0.3, 0.2, 0.05, 0.05 };
		List<Integer> topIndexs = RankingUtils.getIndexTopElements(a, 0.9);
		for (int i : topIndexs) {
			System.out.printf("i = %d f = %f\n", i, a[i]);
		}
	}
}
