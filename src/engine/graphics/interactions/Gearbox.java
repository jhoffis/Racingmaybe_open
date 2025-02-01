package engine.graphics.interactions;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import engine.math.Vec2;
import org.lwjgl.nuklear.Nuklear;

import audio.AudioRemote;
import audio.SfxTypes;
import engine.graphics.objects.Sprite;
import engine.graphics.ui.CursorType;
import engine.graphics.ui.UILabel;
import engine.graphics.Renderer;
import engine.io.Window;
import main.Features;
import player_local.car.Car;
import player_local.car.CarFuncs;
import player_local.car.Rep;
import scenes.game.racing_subscenes.RaceVisual;

public class Gearbox {

    private final Car car;
    private AudioRemote audio;
    private Sprite gearbox;
    private Sprite gearboxLever;
    // all from whatever position of the gearbox.
    private float fromX, yNeutral, xUnit, yUnit;
    private float leverXTarget, leverYTarget;
    private int gearColumn, gearSlots, held;

    private float xMouse, yMouse, xMousePrev, yMousePrev;
    private boolean playedWhoosh;

    private boolean doTimeShifted;
    private long fromTimeShifted;
    private LinkedList<String> timeShifted;
    private Queue<Long> timeShiftedAtTime;

    private long fromTimePowerloss;
    private String powerlossText;
    private boolean mustReleaseThrottle;
    private int fromShiftGear;

    private int lastRealGear = 0;
    private int gearLowerSuggestion = -2;
    private float gearLowerWait;
	private int lastAnyGear;

    public Gearbox(Car car, AudioRemote audio) {
        this.car = car;
        this.audio = audio;

        timeShifted = new LinkedList<>();
        timeShiftedAtTime = new LinkedList<>();
        resetAndUpdateGearTop(true);
    }

    public void resetAndUpdateGearTop(boolean force) {
        int newGearTop = (int) (car.getRep().is(Rep.sequential) ? 2 : car.getRep().getInt(Rep.gearTop));

        if (newGearTop != gearSlots || force) {
            float lastGBPosX = -1;
            if (gearbox != null)
                lastGBPosX = gearbox.position().x;
            setGearTop(newGearTop);
//			if(newGearTop == 2) {
//				if (lastGBPosX != -1) {
//					float thisGBPosX = gearbox.position().x;
//					fromX = Math.abs(thisGBPosX - lastGBPosX);
//				} else {
//					fromX = 0;
//				}
//			}
        }

        timeShifted.clear();
        timeShiftedAtTime.clear();
        held = 0;
        xMouse = 0;
        yMouse = 0;
        xMousePrev = 0;
        yMousePrev = 0;
        leverXTarget = 0;
        leverYTarget = 0;
        lastAnyGear = 0;
        lastRealGear = 0;
        gearLowerSuggestion = -2;
        gearLowerWait = 0;
    }

    private void setGearTop(int gears) {
        boolean alreadySet = gearbox != null;
        if (alreadySet)
            gearbox.destroy();

        this.gearSlots = gears;
//		System.out.println("gears: " + gears); 
        gearbox = new Sprite(Window.HEIGHT / 3f, "./images/gearbox" + gears + ".png", "main");
        gearbox.create();
        gearbox.setPosition(new Vec2(Window.WIDTH - gearbox.getWidth(), gearbox.getHeight() / 2));
//		System.out.println("gearposx " + gearbox.position().x + ", " + (Window.WIDTH - gearbox.getWidth()));
        if (!alreadySet) {
            float size = gearbox.getHeight() / (53f / 11f);
            gearboxLever = new Sprite(new Vec2(0, this.gearbox.position().y), size, "./images/gearboxlever.png", "gearboxlever");
            gearboxLever.create();
        }
        fromX = gearbox.position().x;
        updateResolution();
    }

    public void updateResolution() {
        yNeutral = gearbox.position().y + gearbox.getHeight() / 2;
        xUnit = gearboxLever.getWidth() * 13 / 11; // pixel avstander på gearbox texture
        yUnit = gearboxLever.getHeight() * 7 / 11;
    }
    public void pressPerfectly() {
    	var p = gearboxLever.position();
    	
    	xMouse = p.x;
    	yMouse = p.y;
    	xMousePrev = xMouse;
    	yMousePrev = yMouse;
    	held = 1;
    }
    
    // Used with sequential shifting
    public void press(float x, float y) {
        if (held == 0) {
            if (gearSlots > 2) {
                if (gearboxLever.above(x, y)) {
                    held = 1;
                    // change mouse
                    Features.inst.getWindow().setCursor(CursorType.cursorIsHold);
                }
            } else {
                if (gearbox.above(x, y)) {
                    Features.inst.getWindow().setCursor(CursorType.cursorIsPoint);
                    var now = System.currentTimeMillis();
                    if (y < yNeutral) {
                        held = 1;
                        car.shiftDown(now);
                    } else {
                        held = 2;
                        updateThrottleStats(false);
                        car.shiftUp(now);
                    }
                }
            }
            move(x, y);
        }
    }

