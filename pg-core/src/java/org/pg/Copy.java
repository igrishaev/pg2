package org.pg;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.processor.IProcessor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Copy {

    public static final byte[] COPY_BIN_HEADER = {
            // header
            (byte) 'P',
            (byte) 'G',
            (byte) 'C',
            (byte) 'O',
            (byte) 'P',
            (byte) 'Y',
            (byte) 10,
            (byte) 0xFF,
            (byte) 13,
            (byte) 10,
            (byte) 0,
            // 0 int32
            (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            // 0 int32
            (byte) 0, (byte) 0, (byte) 0, (byte) 0
    };

    public static final byte[] MSG_COPY_BIN_TERM = new byte[] {
            (byte) 'd',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 6,
            (byte) -1,
            (byte) -1
    };

    public static String quoteCSV (final String line) {
        return line.replace("\"", "\"\"");
    }

    public static String encodeRowCSV (
            final List<Object> row,
            final ExecuteParams executeParams,
            final CodecParams codecParams
    ) {
        final StringBuilder sb = new StringBuilder();
        final Iterator<Object> iterator = row.iterator();
        final int[] OIDs = executeParams.OIDs();
        final int OIDLen = OIDs.length;
        short i = 0;
        int oid;
        String encoded;
        IProcessor processor;
        while (iterator.hasNext()) {
            final Object item = iterator.next();
            oid = i < OIDLen ? OIDs[i] : OID.defaultOID(item);
            if (item == null) {
                sb.append(executeParams.CSVNull());
            }
            else {
                processor = codecParams.getProcessor(oid);
                encoded = processor.encodeTxt(item, codecParams);
                sb.append(executeParams.CSVQuote());
                sb.append(quoteCSV(encoded));
                sb.append(executeParams.CSVQuote());
            }
            if (iterator.hasNext()) {
                sb.append(executeParams.CSVCellSep());
            }
            i++;
        }
        sb.append(executeParams.CSVLineSep());
        return sb.toString();
    }

    public static ByteBuffer encodeRowBin (
            final List<Object> row,
            final ExecuteParams executeParams,
            final CodecParams codecParams
    ) {
        final short count = (short) row.size();
        final ByteBuffer[] bufs = new ByteBuffer[count];
        final int[] OIDs = executeParams.OIDs();
        final int OIDLen = OIDs.length;
        int oid;
        IProcessor processor;
        int totalSize = 2;
        int i = 0;
        for (final Object item: row) {
            if (item == null) {
                totalSize += 4;
                bufs[i] = null;
            }
            else {
                oid = i < OIDLen ? OIDs[i] : OID.defaultOID(item);
                processor = codecParams.getProcessor(oid);
                final ByteBuffer buf = processor.encodeBin(item, codecParams);
                totalSize += 4 + buf.array().length;
                bufs[i] = buf;
            }
            i++;
        }

        final ByteBuffer result = ByteBuffer.allocate(totalSize);
        result.putShort(count);

        for (final ByteBuffer buf: bufs) {
            if (buf == null) {
                result.putInt(-1);
            }
            else {
                result.putInt(buf.limit());
                result.put(buf.rewind());
            }
        }
        return result;
    }

    public static void main(final String[] args) {
        System.out.println(encodeRowCSV(
                List.of(1, 2, 3),
                ExecuteParams.standard(),
                CodecParams.create())
        );

        final List<Object> row = new ArrayList<>();
        row.add(1);
        row.add("Ivan");
        row.add(true);
        row.add(null);

        final List<Integer> OIDs = List.of(OID.INT2, OID.DEFAULT, OID.BOOL);

        System.out.println(
                Arrays.toString(
                    encodeRowBin(
                            row,
                            ExecuteParams.builder().OIDs(OIDs).build(),
                            CodecParams.create()
                    ).array())
        );

        System.out.println(Arrays.toString(Copy.COPY_BIN_HEADER));

        final ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort((short)-1);
        System.out.println(Arrays.toString(bb.array()));
    }

}
