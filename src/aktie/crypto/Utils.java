package aktie.crypto;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.RIPEMD256Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.util.encoders.Base64;

import aktie.data.CObj;

public class Utils
{

    public static long CID0 = 0x0123456789abcdefL;
    public static long CID1 = 0x0fedcba987654321L;

    public static SecureRandom Random = new SecureRandom();

    //It may be faster for generating collisions
    public static java.util.Random NOTSECURERandom = new java.util.Random();

    // 4 hour
    public static int TIMEFUZZ = 4 * 60 * 60 * 1000;

    public static long fuzzTime ( long min )
    {
        long nt = ( new Date() ).getTime();
        nt -= Random.nextInt ( TIMEFUZZ );
        return Math.max ( nt, min );
    }

    public static long fuzzTime ( CObj... ol )
    {
        long min = 0;

        for ( CObj o : ol )
        {
            if ( o != null )
            {
                Long mt = o.getNumber ( CObj.CREATEDON );

                if ( mt != null )
                {
                    min = Math.max ( mt, min );
                }

            }

        }

        return fuzzTime ( min + 1 );
    }

    public static AsymmetricCipherKeyPair  generateKeyPair()
    {
        /*
            The probability that the new BigInteger represents a prime number will exceed (1 - 1/(2^certainty))
            2^40 > 1T
        */
        RSAKeyGenerationParameters parms = new RSAKeyGenerationParameters ( BigInteger.valueOf ( 65537 ), // exponent
                Random,                      // Random generator
                2048,                        // Key size
                40 );                        // Certainty
        RSAKeyPairGenerator gen = new RSAKeyPairGenerator();
        gen.init ( parms );
        return gen.generateKeyPair();
    }

    public static KeyParameter generateKey()
    {
        byte[] key = new byte[32];
        Random.nextBytes ( key );
        return new KeyParameter ( key );
    }

    public static String stringFromPublicKey ( RSAKeyParameters k )
    {
        String mod = Utils.toString ( k.getModulus().toByteArray() );
        String exp = Utils.toString ( k.getExponent().toByteArray() );
        StringBuilder sb = new StringBuilder();
        sb.append ( mod );
        sb.append ( "," );
        sb.append ( exp );
        return sb.toString();
    }

    public static RSAKeyParameters publicKeyFromString ( String s )
    {
        /*     public RSAKeyParameters(
                boolean     isPrivate,
                BigInteger  modulus,
                BigInteger  exponent  */
        String prts[] = s.split ( "," );

        byte modb[] = Utils.toByteArray ( prts[0] );
        byte expb[] = Utils.toByteArray ( prts[1] );

        BigInteger mod = new BigInteger ( modb );
        BigInteger exp = new BigInteger ( expb );

        return new RSAKeyParameters ( false, mod, exp );
    }

    public static String stringFromPrivateKey ( RSAPrivateCrtKeyParameters k )
    {
        String mod = Utils.toString ( k.getModulus().toByteArray() );
        String pubex = Utils.toString ( k.getPublicExponent().toByteArray() );
        String privex = Utils.toString ( k.getExponent().toByteArray() );
        String p = Utils.toString ( k.getP().toByteArray() );
        String q = Utils.toString ( k.getQ().toByteArray() );
        String dp = Utils.toString ( k.getDP().toByteArray() );
        String dq = Utils.toString ( k.getDQ().toByteArray() );
        String qinv = Utils.toString ( k.getQInv().toByteArray() );
        StringBuilder sb = new StringBuilder();
        sb.append ( mod );
        sb.append ( "," );
        sb.append ( pubex );
        sb.append ( "," );
        sb.append ( privex );
        sb.append ( "," );
        sb.append ( p );
        sb.append ( "," );
        sb.append ( q );
        sb.append ( "," );
        sb.append ( dp );
        sb.append ( "," );
        sb.append ( dq );
        sb.append ( "," );
        sb.append ( qinv );
        return sb.toString();
    }

