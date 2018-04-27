package co.clai.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class StringUtil {

	public static byte[] byteArrFromInputStream(InputStream in) {

		ArrayList<Byte> arrayList = new ArrayList<>();
		try {
			int read;
			while ((read = in.read()) >= 0) {
				arrayList.add(new Byte((byte) read));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Iterator<Byte> iterator = arrayList.iterator();
		byte[] bytes = new byte[arrayList.size()];
		int i = 0;
		while (iterator.hasNext()) {
			bytes[i++] = iterator.next().byteValue();
		}

		return bytes;
	}

	/**
	 * From BCrypt class try_bytes = plaintext
	 * 
	 * @return
	 */
	public static boolean checkBytes(byte[] hashed_bytes, byte[] try_bytes) {
		if (hashed_bytes.length != try_bytes.length)
			return false;
		byte ret = 0;
		for (int i = 0; i < try_bytes.length; i++)
			ret |= hashed_bytes[i] ^ try_bytes[i];
		return ret == 0;
	}

	public static boolean isValidFilename(String toTest) {
		return toTest.matches("[a-zA-Z0-9.,_-]*");
	}

	public static boolean containsOnlyNumbersAndLetters(String toTest) {
		return toTest.matches("[a-zA-Z0-9]*");
	}

	public static boolean containsOnlyNumbers(String toTest) {
		return toTest.matches("[0-9]*");
	}

	public static boolean containsOnlyLetters(String toTest) {
		return toTest.matches("[a-zA-Z]*");
	}
}
