package org.pg.reducer;

public class Dummy implements IReducer {

    public static IReducer INSTANCE = new Dummy();

    public Object compose(final Object[] keys, final Object[] vals) {
        return null;
    }

    public Object initiate(final Object[] ignored) {
        return null;
    }

    public Object append(final Object acc, final Object row) {
        return acc;
    }

    public Object finalize(final Object acc) {
        return acc;
    }
}
