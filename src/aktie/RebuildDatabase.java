package aktie;

import java.io.File;
import java.io.IOException;

import org.hibernate.Session;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.CommunityMyMember;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.data.PrivateMsgIdentity;
import aktie.index.CObjList;
import aktie.index.Index;

/**
    In the event of H2 database corruption, we can rebuild it from
    the Index data!
    For 6A
*/
public class RebuildDatabase
{

    private Index index;
    private HH2Session session;

    public static void main ( String args[] )
    {
        if ( args.length < 1 )
        {
            System.out.println ( "Node dir must be specified." );
            System.exit ( 1 );
        }

        String nodedir = args[0];
        RebuildDatabase r = new RebuildDatabase();
        String h2dir = nodedir + File.separator + "h2";
        String iddir = nodedir + File.separator + "index";
        File h2 = new File ( h2dir );
        File id = new File ( iddir );

        if ( !h2.exists() || !h2.isDirectory() || !id.exists() || !id.isDirectory() )
        {
            System.out.println ( "You need to be in your aktie_run_dir" );
            System.exit ( 1 );
        }

        r.rebuild ( h2dir, iddir );
        r.close();
    }

    private void backupfile ( String str )
    {
        File tf = new File ( str );

        if ( tf.exists() )
        {
            File f = new File ( str );
            int idx = 0;

            while ( f.exists() )
            {
                f = new File ( str + "." + idx );
                idx++;
            }

            tf.renameTo ( f );
            tf = new File ( str );

            if ( tf.exists() )
            {
                throw new RuntimeException ( "Failed to rename file. " + str );
            }

        }

    }

    public void rebuild ( String dbdir, String idxdir )
    {
        String dbstr = dbdir + File.separator + "data.h2.db";
        backupfile ( dbstr );
        dbstr = dbdir + File.separator + "data.mv.db";
        backupfile ( dbstr );

        session = new HH2Session();
        session.init ( dbdir );

        index = new Index();
        index.setIndexdir ( new File ( idxdir ) );

        try
        {
            index.init();
            buildIdentityData();
            buildCommunityMyMember();
            buildCommunityMember();
            buildPrivateMsgIdentity();
        }

        catch ( IOException e )
        {
            e.printStackTrace();
        }

    }

    public void close()
    {
        session.close();
        index.close();
    }

