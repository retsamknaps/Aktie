package aktie.net;

import java.io.File;
import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.Index;

public class ReqFragProcessor extends GenericProcessor
{
    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private ConnectionThread connection;

    public ReqFragProcessor ( Index i )
    {
        index = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.CON_REQ_FRAG.equals ( type ) )
        {
            boolean fragfnd = false;

            String comid = b.getString ( CObj.COMMUNITYID );
            String wdig = b.getString ( CObj.FILEDIGEST );
            String pdig = b.getString ( CObj.FRAGDIGEST ); //Digest of digests
            String fdig = b.getString ( CObj.FRAGDIG );
            String conid = connection.getEndDestination().getId();

            log.info ( "REQFRAG: REQ_FRAG: comid: " + comid + " wdig: " + wdig + " from id: " + conid );

            if ( comid != null && wdig != null && pdig != null && conid != null )
            {
                CObj sub = index.getSubscription ( comid, conid );

                log.info ( "REQFRAG: sub: " + sub );

                if ( sub != null && "true".equals ( sub.getString ( CObj.SUBSCRIBED ) ) )
                {
                    //Make sure someone has has the file in the context of the community
                    //We get the HasFile object to make sure we actually have the file.
                    CObj hf = index.getIdentHasFile ( comid, //Community
                                                      connection.getLocalDestination().getIdentity().getId(), //My id
                                                      wdig, pdig );

                    //String wdig, String ddig, String dig
                    CObj fg = index.getFragment ( comid, wdig, pdig, fdig );

                    log.info ( "REQFRAG: fg: " + fg + " hf: " + hf );

                    if ( hf != null && fg != null )
                    {
                        String lfs = fg.getPrivate ( CObj.LOCALFILE );
                        Long offset = fg.getNumber ( CObj.FRAGOFFSET );
                        Long len = fg.getNumber ( CObj.FRAGSIZE );

                        if ( lfs != null && offset != null && len != null )
                        {
                            File tf = new File ( lfs );

                            if ( tf.exists() && tf.canRead() &&
                                    tf.length() >= ( offset + len ) )
                            {
                                log.info ( "REQFRAG: send to connection: " + tf +
                                           " offset: " + offset + " len: " + len );
                                fg.setType ( CObj.FILEF ); //Change the type to indicate we're
                                //actually sending over the fragment.
                                connection.enqueue ( fg );
                                connection.setFileUp ( lfs );
                                fragfnd = true;
                            }

                        }

                    }

                }

            }

            if ( !fragfnd )
            {
                CObj nf = b.clone();
                nf.setType ( CObj.FRAGFAILED );
                connection.enqueue ( nf );
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
