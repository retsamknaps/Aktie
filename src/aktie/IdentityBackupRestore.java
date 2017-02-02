package aktie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.hibernate.Session;
import org.json.JSONObject;
import org.json.JSONTokener;

import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.index.CObjList;
import aktie.index.Index;

public class IdentityBackupRestore
{

    public static long SEQADDER = 1000000;

    private Index index;
    private HH2Session session;
    private File destdir;

    private void saveIdentity ( CObj i, long coms, long mems, long subs, long glbs, File tf )
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

            saveIdentity ( ident, comseq, memseq, subseq, glbseq, df );

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
        }

        clst.close();
        fw.close();
    }

}
