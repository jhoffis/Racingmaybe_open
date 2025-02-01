package test;

import org.junit.Test;

import audio.AudioMaster;
import audio.SfxTypes;

public class TestSound {

	AudioMaster sound = new AudioMaster();
	
	@Test
	public void testNewEngine() {
		int i = 0;
		while(i < 1000) {
			sound.play(SfxTypes.BOLT_BONUS0);
			i++;
		}
	}
}
