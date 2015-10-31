package aktie.index;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

import aktie.data.CObj;

public class CObjList
{

    private DirectoryReader reader;
    private IndexSearcher searcher;
    private Query query;
    private TopDocs documents;
    private Sort sort;
    private List<CObj> extras;
    //Barrett .50

    public CObjList()
    {
        extras = new LinkedList<CObj>();
    }

    public CObjList ( DirectoryReader r, IndexSearcher s, Analyzer a, String q ) throws ParseException
    {
        this ( r, s, a, q, null );
    }

    public CObjList ( DirectoryReader r, IndexSearcher s, Analyzer a, String q, Sort srt ) throws ParseException
    {
        QueryParser qp = new QueryParser ( "text_title", a );
        query = qp.parse ( q );
        init ( r, s, query, srt );
    }

    public CObjList ( Query baseq, DirectoryReader r, IndexSearcher s, Analyzer a, String q, Sort srt ) throws ParseException
    {
        QueryParser qp = new QueryParser ( "text_title", a );
        Query sq = qp.parse ( q );
        BooleanQuery bq = new BooleanQuery();
        bq.add ( sq, BooleanClause.Occur.MUST );
        bq.add ( baseq, BooleanClause.Occur.MUST );
        init ( r, s, bq, srt );
    }

    public CObjList ( Query baseq, DirectoryReader r, IndexSearcher s, Analyzer a, String q ) throws ParseException
    {
        QueryParser qp = new QueryParser ( "text_title", a );
        Query sq = qp.parse ( q );
        BooleanQuery bq = new BooleanQuery();
        bq.add ( sq, BooleanClause.Occur.MUST );
        bq.add ( baseq, BooleanClause.Occur.MUST );
        init ( r, s, bq, null );
    }

    public CObjList ( DirectoryReader r, IndexSearcher s, Query q )
    {
        init ( r, s, q, null );
    }

    public CObjList ( DirectoryReader r, IndexSearcher s, Query q, Sort srt )
    {
        init ( r, s, q, srt );
    }

    public void init ( DirectoryReader r, IndexSearcher s, Query q, Sort srt )
    {
        reader = r;
        searcher = s;
        query = q;
        sort = srt;
        extras = new LinkedList<CObj>();
    }

    public void executeQuery ( int max ) throws IOException, ParseException
    {
        if ( sort == null )
        {
            documents = searcher.search ( query, max );
        }

        else
        {
            documents = searcher.search ( query, max, sort );
        }

    }

    public int size()
    {
        if ( documents != null )
        {
            return documents.scoreDocs.length + extras.size();
        }

        else
        {
            return extras.size();
        }

    }

    public void add ( CObj o )
    {
        extras.add ( o );
    }

    public CObj get ( int idx ) throws IOException
    {
        if ( documents != null && idx <  documents.scoreDocs.length )
        {
            int id = documents.scoreDocs[idx].doc;
            Document d = searcher.doc ( id );
            CObj o = new CObj();
            o.loadDocument ( d );
            return o;
        }

        else
        {
            if ( documents != null )
            {
                idx -= documents.scoreDocs.length;
            }

            return extras.get ( idx );
        }

    }

    public void close()
    {
        if ( reader != null )
        {
            try
            {
                reader.close();
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

    }

}
