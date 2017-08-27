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

    public ReqFragListProcessor ( Index i )
    {
        index = i;
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
                    //We get the HasFile object to make sure we actually have the file.
                    CObj hf = index.getIdentHasFile ( comid, //Community
                                                      connection.getLocalDestination().getIdentity().getId(), //My id
                                                      wdig, pdig );

                    if ( hf != null )
                    {
                        log.info ( "Yes, you have the file" );

                        CObjList frags = index.getFragments ( wdig, pdig );

                        log.info ( "Enqueue fragment list: " + frags.size() );

                        if ( frags.size() > 0 )
                        {
                            connection.enqueue ( frags );
                            String lf = hf.getPrivate ( CObj.LOCALFILE );

                            if ( lf != null )
                            {
                                connection.setFileUp ( lf );
                            }

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

    @Override
    public void setContext ( Object c )
    {
        connection = ( ConnectionThread ) c;

    }


}
