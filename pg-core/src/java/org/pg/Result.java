package org.pg;

import clojure.lang.IFn;
import clojure.lang.PersistentHashMap;
import org.pg.auth.ScramSha256;
import org.pg.clojure.KW;
import org.pg.clojure.RowMap;
import org.pg.error.PGError;
import org.pg.error.PGErrorResponse;
import org.pg.msg.server.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Result {

    public final static class Node {

         private PortalSuspended portalSuspended;
         private RowDescription rowDescription;
         private CommandComplete commandComplete;
         private ParameterDescription parameterDescription;
         private Map<Object, Short> keysIndex;
         private Object[] keys;
         private Object acc;

         private boolean isComplete() {
             return commandComplete != null || portalSuspended != null;
         }

         public Object toResult(final ExecuteParams executeParams) {

             if (rowDescription != null) {
                 return executeParams.reducer().invoke(acc);
             }

             final String command = commandComplete.command();

             final String[] parts = command.split(" +");
             final String lead = parts[0];

             return switch (lead) {
                 case "INSERT" -> PersistentHashMap.create(
                         KW.inserted,
                         Integer.parseInt(parts[2])
                 );
                 case "UPDATE" -> PersistentHashMap.create(
                         KW.updated,
                         Integer.parseInt(parts[1])
                 );
                 case "DELETE" -> PersistentHashMap.create(
                         KW.deleted,
                         Integer.parseInt(parts[1])
                 );
                 case "SELECT" -> PersistentHashMap.create(
                         KW.selected,
                         Integer.parseInt(parts[1])
                 );
                 case "COPY" -> PersistentHashMap.create(
                         KW.copied,
                         Integer.parseInt(parts[1])
                 );
                 default -> PersistentHashMap.create(
                         KW.command,
                         command
                 );
             };

         }
    }

    public final ExecuteParams executeParams;
    private final ArrayList<Node> nodes;
    private ErrorResponse errorResponse;
    private Node current;
    private Throwable exception;
    public ScramSha256.Pipeline scramPipeline;
    private final String sql;
    private int notificationCount;

    public static String[] unifyKeys (final String[] oldKeys) {
        final Map<String, Integer> map = new HashMap<>();
        final String[] newKeys = new String[oldKeys.length];
        for (int i = 0; i < oldKeys.length; i++) {
            final String oldKey = oldKeys[i];
            final int idx = map.getOrDefault(oldKey, 0);
            if (idx == 0) {
                newKeys[i] = oldKey;
                map.put(oldKey, 1);
            }
            else {
                final String newKey = oldKey + "_" + idx;
                newKeys[i] = newKey;
                map.put(oldKey, idx + 1);
            }
        }
        return newKeys;
    }

    public Result(final ExecuteParams executeParams) {
        this(executeParams, null);
    }

    public void incNotificationCount() {
        notificationCount += 1;
    }

    public int getNotificationCount() {
        return notificationCount;
    }

    public Result(final ExecuteParams executeParams, final String sql) {
        this.executeParams = executeParams;
        this.sql = sql;
        nodes = new ArrayList<>(2);
        addNode();
    }

    public void setException(final Throwable e) {
        this.exception = e;
    }

    public boolean hasException () {
        return exception != null;
    }

    public void addErrorResponse (final ErrorResponse msg) {
        errorResponse = msg;
    }

    public void handleParameterDescription(final ParameterDescription msg) {
        current.parameterDescription = msg;
    }

    public void handlePortalSuspended(PortalSuspended msg) {
        current.portalSuspended = msg;
        addNode();
    }

    public RowDescription getRowDescription () {
        return current.rowDescription;
    }

    public ParameterDescription getParameterDescription () {
        return current.parameterDescription;
    }

    public Map<Object, Short> getCurrentKeysIndex () {
        return current.keysIndex;
    }

    public Object[] getCurrentKeys () {
        return current.keys;
    }

    public void handleRowDescription(final RowDescription msg) {
        current.rowDescription = msg;
        final IFn fnKeyTransform = executeParams.fnKeyTransform();
        final String[] names = unifyKeys(msg.getColumnNames());
        final int len = names.length;
        final Object[] keys = new Object[len];
        final Map<Object, Short> keysIndex = new HashMap<>(len);
        for (short i = 0; i < len; i ++) {
            final Object newKey = fnKeyTransform.invoke(names[i]);
            keys[i] = newKey;
            keysIndex.put(newKey, i);
        }
        current.keysIndex = keysIndex;
        current.keys = keys;
        current.acc = executeParams.reducer().invoke();
    }

    /*
    Return an array of oids collected across all nodes.
     */
    public int[][] allOids() {
        final int[][] result = new int[nodes.size()][];
        int i = 0;
        for (Node node: nodes) {
            result[i++] = node.rowDescription == null
                    ? null
                    : node.rowDescription.oids();
        }
        return result;
    }

    public void handleCommandComplete (final CommandComplete msg) {
        current.commandComplete = msg;
        addNode();
    }

    public Object getResult () {
        final ArrayList<Object> results = new ArrayList<>(1);
        for (Node node: nodes) {
            if (node.isComplete()) {
                results.add(node.toResult(executeParams));
            }
        }
        return switch (results.size()) {
            case 0 -> null;
            case 1 -> results.get(0);
            default -> results;
        };
    }

    public void addClojureRow (final RowMap rowMap) {
        final IFn reducer = executeParams.reducer();
        current.acc = reducer.invoke(current.acc, rowMap);
    }

    private void addNode() {
        current = new Node();
        nodes.add(current);
    }

    public void maybeThrowError() {
        if (errorResponse == null) {
            if (exception != null) {
                throw new PGError(exception, "Unhandled exception: %s", exception.getMessage());
            }
        }
        else {
            if (exception != null) {
                throw new PGError(exception, "Unhandled exception: %s", exception.getMessage());
            }
            else {
                throw new PGErrorResponse(errorResponse, sql);
            }
        }
    }

    public static void main (final String[] args) {
        final String[] keys = new String[] {"aaa", "bbb", "bbb", "ccc", "bbb"};
        System.out.println(Arrays.toString(unifyKeys(keys)));
    }
}
