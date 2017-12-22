package co.clai.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;

import com.google.common.io.ByteStreams;

public class FileUtil {

	public static String getFileContentAsString(String filename) {
		return getFileContentAsString(new File(filename));
	}

	public static String getFileContentAsString(File file) {
		StringBuilder sb = new StringBuilder();

		try (BufferedReader b = new BufferedReader(new FileReader(file))) {

			while (b.ready()) {
				sb.append(b.readLine() + "\n");
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return sb.toString();
	}

	public static byte[] getFileAsByteArr(String string) {
		try (InputStream in = new FileInputStream(string)) {
			return ByteStreams.toByteArray(in);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
