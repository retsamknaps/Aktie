package aktie.index;

import java.io.File;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.SimpleFSDirectory;

import aktie.data.CObj;

public class Upgrade
{

    public static void main ( String args[] )
    {
        //if (args.length >= 1) {
        try
        {
            System.out.println ( "HERE...0" );
            File idxdir = new File ( args[0] );
            SimpleFSDirectory fsdir = new SimpleFSDirectory ( idxdir.toPath() );

            GenenskapAnalyzer analyzer = new GenenskapAnalyzer();
            IndexWriterConfig idxconf = new IndexWriterConfig ( analyzer );
            IndexWriter writer = new IndexWriter ( fsdir, idxconf );
            writer.commit();

            DirectoryReader dr = DirectoryReader.open ( writer, true );
            IndexReader reader = dr;
            IndexSearcher searcher = new IndexSearcher ( reader );

            Query q = new MatchAllDocsQuery();
            int numdocs = searcher.count ( q );
            System.out.println ( "UPDATING: " + numdocs + " documents" );

            TopScoreDocCollector collector = TopScoreDocCollector.create ( numdocs );
            searcher.search ( q, collector );
            TopDocs dcs = collector.topDocs();
            Index ti = new Index();
            System.out.print ( "Updated: 0" );

            for ( int c = 0 ; c < dcs.scoreDocs.length; c++ )
            {
                Document dc = searcher.doc ( dcs.scoreDocs[c].doc );
                CObj co = new CObj();
                co.loadDocument ( dc );
                ti.indexNoCommit ( writer, co, false );
                System.out.print ( "\rUpdated: " + c + "            " );
            }

            System.out.println ( "\nDone.  Committing." );
            writer.commit();
            System.out.println ( "Closing." );
            reader.close();
            writer.close();

        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        //}

    }

}
