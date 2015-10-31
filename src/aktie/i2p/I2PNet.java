package aktie.i2p;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.util.Properties;

import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.DataHelper;
import net.i2p.data.router.RouterAddress;
import net.i2p.data.router.RouterInfo;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.RouterVersion;
import net.i2p.router.networkdb.kademlia.FloodfillNetworkDatabaseFacade;
import net.i2p.router.CommSystemFacade.Status;
import net.i2p.router.transport.TransportUtil;
import aktie.net.Destination;
import aktie.net.Net;

public class I2PNet  implements Net
{

    //Copypasta from ConfigNetHelper.java
    final static String PROP_I2NP_NTCP_HOSTNAME = "i2np.ntcp.hostname";
    final static String PROP_I2NP_NTCP_PORT = "i2np.ntcp.port";
    final static String PROP_I2NP_NTCP_AUTO_PORT = "i2np.ntcp.autoport";
    final static String PROP_I2NP_NTCP_AUTO_IP = "i2np.ntcp.autoip";

    private Properties customProps;

    private File i2pdir;
    private Router router;

    public void setProperties ( Properties p )
    {
        customProps = p;
    }

    public I2PNet ( String nodedir, Properties p )
    {
        customProps = p;

        i2pdir = new File ( nodedir + File.separator + "i2p" );

        if ( i2pdir.exists() )
        {
            i2pdir.mkdirs();
        }

        if ( !externHost() )
        {
            if ( !testClient() )
            {
                System.out.println ( "No i2p found.  Starting router." );
                startI2P ();
            }

            else
            {
                System.out.println ( "Router seems to be running already." );
            }

        }

        else
        {
            System.out.println ( "You have selected to use an external I2P router." );
        }

    }

    private boolean externHost()
    {
        if ( customProps != null )
        {
            String ext = customProps.getProperty ( "aktie.externali2p" );

            if ( "true".equals ( ext ) )
            {
                return true;
            }

        }

        return false;
    }

    private void startI2P ()
    {
        System.setProperty ( "java.net.preferIPv4Stack", "false" );
        System.setProperty ( "i2p.dir.base", i2pdir.getPath() );
        System.setProperty ( "loggerFilenameOverride", "log" + File.separator + "log-router.log" );
        router = new Router();
        router.setKillVMOnEnd ( false );
        router.runRouter();
    }

    public void exit()
    {
        if ( router != null )
        {
            router.shutdown ( Router.EXIT_HARD );
        }

    }

    @Override
    public Destination getExistingDestination ( File privateinfo )
    {
        String hst = "127.0.0.1";
        int port = 7654;
        Properties p = new Properties();

        if ( customProps == null )
        {
            p.setProperty ( "i2cp.tcp.host", hst );
            p.setProperty ( "i2cp.tcp.port", Integer.toString ( port ) );
            p.setProperty ( "inbound.nickname", "aktie" );
            p.setProperty ( "inbound.length", "2" );
            p.setProperty ( "outbound.length", "2" );
            p.setProperty ( "i2cp.tcp.host", "127.0.0.1" );
            p.setProperty ( "i2cp.tcp.port", "7654" );

        }

        else
        {
            p.putAll ( customProps );
            String h = customProps.getProperty ( "i2cp.tcp.host" );

            if ( h != null )
            {
                hst = h;
            }

            String pt = customProps.getProperty ( "i2cp.tcp.port" );

            if ( pt != null )
            {
                port = Integer.valueOf ( pt );
            }

        }

        try
        {
            I2PSocketManager manager = null;

            while ( manager == null )
            {
                FileInputStream fis = new FileInputStream ( privateinfo );
                manager = I2PSocketManagerFactory.createManager ( fis, hst, port, p );
                fis.close();

                if ( manager == null )
                {
                    System.out.println ( "Wating for socket manager for existing destination." );
                    Thread.sleep ( 1000 );
                }

            }

            return new I2PDestination ( i2pdir, manager );
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Destination getNewDestination()
    {
        String hst = "127.0.0.1";
        int port = 7654;
        Properties p = new Properties();

        if ( customProps == null )
        {
            p.setProperty ( "i2cp.tcp.host", hst );
            p.setProperty ( "i2cp.tcp.port", Integer.toString ( port ) );
            p.setProperty ( "inbound.nickname", "aktie" );
            p.setProperty ( "inbound.length", "2" );
            p.setProperty ( "outbound.length", "2" );
        }

        else
        {
            p.putAll ( customProps );
            String h = customProps.getProperty ( "i2cp.tcp.host" );

            if ( h != null )
            {
                hst = h;
            }

            String pt = customProps.getProperty ( "i2cp.tcp.port" );

            if ( pt != null )
            {
                port = Integer.valueOf ( pt );
            }

        }

        I2PSocketManager manager = null;

        while ( manager == null )
        {
            manager = I2PSocketManagerFactory.createManager ( hst, port, p );

            if ( manager == null )
            {
                System.out.println ( "Wating for socket manager for new destination." );

                try
                {
                    Thread.sleep ( 1000 );
                }

                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }

            }

        }

        return new I2PDestination ( i2pdir, manager );
    }


    public void waitUntilReady()
    {
        while ( !testClient() )
        {
            try
            {
                Thread.sleep ( 1000 );
            }

            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }

        }

    }

    public boolean testClient()
    {
        try
        {
            String hst = "127.0.0.1";
            int port = 7654;

            if ( customProps != null )
            {
                String h = customProps.getProperty ( "i2cp.tcp.host" );

                if ( h != null )
                {
                    hst = h;
                }

                String pt = customProps.getProperty ( "i2cp.tcp.port" );

                if ( pt != null )
                {
                    port = Integer.valueOf ( pt );
                }

            }

            Socket s = new Socket ( hst, port );
            s.close();
            return true;
        }

        catch ( Exception e )
        {
        }

        return false;
    }

