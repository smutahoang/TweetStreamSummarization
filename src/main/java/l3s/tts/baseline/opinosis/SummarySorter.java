/*
 * Decompiled with CFR 0_123.
 */
package l3s.tts.baseline.opinosis;


import java.util.Comparator;

public class SummarySorter
implements Comparator<Candidate> {
    public int compare(Candidate s1, Candidate s2) {
        if (s1.gain > s2.gain) {
            return -1;
        }
        if (s1.gain < s2.gain) {
            return 1;
        }
        return 0;
    }
}

