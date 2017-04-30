package aktie.net;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.CObjList;
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

    public InHasFileProcessor ( CObj id, HH2Session s, Index i, IdentityManager im, ConnectionThread cb, HasFileCreator h, SpamTool st )
    {
        hfc = h;
        destIdent = id;
        index = i;
        session = s;
        conThread = cb;
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
            if ( Level.INFO.equals ( log.getLevel() ) )
            {
                logIt ( "New HasFile: " + b.getDig() );
            }

            if ( validator.valid ( b ) )
            {
                boolean isNew = ( null == index.getByDig ( b.getDig() ) );

                Long seqNum = b.getNumber ( CObj.SEQNUM );
                String creatorID = b.getString ( CObj.CREATOR );
                String communityID = b.getString ( CObj.COMMUNITYID );
                String fileDigest = b.getString ( CObj.FILEDIGEST );
                String fragDigest = b.getString ( CObj.FRAGDIGEST );

                if ( Level.INFO.equals ( log.getLevel() ) )
                {
                    logIt ( "IS VALID: isnew: " + isNew + " creator: " +
                            creatorID + " comid: " + communityID + " wdig: " + fileDigest + " seq: " + seqNum );
                }

                if ( communityID != null && creatorID != null && fileDigest != null && fragDigest != null && seqNum != null )
                {
                    String id = HasFileCreator.getCommunityMemberId ( creatorID, communityID );

                    //Hasfileid is an upgrade.  We just set it here to what it is supposed to
                    //be.  If the signature does not match with the id value set.  DigestValidator
                    //has been upgraded to check the signature with a null id value for
                    //hasfile records. All new hasfile records should have the proper id value
                    //set so this does nothing.
                    String hasfileid = HasFileCreator.getHasFileId ( id, fragDigest, fileDigest );
                    b.setId ( hasfileid );

                    //TODO: Do we want to validate the id or not   if (hasfileid.equals())
                    //Nice if we could set it, but then when we pass on to others
                    //it won't validate properly.

                    CObj mysubid = subvalid.isMyUserSubscribed ( communityID, destIdent.getId() );
                    CObj sid = subvalid.isUserSubscribed ( communityID, creatorID );

                    if ( Level.INFO.equals ( log.getLevel() ) )
                    {
                        logIt ( "Mysub: " + mysubid + " sid: " + sid + " isnew: " + isNew + " creator: " +
                                creatorID + " comid: " + communityID + " wdig: " + fileDigest + " seq: " + seqNum );
                    }

                    if ( mysubid != null && sid != null )
                    {
                        try
                        {

                            FileSequence fileSequence = new FileSequence ( session );
                            fileSequence.setId ( id );
                            fileSequence.updateSequence ( b );

                            if ( isNew )
                            {
                                //Set the rank of the post based on the rank of the
                                //user
                                CObj idty = index.getIdentity ( creatorID );

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

                                // HasPart
                                // If the has file is new, remove any part file information associated with it.
                                // The other identity now holds the complete file and hence does not serve the part file anymore.

                                CObjList partFiles = index.getPartFiles ( creatorID, fileDigest, fragDigest );

                                try
                                {
                                    // Remove a corresponding part files from index as this file is now complete
                                    index.delete ( partFiles );
                                }

                                catch ( IOException e )
                                {
                                    partFiles.close();
                                    throw e;
                                }

                                partFiles.close();

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

            return true;
        }

        return false;
    }

}
