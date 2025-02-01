package audio;

import player_local.car.Car;
import player_local.upgrades.TileNames;
import player_local.upgrades.Upgrade;
import engine.math.Vec3;

public interface AudioRemote {

	void setVolume(AudioTypes type, float volume);
	float getVolume(AudioTypes type);

	void updateVolumeSfx();
	void updateVolumeMusic();
	
	CarAudio getNewCarAudio(String carname);
	
	void stop(SfxTypes sfx);
	void play(SfxTypes sfx);
	void playUpgrade(TileNames upgrade);
	void playUpgradeHover(Upgrade upgrade);
	Source getTaunt(int i);
	void checkMusic();
	void setListenerData(float x, float y, float z);
	void setListenerData(Vec3 vector);
	void updateCarAudio(Car car);
}
