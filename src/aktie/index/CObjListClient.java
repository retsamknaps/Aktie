package aktie.index;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.json.JSONTokener;

import aktie.data.CObj;

public class CObjListClient implements Closeable
{

    Logger log = Logger.getLogger ( "aktie" );

    private boolean closed = false;
    private SocketChannel socket;
    private int size;
    private boolean lengthpending;
    private byte backingbytes[];
    private ByteBuffer readbuffer;
    private Queue<ByteBuffer> writelist;
    private int indexpending;
    private Queue<PendingRead> pendingreads;
    private PendingRead pendingsize;
    private SelectionKey selectionkey;
    //SoftReferences aren't required to behave any differently than
    //WeakReferences, but in practice softly reachable objects are
    //generally retained as long as memory is in plentiful supply.
    private Map<Integer, CObj> cache;

    private class PendingRead
    {
        int pendingindex;
        public CObj co;
        public boolean closed = false;

        public synchronized CObj waitForIndex ( int idx )
        {
            pendingindex = idx;

            try
            {
                log.info ( "wait.." );

                if ( co == null && !closed )
                {
                    wait();
                    log.info ( "done with wait.." );
                }

            }

            catch ( InterruptedException e )
            {
            }

            return co;
        }

        public synchronized boolean indexRead ( int idx, CObj c )
        {
            if ( idx == pendingindex )
            {
                log.info ( "indexRead: " + idx );
                co = c;
                notifyAll();
                return true;
            }

            return false;
        }

        public synchronized void close()
        {
            log.info ( "close" );
            closed = true;
            notifyAll();
        }

    }

    public int size()
    {
        while ( size == -1 )
        {
            pendingsize.waitForIndex ( 0 );
            log.info ( "Done waiting on size: " + size );

            if ( pendingsize.closed )
            {
                log.info ( "pending size closed..." );
                size = 0;
            }

        }

        return size;
    }

    public CObj get ( int idx ) throws IOException
    {
        if ( isClosed() || idx < 0 && idx >= size() )
        {
            return null;
        }

        CObj co = cache.get ( idx );

        if ( co == null )
        {
            PendingRead pr = new PendingRead();
            addPendingRead ( pr );
            //add index to write list
            ByteBuffer b = ByteBuffer.allocate ( Integer.SIZE / Byte.SIZE );
            b.putInt ( idx );
            b.clear();
            writelist.add ( b );

            while ( selectionkey == null )
            {
                try
                {
                    Thread.sleep ( 100L );
                }

                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }

            }

            selectionkey.selector().wakeup(); //cause interest ops to be updated
            log.info ( "read index: " + idx );
            pr.waitForIndex ( idx );
            return pr.co;
        }

        return co;
    }


    public CObjListClient ( SocketChannel s ) throws ClosedChannelException
    {
        cache = new WeakHashMap<Integer, CObj>();
        socket = s;
        size = -1;
        pendingreads = new ConcurrentLinkedQueue<PendingRead>();
        writelist = new ConcurrentLinkedQueue<ByteBuffer>();
        pendingsize = new PendingRead();
        setBufferSize ( Integer.SIZE / Byte.SIZE );
    }

    public void doRegister ( Selector sel ) throws ClosedChannelException
    {
        selectionkey = socket.register ( sel,
                                         ( SelectionKey.OP_READ ) );
        selectionkey.attach ( this );
    }

    public void updateInterestOpts()
    {
        log.info ( "update interest ops: " + writelist.size() );

        if ( writelist.size() > 0 )
        {
            log.info ( "set write interest ops" );
            selectionkey.interestOps ( selectionkey.interestOps() |
                                       SelectionKey.OP_WRITE );
        }

        else
        {
            selectionkey.interestOps ( selectionkey.interestOps() &
                                       ( ~SelectionKey.OP_WRITE ) );
        }

    }

    private synchronized void setClosed()
    {
        closed = true;

        for ( PendingRead pr : pendingreads )
        {
            pr.close();
        }

        pendingsize.close();
        pendingreads.clear();
    }

    private synchronized void addPendingRead ( PendingRead pr )
    {
        if ( closed )
        {
            pr.close();
        }

        else
        {
            pendingreads.add ( pr );
        }

    }

    private synchronized boolean isClosed()
    {
        return closed;
    }

    @Override
    public void close()
    {
        setClosed();

        try
        {
            socket.close();
        }

        catch ( IOException e )
        {
        }

    }

    private void reset()
    {
        lengthpending = true;
        setBufferSize ( 2 * Integer.SIZE / Byte.SIZE );
    }

    private void prepareForCObjRead ( int idx, int len )
    {
        indexpending = idx;
        lengthpending = false;
        setBufferSize ( len );
    }

    private void setBufferSize ( int len )
    {
        backingbytes = new byte[len];
        readbuffer = ByteBuffer.wrap ( backingbytes );
    }

    private void readCObj()
    {
        try
        {
            String s = new String ( backingbytes, "UTF8" );
            JSONTokener t = new JSONTokener ( s );
            JSONObject jo =  new JSONObject ( t );
            CObj co = new CObj();
            co.LOADPRIVATEJSON ( jo );
            cache.put ( indexpending, co );
            Iterator<PendingRead> i = pendingreads.iterator();

            while ( i.hasNext() )
            {
                PendingRead r = i.next();

                if ( r.indexRead ( indexpending, co ) )
                {
                    i.remove();
                }

            }

        }

        catch ( UnsupportedEncodingException e )
        {
            e.printStackTrace();
        }

        reset();
    }

    public void ready ( SelectionKey key )
    {
        try
        {
            if ( ( key.readyOps() & SelectionKey.OP_CONNECT ) != 0 )
            {
                socket.finishConnect();
                log.info ( "connection finished" );
                key.interestOps ( key.interestOps() & ( ~SelectionKey.OP_CONNECT ) );
                key.interestOps ( key.interestOps() | SelectionKey.OP_READ );
            }

            if ( ( key.readyOps() & SelectionKey.OP_WRITE ) != 0 )
            {
                log.info ( "write op ready" );
                ByteBuffer w = writelist.peek();
                log.info ( "w: " + w );

                while ( w != null )
                {
                    log.info ( "w: hasremainging: " + w.hasRemaining() );

                    if ( w.hasRemaining() )
                    {
                        log.info ( "bytes sent" );
                        socket.write ( w );
                        break;
                    }

                    else
                    {
                        writelist.poll();
                        w = writelist.peek();
                    }

                }

                if ( writelist.size() == 0 )
                {
                    key.interestOps (
                        key.interestOps() & ( ~SelectionKey.OP_WRITE ) );
                }

            }

            if ( ( key.readyOps() & SelectionKey.OP_READ ) != 0 )
            {
                log.info ( "read op ready" );
                int l = socket.read ( readbuffer );

                if ( l < 0 )
                {
                    close();
                    return;
                }

                if ( !readbuffer.hasRemaining() )
                {
                    readbuffer.flip();

                    if ( size == -1 )
                    {
                        size = readbuffer.getInt();
                        log.info ( "read size: " + size );
                        pendingsize.indexRead ( 0, null );
                        reset();
                    }

                    else if ( lengthpending )
                    {
                        int idx = readbuffer.getInt();
                        int len = readbuffer.getInt();
                        prepareForCObjRead ( idx, len );
                    }

                    else
                    {
                        readCObj();
                    }

                }

            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            close();
        }

    }

}
