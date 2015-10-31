package aktie.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aktie.crypto.Utils;
import aktie.data.CObj;

import org.bouncycastle.crypto.params.KeyParameter;

public class SymDecoder
{

    public boolean decode ( CObj b, KeyParameter kp )
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

    public void decodeText ( CObj b, String pl0, String pl1 )
    {
        if ( pl0 != null )
        {
            String prts[] =  pl0.split ( "," );
            Matcher m = Pattern.compile ( "(\\S+)=(.+)" ).matcher ( "" );

            for ( int c = 0; c < prts.length; c++ )
            {
                m.reset ( prts[c] );

                if ( m.find() )
                {
                    String k = m.group ( 1 );
                    String v = m.group ( 2 );
                    b.pushPrivate ( k, v );
                }

            }

        }

        if ( pl1 != null )
        {
            String prts[] =  pl1.split ( "," );
            Matcher m = Pattern.compile ( "(\\S+)=(.+)" ).matcher ( "" );

            for ( int c = 0; c < prts.length; c++ )
            {
                m.reset ( prts[c] );

                if ( m.find() )
                {
                    String k = m.group ( 1 );

                    try
                    {
                        Long v = Long.valueOf ( m.group ( 2 ) );
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
