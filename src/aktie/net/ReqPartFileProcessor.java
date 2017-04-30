package aktie.net;

import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

// HasPart
/**
    Processor to request part file information

*/
public class ReqPartFileProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private ConnectionThread connection;

    public ReqPartFileProcessor ( Index i, ConnectionThread c )
    {
        index = i;
        connection = c;
    }

    @Override
    public boolean process ( CObj co )
    {
        String type = co.getType();

        if ( CObj.CON_REQ_HASPART.equals ( type ) )
        {
            String creatorID = co.getString ( CObj.CREATOR );

            // TODO: HasPart -> adapt copy-paste from ReqHasFileProcessor

            String remoteDestinationID = connection.getEndDestination().getId();

            if ( creatorID != null && remoteDestinationID != null )
            {
                //Get the member we're requesting posts from
                long first = co.getNumber ( CObj.FIRSTNUM );
                long last = co.getNumber ( CObj.LASTNUM );
                long maxlast = first + 1000;
                last = Math.min ( last, maxlast );
                CObjList partFiles = index.getPartFiles ( creatorID, first, last );

                if ( partFiles.size() > 0 )
                {
                    connection.enqueue ( partFiles );
                }

                else
                {
                    partFiles.close();
                }

            }

            return true;
        }

        return false;
    }

}
