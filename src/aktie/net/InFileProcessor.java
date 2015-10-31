package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class InFileProcessor extends GenericProcessor
{

    private ConnectionThread connection;

    public InFileProcessor ( ConnectionThread c )
    {
        connection = c;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.FILEF.equals ( type ) )
        {
            Long len = b.getNumber ( CObj.FRAGSIZE );

            if ( len != null )
            {
                long ll = len;

                if ( ll > ConnectionThread.LONGESTLIST ) { connection.stop(); }

                connection.setLength ( ( int ) ll );
                connection.setLoadFile ( true );
            }

        }

        return false;
    }


}