    public static RSAPrivateCrtKeyParameters privateKeyFromString ( String k )
    {
        /*      public RSAPrivateCrtKeyParameters(
                BigInteger  modulus,
                BigInteger  publicExponent,
                BigInteger  privateExponent,
                BigInteger  p,
                BigInteger  q,
                BigInteger  dP,
                BigInteger  dQ,
                BigInteger  qInv)    */
        String prts[] = k.split ( "," );

        if ( prts.length != 8 )
        {
            throw new RuntimeException ( "Invalid private key string format" );
        }

        byte modb[] = Utils.toByteArray ( prts[0] );
        byte pubexb[] = Utils.toByteArray ( prts[1] );
        byte privexb[] = Utils.toByteArray ( prts[2] );
        byte pb[] = Utils.toByteArray ( prts[3] );
        byte qb[] = Utils.toByteArray ( prts[4] );
        byte dpb[] = Utils.toByteArray ( prts[5] );
        byte dqb[] = Utils.toByteArray ( prts[6] );
        byte qinvb[] = Utils.toByteArray ( prts[7] );

        BigInteger mod = new BigInteger ( modb );
        BigInteger pubex = new BigInteger ( pubexb );
        BigInteger privex = new BigInteger ( privexb );
        BigInteger p = new BigInteger ( pb );
        BigInteger q = new BigInteger ( qb );
        BigInteger dp = new BigInteger ( dpb );
        BigInteger dq = new BigInteger ( dqb );
        BigInteger qinv = new BigInteger ( qinvb );

        return new RSAPrivateCrtKeyParameters (
                   mod,
                   pubex,
                   privex,
                   p,
                   q,
                   dp,
                   dq,
                   qinv );
    }

    public static String toString ( byte b[] )
    {
        try
        {
            String s = new String ( Base64.encode ( b ), "UTF-8" );
            return s;
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            throw new RuntimeException ( "Could not enocde bytes!" );
        }

    }

    public static byte[] stringToByteArray ( String s )
    {
        try
        {
            return s.getBytes ( "UTF-8" );
        }

        catch ( UnsupportedEncodingException e )
        {
            throw new RuntimeException ( "Ooops.", e );
        }

    }

    public static String toStringRaw ( byte b[] )
    {
        try
        {
            return new String ( b, "UTF-8" );
        }

        catch ( UnsupportedEncodingException e )
        {
            throw new RuntimeException ( "Ooops.", e );
        }

    }

    public static byte[] toByteArray ( String s )
    {
        if ( s == null ) { return null; }

        return Base64.decode ( s );
    }

    public static byte[] fixLength ( byte[] b, int len )
    {
        if ( b.length >= len )
        {
            return b;
        }

        byte [] r = new byte[len];
        Arrays.fill ( r, ( byte ) 0 );
        System.arraycopy ( b, 0, r, 0, b.length );
        return r;
    }

    public static void digBytes ( Digest d, byte[] b )
    {
        d.update ( b, 0, b.length );
    }

    public static byte[] digBytes ( byte d[], byte b[] )
    {
        RIPEMD256Digest dig = new RIPEMD256Digest();

        if ( d != null )
        {
            dig.update ( d, 0, d.length );
        }

        else
        {
            d = new byte[dig.getDigestSize()];
        }

        dig.update ( b, 0, b.length );
        dig.doFinal ( d, 0 );
        return d;
    }

    /**
        Digest d, then b, then put result in t
        @param t
        @param d
        @param b
        @return
    */
    public static byte[] digBytes ( byte t[], byte d[], byte b[] )
    {
        RIPEMD256Digest dig = new RIPEMD256Digest();

        if ( t == null )
        {
            t = new byte[dig.getDigestSize()];
        }

        dig.update ( d, 0, d.length );
        dig.update ( b, 0, b.length );
        dig.doFinal ( t, 0 );
        return d;
    }

    public static byte[] digString ( byte d[], String s )
    {
        try
        {
            return digBytes ( d, s.getBytes ( "UTF-16BE" ) );
        }

        catch ( UnsupportedEncodingException e )
        {
            e.printStackTrace();
            throw new RuntimeException ( "Invalid encoding: " + e.getMessage() );
        }

    }

    public static void digString ( Digest d, String s )
    {
        try
        {
            digBytes ( d, s.getBytes ( "UTF-16BE" ) );
        }

        catch ( Exception e )
        {
            throw new RuntimeException ( "Could not convert string to bytes." );
        }

    }

