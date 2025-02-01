package engine.math;

import java.util.Arrays;

/**
 * Liksom man går til max val og så 0. Om man så går under 0 så betyr det at man går ned en slide...
 * Tenk liksom 9 -> 10 eller b -> 10 (12)
 */
public class Val {
    public double val;
    public long[] slides = new long[0]; // 0 -> 1 is Double.MaxValue + 0. This is not infinite, but very large indeed.

//    private int compInc(long l) {
//
//    }

    /**
     * @return 1, 0, -1 = go up, no change, go down.
     */
//    private int compInc(double l) {
//
//    }

//    public void add(double add) {
//        var prev = val;
//        val += add;
//        if (compInc(val) > 0) {
//            val = add - (Double.MAX_VALUE - prev);
//            slide++;
//        } else if (val < 0) {
//            val = Double.MAX_VALUE - val;
//            slide--;
//        }
//    }

    public void addToNumber(int index, long amount, long min, long max) { // burde lage tester spesifikt mot denne her feks.
        if (index >= slides.length) {
            slides = Arrays.copyOf(slides, index + 1);
        }

        var prev = slides[index];
        var val = prev;
        val += amount;
        if (val >= max) {
            addToNumber(index + 1, 1, min, max);
            val = 0; // egt ikke 1 men rest? liksommmmmmmmmmmmm man må gjøre denne her på nytt likevel også.....
        } else if (val < min) {
            addToNumber(index + 1, -1, min, max);
            val = max - 1;
        } else {
            return;
        }

        val += Math.signum(amount) * ((Math.abs(prev) - max) + Math.abs(amount));
        slides[index] = val;
    }

    public void addSliders(Val add) {
        if (slides.length < add.slides.length) {
            slides = Arrays.copyOf(slides, add.slides.length);
        }
        for (var i = 0; i < add.slides.length; i++) {
            addToNumber(i, add.slides[i], 0, Long.MAX_VALUE);
        }
    }

//    public void add(Val add) { // sjekk om add + prev >= maxval. ta så add - (maxval - prev) og set det som
//        var prev = val;
//        if (prev >= 0) {
//            val += add.val;
//            if (val >= Double.MAX_VALUE) {
//                addToNumber(0, 1, 0, Long.MAX_VALUE);
//                val = 0;
//            } else if (val < 0) {
//                addToNumber(0, -1, 0, Long.MAX_VALUE); // hva om siste sliders ender mot < 0
//                val = 0;
//            }
//
//            addSliders(add);
//
//            val +=
//        } else {
//
//        }
//    }

    public void mul(double val) {

    }

    public void mul(Val val) {

    }


}
