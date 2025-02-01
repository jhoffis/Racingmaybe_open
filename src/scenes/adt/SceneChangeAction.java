package scenes.adt;

public interface SceneChangeAction {
	/**
	 * @return Scene it changes into
	 */
	Scene change(int scenenr, boolean logCurrent, int... args);
}
