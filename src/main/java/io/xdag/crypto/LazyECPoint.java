package io.xdag.crypto;

import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECPoint;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

public class LazyECPoint {

    // If curve is set, bits is also set. If curve is unset, point is set and bits is unset. Point can be set along
    // with curve and bits when the cached form has been accessed and thus must have been converted.

    private final ECCurve curve;
    private final byte[] bits;
    private final boolean compressed;

    // This field is effectively final - once set it won't change again. However it can be set after
    // construction.
    @Nullable
    private ECPoint point;

    public LazyECPoint(ECCurve curve, byte[] bits) {
        this.curve = curve;
        this.bits = bits;
        this.compressed = ECKey.isPubKeyCompressed(bits);
    }

    public LazyECPoint(ECPoint point, boolean compressed) {
        this.point = checkNotNull(point).normalize();
        this.compressed = compressed;
        this.curve = null;
        this.bits = null;
    }

    public ECPoint get() {
        if (point == null)
            point = curve.decodePoint(bits);
        return point;
    }

    public byte[] getEncoded() {
        if (bits != null)
            return Arrays.copyOf(bits, bits.length);
        else
            return get().getEncoded(compressed);
    }

    // Delegated methods.

    public ECPoint getDetachedPoint() {
        return get().getDetachedPoint();
    }

    public boolean isInfinity() {
        return get().isInfinity();
    }

    public ECPoint timesPow2(int e) {
        return get().timesPow2(e);
    }

    public ECFieldElement getYCoord() {
        return get().getYCoord();
    }

    public ECFieldElement[] getZCoords() {
        return get().getZCoords();
    }

    public boolean isNormalized() {
        return get().isNormalized();
    }

    public boolean isCompressed() {
        return compressed;
    }

    public ECPoint multiply(BigInteger k) {
        return get().multiply(k);
    }

    public ECPoint subtract(ECPoint b) {
        return get().subtract(b);
    }

    public boolean isValid() {
        return get().isValid();
    }

    public ECPoint scaleY(ECFieldElement scale) {
        return get().scaleY(scale);
    }

    public ECFieldElement getXCoord() {
        return get().getXCoord();
    }

    public ECPoint scaleX(ECFieldElement scale) {
        return get().scaleX(scale);
    }

    public boolean equals(ECPoint other) {
        return get().equals(other);
    }

    public ECPoint negate() {
        return get().negate();
    }

    public ECPoint threeTimes() {
        return get().threeTimes();
    }

    public ECFieldElement getZCoord(int index) {
        return get().getZCoord(index);
    }

    public byte[] getEncoded(boolean compressed) {
        if (compressed == isCompressed() && bits != null)
            return Arrays.copyOf(bits, bits.length);
        else
            return get().getEncoded(compressed);
    }

    public ECPoint add(ECPoint b) {
        return get().add(b);
    }

    public ECPoint twicePlus(ECPoint b) {
        return get().twicePlus(b);
    }

    public ECCurve getCurve() {
        return get().getCurve();
    }

    public ECPoint normalize() {
        return get().normalize();
    }

    public ECFieldElement getY() {
        return this.normalize().getYCoord();
    }

    public ECPoint twice() {
        return get().twice();
    }

    public ECFieldElement getAffineYCoord() {
        return get().getAffineYCoord();
    }

    public ECFieldElement getAffineXCoord() {
        return get().getAffineXCoord();
    }

    public ECFieldElement getX() {
        return this.normalize().getXCoord();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Arrays.equals(getCanonicalEncoding(), ((LazyECPoint)o).getCanonicalEncoding());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getCanonicalEncoding());
    }

    private byte[] getCanonicalEncoding() {
        return getEncoded(true);
    }

}
