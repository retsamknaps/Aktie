package aktie;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class DumpIndex
{

    public static void main ( String args[] )
    {
        if ( args.length < 1 )
        {
            System.out.println ( "Node dir must be specified." );
            System.exit ( 1 );
        }

        String nodedir = args[0];
        File idxdir = new File ( nodedir + File.separator + "index" );
        Index  index = new Index();
        index.setIndexdir ( idxdir );

        try
        {
            index.init();
        }

        catch ( IOException e1 )
        {
            e1.printStackTrace();
            System.exit ( 1 );
        }

        CObjList lst = index.getAllCObj();

        for ( int c = 0; c < lst.size(); c++ )
        {
            try
            {
                CObj co = lst.get ( c );
                JSONObject jo = co.GETPRIVATEJSON();
                System.out.println ( jo.toString ( 4 ) );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        lst.close();
    }

}
