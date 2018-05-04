package l3s.tts.utils;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.validator.routines.UrlValidator;

import cmu.arktweetnlp.Tagger;
import cmu.arktweetnlp.Tagger.TaggedToken;
import l3s.tts.configure.Configure;

public class TweetPreprocessingUtils {

	static HashSet<Character> punctuations;
	static HashSet<Character> braces;
	static HashSet<Character> quoteSymbols;
	static HashSet<Character> validPrefixSymbols;
	static HashSet<Character> validSuffixSymbols;
	private Tagger tagger;
	// static String stopwordPath = "/home/hoang/attt/data/stopwords";
	// static String stopwordPath =
	// "E:/code/java/AdaptiveTopicTrackingTwitter/data/stopwords";
	private HashSet<String> stopWords;

	private void getStopWords() {
		try {
			stopWords = new HashSet<String>();
			BufferedReader br;
			String line = null;

			br = new BufferedReader(
					new FileReader(String.format("%s/common-english-adverbs.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(
					new FileReader(String.format("%s/common-english-prep-conj.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(
					new FileReader(String.format("%s/common-english-words.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(
					new FileReader(String.format("%s/smart-common-words.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(new FileReader(String.format("%s/mysql-stopwords.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(new FileReader(String.format("%s/twitter-slang.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			br = new BufferedReader(new FileReader(String.format("%s/shorthen.txt", Configure.STOPWORD_PATH)));
			line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.toLowerCase().split(",");
				for (int i = 0; i < tokens.length; i++) {
					stopWords.add(tokens[i]);
				}
			}
			br.close();

			addMoreStopWords();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/***
	 * more stop-words found while conducting experiments
	 */
	private void addMoreStopWords() {
		// words due to truncated tweets
		stopWords.add("//t");
		stopWords.add("http");
		stopWords.add("https");
	}

	private UrlValidator urlValidator;

	private void initPunctuations() {
		punctuations = new HashSet<Character>();
		punctuations.add('~');
		punctuations.add('^');
		punctuations.add('(');
		punctuations.add(')');
		punctuations.add('{');
		punctuations.add('}');
		punctuations.add('[');
		punctuations.add(']');
		punctuations.add('<');
		punctuations.add('>');
		punctuations.add(':');
		punctuations.add(';');
		punctuations.add(',');
		punctuations.add('.');
		punctuations.add('?');
		punctuations.add('!');
	}

	private void initBraces() {
		braces = new HashSet<Character>();
		braces.add('~');
		braces.add('^');
		braces.add('(');
		braces.add(')');
		braces.add('{');
		braces.add('}');
		braces.add('[');
		braces.add(']');
		braces.add('<');
		braces.add('>');
	}

	private void initQouteSymbols() {

		quoteSymbols = new HashSet<Character>();
		quoteSymbols.add('\"');
		quoteSymbols.add('\'');
		quoteSymbols.add('`');
		quoteSymbols.add('\u2014');// long dash
		quoteSymbols.add('\u0022');
		// quotation mark (")
		// quoteSymbols.add('\u0027'); // apostrophe (')
		// quoteSymbols.add('\u00ab'); // left-pointing // double-angle //

		quoteSymbols.add('\u00bb'); // right-pointing double-angle quotation
									// mark
		quoteSymbols.add('\u2018'); // left single quotation mark
		quoteSymbols.add('\u2019'); // right single quotation mark
		quoteSymbols.add('\u201a'); // single low-9 quotation mark
		quoteSymbols.add('\u201b'); // single high-reversed-9 quotation mark
		quoteSymbols.add('\u201c'); // left double quotation mark
		quoteSymbols.add('\u201d'); // right double quotation mark
		quoteSymbols.add('\u201e'); // double low-9 quotation mark
		quoteSymbols.add('\u201f'); // double high-reversed-9 quotation mark
		quoteSymbols.add('\u2039'); // single left-pointing angle quotation mark
		quoteSymbols.add('\u203a'); // single right-pointing angle quotation
									// mark
		quoteSymbols.add('\u300c'); // left corner bracket
		quoteSymbols.add('\u300d'); // right corner bracket
		quoteSymbols.add('\u300e'); // left white corner bracket
		quoteSymbols.add('\u300f'); // right white corner bracket
		quoteSymbols.add('\u301d'); // reversed double prime quotation mark
		quoteSymbols.add('\u301e'); // double prime quotation mark
		quoteSymbols.add('\u301f'); // low double prime quotation mark
		quoteSymbols.add('\ufe41'); // presentation form for vertical left
									// corner bracket
		quoteSymbols.add('\ufe42'); // presentation form for vertical right
									// corner bracket
		quoteSymbols.add('\ufe43'); // presentation form for vertical left
									// corner white bracket
		quoteSymbols.add('\ufe44'); // presentation form for vertical right
									// corner white bracket
		quoteSymbols.add('\uff02'); // fullwidth quotation mark
		quoteSymbols.add('\uff07'); // fullwidth apostrophe
		quoteSymbols.add('\uff62'); // halfwidth left corner bracket
		quoteSymbols.add('\uff63'); // halfwidth right corner bracket

	}

	private void initPrefixSuffixSymbols() {
		validPrefixSymbols = new HashSet<Character>();
		validPrefixSymbols.add('@');
		validPrefixSymbols.add('#');
		validPrefixSymbols.add('%');
		validPrefixSymbols.add('$');

		validSuffixSymbols = new HashSet<Character>();
		validSuffixSymbols.add('%');
		validSuffixSymbols.add('$');
	}

	private void init() {
		initPunctuations();
		initBraces();
		initQouteSymbols();
		initPrefixSuffixSymbols();

		getStopWords();
		initPosTaggerModel();
		urlValidator = new UrlValidator();
	}

	public TweetPreprocessingUtils() {
		init();
	}

	public void initPosTaggerModel() {
		tagger = new Tagger();
		try {
			tagger.loadModel(Configure.TAGGING_MODEL);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void removeOriginalAuthors(char[] chars) {
		for (int i = 0; i < chars.length; i++) {
			if (i > chars.length - 4) {
				return;
			}
			if (Character.toLowerCase(chars[i]) != 'r') {
				continue;
			}
			if (Character.toLowerCase(chars[i + 1]) != 't') {
				continue;
			}
			if (chars[i + 2] != ' ') {
				continue;
			}
			if (chars[i + 3] != '@') {
				continue;
			}
			if (i > 0) {
				if (chars[i - 1] != ' ')
					continue;
			}
			int j = i + 4;
			while (j < chars.length) {
				if (chars[j] == ' ')
					break;
				j++;
			}
			for (int p = i; p < j; p++) {
				chars[p] = ' ';
			}
		}
	}

	private void removeNewLineAndTabCharacter(char[] chars) {
		// System.out.printf("Before removeNewLineAndTabCharacter:\t");
		// ptScreen(chars);
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '\n') {
				chars[i] = ' ';
				continue;
			} else if (chars[i] == '\r') {
				chars[i] = ' ';
				continue;
			} else if (chars[i] == '\t') {
				chars[i] = ' ';
				continue;
			} else {
				continue;
			}
		}
		// System.out.printf("After removeNewLineAndTabCharacter:\t");
		// ptScreen(chars);
	}

	private boolean isURLStart(char[] chars, int i) {
		if (i >= chars.length - 4)
			return false;
		if (chars[i] != 'h')
			return false;
		if (chars[i + 1] != 't')
			return false;
		if (chars[i + 2] != 't')
			return false;
		if (chars[i + 3] != 'p')
			return false;
		return true;
	}

	private boolean isShorten(char[] chars, int i) {
		if (!quoteSymbols.contains(chars[i]))
			return false;
		if (i <= chars.length - 3) {
			if (chars[i + 1] == 'r' && chars[i + 2] == 'e') // e.g, "they're"
				return true;
			if (chars[i + 1] == 'v' && chars[i + 2] == 'e') // e.g, "they've"
				return true;
			if (chars[i + 1] == 'l' && chars[i + 2] == 'l') // e.g, "she'll"
				return true;
		} else if (i <= chars.length - 2) {
			if (chars[i + 1] == 'm') // e.g, "I'm"
				return true;
			if (chars[i + 1] == 'd') // e.g, "it'd"
				return true;
			if (chars[i + 1] == 't') // e.g, "it'd"
				return true;
		}
		return false;
	}

	private void removeQuotationSymbols(char[] chars) {
		for (int i = 0; i < chars.length; i++) {
			if (quoteSymbols.contains(chars[i])) {
				if (i == 0) {// first character
					chars[i] = ' ';
					continue;
				} else if (i == chars.length - 1) {// last character
					chars[i] = ' ';
					continue;
				} else if (chars[i - 1] == ' ') {// first character of the word
					chars[i] = ' ';
					continue;
				} else if (chars[i + 1] == ' ') {// last character of the word
					chars[i] = ' ';
					if (chars[i - 1] == 's') { // for the case, e.g., "mothers'"
						chars[i - 1] = ' ';
					}
					continue;
				} else if (chars[i + 1] == 's') {// for the case, e.g.,
													// "mother's"
					chars[i] = ' ';
					chars[i + 1] = ' ';
				} else if (i < chars.length - 2) {
					if (chars[i + 2] == ' ') // for the case, e.g., "don't"
						continue;
				} else if (isShorten(chars, i)) {
					continue;
				} else {
					chars[i] = ' ';
				}
			}
		}
	}

	private void removeHTMLsymbols(char[] chars) {
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '&') {// html character
				int j = i;
				while (j < chars.length) {
					if (chars[j] == ' ')
						break;
					else if (punctuations.contains(chars[j]))
						break;
					else if (validPrefixSymbols.contains(chars[j]))
						break;
					else {
						chars[j] = ' ';
						j++;
					}
				}
				i = j;
			}
		}
	}

	private int getWord(char[] chars, int i) {
		int j = i;
		while (true) {
			if (j >= chars.length)
				break;
			else if (chars[j] == ' ') {
				break;
			} else {
				j++;
			}
		}
		return j;
	}

	private boolean isNumber(char[] chars, int start, int end) {
		for (int i = start; i < end; i++) {
			if (Character.isDigit(chars[i])) {
				continue;
			}
			if (chars[i] == '.') {
				continue;
			}
			if (chars[i] == ',') {
				continue;
			}
			return false;
		}
		return true;
	}

	private boolean isHour(char[] chars, int start, int end) {
		for (int i = start; i < end; i++) {
			if (Character.isDigit(chars[i])) {
				continue;
			}
			if (chars[i] == ':') {
				continue;
			}
			return false;
		}
		return true;
	}

	private boolean isURL(String text, int i, int j) {
		return urlValidator.isValid(text.substring(i, j));
	}

	private boolean isEnglish(char[] chars, int start, int end) {
		for (int i = start; i < end; i++)
			if ((int) chars[i] > 128)
				return false;
		return true;
	}

	private boolean containCharacterORNumber(char[] chars, int start, int end) {
		for (int i = start; i < end; i++) {
			if (Character.isLetterOrDigit(chars[i]))
				return true;
		}
		return false;
	}

	private void removeSymbolInWord(char[] chars, int start, int end) {

		// ptScreen(chars);
		// System.out.printf("\ni = %d, j = %d", start, end);

		int i = end - 1;
		while (!Character.isLetterOrDigit(chars[i])) {
			if (validSuffixSymbols.contains(chars[i])) {
				break;
			}
			chars[i] = ' ';
			i--;
			if (i <= start)
				return;
		}

		i = start;
		while (!Character.isLetterOrDigit(chars[i])) {
			if (validPrefixSymbols.contains(chars[i])) {
				break;
			}
			chars[i] = ' ';
			i++;
			if (i >= end)
				return;
		}
	}

	/*private void ptScreen(char[] chars) {
		System.out.printf("array = [");
		for (int i = 0; i < chars.length; i++) {
			System.out.printf("%c", chars[i]);
		}
		System.out.println("]");
	}*/

	private List<String> termExtraction(String text) {
		// System.out.printf("text = %s\n", text);
		char[] chars = text.trim().toCharArray();

		removeNewLineAndTabCharacter(chars);
		removeOriginalAuthors(chars);

		removeHTMLsymbols(chars);
		// System.out.printf("After remove symbol:\t");
		// ptScreen(chars);

		removeQuotationSymbols(chars);
		// System.out.printf("After remove quotation:\t");
		// ptScreen(chars);

		removePunct(chars);
		// System.out.printf("After remove punctuations and tab:\t");
		// ptScreen(chars);

		int i = 0;
		while (i < chars.length) {
			int j = getWord(chars, i);
			if (j == i) {
				i++;
				continue;
			}
			// System.out.printf("\t isNumber = %s", isNumber(chars, i, j));
			// System.out.printf("\t isHours = %s", isHour(chars, i, j));
			// System.out.printf("\t isURL = %s", isURL(text, i, j));
			if (isNumber(chars, i, j)) {
				i = j;
			} else if (isHour(chars, i, j)) {
				i = j;
			} else if (isURL(text, i, j)) {
				i = j;
			} else if (!isEnglish(chars, i, j)) {
				for (int p = i; p < j; p++) {
					chars[p] = ' ';
				}
				i = j;
			} else {
				for (int p = i; p < j; p++) {
					if (punctuations.contains(chars[p])) {
						chars[p] = ' ';
					} else {
						chars[p] = Character.toLowerCase(chars[p]);
					}
				}
				removeSymbolInWord(chars, i, j);
				i = j;
			}
		}

		List<String> terms = new ArrayList<String>();
		i = 0;
		while (i < chars.length) {
			int j = getWord(chars, i);
			if (j == i) {
				i++;
				continue;
			}
			if (containCharacterORNumber(chars, i, j)) {
				String term = new String(chars, i, j - i);
				if (!stopWords.contains(term)) {
					// System.out.printf("\nnew-word: %s", term);
					terms.add(terms.size(), term);
				}
			}
			i = j;
		}

		return terms;
	}

	public List<String> extractTermInTweet(String tweet) {
		// System.out.printf("tweet = [[%s]]\n", tweet);
		List<String> terms = termExtraction(tweet);
		return terms;
	}

	public void checkStopWordList() {
		System.out.println("more: " + stopWords.contains("more"));
		System.out.println("this: " + stopWords.contains("this"));
	}

	public void checkQuoteSymbols() {
		for (char s : quoteSymbols) {
			System.out.printf("c= %c\n", s);
		}
	}
	
	private void removePunct(char[] chars) {
		for (int i = 0; i < chars.length; i++) {
			if (punctuations.contains(chars[i]) && !String.valueOf(chars[i]).matches(Configure.ENDTOKENS)) {// ignore removing ending tokens
				if (i == 0) {// first character
					chars[i] = ' ';
					continue;
				} else if (i == chars.length - 1) {// last character
					chars[i] = ' ';
					continue;
				} else if (chars[i - 1] == ' ') {// first character of the word
					chars[i] = ' ';
					continue;
				} else if (chars[i + 1] == ' ') {// last character of the word
					chars[i] = ' ';
					continue;
				} else if (isURLStart(chars, i + 1)) {// right before url
					chars[i] = ' ';
					continue;
				} else if (chars[i] == '.') {// do nothing, in case of url
					continue;
				} else if (chars[i] == ':') {// do nothing, in case of url
					continue;
				} else if (!Character.isDigit(chars[i - 1])) {
					chars[i] = ' ';
					continue;
				} else if (!Character.isDigit(chars[i + 1])) {
					chars[i] = ' ';
					continue;
				} else {
					// do nothing
					continue;
				}
			}
		}
	}
	public String normalizeString(String text) {
		text = text.trim();
		String end = text.substring(text.length()-1);
		if(!end.matches(Configure.ENDTOKENS))
			text = text.concat(".");
		char[] chars = text.trim().toCharArray();
		
		removeNewLineAndTabCharacter(chars);
		removeOriginalAuthors(chars);

		removeHTMLsymbols(chars);
		// System.out.printf("After remove symbol:\t");
		// ptScreen(chars);

		removeQuotationSymbols(chars);
		// System.out.printf("After remove quotation:\t");
		// ptScreen(chars);

		// only remove punctuations that are different from end tokens
		removePunct(chars);
		// System.out.printf("After remove punctuations and tab:\t");
		// ptScreen(chars);

		int i = 0;
		while (i < chars.length) {
			int j = getWord(chars, i);
			if (j == i) {
				i++;
				continue;
			}
			// System.out.printf("\t isNumber = %s", isNumber(chars, i, j));
			// System.out.printf("\t isHours = %s", isHour(chars, i, j));
			// System.out.printf("\t isURL = %s", isURL(text, i, j));
			if (isNumber(chars, i, j)) {
				i = j;
			} else if (isHour(chars, i, j)) {
				i = j;
			} else if (isURL(text, i, j)) {
				i = j;
			} else if (!isEnglish(chars, i, j)) {
				for (int p = i; p < j; p++) {
					chars[p] = ' ';
				}
				i = j;
			} else {
				for (int p = i; p < j; p++) {
					/*if (punctuations.contains(chars[p])) {
						chars[p] = ' ';
					} else {*/
						chars[p] = Character.toLowerCase(chars[p]);
					//}
				}
				//removeSymbolInWord(chars, i, j); // this will remove . at the end of a word
				i = j;
			}
		}
		String result = String.valueOf(chars);
		return result.trim();
	}
	/**
	 * @author Huyen Nguyen
	 * @param text: a string
	 * @return the string that is tokenized and tagged
	 */
	public List<TaggedToken> getTaggedTokens(String text) {
		
		List<TaggedToken> taggedTokens = tagger.tokenizeAndTag(text);
		return taggedTokens;
	}
	
	public static void main(String[] args) {
		new Configure();
		// String text = "trumpâ€™s";
		// char a = '\u2019';
		// System.out.println("code = " + a);

		// char c = '\u2026'; // System.out.println("c = " + (int) c);

		TweetPreprocessingUtils nlpUtils = new TweetPreprocessingUtils();
		// nlpUtils.checkQuoteSymbols();

		// nlpUtils.checkStopWordList();

		String message = "Neil Gorsuch on the Supreme Court: Facts about Donald Trump's pick https://t.co/BVrO15VF7k by #ForexBTC via @c0nvey";
		System.out.printf("message = [[%s]]\n", message);
		List<String> terms = nlpUtils.extractTermInTweet(message);

		for (int i = 0; i < terms.size(); i++) {
			System.out.printf("\ni = %d term = |%s|", i, terms.get(i));
		}

		
		//List<TaggedToken> result = nlpUtils.getTaggedTokens(message);
	}

}
