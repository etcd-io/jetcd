package com.coreos.jetcd.data;

import com.google.protobuf.ByteString;

import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Etcd binary bytes, easy to convert between byte[], String and ByteString.
 */
public class ByteSequence {

    private final int hashVal;
    private final ByteString byteString;


    public ByteSequence(byte[] source) {
        hashVal = calcHashCore(source);
        byteString = toByteString(source);
    }

    protected ByteSequence(ByteString byteString) {
        this(byteString.toByteArray());
    }

    public ByteSequence(String string) {
        this(string.getBytes());
    }

    public ByteSequence(CharBuffer charBuffer) {
        this(String.valueOf(charBuffer.array()));
    }

    public ByteSequence(CharSequence charSequence) {
        this(java.nio.CharBuffer.wrap(charSequence));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ByteSequence) {
            ByteSequence other = (ByteSequence) obj;
            if (other.hashCode() != hashCode()) return false;
            return byteString.equals(other.byteString);
        } else {
            return false;
        }
    }

    protected ByteString getByteString() {
        return this.byteString;
    }

    private ByteString toByteString(byte[] bytes) {
        return ByteString.copyFrom(bytes);
    }

    private int calcHashCore(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; ++i) {
            result = 31 * result + bytes[i];
        }
        return result;
    }

    @Override
    public int hashCode() {
        return hashVal;
    }

    public String toStringUtf8() {
        return byteString.toStringUtf8();
    }

    public String toString(Charset charset) {
        return byteString.toString(charset);
    }

    public String toString(String charsetName) throws UnsupportedEncodingException {
        return byteString.toString(charsetName);
    }

    public byte[] getBytes() {
        return byteString.toByteArray();
    }

    public static ByteSequence fromString(String string) {
        return new ByteSequence(string);
    }

    public static ByteSequence fromCharSequence(CharSequence charSequence) {
        return new ByteSequence(charSequence);
    }

    public static ByteSequence fromCharBuffer(CharBuffer charBuffer) {
        return new ByteSequence(charBuffer);
    }

    protected static ByteSequence fromByteString(ByteString byteString) {
        return new ByteSequence(byteString);
    }

    public static ByteSequence fromBytes(byte[] bytes) {
        return new ByteSequence(bytes);
    }

}
