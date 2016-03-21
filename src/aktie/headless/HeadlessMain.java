package aktie.headless;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import aktie.Node;
import aktie.i2p.I2PNet;

public class HeadlessMain
{

    Logger log = Logger.getLogger ( "aktie" );

    private String nodeDir;
    private Node node;
    private ClientServer server;

    private I2PNet i2pnet;

    public Node getNode()
    {
        return node;
    }

    public void setVerbose()
    {
        System.out.println ( "SETTING VERBOSE-!-" );
        log.setLevel ( Level.INFO );
    }

    public void setSevere()
    {
        log.setLevel ( Level.SEVERE );
    }

    private void setDefaults ( Properties i2pProps )
    {
        String tmp = i2pProps.getProperty ( "inbound.length" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "inbound.length", "2" );
        }

        tmp = i2pProps.getProperty ( "inbound.quantity" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "inbound.quantity", "3" );
        }

        tmp = i2pProps.getProperty ( "outbound.length" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "outbound.length", "2" );
        }

        tmp = i2pProps.getProperty ( "outbound.quantity" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "outbound.quantity", "3" );
        }

        tmp = i2pProps.getProperty ( "inbound.nickname" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "inbound.nickname", "AKTIE" );
        }

        tmp = i2pProps.getProperty ( "i2cp.tcp.host" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "i2cp.tcp.host", "127.0.0.1" );
        }

        tmp = i2pProps.getProperty ( "i2cp.tcp.port" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "i2cp.tcp.port", "7654" );
        }


    }

    private Properties getI2PReady()
    {
        File i2pp = new File ( nodeDir + File.separator + "i2p.props" );

        Properties i2pProps = new Properties();

        try
        {
            FileInputStream fis = new FileInputStream ( i2pp );
            i2pProps.load ( fis );
            fis.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        setDefaults ( i2pProps );
        return i2pProps;

    }

    public void startNode()
    {
        Properties p = getI2PReady();

        i2pnet = new I2PNet ( nodeDir, p );
        i2pnet.waitUntilReady();

        try
        {
            node = new Node ( nodeDir, i2pnet, null,
                              null, null );

        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    public void shutdown()
    {
        if ( server != null )
        {
            server.stop();
        }

        if ( node != null )
        {
            node.close();
        }

        if ( i2pnet != null )
        {
            i2pnet.exit();
        }

        System.exit ( 0 );
    }

    public void startClientServer()
    {
        server = new ClientServer ( this );
    }

    /**
        Launch the application.
        @param args
    */
    public static void main ( String[] args )
    {
        boolean verbose = false;

        try
        {

            HeadlessMain hmain = new HeadlessMain();

            if ( args.length > 0 )
            {
                hmain.nodeDir = args[0];

                if ( args.length > 1 )
                {
                    if ( "-v".equals ( args[1] ) )
                    {
                        verbose = true;
                    }

                }

                if ( args.length > 2 )
                {
                    for ( int ct = 2; ct < args.length && !verbose; ct++ )
                    {
                        verbose = "-v".equals ( args[ct] );
                    }

                }

            }

            else
            {
                hmain.nodeDir = "aktie_node";
            }

            if ( verbose )
            {
                hmain.setVerbose();
            }

            else
            {
                hmain.setSevere();
            }

            hmain.startNode();
            hmain.startClientServer();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }


}
