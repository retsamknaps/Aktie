package aktie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;
import org.json.JSONTokener;

import aktie.data.CObj;
import aktie.i2p.I2PNet;
import aktie.index.CObjList;
import aktie.index.Upgrade0301;
import aktie.index.Upgrade0405;
import aktie.net.ConnectionListener;
import aktie.net.Net;
import aktie.net.RawNet;
import aktie.upgrade.UpgradeControllerCallback;

public class StandardI2PNode
{

    public static boolean TESTNODE = false;

    private String nodeDir;
    private Node node;
    private IdentityCache idCache;
    private I2PNet i2pnet;

    public IdentityCache getIdCache()
    {
        return idCache;
    }

    public Node getNode()
    {
        return node;
    }

    public void bootNode ( String nodedir, Properties p, UpdateCallback uc,
                           UpdateCallback nc, ConnectionListener cc,
                           UpgradeControllerCallback ug )
    {

        nodeDir = nodedir;

        if ( !isSameOrNewer() )
        {
            //Something went wrong on upgrade
            deleteVersionAndExit();
        }

        preStartUpdate();

        Net net = null;

        try
        {
            if ( !TESTNODE )
            {
                i2pnet = new I2PNet ( nodeDir, p );
                i2pnet.waitUntilReady();
                net = i2pnet;
            }

            else
            {
                net = new RawNet ( new File ( nodeDir ) );
            }

            node = new Node ( nodeDir, net, uc,
                              nc, cc, ug );

            idCache = new IdentityCache ( node.getIndex() );

            updateAfterNodeStart();

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            failedToStart();
        }

    }

    private void failedToStart()
    {
        System.exit ( 1 );
    }

