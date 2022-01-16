/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.helper;

import java.util.HashMap;

/**
 * @author Daniel McCoy Stephenson
 */
public class ChunkFlags {
    private HashMap<Long, long[]> chunkmap = new HashMap<>();
    private long last_key = Long.MAX_VALUE;
    private long[] last_row;

    public ChunkFlags() {

    }


    public boolean getFlag(int x, int y) {
        long k = (((long)(x >> 6)) << 32) | (0xFFFFFFFFL & (long)(y >> 6));
        long[] row;
        if(k == last_key) {
            row = last_row;
        }
        else {
            row = chunkmap.get(k);
            last_key = k;
            last_row = row;
        }
        if(row == null)
            return false;
        else
            return (row[y & 0x3F] & (1L << (x & 0x3F))) != 0;
    }


    public void setFlag(int x, int y, boolean f) {
        long k = (((long)(x >> 6)) << 32) | (0xFFFFFFFFL & (long)(y >> 6));
        long[] row;
        if(k == last_key) {
            row = last_row;
        }
        else {
            row = chunkmap.get(k);
            last_key = k;
            last_row = row;
        }
        if(f) {
            if(row == null) {
                row = new long[64];
                chunkmap.put(k, row);
                last_row = row;
            }
            row[y & 0x3F] |= (1L << (x & 0x3F));
        }
        else {
            if(row != null)
                row[y & 0x3F] &= ~(1L << (x & 0x3F));
        }
    }


    public void clear() {
        chunkmap.clear();
        last_row = null;
        last_key = Long.MAX_VALUE;
    }
}