package aktie.index;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.logging.Logger;

public class CObjListServerSelector implements Runnable
{

    Logger log = Logger.getLogger ( "aktie" );

    private Selector selector;
    private boolean closed = false;
    private Queue<CObjListServer> newlist;

    public CObjListServerSelector() throws IOException
    {
        newlist = new ConcurrentLinkedQueue<CObjListServer>();
        selector = Selector.open();
        Thread t = new Thread ( this );
        t.start();
    }

    public CObjListServer getServer ( SocketChannel s, CObjList l ) throws IOException
    {
        s.configureBlocking ( false );
        CObjListServer r = new CObjListServer ( l, s );
        newlist.add ( r );
        selector.wakeup();
        return r;
    }

    public void close()
    {
        closed = true;
        selector.wakeup();

        try
        {
            selector.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    @SuppressWarnings ( "resource" )
    public void run()
    {
        while ( !closed )
        {
            try
            {
                CObjListServer s = newlist.poll();

                while ( s != null )
                {
                    log.info ( "Register new socket" );
                    s.doRegister ( selector );
                    s = newlist.poll();
                }

                selector.select();
                Iterator<SelectionKey> i = selector.selectedKeys().iterator();

                while ( i.hasNext() )
                {
                    SelectionKey sk = i.next();
                    i.remove();
                    Object at = sk.attachment();

                    if ( at instanceof CObjListServer )
                    {
                        log.info ( "Processing socket " + sk.interestOps() + " " +
                                   sk.readyOps() + " " +
                                   SelectionKey.OP_ACCEPT + " " +
                                   SelectionKey.OP_CONNECT + " " +
                                   SelectionKey.OP_READ + " " +
                                   SelectionKey.OP_WRITE );
                        CObjListServer c = ( CObjListServer ) at;
                        c.ready ( sk );
                    }

                }

            }

            catch ( IOException e )
            {
                close();
                e.printStackTrace();
            }

        }

    }


}
