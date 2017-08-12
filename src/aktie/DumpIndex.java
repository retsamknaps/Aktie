package aktie;

import java.io.File;
import java.io.IOException;

import aktie.index.DumpIndexUtil;
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

        DumpIndexUtil.dumpIndex ( index );
        index.close();

    }

}
