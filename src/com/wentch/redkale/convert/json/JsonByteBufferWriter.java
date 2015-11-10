/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wentch.redkale.convert.json;

import com.wentch.redkale.util.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

/**
 *
 * @author zhangjx
 */
public final class JsonByteBufferWriter extends JsonWriter {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final Charset charset;

    private final Supplier<ByteBuffer> supplier;

    private ByteBuffer[] buffers;

    private int index;

    public JsonByteBufferWriter(Supplier<ByteBuffer> supplier) {
        this(null, supplier);
    }

    public JsonByteBufferWriter(Charset charset, Supplier<ByteBuffer> supplier) {
        this.charset = UTF8.equals(charset) ? null : charset;
        this.supplier = supplier;
    }

    @Override
    protected boolean recycle() {
        this.index = 0;
        this.buffers = null;
        return true;
    }

    public ByteBuffer[] toBuffers() {
        if (buffers == null) return new ByteBuffer[0];
        for (int i = index; i < this.buffers.length; i++) {
            this.buffers[i].flip();
        }
        return this.buffers;
    }

    @Override
    public char[] toArray() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] toUTF8Bytes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int count() {
        if (this.buffers == null) return 0;
        int len = 0;
        for (ByteBuffer buffer : buffers) {
            len += buffer.remaining();
        }
        return len;
    }

    private int expand(final int byteLength) {
        if (this.buffers == null) {
            this.index = 0;
            this.buffers = new ByteBuffer[]{supplier.get()};
        }
        ByteBuffer buffer = this.buffers[index];
        if (!buffer.hasRemaining()) {
            buffer.flip();
            buffer = supplier.get();
            ByteBuffer[] bufs = new ByteBuffer[this.buffers.length + 1];
            System.arraycopy(this.buffers, 0, bufs, 0, this.buffers.length);
            bufs[this.buffers.length] = buffer;
            this.buffers = bufs;
            this.index++;
        }
        int len = buffer.remaining();
        int size = 0;
        while (len < byteLength) {
            buffer = supplier.get();
            ByteBuffer[] bufs = new ByteBuffer[this.buffers.length + 1];
            System.arraycopy(this.buffers, 0, bufs, 0, this.buffers.length);
            bufs[this.buffers.length] = buffer;
            this.buffers = bufs;
            len += buffer.remaining();
            size++;
        }
        return size;
    }

    @Override
    public void writeTo(final char ch) {
        if (ch > Byte.MAX_VALUE) throw new RuntimeException("writeTo char(int.value = " + (int) ch + ") must be less 127");
        expand(1);
        this.buffers[index].put((byte) ch);
    }

    @Override
    public void writeTo(final char[] chs, final int start, final int len) {
        writeTo(-1, false, chs, start, len);
    }

    private void writeTo(int expandsize, final boolean quote, final char[] chs, final int start, final int len) {
        int byteLength = quote ? 2 : 0;
        ByteBuffer bb = null;
        if (charset == null) {
            byteLength += encodeUTF8Length(chs, start, len);
        } else {
            bb = charset.encode(CharBuffer.wrap(chs, start, len));
            byteLength += bb.remaining();
        }
        if (expandsize < 0) expandsize = expand(byteLength);
        if (expandsize == 0) { // 只需要一个buffer 
            final ByteBuffer buffer = this.buffers[index];
            if (quote) buffer.put((byte) '"');

            if (charset == null) { //UTF-8
                final int limit = start + len;
                for (int i = start; i < limit; i++) {
                    char c = chs[i];
                    if (c < 0x80) {
                        buffer.put((byte) c);
                    } else if (c < 0x800) {
                        buffer.put((byte) (0xc0 | (c >> 6)));
                        buffer.put((byte) (0x80 | (c & 0x3f)));
                    } else {
                        buffer.put((byte) (0xe0 | ((c >> 12))));
                        buffer.put((byte) (0x80 | ((c >> 6) & 0x3f)));
                        buffer.put((byte) (0x80 | (c & 0x3f)));
                    }
                }
            } else {
                buffer.put(bb);
            }

            if (quote) buffer.put((byte) '"');
            return;
        }
        ByteBuffer buffer = this.buffers[index];
        if (quote) {
            if (!buffer.hasRemaining()) buffer = nextByteBuffer();
            buffer.put((byte) '"');
        }
        if (charset == null) { //UTF-8
            final int limit = start + len;
            for (int i = start; i < limit; i++) {
                buffer = putChar(buffer, chs[i]);
            }
        } else {
            while (bb.hasRemaining()) {
                if (!buffer.hasRemaining()) buffer = nextByteBuffer();
                buffer.put(bb.get());
            }
        }
        if (quote) {
            if (!buffer.hasRemaining()) buffer = nextByteBuffer();
            buffer.put((byte) '"');
        }
    }

    private ByteBuffer putChar(ByteBuffer buffer, char c) {
        if (c < 0x80) {
            if (!buffer.hasRemaining()) buffer = nextByteBuffer();
            buffer.put((byte) c);
        } else if (c < 0x800) {
            if (!buffer.hasRemaining()) buffer = nextByteBuffer();
            buffer.put((byte) (0xc0 | (c >> 6)));
            if (!buffer.hasRemaining()) buffer = nextByteBuffer();
            buffer.put((byte) (0x80 | (c & 0x3f)));
        } else {
            if (!buffer.hasRemaining()) buffer = nextByteBuffer();
            buffer.put((byte) (0xe0 | ((c >> 12))));
            if (!buffer.hasRemaining()) buffer = nextByteBuffer();
            buffer.put((byte) (0x80 | ((c >> 6) & 0x3f)));
            if (!buffer.hasRemaining()) buffer = nextByteBuffer();
            buffer.put((byte) (0x80 | (c & 0x3f)));
        }
        return buffer;
    }

    private ByteBuffer nextByteBuffer() {
        this.buffers[this.index].flip();
        return this.buffers[++this.index];
    }

    private static int encodeUTF8Length(final char[] text, final int start, final int len) {
        char c;
        int size = 0;
        final char[] chars = text;
        final int limit = start + len;
        for (int i = start; i < limit; i++) {
            c = chars[i];
            size += (c < 0x80 ? 1 : (c < 0x800 ? 2 : 3));
        }
        return size;
    }

    private static int encodeEscapeUTF8Length(final char[] text, final int start, final int len) {
        char c;
        int size = 0;
        final char[] chars = text;
        final int limit = start + len;
        for (int i = start; i < limit; i++) {
            c = chars[i];
            switch (c) {
                case '\n': size += 2;
                    break;
                case '\r': size += 2;
                    break;
                case '\t': size += 2;
                    break;
                case '\\': size += 2;
                    break;
                case '"': size += 2;
                    break;
                default:
                    size += (c < 0x80 ? 1 : (c < 0x800 ? 2 : 3));
                    break;
            }
        }
        return size;
    }

    /**
     * <b>注意：</b> 该String值不能为null且不会进行转义， 只用于不含需要转义字符的字符串，例如enum、double、BigInteger转换的String
     *
     * @param quote
     * @param value
     */
    @Override
    public void writeTo(final boolean quote, final String value) {
        char[] chs = Utility.charArray(value);
        writeTo(-1, quote, chs, 0, chs.length);
    }

    @Override
    public void writeString(String value) {
        if (value == null) {
            writeNull();
            return;
        }
        final char[] chs = Utility.charArray(value);
        int len = 0;
        for (char ch : chs) {
            switch (ch) {
                case '\n': len += 2;
                    break;
                case '\r': len += 2;
                    break;
                case '\t': len += 2;
                    break;
                case '\\': len += 2;
                    break;
                case '"': len += 2;
                    break;
                default: len++;
                    break;
            }
        }
        if (len == chs.length) {
            writeTo(-1, true, chs, 0, len);
            return;
        }
        int expandsize = -1;
        if (this.charset == null) { //UTF-8
            final int byteLength = 2 + encodeEscapeUTF8Length(chs, 0, chs.length);
            expandsize = expand(byteLength);
            if (expandsize == 0) { // 只需要一个buffer 
                final ByteBuffer buffer = this.buffers[index];
                buffer.put((byte) '"');
                for (char c : chs) {
                    switch (c) {
                        case '\n': buffer.put((byte) '\\').put((byte) 'n');
                            break;
                        case '\r': buffer.put((byte) '\\').put((byte) 'r');
                            break;
                        case '\t': buffer.put((byte) '\\').put((byte) 't');
                            break;
                        case '\\': buffer.put((byte) '\\').put((byte) '\\');
                            break;
                        case '"': buffer.put((byte) '\\').put((byte) '"');
                            break;
                        default:
                            if (c < 0x80) {
                                buffer.put((byte) c);
                            } else if (c < 0x800) {
                                buffer.put((byte) (0xc0 | (c >> 6)));
                                buffer.put((byte) (0x80 | (c & 0x3f)));
                            } else {
                                buffer.put((byte) (0xe0 | ((c >> 12))));
                                buffer.put((byte) (0x80 | ((c >> 6) & 0x3f)));
                                buffer.put((byte) (0x80 | (c & 0x3f)));
                            }
                            break;
                    }
                }
                buffer.put((byte) '"');
                return;
            }
        }
        StringBuilder sb = new StringBuilder(len);
        for (char ch : chs) {
            switch (ch) {
                case '\n': sb.append("\\n");
                    break;
                case '\r': sb.append("\\r");
                    break;
                case '\t': sb.append("\\t");
                    break;
                case '\\': sb.append("\\\\");
                    break;
                case '"': sb.append("\\\"");
                    break;
                default: sb.append(ch);
                    break;
            }
        }
        char[] cs = Utility.charArray(sb);
        writeTo(expandsize, true, cs, 0, sb.length());
    }

    @Override
    public void writeField(boolean comma, Attribute attribute) {
        if (comma) writeTo(',');
        writeTo(true, attribute.field());
        writeTo(':');
    }

    @Override
    public void writeSmallString(String value) {
        writeTo(false, value);
    }

    @Override
    public String toString() {
        return Objects.toString(this);
    }
}
