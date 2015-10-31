package aktie.net;

import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class ReqFragListProcessor extends GenericProcessor
{
    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private ConnectionThread connection;

    public ReqFragListProcessor ( Index i, ConnectionThread c )
    {
        index = i;
        connection = c;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.CON_REQ_FRAGLIST.equals ( type ) )
        {
            String comid = b.getString ( CObj.COMMUNITYID );
            String wdig = b.getString ( CObj.FILEDIGEST );
            String pdig = b.getString ( CObj.FRAGDIGEST ); //Digest of digests
            String conid = connection.getEndDestination().getId();

            log.info ( "Requesting file fragment list. comid: " +
                       comid + " wdig " + wdig + " pdig " + pdig + " conid: " + conid );

            if ( comid != null && wdig != null && pdig != null && conid != null )
            {
                CObj sub = index.getSubscription ( comid, conid );

                log.info ( "Subscribed? " + sub );

                if ( sub != null && "true".equals ( sub.getString ( CObj.SUBSCRIBED ) ) )
                {
                    //Make sure someone has has the file in the context of the community
                    CObjList hfl = index.getHasFiles ( comid, wdig, pdig );
                    boolean getti = hfl.size() > 0;
                    hfl.close();
                    log.info ( "Yes, subscribed: has file: " + hfl.size() );

                    if ( getti )
                    {
                        CObjList frags = index.getFragments ( wdig, pdig );

                        log.info ( "Enqueue fragment list: " + frags.size() );

                        if ( frags.size() > 0 )
                        {
                            connection.enqueue ( frags );
                        }

                        else
                        {
                            frags.close();
                        }

                    }

                }

            }

            return true;
        }

        return false;
    }


}
