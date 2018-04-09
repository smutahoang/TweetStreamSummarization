/*
 * Decompiled with CFR 0_123.
 */
package l3s.tts.baseline.opinosis;

import java.util.List;

public class Candidate
implements Comparable<Candidate> {
    public boolean discard = false;
    public double gain;
    int level = 0;
    public double localgain;
    public double overlap;
    public double rawscore;
    public String sent;
    public List<int[]> sentList;
    public List<Node> theNodeList;

    public Candidate(double ogain, String sentence, List<int[]> sentList, int level) {
        this.gain = ogain;
        this.sent = sentence;
        this.sentList = sentList;
        this.level = level;
    }

    public Candidate(double overallGain, String str, List<int[]> overlapList, int level, double score, double gain) {
        this.gain = overallGain;
        this.sent = str;
        this.sentList = overlapList;
        this.level = level;
        this.rawscore = score;
        this.localgain = gain;
    }

    public boolean equals(Object b) {
        Candidate infob = (Candidate)b;
        if (this.sent.equals(infob.sent)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return this.sent.hashCode();
    }

    public int compareTo(Candidate info) {
        List<int[]> sentList2 = info.sentList;
        double overlap = Node.getSetenceJaccardOverlap(this.sentList, sentList2);
        if (sentList2.get(0)[0] == this.sentList.get(0)[0]) {
            if (this.sentList.size() > sentList2.size()) {
                return 1;
            }
            if (this.sentList.size() < sentList2.size()) {
                return -1;
            }
            return 0;
        }
        if (sentList2.get(0)[0] > this.sentList.get(0)[0]) {
            return 1;
        }
        return -1;
    }
    
    public String toString() {
    	
    	String sentence = sent;
    	sentence = sentence.replaceAll("(/[a-z,.;$]+(\\s+|$))", " ");
    	sentence = sentence.replaceAll("xx", "");
    	sentence = String.valueOf(sent) + " .";
    	sentence = sentence.replaceAll("\\s+", " ");
        return sentence;
    }
}

