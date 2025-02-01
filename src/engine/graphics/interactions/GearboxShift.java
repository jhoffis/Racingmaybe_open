package engine.graphics.interactions;

public enum GearboxShift {
    grind, nothing, neutral, shifted;

    public boolean didShift() {
        return this.ordinal() > nothing.ordinal();
    }
}
