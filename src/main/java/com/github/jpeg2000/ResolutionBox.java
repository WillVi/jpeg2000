package com.github.jpeg2000;

import java.io.*;
import jj2000.j2k.io.*;

/** This class is defined to represent a Resolution Box of JPEG JP2
 *  file format.  A Data Entry URL Box has a length, and a fixed type
 *  of "resc" (capture resolution) or "resd" (default display resolution).
 *
 * Its contens includes the resolution numerators, denominator, and the
 * exponents for both horizontal and vertical directions.
 */
public class ResolutionBox extends Box {

    private short numV;
    private short numH;
    private short denomV;
    private short denomH;
    private byte expV;
    private byte expH;

    /** The cached horizontal/vertical resolutions. */
    private float hRes;
    private float vRes;

    public ResolutionBox(int type) {
        super(type);
    }

    /** Constructs a <code>ResolutionBox</code> from the provided type and
     *  horizontal/vertical resolutions.
     */
    public ResolutionBox(int type, float hRes, float vRes) {
        this(type);
        this.hRes = hRes;
        this.vRes = vRes;
        denomH = denomV = 1;

        expV = 0;
        if (vRes >= 32768) {
            int temp = (int)vRes;
            while (temp >= 32768) {
                expV++;
                temp /= 10;
            }
            numV = (short)(temp & 0xFFFF);
        } else {
            numV = (short)vRes;
        }

        expH = 0;
        if (hRes >= 32768) {
            int temp = (int)hRes;
            while (temp >= 32768) {
                expH++;
                temp /= 10;
            }
            numH = (short)(temp & 0xFFFF);
        } else {
            numH = (short)hRes;
        }
    }

    /** Return the horizontal resolution. */
    public float getHorizontalResolution() {
        return hRes;
    }

    /** Return the vertical resolution. */
    public float getVerticalResolution() {
        return vRes;
    }

    @Override public int getLength() {
        return 10;
    }

    @Override public void read(RandomAccessIO in) throws IOException {
        numV = in.readShort();
        denomV = in.readShort();
        numH = in.readShort();
        denomH = in.readShort();
        expV = in.readByte();
        expH = in.readByte();
        vRes = (float)((numV & 0xFFFF) * Math.pow(10, expV) / (denomV & 0xFFFF));
        hRes = (float)((numH & 0xFFFF)* Math.pow(10, expH) / (denomH & 0xFFFF));
    }

    @Override public void write(DataOutputStream out) throws IOException {
        out.writeShort(numV);
        out.writeShort(denomV);
        out.writeShort(numH);
        out.writeShort(denomH);
        out.write(expV);
        out.write(expH);
    }

}
