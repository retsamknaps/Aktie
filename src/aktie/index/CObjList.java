package aktie.index;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

import aktie.data.CObj;

public class CObjList
{

    //private DirectoryReader reader;
    private AktieSearcher searcher;
    private Query query;
    private TopDocs documents;
    private Sort sort;
    private List<CObj> extras;

    public CObjList()
    {
        extras = new LinkedList<CObj>();
    }

    public CObjList ( AktieSearcher s, Analyzer a, String q ) throws ParseException
    {
        this ( s, a, q, null );
    }

    public CObjList ( AktieSearcher s, Analyzer a, String q, Sort srt ) throws ParseException
    {
        QueryParser qp = new QueryParser ( "text_title", a );
        query = qp.parse ( q );
        init ( s, query, srt );
    }

    public CObjList ( Query baseq, AktieSearcher s, Analyzer a, String q, Sort srt ) throws ParseException
    {
        QueryParser qp = new QueryParser ( "text_title", a );
        Query sq = qp.parse ( q );
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        builder.add ( sq, BooleanClause.Occur.MUST );
        builder.add ( baseq, BooleanClause.Occur.MUST );
        init ( s, builder.build(), srt );
    }

    public CObjList ( Query baseq, AktieSearcher s, Analyzer a, String q ) throws ParseException
    {
        QueryParser qp = new QueryParser ( "text_title", a );
        Query sq = qp.parse ( q );
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        builder.add ( sq, BooleanClause.Occur.MUST );
        builder.add ( baseq, BooleanClause.Occur.MUST );
        init ( s, builder.build(), null );
    }

    public CObjList ( AktieSearcher s, Query q )
    {
        init ( s, q, null );
    }

    public CObjList ( AktieSearcher s, Query q, Sort srt )
    {
        init ( s, q, srt );
    }

    private static List<CObjList> alllists = new LinkedList<CObjList>();
    private StackTraceElement creationStack[];
    private long createdOn;
    public void init ( AktieSearcher s, Query q, Sort srt )
    {
        //reader = r;
        searcher = s;
        query = q;
        sort = srt;
        extras = new LinkedList<CObj>();
        createdOn = System.currentTimeMillis();
        creationStack = Thread.currentThread().getStackTrace();

        //        synchronized ( alllists )
        //        {
        //            alllists.add ( this );
        //        }

    }

    public static void displayAllStillOpen()
    {
        List<CObjList> tl = new LinkedList<CObjList>();

        synchronized ( alllists )
        {
            tl.addAll ( alllists );
        }

        for ( CObjList l : tl )
        {
            l.displayStillOpen();
        }

    }

    private long DISPTIME = 5L * 60L * 1000L;
    private void displayStillOpen()
    {
        long ct = System.currentTimeMillis();
        long life = ct - createdOn;

        if ( life >= DISPTIME && creationStack != null )
        {

            System.out.println ( "============================= " + this + " still open after " + ( life / 1000L ) + " seconds" );

            for ( StackTraceElement e : creationStack )
            {
                System.out.println ( e.getClassName() + "." + e.getMethodName() + " : " + e.getFileName() + " line: " + e.getLineNumber() );
            }

        }

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
        if ( searcher != null )
        {
            searcher.closeSearch();
        }

        //        synchronized ( alllists )
        //        {
        //            alllists.remove ( this );
        //        }

    }

}
