package aktie;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map.Entry;

import org.junit.Test;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class CheckIndex
{

    //@Test
    public void testIndex()
    {
        try
        {
            long maxval = 0;
            String maxvaluekey = "";
            long maxkeysize = 0;
            String maxkey = "";
            Index i = new Index();
            i.setIndexdir ( new File ( "XXXXXXXXX" ) );
            i.init();
            CObjList cl = i.getAllCObj();
            System.out.println ( "SIZE: " + cl.size() );

            for ( int c = 0; c < cl.size(); c++ )
            {
                CObj co = cl.get ( c );

                for ( Entry<String, String> e : co.getStrings().entrySet() )
                {
                    if ( e.getKey().length() > maxkeysize )
                    {
                        maxkeysize = e.getKey().length();
                        maxkey = e.getKey();
                    }

                    if ( e.getValue().length() > maxval )
                    {
                        maxval = e.getValue().length();
                        maxvaluekey = e.getKey();
                    }

                }

                if ( co.getText() != null )
                {
                    for ( Entry<String, String> e : co.getText().entrySet() )
                    {
                        if ( e.getKey().length() > maxkeysize )
                        {
                            maxkeysize = e.getKey().length();
                            maxkey = e.getKey();
                        }

                        if ( e.getValue().length() > maxval )
                        {
                            maxval = e.getValue().length();
                            maxvaluekey = e.getKey();
                        }

                    }

                }

            }

            i.close();
            System.out.println ( "MAX Value key: " + maxvaluekey + " value size: " + maxval );
            System.out.println ( "MAX Key: " + maxkey + " size: " + maxkeysize );
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail ( "nope" );
        }

    }

}
