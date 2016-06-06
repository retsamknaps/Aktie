package aktie;

import static org.junit.Assert.*;

import org.apache.lucene.document.Document;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import aktie.crypto.Utils;
import aktie.data.CObj;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Random;

public class testCryptoUtils
{


    @Test
    public void testAnonSym()
    {
        KeyParameter k0 = Utils.generateKey();
        KeyParameter k1 = Utils.generateKey();
        byte t[] = new byte[1233];
        Utils.Random.nextBytes ( t );
        byte e[] = Utils.anonymousSymEncode ( k0,
                                              0xDEADBEEFL, 0x012345L, t );
        assertNotNull ( e );
        byte d[] = Utils.attemptSymDecode ( k1,
                                            0xDEADBEEFL, 0x012345L, e );
        assertNull ( d );
        d = Utils.attemptSymDecode ( k0,
                                     0xDEADBEEFL, 0x012345L, e );
        assertArrayEquals ( t, d );
        d = Utils.attemptSymDecode ( k0,
                                     0xCEADBEEFL, 0x012345L, e );
        assertNull ( d );
    }

    @Test
    public void testAnonAsym()
    {
        AsymmetricCipherKeyPair k0 = Utils.generateKeyPair();
        AsymmetricCipherKeyPair k1 = Utils.generateKeyPair();
        byte t[] = new byte[123];
        Utils.Random.nextBytes ( t );
        byte e[] = Utils.anonymousAsymEncode ( ( RSAKeyParameters ) k0.getPublic(),
                                               0x01234567L, 0xBEEFF, t );
        assertNotNull ( e );
        byte d[] = Utils.attemptAsymDecode ( ( RSAPrivateCrtKeyParameters ) k1.getPrivate(),
                                             0x01234567L, 0xBEEFF, e );
        assertNull ( d );
        d = Utils.attemptAsymDecode ( ( RSAPrivateCrtKeyParameters ) k0.getPrivate(),
                                      0x01234567L, 0xBEEFF, e );
        assertArrayEquals ( t, d );
        d = Utils.attemptAsymDecode ( ( RSAPrivateCrtKeyParameters ) k0.getPrivate(),
                                      0x01234567L, 0xAEEFF, e );
        assertNull ( d );
    }

    @Test
    public void testSignature()
    {
        AsymmetricCipherKeyPair k = Utils.generateKeyPair();
        CObj o = new CObj();
        o.setType ( "typeblah" );
        o.setId ( "id0" );
        byte b0[] = new byte[24];
        byte b1[] = new byte[76];
        Utils.Random.nextBytes ( b0 );
        Utils.Random.nextBytes ( b1 );
        o.pushNumber ( "n0", Utils.Random.nextLong() );
        o.pushNumber ( "n1", Utils.Random.nextLong() );
        o.pushDecimal ( "d0", Utils.Random.nextDouble() );
        o.pushDecimal ( "d1", Utils.Random.nextDouble() );
        o.pushString ( "s0", "string0" );
        o.pushString ( "s1", "string1" );
        o.pushText ( "t0", "text0" );
        o.pushText ( "t1", "test1" );
        o.signX ( ( RSAPrivateCrtKeyParameters ) k.getPrivate(), 8 );
        byte d[] = Utils.toByteArray ( o.getDig() );
        System.out.println ( "DIG: " + Integer.toHexString ( d[0] ) +
                             Integer.toHexString ( d[1] ) + Integer.toHexString ( d[2] ) );
        assertTrue ( o.checkSignatureX ( ( RSAKeyParameters ) k.getPublic(), 8 ) );
    }

    @Test
    public void testSignatureNull()
    {
        AsymmetricCipherKeyPair k = Utils.generateKeyPair();
        CObj o = new CObj();
        //o.setType("typeblah");
        byte b0[] = new byte[24];
        byte b1[] = new byte[76];
        Utils.Random.nextBytes ( b0 );
        Utils.Random.nextBytes ( b1 );
        //o.pushNumber("n0", Utils.Random.nextLong());
        //o.pushNumber("n1", Utils.Random.nextLong());
        o.pushDecimal ( "d0", Utils.Random.nextDouble() );
        o.pushDecimal ( "d1", Utils.Random.nextDouble() );
        //o.pushString("s0", "string0");
        //o.pushString("s1", "string1");
        o.pushText ( "t0", "text0" );
        o.pushText ( "t1", "test1" );
        long stt = System.currentTimeMillis();
        o.signX ( ( RSAPrivateCrtKeyParameters ) k.getPrivate(), 26 );
        long edt = System.currentTimeMillis();
        edt = ( edt - stt ) / 1000L;
        System.out.println ( "GEN TIME: " + edt + "s" );
        byte d[] = Utils.toByteArray ( o.getDig() );
        System.out.println ( "DIG: " + Utils.bytesToHex ( d ) );
        assertTrue ( o.checkSignatureX ( ( RSAKeyParameters ) k.getPublic(), 26 ) );
    }

