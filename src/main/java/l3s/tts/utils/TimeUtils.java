package l3s.tts.utils;

public class TimeUtils {
	public static int getElapsedTime(long currentTime, long refTime, long stepWidth) {
		if (currentTime <= refTime) {
			return 0;
		}
		return (int) ((currentTime - refTime) / stepWidth);
	}
}
