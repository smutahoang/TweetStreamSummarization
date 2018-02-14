package l3s.tts.runner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;

public class Test {
	public static void main(String[] args) {
		HashSet<Integer> set = new HashSet<Integer>();
		for(int i = 0; i<100; i++) {
			set.add(i);
		}
		HashSet<Integer> result = new HashSet<Integer>();
		Iterator<Integer> iter = set.iterator();
		int i = 0;
		while(i < 5) {
			HashSet<Integer> term = new HashSet<Integer>();
			term.add(iter.next());
			i++;
			//if(i==3)
				result = term;
		}
		Iterator<Integer> iter1 = result.iterator();
		while(iter1.hasNext())
			System.out.println(iter1.next());
	}
}
