package aktie.utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortedNumericSortField;
import org.hibernate.Session;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.HH2Session;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;

public class HasFileCreator
{
    Logger log = Logger.getLogger ( "aktie" );

    private HH2Session session;
    private Index index;
    private SubscriptionValidator validator;
    private SpamTool spamtool;
    private IdentityManager identManager;

    public HasFileCreator ( HH2Session s, Index i, SpamTool st )
    {
        index = i;
        session = s;
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

    public static void wtfHasFileWithoutfile ( CObj o )
    {
        System.out.println ( "--------------------------------------------------" );
        System.out.println ( "WTF?  I think I have a file.. but I don't." );
        System.out.println ( "Is there something odd with: " );
        System.out.println ( o.toString() );
        System.out.println ( "--------------------------------------------------" );
    }

    public boolean createHasFile ( CObj o )
    {
        //Set File sequence number for the community/creator
        String creator = o.getString ( CObj.CREATOR );
        String comid = o.getString ( CObj.COMMUNITYID );

        String digofdigs = o.getString ( CObj.FRAGDIGEST );
        String wholedig = o.getString ( CObj.FILEDIGEST );

        CObj myid = validator.isMyUserSubscribed ( comid, creator );

        if ( myid == null )
        {
            o.pushString ( CObj.ERROR, "Cannot add file - not properly subscribed" );
            return false;
        }

        if ( !validator.canHasFile ( comid, creator, wholedig, digofdigs ) )
        {
            o.pushString ( CObj.ERROR, "Cannot add file to this blog" );
            return false;
        }

        String id = getCommunityMemberId ( creator, comid );
        String hasfileid = getHasFileId ( id, digofdigs, wholedig );

        o.setId ( hasfileid ); //only 1 has file per user per community per file digest

        //Make the path absolute to help with queries based on the file
        //name later.
        String lf = o.getPrivate ( CObj.LOCALFILE );

        if ( lf != null )
        {
            File f = new File ( lf );

            if ( f.exists() )
            {
                try
                {
                    lf = f.getCanonicalPath();
                    o.pushPrivate ( CObj.LOCALFILE, lf );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                    o.pushString ( CObj.ERROR, e.getMessage() );
                    return false;
                }

            }

        }

        //Check if we're a duplicate
        CObj ed = index.getDuplicate ( hasfileid, lf );
        CObj oldfile = index.getById ( hasfileid );

        if ( oldfile != null )
        {
            String oldlf = oldfile.getPrivate ( CObj.LOCALFILE );
            String oldsh = oldfile.getString ( CObj.STILLHASFILE );

            if ( oldlf == null )
            {
                if ( "true".equals ( oldsh ) || oldlf != null )
                {
                    wtfHasFileWithoutfile ( oldfile );
                }

            }

            boolean oldfexists = false;

            if ( oldlf != null )
            {
                File oldf = new File ( oldlf );
                oldfexists = oldf.exists();
            }

            String newsh = o.getString ( CObj.STILLHASFILE );

            log.info ( "Old hasfile found: " + oldlf + " name: " + oldfile.getString ( CObj.NAME ) +
                       " exists: " + oldfexists +
                       " old stillhas: " + oldsh + " new stillhas: " + newsh );

            if ( newsh != null && newsh.equals ( oldsh ) && lf != null && lf.equals ( oldlf ) )
            {
                log.info ( "The old hasfile is the same as the new" );
                oldfile.makeCopy ( o );
                return true;
            }

            //Can never re-use if stillhas is different
            if ( oldsh != null && oldsh.equals ( newsh ) )
            {
                File lff = new File ( lf );

                if ( lff.exists() && !oldfexists && "true".equals ( newsh ) )
                {
                    //The new file exists, the old one does not.  Just
                    //update the localfile path of the old one to match this
                    //file.  If it's false we don't care really.
                    log.info ( "Reusing old hasfile.  Changing localfile. name: " +
                               oldfile.getString ( CObj.NAME ) );
                    oldfile.pushPrivate ( CObj.LOCALFILE, lf );

                    try
                    {
                        index.index ( oldfile );
                        index.forceNewSearcher();
                    }

                    catch ( IOException e )
                    {
                        o.pushString ( CObj.ERROR, e.getMessage() );
                        e.printStackTrace();
                        return false;
                    }

                    if ( ed != null )
                    {
                        try
                        {
                            //It's no longer a duplicate
                            index.delete ( ed );
                            index.forceNewSearcher();
                        }

                        catch ( IOException e )
                        {
                            o.pushString ( CObj.ERROR, e.getMessage() );
                            e.printStackTrace();
                            return false;
                        }

                    }

                }

                if ( ed != null && !lff.exists() )
                {
                    try
                    {
                        index.delete ( ed );
                        index.forceNewSearcher();
                    }

                    catch ( IOException e )
                    {
                        e.printStackTrace();
                    }

                }

                if ( ed == null && lff.exists() && oldfexists && "true".equals ( newsh ) )
                {
                    log.info ( "Creating duplicate for: " + lf );
                    CObj dp = new CObj();
                    dp.setType ( CObj.DUPFILE );
                    dp.pushString ( CObj.HASFILE, hasfileid );
                    dp.pushString ( CObj.LOCALFILE, lf );
                    dp.pushString ( CObj.COMMUNITYID, comid );
                    dp.pushString ( CObj.CREATOR, creator );
                    dp.simpleDigest();

                    try
                    {
                        index.index ( dp );
                        index.forceNewSearcher();
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                        o.pushString ( CObj.ERROR, e.getMessage() );
                        return false;
                    }

                }

                log.info ( "Reused..." );
                oldfile.makeCopy ( o );
                return true;


            }

        }

        //Ok, we have to create a new one.
        Session s = null;
        long lastnum = 0;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            CommunityMember m = ( CommunityMember ) s.get ( CommunityMember.class, id );

            if ( m == null )
            {
                m = new CommunityMember();
                m.setId ( id );
                m.setCommunityId ( comid );
                m.setMemberId ( creator );
                s.persist ( m );
            }

            long num = m.getLastFileNumber();
            lastnum = num;
            num++;
            o.pushNumber ( CObj.SEQNUM, num );
            o.pushPrivate ( CObj.MINE, "true" );
            m.setLastFileNumber ( num );
            s.merge ( m );
            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            o.pushString ( CObj.ERROR, e.getMessage() );
            e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    if ( s.getTransaction().isActive() )
                    {
                        s.getTransaction().rollback();
                    }

                }

                catch ( Exception e2 )
                {
                }

                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

            return false;
        }

        //See if there is an existing file already for it
        //Make the old one a duplicate

        //Get the time of the last file and make sure our new fuzzy time
        //is after that.
        long lasttime = 0;
        CObjList ol = index.getHasFiles ( comid, creator, lastnum, lastnum );

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
            o.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
        }

        //Set the created on time
        o.pushNumber ( CObj.CREATEDON, Utils.fuzzTime ( lasttime + 1 ) );

        long sq = identManager.getGlobalSequenceNumber ( creator, false );
        o.pushPrivateNumber ( CObj.getGlobalSeq ( creator ), sq );

        //Sign it.
        spamtool.finalize ( Utils.privateKeyFromString ( myid.getPrivate ( CObj.PRIVATEKEY ) ), o );

        if ( Level.INFO.equals ( log.getLevel() ) )
        {
            log.info ( "FILE CREATED: " + creator + " DIG " + o.getDig() + " COMID: " +
                       o.getString ( CObj.COMMUNITYID ) + " SEQ: " + sq );
        }

        try
        {
            index.index ( o );
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            o.pushString ( CObj.ERROR, "File record could not be indexed" );
            return false;
        }

        return true;
    }

}
