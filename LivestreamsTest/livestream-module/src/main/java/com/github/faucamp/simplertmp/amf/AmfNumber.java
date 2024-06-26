package com.github.faucamp.simplertmp.amf;

import com.github.faucamp.simplertmp.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * AMF0 Number data type
 *
 * @author francois
 */
public class AmfNumber implements AmfData {

    /**
     * Size of an AMF number, in bytes (including type bit)
     */
    public static final int SIZE = 9;
    private double value;

    public AmfNumber(double value) {
        this.value = value;
    }

    public AmfNumber() {
    }

    public static double readNumberFrom(InputStream in) throws IOException {
        // Skip data type byte
        in.read();
        return Util.readDouble(in);
    }

    public static void writeNumberTo(OutputStream out, double number) throws IOException {
        out.write(AmfType.NUMBER.getValue());
        Util.writeDouble(out, number);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(AmfType.NUMBER.getValue());
        Util.writeDouble(out, value);
    }

    @Override
    public void readFrom(InputStream in) throws IOException {
        // Skip data type byte (we assume it's already read)
        value = Util.readDouble(in);
    }

    @Override
    public int getSize() {
        return SIZE;
    }
}
