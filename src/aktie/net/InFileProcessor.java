package aktie.net;

import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class InFileProcessor extends GenericProcessor
{
    Logger log = Logger.getLogger ( "aktie" );

    private ConnectionThread connection;

    public InFileProcessor ( ConnectionThread c )
    {
        connection = c;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.FRAGFAILED.equals ( type ) )
        {
            connection.decrFileRequest();

            log.info ( "FILE REQUEST FAILED ===============================" );
            log.info ( "COMMUNITY: " + b.getString ( CObj.COMMUNITYID ) );
            log.info ( "WHOLE DIG: " + b.getString ( CObj.FILEDIGEST ) );
            log.info ( "DIGOFDIGS: " + b.getString ( CObj.FRAGDIGEST ) ); //Digest of digests
            log.info ( "FRAGMENT:  " + b.getString ( CObj.FRAGDIG ) );

            return true;
        }

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

            return true;
        }

        return false;
    }


}
