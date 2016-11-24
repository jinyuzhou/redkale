/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.util;

import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import javax.net.ssl.*;

/**
 *
 * <p>
 * 详情见: https://redkale.org
 *
 * @author zhangjx
 */
public final class Utility {

    private static final int zoneRawOffset = TimeZone.getDefault().getRawOffset();

    private static final String format = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final char hex[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static final sun.misc.Unsafe UNSAFE;

    private static final long strvaloffset;

    private static final long sbvaloffset;

    private static final javax.net.ssl.SSLContext DEFAULTSSL_CONTEXT;

    private static final javax.net.ssl.HostnameVerifier defaultVerifier = (s, ss) -> true;

    static {
        sun.misc.Unsafe usafe = null;
        long fd1 = 0L;
        long fd2 = 0L;
        try {
            Field f = String.class.getDeclaredField("value");
            if (f.getType() == char[].class) { //JDK9及以上不再是char[]
                Field safeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                safeField.setAccessible(true);
                usafe = (sun.misc.Unsafe) safeField.get(null);
                fd1 = usafe.objectFieldOffset(f);
                fd2 = usafe.objectFieldOffset(StringBuilder.class.getSuperclass().getDeclaredField("value"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e); //不可能会发生
        }
        UNSAFE = usafe;
        strvaloffset = fd1;
        sbvaloffset = fd2;

        try {
            DEFAULTSSL_CONTEXT = javax.net.ssl.SSLContext.getInstance("SSL");
            DEFAULTSSL_CONTEXT.init(null, new javax.net.ssl.TrustManager[]{new javax.net.ssl.X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws java.security.cert.CertificateException {
                }

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) throws java.security.cert.CertificateException {
                }
            }}, null);
        } catch (Exception e) {
            throw new RuntimeException(e); //不可能会发生
        }
    }

    private Utility() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String toString(String string, ByteBuffer buffer) {
        if (buffer == null || !buffer.hasRemaining()) return string;
        int pos = buffer.position();
        int limit = buffer.limit();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        buffer.position(pos);
        buffer.limit(limit);
        if (string == null) return new String(bytes, UTF_8);
        return string + new String(bytes, UTF_8);
    }

    public static void println(String string, ByteBuffer buffer) {
        if (buffer == null || !buffer.hasRemaining()) return;
        int pos = buffer.position();
        int limit = buffer.limit();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        buffer.position(pos);
        buffer.limit(limit);
        println(string, bytes);
    }

    public static void println(String string, byte... bytes) {
        if (bytes == null) return;
        StringBuilder sb = new StringBuilder();
        if (string != null) sb.append(string);
        sb.append(bytes.length).append(".[");
        boolean last = false;
        for (byte b : bytes) {
            if (last) sb.append(',');
            int v = b & 0xff;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v));
            last = true;
        }
        sb.append(']');
        (System.out).println(sb);
    }

    /**
     * 返回本机的第一个内网IPv4地址， 没有则返回null
     *
     * @return IPv4地址
     */
    public static InetAddress localInetAddress() {
        InetAddress back = null;
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                if (!nif.isUp()) continue;
                Enumeration<InetAddress> eis = nif.getInetAddresses();
                while (eis.hasMoreElements()) {
                    InetAddress ia = eis.nextElement();
                    if (ia.isLoopbackAddress()) back = ia;
                    if (ia.isSiteLocalAddress()) return ia;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return back;
    }

    public static String now() {
        return String.format(format, System.currentTimeMillis());
    }

    public static String formatTime(long time) {
        return String.format(format, time);
    }

    public static String format36time(long time) {
        String time36 = Long.toString(time, 36);
        return time36.length() < 9 ? ("0" + time36) : time36;
    }

    /**
     * 获取当天凌晨零点的格林时间
     *
     * @return 毫秒数
     */
    public static long midnight() {
        return midnight(System.currentTimeMillis());
    }

    /**
     * 获取指定时间当天凌晨零点的格林时间
     *
     * @param time 指定时间
     *
     * @return 毫秒数
     */
    public static long midnight(long time) {
        return (time + zoneRawOffset) / 86400000 * 86400000 - zoneRawOffset;
    }

    /**
     * 获取当天20151231格式的int值
     *
     * @return 20151231格式的int值
     */
    public static int today() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return today.getYear() * 10000 + today.getMonthValue() * 100 + today.getDayOfMonth();
    }

    /**
     * 获取指定时间的20160202格式的int值
     *
     * @param time 指定时间
     *
     * @return 毫秒数
     */
    public static int yyyyMMdd(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取时间点所在星期的周一
     *
     * @param time 指定时间
     *
     * @return 毫秒数
     */
    public static long monday(long time) {
        ZoneId zid = ZoneId.systemDefault();
        Instant instant = Instant.ofEpochMilli(time);
        LocalDate ld = instant.atZone(zid).toLocalDate();
        ld = ld.minusDays(ld.getDayOfWeek().getValue() - 1);
        return ld.atStartOfDay(zid).toInstant().toEpochMilli();
    }

    /**
     * 获取时间点所在星期的周日
     *
     * @param time 指定时间
     *
     * @return 毫秒数
     */
    public static long sunday(long time) {
        ZoneId zid = ZoneId.systemDefault();
        Instant instant = Instant.ofEpochMilli(time);
        LocalDate ld = instant.atZone(zid).toLocalDate();
        ld = ld.plusDays(7 - ld.getDayOfWeek().getValue());
        return ld.atStartOfDay(zid).toInstant().toEpochMilli();
    }

    /**
     * 获取时间点所在月份的1号
     *
     * @param time 指定时间
     *
     * @return 毫秒数
     */
    public static long monthFirstDay(long time) {
        ZoneId zid = ZoneId.systemDefault();
        Instant instant = Instant.ofEpochMilli(time);
        LocalDate ld = instant.atZone(zid).toLocalDate().withDayOfMonth(1);
        return ld.atStartOfDay(zid).toInstant().toEpochMilli();
    }

    public static String binToHexString(byte[] bytes) {
        return new String(binToHex(bytes));
    }

    public static char[] binToHex(byte[] bytes) {
        return binToHex(bytes, 0, bytes.length);
    }

    public static String binToHexString(byte[] bytes, int offset, int len) {
        return new String(binToHex(bytes, offset, len));
    }

    public static char[] binToHex(byte[] bytes, int offset, int len) {
        final char[] sb = new char[len * 2];
        final int end = offset + len;
        int index = 0;
        final char[] hexs = hex;
        for (int i = offset; i < end; i++) {
            byte b = bytes[i];
            sb[index++] = (hexs[((b >> 4) & 0xF)]);
            sb[index++] = hexs[((b) & 0xF)];
        }
        return sb;
    }

    public static byte[] hexToBin(CharSequence src) {
        return hexToBin(src, 0, src.length());
    }

    public static byte[] hexToBin(CharSequence src, int offset, int len) {
        final int size = (len + 1) / 2;
        final byte[] bytes = new byte[size];
        final int end = offset + len;
        String digits = "0123456789abcdef";
        for (int i = 0; i < size; i++) {
            int ch1 = src.charAt(offset + i * 2);
            if ('A' <= ch1 && 'F' >= ch1) ch1 = ch1 - 'A' + 'a';
            int ch2 = src.charAt(offset + i * 2 + 1);
            if ('A' <= ch2 && 'F' >= ch2) ch2 = ch2 - 'A' + 'a';
            int pos1 = digits.indexOf(ch1);
            if (pos1 < 0) throw new NumberFormatException();
            int pos2 = digits.indexOf(ch2);
            if (pos2 < 0) throw new NumberFormatException();
            bytes[i] = (byte) (pos1 * 0x10 + pos2);
        }
        return bytes;
    }

    public static byte[] hexToBin(String str) {
        return hexToBin(charArray(str));
    }

    public static byte[] hexToBin(char[] src) {
        return hexToBin(src, 0, src.length);
    }

    public static byte[] hexToBin(char[] src, int offset, int len) {
        final int size = (len + 1) / 2;
        final byte[] bytes = new byte[size];
        final int end = offset + len;
        String digits = "0123456789abcdef";
        for (int i = 0; i < size; i++) {
            int ch1 = src[offset + i * 2];
            if ('A' <= ch1 && 'F' >= ch1) ch1 = ch1 - 'A' + 'a';
            int ch2 = src[offset + i * 2 + 1];
            if ('A' <= ch2 && 'F' >= ch2) ch2 = ch2 - 'A' + 'a';
            int pos1 = digits.indexOf(ch1);
            if (pos1 < 0) throw new NumberFormatException();
            int pos2 = digits.indexOf(ch2);
            if (pos2 < 0) throw new NumberFormatException();
            bytes[i] = (byte) (pos1 * 0x10 + pos2);
        }
        return bytes;
    }

    //-----------------------------------------------------------------------------
    public static char[] decodeUTF8(final byte[] array) {
        return decodeUTF8(array, 0, array.length);
    }

    public static char[] decodeUTF8(final byte[] array, final int start, final int len) {
        byte b;
        int size = len;
        final byte[] bytes = array;
        final int limit = start + len;
        for (int i = start; i < limit; i++) {
            b = bytes[i];
            if ((b >> 5) == -2) {
                size--;
            } else if ((b >> 4) == -2) {
                size -= 2;
            }
        }
        final char[] text = new char[size];
        size = 0;
        for (int i = start; i < limit;) {
            b = bytes[i++];
            if (b >= 0) {
                text[size++] = (char) b;
            } else if ((b >> 5) == -2) {
                text[size++] = (char) (((b << 6) ^ bytes[i++]) ^ (((byte) 0xC0 << 6) ^ ((byte) 0x80)));
            } else if ((b >> 4) == -2) {
                text[size++] = (char) ((b << 12) ^ (bytes[i++] << 6) ^ (bytes[i++] ^ (((byte) 0xE0 << 12) ^ ((byte) 0x80 << 6) ^ ((byte) 0x80))));
            }
        }
        return text;
    }

    public static byte[] encodeUTF8(final String value) {
        if (value == null) return new byte[0];
        if (UNSAFE == null) return encodeUTF8(value.toCharArray());
        return encodeUTF8((char[]) UNSAFE.getObject(value, strvaloffset));
    }

    public static byte[] encodeUTF8(final char[] array) {
        return encodeUTF8(array, 0, array.length);
    }

    public static byte[] encodeUTF8(final char[] text, final int start, final int len) {
        char c;
        int size = 0;
        final char[] chars = text;
        final int limit = start + len;
        for (int i = start; i < limit; i++) {
            c = chars[i];
            if (c < 0x80) {
                size++;
            } else if (c < 0x800) {
                size += 2;
            } else {
                size += 3;
            }
        }
        final byte[] bytes = new byte[size];
        size = 0;
        for (int i = start; i < limit; i++) {
            c = chars[i];
            if (c < 0x80) {
                bytes[size++] = (byte) c;
            } else if (c < 0x800) {
                bytes[size++] = (byte) (0xc0 | (c >> 6));
                bytes[size++] = (byte) (0x80 | (c & 0x3f));
            } else {
                bytes[size++] = (byte) (0xe0 | ((c >> 12)));
                bytes[size++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                bytes[size++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        return bytes;
    }

    public static char[] charArray(String value) {
        if (value == null) return null;
        if (UNSAFE == null) return value.toCharArray();
        return (char[]) UNSAFE.getObject(value, strvaloffset);
    }

    public static char[] charArray(StringBuilder value) {
        if (value == null) return null;
        if (UNSAFE == null) return value.toString().toCharArray();
        return (char[]) UNSAFE.getObject(value, sbvaloffset);
    }

    public static ByteBuffer encodeUTF8(final ByteBuffer buffer, final char[] array) {
        return encodeUTF8(buffer, array, 0, array.length);
    }

    public static ByteBuffer encodeUTF8(final ByteBuffer buffer, int bytesLength, final char[] array) {
        return encodeUTF8(buffer, bytesLength, array, 0, array.length);
    }

    public static int encodeUTF8Length(String value) {
        if (value == null) return -1;
        if (UNSAFE == null) return encodeUTF8Length(value.toCharArray());
        return encodeUTF8Length((char[]) UNSAFE.getObject(value, strvaloffset));
    }

    public static int encodeUTF8Length(final char[] text) {
        return encodeUTF8Length(text, 0, text.length);
    }

    public static int encodeUTF8Length(final char[] text, final int start, final int len) {
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

    /**
     * 将两个数字组装成一个long
     *
     * @param high 高位值
     * @param low  低位值
     *
     * @return long值
     */
    public static long merge(int high, int low) {
        return (0L + high) << 32 | low;
    }

    public static ByteBuffer encodeUTF8(final ByteBuffer buffer, final char[] text, final int start, final int len) {
        return encodeUTF8(buffer, encodeUTF8Length(text, start, len), text, start, len);
    }

    public static ByteBuffer encodeUTF8(final ByteBuffer buffer, int bytesLength, final char[] text, final int start, final int len) {
        char c;
        char[] chars = text;
        final int limit = start + len;
        int remain = buffer.remaining();
        final ByteBuffer buffer2 = remain >= bytesLength ? null : ByteBuffer.allocate(bytesLength - remain + 3); //最差情况buffer最后两byte没有填充
        ByteBuffer buf = buffer;
        for (int i = start; i < limit; i++) {
            c = chars[i];
            if (c < 0x80) {
                if (buf.remaining() < 1) buf = buffer2;
                buf.put((byte) c);
            } else if (c < 0x800) {
                if (buf.remaining() < 2) buf = buffer2;
                buf.put((byte) (0xc0 | (c >> 6)));
                buf.put((byte) (0x80 | (c & 0x3f)));
            } else {
                if (buf.remaining() < 3) buf = buffer2;
                buf.put((byte) (0xe0 | ((c >> 12))));
                buf.put((byte) (0x80 | ((c >> 6) & 0x3f)));
                buf.put((byte) (0x80 | (c & 0x3f)));
            }
        }
        if (buffer2 != null) buffer2.flip();
        return buffer2;
    }

    //-----------------------------------------------------------------------------
    public static javax.net.ssl.SSLContext getDefaultSSLContext() {
        return DEFAULTSSL_CONTEXT;
    }

    public static Socket createDefaultSSLSocket(InetSocketAddress address) throws IOException {
        return createDefaultSSLSocket(address.getAddress(), address.getPort());
    }

    public static Socket createDefaultSSLSocket(InetAddress host, int port) throws IOException {
        Socket socket = DEFAULTSSL_CONTEXT.getSocketFactory().createSocket(host, port);

        return socket;
    }

    public static String postHttpContent(String url) throws IOException {
        return remoteHttpContent(null, "POST", url, null, null).toString("UTF-8");
    }

    public static String postHttpContent(String url, String body) throws IOException {
        return remoteHttpContent(null, "POST", url, null, body).toString("UTF-8");
    }

    public static String postHttpContent(String url, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(null, "POST", url, headers, body).toString("UTF-8");
    }

    public static String postHttpContent(SSLContext ctx, String url) throws IOException {
        return remoteHttpContent(ctx, "POST", url, null, null).toString("UTF-8");
    }

    public static String postHttpContent(SSLContext ctx, String url, String body) throws IOException {
        return remoteHttpContent(ctx, "POST", url, null, body).toString("UTF-8");
    }

    public static String postHttpContent(SSLContext ctx, String url, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(ctx, "POST", url, headers, body).toString("UTF-8");
    }

    public static String postHttpContent(String url, Charset charset) throws IOException {
        return remoteHttpContent(null, "POST", url, null, null).toString(charset.name());
    }

    public static String postHttpContent(String url, Charset charset, String body) throws IOException {
        return remoteHttpContent(null, "POST", url, null, body).toString(charset.name());
    }

    public static String postHttpContent(String url, Charset charset, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(null, "POST", url, headers, body).toString(charset.name());
    }

    public static String postHttpContent(SSLContext ctx, String url, Charset charset) throws IOException {
        return remoteHttpContent(ctx, "POST", url, null, null).toString(charset.name());
    }

    public static String postHttpContent(SSLContext ctx, String url, Charset charset, String body) throws IOException {
        return remoteHttpContent(ctx, "POST", url, null, body).toString(charset.name());
    }

    public static String postHttpContent(SSLContext ctx, String url, Charset charset, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(ctx, "POST", url, headers, body).toString(charset.name());
    }

    public static byte[] postHttpBytesContent(String url) throws IOException {
        return remoteHttpContent(null, "POST", url, null, null).toByteArray();
    }

    public static byte[] postHttpBytesContent(SSLContext ctx, String url) throws IOException {
        return remoteHttpContent(ctx, "POST", url, null, null).toByteArray();
    }

    public static byte[] postHttpBytesContent(String url, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(null, "POST", url, headers, body).toByteArray();
    }

    public static byte[] postHttpBytesContent(SSLContext ctx, String url, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(ctx, "POST", url, headers, body).toByteArray();
    }

    public static String getHttpContent(String url) throws IOException {
        return remoteHttpContent(null, "GET", url, null, null).toString("UTF-8");
    }

    public static String getHttpContent(SSLContext ctx, String url) throws IOException {
        return remoteHttpContent(ctx, "GET", url, null, null).toString("UTF-8");
    }

    public static String getHttpContent(SSLContext ctx, String url, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(ctx, "GET", url, headers, body).toString("UTF-8");
    }

    public static String getHttpContent(String url, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(null, "GET", url, headers, body).toString("UTF-8");
    }

    public static String getHttpContent(String url, Charset charset) throws IOException {
        return remoteHttpContent(null, "GET", url, null, null).toString(charset.name());
    }

    public static String getHttpContent(SSLContext ctx, String url, Charset charset) throws IOException {
        return remoteHttpContent(ctx, "GET", url, null, null).toString(charset.name());
    }

    public static String getHttpContent(SSLContext ctx, String url, Charset charset, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(ctx, "GET", url, headers, body).toString(charset.name());
    }

    public static String getHttpContent(String url, Charset charset, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(null, "GET", url, headers, body).toString(charset.name());
    }

    public static byte[] getHttpBytesContent(String url) throws IOException {
        return remoteHttpContent(null, "GET", url, null, null).toByteArray();
    }

    public static byte[] getHttpBytesContent(SSLContext ctx, String url) throws IOException {
        return remoteHttpContent(ctx, "GET", url, null, null).toByteArray();
    }

    public static byte[] getHttpBytesContent(String url, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(null, "GET", url, headers, body).toByteArray();
    }

    public static byte[] getHttpBytesContent(SSLContext ctx, String url, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(ctx, "GET", url, headers, body).toByteArray();
    }

    public static ByteArrayOutputStream remoteHttpContent(String method, String url, Map<String, String> headers, String body) throws IOException {
        return remoteHttpContent(null, method, url, headers, body);
    }

    public static ByteArrayOutputStream remoteHttpContent(SSLContext ctx, String method, String url, Map<String, String> headers, String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        if (conn instanceof HttpsURLConnection) {
            HttpsURLConnection httpsconn = ((HttpsURLConnection) conn);
            httpsconn.setSSLSocketFactory((ctx == null ? DEFAULTSSL_CONTEXT : ctx).getSocketFactory());
            httpsconn.setHostnameVerifier(defaultVerifier);
        }
        conn.setRequestMethod(method);
        if (headers != null) {
            for (Map.Entry<String, String> en : headers.entrySet()) { //不用forEach是为了兼容JDK 6
                conn.setRequestProperty(en.getKey(), en.getValue());
            }
        }
        if (body != null) {
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.getOutputStream().write(body.getBytes(UTF_8));
        }
        conn.connect();
        int rs = conn.getResponseCode();
        if (rs == 301 || rs == 302) {
            String newurl = conn.getHeaderField("Location");
            conn.disconnect();
            return remoteHttpContent(ctx, method, newurl, headers, body);
        }
        InputStream in = (rs < 400 || rs == 404) && rs != 405 ? conn.getInputStream() : conn.getErrorStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] bytes = new byte[1024];
        int pos;
        while ((pos = in.read(bytes)) != -1) {
            out.write(bytes, 0, pos);
        }
        conn.disconnect();
        return out;
    }

    public static String read(InputStream in) throws IOException {
        return read(in, "UTF-8");
    }

    public static String read(InputStream in, String charsetName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] bytes = new byte[1024];
        int pos;
        while ((pos = in.read(bytes)) != -1) {
            out.write(bytes, 0, pos);
        }
        return charsetName == null ? out.toString() : out.toString(charsetName);
    }

    public static ByteArrayOutputStream readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] bytes = new byte[1024];
        int pos;
        while ((pos = in.read(bytes)) != -1) {
            out.write(bytes, 0, pos);
        }
        return out;
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        return readStream(in).toByteArray();
    }

    public static ByteArrayOutputStream readStreamThenClose(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] bytes = new byte[1024];
        int pos;
        while ((pos = in.read(bytes)) != -1) {
            out.write(bytes, 0, pos);
        }
        in.close();
        return out;
    }

    public static byte[] readBytesThenClose(InputStream in) throws IOException {
        return readStreamThenClose(in).toByteArray();
    }
}
