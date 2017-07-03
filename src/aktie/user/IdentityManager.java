package aktie.user;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.CommunityMyMember;
import aktie.data.DeveloperIdentity;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.data.PrivateMsgIdentity;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.sequences.FileSequence;
import aktie.sequences.MemberSequence;
import aktie.sequences.PostSequence;
import aktie.sequences.PrivIdentSequence;
import aktie.sequences.PrivMsgSequence;
import aktie.sequences.SpamSequence;
import aktie.sequences.SubSequence;
import aktie.utils.HasFileCreator;

public class IdentityManager
{

    public static int MAX_LAST_NUMBER = 10;
    //Now that we have global sequences always
    //take the next greatest object sequence number
    public static int MAX_UPDATE_CYCLE = 2;

    private HH2Session session;
    private Index index;

    public IdentityManager ( HH2Session s, Index i )
    {
        session = s;
        index = i;
    }

    public IdentityData getIdentity ( String id )
    {
        IdentityData r = null;
        Session s = null;

        try
        {
            s = session.getSession();
            r = ( IdentityData ) s.get ( IdentityData.class, id );
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return r;
    }

    public void connectionClose ( String id, long inNonFile, long inTotal, long outTotal )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();//LOCKED HERE!
            IdentityData idat = ( IdentityData ) s.get ( IdentityData.class, id );

            if ( idat != null )
            {
                idat.setTotalNonFileReceived (
                    idat.getTotalNonFileReceived() + inNonFile );
                idat.setTotalReceived (
                    idat.getTotalReceived() + inTotal );
                idat.setTotalSent (
                    idat.getTotalSent() + outTotal );

                if ( inTotal > 0 )
                {
                    idat.setLastSuccessfulConnection ( ( new Date() ).getTime() );
                }

                s.merge ( idat );
            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

    }

    public void connectionAttempted ( String id )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            IdentityData idat = ( IdentityData ) s.get ( IdentityData.class, id );

            if ( idat != null )
            {
                idat.setLastConnectionAttempt ( ( new Date() ).getTime() );
                s.merge ( idat );
            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

    }

    @SuppressWarnings ( "unchecked" )
    public List<CommunityMember> nextHasFileUpdate ( int max )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            //Only set UPDATE for CommunityMember for communities to which
            //we are subscribed.
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.fileStatus = :st ORDER BY x.fileUpdatePriority DESC, "
                                      + "x.lastFileUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            q.setMaxResults ( max );
            List<CommunityMember> r = q.list();
            s.close();
            return r;
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return new LinkedList<CommunityMember>();
    }

    @SuppressWarnings ( "unchecked" )
    public CommunityMember claimHasFileUpdate ( String thisid, Map<String, Integer> comids, int rereqperiod )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.fileStatus = :st "
                                      + " ORDER BY "
                                      + "x.fileUpdatePriority DESC, "
                                      + "x.lastFileUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            //q.setMaxResults ( 100 );
            CommunityMember cm = null;
            List<CommunityMember> r = q.list();
            Iterator<CommunityMember> i = r.iterator();

            while ( i.hasNext() && cm == null )
            {
                CommunityMember c = i.next();

                Integer nummem = comids.get ( c.getCommunityId() );

                if ( nummem != null )
                {
                    //If only 2 subscribers, then always get update from other
                    //subscriber (you one of the two).
                    if ( nummem <= 2 ) //Crazy if less than 2. but 2 ok.
                    {
                        cm = c;
                    }

                    //More than 2 subscribers, get update from someone other than
                    //same person got last one.  if updates have been requested
                    //a few times sense the last time we got updates then ok
                    else if ( ( !thisid.equals ( c.getLastFileUpdateFrom() ) ) ||
                              c.getFileUpdateCycle() > rereqperiod )
                    {
                        cm = c;
                    }

                }

            }

            if ( cm != null )
            {
                cm.setFileStatus ( CommunityMember.DONE );
                cm.setLastFileUpdate ( System.currentTimeMillis() );
                cm.setFileUpdateCycle ( 0 );
                cm.setLastFileUpdateFrom ( thisid );
                s.merge ( cm );
            }

