package org.pg.clojure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public class CljAPI {
    public static IFn keyword = Clojure.var("clojure.core", "keyword");
    public static IFn getIn = Clojure.var("clojure.core", "get-in");
    public static IFn assocIn = Clojure.var("clojure.core", "assoc-in");
}
