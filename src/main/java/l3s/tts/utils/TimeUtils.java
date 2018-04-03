package l3s.tts.utils;

public class TimeUtils {
	public static int getElapsedTime(long currentTime, long refTime, long stepWidth) {
		return (int) ((currentTime - refTime) / stepWidth);
	}
}