    @Test
    public void testCObjectDocumentNull()
    {
        CObj o = new CObj();
        //o.setDig(new byte[25]);
        //Utils.Random.nextBytes(o.getDig());
        byte sb[] = new byte[121];
        Utils.Random.nextBytes ( sb );
        o.setSignature ( Utils.toString ( sb ) );
        //o.setType("typeblah");
        byte b0[] = new byte[24];
        byte b1[] = new byte[76];
        Utils.Random.nextBytes ( b0 );
        Utils.Random.nextBytes ( b1 );
        //o.pushNumber("n0", Utils.Random.nextLong());
        //o.pushNumber("n1", Utils.Random.nextLong());
        o.pushDecimal ( "d0", Utils.Random.nextDouble() );
        //o.pushDecimal("d1", Utils.Random.nextDouble());
        o.pushString ( "s0", "string0" );
        o.pushString ( "s1", "string1" );
        //o.pushText("t0", "text0");
        //o.pushText("t1", "test1");
        Document d = o.getDocument();

        CObj o2 = new CObj();
        o2.loadDocument ( d );
        assertTrue ( o.whoopyEquals ( o2 ) );
    }

    @Test
    public void testCObjectDocument()
    {
        CObj o = new CObj();
        byte db[] = new byte[25];
        Utils.Random.nextBytes ( db );
        o.setDig ( Utils.toString ( db ) );
        byte sb[] = new byte[121];
        Utils.Random.nextBytes ( sb );
        o.setSignature ( Utils.toString ( sb ) );
        o.setId ( "id0" );
        o.setType ( "typeblah" );
        o.pushNumber ( "n0", Utils.Random.nextLong() );
        o.pushNumber ( "n1", Utils.Random.nextLong() );
        o.pushDecimal ( "d0", Utils.Random.nextDouble() );
        o.pushDecimal ( "d1", Utils.Random.nextDouble() );
        o.pushString ( "s0", "string0" );
        o.pushString ( "s1", "string1" );
        o.pushText ( "t0", "text0" );
        o.pushText ( "t1", "test1" );
        Document d = o.getDocument();
        CObj o2 = new CObj();
        o2.loadDocument ( d );
        assertEquals ( o, o2 );
    }

    @Test
    public void testCObjectJSONNull()
    {
        CObj o = new CObj();
        //o.setDig(new byte[25]);
        //Utils.Random.nextBytes(o.getDig());
        byte sb[] = new byte[121];
        Utils.Random.nextBytes ( sb );;
        o.setSignature ( Utils.toString ( sb ) );
        //o.setType("typeblah");
        //o.pushNumber("n0", Utils.Random.nextLong());
        //o.pushNumber("n1", Utils.Random.nextLong());
        o.pushDecimal ( "d0", Utils.Random.nextDouble() );
        //o.pushDecimal("d1", Utils.Random.nextDouble());
        o.pushString ( "s0", "string0" );
        o.pushString ( "s1", "string1" );
        //o.pushText("t0", "text0");
        //o.pushText("t1", "test1");
        JSONObject jo = o.getJSON();
        String js = jo.toString();
        JSONObject j2 = new JSONObject ( new JSONTokener ( js ) );
        CObj o2 = new CObj();
        o2.loadJSON ( j2 );
        assertTrue ( o.whoopyEquals ( o2 ) );
    }

    @Test
    public void testCObjectJSON()
    {
        CObj o = new CObj();
        byte db[] = new byte[25];
        Utils.Random.nextBytes ( db );
        o.setDig ( Utils.toString ( db ) );
        byte sb[] = new byte[121];
        Utils.Random.nextBytes ( sb );;
        o.setSignature ( Utils.toString ( sb ) );
        o.setId ( "id0" );
        o.setType ( "typeblah" );
        o.pushNumber ( "n0", Utils.Random.nextLong() );
        o.pushNumber ( "n1", Utils.Random.nextLong() );
        o.pushDecimal ( "d0", Utils.Random.nextDouble() );
        o.pushDecimal ( "d1", Utils.Random.nextDouble() );
        o.pushString ( "s0", "string0" );
        o.pushString ( "s1", "string1" );
        o.pushText ( "t0", "text0" );
        o.pushText ( "t1", "test1" );
        JSONObject jo = o.getJSON();
        String js = jo.toString();
        JSONObject j2 = new JSONObject ( new JSONTokener ( js ) );
        CObj o2 = new CObj();
        o2.loadJSON ( j2 );
        assertEquals ( o, o2 );
    }

    @Test
    public void testHexStringLong()
    {
        int ls = ( Long.SIZE / Byte.SIZE );
        byte e[] = new byte[ ( ls * 23 ) + 1];
        assertTrue ( e.length % ls != 0 );
        Random r = new Random ( 0xDEADBEEFL );
        r.nextBytes ( e );
        String s = Utils.toString ( e );
        byte eb[] = Utils.toByteArray ( s );
        assertArrayEquals ( e, eb );
    }

