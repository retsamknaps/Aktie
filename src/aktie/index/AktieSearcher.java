package aktie.index;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

public class AktieSearcher
{

    static Logger log = Logger.getLogger ( "aktie" );

    private IndexSearcher searcher;
    private boolean closed;
    public long closedAt;

    public synchronized static AktieSearcher newSearcher ( String dir ) throws IOException
    {
        AktieSearcher a = new AktieSearcher ( dir );
        return a;
    }

    private AktieSearcher ( String dir ) throws IOException
    {

        IndexReader reader = DirectoryReader.open ( FSDirectory.open ( Paths.get ( dir ) ) );
        searcher = new IndexSearcher ( reader );
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

        if ( max == Integer.MAX_VALUE )
        {
            max = searcher.count ( query );
        }

        max = Math.max ( 1, max );

        TopScoreDocCollector collector = TopScoreDocCollector.create ( max );
        searcher.search ( query, collector );

        return collector.topDocs();
    }

    public TopDocs search ( Query query, int max, Sort s ) throws IOException
    {
        if ( closed )
        {
            log.severe ( "AktieSearcher searched after closed! " + this );
            throw new IOException ( "AktieSearcher already closed." );
        }

        if ( max == Integer.MAX_VALUE )
        {
            max = searcher.count ( query );
        }

        max = Math.max ( 1, max );

        TopFieldCollector collector = TopFieldCollector.create ( s, max, false, false, false );
        searcher.search ( query, collector );

        return collector.topDocs();
    }

    public void closeSearch()
    {
        if ( !closed )
        {
            closed = true;
            closedAt = System.currentTimeMillis();

            try
            {
                searcher.getIndexReader().close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
                log.log ( Level.SEVERE, "Failed to close searcher.", e );
            }

        }

    }

}
