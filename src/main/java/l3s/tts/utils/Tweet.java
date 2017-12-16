package l3s.tts.utils;

import java.util.List;

import cmu.arktweetnlp.Tagger.TaggedToken;

public class Tweet {
	private String tweetId;
	private String text;
	private String userId;
	private long createdAt;
	
	private List<TaggedToken> taggedTokens;
	
	public static String RETWEET = "RT @";
	
	public Tweet(String tweetId, String text, String userId, long createdAt) {
		// TODO Auto-generated constructor stub
		this.tweetId = tweetId;
		this.userId = userId;
		this.text = text;
		this.createdAt = createdAt;
		
		taggedTokens = null;
	}
	
	public String getTweetId() {
		return tweetId;
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
		///String normalizedText = preprocessingUtils.normalizeString(text);
		//text = normalizedText;
		if(taggedTokens == null)
			taggedTokens = preprocessingUtils.getTaggedTokens(text);
		return taggedTokens;
	}
}
