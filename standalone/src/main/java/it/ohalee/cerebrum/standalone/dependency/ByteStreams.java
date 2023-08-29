package it.ohalee.cerebrum.standalone.dependency;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

public final class ByteStreams {

    /**
     * There are three methods to implement
     * {@link FileChannel#transferTo(long, long, WritableByteChannel)}:
     *
     * <ol>
     * <li>Use sendfile(2) or equivalent. Requires that both the input channel and the output channel
     *     have their own file descriptors. Generally this only happens when both channels are files
     *     or sockets. This performs zero copies - the bytes never enter userspace.
     * <li>Use mmap(2) or equivalent. Requires that either the input channel or the output channel
     *     have file descriptors. Bytes are copied from the file into a kernel buffer, then directly
     *     into the other buffer (userspace). Note that if the file is very large, a naive
     *     implementation will effectively put the whole file in memory. On many systems with paging
     *     and virtual memory, this is not a problem - because it is mapped read-only, the kernel can
     *     always page it to disk "for free". However, on systems where killing processes happens all
     *     the time in normal conditions (i.e., android) the OS must make a tradeoff between paging
     *     memory and killing other processes - so allocating a gigantic buffer and then sequentially
     *     accessing it could result in other processes dying. This is solvable via madvise(2), but
     *     that obviously doesn't exist in java.
     * <li>Ordinary copy. Kernel copies bytes into a kernel buffer, from a kernel buffer into a
     *     userspace buffer (byte[] or ByteBuffer), then copies them from that buffer into the
     *     destination channel.
     * </ol>
     * <p>
     * This value is intended to be large enough to make the overhead of system calls negligible,
     * without being so large that it causes problems for systems with atypical memory management if
     * approaches 2 or 3 are used.
     */
    private static final int ZERO_COPY_CHUNK_SIZE = 512 * 1024;
    private static final OutputStream NULL_OUTPUT_STREAM =
            new OutputStream() {
                /** Discards the specified byte. */
                @Override
                public void write(int b) {
                }

                /** Discards the specified byte array. */
                @Override
                public void write(byte[] b) {

                }

                /** Discards the specified byte array. */
                @Override
                public void write(byte[] b, int off, int len) {

                }

                @Override
                public String toString() {
                    return "ByteStreams.nullOutputStream()";
                }
            };

    private ByteStreams() {
    }

    /**
     * Creates a new byte array for buffering reads or writes.
     */
    static byte[] createBuffer() {
        return new byte[8192];
    }

