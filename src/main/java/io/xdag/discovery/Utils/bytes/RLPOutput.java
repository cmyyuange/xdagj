package io.xdag.discovery.Utils.bytes;

import io.xdag.discovery.Utils.bytes.uint.UInt256Value;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Collection;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkArgument;

public interface RLPOutput {

    /** Starts a new list. */
    void startList();

    /**
     * Ends the current list.
     *
     * @throws IllegalStateException if no list has been previously started with {@link #startList()}
     *     (or any started had already be ended).
     */
    void endList();

    /**
     * Writes a new value.
     *
     * @param v The value to write.
     */
    void writeBytesValue(BytesValue v);

    /**
     * Writes a RLP "null", that is an empty value.
     *
     * <p>This is a shortcut for {@code writeBytesValue(BytesValue.EMPTY)}.
     */
    default void writeNull() {
        writeBytesValue(BytesValue.EMPTY);
    }

    /**
     * Writes a scalar (encoded with no leading zeroes).
     *
     * @param v The scalar to write.
     * @throws IllegalArgumentException if {@code v < 0}.
     */
    default void writeIntScalar(final int v) {
        writeLongScalar(v);
    }

    /**
     * Writes a scalar (encoded with no leading zeroes).
     *
     * @param v The scalar to write.
     * @throws IllegalArgumentException if {@code v < 0}.
     */
    default void writeLongScalar(final long v) {
        checkArgument(v >= 0, "Invalid negative value %s for scalar encoding", v);
        writeBytesValue(BytesValues.toMinimalBytes(v));
    }

    /**
     * Writes a scalar (encoded with no leading zeroes).
     *
     * @param v The scalar to write.
     * @throws IllegalArgumentException if {@code v} is a negative integer ({@code v.signum() < 0}).
     */
    default void writeBigIntegerScalar(final BigInteger v) {
        checkArgument(v.signum() >= 0, "Invalid negative integer %s for scalar encoding", v);

        final byte[] bytes = v.toByteArray();
        // BigInteger will not include leading zeros by contract, but it always include at least one
        // bit of sign (a zero here since it's positive). What that mean is that if the first 1 of the
        // resulting number is exactly on a byte boundary, then the sign bit constraint will make the
        // value include one extra byte, which will be zero. In other words, they can be one zero bytes
        // in practice we should ignore, but there should never be more than one.
        writeBytesValue(
                bytes.length > 1 && bytes[0] == 0
                        ? BytesValue.wrap(bytes, 1, bytes.length - 1)
                        : BytesValue.wrap(bytes));
    }

    /**
     * Writes a scalar (encoded with no leading zeroes).
     *
     * @param v The scalar to write.
     */
    default void writeUInt256Scalar(final UInt256Value<?> v) {
        writeBytesValue(BytesValues.trimLeadingZeros(v.getBytes()));
    }

    /**
     * Writes a single byte value.
     *
     * @param b The byte to write.
     */
    default void writeByte(final byte b) {
        writeBytesValue(BytesValue.of(b));
    }

    /**
     * Writes a 2-bytes value.
     *
     * <p>Note that this is not a "scalar" write: the value will be encoded with exactly 2 bytes.
     *
     * @param s The 2-bytes short to write.
     */
    default void writeShort(final short s) {
        final byte[] res = new byte[2];
        res[0] = (byte) (s >> 8);
        res[1] = (byte) s;
        writeBytesValue(BytesValue.wrap(res));
    }

    /**
     * Writes a 4-bytes value.
     *
     * <p>Note that this is not a "scalar" write: the value will be encoded with exactly 4 bytes.
     *
     * @param i The 4-bytes int to write.
     */
    default void writeInt(final int i) {
        final MutableBytesValue v = MutableBytesValue.create(4);
        v.setInt(0, i);
        writeBytesValue(v);
    }

    /**
     * Writes a 8-bytes value.
     *
     * <p>Note that this is not a "scalar" write: the value will be encoded with exactly 8 bytes.
     *
     * @param l The 8-bytes long to write.
     */
    default void writeLong(final long l) {
        final MutableBytesValue v = MutableBytesValue.create(8);
        v.setLong(0, l);
        writeBytesValue(v);
    }

    /**
     * Writes a single byte value.
     *
     * @param b A value that must fit an unsigned byte.
     * @throws IllegalArgumentException if {@code b} does not fit an unsigned byte, that is if either
     *     {@code b < 0} or {@code b > 0xFF}.
     */
    default void writeUnsignedByte(final int b) {
        writeBytesValue(BytesValues.ofUnsignedByte(b));
    }

    /**
     * Writes a 2-bytes value.
     *
     * @param s A value that must fit an unsigned 2-bytes short.
     * @throws IllegalArgumentException if {@code s} does not fit an unsigned 2-bytes short, that is
     *     if either {@code s < 0} or {@code s > 0xFFFF}.
     */
    default void writeUnsignedShort(final int s) {
        writeBytesValue(BytesValues.ofUnsignedShort(s));
    }

    /**
     * Writes a 4-bytes value.
     *
     * @param i A value that must fit an unsigned 4-bytes integer.
     * @throws IllegalArgumentException if {@code i} does not fit an unsigned 4-bytes int, that is if
     *     either {@code i < 0} or {@code i > 0xFFFFFFFFL}.
     */
    default void writeUnsignedInt(final long i) {
        writeBytesValue(BytesValues.ofUnsignedInt(i));
    }

    /**
     * Writes the byte representation of an inet address (so either 4 or 16 bytes long).
     *
     * @param address The address to write.
     */
    default void writeInetAddress(final InetAddress address) {
        writeBytesValue(BytesValue.wrap(address.getAddress()));
    }

    /**
     * Writes a list of values of a specific class provided a function to write values of that class
     * to an {@link RLPOutput}.
     *
     * <p>This is a convenience method whose result is equivalent to doing:
     *
     * <pre>{@code
     * startList();
     * for (T v : values) {
     *   valueWriter.accept(v, this);
     * }
     * endList();
     * }</pre>
     *
     * @param values A list of value of type {@code T}.
     * @param valueWriter A method that given a value of type {@code T} and an {@link RLPOutput},
     *     writes this value to the output.
     * @param <T> The type of values to write.
     */
    default <T> void writeList(
            final Collection<T> values, final BiConsumer<T, RLPOutput> valueWriter) {
        startList();
        for (final T v : values) {
            valueWriter.accept(v, this);
        }
        endList();
    }

    /**
     * Writes an already RLP encoded item to the output.
     *
     * <p>This method is the functional equivalent of decoding the provided value entirely (to an
     * fully formed Java object) and then re-encoding that result to this output. It is however a lot
     * more efficient in that it saves most of that decoding/re-encoding work. Please note however
     * that this method <b>does</b> validate that the input is a valid RLP encoding. If you can
     * guaranteed that the input is valid and do not want this validation step, please have a look at
     * {@link #writeRLPUnsafe(BytesValue)}.
     *
     * @param rlpEncodedValue An already RLP encoded value to write as next item of this output.
     */
    default void writeRLP(final BytesValue rlpEncodedValue) {
        RLP.validate(rlpEncodedValue);
        writeRLPUnsafe(rlpEncodedValue);
    }

    /**
     * Writes an already RLP encoded item to the output.
     *
     * <p>This method is equivalent to {@link #writeRLP(BytesValue)}, but is unsafe in that it does
     * not do any validation of the its input. As such, it is faster but can silently yield invalid
     * RLP output if misused.
     *
     * @param rlpEncodedValue An already RLP encoded value to write as next item of this output.
     */
    void writeRLPUnsafe(BytesValue rlpEncodedValue);
}
