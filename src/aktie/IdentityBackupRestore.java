package aktie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.lucene.search.Sort;
import org.hibernate.Session;
import org.json.JSONObject;
import org.json.JSONTokener;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.CommunityMyMember;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.data.PrivateMsgIdentity;
import aktie.gui.SWTApp;
import aktie.gui.Wrapper;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.user.IdentityManager;

public class IdentityBackupRestore
{

    public static long SEQADDER = 1000000;

    private Index index;
    private HH2Session session;
    private File destdir;
    private IdentityManager identManager;

    public static void main ( String args[] )
    {
        IdentityBackupRestore bs = new IdentityBackupRestore();

        try
        {
            File bak = new File ( "aktie_identity_backup.dat" );
            String dest = Wrapper.NODEDIR + File.separator + "i2p";

            if ( SWTApp.TESTNODE )
            {
                dest = Wrapper.NODEDIR;
            }

            bs.init ( Wrapper.NODEDIR, dest );

            if ( args.length == 0 || ( !"restore".equals ( args[0] ) ) )
            {
                if ( args.length > 0 )
                {
                    bak = new File ( args[0] );
                }

                bs.saveIdentity ( bak );
            }

            else
            {
                if ( args.length > 1 )
                {
                    bak = new File ( args[1] );
                }

                bs.loadIdentity ( bak );
            }

        }

        catch ( IOException e )
        {
            e.printStackTrace();
        }

    }

    public void close()
    {
        index.close();
        session.close();
    }

    //nodedir + File.separator + "i2p"
    public void init ( String nd, String dd ) throws IOException
    {
        File idxdir = new File ( nd + File.separator + "index" );
        index = new Index();
        index.setIndexdir ( idxdir );
        index.init();

        session = new HH2Session();
        session.init ( nd + File.separator + "h2" );

        destdir = new File ( dd );

        identManager = new IdentityManager ( session, index );
    }

    private void saveIdentity ( CObj i, long coms, long mems, long subs, long glbs, long pid, long pmsg, File tf )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            IdentityData id = ( IdentityData ) s.get ( IdentityData.class, i.getId() );

            if ( id == null )
            {
                id = new IdentityData();
                id.setId ( i.getId() );
                id.setFirstSeen ( ( new Date() ).getTime() );
                id.setMine ( true );
                id.setLastCommunityNumber ( coms + SEQADDER );
                id.setLastMembershipNumber ( mems + SEQADDER );
                id.setLastSubNumber ( subs + SEQADDER );
                id.setLastPubGlobalSequence ( glbs + SEQADDER );
                s.merge ( id );
            }

            PrivateMsgIdentity pmid = ( PrivateMsgIdentity ) s.get ( PrivateMsgIdentity.class, i.getId() );

            if ( pmid == null )
            {
                pmid = new PrivateMsgIdentity();
                pmid.setId ( i.getId() );
                pmid.setLastIdentNumber ( pid + SEQADDER );
                pmid.setLastMsgNumber ( pmsg + SEQADDER );
                s.merge ( pmid );
            }

