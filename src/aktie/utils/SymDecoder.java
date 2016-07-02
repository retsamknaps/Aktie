package aktie.utils;

import aktie.crypto.Utils;
import aktie.data.CObj;

import org.bouncycastle.crypto.params.KeyParameter;

public class SymDecoder
{

    public static String SPLITTER = "~%@ASeP&_~";

    public static StringBuilder encodeText ( StringBuilder sb, String k, String v )
    {
        if ( sb == null )
        {
            sb = new StringBuilder();
        }

        if ( k.contains ( "=" ) )
        {
            throw new RuntimeException ( "= may not be in key!" );
        }

        sb.append ( SPLITTER );
        sb.append ( k );
        sb.append ( "=" );
        sb.append ( v );
        return sb;
    }

    public static boolean decode ( CObj b, KeyParameter kp )
    {
        String pl = b.getString ( CObj.PAYLOAD );
        String pl2 = b.getString ( CObj.PAYLOAD2 );

        if ( pl != null || pl2 != null )
        {
            String pl0 = null;
            String pl1 = null;

            if ( pl != null )
            {
                byte pb[] = Utils.toByteArray ( pl );
                byte db[] = Utils.attemptSymDecode ( kp, Utils.CID0, Utils.CID1, pb );

                if ( db != null )
                {
                    pl0 = Utils.toStringRaw ( db );
                }

            }

            if ( pl2 != null )
            {
                byte pb2[] = Utils.toByteArray ( pl2 );
                byte db2[] = Utils.attemptSymDecode ( kp, Utils.CID0, Utils.CID1, pb2 );

                if ( db2 != null )
                {
                    pl1 = Utils.toStringRaw ( db2 );
                }

            }

            decodeText ( b, pl0, pl1 );

            if ( pl0 != null || pl1 != null )
            {
                return true;
            }

        }

        return false;
    }

    public static void decodeText ( CObj b, String pl0, String pl1 )
    {
        if ( pl0 != null )
        {
            String splitwith = ",";

            if ( pl0.contains ( SPLITTER ) )
            {
                splitwith = SPLITTER;
            }

            String prts[] =  pl0.split ( splitwith );

            for ( int c = 0; c < prts.length; c++ )
            {
                int eat = prts[c].indexOf ( "=" );

                if ( eat > 0 )
                {
                    String k = prts[c].substring ( 0, eat );
                    String v = prts[c].substring ( eat + 1 );
                    b.pushPrivate ( k, v );
                }

            }

        }

        if ( pl1 != null )
        {
            String splitwith = ",";

            if ( pl0.contains ( SPLITTER ) )
            {
                splitwith = SPLITTER;
            }

            String prts[] =  pl1.split ( splitwith );

            for ( int c = 0; c < prts.length; c++ )
            {

                int eat = prts[c].indexOf ( "=" );

                if ( eat > 0 )
                {
                    String k = prts[c].substring ( 0, eat );
                    String vs = prts[c].substring ( eat + 1 );

                    try
                    {
                        Long v = Long.valueOf ( vs );
                        b.pushPrivateNumber ( k, v );
                    }

                    catch ( Exception e )
                    {
                    }

                }

            }

        }

    }

}
