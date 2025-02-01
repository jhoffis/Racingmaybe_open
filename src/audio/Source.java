package audio;

import org.lwjgl.openal.AL11;

import engine.math.Vec3;

/**
 * NB! Posisjon av lyd og velocity fungerer bare med MONO og ikke STEREO!!!
 */
public class Source {

	private int sourceID, currentBuffer;

	public Source() {
		sourceID = AL11.alGenSources();
		AL11.alSourcef(sourceID, AL11.AL_ROLLOFF_FACTOR, 2f);
		AL11.alSourcef(sourceID, AL11.AL_REFERENCE_DISTANCE, 8);
		AL11.alSourcef(sourceID, AL11.AL_MAX_DISTANCE, 2000);
		
		volume(1);
		pitch(1);
		position(0, 0, 0);
	}
	
	public Source(int buffer) {
		this();
		AL11.alSourcei(sourceID, AL11.AL_BUFFER, buffer);
		this.currentBuffer = buffer;
	}

	public void play(int buffer) {
		stop();
		AL11.alSourcei(sourceID, AL11.AL_BUFFER, buffer);
		this.currentBuffer = buffer;
		resume();
	}
	
	public void play() {
		stop();
		resume();
	}
	
	public void play(float pitch) {
		pitch(pitch);
		play();
	}

	public void stop() {
		AL11.alSourceStop(sourceID);
	}

	public void pause() {
		AL11.alSourcePause(sourceID);
	}

	public void resume() {
		AL11.alSourcePlay(sourceID);
	}

	public void velocity(float x, float y, float z) {
		AL11.alSource3f(sourceID, AL11.AL_VELOCITY, x, y, z); 
	}

	public void loop(boolean loop) {
		AL11.alSourcei(sourceID, AL11.AL_LOOPING,
				loop ? AL11.AL_TRUE : AL11.AL_FALSE);
	}

	public void volume(float volume) {
		AL11.alSourcef(sourceID, AL11.AL_GAIN, volume);
	}

	public void pitch(float pitch) {
		AL11.alSourcef(sourceID, AL11.AL_PITCH, pitch);
	}

	public void position(Vec3 position) {
		AL11.alSource3f(sourceID, AL11.AL_POSITION, position.x, position.y, position.z);
	}
	
	public void position(float x, float y, float z) {
		AL11.alSource3f(sourceID, AL11.AL_POSITION, x, y, z);
	}

	public boolean isPlaying() {
		return AL11.alGetSourcei(sourceID,
				AL11.AL_SOURCE_STATE) == AL11.AL_PLAYING;
	}
	
	public void delete() {
		AL11.alDeleteSources(sourceID);
	}
	
	public void destroy() {
		deleteBuffer();
		AL11.alDeleteSources(sourceID);
	}

	public int getBuffer() {
		return AL11.alGetBufferi(sourceID, AL11.AL_BUFFER);
	}
	
	public void deleteBuffer() {
		stop();
		AL11.alSourcei(sourceID, AL11.AL_BUFFER, 0);
		AL11.alDeleteBuffers(currentBuffer);
		currentBuffer = 0;
	}
}