    public static void digLong ( Digest d, long l )
    {
        byte[] bl = new byte[Long.SIZE / Byte.SIZE];
        ByteBuffer bb = ByteBuffer.wrap ( bl );
        bb.putLong ( l );
        digBytes ( d, bl );
    }

    public static void digDouble ( Digest d, double b )
    {
        byte[] bl = new byte[Double.SIZE / Byte.SIZE];
        ByteBuffer bb = ByteBuffer.wrap ( bl );
        bb.putDouble ( b );
        digBytes ( d, bl );
    }

    public static byte[] xorBytes ( byte x0[], byte x1[] )
    {
        if ( x0.length != x1.length )
        {
            throw new RuntimeException ( "Must XOR byte arrays the same length." );
        }

        for ( int c = 0; c < x0.length; c++ )
        {
            x0[c] ^= x1[c];
        }

        return x0;
    }

    public static byte[] xorBytes ( byte rb[], byte x0[], byte x1[] )
    {
        if ( rb == null )
        {
            rb = new byte[x0.length];
        }

        if ( x0.length != x1.length || x0.length != rb.length )
        {
            throw new RuntimeException ( "Must XOR byte arrays the same length." );
        }

        for ( int c = 0; c < x0.length; c++ )
        {
            rb[c] = ( byte ) ( x0[c] ^ x1[c] );
        }

        return rb;
    }

    public static byte[] digByteMap ( byte db[], Map<String, byte[]> m )
    {
        Digest d = new RIPEMD256Digest();

        if ( db == null )
        {
            db = new byte[d.getDigestSize()];
            Arrays.fill ( db, ( byte ) 0 );
        }

        byte tb[] = new byte[d.getDigestSize()];

        for ( Entry<String, byte[]> e : m.entrySet() )
        {
            d.reset();
            digString ( d, e.getKey() );
            digBytes ( d, e.getValue() );
            d.doFinal ( tb, 0 );
            xorBytes ( db, tb );
        }

        return db;
    }

    public static final byte LASTB[] =
    {
        ( byte ) 0x00,
        ( byte ) 0x80,
        ( byte ) 0xC0,
        ( byte ) 0xE0,
        ( byte ) 0xF0,
        ( byte ) 0xF8,
        ( byte ) 0xFC,
        ( byte ) 0xFE,
        ( byte ) 0xFF
    };

    public static byte [] getTarget ( long tv, int len )
    {
        byte r[] = new byte[len];
        Arrays.fill ( r, ( byte ) 0 );
        ByteBuffer bf = ByteBuffer.wrap ( r );
        bf.putLong ( tv );
        return r;
    }

    public static boolean checkDig ( byte d[], byte v[] )
    {
        if ( d.length != v.length )
        {
            throw new RuntimeException ( "MASK AND DIG NOT THE SAME LENGTH!" );
        }

        int idx = 0;

        while ( d[idx] == v[idx] && idx < d.length )
        {
            idx++;
        }

        if ( idx == d.length )
        {
            return true;
        }

        return ( ( 0xFF & d[idx] ) < ( 0xFF & v[idx] ) );
    }

    public static byte[] digStringMap ( byte db[], Map<String, String> m )
    {
        Digest d = new RIPEMD256Digest();

        if ( db == null )
        {
            db = new byte[d.getDigestSize()];
            Arrays.fill ( db, ( byte ) 0 );
        }

        byte tb[] = new byte[d.getDigestSize()];

        for ( Entry<String, String> e : m.entrySet() )
        {
            d.reset();
            digString ( d, e.getKey() );
            digString ( d, e.getValue() );
            d.doFinal ( tb, 0 );
            xorBytes ( db, tb );
        }

        return db;
    }

    public static byte[] digLongMap ( byte db[], Map<String, Long> m )
    {
        Digest d = new RIPEMD256Digest();

        if ( db == null )
        {
            db = new byte[d.getDigestSize()];
            Arrays.fill ( db, ( byte ) 0 );
        }

        byte tb[] = new byte[d.getDigestSize()];

        for ( Entry<String, Long> e : m.entrySet() )
        {
            d.reset();
            digString ( d, e.getKey() );
            digLong ( d, e.getValue() );
            d.doFinal ( tb, 0 );
            xorBytes ( db, tb );
        }

        return db;
    }

