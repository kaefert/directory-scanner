package com.googlecode.directory_scanner.workers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumSHA1Calculator {

    public static class Sha1WithSize {
	private byte[] sha1;
	private long bytesRead;

	public Sha1WithSize(byte[] sha1, long bytesRead) {
	    this.sha1 = sha1;
	    this.bytesRead = bytesRead;
	}
	
	public byte[] getSha1() {
	    return sha1;
	}

	public long getBytesRead() {
	    return bytesRead;
	}
    }

    public static Sha1WithSize getSHA1Checksum(File file) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
	return getSHA1Checksum(new FileInputStream(file));
    }

    public static Sha1WithSize getSHA1Checksum(String filename) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
	return getSHA1Checksum(new FileInputStream(filename));
    }

    public static Sha1WithSize getSHA1Checksum(InputStream fis) throws NoSuchAlgorithmException, IOException {
	byte[] buffer = new byte[1024];
	MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
	int readNow = 0;
	long bytesRead = 0;
	do {
	    bytesRead+=readNow;
	    readNow = fis.read(buffer);
	    if (readNow > 0) {
		messageDigest.update(buffer, 0, readNow);
	    }
	} while (readNow != -1);
	fis.close();
	return new Sha1WithSize(messageDigest.digest(), bytesRead);
    }  
//    new Sha1WithSize(messageDigest.digest(), bytesRead).getSha1HexString()

    public static Sha1WithSize getSHA1Checksum(Path path) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
	return getSHA1Checksum(path.toFile());
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
//    public static String bytesToString(byte[] b) {
//	String result = "";
//	for (int i = 0; i < b.length; i++) {
//	    result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
//	}
//	return result;
//    }

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