    private String getRouterVersion()
    {
        return RouterVersion.FULL_VERSION;
    }

    private int getActivePeers()
    {
        if ( router != null )
        {
            RouterContext _context = router.getContext();

            if ( _context != null )
            {
                return _context.commSystem().countActivePeers();
            }

        }

        return 0;
    }

    //Copypasta from I2P code
    @Override
    public String getStatus()
    {
        if ( router != null )
        {
            RouterContext _context = router.getContext();

            if ( _context != null )
            {
                if ( _context.commSystem().isDummy() )
                { return "VM Comm System (" + getRouterVersion() + ")"; }

                if ( _context.router().getUptime() > 60 * 1000 && ( !_context.router().gracefulShutdownInProgress() ) &&
                        !_context.clientManager().isAlive() )
                { return "I2P: ERR-Client Manager I2CP Error - check logs (" + getRouterVersion() + ")"; }  // not a router problem but the user should know

                // Warn based on actual skew from peers, not update status, so if we successfully offset
                // the clock, we don't complain.
                //if (!_context.clock().getUpdatedSuccessfully())
                long skew = _context.commSystem().getFramedAveragePeerClockSkew ( 33 );

                // Display the actual skew, not the offset
                if ( Math.abs ( skew ) > 30 * 1000 )
                {
                    return "I2P: ERR-Clock Skew of " + DataHelper.formatDuration2 ( Math.abs ( skew ) ) + " (" +
                           getRouterVersion() + ")";
                }

                if ( _context.router().isHidden() )
                { return "I2P: Hidden (" + getRouterVersion() + ")"; }

                RouterInfo routerInfo = _context.router().getRouterInfo();

                if ( routerInfo == null )
                { return "I2P: Testing (" + getRouterVersion() + ")"; }

                Status status = _context.commSystem().getStatus();

                switch ( status )
                {
                case OK:
                case IPV4_OK_IPV6_UNKNOWN:
                case IPV4_OK_IPV6_FIREWALLED:
                case IPV4_UNKNOWN_IPV6_OK:
                case IPV4_DISABLED_IPV6_OK:
                case IPV4_SNAT_IPV6_OK:
                    RouterAddress ra = routerInfo.getTargetAddress ( "NTCP" );

                    if ( ra == null )
                    { return status.toStatusString() + " (" + getRouterVersion() + ")"; }

                    byte[] ip = ra.getIP();

                    if ( ip == null )
                    { return "I2P: ERR-Unresolved TCP Address (" + getRouterVersion() + ")"; }

                    // TODO set IPv6 arg based on configuration?
                    if ( TransportUtil.isPubliclyRoutable ( ip, true ) )
                    { return status.toStatusString() + " (" + getRouterVersion() + ")"; }

                    return "I2P: ERR-Private TCP Address (" + getRouterVersion() + ")";

                case IPV4_SNAT_IPV6_UNKNOWN:
                case DIFFERENT:
                    return "I2P: ERR-SymmetricNAT (" + getRouterVersion() + ")";

                case REJECT_UNSOLICITED:
                case IPV4_DISABLED_IPV6_FIREWALLED:
                    if ( routerInfo.getTargetAddress ( "NTCP" ) != null )
                    { return "I2P: WARN-Firewalled with Inbound TCP Enabled (" + getRouterVersion() + ")"; }

                // fall through...
                case IPV4_FIREWALLED_IPV6_OK:
                case IPV4_FIREWALLED_IPV6_UNKNOWN:
                    if ( ( ( FloodfillNetworkDatabaseFacade ) _context.netDb() ).floodfillEnabled() )
                    { return "I2P: WARN-Firewalled and Floodfill (" + getRouterVersion() + ")"; }

                    //if (_context.router().getRouterInfo().getCapabilities().indexOf('O') >= 0)
                    //    return "I2P: WARN-Firewalled and Fast (" + getRouterVersion() + ")";
                    return status.toStatusString() + " (" + getRouterVersion() + ")";

                case DISCONNECTED:
                    return "I2P: Disconnected - check network cable (" + getRouterVersion() + ")";

                case HOSED:
                    return "I2P: ERR-UDP Port In Use - Set i2np.udp.internalPort=xxxx in advanced config and restart (" + getRouterVersion() + ")";

                case UNKNOWN:
                case IPV4_UNKNOWN_IPV6_FIREWALLED:
                case IPV4_DISABLED_IPV6_UNKNOWN:
                default:
                    ra = routerInfo.getTargetAddress ( "SSU" );

                    if ( ra == null && _context.router().getUptime() > 5 * 60 * 1000 )
                    {
                        if ( getActivePeers() <= 0 )
                        { return "I2P: ERR-No Active Peers, Check Network Connection and Firewall (" + getRouterVersion() + ")"; }

                        else if ( _context.getProperty ( PROP_I2NP_NTCP_HOSTNAME ) == null ||
                                  _context.getProperty ( PROP_I2NP_NTCP_PORT ) == null )
                        { return "I2P: ERR-UDP Disabled and Inbound TCP host/port not set (" + getRouterVersion() + ")"; }

                        else
                        { return "I2P: WARN-Firewalled with UDP Disabled (" + getRouterVersion() + ")"; }

                    }

                    return status.toStatusString() + " (" + getRouterVersion() + ")";
                }

            }

        }

        return "I2P: Using existing router (" + getRouterVersion() + ")";

    }


}