    public void release(float x, float y) {
        held = 0;
        lastAnyGear = 0;
        car.getStats().grinding = false;

        if (gearSlots > 2) {
            if (gearboxLever.above(x, y)) {
                Features.inst.getWindow().setCursor(CursorType.cursorCanHold);
                return;
            }
        } else {
            if (gearbox.above(x, y)) {
                Features.inst.getWindow().setCursor(CursorType.cursorCanPoint);
                return;
            }
        }
        Features.inst.getWindow().setCursor(CursorType.cursorNormal);
    }

    public void move(float x, float y) {
//		leverXTarget, leverYTarget
        if (gearSlots > 2) {
            if (held != 0) {
                var sizeMoveSpace = gearboxLever.getHeight() / 2;

                if (y > yNeutral - sizeMoveSpace && y < yNeutral + sizeMoveSpace) {
                    // if inside of neutral area, smooth else lock the x on intervals
                    leverXTarget = x - gearbox.position().x - gearboxLever.getWidth() / 2;
                    gearColumn = 0;
                } else {
                    // if x is within a space, allow for smooth movement in y.
                    leverYTarget = y - gearbox.position().y - gearboxLever.getHeight() / 2;
                    // what big x space is closest?
                    var gearspace = gearbox.getWidth() / Math.ceil(gearSlots / 2f);
                    gearColumn = 0;
                    int gearColumnAmount = (gearSlots + gearSlots % 2) / 2;
                    for (int i = 1; i < gearColumnAmount; i++) {
                        if (x < gearbox.position().x + gearspace * i) {
                            // left
                            gearColumn = i;
                            break;
                        }
                    }
                    if (gearColumn == 0)
                        gearColumn = gearColumnAmount;
                }
                //			System.out.println("y:" + y + " x: " + leverXTarget + ", " + yNeutral + ", " + sizeMoveSpace);
            } else if (gearboxLever.above(x, y)) {
                // change mouse grabby
                Features.inst.getWindow().setCursor(CursorType.cursorCanHold);
            } else {
                // change mouse pointy or normal
                Features.inst.getWindow().setCursor(CursorType.cursorNormal);
            }
        } else if (gearbox.above(x, y)) {
            Features.inst.getWindow().setCursor(CursorType.cursorCanPoint);
        } else {
            // change mouse pointy or normal
            Features.inst.getWindow().setCursor(CursorType.cursorNormal);
        }

        xMousePrev = xMouse;
        yMousePrev = yMouse;
        if (held != 0) {
            xMouse = x;
            yMouse = y;
        }
    }