    public static byte[] digLongMap ( byte rb[], byte tb[], byte db[], Map<String, Long> m )
    {
        Digest d = new RIPEMD256Digest();

        if ( db == null )
        {
            db = new byte[d.getDigestSize()];
            Arrays.fill ( db, ( byte ) 0 );
        }

        if ( rb == null )
        {
            rb = new byte[d.getDigestSize()];
        }

        if ( tb == null )
        {
            tb = new byte[d.getDigestSize()];
        }

        for ( Entry<String, Long> e : m.entrySet() )
        {
            d.reset();
            digString ( d, e.getKey() );
            digLong ( d, e.getValue() );
            d.doFinal ( tb, 0 );
            xorBytes ( rb, db, tb );
        }

        return rb;
    }

    public static byte[] digStringMap ( byte rb[], byte tb[], byte db[], Map<String, String> m )
    {
        Digest d = new RIPEMD256Digest();

        if ( db == null )
        {
            db = new byte[d.getDigestSize()];
            Arrays.fill ( db, ( byte ) 0 );
        }

        if ( rb == null )
        {
            rb = new byte[d.getDigestSize()];
        }

        if ( tb == null )
        {
            tb = new byte[d.getDigestSize()];
        }

        for ( Entry<String, String> e : m.entrySet() )
        {
            d.reset();
            digString ( d, e.getKey() );
            digString ( d, e.getValue() );
            d.doFinal ( tb, 0 );
            xorBytes ( rb, db, tb );
        }

        return rb;
    }

    public static byte[] digDoubleMap ( byte db[], Map<String, Double> m )
    {
        Digest d = new RIPEMD256Digest();

        if ( db == null )
        {
            db = new byte[d.getDigestSize()];
            Arrays.fill ( db, ( byte ) 0 );
        }

        byte tb[] = new byte[d.getDigestSize()];

        for ( Entry<String, Double> e : m.entrySet() )
        {
            d.reset();
            digString ( d, e.getKey() );
            digDouble ( d, e.getValue() );
            d.doFinal ( tb, 0 );
            xorBytes ( db, tb );
        }

        return db;
    }

    public static byte[] anonymousSymEncode ( KeyParameter key, long mn0, long mn1, byte plain[] )
    {
        int ls = Long.SIZE / Byte.SIZE;
        int is = Integer.SIZE / Byte.SIZE;
        byte tb[] = new byte[plain.length + ( 2 * ls ) + ( 2 * is )];
        ByteBuffer bb = ByteBuffer.wrap ( tb );
        bb.putInt ( Random.nextInt() );
        bb.putLong ( mn0 );
        bb.putLong ( mn1 );
        bb.putInt ( plain.length );
        bb.put ( plain );
        CBCBlockCipher aes = new CBCBlockCipher ( new AESEngine() );
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher ( aes, new PKCS7Padding() );
        cipher.init ( true, key );

        try
        {
            int dlen = cipher.getOutputSize ( tb.length );
            byte[] output = new byte[dlen];
            int len = cipher.processBytes ( tb, 0, tb.length, output, 0 );
            cipher.doFinal ( output, len );
            return output;
        }

        catch ( Exception e )
        {
        }

        return null;
    }

    public static byte[] attemptSymDecode ( KeyParameter key, long mn0, long mn1, byte encd[] )
    {
        int ls = Long.SIZE / Byte.SIZE;
        int is = Integer.SIZE / Byte.SIZE;
        CBCBlockCipher aes = new CBCBlockCipher ( new AESEngine() );
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher ( aes, new PKCS7Padding() );
        cipher.init ( false, key );

        try
        {
            int dlen = cipher.getOutputSize ( encd.length );

            if ( dlen > ( ( 2 * ls ) + ( 2 * is ) ) )
            {
                byte[] output = new byte[dlen];
                int len = cipher.processBytes ( encd, 0, encd.length, output, 0 );
                cipher.doFinal ( output, len );
                ByteBuffer bb = ByteBuffer.wrap ( output );
                bb.getInt();
                long c0 = bb.getLong();
                long c1 = bb.getLong();
                int nlen = bb.getInt();

                if ( c0 == mn0 && c1 == mn1 && nlen <= bb.remaining() )
                {
                    byte rb[] = new byte[nlen];
                    bb.get ( rb );
                    return rb;
                }

            }

        }

        catch ( Exception e )
        {
        }

        return null;
    }