    @Test
    public void testHexStringShort()
    {
        byte e[] = new byte[1];
        Random r = new Random ( 0xDEADBEEFL );
        r.nextBytes ( e );
        String s = Utils.toString ( e );
        byte eb[] = Utils.toByteArray ( s );
        assertArrayEquals ( e, eb );
    }

    @Test
    public void testDigStringMap()
    {
        String k0 = "KEY0";
        String v0 = "VALUE0";
        String k1 = "KEY1";
        String v1 = "VALUE1";
        String k2 = "KEY2";
        String v2 = "VALUE2";

        LinkedHashMap<String, String> e0 = new LinkedHashMap<String, String>();
        e0.put ( k0, v0 );
        e0.put ( k1, v1 );
        e0.put ( k2, v2 );
        byte[] e0dig = Utils.digStringMap ( null, e0 );

        LinkedHashMap<String, String> e1 = new LinkedHashMap<String, String>();
        e1.put ( k2, v2 );
        e1.put ( k0, v0 );
        e1.put ( k1, v1 );
        byte[] e1dig = Utils.digStringMap ( null, e1 );

        assertArrayEquals ( e0dig, e1dig );

        Iterator<String> i0 = e0.keySet().iterator();
        String tk0 = i0.next();
        Iterator<String> i1 = e1.keySet().iterator();
        String tk1 = i1.next();
        assertNotEquals ( tk0, tk1 );

        LinkedHashMap<String, String> e2 = new LinkedHashMap<String, String>();
        e2.put ( k2, v0 );
        e2.put ( k0, v2 );
        e2.put ( k1, v1 );
        byte[] e2dig = Utils.digStringMap ( null, e2 );
        assertFalse ( Arrays.equals ( e0dig, e2dig ) );
    }

    @Test
    public void testDigLongMap()
    {
        String k0 = "KEY0";
        Long v0 = 0L;
        String k1 = "KEY1";
        Long v1 = 10L;
        String k2 = "KEY2";
        Long v2 = 100L;

        LinkedHashMap<String, Long> e0 = new LinkedHashMap<String, Long>();
        e0.put ( k0, v0 );
        e0.put ( k1, v1 );
        e0.put ( k2, v2 );
        byte[] e0dig = Utils.digLongMap ( null, e0 );

        LinkedHashMap<String, Long> e1 = new LinkedHashMap<String, Long>();
        e1.put ( k2, v2 );
        e1.put ( k0, v0 );
        e1.put ( k1, v1 );
        byte[] e1dig = Utils.digLongMap ( null, e1 );

        assertArrayEquals ( e0dig, e1dig );

        Iterator<String> i0 = e0.keySet().iterator();
        String tk0 = i0.next();
        Iterator<String> i1 = e1.keySet().iterator();
        String tk1 = i1.next();
        assertNotEquals ( tk0, tk1 );

        LinkedHashMap<String, Long> e2 = new LinkedHashMap<String, Long>();
        e2.put ( k2, v0 );
        e2.put ( k0, v2 );
        e2.put ( k1, v1 );
        byte[] e2dig = Utils.digLongMap ( null, e2 );
        assertFalse ( Arrays.equals ( e0dig, e2dig ) );
    }

    @Test
    public void testDigDoubleMap()
    {
        String k0 = "KEY0";
        Double v0 = 0.1D;
        String k1 = "KEY1";
        Double v1 = 10.01D;
        String k2 = "KEY2";
        Double v2 = 100.001D;

        LinkedHashMap<String, Double> e0 = new LinkedHashMap<String, Double>();
        e0.put ( k0, v0 );
        e0.put ( k1, v1 );
        e0.put ( k2, v2 );
        byte[] e0dig = Utils.digDoubleMap ( null, e0 );

        LinkedHashMap<String, Double> e1 = new LinkedHashMap<String, Double>();
        e1.put ( k2, v2 );
        e1.put ( k0, v0 );
        e1.put ( k1, v1 );
        byte[] e1dig = Utils.digDoubleMap ( null, e1 );

        assertArrayEquals ( e0dig, e1dig );

        Iterator<String> i0 = e0.keySet().iterator();
        String tk0 = i0.next();
        Iterator<String> i1 = e1.keySet().iterator();
        String tk1 = i1.next();
        assertNotEquals ( tk0, tk1 );

        LinkedHashMap<String, Double> e2 = new LinkedHashMap<String, Double>();
        e2.put ( k2, v0 );
        e2.put ( k0, v2 );
        e2.put ( k1, v1 );
        byte[] e2dig = Utils.digDoubleMap ( null, e2 );
        assertFalse ( Arrays.equals ( e0dig, e2dig ) );
    }

}