    public void nodeStarted()
    {
        try
        {
            CObjList mlst = node.getIndex().getMyIdentities();

            if ( mlst.size() == 0 )
            {
                CObj co = new CObj();
                co.setType ( CObj.IDENTITY );
                co.pushString ( CObj.NAME, "anon" );
                node.enqueue ( co );

                if ( !TESTNODE )
                {
                    loadDefaults();
                }

            }

            else
            {
                for ( int c = 0; c < mlst.size(); c++ )
                {
                    CObj mc = mlst.get ( c );
                    node.getUsrCallback().update ( mc );
                }

            }

            mlst.close();

            if ( !TESTNODE )
            {
                File devid = new File ( nodeDir + File.separator + "developerid.dat" );

                if ( devid.exists() )
                {
                    loadDeveloperIdentity ( devid );
                }

            }

            mlst = node.getIndex().getMySubscriptions();

            for ( int c = 0; c < mlst.size(); c++ )
            {
                node.getUsrCallback().update ( mlst.get ( c ) );
            }

            mlst.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        if ( Wrapper.getStartDestinationsOnStartup() )
        {
            node.startDestinations ( Wrapper.getStartDestinationDelay() );
        }

        saveVersionFile();

        CObj u = new CObj();
        u.setType ( CObj.USR_SPAMEX_UPDATE );
        node.enqueue ( u );

    }

    public void loadSeed ( File f )
    {
        BufferedReader br = null;

        try
        {
            br = new BufferedReader ( new FileReader ( f ) );
            JSONTokener p = new JSONTokener ( br );
            JSONObject o = new JSONObject ( p );
            CObjList uplst = new CObjList();

            while ( o != null )
            {
                CObj co = new CObj();
                co.loadJSON ( o );

                if ( CObj.IDENTITY.equals ( co.getType() ) )
                {
                    co.setType ( CObj.USR_SEED );
                    uplst.add ( co );
                }

                try
                {
                    o = new JSONObject ( p );
                }

                catch ( Exception xr )
                {
                    o = null;
                }

            }

            br.close();
            node.enqueue ( uplst );
            CObj ns = new CObj();
            ns.setType ( CObj.USR_FORCE_SEARCHER );
            node.enqueue ( ns );
        }

        catch ( Exception e )
        {
            e.printStackTrace();

            if ( br != null )
            {
                try
                {
                    br.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    public void loadSpamEx ( File f )
    {
        BufferedReader br = null;

        try
        {
            br = new BufferedReader ( new FileReader ( f ) );
            JSONTokener p = new JSONTokener ( br );
            JSONObject o = new JSONObject ( p );

            CObjList uplst = new CObjList();

            while ( o != null )
            {
                CObj co = new CObj();
                co.loadJSON ( o );

                if ( CObj.SPAMEXCEPTION.equals ( co.getType() ) )
                {
                    co.setType ( CObj.USR_SPAMEX );
                    uplst.add ( co );
                }

                try
                {
                    o = new JSONObject ( p );
                }

                catch ( Exception xr )
                {
                    o = null;
                }

            }

            br.close();
            node.enqueue ( uplst );
            CObj ns = new CObj();
            ns.setType ( CObj.USR_FORCE_SEARCHER );
            node.enqueue ( ns );
        }

        catch ( Exception e )
        {
            e.printStackTrace();

            if ( br != null )
            {
                try
                {
                    br.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    public void setI2PProps ( Properties p )
    {
        if ( i2pnet != null )
        {
            i2pnet.setProperties ( p );
            List<CObj> myonlist = new LinkedList<CObj>();
            CObjList myids = node.getIndex().getMyIdentities();

            for ( int c = 0; c < myids.size(); c++ )
            {
                try
                {
                    CObj ido = myids.get ( c );

                    if ( ido.getPrivateNumber ( CObj.PRV_DEST_OPEN ) == null ||
                            ido.getPrivateNumber ( CObj.PRV_DEST_OPEN ) == 1L )
                    {
                        myonlist.add ( ido );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            myids.close();

            for ( CObj mid : myonlist )
            {
                CObj off = mid.clone();
                off.setType ( CObj.USR_START_DEST );
                off.pushPrivateNumber ( CObj.PRV_DEST_OPEN, 0L );
                node.enqueue ( off );
            }

            for ( CObj mid : myonlist )
            {
                CObj on = mid.clone();
                on.setType ( CObj.USR_START_DEST );
                on.pushPrivateNumber ( CObj.PRV_DEST_OPEN, 1L );
                node.enqueue ( on );
            }

        }

    }

    public void closeNode()
    {
        node.close();

        if ( i2pnet != null )
        {
            i2pnet.exit();
        }

    }

    private void preStartUpdate()
    {

        String lastversion = lastVersion();

        if ( lastversion != null )
        {
            upgrade0301 ( lastversion );
            upgrade0405 ( lastversion );
            upgrade0418 ( lastversion );
        }

    }

    private void updateAfterNodeStart()
    {
        String lastversion = lastVersion();

        if ( lastversion != null )
        {
        }

    }

    public String lastVersion()
    {
        File vf = new File ( nodeDir + File.separator + Wrapper.VERSION_FILE );

        if ( vf.exists() )
        {
            try
            {
                FileReader fr = new FileReader ( vf );
                BufferedReader br = new BufferedReader ( fr );
                String vl = br.readLine();
                br.close();

                return vl;
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        return null;
    }

    private void upgrade0301 ( String lastversion )
    {
        if ( Wrapper.compareVersions ( lastversion, Wrapper.VERSION_0403 ) < 0 )
        {
            Upgrade0301.upgrade ( nodeDir + File.separator + "index" );
        }

    }

    private void upgrade0405 ( String lastversion )
    {
        if ( Wrapper.compareVersions ( lastversion, Wrapper.VERSION_0405 ) < 0 )
        {
            Upgrade0405.upgrade ( nodeDir + File.separator + "index" );
        }

    }

    private void upgrade0418 ( String lastversion )
    {
        if ( Wrapper.compareVersions ( lastversion, Wrapper.VERSION_0418 ) < 0 )
        {
            Upgrade0405.upgrade ( nodeDir + File.separator + "index" );
        }

    }

    private void saveVersionFile()
    {
        try
        {
            File vf = new File ( nodeDir + File.separator + Wrapper.VERSION_FILE );
            FileOutputStream fos = new FileOutputStream ( vf );
            PrintWriter pw = new PrintWriter ( fos );
            pw.println ( Wrapper.VERSION );
            pw.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    private void loadDefaults()
    {
        //Load default seed file.
        File defseedfile = new File ( nodeDir + File.separator + "defseed.dat" );

        if ( defseedfile.exists() )
        {
            loadSeed ( defseedfile );
        }

        File spamex = new File ( nodeDir + File.separator + "spamex.dat" );

        if ( spamex.exists() )
        {
            loadSpamEx ( spamex );
        }

        //Load default communities and subscribe.
        File defcomfile = new File ( nodeDir + File.separator + "defcom.dat" );

        if ( defcomfile.exists() )
        {
            loadDefCommunitySubs ( defcomfile );
        }

    }

    private void loadDefCommunitySubs ( File f )
    {
        BufferedReader br = null;
        List<CObj> comlst = new LinkedList<CObj>();

        try
        {
            br = new BufferedReader ( new FileReader ( f ) );
            JSONTokener p = new JSONTokener ( br );
            JSONObject o = new JSONObject ( p );
            CObjList colst = new CObjList();

            while ( o != null )
            {
                CObj co = new CObj();
                co.loadJSON ( o );

                if ( CObj.COMMUNITY.equals ( co.getType() ) )
                {
                    co.setType ( CObj.USR_COMMUNITY );
                    comlst.add ( co );
                    colst.add ( co );
                }

                try
                {
                    o = new JSONObject ( p );
                }

                catch ( Exception re )
                {
                    o = null;
                }

            }

            br.close();
            node.enqueue ( colst );
        }

        catch ( Exception e )
        {
            if ( br != null )
            {
                try
                {
                    br.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        if ( comlst.size() > 0 )
        {
            new DefComSubThread ( node, comlst );
        }

    }

    private void loadDeveloperIdentity ( File f )
    {
        BufferedReader br = null;

        try
        {
            br = new BufferedReader ( new FileReader ( f ) );
            JSONTokener p = new JSONTokener ( br );
            JSONObject o = new JSONObject ( p );

            while ( o != null )
            {
                CObj co = new CObj();
                co.loadJSON ( o );
                node.getUpgrader().addDeveloperId ( co.getId() );
                node.newDeveloperIdentity ( co.getId() );

                try
                {
                    o = new JSONObject ( p );
                }

                catch ( Exception xr )
                {
                    o = null;
                }

            }

            br.close();
        }

        catch ( Exception e )
        {
            if ( br != null )
            {
                try
                {
                    br.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    private boolean isSameOrNewer()
    {
        String vl = lastVersion();

        if ( Wrapper.compareVersions ( vl, Wrapper.VERSION ) > 0 )
        {
            return false;
        }

        return true;
    }

    private void deleteVersionAndExit()
    {
        File vf = new File ( nodeDir + File.separator + Wrapper.VERSION_FILE );

        if ( vf.exists() )
        {
            vf.delete();
        }

        System.exit ( Wrapper.RESTART_RC );
    }

}
