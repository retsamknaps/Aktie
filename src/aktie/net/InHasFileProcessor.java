package aktie.net;

import java.util.logging.Level;
import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.Index;
import aktie.sequences.FileSequence;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;
import aktie.utils.DigestValidator;
import aktie.utils.HasFileCreator;
import aktie.utils.SubscriptionValidator;

public class InHasFileProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private ConnectionThread conThread;
    private Index index;
    private HH2Session session;
    private DigestValidator validator;
    private SubscriptionValidator subvalid;
    private CObj destIdent;
    private HasFileCreator hfc;
    private IdentityManager identManager;

    public InHasFileProcessor ( HH2Session s, Index i, IdentityManager im, HasFileCreator h, SpamTool st )
    {
        hfc = h;
        index = i;
        session = s;
        identManager = im;
        validator = new DigestValidator ( index, st );
        subvalid = new SubscriptionValidator ( index );
    }

    private void logIt ( String msg )
    {
        log.info ( "InHasFileProcessor ME: " + conThread.getLocalDestination().getIdentity().getId() +
                   " FROM: " + conThread.getEndDestination().getId() + " :: " + msg );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.HASFILE.equals ( type ) )
        {
            if ( validator.valid ( b ) )
            {
                boolean isnew = ( null == index.getByDig ( b.getDig() ) );
                Long seqnum = b.getNumber ( CObj.SEQNUM );
                String creatorid = b.getString ( CObj.CREATOR );
                String comid = b.getString ( CObj.COMMUNITYID );
                String wdig = b.getString ( CObj.FILEDIGEST );
                String ddig = b.getString ( CObj.FRAGDIGEST );

                if ( comid != null && creatorid != null && wdig != null && ddig != null && seqnum != null )
                {
                    String id = HasFileCreator.getCommunityMemberId ( creatorid, comid );

                    //Hasfileid is an upgrade.  We just set it here to what it is supposed to
                    //be.  If the signature does not match with the id value set.  DigestValidator
                    //has been upgraded to check the signature with a null id value for
                    //hasfile records. All new hasfile records should have the proper id value
                    //set so this does nothing.
                    String hasfileid = HasFileCreator.getHasFileId ( id, ddig, wdig );
                    b.setId ( hasfileid );

                    //TODO: Do we want to validate the id or not   if (hasfileid.equals())
                    //Nice if we could set it, but then when we pass on to others
                    //it won't validate properly.

                    CObj mysubid = subvalid.isMyUserSubscribed ( comid, destIdent.getId() );
                    //CObj sid = subvalid.isUserSubscribed ( comid, creatorid );
                    boolean cansub = subvalid.canSubscribe ( comid, creatorid );
                    boolean canhas = subvalid.canHasFile ( comid, creatorid, wdig, ddig );

                    if ( Level.INFO.equals ( log.getLevel() ) )
                    {
                        logIt ( "Mysub: " + mysubid + " cansub: " + cansub + " isnew: " + isnew + " creator: " +
                                creatorid + " comid: " + comid + " wdig: " + wdig + " canhas: " + canhas +
                                " seq: " + seqnum );
                    }

                    if ( mysubid != null && cansub && canhas )
                    {
                        try
                        {

                            FileSequence fseq = new FileSequence ( session );
                            fseq.setId ( id );
                            fseq.updateSequence ( b );

                            if ( isnew )
                            {
                                //Set the rank of the post based on the rank of the
                                //user
                                CObj idty = index.getIdentity ( creatorid );

                                if ( idty != null )
                                {
                                    Long rnk = idty.getPrivateNumber ( CObj.PRV_USER_RANK );

                                    if ( rnk != null )
                                    {
                                        b.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
                                    }

                                }

                                long seq = identManager.getGlobalSequenceNumber ( destIdent.getId(), false );
                                b.pushPrivateNumber ( CObj.getGlobalSeq ( destIdent.getId() ), seq );

                                index.index ( b );
                                hfc.updateFileInfo ( b );
                                conThread.update ( b );
                            }

                        }

                        catch ( Exception e )
                        {
                            e.printStackTrace();

                        }

                    }

                }

            }

        }

        return false;
    }

    @Override
    public void setContext ( Object c )
    {
        conThread = ( ConnectionThread ) c;
        destIdent = conThread.getLocalDestination().getIdentity();
    }

}
