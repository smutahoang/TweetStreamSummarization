package l3s.tts.baseline.sumblr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Util {

	public static String[][] TOPIC_SET = { { "obama" }, { "arsenal", "arsene", "wenger" }, { "chelsea" },
			{ "smartphone", "smart", "phone", "tablet", "cellphone", "cell" }, { "blackfriday", "black", "friday" },
			{ "2013arsenal", "arsenal", "arsene", "wenger" }, { "2013chelsea", "chelsea" },
			{ "2013champions", "champions", "league", "championsleague" } };

	public static int SUM_LENGTH = 20; // default summary length

	// used for genhistorySets()
	public static int HORIZON = 48;
	public static int HORIZON_NUM = 10;
	public static int STEP = 24;
	public static int STEP_NUM = 10;

	public static void sortHistoryFiles(String[] files) {
		Arrays.sort(files, new Comparator<String>() {
			// @Override
			public int compare(String s1, String s2) {
				// ��ȡ�ļ����ĵ�һ������, �������ִ�С����
				int hyphenIndex = s1.indexOf("-");
				int dotIndex = s1.indexOf(".");
				int baseTime1 = new Integer(s1.substring(0, hyphenIndex));
				int endTime1 = new Integer(s1.substring(hyphenIndex + 1, dotIndex));
				hyphenIndex = s2.indexOf("-");
				dotIndex = s2.indexOf(".");
				int baseTime2 = new Integer(s2.substring(0, hyphenIndex));
				int endTime2 = new Integer(s2.substring(hyphenIndex + 1, dotIndex));
				if (baseTime1 < baseTime2)
					return -1;
				if (baseTime1 > baseTime2)
					return 1;
				if (endTime1 < endTime2)
					return -1;
				if (endTime1 > endTime2)
					return 1;
				return 0;
			}
		});
	}

	public static void sortGranFiles(String[] files) {
		Arrays.sort(files, new Comparator<String>() {
			// @Override
			public int compare(String s1, String s2) {
				// ��ȡ�ļ����ĵ�һ������, �������ִ�С����
				int spaceIndex = s1.indexOf(" ");
				int hyphenIndex = s1.indexOf("-");
				int gran1 = new Integer(s1.substring(0, spaceIndex));
				int baseTime1 = new Integer(s1.substring(spaceIndex + 1, hyphenIndex));
				spaceIndex = s2.indexOf(" ");
				hyphenIndex = s2.indexOf("-");
				int gran2 = new Integer(s2.substring(0, spaceIndex));
				int baseTime2 = new Integer(s2.substring(spaceIndex + 1, hyphenIndex));
				if (gran1 < gran2)
					return -1;
				if (gran1 > gran2)
					return 1;
				if (baseTime1 < baseTime2)
					return -1;
				if (baseTime1 > baseTime2)
					return 1;
				return 0;
			}
		});
	}

	// �˷�����Ҫ������slidingWindow����ļ�����������
	public static void sortSWFiles(String[] files) {
		Arrays.sort(files, new Comparator<String>() {
			// @Override
			public int compare(String s1, String s2) {
				// ��ȡ�ļ����ĵ�һ������, �������ִ�С����
				int idx1 = s1.indexOf(" ");
				int idx2 = s1.indexOf("-");
				int n1 = new Integer(s1.substring(idx1 + 1, idx2));
				idx1 = s2.indexOf(" ");
				idx2 = s2.indexOf("-");
				int n2 = new Integer(s2.substring(idx1 + 1, idx2));
				if (n1 < n2)
					return -1;
				if (n1 > n2)
					return 1;
				return 0;
			}
		});
	}

	// ��ȡԭʼ΢��: map<tweet id, original text>
	public static Map<String, String> loadRawTweets(String rawFile) {
		Map<String, String> rawMap = new TreeMap<String, String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(rawFile));
			String line;
			while ((line = br.readLine()) != null) {
				String[] items = line.split("\\s+", 8);
				rawMap.put(items[0], items[7]);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rawMap;
	}

	// ���ڼ����Ը��ļ�Ϊ�����ļ��Ĵʻ��idf
	public static Map<String, Double> getIDFs(String tokenFile) {
		Map<String, Double> docFreqMap = new HashMap<String, Double>();
		int docNum = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(tokenFile));
			String line;
			while ((line = br.readLine()) != null) {
				String[] items = line.split("\\s+", 5);
				String[] tokens = items[4].split("\\s+");
				// �������Ѿ��������term��dfû��Ӱ����
				Set<String> alreadyCount = new HashSet<String>();
				for (String tok : tokens) {
					if (alreadyCount.contains(tok))
						continue;
					else {
						alreadyCount.add(tok);
						if (docFreqMap.containsKey(tok))
							docFreqMap.put(tok, docFreqMap.get(tok) + 1);
						else
							docFreqMap.put(tok, 1.0);
					}
				}
				if (tokens.length > 0)
					docNum++;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (String token : docFreqMap.keySet()) {
			double docFreq = docFreqMap.get(token);
			docFreqMap.put(token, Math.log(docNum / docFreq));
		}
		return docFreqMap;
	}

	// ��token��stringת����id
	public static Map<String, Integer> createTokenIDMap(String tokenStatFile) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(tokenStatFile));
			String line;
			int id = 0;
			while ((line = br.readLine()) != null) {
				String[] items = line.split("\\s+", 5);
				String[] tokens = items[4].split("\\s+");
				for (String tok : tokens) {
					if (!map.containsKey(tok))
						map.put(tok, id++);
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static void subtractMap(Map<Integer, Double> base, Map<Integer, Double> subtract) {
		for (Map.Entry<Integer, Double> token : subtract.entrySet()) {
			int tokenID = token.getKey();
			double value = token.getValue();
			if (base.containsKey(tokenID)) {
				double newValue = base.get(tokenID) - value;
				if (newValue > 0)
					base.put(tokenID, newValue);
				else
					base.remove(tokenID);
			}
		}
	}

	public static Map<Integer, Double> string2Map(String ss) {
		String[] keyValues = ss.split(" ");
		if (keyValues.length % 2 != 0)
			System.err.println("map parse error: " + ss);

		Map<Integer, Double> map = new TreeMap<Integer, Double>();
		int size = keyValues.length / 2;
		for (int i = 0; i < size; i++)
			map.put(new Integer(keyValues[i * 2]), new Double(keyValues[i * 2 + 1]));
		return map;
	}

	public static String map2String(Map<Integer, Double> map) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, Double> entry : map.entrySet())
			sb.append(entry.getKey() + " " + entry.getValue() + " ");
		return sb.toString();
	}

	public static double computeSim(Map<Integer, Double> map1, Map<Integer, Double> map2) {
		double mul = 0, ss1 = 0, ss2 = 0;
		for (Map.Entry<Integer, Double> me : map1.entrySet()) {
			if (map2.containsKey(me.getKey())) {
				mul += me.getValue() * map2.get(me.getKey());
			}
			ss1 += me.getValue() * me.getValue();
		}
		for (Double value : map2.values())
			ss2 += value * value;

		return mul / Math.sqrt(ss1 * ss2);
	}

	// from MOA
	public static double getQuantile(double z) {
		assert (z >= 0 && z <= 1);
		return Math.sqrt(2) * inverseError(2 * z - 1);
	}

	private static double inverseError(double x) {
		double z = Math.sqrt(Math.PI) * x;
		double res = (z) / 2;

		double z2 = z * z;
		double zProd = z * z2; // z^3
		res += (1.0 / 24) * zProd;

		zProd *= z2; // z^5
		res += (7.0 / 960) * zProd;

		zProd *= z2; // z^7
		res += (127 * zProd) / 80640;

		zProd *= z2; // z^9
		res += (4369 * zProd) / 11612160;

		zProd *= z2; // z^11
		res += (34807 * zProd) / 364953600;

		zProd *= z2; // z^13
		res += (20036983 * zProd) / 797058662400d;

		return res;
	}

}
