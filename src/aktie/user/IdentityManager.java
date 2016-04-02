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
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.index.CObjList;
import aktie.index.Index;

public class IdentityManager
{

    public static int MAX_LAST_NUMBER = 20;

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
            e.printStackTrace();

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
            e.printStackTrace();

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

        }

        return null;
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

        }

        return null;
    }

    @SuppressWarnings ( "unchecked" )
    public CommunityMember claimSubUpdate ( String thisid, Map<String, Integer> comids, int rereqperiod )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.subscriptionStatus = :st ORDER BY "
                                      + "x.subscriptionUpdatePriority DESC, "
                                      + "x.lastSubscriptionUpdate ASC" );
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
                    //If only 1 member (2 counting creator), then just always get
                    //update from other member.
                    if ( nummem <= 1 )
                    {
                        cm = c;
                    }

                    //More than 1(2) members, get update from someone other than
                    //same person got last one.  if updates have been requested
                    //a few times sense the last time we got updates then ok
                    else if ( ( !thisid.equals ( c.getLastSubscriptionUpdateFrom() ) ) ||
                              c.getSubscriptionUpdateCycle() > rereqperiod )
                    {
                        cm = c;
                    }

                }

            }

            if ( cm != null )
            {
                cm.setSubscriptionStatus ( CommunityMember.DONE );
                cm.setLastSubscriptionUpdate ( System.currentTimeMillis() );
                cm.setSubscriptionUpdateCycle ( 0 );
                cm.setLastSubscriptionUpdateFrom ( thisid );
                s.merge ( cm );
            }

            s.getTransaction().commit();
            s.close();
            return cm;
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
    public CommunityMember claimSubUpdate ( )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM CommunityMember x WHERE "
                                      + "x.subscriptionStatus = :st ORDER BY "
                                      + "x.subscriptionUpdatePriority DESC, "
                                      + "x.lastSubscriptionUpdate ASC" );
            q.setParameter ( "st", CommunityMember.UPDATE );
            //q.setMaxResults ( 100 );
            CommunityMember cm = null;
            List<CommunityMember> r = q.list();
            Iterator<CommunityMember> i = r.iterator();

            if ( i.hasNext() )
            {
                cm = i.next();
            }

            if ( cm != null )
            {
                cm.setSubscriptionStatus ( CommunityMember.DONE );
                cm.setLastSubscriptionUpdate ( System.currentTimeMillis() );
                cm.setSubscriptionUpdateCycle ( 0 );
                cm.setLastSubscriptionUpdateFrom ( "" );
                s.merge ( cm );
            }

            s.getTransaction().commit();
            s.close();
            return cm;
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

        }

        return null;
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

        }

        return null;
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
            e.printStackTrace();

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
            e.printStackTrace();

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
            e.printStackTrace();

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
            e.printStackTrace();

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
            e.printStackTrace();

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
                e.printStackTrace();
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
                e.printStackTrace();
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

        }

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
            e.printStackTrace();

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
    public void requestCommunities()
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

                if ( idat != null )
                {
                    if ( idat.getNextClosestCommunityNumber() >
                            idat.getLastCommunityNumber() &&
                            idat.getNumClosestCommunityNumber() > MAX_LAST_NUMBER )
                    {
                        idat.setLastCommunityNumber ( idat.getNextClosestCommunityNumber() );
                        idat.setNumClosestCommunityNumber ( 0 );
                    }

                    idat.setCommunityStatus ( IdentityData.UPDATE );
                    idat.setCommunityUpdateCycle ( idat.getCommunityUpdateCycle() + 1 );
                    s.merge ( idat );
                }

                s.getTransaction().commit();
            }

            s.close();
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
                s.getTransaction().begin();
                IdentityData idat = ( IdentityData ) s.get ( IdentityData.class, i );

                if ( idat != null )
                {
                    if ( idat.getNextClosestMembershipNumber() >
                            idat.getLastMembershipNumber() &&
                            idat.getNumClosestMembershipNumber() > MAX_LAST_NUMBER )
                    {
                        idat.setLastMembershipNumber ( idat.getNextClosestMembershipNumber() );
                        idat.setNumClosestMembershipNumber ( 0 );
                    }

                    idat.setMemberStatus ( IdentityData.UPDATE );
                    idat.setMemberUpdateCycle ( idat.getMemberUpdateCycle() + 1 );
                    s.merge ( idat );
                }

                s.getTransaction().commit();
            }

            s.close();
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
                    s.getTransaction().begin();
                    CommunityMember cm = ( CommunityMember ) s.get ( CommunityMember.class, id );

                    if ( cm != null )
                    {
                        if ( cm.getNextClosestFileNumber() >
                                cm.getLastFileNumber() &&
                                cm.getNumClosestFileNumber() > MAX_LAST_NUMBER )
                        {
                            cm.setLastFileNumber ( cm.getNextClosestFileNumber() );
                            cm.setNumClosestFileNumber ( 0 );
                        }

                        cm.setFileStatus ( CommunityMember.UPDATE );
                        cm.setFileUpdatePriority ( priority );
                        cm.setFileUpdateCycle ( cm.getFileUpdateCycle() + 1 );
                        s.merge ( cm );
                    }

                    else
                    {
                        cm = new CommunityMember();
                        cm.setId ( id );
                        cm.setCommunityId ( comid );
                        cm.setMemberId ( memid );
                        cm.setFileStatus ( CommunityMember.UPDATE );
                        cm.setFileUpdateCycle ( 1 );
                        cm.setFileUpdatePriority ( priority );
                        s.persist ( cm );
                    }

                    s.getTransaction().commit();
                }

            }

            s.close();
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
                    s.getTransaction().begin();
                    CommunityMember cm = ( CommunityMember ) s.get ( CommunityMember.class, id );

                    if ( cm != null )
                    {
                        if ( cm.getNextClosestPostNumber() >
                                cm.getLastPostNumber() &&
                                cm.getNumClosestPostNumber() > MAX_LAST_NUMBER )
                        {
                            cm.setLastPostNumber ( cm.getNextClosestPostNumber() );
                            cm.setNumClosestPostNumber ( 0 );
                        }

                        cm.setPostStatus ( CommunityMember.UPDATE );
                        cm.setPostUpdatePriority ( priority );
                        cm.setPostUpdateCycle ( cm.getPostUpdateCycle() + 1 );
                        s.merge ( cm );
                    }

                    else
                    {
                        cm = new CommunityMember();
                        cm.setId ( id );
                        cm.setCommunityId ( comid );
                        cm.setMemberId ( memid );
                        cm.setPostStatus ( CommunityMember.UPDATE );
                        cm.setPostUpdatePriority ( priority );
                        cm.setPostUpdateCycle ( 1 );
                        s.persist ( cm );
                    }

                    s.getTransaction().commit();
                }

            }

            s.close();
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

    public void requestAllSubscriptions ( int priority )
    {
        CObjList coml = index.getValidCommunities();

        for ( int c = 0; c < coml.size(); c++ )
        {
            try
            {
                CObj com = coml.get ( c );
                requestSubscriptions ( com.getDig(), priority );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        coml.close();
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
                        e.printStackTrace();
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
                        e.printStackTrace();
                    }

                }

                mlst.close();
            }

        }

        return r;
    }

    public void requestSubscriptions ( String comid, int priority )
    {
        List<String> myid = getMyIds();
        List<String> mems = getMembers ( comid );
        Session s = null;

        try
        {
            s = session.getSession();
            Iterator<String> i = mems.iterator();

            while ( i.hasNext() )
            {
                String id = null;
                String mid = i.next();

                if ( !myid.contains ( mid ) )
                {
                    id = Utils.mergeIds ( mid, comid );
                }

                if ( id != null )
                {
                    s.getTransaction().begin();
                    CommunityMember cm = ( CommunityMember ) s.get ( CommunityMember.class, id );

                    if ( cm != null )
                    {
                        cm.setSubscriptionStatus ( CommunityMember.UPDATE );
                        cm.setSubscriptionUpdatePriority ( priority );
                        cm.setSubscriptionUpdateCycle ( cm.getSubscriptionUpdateCycle() + 1 );
                        s.merge ( cm );
                    }

                    else
                    {
                        cm = new CommunityMember();
                        cm.setId ( id );
                        cm.setCommunityId ( comid );
                        cm.setMemberId ( mid );
                        cm.setSubscriptionStatus ( CommunityMember.UPDATE );
                        cm.setSubscriptionUpdatePriority ( priority );
                        cm.setSubscriptionUpdateCycle ( 1 );
                        s.persist ( cm );
                    }

                    s.getTransaction().commit();
                }

            }

            s.close();
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

}
