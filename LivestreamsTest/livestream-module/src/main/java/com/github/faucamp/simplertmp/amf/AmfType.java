package com.github.faucamp.simplertmp.amf;

import java.util.HashMap;
import java.util.Map;

/**
 * AMF0 data type enum
 *
 * @author francois
 */
public enum AmfType {

    /**
     * Number (encoded as IEEE 64-bit double precision floating point number)
     */
    NUMBER(0x00), /**
     * Boolean (Encoded as a single byte of value 0x00 or 0x01)
     */
    BOOLEAN(0x01), /**
     * String (ASCII encoded)
     */
    STRING(0x02), /**
     * Object - set of key/value pairs
     */
    OBJECT(0x03), NULL(0x05), UNDEFINED(0x06), ECMA_MAP(0x08), STRICT_ARRAY(0x0A);

    private static final Map<Byte, AmfType> quickLookupMap = new HashMap<>();

    static {
        for (AmfType amfType : AmfType.values()) {
            quickLookupMap.put(amfType.getValue(), amfType);
        }
    }

    private byte value;

    private AmfType(int intValue) {
        this.value = (byte) intValue;
    }

    public static AmfType valueOf(byte amfTypeByte) {
        return quickLookupMap.get(amfTypeByte);
    }

    public byte getValue() {
        return value;
    }
}
