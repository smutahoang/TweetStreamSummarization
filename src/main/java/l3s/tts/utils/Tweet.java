package l3s.tts.utils;

import java.util.List;

import cmu.arktweetnlp.Tagger.TaggedToken;

public class Tweet {
	private int tweetId;
	private String text;
	private String userId;
	private long createdAt;
	private double weight;
	
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public double getWeight() {
		return weight;
	}
	
	private List<TaggedToken> taggedTokens;
	
	public static String RETWEET = "RT @";
	
	public Tweet(String text, String userId, long createdAt) {
		// TODO Auto-generated constructor stub
		//this.tweetId = tweetId;
		this.userId = userId;
		this.text = text;
		this.createdAt = createdAt;
		this.weight = 1.0;
		taggedTokens = null;
	}
	
	public int getTweetId() {
		return tweetId;
	}
	
	public void setTweetId(int tweetId) {
		this.tweetId = tweetId;
	}
	
	public String getText() {
		return text;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public long getPublishedTime () {
		return createdAt;
	}
	
	public boolean isReTweet() {
		return text.trim().startsWith(RETWEET);
	}
	
	public void print() {
		System.out.printf("time = %d user = %s tweetId = %s text = %s\n", createdAt, userId, tweetId, text);
	}
	
	public List<TaggedToken> getTaggedTokens(TweetPreprocessingUtils preprocessingUtils) {
		String normalizedText = preprocessingUtils.normalizeString(text);
		text = normalizedText;
		if(taggedTokens == null)
			taggedTokens = preprocessingUtils.getTaggedTokens(text);
		/*for(int i = 0; i<taggedTokens.size(); i++) {
			System.out.print(taggedTokens.get(i).token+"/"+taggedTokens.get(i).tag+" ");
		}
		System.out.println();*/
		return taggedTokens;
	}
}
