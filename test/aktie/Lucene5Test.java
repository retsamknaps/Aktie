package aktie;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import aktie.index.GenenskapAnalyzer;
import aktie.utils.FUtils;

public class Lucene5Test
{

    @Test
    public void testIt()
    {
        try
        {
            File indexdir = new File ( "testindex" );
            FUtils.deleteDir ( indexdir );
            indexdir.mkdirs();

            SimpleFSDirectory fsdir = new SimpleFSDirectory ( indexdir.toPath() );
            //analyzer = new StandardAnalyzer();
            GenenskapAnalyzer analyzer = new GenenskapAnalyzer();
            IndexWriterConfig idxconf = new IndexWriterConfig ( analyzer );
            IndexWriter writer = new IndexWriter ( fsdir, idxconf );
            writer.commit();

            Document d = new Document();
            d.add ( new StringField ( "id", "doc000", Store.YES ) );
            d.add ( new SortedDocValuesField ( "id", new BytesRef ( "doc010" ) ) );
            d.add ( new TextField ( "test", "here is some text aaa", Store.NO ) );
            d.add ( new LongField ( "number", 10L, Store.YES ) );
            d.add ( new SortedNumericDocValuesField ( "number", 10L ) );
            Term idt = new Term ( "id", "doc000" );
            writer.updateDocument ( idt, d );

            d = new Document();
            d.add ( new StringField ( "id", "doc001", Store.YES ) );
            d.add ( new SortedDocValuesField ( "id", new BytesRef ( "doc001" ) ) );
            d.add ( new TextField ( "test", "here is some text bbb", Store.NO ) );
            d.add ( new LongField ( "number", 11L, Store.YES ) );
            d.add ( new SortedNumericDocValuesField ( "number", 11L ) );
            idt = new Term ( "id", "doc001" );
            writer.updateDocument ( idt, d );

            d = new Document();
            d.add ( new StringField ( "id", "doc002", Store.YES ) );
            d.add ( new SortedDocValuesField ( "id", new BytesRef ( "doc002" ) ) );
            d.add ( new TextField ( "test", "here is some text ccc", Store.NO ) );
            d.add ( new LongField ( "number", 15L, Store.YES ) );
            d.add ( new SortedNumericDocValuesField ( "number", 15L ) );
            idt = new Term ( "id", "doc002" );
            writer.updateDocument ( idt, d );
            writer.commit();

            DirectoryReader reader = DirectoryReader.open ( writer, true );
            IndexSearcher searcher = new IndexSearcher ( reader );

            QueryParser qp = new QueryParser ( "test", analyzer );
            Query sq = qp.parse ( "text" );

            //SortedNumericSortField field = new SortedNumericSortField ( "number", SortedNumericSortField.Type.LONG, true );
            SortField field = new SortField ( "id", SortField.Type.STRING, true );
            Sort sort = new Sort ( field );

            int cnt = searcher.count ( sq );
            System.out.println ( "COUNT: " + cnt );
            TopFieldCollector collector = TopFieldCollector.create ( sort, cnt, false, false, false );
            searcher.search ( sq, collector );

            TopFieldDocs dcs = collector.topDocs();

            for ( int c = 0 ; c < dcs.scoreDocs.length; c++ )
            {
                ScoreDoc sd = dcs.scoreDocs[c];
                Document d2 = searcher.doc ( sd.doc );
                System.out.println ( "Number: " + d2.get ( "number" ) );
            }

            reader.close();
            writer.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail ( "blah" );
        }

    }

}
