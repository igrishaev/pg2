package org.pg.reducer;

public interface IReducer {
    Object compose(Object[] keys, Object[] vals);
    Object initiate();
    Object append(Object acc, Object row);
    Object finalize(Object acc);
}
