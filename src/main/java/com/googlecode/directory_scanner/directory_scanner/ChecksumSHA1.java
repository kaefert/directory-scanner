package com.googlecode.directory_scanner.directory_scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumSHA1 {

    public static byte[] createChecksum(File file) throws NoSuchAlgorithmException, IOException {
	return createChecksum(new FileInputStream(file));
    }

    public static byte[] createChecksum(String filename) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
	return createChecksum(new FileInputStream(filename));
    }

    public static byte[] createChecksum(InputStream fis) throws NoSuchAlgorithmException, IOException {
	byte[] buffer = new byte[1024];
	MessageDigest complete = MessageDigest.getInstance("SHA1");
	int numRead;
	do {
	    numRead = fis.read(buffer);
	    if (numRead > 0) {
		complete.update(buffer, 0, numRead);
	    }
	} while (numRead != -1);
	fis.close();
	return complete.digest();
    }

    public static String getSHA1Checksum(Path path) throws NoSuchAlgorithmException, IOException, FileNotFoundException {
	return getSHA1Checksum(path.toFile());
    }

    public static String getSHA1Checksum(File file) throws NoSuchAlgorithmException, IOException {
	return getSHA1Checksum(new FileInputStream(file));
    }

    public static String getSHA1Checksum(String filename) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
	return getSHA1Checksum(new FileInputStream(filename));
    }

    public static String getSHA1Checksum(FileInputStream stream) throws NoSuchAlgorithmException, IOException {
	byte[] b = createChecksum(stream);
	return bytesToString(b);
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String bytesToString(byte[] b) {
	String result = "";
	for (int i = 0; i < b.length; i++) {
	    result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
	}
	return result;
    }

    // public static void main(String args[]) {
    // try {
    // System.out
    // .println(getSHA1Checksum("c:/temp/isapi_redirect-1.2.30.dll"));
    // // output :
    // // cca9176f72ff56beb1f76c21b1d7daa6be192890
    // // ref :
    // // http://tomcat.apache.org/
    // // dev/dist/tomcat-connectors/
    // // jk/binaries/win32/jk-1.2.30/
    // // isapi_redirect-1.2.30.dll.sha1
    // //
    // // cca9176f72ff56beb1f76c21b1d7daa6be192890
    // // *isapi_redirect-1.2.30.dll
    //
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
}