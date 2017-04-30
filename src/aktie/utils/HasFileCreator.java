package aktie.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortedNumericSortField;
import org.hibernate.Session;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.HH2Session;
import aktie.data.RequestFile;
import aktie.gui.UpdateInterface;
import aktie.gui.Wrapper;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;

public class HasFileCreator
{
    Logger log = Logger.getLogger ( "aktie" );

    private HH2Session hh2Session;
    private Index index;
    private SubscriptionValidator validator;
    private SpamTool spamtool;
    private IdentityManager identManager;

    public HasFileCreator ( HH2Session s, Index i, SpamTool st )
    {
        index = i;
        hh2Session = s;
        spamtool = st;
        identManager = new IdentityManager ( s, i );
        validator = new SubscriptionValidator ( index );
    }

    public void updateDownloadRequested ( CObj hasfile )
    {

    }

    /**
        Called whenever we get a hasfile record
        @param f
    */
    public void updateFileInfo ( CObj f )
    {
        if ( !CObj.HASFILE.equals ( f.getType() ) )
        {
            throw new RuntimeException ( "This should only be called with hasfile." );
        }

        //This makes the hasfile immediately available
        //so that it is counted correctly for updating
        //the file info
        index.forceNewSearcher();

        //Create FILE type CObj.  only index if new
        String comid = f.getString ( CObj.COMMUNITYID );
        Long filesize = f.getNumber ( CObj.FILESIZE );
        Long fragsize = f.getNumber ( CObj.FRAGSIZE );
        Long fragnumber = f.getNumber ( CObj.FRAGNUMBER );
        String digofdigs = f.getString ( CObj.FRAGDIGEST );
        String wholedig = f.getString ( CObj.FILEDIGEST );
        String name = f.getString ( CObj.NAME );
        String localfile = f.getPrivate ( CObj.LOCALFILE );
        String txtname = f.getString ( CObj.TXTNAME );
        String stillhas = f.getString ( CObj.STILLHASFILE );
        String share = f.getString ( CObj.SHARE_NAME );

        if ( txtname == null )
        {
            txtname = name;
        }

        if ( digofdigs != null && wholedig != null && name != null && comid != null && filesize != null &&
                fragsize != null && fragnumber != null )
        {

            long latest = 0;
            long earliest = 0;
            Sort s = new Sort();
            s.setSort ( new SortedNumericSortField ( CObj.docNumber ( CObj.CREATEDON ), SortedNumericSortField.Type.LONG, true ) );
            CObjList wl = index.getHasFiles ( comid, wholedig, digofdigs, s );
            int numberhasfile = wl.size();

            if ( numberhasfile > 0 )
            {
                try
                {
                    CObj lat = wl.get ( 0 );
                    latest = lat.getNumber ( CObj.CREATEDON );
                    CObj ear = wl.get ( numberhasfile - 1 );
                    earliest = ear.getNumber ( CObj.CREATEDON );
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            wl.close();

            String id = Utils.mergeIds ( comid, digofdigs, wholedig );
            CObj fi = index.getFileInfo ( id );

            if ( fi == null )
            {
                fi = new CObj();
                fi.setId ( id );
                fi.setType ( CObj.FILE );
            }

            fi.pushNumber ( CObj.LASTUPDATE, latest );
            fi.pushNumber ( CObj.CREATEDON, earliest );
            fi.pushNumber ( CObj.NUMBER_HAS, numberhasfile );
            String creatorid = f.getString ( CObj.CREATOR );

            if ( creatorid != null )
            {
                CObj creator = index.getIdentity ( creatorid );

                if ( creator != null )
                {
                    Long rnk = creator.getPrivateNumber ( CObj.PRV_USER_RANK );

                    if ( rnk != null )
                    {
                        Long crnk = fi.getPrivateNumber ( CObj.PRV_USER_RANK );

                        if ( crnk == null || crnk < rnk )
                        {
                            fi.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
                        }

                    }

                }

            }

            try
            {
                if ( localfile != null )
                {
                    if ( "false".equals ( stillhas ) )
                    {
                        fi.pushString ( CObj.STATUS, "" );
                        localfile = "";
                    }

                    else
                    {
                        fi.pushString ( CObj.STATUS, "done" );
                    }

                    fi.pushString ( CObj.LOCALFILE, localfile );
                    fi.pushString ( CObj.SHARE_NAME, share );
                }

                fi.pushString ( CObj.COMMUNITYID, comid );
                fi.pushString ( CObj.FILEDIGEST, wholedig );
                fi.pushString ( CObj.FRAGDIGEST, digofdigs );
                fi.pushNumber ( CObj.FILESIZE, filesize );
                fi.pushNumber ( CObj.FRAGSIZE, fragsize );
                fi.pushNumber ( CObj.FRAGNUMBER, fragnumber );
                fi.pushString ( CObj.NAME, name );
                fi.pushText ( CObj.TXTNAME, txtname );

                index.index ( fi );

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }


        }

    }

    public static String getCommunityMemberId ( String creator, String comid )
    {
        return Utils.mergeIds ( creator, comid );
    }

    public static String getHasFileId ( String commemid, String digofdigs, String wholedig )
    {
        String hasfileid = Utils.mergeIds ( commemid, digofdigs, wholedig );
        return hasfileid;
    }

    public static String getHasFileId ( String creator, String comid, String digofdigs, String wholedig )
    {
        String id = getCommunityMemberId ( creator, comid );
        return getHasFileId ( id, digofdigs, wholedig );
    }

    public void updateHasFile()
    {
        updateHasFile ( null );
    }

    public void updateHasFile ( UpdateInterface up )
    {
        CObjList hflst = index.getAllHasFiles();

        for ( int c = 0; c < hflst.size(); c++ )
        {
            try
            {
                if ( c % 10 == 0 && up != null )
                {
                    long percent = ( c * 100 ) / hflst.size();
                    StringBuilder sb = new StringBuilder();
                    sb.append ( "Update to " );
                    sb.append ( Wrapper.VERSION );
                    sb.append ( "    " );
                    sb.append ( percent );
                    sb.append ( "% complete." );
                    up.updateStatus ( sb.toString() );
                }

                CObj b = hflst.get ( c );

                String creatorid = b.getString ( CObj.CREATOR );
                String comid = b.getString ( CObj.COMMUNITYID );
                String wdig = b.getString ( CObj.FILEDIGEST );
                String ddig = b.getString ( CObj.FRAGDIGEST );

                if ( creatorid != null && comid != null &&
                        wdig != null && ddig != null )
                {

                    String id = HasFileCreator.getCommunityMemberId ( creatorid, comid );

                    //Hasfileid is an upgrade.  We just set it here to what it is supposed to
                    //be.  If the signature does not match with the id value set.  DigestValidator
                    //has been upgraded to check the signature with a null id value for
                    //hasfile records. All new hasfile records should have the proper id value
                    //set so this does nothing.
                    String hasfileid = HasFileCreator.getHasFileId ( id, ddig, wdig );

                    index.delete ( b );
                    b.setId ( hasfileid );
                    index.index ( b );
                    updateFileInfo ( b );
                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        hflst.close();

    }

    public void updateOnlyHasFile ( UpdateInterface up )
    {
        Set<String> doneset = new HashSet<String>();
        CObjList hflst = index.getAllHasFiles();

        for ( int c = 0; c < hflst.size(); c++ )
        {
            try
            {
                if ( c % 10 == 0 && up != null )
                {
                    long percent = ( c * 100 ) / hflst.size();
                    StringBuilder sb = new StringBuilder();
                    sb.append ( "Update to " );
                    sb.append ( Wrapper.VERSION );
                    sb.append ( "    " );
                    sb.append ( percent );
                    sb.append ( "% complete." );
                    up.updateStatus ( sb.toString() );
                }

                CObj b = hflst.get ( c );

                String creatorid = b.getString ( CObj.CREATOR );
                String comid = b.getString ( CObj.COMMUNITYID );
                String wdig = b.getString ( CObj.FILEDIGEST );
                String ddig = b.getString ( CObj.FRAGDIGEST );

                if ( creatorid != null && comid != null &&
                        wdig != null && ddig != null && !doneset.contains ( ddig ) )
                {
                    doneset.add ( ddig );
                    updateFileInfo ( b );
                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        hflst.close();

    }

    public boolean createHasFile ( CObj hasFile )
    {
        //Set File sequence number for the community/creator
        String creatorID = hasFile.getString ( CObj.CREATOR );
        String communityID = hasFile.getString ( CObj.COMMUNITYID );

        String fragDigest = hasFile.getString ( CObj.FRAGDIGEST );
        String fileDigest = hasFile.getString ( CObj.FILEDIGEST );

        CObj myid = validator.isMyUserSubscribed ( communityID, creatorID );

        if ( myid == null ) { return false; }

        String id = getCommunityMemberId ( creatorID, communityID );
        String hasfileid = getHasFileId ( id, fragDigest, fileDigest );

        hasFile.setId ( hasfileid ); //only 1 has file per user per community per file digest
        //This is an upgrade.  We have to make adjustments for this
        //to be null when validating signatures for old hasfile records

        Session session = null;
        long lastSeqNumber = 0L;

        try
        {
            session = hh2Session.getSession();
            session.getTransaction().begin();
            CommunityMember member = ( CommunityMember ) session.get ( CommunityMember.class, id );

            if ( member == null )
            {
                member = new CommunityMember();
                member.setId ( id );
                member.setCommunityId ( communityID );
                member.setMemberId ( creatorID );
                session.persist ( member );
            }

            long seqNumber = member.getLastFileNumber();
            lastSeqNumber = seqNumber;
            seqNumber++;
            hasFile.pushNumber ( CObj.SEQNUM, seqNumber );
            hasFile.pushPrivate ( CObj.MINE, CObj.TRUE );
            member.setLastFileNumber ( seqNumber );
            session.merge ( member );
            session.getTransaction().commit();
            session.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            hasFile.pushString ( CObj.ERROR, "Bad error: " + e.getMessage() );
            e.printStackTrace();

            if ( session != null )
            {
                try
                {
                    if ( session.getTransaction().isActive() )
                    {
                        session.getTransaction().rollback();
                    }

                }

                catch ( Exception e2 )
                {
                }

                try
                {
                    session.close();
                }

                catch ( Exception e2 )
                {
                }

            }

            return false;
        }

        //Make the path absolute to help with queries based on the file
        //name later.
        String localFile = hasFile.getPrivate ( CObj.LOCALFILE );

        if ( localFile != null )
        {
            File file = new File ( localFile );

            if ( file.exists() )
            {
                try
                {
                    localFile = file.getCanonicalPath();
                    hasFile.pushPrivate ( CObj.LOCALFILE, localFile );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

        }

        //If the file is a duplicate remove it
        CObj ed = index.getDuplicate ( hasfileid, localFile );

        if ( ed != null )
        {
            try
            {
                index.delete ( ed );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        //See if there is an existing file already for it
        //Make the old one a duplicate
        CObj oldfile = index.getById ( hasfileid );

        if ( oldfile != null )
        {
            String oldlocal = oldfile.getPrivate ( CObj.LOCALFILE );

            if ( oldlocal != null && !oldlocal.equals ( localFile ) )
            {
                File f = new File ( oldlocal );

                if ( f.exists() )
                {
                    CObj dp = new CObj();
                    dp.setType ( CObj.DUPFILE );
                    dp.pushString ( CObj.HASFILE, hasfileid );
                    dp.pushString ( CObj.LOCALFILE, oldlocal );
                    dp.pushString ( CObj.COMMUNITYID, communityID );
                    dp.pushString ( CObj.CREATOR, creatorID );
                    dp.simpleDigest();

                    try
                    {
                        index.index ( dp );
                        index.forceNewSearcher();
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

            }

        }

        //Get the time of the last file and make sure our new fuzzy time
        //is after that.
        long lasttime = 0;
        CObjList ol = index.getHasFiles ( communityID, creatorID, lastSeqNumber, lastSeqNumber );

        if ( ol.size() > 0 )
        {
            try
            {
                CObj of = ol.get ( 0 );

                if ( of != null )
                {
                    Long ct = of.getNumber ( CObj.CREATEDON );

                    if ( ct != null )
                    {
                        lasttime = ct;
                    }

                }

            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        ol.close();

        //Set the rank of the post based on the rank of the
        //user
        Long rnk = myid.getPrivateNumber ( CObj.PRV_USER_RANK );

        if ( rnk != null )
        {
            hasFile.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
        }

        //Set the created on time
        hasFile.pushNumber ( CObj.CREATEDON, Utils.fuzzTime ( lasttime + 1 ) );

        long sq = identManager.getGlobalSequenceNumber ( creatorID, false );
        hasFile.pushPrivateNumber ( CObj.getGlobalSeq ( creatorID ), sq );

        //Sign it.
        spamtool.finalize ( Utils.privateKeyFromString ( myid.getPrivate ( CObj.PRIVATEKEY ) ), hasFile );

        if ( Level.INFO.equals ( log.getLevel() ) )
        {
            log.info ( "FILE CREATED: " + creatorID + " DIG " + hasFile.getDig() + " COMID: " +
                       hasFile.getString ( CObj.COMMUNITYID ) + " SEQ: " + sq );
        }

        try
        {
            index.index ( hasFile );
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            hasFile.pushString ( CObj.ERROR, "File record could not be indexed" );
            return false;
        }

        return true;
    }

    public boolean createPartFile ( RequestFile requestFile, String myCreatorID )
    {
        String communityID = requestFile.getCommunityId();

        CObj myID = validator.isMyUserSubscribed ( communityID, myCreatorID );

        if ( myID == null )
        {
            return false;
        }

        String partFile = requestFile.getLocalPartFile();
        String fileDigest = requestFile.getWholeDigest();
        String fragDigest = requestFile.getFragmentDigest();
        long totalFragments = requestFile.getFragsTotal();

        long now = System.currentTimeMillis();
        long lastCreatedOn = 0L;
        long firstSeen = now;
        long revision = 1L;

        CObj existingHasPart = index.getPartFile ( myCreatorID, communityID, fileDigest, fragDigest );

        // Should have delivered us a list with a maximum of one object, but who knows for sure.
        // If there is already a HasPart in the index, find out when it was created.
        if ( existingHasPart != null )
        {
            Long createdOn = existingHasPart.getNumber ( CObj.CREATEDON );

            if ( createdOn != null )
            {
                lastCreatedOn = Math.max ( lastCreatedOn, createdOn );
            }

            Long compareFirstSeen = existingHasPart.getPrivateNumber ( CObj.FIRSTSEEN );

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

        String id = getCommunityMemberId ( myCreatorID, communityID );
        // merge us a unique ID for this part file
        String partFileID = getHasFileId ( id, fragDigest, fileDigest );

        long fileSize = requestFile.getFileSize();
        CObjList completedFragments = index.getFragmentsComplete ( fileDigest, fragDigest );

        // Create the payload which indicates the indices of the completed fragments.
        String completedFragmentsPayload = CObjHelper.createHasPartPayload ( completedFragments, fileSize, totalFragments );
        completedFragments.close();

        CObj hasPart = new CObj();
        hasPart.setType ( CObj.HASPART );
        hasPart.setId ( partFileID );
        hasPart.pushString ( CObj.CREATOR, myCreatorID );
        hasPart.pushNumber ( CObj.CREATEDON,  Utils.fuzzTime ( lastCreatedOn ) );
        hasPart.pushString ( CObj.COMMUNITYID, communityID );
        hasPart.pushString ( CObj.FILEDIGEST, fileDigest );
        hasPart.pushString ( CObj.FRAGDIGEST,  fragDigest );
        hasPart.pushNumber ( CObj.FRAGNUMBER, totalFragments );
        hasPart.pushString ( CObj.PAYLOAD, completedFragmentsPayload );
        hasPart.pushString ( CObj.STILLHASFILE,  CObj.TRUE );

        hasPart.pushPrivateNumber ( CObj.FIRSTSEEN, firstSeen );
        hasPart.pushPrivateNumber ( CObj.LASTSEEN, now );
        hasPart.pushPrivateNumber ( CObj.REVISION, revision );
        hasPart.pushPrivate ( CObj.MINE, CObj.TRUE );
        hasPart.pushPrivate ( CObj.PRV_SKIP_PAYMENT, CObj.TRUE );

        Session session = null;

        try
        {
            session = hh2Session.getSession();
            session.getTransaction().begin();
            CommunityMember communityMember = ( CommunityMember ) session.get ( CommunityMember.class, myCreatorID );

            if ( communityMember == null )
            {
                communityMember = new CommunityMember();
                communityMember.setId ( id );
                communityMember.setCommunityId ( communityID );
                communityMember.setMemberId ( myCreatorID );
                session.persist ( communityMember );
            }

            long seqNumber = communityMember.getLastPartNumber();
            seqNumber++;
            hasPart.pushNumber ( CObj.SEQNUM, seqNumber );
            communityMember.setLastFileNumber ( seqNumber );
            session.merge ( communityMember );
            session.getTransaction().commit();
            session.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            hasPart.pushString ( CObj.ERROR, "Bad error: " + e.getMessage() );
            e.printStackTrace();

            if ( session != null )
            {
                try
                {
                    if ( session.getTransaction().isActive() )
                    {
                        session.getTransaction().rollback();
                    }

                }

                catch ( Exception e2 )
                {
                }

                try
                {
                    session.close();
                }

                catch ( Exception e2 )
                {
                }

            }

            return false;
        }

        // Add the canonical path of the part file.
        // The filename should include the Aktie part extension.
        if ( partFile != null )
        {
            File file = new File ( partFile );

            if ( file.exists() )
            {
                try
                {
                    partFile = file.getCanonicalPath();
                    hasPart.pushPrivate ( CObj.LOCALFILE, partFile );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

        }

        long globalSequence = identManager.getGlobalSequenceNumber ( myCreatorID, false );
        hasPart.pushPrivateNumber ( CObj.getGlobalSeq ( myCreatorID ), globalSequence );

        //Sign it.
        spamtool.finalize ( Utils.privateKeyFromString ( myID.getPrivate ( CObj.PRIVATEKEY ) ), hasPart );

        // Put into the local index.
        try
        {
            index.index ( hasPart );
        }

        catch ( IOException e )
        {
        }

        return true;
    }

}
