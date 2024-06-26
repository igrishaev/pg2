package org.pg.clojure;

import clojure.lang.Keyword;

public final class KW {

    public final static Keyword inserted = Keyword.intern("inserted");
    public final static Keyword updated = Keyword.intern("updated");
    public final static Keyword deleted = Keyword.intern("deleted");
    public final static Keyword selected = Keyword.intern("selected");
    public final static Keyword copied = Keyword.intern("copied");
    public final static Keyword command = Keyword.intern("command");
    public final static Keyword msg = Keyword.intern("msg");
    public final static Keyword NoticeResponse = Keyword.intern("NoticeResponse");
    public final static Keyword pid = Keyword.intern("pid");
    public final static Keyword channel = Keyword.intern("channel");
    public final static Keyword message = Keyword.intern("message");
    public final static Keyword version = Keyword.intern("version");
    public final static Keyword paramCount = Keyword.intern("param-count");
    public final static Keyword params = Keyword.intern("params");
    public final static Keyword NegotiateProtocolVersion = Keyword.intern("NegotiateProtocolVersion");


    public static void main(String... args) {
        System.out.println(inserted);
    }

}
