package stroom.testdata;

import java.util.concurrent.atomic.AtomicInteger;

class AtomicLoopedIntegerSequence {
    private final int startInc;
    private final int endExc;
    private final AtomicInteger lastVal = new AtomicInteger();

    public AtomicLoopedIntegerSequence(final int endExc) {
        this(0, endExc);
    }
    public AtomicLoopedIntegerSequence(final int startInc, final int endExc) {
        Utils.checkArgument(endExc > startInc, "endExc must be > startInc");

        this.startInc = startInc;
        this.endExc = endExc;
        this.lastVal.set(startInc - 1);
    }

    public int getNext() {
        return lastVal.updateAndGet(val -> {
            int newVal = val + 1;

            if (newVal >= endExc) {
                newVal = startInc;
            }
            return newVal;
        });
    }
}