    public void tick(float tickFactor) {
        // check for shift resistance which slows down movement of the lever. So it goes towards a point slowly in tick

        float x = 0;
        float y = 0;
        var now = System.currentTimeMillis();

        // Checks if your car does not have sequentialshift
        if (gearSlots > 2) {
            final int gear = car.getStats().gear;
            if (gear != 0)
            	lastRealGear = gear;
        	lastAnyGear = gear;
            GearboxShift typeShift = GearboxShift.nothing;

            // up down
            // gearcolumn = 0 means that youre in neutral
            if (held == 0 && gear > 0 || held != 0 && gearColumn > 0) {
                int top = 0;
                if (held == 0) {
                    // if you are not holding the lever move the lever to the top and into its slot
                    gearColumn = (gear + 1) / 2;
                    y = gear % 2 == 1 ? yUnit : gearbox.getHeight() - yUnit - gearboxLever.getHeight();
                } else {
                    // holds down and is not in neutral. Smooth drag up and down.
                    y = leverYTarget;

                    top = -1;

                }
                float bottomLimit = 0;
                if (gearColumn * 2 <= gearSlots)
                    bottomLimit = gearbox.getHeight() - yUnit - gearboxLever.getHeight();
                else
                    bottomLimit = gearbox.getHeight() / 2 - gearboxLever.getHeight() / 2;

                float joinHeight = gearboxLever.getHeight() / 2;
                if (y < yUnit + joinHeight) {
                    // top
                    top = 1;
                    y = yUnit;
                } else if (y > bottomLimit - joinHeight) {
                    // bottom
                    top = 0;
                    y = bottomLimit;
                }
                var gearSuggestion = gearColumn * 2 - top;

                if (gearSuggestion != lastAnyGear && top != -1) {
//                    System.out.println("gearSuggestion: " + gearSuggestion + ", last: " + gearLowerSuggestion + " time: " + System.currentTimeMillis());
                    if (gearSuggestion < lastRealGear) {
                        if (gearLowerSuggestion == gearSuggestion) {
                            if (gearLowerWait > 3.5f
                            || car.getStats().speed < Car.funcs.gearMax(gearSuggestion, car.getStats(), car.getRep())) {
                                typeShift = car.shift(gearSuggestion, now);
                            }
                        } else {
                            gearLowerSuggestion = gearSuggestion;
                            gearLowerWait = 0;
                        }
                        gearLowerWait += tickFactor;
                    } else {
                        gearLowerSuggestion = -2;
                        gearLowerWait = 0;
                        typeShift = car.shift(gearSuggestion, now);
                    }
                }

                x =
                        gearColumn * gearboxLever.getWidth() // hvit bar bredde
                                + (gearColumn - 1) * xUnit; // mørk bar i mellom de hvite.
            } else {
                //Is in neutral. Drag the lever smooth right to left
                if (held == 0)
                    x = gearbox.getWidth() / 2 - gearboxLever.getWidth() / 2;
                else {
                    x = leverXTarget;
                    float leftLimit = gearboxLever.getWidth();
                    float rightLimit = gearbox.getWidth() - gearboxLever.getWidth() * 2;
                    if (x < leftLimit)
                        x = leftLimit;
                    else if (x > rightLimit)
                        x = rightLimit;
                }
                typeShift = car.shift(0, now);
                y = gearbox.getHeight() / 2 - gearboxLever.getHeight() / 2;
            }

            if (typeShift == GearboxShift.neutral) {
                fromShiftGear = gear;
                if (car.getRep().is(Rep.throttleShift)) {
                    updateThrottleStats(false);
                }
            } else if (typeShift == GearboxShift.shifted) {
                doTimeShifted = true;
                if (car.getRep().is(Rep.throttleShift)) {
                    updateThrottleStats(true);
                }
            }

            mustReleaseThrottle = typeShift == GearboxShift.grind;

        } else {

            x = gearboxLever.getWidth();

            y = switch (held) {
                case 0 -> gearbox.getHeight() / 2 - gearboxLever.getHeight() / 2;
                case 1 -> yUnit;
                case 2 -> gearbox.getHeight() - gearboxLever.getHeight() - yUnit;
                default -> y;
            };
        }

        if (held != 0) {
            float distanceMove = 0;
            Vec2 gbPos = gearbox.position();
            if ((xMouse >= gbPos.x || xMousePrev >= gbPos.x) && (xMouse <= gbPos.x + gearbox.getWidth() || xMousePrev <= gbPos.x + gearbox.getWidth())
                    && (yMouse >= gbPos.y || yMousePrev >= gbPos.y) && (yMouse <= gbPos.y + gearbox.getHeight() || yMousePrev <= gbPos.y + gearbox.getHeight())) {
                distanceMove = Math.abs(xMouse - xMousePrev);
                if (Math.abs(yMouse - yMousePrev) > distanceMove)
                    distanceMove = Math.abs(yMouse - yMousePrev);
                distanceMove *= tickFactor;

            }
            if (!playedWhoosh && distanceMove > 50) {
                audio.play(SfxTypes.WHOOSH);
                playedWhoosh = true;
            } else if (playedWhoosh && distanceMove < 4) {
//				audio.get(SfxTypes.WHOOSH).stop();
                playedWhoosh = false;
            }
        }

        gearboxLever.setPositionX(fromX + x); // x i tillegg til plassering av mesh som gjøres ved init.
        gearboxLever.setPositionY(y);
    }

    public void updateThrottleStats(boolean down) {
        long now = System.currentTimeMillis();

        if (down) {
            if (doTimeShifted) {
                timeShifted.add("Time shifting " + fromShiftGear + "->" + car.getStats().gear + ": " + (int) (now - fromTimeShifted) + "ms ");
                timeShiftedAtTime.add(now);
                doTimeShifted = false;
            }
        } else {
            double powerloss = car.calcPowerloss(true);
            powerlossText = "RPM: " + (powerloss > 0 ? "-" : "+") + Math.abs(powerloss) + "% ";
            timeShifted.add(powerlossText);
            timeShiftedAtTime.add(now);

            fromTimeShifted = now;
        }
    }

    public void render(Renderer renderer) {
        renderer.renderOrthoMesh(gearbox);
        gearboxLever.getShader().setUniform("possible", car.canShift() ? 1f : 0f);
        renderer.renderOrthoMesh(gearboxLever);
    }

    public UILabel[] getTimeShifted() {
        if (!timeShiftedAtTime.isEmpty() && System.currentTimeMillis() > timeShiftedAtTime.peek() + 2000) {
            timeShiftedAtTime.poll();
            timeShifted.poll();
        }

        int i = (mustReleaseThrottle ? 1 : 0);
        UILabel[] res = new UILabel[timeShifted.size() + i];

        if (mustReleaseThrottle)
            res[0] = new UILabel("Release throttle! #R", Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE);

        Iterator<String> it = timeShifted.descendingIterator();

        while (it.hasNext()) {
            res[i] = new UILabel(it.next(), Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
            i++;
        }

        return res;
    }

    public Sprite getGearbox() {
        return gearbox;
    }

    public Sprite getGearboxLever() {
        return gearboxLever;
    }

    public String getPowerloss() {
        if (System.currentTimeMillis() <= fromTimePowerloss && powerlossText != null)
            return powerlossText;
        return "";
    }

    public void destroy() {
        gearbox.destroy();
        gearboxLever.destroy();
    }

    public void addLog(String str) {
        timeShifted.add(str);
        timeShiftedAtTime.add(System.currentTimeMillis());
    }

    public boolean isHolding() {
        return held != 0;
    }

    public int getHeld() {
        return held;
    }
}