    public static byte[] anonymousAsymEncode ( RSAKeyParameters key, long mn0, long mn1, byte plain[] )
    {
        int ls = Long.SIZE / Byte.SIZE;
        int is = Integer.SIZE / Byte.SIZE;
        byte nb[] = new byte[plain.length + ( ls * 2 ) + ( is * 2 )];
        ByteBuffer bb = ByteBuffer.wrap ( nb );
        bb.putInt ( Random.nextInt() );
        bb.putLong ( mn0 );
        bb.putLong ( mn1 );
        bb.putInt ( plain.length );
        bb.put ( plain );
        RSAEngine eng = new RSAEngine();
        PKCS1Encoding enc = new PKCS1Encoding ( eng );
        enc.init ( true, key );

        try
        {
            byte decsig[] = enc.processBlock ( nb, 0, nb.length );
            return decsig;
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] attemptAsymDecode ( RSAPrivateCrtKeyParameters key, long mn0, long mn1, byte encdat[] )
    {
        RSAEngine eng = new RSAEngine();
        PKCS1Encoding enc = new PKCS1Encoding ( eng );
        enc.init ( false, key );

        try
        {
            byte decsig[] = enc.processBlock ( encdat, 0, encdat.length );
            int ls = Long.SIZE / Byte.SIZE;
            int il = Integer.SIZE / Byte.SIZE;

            if ( decsig.length > ( ( ls * 2 ) + ( il * 2 ) ) )
            {
                ByteBuffer bb = ByteBuffer.wrap ( decsig );
                bb.getInt();
                long c0 = bb.getLong();
                long c1 = bb.getLong();
                int len = bb.getInt();

                if ( c0 == mn0 && c1 == mn1 && len <= bb.remaining() )
                {
                    byte rb[] = new byte[len];
                    bb.get ( rb );
                    return rb;
                }

            }

        }

        catch ( Exception e )
        {
        }

        return null;
    }

    public static String mergeIds ( String id0, String id1 )
    {
        byte i0[] = toByteArray ( id0 );
        byte i1[] = toByteArray ( id1 );

        int mlen = Math.max ( i0.length, i1.length );
        i0 = fixLength ( i0, mlen );
        i1 = fixLength ( i1, mlen );

        //if ( i0.length != i1.length )
        //{
        //    throw new RuntimeException ( "Lengths must be equal." );
        //}

        for ( int c = 0; c < i0.length; c++ )
        {
            i0[c] = ( byte ) ( ( 0xFF & i0[c] ) ^ ( 0xFF & i1[c] ) );
        }

        return toString ( i0 );
    }

    public static String mergeIds ( String id0, String id1, String id2 )
    {
        byte i0[] = toByteArray ( id0 );
        byte i1[] = toByteArray ( id1 );
        byte i2[] = toByteArray ( id2 );

        int mlen = Math.max ( i0.length, i1.length );
        mlen = Math.max ( mlen, i2.length );
        i0 = fixLength ( i0, mlen );
        i1 = fixLength ( i1, mlen );
        i2 = fixLength ( i2, mlen );

        //if ( i0.length != i1.length || i0.length != i2.length )
        //{
        //    throw new RuntimeException ( "Lengths must be equal." );
        //}

        for ( int c = 0; c < i0.length; c++ )
        {
            i0[c] = ( byte ) ( ( 0xFF & i0[c] ) ^ ( 0xFF & i1[c] ) ^ ( 0xFF & i2[c] ) ) ;
        }

        return toString ( i0 );
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex ( byte[] bytes )
    {
        char[] hexChars = new char[bytes.length * 2];

        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String ( hexChars );
    }

}