    public static long copy(InputStream from, OutputStream to) throws IOException {
        byte[] buf = createBuffer();
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    public static long copy(ReadableByteChannel from, WritableByteChannel to) throws IOException {
        if (from instanceof FileChannel sourceChannel) {
            long oldPosition = sourceChannel.position();
            long position = oldPosition;
            long copied;
            do {
                copied = sourceChannel.transferTo(position, ZERO_COPY_CHUNK_SIZE, to);
                position += copied;
                sourceChannel.position(position);
            } while (copied > 0 || position < sourceChannel.size());
            return position - oldPosition;
        }

        ByteBuffer buf = ByteBuffer.wrap(createBuffer());
        long total = 0;
        while (from.read(buf) != -1) {
            buf.flip();
            while (buf.hasRemaining()) {
                total += to.write(buf);
            }
            buf.clear();
        }
        return total;
    }

    /**
     * Reads all bytes from an input stream into a byte array. Does not close the stream.
     *
     * @param in the input stream to read from
     * @return a byte array containing all the bytes from the stream
     * @throws IOException if an I/O error occurs
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        // Presize the ByteArrayOutputStream since we know how large it will need
        // to be, unless that value is less than the default ByteArrayOutputStream
        // size (32).
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, in.available()));
        copy(in, out);
        return out.toByteArray();
    }

    /**
     * Reads all bytes from an input stream into a byte array. The given expected size is used to
     * create an initial byte array, but if the actual number of bytes read from the stream differs,
     * the correct result will be returned anyway.
     */
    static byte[] toByteArray(InputStream in, int expectedSize) throws IOException {
        byte[] bytes = new byte[expectedSize];
        int remaining = expectedSize;

        while (remaining > 0) {
            int off = expectedSize - remaining;
            int read = in.read(bytes, off, remaining);
            if (read == -1) {
                // end of stream before reading expectedSize bytes
                // just return the bytes read so far
                return Arrays.copyOf(bytes, off);
            }
            remaining -= read;
        }

        // bytes is now full
        int b = in.read();
        if (b == -1) {
            return bytes;
        }

        // the stream was longer, so read the rest normally
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(b); // write the byte we read when testing for end of stream
        copy(in, out);

        byte[] result = new byte[bytes.length + out.size()];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        out.writeTo(result, bytes.length);
        return result;
    }

    public static long exhaust(InputStream in) throws IOException {
        long total = 0;
        long read;
        byte[] buf = createBuffer();
        while ((read = in.read(buf)) != -1) {
            total += read;
        }
        return total;
    }

    /**
     * Returns an {@link OutputStream} that simply discards written bytes.
     *
     * @since 14.0 (since 1.0 as com.google.common.io.NullOutputStream)
     */
    public static OutputStream nullOutputStream() {
        return NULL_OUTPUT_STREAM;
    }

    /**
     * Wraps a {@link InputStream}, limiting the number of bytes which can be read.
     *
     * @param in    the input stream to be wrapped
     * @param limit the maximum number of bytes to be read
     * @return a length-limited {@link InputStream}
     * @since 14.0 (since 1.0 as com.google.common.io.LimitInputStream)
     */
    public static InputStream limit(InputStream in, long limit) {
        return new LimitedInputStream(in, limit);
    }

    /**
     * Attempts to read enough bytes from the stream to fill the given byte array, with the same
     * behavior as {@link DataInput#readFully(byte[])}. Does not close the stream.
     *
     * @param in the input stream to read from.
     * @param b  the buffer into which the data is read.
     * @throws EOFException if this stream reaches the end before reading all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    public static void readFully(InputStream in, byte[] b) throws IOException {
        readFully(in, b, 0, b.length);
    }

    /**
     * Attempts to read {@code len} bytes from the stream into the given array starting at
     * {@code off}, with the same behavior as {@link DataInput#readFully(byte[], int, int)}. Does not
     * close the stream.
     *
     * @param in  the input stream to read from.
     * @param b   the buffer into which the data is read.
     * @param off an int specifying the offset into the data.
     * @param len an int specifying the number of bytes to read.
     * @throws EOFException if this stream reaches the end before reading all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    public static void readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        int read = read(in, b, off, len);
        if (read != len) {
            throw new EOFException(
                    "reached end of stream after reading " + read + " bytes; " + len + " bytes expected");
        }
    }

    /**
     * Discards {@code n} bytes of data from the input stream. This method will block until the full
     * amount has been skipped. Does not close the stream.
     *
     * @param in the input stream to read from
     * @param n  the number of bytes to skip
     * @throws EOFException if this stream reaches the end before skipping all the bytes
     * @throws IOException  if an I/O error occurs, or the stream does not support skipping
     */
    public static void skipFully(InputStream in, long n) throws IOException {
        long skipped = skipUpTo(in, n);
        if (skipped < n) {
            throw new EOFException(
                    "reached end of stream after skipping " + skipped + " bytes; " + n + " bytes expected");
        }
    }

    /**
     * Discards up to {@code n} bytes of data from the input stream. This method will block until
     * either the full amount has been skipped or until the end of the stream is reached, whichever
     * happens first. Returns the total number of bytes skipped.
     */
    static long skipUpTo(InputStream in, final long n) throws IOException {
        long totalSkipped = 0;
        byte[] buf = createBuffer();

        while (totalSkipped < n) {
            long remaining = n - totalSkipped;
            long skipped = skipSafely(in, remaining);

            if (skipped == 0) {
                // Do a buffered read since skipSafely could return 0 repeatedly, for example if
                // in.available() always returns 0 (the default).
                int skip = (int) Math.min(remaining, buf.length);
                if ((skipped = in.read(buf, 0, skip)) == -1) {
                    // Reached EOF
                    break;
                }
            }

            totalSkipped += skipped;
        }

        return totalSkipped;
    }

    /**
     * Attempts to skip up to {@code n} bytes from the given input stream, but not more than
     * {@code in.available()} bytes. This prevents {@code FileInputStream} from skipping more bytes
     * than actually remain in the file, something that it {@linkplain FileInputStream#skip(long)
     * specifies} it can do in its Javadoc despite the fact that it is violating the contract of
     * {@code InputStream.skip()}.
     */
    private static long skipSafely(InputStream in, long n) throws IOException {
        int available = in.available();
        return available == 0 ? 0 : in.skip(Math.min(available, n));
    }

    // Sometimes you don't care how many bytes you actually read, I guess.
    // (You know that it's either going to read len bytes or stop at EOF.)
    public static int read(InputStream in, byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException("len is negative");
        }
        int total = 0;
        while (total < len) {
            int result = in.read(b, off + total, len - total);
            if (result == -1) {
                break;
            }
            total += result;
        }
        return total;
    }

    /**
     * BAOS that provides limited access to its internal byte array.
     */
    private static final class FastByteArrayOutputStream extends ByteArrayOutputStream {
        /**
         * Writes the contents of the internal buffer to the given array starting at the given offset.
         * Assumes the array has space to hold count bytes.
         */
        void writeTo(byte[] b, int off) {
            System.arraycopy(buf, 0, b, off, count);
        }
    }

    private static final class LimitedInputStream extends FilterInputStream {

        private long left;
        private long mark = -1;

        LimitedInputStream(InputStream in, long limit) {
            super(in);
            left = limit;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(in.available(), left);
        }

        // it's okay to mark even if mark isn't supported, as reset won't work
        @Override
        public synchronized void mark(int readLimit) {
            in.mark(readLimit);
            mark = left;
        }

        @Override
        public int read() throws IOException {
            if (left == 0) {
                return -1;
            }

            int result = in.read();
            if (result != -1) {
                --left;
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (left == 0) {
                return -1;
            }

            len = (int) Math.min(len, left);
            int result = in.read(b, off, len);
            if (result != -1) {
                left -= result;
            }
            return result;
        }

        @Override
        public synchronized void reset() throws IOException {
            if (!in.markSupported()) {
                throw new IOException("Mark not supported");
            }
            if (mark == -1) {
                throw new IOException("Mark not set");
            }

            in.reset();
            left = mark;
        }

        @Override
        public long skip(long n) throws IOException {
            n = Math.min(n, left);
            long skipped = in.skip(n);
            left -= skipped;
            return skipped;
        }
    }
}
