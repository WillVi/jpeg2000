package com.github.jpeg2000;

import java.io.*;
import javax.xml.stream.*;
import java.util.*;
import java.math.BigInteger;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This class is defined to create the box of JP2 file format.  A box has
 *  a length, a type, an optional extra length and its content.  The subclasses
 *  should explain the content information.
 */
public class Box {
    
    private static Map<Integer,Class<? extends Box>> boxClasses = new HashMap<Integer,Class<? extends Box>>();
    private final int type;
    private byte[] raw;

    public Box(int type) {
        this.type = type;
    }

    /**
     * Read the content from the specified IO, which should be truncated
     * to the correct length. The current position of the specified "in"
     * is at the start of the "box contents" (DBox field)
     */
    public void read(RandomAccessIO in) throws IOException {
        raw = new byte[in.length()];
        in.readFully(raw, 0, raw.length);
    }

    /**
     * Write the content of this box to the OutputStream. The content
     * itself is written, not the length
     */
    public void write(DataOutputStream out) throws IOException {
        out.write(raw);
    }

    public int getType() {
        return type;
    }

    public static int fromString(String s) {
        if (s.length() != 4) {
            throw new IllegalArgumentException();
        }
        return (s.charAt(0)<<24) | (s.charAt(1)<<16) | (s.charAt(2)<<8) | s.charAt(3);
    }

    public static String toString(int v) {
        StringBuilder sb = new StringBuilder(4);
        for (int i=24;i>=0;i-=8) {
            int c = (v>>i) & 0xFF;
            if (c >= 0x20 && c <= 0x7f) {
                sb.append((char)c);
            } else {
                String s = "0000000" + Integer.toHexString(v);
                return "0x" + s.substring(s.length() - 8);
            }
        }
        return sb.toString();
    }

    public int getLength() {
        return raw.length;
    }

    public static Box createBox(int type) {
        Class<? extends Box> cl = boxClasses.get(type);
        if (cl == null) {
            return new Box(type);
        } else {
            try {
                return (Box)cl.newInstance();
            } catch (InstantiationException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException)e.getCause();
                }
                // Shouldn't happen
                throw new Error("Failed during box creation", e);
            } catch (Exception e) {
                // Shouldn't happen
                throw new Error("Failed during box creation", e);
            }
        }
    }
    
    static {
        //children for the root
        boxClasses.put(Integer.valueOf(fromString("ftyp")), FileTypeBox.class);
        boxClasses.put(Integer.valueOf(fromString("jp2h")), HeaderBox.class);
          // boxClasses.put(Integer.valueOf(0x69686472), LabelBox.class);       // L.9.13
          boxClasses.put(Integer.valueOf(fromString("ihdr")), ImageHeaderBox.class);
          boxClasses.put(Integer.valueOf(fromString("bpcc")), BitsPerComponentBox.class);
          boxClasses.put(Integer.valueOf(fromString("colr")), ColorSpecificationBox.class);
          boxClasses.put(Integer.valueOf(fromString("pclr")), PaletteBox.class);
          boxClasses.put(Integer.valueOf(fromString("cmap")), ComponentMappingBox.class);
          boxClasses.put(Integer.valueOf(fromString("cdef")), ChannelDefinitionBox.class);
          boxClasses.put(Integer.valueOf(fromString("res ")), ResolutionBox.class);
            boxClasses.put(Integer.valueOf(fromString("resc")), RescBox.class);
            boxClasses.put(Integer.valueOf(fromString("resd")), ResdBox.class);
        boxClasses.put(Integer.valueOf(fromString("jp2c")), CodeStreamBox.class);
        // boxClasses.put(Integer.valueOf(0x6A703269), IntellectualPropertyBox.class);
        boxClasses.put(Integer.valueOf(fromString("xml ")), XMLBox.class);      // L.9.18
        boxClasses.put(Integer.valueOf(fromString("uuid")), UUIDBox.class);
        boxClasses.put(Integer.valueOf(fromString("uinf")), UUIDInfoBox.class);
          boxClasses.put(Integer.valueOf(fromString("ulst")), UUIDListBox.class);
          boxClasses.put(Integer.valueOf(fromString("url ")), URLBox.class);

        // Children of JPEG2000UUIDInfoBox
    }

    public static class RescBox extends ResolutionBox {
        RescBox() {
            super(fromString("resc"));
        }
    }
    public static class ResdBox extends ResolutionBox {
        ResdBox() {
            super(fromString("resd"));
        }
    }

    public void write(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(toString(getType()).trim());
        out.writeAttribute("length", Integer.toString(getLength()));
        if (raw != null) {
            String s = new BigInteger(1, raw).toString();
            if ((s.length() & 1) == 1) {
                out.writeCharacters("0");
            }
            out.writeCharacters(s);
        }
        out.writeEndElement();
    }

}
