package aktie.index;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.logging.Logger;

public class CObjListClientSelector implements Runnable
{

    Logger log = Logger.getLogger ( "aktie" );

    private boolean closed;
    private Selector selector;
    private Queue<CObjListClient> newlist;

    public CObjListClientSelector() throws IOException
    {
        selector = Selector.open();
        newlist = new ConcurrentLinkedQueue<CObjListClient>();
        Thread t = new Thread ( this );
        t.start();
    }

    public CObjListClient getClient ( SocketChannel s ) throws IOException
    {
        s.configureBlocking ( false );
        CObjListClient r = new CObjListClient ( s );
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
                Iterator<SelectionKey> i = selector.keys().iterator();

                while ( i.hasNext() )
                {
                    SelectionKey sk = i.next();
                    Object at = sk.attachment();

                    if ( at instanceof CObjListClient )
                    {
                        CObjListClient srv = ( CObjListClient ) at;
                        srv.updateInterestOpts();
                    }

                }

                CObjListClient r = newlist.poll();

                while ( r != null )
                {
                    log.info ( "Register new socket" );
                    r.doRegister ( selector );
                    r = newlist.poll();
                }

                //log.info("Select..");
                selector.select ( 30000L );
                //log.info("Select done.. " + selector.selectedKeys().size());
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                while ( it.hasNext() )
                {
                    SelectionKey sk = it.next();
                    it.remove();
                    Object at = sk.attachment();

                    if ( at instanceof CObjListClient )
                    {
                        log.info ( "interest ops ready " + sk.interestOps() );
                        CObjListClient c = ( CObjListClient ) at;
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