            s.getTransaction().commit();
            i.pushPrivate ( CObj.DEST, tf.getPath() );
            index.index ( i, true );
        }

        catch ( Exception e )
        {
            if ( s != null )
            {
                if ( s.getTransaction().isActive() )
                {
                    s.getTransaction().rollback();
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
                }

            }

        }

    }

    private void saveCommunityMember ( String creator, String comid, long lastfile, long lastpost )
    {
        String id = Utils.mergeIds ( creator, comid );
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            CommunityMember cm = ( CommunityMember ) s.get ( CommunityMember.class, id );

            if ( cm == null )
            {
                cm = new CommunityMember();
                cm.setId ( id );
                cm.setCommunityId ( comid );
                cm.setMemberId ( creator );
                cm.setLastFileNumber ( lastfile + SEQADDER );
                cm.setLastPostNumber ( lastpost + SEQADDER );
                s.merge ( cm );
            }

            s.getTransaction().commit();
        }

        catch ( Exception e )
        {
            if ( s != null )
            {
                if ( s.getTransaction().isActive() )
                {
                    s.getTransaction().rollback();
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
                }

            }

        }

    }

    private void saveMyCommunityMember ( CommunityMyMember mem )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            s.merge ( mem );
            s.getTransaction().commit();
        }

        catch ( Exception e )
        {
            if ( s != null )
            {
                if ( s.getTransaction().isActive() )
                {
                    s.getTransaction().rollback();
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
                }

            }

        }

    }

    public void loadIdentity ( File f ) throws IOException
    {
        BufferedReader br = new BufferedReader ( new FileReader ( f ) );
        String lstr = br.readLine();
        int len = Integer.valueOf ( lstr );

        for ( int c = 0; c < len; c++ )
        {
            JSONTokener p = new JSONTokener ( br );
            JSONObject o = new JSONObject ( p );
            CObj ident = new CObj();
            ident.LOADPRIVATEJSON ( o );
            lstr = br.readLine();
            long comseq = Long.valueOf ( lstr );
            lstr = br.readLine();
            long memseq = Long.valueOf ( lstr );
            lstr = br.readLine();
            long subseq = Long.valueOf ( lstr );
            lstr = br.readLine();
            long glbseq = Long.valueOf ( lstr );

            lstr = br.readLine();
            long lstid = Long.valueOf ( lstr );
            lstr = br.readLine();
            long lstmsg = Long.valueOf ( lstr );

            File df = File.createTempFile ( "dest", ".bin", destdir );
            FileOutputStream fos = new FileOutputStream ( df );
            lstr = br.readLine();
            long destsize = Long.valueOf ( lstr );

            for ( long b = 0; b < destsize; b++ )
            {
                int bt = br.read();
                fos.write ( bt );
            }

            fos.close();

            saveIdentity ( ident, comseq, memseq, subseq, glbseq, lstid, lstmsg, df );

            lstr = br.readLine();
            int nummembers = Integer.valueOf ( lstr );

            for ( int c1 = 0; c1 < nummembers; c1++ )
            {
                JSONTokener pt = new JSONTokener ( br );
                JSONObject ot = new JSONObject ( pt );
                CObj mem = new CObj();
                mem.LOADPRIVATEJSON ( ot );
                index.index ( mem, true );
            }

        }

        lstr = br.readLine();
        int nummem = Integer.valueOf ( lstr );

        for ( int c = 0; c < nummem; c++ )
        {
            CommunityMyMember mem = new CommunityMyMember();
            //String comid = commy.getCommunityId();
            //fw.write(comid + "\n");
            String comid = br.readLine();
            mem.setCommunityId ( comid );
            //String memid = commy.getMemberId();
            //fw.write(memid + "\n");
            String memid = br.readLine();
            mem.setMemberId ( memid );
            //String id = commy.getId();
            //fw.write(id + "\n");
            String id = br.readLine();
            mem.setId ( id );
            //String key = Utils.toString (commy.getKey());
            //fw.write(key + "\n");
            String keystr = br.readLine();
            mem.setKey ( Utils.toByteArray ( keystr ) );

            saveMyCommunityMember ( mem );

        }

        lstr = br.readLine();
        nummem = Integer.valueOf ( lstr );

        for ( int c = 0; c < nummem; c++ )
        {
            JSONTokener pt = new JSONTokener ( br );
            JSONObject ot = new JSONObject ( pt );
            CObj com = new CObj();
            com.LOADPRIVATEJSON ( ot );
        }

        lstr = br.readLine();
        int numsubs = Integer.valueOf ( lstr );

        for ( int c = 0; c < numsubs; c++ )
        {
            JSONTokener pt = new JSONTokener ( br );
            JSONObject ot = new JSONObject ( pt );
            CObj com = new CObj();
            com.LOADPRIVATEJSON ( ot );

            ot = new JSONObject ( pt );
            CObj sub = new CObj();
            sub.LOADPRIVATEJSON ( ot );

            index.index ( com, true );
            index.index ( sub, true );

            lstr = br.readLine();
            long lastfile = Long.valueOf ( lstr );
            lstr = br.readLine();
            long lastpost = Long.valueOf ( lstr );
            saveCommunityMember ( sub.getString ( CObj.CREATOR ),
                                  sub.getString ( CObj.COMMUNITYID ), lastfile, lastpost );
        }

        lstr = br.readLine();
        int numid = Integer.valueOf ( lstr );

        for ( int c = 0; c < numid; c++ )
        {
            JSONTokener pt = new JSONTokener ( br );
            JSONObject ot = new JSONObject ( pt );
            CObj pm = new CObj();
            pm.LOADPRIVATEJSON ( ot );
            index.index ( pm, true );
        }

        br.close();
    }

    private IdentityData getIdentityData ( String id )
    {
        IdentityData idat = null;
        Session s = null;

        try
        {
            s = session.getSession();
            idat = ( IdentityData ) s.get ( IdentityData.class, id );
        }

        catch ( Exception e )
        {
            if ( s != null )
            {
                if ( s.getTransaction().isActive() )
                {
                    s.getTransaction().rollback();
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
                }

            }

        }

        return idat;
    }

    private PrivateMsgIdentity getPrivateMsgIdentData ( String creator )
    {
        PrivateMsgIdentity idat = null;
        Session s = null;

        try
        {
            s = session.getSession();
            idat = ( PrivateMsgIdentity ) s.get ( PrivateMsgIdentity.class, creator );
        }

        catch ( Exception e )
        {
            if ( s != null )
            {
                if ( s.getTransaction().isActive() )
                {
                    s.getTransaction().rollback();
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
                }

            }

        }

        return idat;
    }

    private CommunityMember getCommunityMember ( String creator, String comid )
    {
        String id = Utils.mergeIds ( creator, comid );

        CommunityMember idat = null;
        Session s = null;

        try
        {
            s = session.getSession();
            idat = ( CommunityMember ) s.get ( CommunityMember.class, id );
        }

        catch ( Exception e )
        {
            if ( s != null )
            {
                if ( s.getTransaction().isActive() )
                {
                    s.getTransaction().rollback();
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
                }

            }

        }

        return idat;
    }

    public void saveIdentity ( File f ) throws IOException
    {
        FileWriter fw = new FileWriter ( f );

        CObjList clst = index.getMyIdentities();
        fw.write ( clst.size() + "\n" );

        for ( int c = 0; c < clst.size(); c++ )
        {
            CObj co = clst.get ( c );
            JSONObject jo = co.GETPRIVATEJSON();
            jo.write ( fw );

            IdentityData idat = getIdentityData ( co.getId() );
            fw.write ( idat.getLastCommunityNumber() + "\n" );
            fw.write ( idat.getLastMembershipNumber() + "\n" );
            fw.write ( idat.getLastSubNumber() + "\n" );
            fw.write ( idat.getLastPubGlobalSequence() + "\n" );

            PrivateMsgIdentity pid = getPrivateMsgIdentData ( co.getId() );

            if ( pid != null )
            {
                fw.write ( pid.getLastIdentNumber() + "\n" );
                fw.write ( pid.getLastMsgNumber() + "\n" );
            }

            else
            {
                fw.write ( "0\n" );
                fw.write ( "0\n" );
            }

            String destfile = co.getPrivate ( CObj.DEST );
            File df = new File ( destfile );
            fw.write ( df.length() + "\n" );
            FileInputStream fis = new FileInputStream ( df );
            int r = fis.read();

            while ( r > -1 )
            {
                fw.write ( r );
                r = fis.read();
            }

            fis.close();

            CObjList mlst = index.getIdentityMemberships ( co.getId() );
            fw.write ( mlst.size() + "\n" );

            for ( int c0 = 0; c0 < mlst.size(); c0++ )
            {
                CObj mo = mlst.get ( c0 );
                JSONObject jo2 = mo.GETPRIVATEJSON();
                jo2.write ( fw );
            }

            mlst.close();

        }

        clst.close();

        List<CommunityMyMember> mylst = identManager.getMyMemberships();
        fw.write ( mylst.size() + "\n" );

        for ( int c = 0; c < mylst.size(); c++ )
        {
            CommunityMyMember commy = mylst.get ( c );
            String comid = commy.getCommunityId();
            fw.write ( comid + "\n" );
            String memid = commy.getMemberId();
            fw.write ( memid + "\n" );
            String id = commy.getId();
            fw.write ( id + "\n" );
            String key = Utils.toString ( commy.getKey() );
            fw.write ( key + "\n" );
        }

        clst = index.getMyMemberships ( ( Sort ) null );
        fw.write ( clst.size() + "\n" );

        for ( int c = 0; c < clst.size(); c++ )
        {
            CObj com = clst.get ( c );
            JSONObject jo2 = com.GETPRIVATEJSON();
            jo2.write ( fw );
        }

        clst.close();

        clst = index.getMySubscriptions();
        fw.write ( clst.size() + "\n" );

        for ( int c = 0; c < clst.size(); c++ )
        {
            CObj sub = clst.get ( c );
            CObj com = index.getCommunity ( sub.getString ( CObj.COMMUNITYID ) );
            JSONObject co = com.GETPRIVATEJSON();
            co.write ( fw );
            JSONObject so = sub.GETPRIVATEJSON();
            so.write ( fw );
            CommunityMember cm = getCommunityMember ( sub.getString ( CObj.CREATOR ),
                                 sub.getString ( CObj.COMMUNITYID ) );

            if ( cm != null )
            {
                fw.write ( cm.getLastFileNumber() + "\n" );
                fw.write ( cm.getLastPostNumber() + "\n" );
            }

            else
            {
                fw.write ( "0\n" );
                fw.write ( "0\n" );
            }

        }

        clst.close();

        clst = index.getAllPrivIdents();
        fw.write ( clst.size() + "\n" );

        for ( int c = 0; c < clst.size(); c++ )
        {
            CObj co = clst.get ( c );
            JSONObject jo = co.GETPRIVATEJSON();
            jo.write ( fw );
        }

        clst.close();

        fw.close();
    }

}
