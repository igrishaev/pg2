public Object foo (final String sql) {
        final ExecuteParams executeParams = ExecuteParams.INSTANCE;
        final PreparedStatement stmt = prepare(sql);
        final String portal = generatePortal();
        sendBind(portal, stmt, executeParams);
        sendDescribePortal(portal);
        sendExecute(portal, 3);
        // sendClosePortal(portal);
        // sendFlush();
        sendSync();
        flush();
        // return interact(executeParams, sql).getResult();
        // readMessage()

        System.out.println("reading 1");

        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        // System.out.println(readMessage(false));

        System.out.println("reading 2 DONE");

        sendExecute(portal, 3);
        // sendClosePortal(portal);
        sendFlush();
        sendSync();
        flush();
        // return interact(executeParams, sql).getResult();
        // readMessage()

        System.out.println("reading 2");

        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));
        System.out.println(readMessage(false));


        // System.out.println(readMessage(false));


        return null;


    }
