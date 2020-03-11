/*
 * Provides Schnorr signature functionality.
 */

package crypto.EC;

import crypto.keccak.Keccak;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Provides Schnorr signature functionality.
 * @author Spencer Little
 * @version 1.0.0
 */
public class ECSign {

    /** The standard byte length for one integer in a Schnorr signature. h = 65 bytes, z <= 65 bytes. */
    private static final int STD_BLEN = 130;

    /**
     * Generates a Schnorr signature of the provided byte array.
     * @param prvScl the private key of the EC key pair to sign the data with
     * @param in the bytes to be signed
     * @return the digital signature in the form of a byte array
     */
    public static byte[] schnorrSign(BigInteger prvScl, byte[] in) {
        byte[] tmp = Keccak.KMACXOF256(prvScl.toByteArray(), in, 512, "N");
        byte[] kBytes = new byte[65];
        System.arraycopy(tmp, 0, kBytes, 1, tmp.length); // assure k is positive
        BigInteger k = new BigInteger(kBytes);
        k = k.multiply(BigInteger.valueOf(4L));

        CurvePoint U = ECKeyPair.G.scalarMultiply(k);
        tmp = Keccak.KMACXOF256(U.getX().toByteArray(), in, 512, "T");
        byte[] hBytes = new byte[65];
        System.arraycopy(tmp, 0, hBytes, 1, tmp.length); // assure h is positive
        BigInteger h = new BigInteger(hBytes);
        BigInteger z = k.subtract(h.multiply(prvScl)).mod(CurvePoint.R);

        return sigToByteArray(h, z);
    }

    /**
     * Verifies a Schnorr signature of the provided bytes based on the
     * provided public key.
     * @param sgn the Schnorr signature, see schnorrSign for details
     * @param pub the public key to valid the signature with
     * @param in the message to be validated
     * @return a boolean value indicating the validity of the signature
     */
    public static boolean validateSignature(byte[] sgn, CurvePoint pub, byte[] in) {
        boolean valid;
        try {
            BigInteger[] ints = sigFromByteArray(sgn);
            CurvePoint U = ECKeyPair.G.scalarMultiply(ints[1]).add(pub.scalarMultiply(ints[0]));
            byte[] tmp = Keccak.KMACXOF256(U.getX().toByteArray(), in, 512, "T");
            byte[] hBytes = new byte[65];
            System.arraycopy(tmp, 0, hBytes, 1, tmp.length); // assure h is positive
            BigInteger h = new BigInteger(hBytes);
            valid = h.equals(ints[0]);
        } catch (IllegalArgumentException iae) { // signature was not formatted properly
            valid = false;
        }

        return valid;
    }

    /**
     * Converts a Schnorr signature to a byte array of a standard fixed size
     * by calling toByteArray() on h and z. Each BigInteger is allotted 64
     * bytes, sign extended, and then places in the asBytes array.
     * @return an unambiguous byte array representation of this signature (h, z)
     */
    private static byte[] sigToByteArray(BigInteger h, BigInteger z) {
        byte[] asBytes = new byte[STD_BLEN];
        byte[] hBytes = h.toByteArray(), zBytes = z.toByteArray();
        int hPos = STD_BLEN / 2 - hBytes.length, zPos = asBytes.length - zBytes.length;

        if (h.signum() < 0) Arrays.fill(asBytes, 0, hPos, (byte) 0xff); // sign extend
        if (z.signum() < 0) Arrays.fill(asBytes, STD_BLEN / 2, zPos, (byte) 0xff);
        System.arraycopy(hBytes, 0, asBytes, hPos, hBytes.length);
        System.arraycopy(zBytes, 0, asBytes, zPos, zBytes.length);

        return asBytes;
    }

    /**
     * Extracts two BigIntegers from the provided byte array. Assumes the BigIntegers
     * have been encoded in the format specified in bigIntsToByteArray.
     * @param in the byte array to decode
     * @return a Schnorr signature in the form of two BigIntegers (h, z)
     */
    private static BigInteger[] sigFromByteArray(byte[] in) {
        if (in.length != STD_BLEN) throw new IllegalArgumentException("Provided byte array is not properly formatted");

        BigInteger h = new BigInteger(Arrays.copyOfRange(in, 0, STD_BLEN / 2));
        BigInteger z = new BigInteger(Arrays.copyOfRange(in, STD_BLEN / 2, STD_BLEN));

        return new BigInteger[] {h, z};
    }
}
