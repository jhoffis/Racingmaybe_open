package test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.Test;

public class TestSlowness {

	@Test
	void testSlow() {
//		var ran = new Random();
//		var bb = ByteBuffer.allocateDirect(1000);
//		
//		while (bb.position() < bb.limit()) {
//			bb.put((byte) ran.nextInt());
//		}
//		
//		// convert the message
//		String converted = null;
//		byte[] bytes = new byte[bb.limit()];
//		for (int i = 0; i < bytes.length; i++) {
//			bytes[i] = bb.get(i);
//		}
//		converted = new String(bytes, StandardCharsets.UTF_8);
//		System.out.println("Popped, converted msg by steamremote: " + converted);
		System.out.println("Popped, converted msg by steamremote: ");
	}
	
}
