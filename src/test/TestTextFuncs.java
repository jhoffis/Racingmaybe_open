package test;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import main.Texts;

public class TestTextFuncs {
	
	@Test
	void textFormatNumbers() {
		Assert.assertTrue(
				Texts.formatNumber(Double.POSITIVE_INFINITY)
				.length() != 0);
		
		try {
			Double.parseDouble(Texts.formatNumber(10000000));
			Assert.assertTrue(false);
		} catch (NumberFormatException e) {
			Assert.assertTrue(true);
		}
		for (int i = 0; i < 100000; i++)
//			System.out.println(
					Texts.formatNumber(Double.MAX_VALUE)
					;
//					);
	}
	
	@Test
	void textNumberEndings() {
		Assert.assertTrue(
				Texts.getNumberEndings(Integer.MAX_VALUE)
				.length() != 0);
	}

	@Test
	void textSimpleFormat() {
//		String str1 = Texts.formatNumberSimple(999);
//		String str2 = Texts.formatNumberSimple(124092837400L);
//		String str3 = Texts.formatNumberSimple(1000);
//		String str4 = Texts.formatNumberSimple(2020000);
		
		int score = Integer.MAX_VALUE;
		int car = 2;
		int id = ((int) Math.ceil((double) score / 1500d) * 1000) + car;
//		id = car;
		System.out.println(id);
		if (id >= 1000) {
			var str = String.valueOf(id);
			id = Integer.parseInt(str.substring(str.length() - 3));
		}
		System.out.println(id);
	}
}
