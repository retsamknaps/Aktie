package aktie.net;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.sequences.PartSequence;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;
import aktie.utils.DigestValidator;
import aktie.utils.HasFileCreator;

public class InHasPartProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private ConnectionThread connectionThread;
    private Index index;
    private HH2Session session;
    private DigestValidator validator;
    private CObj destinationIdentity;
    private IdentityManager identManager;

    public InHasPartProcessor ( CObj id, HH2Session s, Index i, IdentityManager im, ConnectionThread cb, SpamTool st )
    {
        destinationIdentity = id;
        index = i;
        session = s;
        connectionThread = cb;
        identManager = im;
        validator = new DigestValidator ( index, st );
    }

    private void logIt ( String msg )
    {
        log.info ( "InHasPartProcessor ME: " + connectionThread.getLocalDestination().getIdentity().getId() +
                   " FROM: " + connectionThread.getEndDestination().getId() + " :: " + msg );
    }

    @Override
    public boolean process ( CObj co )
    {
        String type = co.getType();

        if ( CObj.HASPART.equals ( type ) )
        {
            if ( Level.INFO.equals ( log.getLevel() ) )
            {
                logIt ( "New HasPart: " + co.getDig() );
            }

            if ( !validator.valid ( co ) )
            {
                return true;
            }

            Long sequenceNumber = co.getNumber ( CObj.SEQNUM );
            String creatorID = co.getString ( CObj.CREATOR );
            String communityID = co.getString ( CObj.COMMUNITYID );
            String fileDigest = co.getString ( CObj.FILEDIGEST );
            String fragDigest = co.getString ( CObj.FRAGDIGEST );
            Long totalFragments = co.getNumber ( CObj.FRAGNUMBER );
            Long createdOn = co.getNumber ( CObj.CREATEDON );
            String completedFragmentsPayload = co.getString ( CObj.PAYLOAD );

            if ( creatorID == null || communityID == null || fileDigest == null || fragDigest == null || totalFragments == null || createdOn == null || completedFragmentsPayload == null )
            {
                return true;
            }

            if ( Level.INFO.equals ( log.getLevel() ) )
            {
                logIt ( "IS VALID: creator: " +
                        creatorID + " comid: " + communityID + " wdig: " + fileDigest + " seq: " + sequenceNumber );
            }

            long latestAcceptedCreationDate = System.currentTimeMillis() - Index.HAS_PART_MAX_AGE;

            if ( createdOn.longValue() < latestAcceptedCreationDate )
            {
                // Do not process information that is too old.
                return true;
            }

            // Do not process has part information if we do not know at least one has file for this part file.
            CObjList knownHasFiles = index.getHasFiles ( communityID, fileDigest, fragDigest );

            if ( knownHasFiles.size() < 1 )
            {
                knownHasFiles.close();
                return true;
            }

            knownHasFiles.close();

            // Avoid further processing of HasPart that belong to one of our own identities.
            CObjList myIdentities = index.getMyIdentities();

            for ( int i = 0; i < myIdentities.size(); i++ )
            {
                try
                {
                    CObj identity = myIdentities.get ( i );

                    String id = identity.getId();

                    if ( id != null && id.equals ( creatorID ) )
                    {
                        myIdentities.close();
                        return true;
                    }

                }

                catch ( IOException e )
                {
                    myIdentities.close();
                    return false;
                }

            }

            myIdentities.close();

            long now = System.currentTimeMillis();
            long firstSeen = now;
            long lastCreatedOn = 0L;
            long revision = 1L;

            CObj existingHasPart = index.getPartFile ( creatorID, communityID, fileDigest, fragDigest );

            // Should have delivered us a list with a maximum of one object, but who knows for sure.
            // If we already know a part file with the same digests from this creator, find out when it was created
            if ( existingHasPart != null )
            {

                Long compareCreatedOn = existingHasPart.getNumber ( CObj.CREATEDON );

                if ( createdOn != null )
                {
                    lastCreatedOn = Math.max ( lastCreatedOn, compareCreatedOn );
                }

                Long compareFirstSeen = existingHasPart.getPrivateNumber ( CObj.CREATEDON );

                if ( compareFirstSeen != null )
                {
                    firstSeen = Math.min ( firstSeen, compareFirstSeen );
                }

                Long compareRevision = existingHasPart.getPrivateNumber ( CObj.REVISION );

                if ( compareRevision != null )
                {
                    revision = compareRevision + 1;
                }

            }

            // If if the part file we are processing is not newer than the already known part file, do not proceed.
            if ( createdOn.longValue() <= lastCreatedOn )
            {
                return true;
            }

            co.pushPrivateNumber ( CObj.FIRSTSEEN,  firstSeen );
            co.pushPrivateNumber ( CObj.LASTSEEN, now );
            co.pushPrivateNumber ( CObj.REVISION, revision );

            String id = HasFileCreator.getCommunityMemberId ( creatorID, communityID );
            String partFileID = HasFileCreator.getHasFileId ( id, fragDigest, fileDigest );
            co.setId ( partFileID );

            PartSequence partSequence = new PartSequence ( session );
            partSequence.setId ( creatorID );

            try
            {
                partSequence.updateSequence ( co );
            }

            catch ( Exception e )
            {
                return false;
            }

            // If the part file is new (revision 1)
            if ( revision == 1L )
            {
                //Set the rank of the post based on the rank of the user
                CObj idendity = index.getIdentity ( creatorID );

                if ( idendity != null )
                {
                    Long rank = idendity.getPrivateNumber ( CObj.PRV_USER_RANK );

                    if ( rank != null )
                    {
                        co.pushPrivateNumber ( CObj.PRV_USER_RANK, rank );
                    }

                }

                long localSequence = identManager.getGlobalSequenceNumber ( destinationIdentity.getId(), false );
                co.pushPrivateNumber ( CObj.getGlobalSeq ( destinationIdentity.getId() ), localSequence );

                try
                {
                    index.index ( co );
                }

                catch ( IOException e )
                {
                    return true;
                }

                connectionThread.update ( co );
            }

            return true;
        }

        return false;
    }

}
