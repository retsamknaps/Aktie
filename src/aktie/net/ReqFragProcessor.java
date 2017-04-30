package aktie.net;

import java.io.File;
import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.Index;

/**
    Process fragment requests from  a remote destination.

*/
public class ReqFragProcessor extends GenericProcessor
{
    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private ConnectionThread connection;

    public ReqFragProcessor ( Index i, ConnectionThread c )
    {
        index = i;
        connection = c;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.CON_REQ_FRAG.equals ( type ) )
        {
            boolean fragmentFound = false;

            String communityID = b.getString ( CObj.COMMUNITYID );
            String fileDigest = b.getString ( CObj.FILEDIGEST );
            String fragDigest = b.getString ( CObj.FRAGDIGEST ); //Digest of digests
            String fragDig = b.getString ( CObj.FRAGDIG );
            String conid = connection.getEndDestination().getId();

            log.info ( "REQFRAG: REQ_FRAG: comid: " + communityID + " wdig: " + fileDigest + " from id: " + conid );

            if ( communityID != null && fileDigest != null && fragDigest != null && conid != null )
            {
                CObj sub = index.getSubscription ( communityID, conid );

                log.info ( "REQFRAG: sub: " + sub );

                // TODO: Why check if the identity is subscribed to the community? Why not just deliver the fragment if asked for it?
                if ( sub != null && CObj.TRUE.equals ( sub.getString ( CObj.SUBSCRIBED ) ) )
                {
                    //Make sure someone has has the file in the context of the community
                    //We get the HasFile object to make sure we actually have the file.
                    CObj hasFile = index.getIdentHasFile ( communityID, //Community
                                                           connection.getLocalDestination().getIdentity().getId(), //My id
                                                           fileDigest, fragDigest );

                    //String wdig, String ddig, String dig
                    CObj fragment = index.getFragment ( communityID, fileDigest, fragDigest, fragDig );

                    log.info ( "REQFRAG: fg: " + fragment + " hf: " + hasFile );

                    if ( hasFile != null && fragment != null )
                    {
                        fragmentFound = findFragment ( fragment );

                    }

                    // HasPart
                    // Check if our identity can deliver a requested fragment from a part file
                    if ( !fragmentFound )
                    {
                        String myID = connection.getLocalDestination().getIdentity().getId();
                        CObj partFile = index.getPartFile ( myID, communityID, fileDigest, fragDigest );

                        log.info ( "REQFRAG: fg: " + fragment + " pf: " + partFile );

                        if ( partFile != null && fragment != null )
                        {
                            fragmentFound = findFragment ( fragment );
                        }

                    }

                }

            }

            if ( !fragmentFound )
            {
                CObj nf = b.clone();
                nf.setType ( CObj.FRAGFAILED );
                connection.enqueue ( nf );
            }

            return true;
        }

        return false;
    }

    private boolean findFragment ( CObj fragment )
    {
        String localFile = fragment.getPrivate ( CObj.LOCALFILE );
        Long fragOffset = fragment.getNumber ( CObj.FRAGOFFSET );
        Long fragSize = fragment.getNumber ( CObj.FRAGSIZE );

        if ( localFile != null && fragOffset != null && fragSize != null )
        {
            File file = new File ( localFile );

            if ( file.exists() && file.canRead() && file.length() >= ( fragOffset + fragSize ) )
            {
                // Change the type to indicate we're actually sending over the fragment.
                fragment.setType ( CObj.FILEF );

                connection.enqueue ( fragment );
                connection.setFileUp ( localFile );
                return true;
            }

        }

        return false;
    }

}
