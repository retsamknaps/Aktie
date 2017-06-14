package aktie.net;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.user.RequestFileHandler;

public class DestinationThread implements Runnable
{

    Logger log = Logger.getLogger ( "aktie" );

    public static Map<String, DestinationThread> threadlist = new HashMap<String, DestinationThread>();

    private Index index;
    private HH2Session session;
    private GuiCallback callback;
    private SpamTool spamtool;

    public static void stopAll()
    {
        synchronized ( threadlist )
        {
            for ( DestinationThread d : threadlist.values() )
            {
                d.stop();
            }

        }

    }

    private boolean stop;
    private Destination dest;
    private Map<String, List<ConnectionThread>> connections;
    private CObj identity;
    private GetSendData2 conMan;
    private ConnectionListener conListener;
    private RequestFileHandler fileHandler;
    private File tmpDir;

    public DestinationThread ( Destination d, GetSendData2 sd, HH2Session s, Index i, GuiCallback cb, ConnectionListener cl, RequestFileHandler rf, SpamTool st )
    {
        fileHandler = rf;
        conListener = cl;
        index = i;
        session = s;
        callback = cb;
        conMan = sd;
        dest = d;
        spamtool = st;
        connections = new HashMap<String, List<ConnectionThread>>();
        Thread t = new Thread ( this, "Destination Connection Accept Thread" );
        t.start();

        synchronized ( threadlist )
        {
            threadlist.put ( d.getPublicDestinationInfo(), this );
        }

    }

    public void setTmpDir ( File t )
    {
        tmpDir = t;
    }

    public void setIdentity ( CObj o )
    {
        identity = o;
    }

    public CObj getIdentity()
    {
        return identity;
    }

    public boolean isStopped()
    {
        return stop;
    }

    public Destination getDest()
    {
        return dest;
    }

    public void closeConnections()
    {
        List<ConnectionThread> r = new LinkedList<ConnectionThread>();

        synchronized ( connections )
        {
            for ( List<ConnectionThread> tl : connections.values() )
            {
                r.addAll ( tl );
            }

        }

        for ( ConnectionThread ct : r )
        {
            ct.stop();
        }

    }

    public void stop()
    {
        stop = true;
        dest.close();
        closeConnections();
    }

    public void addEstablishedConnection ( ConnectionThread con )
    {
        conListener.update ( con );

        String d = con.getEndDestination().getId();

        if ( d != null )
        {
            synchronized ( connections )
            {
                List<ConnectionThread> l = connections.get ( d );

                if ( l == null )
                {
                    l = new LinkedList<ConnectionThread>();
                    connections.put ( d, l );
                }

                l.add ( con );
            }

        }

    }

    public void closeConnection ( String id )
    {
        List<ConnectionThread> cl = new LinkedList<ConnectionThread>();

        synchronized ( connections )
        {
            List<ConnectionThread> l = connections.get ( id );

            if ( l != null )
            {
                cl.addAll ( l );
            }

        }

        for ( ConnectionThread ct : cl )
        {
            ct.stop();
        }

    }

    public void toggleConnectionLogging ( String id )
    {
        List<ConnectionThread> cl = new LinkedList<ConnectionThread>();

        synchronized ( connections )
        {
            List<ConnectionThread> l = connections.get ( id );

            if ( l != null )
            {
                cl.addAll ( l );
            }

        }

        for ( ConnectionThread ct : cl )
        {
            ct.toggleLogging();
        }

    }

    public List<String> getConnectedIds()
    {
        List<String> r = new LinkedList<String>();

        synchronized ( connections )
        {
            r.addAll ( connections.keySet() );
        }

        return r;
    }

    public void send ( String id, CObj d )
    {
        List<ConnectionThread> ct = new LinkedList<ConnectionThread>();

        synchronized ( connections )
        {
            ct.addAll ( connections.get ( id ) );
        }

        for ( ConnectionThread c : ct )
        {
            c.enqueue ( d );
        }

    }

    public void connectionClosed ( ConnectionThread con )
    {
        if ( con.getEndDestination() != null )
        {
            String d = con.getEndDestination().getId();

            if ( d != null )
            {
                synchronized ( connections )
                {
                    List<ConnectionThread> l = connections.get ( d );

                    if ( l != null )
                    {
                        l.remove ( con );
                    }

                }

            }

        }

    }

    public int numberConnection()
    {
        synchronized ( connections )
        {
            return connections.size();
        }

    }

    public void update ( CObj o )
    {
        List<ConnectionThread> tl = new LinkedList<ConnectionThread>();

        synchronized ( connections )
        {
            for ( List<ConnectionThread> l : connections.values() )
            {
                tl.addAll ( l );
            }

        }

        for ( ConnectionThread t : tl )
        {
            t.update ( o );
        }

    }

    public void send ( CObj o )
    {
        List<ConnectionThread> tl = new LinkedList<ConnectionThread>();

        synchronized ( connections )
        {
            for ( List<ConnectionThread> l : connections.values() )
            {
                tl.addAll ( l );
            }

        }

        for ( ConnectionThread t : tl )
        {
            t.enqueue ( o );
        }

    }

    public boolean isConnected ( String id, boolean filemode )
    {
        synchronized ( connections )
        {
            List<ConnectionThread> clst = connections.get ( id );

            if ( clst == null ) { return false; }

            for ( ConnectionThread ct : clst )
            {
                if ( ct.isFileMode() == filemode )
                {
                    return true;
                }

            }

            return false;
        }

    }

    public void connect ( String d, boolean filemode )
    {
        Connection con = dest.connect ( d );

        if ( con != null )
        {
            buildConnection ( con, filemode );
        }

    }

    public List<ConnectionThread> getConnectionThreads()
    {
        List<ConnectionThread> l = new LinkedList<ConnectionThread>();

        synchronized ( connections )
        {
            for ( List<ConnectionThread> cl : connections.values() )
            {
                l.addAll ( cl );
            }

        }

        return l;
    }

    private void buildConnection ( Connection c, boolean filemode )
    {
        if ( identity == null )
        {
            c.close();
        }

        else
        {
            ConnectionThread ct = new ConnectionThread ( this, session, index, c, conMan, callback, conListener, fileHandler, filemode, spamtool );
            ct.setTempDir ( tmpDir );
            ct.enqueue ( identity );
        }

    }

    @Override
    public void run()
    {
        try
        {
            while ( !stop )
            {
                Connection c = dest.accept();
                buildConnection ( c, false );
            }

        }

        catch ( Exception e )
        {
        }

        stop();
    }

}
