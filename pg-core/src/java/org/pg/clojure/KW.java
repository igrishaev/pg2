package org.pg.clojure;

import clojure.lang.Keyword;

public final class KW {

    public final static Keyword sql = Keyword.intern("sql");
    public final static Keyword inserted = Keyword.intern("inserted");
    public final static Keyword updated = Keyword.intern("updated");
    public final static Keyword deleted = Keyword.intern("deleted");
    public final static Keyword selected = Keyword.intern("selected");
    public final static Keyword copied = Keyword.intern("copied");
    public final static Keyword command = Keyword.intern("command");
    public final static Keyword msg = Keyword.intern("msg");
    public final static Keyword NoticeResponse = Keyword.intern("NoticeResponse");
    public final static Keyword NotificationResponse = Keyword.intern("NotificationResponse");
    public final static Keyword self_QMARK = Keyword.intern("self?");
    public final static Keyword pid = Keyword.intern("pid");
    public final static Keyword channel = Keyword.intern("channel");
    public final static Keyword message = Keyword.intern("message");
    public final static Keyword version = Keyword.intern("version");
    public final static Keyword paramCount = Keyword.intern("param-count");
    public final static Keyword params = Keyword.intern("params");
    public final static Keyword NegotiateProtocolVersion = Keyword.intern("NegotiateProtocolVersion");
    public final static Keyword oid = Keyword.intern("oid");
    public final static Keyword type = Keyword.intern("type");
    public final static Keyword dim = Keyword.intern("dim");
    public final static Keyword nnz = Keyword.intern("nnz");
    public final static Keyword index = Keyword.intern("index");
    public final static Keyword x = Keyword.intern("x");
    public final static Keyword y = Keyword.intern("y");
    public final static Keyword a = Keyword.intern("a");
    public final static Keyword b = Keyword.intern("b");
    public final static Keyword c = Keyword.intern("c");
    public final static Keyword r = Keyword.intern("r");
    public final static Keyword x1 = Keyword.intern("x1");
    public final static Keyword y1 = Keyword.intern("y1");
    public final static Keyword x2 = Keyword.intern("x2");
    public final static Keyword y2 = Keyword.intern("y2");
    public final static Keyword closed_QMARK = Keyword.intern("closed?");
    public final static Keyword points = Keyword.intern("points");
    public final static Keyword typname = Keyword.intern("typname");
    public final static Keyword typtype = Keyword.intern("typtype");
    public final static Keyword typinput = Keyword.intern("typinput");
    public final static Keyword typoutput = Keyword.intern("typoutput");
    public final static Keyword typreceive = Keyword.intern("typreceive");
    public final static Keyword typsend = Keyword.intern("typsend");
    public final static Keyword typarray = Keyword.intern("typarray");
    public final static Keyword typdelim = Keyword.intern("typdelim");
    public final static Keyword typelem = Keyword.intern("typelem");
    public final static Keyword nspname = Keyword.intern("nspname");

    public static void main(String... args) {
        System.out.println(inserted);
    }

}
