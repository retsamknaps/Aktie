package aktie.net;

import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class ReqHasFileProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private ConnectionThread connection;

    public ReqHasFileProcessor ( Index i, ConnectionThread c )
    {
        index = i;
        connection = c;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.CON_REQ_HASFILE.equals ( type ) )
        {
            String comid = b.getString ( CObj.COMMUNITYID );
            String creator = b.getString ( CObj.CREATOR );
            String conid = connection.getEndDestination().getId();

            if ( creator != null && comid != null && conid != null )
            {
                CObj sub = index.getSubscription ( comid, conid );

                if ( sub != null && "true".equals ( sub.getString ( CObj.SUBSCRIBED ) ) )
                {
                    //Get the member we're requesting posts from
                    long first = b.getNumber ( CObj.FIRSTNUM );
                    long last = b.getNumber ( CObj.LASTNUM );
                    long maxlast = first + 1000;
                    last = Math.min ( last, maxlast );
                    CObjList cl = index.getHasFiles ( comid, creator, first, last );

                    if ( cl.size() > 0 )
                    {
                        connection.enqueue ( cl );
                    }

                    else
                    {
                        cl.close();
                    }

                }

                else
                {
                    log.warning ( "Requested has file without subscription." );
                }

            }

            return true;
        }

        return false;
    }

}