    private void buildPrivateMsgIdentity()
    {
        CObjList col = index.getIdentities();
        Session s = session.getSession();

        try
        {
            s.getTransaction().begin();

            for ( int c = 0; c < col.size(); c++ )
            {
                CObj o = col.get ( c );
                PrivateMsgIdentity mi = new PrivateMsgIdentity();
                mi.setId ( o.getId() );
                CObj lst = index.getLastCreated ( CObj.PRIVIDENTIFIER, o.getId() );

                if ( lst != null )
                {
                    mi.setLastIdentNumber ( lst.getNumber ( CObj.SEQNUM ) );
                }

                lst = index.getLastCreated ( CObj.PRIVMESSAGE, o.getId() );

                if ( lst != null )
                {
                    mi.setLastMsgNumber ( lst.getNumber ( CObj.SEQNUM ) );
                }

                mi.setMine ( "true".equals ( o.getPrivate ( CObj.MINE ) ) );
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

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

        }

        col.close();
    }

    private CommunityMember addCommunityMember ( Session s, String creator, String comid )
    {
        String id = Utils.mergeIds ( creator, comid );
        CommunityMember m = ( CommunityMember ) s.get ( CommunityMember.class, id );

        if ( m == null )
        {
            m = new CommunityMember();
        }

        m.setId ( id );
        m.setCommunityId ( comid );
        m.setMemberId ( creator );
        return m;
    }

    private void buildCommunityMember()
    {
        CObjList col = index.getAllSubscriptions();
        Session s = session.getSession();

        try
        {
            s.getTransaction().begin();

            for ( int c = 0; c < col.size(); c++ )
            {
                CObj sb = col.get ( c );
                String creator = sb.getString ( CObj.CREATOR );
                String comid = sb.getString ( CObj.COMMUNITYID );

                if ( creator != null && comid != null )
                {
                    CommunityMember cm = addCommunityMember ( s, creator, comid );
                    s.merge ( cm );
                }

            }

            col.close();
            col = index.getAllHasFiles();

            for ( int c = 0; c < col.size(); c++ )
            {
                CObj o = col.get ( c );
                String creator = o.getString ( CObj.CREATOR );
                String comid = o.getString ( CObj.COMMUNITYID );
                Long seq = o.getNumber ( CObj.SEQNUM );

                if ( creator != null && comid != null && seq != null )
                {
                    CommunityMember cm = addCommunityMember ( s, creator, comid );

                    if ( seq > cm.getLastFileNumber() )
                    {
                        cm.setLastFileNumber ( seq );
                    }

                    s.merge ( cm );
                }

            }

            col.close();
            col = index.getAllPosts();

            for ( int c = 0; c < col.size(); c++ )
            {
                CObj o = col.get ( c );
                String creator = o.getString ( CObj.CREATOR );
                String comid = o.getString ( CObj.COMMUNITYID );
                Long seq = o.getNumber ( CObj.SEQNUM );

                if ( creator != null && comid != null && seq != null )
                {
                    CommunityMember cm = addCommunityMember ( s, creator, comid );

                    if ( seq > cm.getLastPostNumber() )
                    {
                        cm.setLastPostNumber ( seq );
                    }

                    s.merge ( cm );
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

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

        }

        col.close();
    }

    private void buildCommunityMyMember()
    {
        Session s = session.getSession();
        CObjList col = index.getMyIdentities();
        CObjList col2 = null;

        try
        {
            s.getTransaction().begin();

            for ( int c = 0; c < col.size(); c++ )
            {
                CObj co = col.get ( c );
                col2 = index.getIdentityMemberships ( co.getId() );

                for ( int c2 = 0; c2 < col2.size(); c2++ )
                {
                    CObj b = col2.get ( c2 );
                    CommunityMyMember mm = new CommunityMyMember();
                    String comid = b.getPrivate ( CObj.COMMUNITYID );
                    String memid = b.getPrivate ( CObj.MEMBERID );
                    String key = b.getPrivate ( CObj.KEY );

                    if ( comid != null && memid != null && key != null )
                    {
                        mm.setId ( b.getDig() );
                        mm.setCommunityId ( comid );
                        mm.setMemberId ( memid );
                        mm.setKey ( Utils.toByteArray ( key ) );
                        s.merge ( mm );
                    }

                }

                col2.close();
                col2 = index.getIdentityPrivateCommunities ( co.getId() );

                for ( int c2 = 0; c2 < col2.size(); c2++ )
                {
                    CObj b = col2.get ( c2 );
                    CommunityMyMember mm = new CommunityMyMember();
                    String comid = b.getDig();
                    String memid = co.getId();
                    String key = b.getPrivate ( CObj.KEY );

                    if ( comid != null && memid != null && key != null )
                    {
                        mm.setId ( b.getDig() );
                        mm.setCommunityId ( comid );
                        mm.setMemberId ( memid );
                        mm.setKey ( Utils.toByteArray ( key ) );
                        s.merge ( mm );
                    }

                }

                col2.close();
                col2 = null;
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

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

        }

        col.close();

        if ( col2 != null )
        {
            col2.close();
        }

    }

    private void buildIdentityData()
    {
        Session s = session.getSession();
        CObjList col = index.getIdentities();

        try
        {
            s.getTransaction().begin();

            for ( int c = 0; c < col.size(); c++ )
            {
                CObj co = col.get ( c );

                IdentityData id = new IdentityData();
                id.setId ( co.getId() );
                id.setMine ( "true".equals ( co.getPrivate ( CObj.MINE ) ) );

                CObj lst = index.getLastCreated ( CObj.COMMUNITY, co.getId() );

                if ( lst != null )
                {
                    id.setLastCommunityNumber ( lst.getNumber ( CObj.SEQNUM ) );
                }

                lst = index.getLastCreated ( CObj.MEMBERSHIP, co.getId() );

                if ( lst != null )
                {
                    id.setLastMembershipNumber ( lst.getNumber ( CObj.SEQNUM ) );
                }

                lst = index.getLastCreated ( CObj.SUBSCRIPTION, co.getId() );

                if ( lst != null )
                {
                    id.setLastSubNumber ( lst.getNumber ( CObj.SEQNUM ) );
                }

                if ( id.isMine() )
                {
                    lst = index.getLastGlobalSequence ( co.getId() );

                    if ( lst != null )
                    {
                        id.setLastPubGlobalSequence (
                            lst.getPrivateNumber ( CObj.getGlobalSeq ( co.getId() ) ) );
                    }

                }

                s.merge ( id );
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

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

        }

        col.close();
    }

}
