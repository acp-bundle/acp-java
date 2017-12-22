package co.clai.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RandomUtil {

	public static String getRandomString() {
		return getRandomString(130);
	}

	public static String getRandomString(int keyLength) {
		SecureRandom random = new SecureRandom();
		return new BigInteger(keyLength, random).toString(Character.MAX_RADIX);
	}
}
