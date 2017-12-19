package l3s.tts.runner;

import java.util.ArrayList;

public class Test {
	public static void main(String[] args) {
		ArrayList<String> array = new ArrayList<String>();
		ArrayList<String> result = new ArrayList<String>();
		array.add("Huyen");
		array.add("nguyen");
		String s = array.get(0);
		result.add(s);
		array.remove(0);
		System.out.println(result.get(0));
	}
}
