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
            String communityID = b.getString ( CObj.COMMUNITYID );
            String fileDigest = b.getString ( CObj.FILEDIGEST );
            String fragDigest = b.getString ( CObj.FRAGDIGEST ); //Digest of digests
            String memberID = connection.getEndDestination().getId();

            log.info ( "Requesting file fragment list. comid: " +
                       communityID + " wdig " + fileDigest + " pdig " + fragDigest + " conid: " + memberID );

            if ( communityID != null && fileDigest != null && fragDigest != null && memberID != null )
            {
                CObj subscription = index.getSubscription ( communityID, memberID );

                log.info ( "Subscribed? " + subscription );

                if ( subscription != null && CObj.TRUE.equals ( subscription.getString ( CObj.SUBSCRIBED ) ) )
                {
                    //Make sure someone has has the file in the context of the community
                    //We get the HasFile object to make sure we actually have the file.
                    CObj hasFile = index.getIdentHasFile ( communityID, //Community
                                                           connection.getLocalDestination().getIdentity().getId(), //My id
                                                           fileDigest, fragDigest );

                    if ( hasFile != null )
                    {
                        log.info ( "Yes, you have the file" );

                        CObjList frags = index.getFragments ( fileDigest, fragDigest );

                        log.info ( "Enqueue fragment list: " + frags.size() );

                        if ( frags.size() > 0 )
                        {
                            connection.enqueue ( frags );
                            String localFile = hasFile.getPrivate ( CObj.LOCALFILE );

                            if ( localFile != null )
                            {
                                connection.setFileUp ( localFile );
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


}
