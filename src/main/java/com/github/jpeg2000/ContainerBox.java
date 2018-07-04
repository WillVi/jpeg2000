package com.github.jpeg2000;

import java.io.*;
import java.util.*;
import jj2000.j2k.io.RandomAccessIO;
import jj2000.j2k.io.SubRandomAccessIO;
import javax.xml.stream.*;

/**
 * This class is defined to create the box of JP2 file format.  A box has
 *  a length, a type, an optional extra length and its content.  The subclasses
 *  should explain the content information.
 */
public class ContainerBox extends Box {
    
    private List<Box> boxes;

    public ContainerBox(int type) {
        super(type);
        boxes = new ArrayList<Box>();
    }

    @Override public int getLength() {
        int len = 0;
        for (int i=0;i<boxes.size();i++) {
            int sublen = boxes.get(i).getLength();
            if (sublen == 0) {
                return 0;
            }
            len += 8 + sublen;
        }
        return len;
    }

    /**
     * Read the content from the specified IO, which should be truncated
     * to the correct length. The current position of the specified "in"
     * is at the start of the "box contents" (DBox field)
     */
    @Override public void read(RandomAccessIO in) throws IOException {
        while (in.length() - in.getPos() > 0) {
            add(readBox(in));
        }
    }

    public static Box readBox(RandomAccessIO in) throws IOException {
        int start = in.getPos();
        int len = in.readInt();
        int type = in.readInt();
        Box box = Box.createBox(type);
        RandomAccessIO sub;
        if (len == 0) {
            sub = new SubRandomAccessIO(in, in.length() - in.getPos());
        } else if (len == 1) {
            throw new IOException("Long boxes not supported");
        } else if (len < 8) {
            throw new IOException("Invalid box length "+len);
        } else {
            sub = new SubRandomAccessIO(in, len - 8);
        }
//        System.out.println("Reading box at "+start+" "+toString(type)+" len="+len+" stream="+sub.getPos()+"/"+sub.length());
        box.read(sub);
//        System.out.println("Skip to "+start +"+"+ len+" = "+(start+len)+" from "+in.getPos()+"/"+in.length());
        if (len != 0 || start + len < in.length()) {
            in.seek(len == 0 ? in.length() : start + len);
        }
        return box;
    }

    public static void writeBox(Box box, DataOutputStream out) throws IOException {
        int len = box.getLength();
        out.writeInt(len == 0 ? 0 : len + 8);
        out.writeInt(box.getType());
        box.write(out);
    }

    @Override public void write(DataOutputStream out) throws IOException {
        for (int i=0;i<boxes.size();i++) {
            writeBox(boxes.get(i), out);
        }
    }

    public ContainerBox add(Box box) {
        boxes.add(box);
        return this;
    }

    public List<Box> getBoxes() {
        return Collections.<Box>unmodifiableList(boxes);
    }

    @Override public void write(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(toString(getType()).trim());
        out.writeAttribute("length", Integer.toString(getLength()));
        for (Box box : boxes) {
            box.write(out);
        }
        out.writeEndElement();
    }

}
