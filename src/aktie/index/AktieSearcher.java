package aktie.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.uninverting.UninvertingReader;
import org.apache.lucene.uninverting.UninvertingReader.Type;

public class AktieSearcher
{

    static Logger log = Logger.getLogger ( "aktie" );

    private DirectoryReader reader;
    private IndexSearcher searcher;
    private int numOpen;
    private boolean closeAll;
    private boolean closed;
    private long closedAt;

    private static List<AktieSearcher> list = new LinkedList<AktieSearcher>();

    public synchronized static AktieSearcher newSearcher ( IndexWriter w ) throws IOException
    {
        AktieSearcher a = new AktieSearcher ( w );

        synchronized ( list )
        {
            list.add ( a );
            log.info ( "============== Number searchers: " + list.size() );
            Iterator<AktieSearcher> i = list.iterator();
            long ctime = System.currentTimeMillis();

            while ( i.hasNext() )
            {
                AktieSearcher s = i.next();

                if ( s.closed )
                {
                    i.remove();
                }

                else if ( s.closeAll )
                {
                    long bt = ctime - s.closedAt;
                    bt /= 1000;
                    log.info ( "Open searches: " + s.numOpen + " closed " + bt + " seconds ago." );
                }

            }

            log.info ( "============== << Number searchers" );
        }

        return a;
    }

    private AktieSearcher ( IndexWriter writer ) throws IOException
    {
        DirectoryReader dr = DirectoryReader.open ( writer, true );

        //Map<String, Type> mapping = new HashMap<String, Type>();
        //mapping.put("numbers_createdon", UninvertingReader.Type.LONG);
        //mapping.put("strings_name", UninvertingReader.Type.SORTED);
        //mapping.put("PRIVATE_name", UninvertingReader.Type.SORTED);
        //mapping.put("strings_creator_name", UninvertingReader.Type.SORTED);
        //mapping.put("PRIVNUM_prv_push_time", UninvertingReader.Type.LONG);
        ////mapping.put("", UninvertingReader.Type.);
        ////mapping.put("", UninvertingReader.Type.);
        //reader = UninvertingReader.wrap(dr, mapping);
        reader = dr;

        searcher = new IndexSearcher ( reader );
        numOpen = 0;
        closeAll = false;
        closed = false;
    }

    public synchronized boolean incrNumOpen()
    {
        if ( closeAll )
        {
            return false;
        }

        numOpen++;
        return true;
    }

    public Document doc ( int id ) throws IOException
    {
        if ( closed )
        {
            log.severe ( "AktieSearcher searched after closed!" );
            throw new IOException ( "AktieSearcher already closed. " + this );
        }

        return searcher.doc ( id );
    }

    public TopDocs search ( Query query, int max ) throws IOException
    {
        if ( closed )
        {
            log.severe ( "AktieSearcher searched after closed! " + this );
            throw new IOException ( "AktieSearcher already closed." );
        }

        TopDocs d = searcher.search ( query, max );
        return d;
    }

    public TopDocs search ( Query query, int max, Sort s ) throws IOException
    {
        if ( closed )
        {
            log.severe ( "AktieSearcher searched after closed! " + this );
            throw new IOException ( "AktieSearcher already closed." );
        }

        TopDocs d = searcher.search ( query, max, s );
        return d;
    }

    public void shutdown()
    {
        setShutdown();
        doClose();
    }

    private synchronized void setShutdown()
    {
        closedAt = System.currentTimeMillis();
        closeAll = true;
    }

    private synchronized void decrNumOpen()
    {
        numOpen--;

        if ( numOpen < 0 )
        {
            log.severe ( "AktieSearcher closed more searchers than openned!" );
            numOpen = 0;
        }

    }

    private void doClose()
    {
        if ( closeAll && numOpen == 0 )
        {
            closed = true;

            try
            {
                reader.close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
                log.log ( Level.SEVERE, "Failed to close searcher.", e );
            }

        }

    }

    public void closeSearch()
    {
        decrNumOpen();
        doClose();
    }

}
