package aktie;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import aktie.json.CleanParser;

public class CleanJSONTest
{

    @Test
    public void testJSON()
    {
        JSONObject j = new JSONObject();
        j.put ( "bo\"olval", true );
        j.put ( "double'", 0.5D );
        j.put ( "{", 23 );
        j.put ( "asdf}}}'", "}}Another String\"'" );

        JSONObject j2 = new JSONObject();
        j2.put ( "bo\"olval", false );
        j2.put ( "double'", 0.6D );
        j2.put ( "{", 27 );
        j2.put ( "23452", new Object[] {"$%#}}", 234, 0.6} );

        j2.put ( "asdf}}}'", "}}Another String\"'globgs" );

        StringBuilder sb = new StringBuilder();
        sb.append ( j.toString() );
        sb.append ( j2.toString() );

        String os = sb.toString();
        System.out.println ( "------------------------------------------" );
        System.out.println ( os );
        System.out.println ( "------------------------------------------" );

        byte lb[] = os.getBytes();

        ByteArrayInputStream ba = new ByteArrayInputStream ( lb );

        CleanParser p = new CleanParser ( ba );

        try
        {
            {
                JSONObject k = p.next();
                Iterator<String> i = j.keys();

                while ( i.hasNext() )
                {
                    String y = i.next();
                    Object ob = k.get ( y );
                    System.out.println ( "CLS: " + ob.getClass().getName() );

                    if ( ob instanceof JSONArray )
                    {
                        JSONArray ja = ( JSONArray ) ob;
                        Object[] ta = ( Object[] ) j.get ( y );
                        assertEquals ( ja.length(), ta.length );

                        for ( int c = 0; c < ja.length(); c++ )
                        {
                            assertEquals ( ja.get ( c ), ta[c] );
                        }

                    }

                    else
                    {
                        System.out.println ( "y: " + y );
                        assertEquals ( ob, j.get ( y ) );
                    }

                }

            }

            {
                JSONObject k = p.next();
                Iterator<String> i = j2.keys();

                while ( i.hasNext() )
                {
                    String y = i.next();
                    Object ob = k.get ( y );
                    System.out.println ( "CLS: " + ob.getClass().getName() );

                    if ( ob instanceof JSONArray )
                    {
                        JSONArray ja = ( JSONArray ) ob;
                        Object[] ta = ( Object[] ) j2.get ( y );
                        assertEquals ( ja.length(), ta.length );

                        for ( int c = 0; c < ja.length(); c++ )
                        {
                            assertEquals ( ja.get ( c ), ta[c] );
                        }

                    }

                    else
                    {
                        System.out.println ( "y: " + y );
                        assertEquals ( ob, j2.get ( y ) );
                    }

                }

            }

        }

        catch ( IOException e )
        {
            e.printStackTrace();
            fail ( "Blah" );
        }

    }

}