            s.getTransaction().commit();
            s.close();
            return cm;
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return null;
    }

    @SuppressWarnings ( "unchecked" )
    public List<CommunityMember> claimHasFileUpdate ( int max )
    {
        List<CommunityMember> rl = new LinkedList<CommunityMember>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.fileStatus = :st "
                                      + " ORDER BY "
                                      + "x.fileUpdatePriority DESC, "
                                      + "x.lastFileUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            q.setMaxResults ( max );
            CommunityMember cm = null;
            List<CommunityMember> r = q.list();
            Iterator<CommunityMember> i = r.iterator();

            while ( i.hasNext() )
            {
                cm = i.next();

                if ( cm != null )
                {
                    cm.setFileStatus ( CommunityMember.DONE );
                    cm.setLastFileUpdate ( System.currentTimeMillis() );
                    cm.setFileUpdateCycle ( 0 );
                    cm.setLastFileUpdateFrom ( "" );
                    s.merge ( cm );
                    rl.add ( cm );
                }

            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    @SuppressWarnings ( "unchecked" )
    public List<CommunityMember> claimHasFileUpdate ( String comid, int max )
    {
        List<CommunityMember> rl = new LinkedList<CommunityMember>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.fileStatus = :st AND "
                                      + "x.communityId = :comid "
                                      + " ORDER BY "
                                      + "x.fileUpdatePriority DESC, "
                                      + "x.lastFileUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            q.setParameter ( "comid", comid );
            q.setMaxResults ( max );
            CommunityMember cm = null;
            List<CommunityMember> r = q.list();
            Iterator<CommunityMember> i = r.iterator();

            while ( i.hasNext() )
            {
                cm = i.next();

                if ( cm != null )
                {
                    cm.setFileStatus ( CommunityMember.DONE );
                    cm.setLastFileUpdate ( System.currentTimeMillis() );
                    cm.setFileUpdateCycle ( 0 );
                    cm.setLastFileUpdateFrom ( "" );
                    s.merge ( cm );
                    rl.add ( cm );
                }

            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    @SuppressWarnings ( "unchecked" )
    public CommunityMember claimPostUpdate ( String thisid, Map<String, Integer> comids, int rereqperiod )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();//LOCKED HERE!
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.postStatus = :st ORDER BY "
                                      + "x.postUpdatePriority DESC, "
                                      + "x.lastPostUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            //q.setMaxResults ( 100 );
            CommunityMember cm = null;
            List<CommunityMember> r = q.list();
            Iterator<CommunityMember> i = r.iterator();

            while ( i.hasNext() && cm == null )
            {
                CommunityMember c = i.next();

                Integer nummem = comids.get ( c.getCommunityId() );

                if ( nummem != null )
                {
                    //If only 2 subscribers, then always get update from other
                    //subscriber (you one of the two).
                    if ( nummem <= 2 ) //Crazy if less than 2. but 2 ok.
                    {
                        cm = c;
                    }

                    //More than 2 subscribers, get update from someone other than
                    //same person got last one.  if updates have been requested
                    //a few times sense the last time we got updates then ok
                    else if ( ( !thisid.equals ( c.getLastPostUpdateFrom() ) ) ||
                              c.getPostUpdateCycle() > rereqperiod )
                    {
                        cm = c;
                    }

                }

            }

            if ( cm != null )
            {
                cm.setPostStatus ( CommunityMember.DONE );
                cm.setLastPostUpdate ( System.currentTimeMillis() );
                cm.setPostUpdateCycle ( 0 );
                cm.setLastPostUpdateFrom ( thisid );
                s.merge ( cm );
            }

            s.getTransaction().commit();
            s.close();
            return cm;
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return null;
    }

    @SuppressWarnings ( "unchecked" )
    public List<CommunityMember> claimPostUpdate ( int max )
    {
        List<CommunityMember> rl = new LinkedList<CommunityMember>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();//LOCKED HERE!
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.postStatus = :st ORDER BY "
                                      + "x.postUpdatePriority DESC, "
                                      + "x.lastPostUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            q.setMaxResults ( max );
            CommunityMember cm = null;
            List<CommunityMember> r = q.list();
            Iterator<CommunityMember> i = r.iterator();

            while ( i.hasNext() )
            {
                cm = i.next();

                if ( cm != null )
                {
                    cm.setPostStatus ( CommunityMember.DONE );
                    cm.setLastPostUpdate ( System.currentTimeMillis() );
                    cm.setPostUpdateCycle ( 0 );
                    cm.setLastPostUpdateFrom ( "" );
                    s.merge ( cm );
                    rl.add ( cm );
                }

            }


            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    @SuppressWarnings ( "unchecked" )
    public List<CommunityMember> claimPostUpdate ( String comid, int max )
    {
        List<CommunityMember> rl = new LinkedList<CommunityMember>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();//LOCKED HERE!
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.postStatus = :st AND "
                                      + "x.communityId = :comid ORDER BY "
                                      + "x.postUpdatePriority DESC, "
                                      + "x.lastPostUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            q.setParameter ( "comid", comid );
            q.setMaxResults ( max );
            CommunityMember cm = null;
            List<CommunityMember> r = q.list();
            Iterator<CommunityMember> i = r.iterator();

            while ( i.hasNext() )
            {
                cm = i.next();

                if ( cm != null )
                {
                    cm.setPostStatus ( CommunityMember.DONE );
                    cm.setLastPostUpdate ( System.currentTimeMillis() );
                    cm.setPostUpdateCycle ( 0 );
                    cm.setLastPostUpdateFrom ( "" );
                    s.merge ( cm );
                    rl.add ( cm );
                }

            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    @SuppressWarnings ( "unchecked" )
    public IdentityData claimMemberUpdate ( String fromid, int upcycle )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false AND "
                                      + "x.memberStatus = :st AND "
                                      + "( x.lastMemberUpdateFrom != :fromid OR "
                                      + "  x.memberUpdateCycle >= :cycnum "
                                      + ") ORDER BY "
                                      + "x.memberUpdatePriority DESC, "
                                      + "x.lastMemberUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setParameter ( "fromid", fromid );
            q.setParameter ( "cycnum", upcycle );
            q.setMaxResults ( 1 );
            List<IdentityData> r = q.list();
            IdentityData id = null;

            if ( r.size() > 0 )
            {
                id = r.get ( 0 );

                if ( id != null )
                {
                    id.setLastMemberUpdate ( System.currentTimeMillis() );
                    id.setMemberStatus ( IdentityData.DONE );
                    id.setMemberUpdateCycle ( 0 );
                    id.setLastMemberUpdateFrom ( fromid );
                    s.merge ( id );
                }

            }

            s.getTransaction().commit();
            s.close();
            return id;
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return null;
    }

    @SuppressWarnings ( "unchecked" )
    public List<IdentityData> claimMemberUpdate ( int max )
    {
        List<IdentityData> rl = new LinkedList<IdentityData>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false AND "
                                      + "x.memberStatus = :st ORDER BY "
                                      + "x.memberUpdatePriority DESC, "
                                      + "x.lastMemberUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setMaxResults ( 1 );
            List<IdentityData> r = q.list();
            IdentityData id = null;
            Iterator<IdentityData> i = r.iterator();

            while ( i.hasNext() )
            {
                id = i.next();

                if ( id != null )
                {
                    id.setLastMemberUpdate ( System.currentTimeMillis() );
                    id.setMemberStatus ( IdentityData.DONE );
                    id.setMemberUpdateCycle ( 0 );
                    id.setLastMemberUpdateFrom ( "" );
                    s.merge ( id );
                    rl.add ( id );
                }

            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    @SuppressWarnings ( "unchecked" )
    public List<IdentityData> claimSubUpdate ( int max )
    {
        List<IdentityData> rl = new LinkedList<IdentityData>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false AND "
                                      + "x.subStatus = :st ORDER BY "
                                      + "x.subUpdatePriority DESC, "
                                      + "x.lastSubUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setMaxResults ( 1 );
            List<IdentityData> r = q.list();
            IdentityData id = null;
            Iterator<IdentityData> i = r.iterator();

            while ( i.hasNext() )
            {
                id = i.next();

                if ( id != null )
                {
                    id.setLastSubUpdate ( System.currentTimeMillis() );
                    id.setSubStatus ( IdentityData.DONE );
                    id.setSubUpdateCycle ( 0 );
                    id.setLastSubUpdateFrom ( "" );
                    s.merge ( id );
                    rl.add ( id );
                }

            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    @SuppressWarnings ( "unchecked" )
    public IdentityData claimCommunityUpdate ( String fromid, int upcycle )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false AND "
                                      + "x.communityStatus = :st AND "
                                      + "( x.lastCommunityUpdateFrom != :fromid OR "
                                      + "  x.communityUpdateCycle >= :cycnum "
                                      + ") ORDER BY "
                                      + "x.communityUpdatePriority DESC, "
                                      + "x.lastCommunityUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setParameter ( "fromid", fromid );
            q.setParameter ( "cycnum", upcycle );
            q.setMaxResults ( 1 );
            List<IdentityData> r = q.list();
            IdentityData id = null;

            if ( r.size() > 0 )
            {
                id = r.get ( 0 );

                if ( id != null )
                {
                    id.setLastCommunityUpdate ( System.currentTimeMillis() );
                    id.setCommunityStatus ( IdentityData.DONE );
                    id.setCommunityUpdateCycle ( 0 );
                    id.setLastCommunityUpdateFrom ( fromid );
                    s.merge ( id );
                }

            }

            s.getTransaction().commit();
            s.close();
            return id;
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return null;
    }

    @SuppressWarnings ( "unchecked" )
    public List<IdentityData> claimCommunityUpdate ( int max )
    {
        List<IdentityData> rl = new LinkedList<IdentityData>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false AND "
                                      + "x.communityStatus = :st "
                                      + " ORDER BY "
                                      + "x.communityUpdatePriority DESC, "
                                      + "x.lastCommunityUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setMaxResults ( max );
            List<IdentityData> r = q.list();
            IdentityData id = null;
            Iterator<IdentityData> i = r.iterator();

            while ( i.hasNext() )
            {
                id = i.next();

                if ( id != null )
                {
                    id.setLastCommunityUpdate ( System.currentTimeMillis() );
                    id.setCommunityStatus ( IdentityData.DONE );
                    id.setCommunityUpdateCycle ( 0 );
                    id.setLastCommunityUpdateFrom ( "" );
                    s.merge ( id );
                    rl.add ( id );
                }

            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    @SuppressWarnings ( "unchecked" )
    public List<PrivateMsgIdentity> claimPrvtIdentUpdate ( int max )
    {
        List<PrivateMsgIdentity> rl = new LinkedList<PrivateMsgIdentity>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM PrivateMsgIdentity x WHERE x.mine = false AND "
                                      + "x.identStatus = :st "
                                      + " ORDER BY "
                                      + "x.identUpdatePriority DESC, "
                                      + "x.lastIdentUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setMaxResults ( max );
            List<PrivateMsgIdentity> r = q.list();
            PrivateMsgIdentity id = null;
            Iterator<PrivateMsgIdentity> i = r.iterator();

            while ( i.hasNext() )
            {
                id = i.next();

                if ( id != null )
                {
                    id.setLastIdentUpdate ( System.currentTimeMillis() );
                    id.setIdentStatus ( IdentityData.DONE );
                    id.setIdentUpdateCycle ( 0 );
                    s.merge ( id );
                    rl.add ( id );
                }

            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    @SuppressWarnings ( "unchecked" )
    public List<PrivateMsgIdentity> claimPrvtMsgUpdate ( int max )
    {
        List<PrivateMsgIdentity> rl = new LinkedList<PrivateMsgIdentity>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM PrivateMsgIdentity x WHERE x.mine = false AND "
                                      + "x.msgStatus = :st "
                                      + " ORDER BY "
                                      + "x.msgUpdatePriority DESC, "
                                      + "x.lastMsgUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setMaxResults ( max );
            List<PrivateMsgIdentity> r = q.list();
            PrivateMsgIdentity id = null;
            Iterator<PrivateMsgIdentity> i = r.iterator();

            while ( i.hasNext() )
            {
                id = i.next();

                if ( id != null )
                {
                    id.setLastMsgUpdate ( System.currentTimeMillis() );
                    id.setMsgStatus ( IdentityData.DONE );
                    id.setMsgUpdateCycle ( 0 );
                    s.merge ( id );
                    rl.add ( id );
                }

            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    @SuppressWarnings ( "unchecked" )
    public List<DeveloperIdentity> claimSpamExUpdate ( int max )
    {
        List<DeveloperIdentity> rl = new LinkedList<DeveloperIdentity>();
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM DeveloperIdentity x WHERE "
                                      + "x.spamExStatus = :st "
                                      + " ORDER BY "
                                      + "x.spamExUpdatePriority DESC, "
                                      + "x.lastSpamExUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setMaxResults ( max );
            List<DeveloperIdentity> r = q.list();
            DeveloperIdentity id = null;
            Iterator<DeveloperIdentity> i = r.iterator();

            while ( i.hasNext() )
            {
                id = i.next();

                if ( id != null )
                {
                    id.setLastSpamExUpdate ( System.currentTimeMillis() );
                    id.setSpamExStatus ( IdentityData.DONE );
                    s.merge ( id );
                    rl.add ( id );
                }

            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            ////e.printStackTrace();

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

        }

        return rl;
    }

    public IdentityData claimIdentityUpdate ( String id )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            IdentityData idat = ( IdentityData ) s.get ( IdentityData.class, id );

            if ( idat != null )
            {
                if ( idat != null && idat.getIdentityStatus() == IdentityData.UPDATE )
                {
                    idat.setIdentityStatus ( IdentityData.DONE );
                    idat.setLastIdentityUpdate ( System.currentTimeMillis() );
                    s.merge ( idat );
                }

                else
                {
                    idat = null;
                }

            }

            s.getTransaction().commit();
            s.close();
            return idat;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

        return null;
    }

    @SuppressWarnings ( "unchecked" )
    public List<CommunityMember> nextHasPostUpdate ( int max )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            //Only set UPDATE for CommunityMember for communities to which
            //we are subscribed.
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.postStatus = :st ORDER BY x.postUpdatePriority DESC, "
                                      + "x.lastPostUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            q.setMaxResults ( max );
            List<CommunityMember> r = q.list();
            s.close();
            return r;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return new LinkedList<CommunityMember>();
    }

    @SuppressWarnings ( "unchecked" )
    public List<CommunityMember> nextHasSubscriptionUpdate ( int max )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            //Only set UPDATE for CommunityMember for communities to which
            //we are subscribed.
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.subscriptionStatus = :st ORDER BY x.subscriptionUpdatePriority DESC, "
                                      + "x.lastSubscriptionUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            q.setMaxResults ( max );
            List<CommunityMember> r = q.list();
            s.close();
            return r;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return new LinkedList<CommunityMember>();
    }

    @SuppressWarnings ( "unchecked" )
    public List<IdentityData> nextCommunityUpdate ( int max )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            //Only set UPDATE for CommunityMember for communities to which
            //we are subscribed.
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mind = false AND "
                                      + "x.communityStatus = :st ORDER BY x.communityUpdatePriority DESC, "
                                      + "x.lastCommunityUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setMaxResults ( max );
            List<IdentityData> r = q.list();
            s.close();
            return r;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return new LinkedList<IdentityData>();
    }

    public long getIdentityCommunitySeqNumber ( String id, String comid )
    {
        long sn = 0;
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();

            String cid = HasFileCreator.getCommunityMemberId ( id, comid );
            CommunityMember d = ( CommunityMember ) s.get ( CommunityMember.class, cid );

            if ( d != null )
            {
                sn = d.getLastGlobalSequence();
            }

            s.getTransaction().commit();
        }

        catch ( Exception e )
        {

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

            }

        }

        finally
        {
            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return sn;
    }

    public void updateIdentityCommunitySeqNumber ( String id, String comid, long seq, boolean force )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();

            String cid = HasFileCreator.getCommunityMemberId ( id, comid );
            CommunityMember d = ( CommunityMember ) s.get ( CommunityMember.class, cid );

            if ( d == null )
            {
                d = new CommunityMember();
                d.setCommunityId ( comid );
                d.setMemberId ( id );
                d.setId ( cid );
            }

            if ( d != null )
            {
                if ( d.getLastGlobalSequence() < seq || force )
                {
                    d.setLastGlobalSequence ( seq );
                    s.merge ( d );
                }

            }

            s.getTransaction().commit();
        }

        catch ( Exception e )
        {

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

            }

        }

        finally
        {
            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    public void updateGlobalSequenceNumber ( String id, boolean up, long ps,
            boolean um, long ms,
            boolean us, long ss )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            IdentityData dat = ( IdentityData ) s.get ( IdentityData.class, id );

            if ( dat != null )
            {
                if ( up &&
                        ( ps == 0 || ps > dat.getLastPubGlobalSequence() ) )
                {
                    dat.setLastPubGlobalSequence ( ps );
                }

                if ( um &&
                        ( ms == 0 || ms > dat.getLastMemGlobalSequence() ) )
                {
                    dat.setLastMemGlobalSequence ( ms );
                }

                if ( us &&
                        ( ss == 0 || ss > dat.getLastSubGlobalSequence() ) )
                {
                    dat.setLastSubGlobalSequence ( ss );
                }

                s.merge ( dat );
            }

            s.getTransaction().commit();
        }

        catch ( Exception e )
        {

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

            }

        }

        finally
        {
            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    public IdentityData getLastGlobalSequenceNumber ( String id )
    {
        IdentityData dat = null;
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            dat = ( IdentityData ) s.get ( IdentityData.class, id );
            s.getTransaction().commit();
        }

        catch ( Exception e )
        {

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

            }

        }

        finally
        {
            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return dat;
    }

    public long getMyLastGlobalSequenceNumber ( String id )
    {
        long sn = 0;
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            IdentityData dat = ( IdentityData ) s.get ( IdentityData.class, id );

            if ( dat != null )
            {
                if ( checkGlobalSequenceNumber ( dat, false ) )
                {
                    s.merge ( dat );
                }

                sn = dat.getLastPubGlobalSequence();
            }

            s.getTransaction().commit();
        }

        catch ( Exception e )
        {
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

            }

        }

        finally
        {
            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return sn;
    }

    private boolean checkGlobalSequenceNumber ( IdentityData dat, boolean force )
    {
        boolean update = force;

        if ( dat.getLastPubGlobalSequence() == 0 )
        {
            dat.setLastPubGlobalSequence ( 1 + Utils.Random.nextInt ( 10000 ) );
            update = true;
        }

        update = update ||
                 ( dat.getNextGlobalSequenceUpdateTime() < System.currentTimeMillis() );
        update = update ||
                 ( dat.getCountForLastGlobalSequence() > IdentityData.MAXGLOBALSEQUENCECOUNT );

        if ( update )
        {
            dat.setLastPubGlobalSequence ( dat.getLastPubGlobalSequence() + 1 );
            dat.setNextGlobalSequenceUpdateTime ( System.currentTimeMillis() +
                                                  ( long ) Utils.Random.nextInt ( ( int ) IdentityData.MAXGLOBALSEQUENCETIME ) );
            dat.setCountForLastGlobalSequence (
                ( long ) Utils.Random.nextInt ( ( int ) IdentityData.MAXGLOBALSEQUENCECOUNT ) );
        }

        return update;
    }

    public long getGlobalSequenceNumber ( String id, boolean force )
    {
        long sn = 0;
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            IdentityData dat = ( IdentityData ) s.get ( IdentityData.class, id );

            if ( dat != null )
            {
                checkGlobalSequenceNumber ( dat, force );

                dat.setCountForLastGlobalSequence (
                    dat.getCountForLastGlobalSequence() + 1 );

                s.merge ( dat );

                if ( !force )
                {
                    sn = dat.getLastPubGlobalSequence() + 1;
                }

                else
                {
                    sn = dat.getLastPubGlobalSequence();
                }

            }

            s.getTransaction().commit();
        }

        catch ( Exception e )
        {
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

            }

        }

        finally
        {
            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return sn;
    }

    @SuppressWarnings ( "unchecked" )
    public List<IdentityData> nextMemberUpdate ( int max )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            //Only set UPDATE for CommunityMember for communities to which
            //we are subscribed.
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false AND "
                                      + "x.memberStatus = :st ORDER BY x.memberUpdatePriority DESC, "
                                      + "x.lastMemberUpdate ASC" );
            q.setParameter ( "st", IdentityData.UPDATE );
            q.setMaxResults ( max );
            List<IdentityData> r = q.list();
            s.close();
            return r;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return new LinkedList<IdentityData>();
    }

    @SuppressWarnings ( "unchecked" )
    public List<IdentityData> listMostReliable ( int max )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            //Only set UPDATE for CommunityMember for communities to which
            //we are subscribed.
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false "
                                      + "ORDER BY "
                                      + "x.lastSuccessfulConnection DESC, "
                                      + "x.totalNonFileReceived DESC, "
                                      + "x.lastConnectionAttempt ASC" );
            q.setMaxResults ( max );
            List<IdentityData> r = q.list();
            s.close();
            return r;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return new LinkedList<IdentityData>();
    }

    private List<String> getMyIds()
    {
        CObjList cl = index.getMyIdentities();
        List<String> r = new LinkedList<String>();

        for ( int c = 0; c < cl.size(); c++ )
        {
            try
            {
                CObj mo = cl.get ( c );
                r.add ( mo.getId() );
            }

            catch ( Exception e )
            {
                //e.printStackTrace();
            }

        }

        cl.close();
        return r;
    }

    private List<String> getMySubs()
    {
        List<String> r = new LinkedList<String>();
        CObjList cl = index.getMySubscriptions();

        for ( int c = 0; c < cl.size(); c++ )
        {
            try
            {
                CObj co = cl.get ( c );
                r.add ( co.getString ( CObj.COMMUNITYID ) );
            }

            catch ( IOException e )
            {
                //e.printStackTrace();
            }

        }

        cl.close();
        return r;
    }

    public void saveMyMembership ( CommunityMyMember m )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            s.merge ( m );
            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

    }

    public void newDeveloperIdentity ( String id )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            DeveloperIdentity d = ( DeveloperIdentity ) s.get ( DeveloperIdentity.class, id );

            if ( d == null )
            {
                d = new DeveloperIdentity();
                d.setId ( id );
                s.persist ( d );
            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

    }

    public DeveloperIdentity getDeveloperIdentity ( String id )
    {
        DeveloperIdentity di = null;
        Session s = null;

        try
        {
            s = session.getSession();
            di = ( DeveloperIdentity ) s.get ( DeveloperIdentity.class, id );
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

        return di;
    }

    @SuppressWarnings ( "unchecked" )
    public List<CommunityMyMember> getMyMemberships()
    {
        Session s = null;
        List<CommunityMyMember> r = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM CommunityMyMember x" );
            r = q.list();
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        if ( r == null )
        {
            r = new LinkedList<CommunityMyMember>();
        }

        return r;
    }

    @SuppressWarnings ( "unchecked" )
    public void requestMembers()
    {
        List<String> myids = getMyIds();
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false" );
            List<String> ids = new LinkedList<String>();
            List<IdentityData> idl = q.list();

            for ( IdentityData i : idl )
            {
                String id = i.getId();

                if ( id != null && !myids.contains ( id ) )
                {
                    ids.add ( i.getId() );
                }

            }

            for ( String i : ids )
            {
                MemberSequence mseq = new MemberSequence ( session );
                mseq.request ( s, i, 5, i );

            }

            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

    }

    @SuppressWarnings ( "unchecked" )
    public void requestPrvIdentMsg ( String uid )
    {
        List<String> myids = getMyIds();
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM PrivateMsgIdentity x WHERE x.id = :uid" );
            q.setParameter ( "uid", uid );
            List<String> ids = new LinkedList<String>();
            List<PrivateMsgIdentity> idl = q.list();

            for ( PrivateMsgIdentity i : idl )
            {
                String id = i.getId();

                if ( id != null && !myids.contains ( id ) )
                {
                    ids.add ( i.getId() );
                }

            }

            for ( String i : ids )
            {
                PrivIdentSequence pseq = new PrivIdentSequence ( session );
                pseq.request ( s, i, 5, i );
                PrivMsgSequence mseq = new PrivMsgSequence ( session );
                mseq.request ( s, i, 5, i );
            }

            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

    }

    @SuppressWarnings ( "unchecked" )
    public void requestSpamEx()
    {
        List<String> myids = getMyIds();
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM DeveloperIdentity x" );
            List<String> ids = new LinkedList<String>();
            List<DeveloperIdentity> idl = q.list();

            for ( DeveloperIdentity i : idl )
            {
                String id = i.getId();

                if ( id != null && !myids.contains ( id ) )
                {
                    ids.add ( i.getId() );
                }

            }

            for ( String i : ids )
            {
                SpamSequence sseq = new SpamSequence ( session );
                sseq.request ( s, i, 5, i );
            }

        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

            }

        }

        finally
        {
            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    @SuppressWarnings ( "unchecked" )
    public void requestIdenties()
    {
        List<String> myids = getMyIds();
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false" );
            List<String> ids = new LinkedList<String>();
            List<IdentityData> idl = q.list();

            for ( IdentityData i : idl )
            {
                String id = i.getId();

                if ( id != null && !myids.contains ( id ) )
                {
                    ids.add ( i.getId() );
                }

            }

            for ( String i : ids )
            {
                s.getTransaction().begin();
                IdentityData idat = ( IdentityData ) s.get ( IdentityData.class, i );
                idat.setIdentityStatus ( IdentityData.UPDATE );
                s.merge ( idat );
                s.getTransaction().commit();
            }

            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

    }

    public void requestAllHasFile ( int priority )
    {
        List<String> mysubs = this.getMySubs();

        for ( String cid : mysubs )
        {
            requestHasFile ( cid, priority );
        }

    }

    public void requestHasFile ( String comid, int priority )
    {
        //Get my ids.
        List<String> myids = getMyIds();
        Session s = null;

        try
        {
            s = session.getSession();
            //CObjList othersublist = index.getSubscriptions(comid);
            List<String> memlist = getMembers ( comid );

            for ( int c = 0; c < memlist.size(); c++ )
            {
                String id = null;
                String memid = memlist.get ( c );

                if ( memid != null && !myids.contains ( memid ) )
                {
                    id = Utils.mergeIds ( memid, comid );
                }

                if ( id != null )
                {
                    FileSequence fseq = new FileSequence ( session );
                    fseq.request ( s, id, 5, id, comid, memid );
                }

            }

            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

    }

    public void requestAllPosts ( int priority )
    {
        List<String> mysubs = this.getMySubs();

        for ( String cid : mysubs )
        {
            requestPosts ( cid, priority );
        }

    }

    public void requestPosts ( String comid, int priority )
    {
        //Get my ids.
        List<String> myids = getMyIds();
        Session s = null;
        //CObjList othersublist = index.getSubscriptions(comid);
        List<String> memlist = getMembers ( comid );

        try
        {
            s = session.getSession();

            for ( int c = 0; c < memlist.size(); c++ )
            {
                String id = null;
                String memid = memlist.get ( c );

                if ( memid != null && !myids.contains ( memid ) )
                {
                    id = Utils.mergeIds ( memid, comid );
                }

                if ( id != null )
                {
                    PostSequence pseq = new PostSequence ( session );
                    pseq.request ( s, id, priority, id, comid, memid );

                }

            }

            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

    }

    @SuppressWarnings ( "unchecked" )
    public void requestAllSubscriptions ( )
    {
        List<String> myids = getMyIds();
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.mine = false" );
            List<String> ids = new LinkedList<String>();
            List<IdentityData> idl = q.list();

            for ( IdentityData i : idl )
            {
                String id = i.getId();

                if ( id != null && !myids.contains ( id ) )
                {
                    ids.add ( i.getId() );
                }

            }

            for ( String i : ids )
            {
                SubSequence mseq = new SubSequence ( session );
                mseq.request ( s, i, 5, i );

            }

            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

    }

    private List<String> getMembers ( String comid )
    {
        List<String> r = new LinkedList<String>();
        CObj c = index.getCommunity ( comid );

        if ( c != null )
        {
            if ( CObj.SCOPE_PUBLIC.equals ( c.getString ( CObj.SCOPE ) ) )
            {
                CObjList clst = index.getIdentities();

                for ( int ct = 0; ct < clst.size(); ct++ )
                {
                    try
                    {
                        CObj idt = clst.get ( ct );
                        r.add ( idt.getId() );
                    }

                    catch ( IOException e )
                    {
                        //e.printStackTrace();
                    }

                }

                clst.close();
            }

            else
            {
                String cid = c.getString ( CObj.CREATOR );

                if ( cid != null )
                {
                    r.add ( cid );
                }

                CObjList mlst = index.getMemberships ( comid, null );

                for ( int ct = 0; ct < mlst.size(); ct++ )
                {
                    try
                    {
                        String mid = mlst.get ( ct ).getPrivate ( CObj.MEMBERID );

                        if ( mid != null )
                        {
                            r.add ( mid );
                        }

                    }

                    catch ( Exception e )
                    {
                        //e.printStackTrace();
                    }

                }

                mlst.close();
            }

        }

        return r;
    }

    public IdentityData getIdentityData ( String id )
    {
        Session s = null;
        IdentityData d = null;

        try
        {
            s = session.getSession();
            d = ( IdentityData ) s.get ( IdentityData.class, id );
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

        return d;
    }

    public PrivateMsgIdentity getPrvMsgIdentityData ( String id )
    {
        Session s = null;
        PrivateMsgIdentity d = null;

        try
        {
            s = session.getSession();
            d = ( PrivateMsgIdentity ) s.get ( PrivateMsgIdentity.class, id );
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

        return d;
    }

    public CommunityMember getCommunityMember ( String id )
    {
        Session s = null;
        CommunityMember d = null;

        try
        {
            s = session.getSession();
            d = ( CommunityMember ) s.get ( CommunityMember.class, id );
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

        return d;
    }

    public CommunityMyMember getCommunityMyMember ( String id )
    {
        Session s = null;
        CommunityMyMember d = null;

        try
        {
            s = session.getSession();
            d = ( CommunityMyMember ) s.get ( CommunityMyMember.class, id );
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        }

        return d;
    }

}
