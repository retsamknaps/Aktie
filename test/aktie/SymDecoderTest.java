package aktie;

import static org.junit.Assert.*;

import org.junit.Test;

import aktie.data.CObj;
import aktie.utils.SymDecoder;

public class SymDecoderTest
{

    @Test
    public void testit()
    {
        String subject = "This is a multi-line, subject"
                         + "with commons,\n and new lines! whoa\n!";
        String body = "Test this:\n\n\n" +
                      "And, This!:  Does it work\n?";
        StringBuilder sb = SymDecoder.encodeText ( null, "SUBJECT", subject );
        SymDecoder.encodeText ( sb, "BODY", body );

        System.out.println ( "============================================" );
        System.out.println ( sb.toString() );
        System.out.println ( "============================================" );

        CObj o = new CObj();
        SymDecoder.decodeText ( o, sb.toString(), null );
        String tsub = o.getPrivate ( "SUBJECT" );
        String tbod = o.getPrivate ( "BODY" );

        System.out.println ( "============================================" );
        System.out.println ( tsub );
        System.out.println ( "============================================" );

        System.out.println ( "============================================" );
        System.out.println ( tbod );
        System.out.println ( "============================================" );

        assertEquals ( subject, tsub );
        assertEquals ( body, tbod );
    }

}
