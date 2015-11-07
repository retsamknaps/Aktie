package aktie.index;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.util.Version;

import aktie.data.CObj;

public class Index
{

    // * NOTE *
    // This IS thread safe!
    private Analyzer analyzer;
    private IndexWriter writer;

    private File indexdir;

    public static List<CObj> list ( CObjList cl )
    {
        List<CObj> r = new LinkedList<CObj>();

        if ( cl != null )
        {
            for ( int c = 0; c < cl.size(); c++ )
            {
                try
                {
                    r.add ( cl.get ( c ) );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            cl.close();
        }

        return r;
    }

    public void setIndexdir ( File f )
    {
        indexdir = f;
    }

    public File getIndexdir()
    {
        return indexdir;
    }

    public void init() throws IOException
    {
        //analyzer = new StandardAnalyzer();
        analyzer = new GenenskapAnalyzer();
        IndexWriterConfig idxconf = new IndexWriterConfig ( Version.LUCENE_4_10_2, analyzer );
        SimpleFSDirectory fsdir = new SimpleFSDirectory ( indexdir );
        writer = new IndexWriter ( fsdir, idxconf );
        writer.commit();

    }

    public void close()
    {
        try
        {
            writer.close();
        }

        catch ( Exception e )
        {
        }

    }

    public CObjList search ( Query q, int max )
    {
        return search ( q, max, null );
    }

    public CObjList search ( Query q, int max, Sort srt )
    {
        try
        {
            DirectoryReader reader = DirectoryReader.open ( writer, true );
            IndexSearcher searcher = new IndexSearcher ( reader );
            CObjList l = new CObjList ( reader, searcher, q, srt );
            l.executeQuery ( max );
            return l;
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;

    }

    public CObjList search ( String qs, int max )
    {
        return search ( qs, max, null );
    }

    public CObjList search ( String qs, int max, Sort s )
    {
        try
        {
            DirectoryReader reader = DirectoryReader.open ( writer, true );
            IndexSearcher searcher = new IndexSearcher ( reader );
            CObjList l = new CObjList ( reader, searcher, analyzer, qs, s );
            l.executeQuery ( max );
            return l;
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;

    }

    public CObjList search ( Query bq, String qs, int max, Sort s )
    {
        try
        {
            DirectoryReader reader = DirectoryReader.open ( writer, true );
            IndexSearcher searcher = new IndexSearcher ( reader );
            CObjList l = new CObjList ( bq, reader, searcher, analyzer, qs, s );
            l.executeQuery ( max );
            return l;
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;

    }

    public CObjList search ( Query bq, String qs, int max )
    {
        try
        {
            DirectoryReader reader = DirectoryReader.open ( writer, true );
            IndexSearcher searcher = new IndexSearcher ( reader );
            CObjList l = new CObjList ( bq, reader, searcher, analyzer, qs, null );
            l.executeQuery ( max );
            return l;
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;

    }

    public CObjList getIdentities()
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    //Could be based on creator and sequence number, but
    //not enough to worry about.  Just send all.
    public CObjList getCommunities ( String creator, long first, long last )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), creator );
        bq.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.SEQNUM ),
                                         first, last, true, true );
        bq.add ( nq, BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    //Could be based on creator and sequence number, but
    //not enough to worry about.  Just send all.
    public CObjList getIdentityPrivateCommunities ( String creator )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), creator );
        bq.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term scopeterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PRIVATE );
        bq.add ( new TermQuery ( scopeterm ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getPublicCommunities()
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term pubterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PUBLIC );
        bq.add ( new TermQuery ( pubterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getValidCommunities()
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term valterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( valterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    //Could be based on creator and sequence number, but
    //not enough to worry about.  Just send all.
    //send ALL, even ones not maked decoded or valid
    public CObjList getMemberships ( String creator, long first, long last )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), creator );
        bq.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.SEQNUM ),
                                         first, last, true, true );
        bq.add ( nq, BooleanClause.Occur.MUST );

        CObjList r = search ( bq, Integer.MAX_VALUE );
        return r;
    }

    //Could be based on creator and sequence number, but
    //not enough to worry about.  Just send all.
    //send ALL, even ones not maked decoded or valid
    public CObjList getIdentityMemberships ( String memid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docPrivate ( CObj.MEMBERID ), memid );
        bq.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "true" );
        bq.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        CObjList r = search ( bq, Integer.MAX_VALUE );
        return r;
    }

    public CObjList getMemberships ( String comid, Sort s )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docPrivate ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "true" );
        bq.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE, s );
    }

    public CObjList getMyMemberships ( Sort s )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        Term privterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PRIVATE );
        bq.add ( new TermQuery ( privterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE, s );
    }

    public CObjList getMyValidMemberships ( Sort s )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        Term valterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "true" );
        bq.add ( new TermQuery ( valterm ), BooleanClause.Occur.MUST );

        Term privterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PRIVATE );
        bq.add ( new TermQuery ( privterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE, s );
    }

    public CObjList getMyMemberships ( String comid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docPrivate ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term validmem = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "true" );
        bq.add ( new TermQuery ( validmem ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList searchIdenties ( String squery, Sort s )
    {
        BooleanQuery idq = new BooleanQuery();
        Term idterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        idq.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        Matcher m = Pattern.compile ( "\\S+" ).matcher ( squery );

        if ( m.find() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append ( CObj.docStringText ( CObj.NAME ) );
            sb.append ( ":\"" );
            sb.append ( squery );
            sb.append ( "\" OR " );
            sb.append ( CObj.docStringText ( CObj.DESCRIPTION ) );
            sb.append ( ":\"" );
            sb.append ( squery );
            sb.append ( "\"" );
            return search ( idq, sb.toString(), Integer.MAX_VALUE, s );
        }

        else
        {
            return search ( idq, Integer.MAX_VALUE, s );
        }

    }

    public CObjList searchSemiPrivateCommunities ( String squery,  Sort s )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "false" );
        bq.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        Term privterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PRIVATE );
        bq.add ( new TermQuery ( privterm ), BooleanClause.Occur.MUST );

        Term pnterm = new Term ( CObj.docString ( CObj.NAME_IS_PUBLIC ), "true" );
        bq.add ( new TermQuery ( pnterm ), BooleanClause.Occur.MUST );

        if ( squery != null )
        {
            Matcher m = Pattern.compile ( "\\S+" ).matcher ( squery );

            if ( m.find() )
            {
                StringBuilder sb = new StringBuilder();
                sb.append ( CObj.docStringText ( CObj.NAME ) );
                sb.append ( ":\"" );
                sb.append ( squery );
                sb.append ( "\" OR " );
                sb.append ( CObj.docStringText ( CObj.DESCRIPTION ) );
                sb.append ( ":\"" );
                sb.append ( squery );
                sb.append ( "\" OR " );
                sb.append ( CObj.docStringText ( CObj.CREATOR_NAME ) );
                sb.append ( ":\"" );
                sb.append ( squery );
                sb.append ( "\"" );
                return search ( bq, sb.toString(), Integer.MAX_VALUE );
            }

        }

        return search ( bq, Integer.MAX_VALUE, s );
    }

    public CObjList searchSubscribable ( String squery, String memid, boolean prv, boolean pub, Sort s )
    {
        if ( !prv && !pub )
        {
            prv = true;
            pub = true;
        }

        BooleanQuery combined = new BooleanQuery();
        combined.setMinimumNumberShouldMatch ( 1 );

        Term comterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        combined.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        if ( pub )
        {

            Term pubterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PUBLIC );
            combined.add ( new TermQuery ( pubterm ), BooleanClause.Occur.SHOULD );

        }

        if ( prv && memid != null )
        {

            Term pubterm2 = new Term ( CObj.docPrivate ( memid ), "true" );
            combined.add ( new TermQuery ( pubterm2 ), BooleanClause.Occur.SHOULD );

        }

        if ( squery != null )
        {
            Matcher m = Pattern.compile ( "\\S+" ).matcher ( squery );

            if ( m.find() )
            {
                StringBuilder sb = new StringBuilder();
                sb.append ( CObj.docPrivateText ( CObj.NAME ) );
                sb.append ( ":\"" );
                sb.append ( squery );
                sb.append ( "\" OR " );
                sb.append ( CObj.docPrivateText ( CObj.DESCRIPTION ) );
                sb.append ( ":\"" );
                sb.append ( squery );
                sb.append ( "\"" );
                return search ( combined, sb.toString(), Integer.MAX_VALUE, s );
            }

        }

        return search ( combined, Integer.MAX_VALUE, s );
    }

    public CObjList searchPosts ( String comid, String qstr, Sort srt )
    {
        BooleanQuery query = new BooleanQuery();

        Term pstterm = new Term ( CObj.PARAM_TYPE, CObj.POST );
        query.add ( new TermQuery ( pstterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        query.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Matcher m = Pattern.compile ( "\\S+" ).matcher ( qstr );

        if ( m.find() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append ( CObj.docStringText ( CObj.SUBJECT ) );
            sb.append ( ":\"" );
            sb.append ( qstr );
            sb.append ( "\" OR " );
            sb.append ( CObj.docText ( CObj.BODY ) );
            sb.append ( ":\"" );
            sb.append ( qstr );
            sb.append ( "\" OR " );
            sb.append ( CObj.docStringText ( CObj.NAME ) );
            sb.append ( ":\"" );
            sb.append ( qstr );
            sb.append ( "\"" );
            return search ( query, sb.toString(), Integer.MAX_VALUE, srt );
        }

        else
        {
            return search ( query, Integer.MAX_VALUE, srt );
        }

    }

    public CObj getFileInfo ( String id )
    {
        BooleanQuery query = new BooleanQuery();

        Term pstterm = new Term ( CObj.PARAM_TYPE, CObj.FILE );
        query.add ( new TermQuery ( pstterm ), BooleanClause.Occur.MUST );

        Term idterm = new Term ( CObj.PARAM_ID, id );
        query.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( query, 1 );

        if ( l.size() > 0 )
        {
            try
            {
                r = l.get ( 0 );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        l.close();
        return r;
    }

    public CObjList searchFiles ( String comid, String share, String qstr, Sort srt )
    {
        BooleanQuery query = new BooleanQuery();

        Term pstterm = new Term ( CObj.PARAM_TYPE, CObj.FILE );
        query.add ( new TermQuery ( pstterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        query.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.NUMBER_HAS ),
                                         0L, Long.MAX_VALUE, false, true );
        query.add ( nq, BooleanClause.Occur.MUST );


        if ( share != null )
        {
            Term shareterm = new Term ( CObj.docString ( CObj.SHARE_NAME ), share );
            query.add ( new TermQuery ( shareterm ), BooleanClause.Occur.MUST );
        }

        Matcher m = Pattern.compile ( "\\S+" ).matcher ( qstr );

        if ( m.find() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append ( CObj.docStringText ( CObj.NAME ) );
            sb.append ( ":\"" );
            sb.append ( qstr );
            sb.append ( "\"" );
            return search ( query, sb.toString(), Integer.MAX_VALUE, srt );
        }

        else
        {
            return search ( query, Integer.MAX_VALUE, srt );
        }

    }

    public CObjList getSubscriptions ( String comid, Sort s )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        bq.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE, s );
    }

    public CObjList getSubsUnsubs ( String comid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getMySubscriptions ( String comid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        bq.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getMemberSubscriptions ( String memid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        bq.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getMySubscriptions()
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        bq.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getFragments ( String wdig, String pdig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term dig = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        bq.add ( new TermQuery ( dig ), BooleanClause.Occur.MUST );

        Term ddig = new Term ( CObj.docString ( CObj.FRAGDIGEST ), pdig );
        bq.add ( new TermQuery ( ddig ), BooleanClause.Occur.MUST );

        SortField field = new SortField ( CObj.docNumber ( CObj.FRAGOFFSET ), SortField.Type.LONG );
        Sort sort = new Sort ( field );

        return search ( bq, Integer.MAX_VALUE, sort );
    }

    public CObj getFragment ( String comid, String wdig, String ddig, String dig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term ct = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( ct ), BooleanClause.Occur.MUST );

        Term wdt = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        bq.add ( new TermQuery ( wdt ), BooleanClause.Occur.MUST );

        Term tddt = new Term ( CObj.docString ( CObj.FRAGDIGEST ), ddig );
        bq.add ( new TermQuery ( tddt ), BooleanClause.Occur.MUST );

        Term ddt = new Term ( CObj.docString ( CObj.FRAGDIG ), dig );
        bq.add ( new TermQuery ( ddt ), BooleanClause.Occur.MUST );

        Term shf = new Term ( CObj.docPrivate ( CObj.COMPLETE ), "true" );
        bq.add ( new TermQuery ( shf ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList cl = search ( bq, Integer.MAX_VALUE );

        if ( cl.size() > 0 )
        {
            try
            {
                r = cl.get ( 0 );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        cl.close();
        return r;
    }

    public CObjList getFragmentsComplete ( String comid, String wdig, String fdig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        bq.add ( new TermQuery ( wterm ), BooleanClause.Occur.MUST );

        Term fterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fdig );
        bq.add ( new TermQuery ( fterm ), BooleanClause.Occur.MUST );

        Term cpltterm = new Term ( CObj.docPrivate ( CObj.COMPLETE ), "true" );
        bq.add ( new TermQuery ( cpltterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getFragmentsToRequest ( String comid, String wdig, String fdig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        bq.add ( new TermQuery ( wterm ), BooleanClause.Occur.MUST );

        Term fterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fdig );
        bq.add ( new TermQuery ( fterm ), BooleanClause.Occur.MUST );

        Term cpltterm = new Term ( CObj.docPrivate ( CObj.COMPLETE ), "false" );
        bq.add ( new TermQuery ( cpltterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getFragmentsToReset ( String comid, String wdig, String fdig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        bq.add ( new TermQuery ( wterm ), BooleanClause.Occur.MUST );

        Term fterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fdig );
        bq.add ( new TermQuery ( fterm ), BooleanClause.Occur.MUST );

        Term cpltterm = new Term ( CObj.docPrivate ( CObj.COMPLETE ), "req" );
        bq.add ( new TermQuery ( cpltterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getFragments ( String digs )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term dig = new Term ( CObj.docString ( CObj.FRAGDIG ), digs );
        bq.add ( new TermQuery ( dig ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getPosts ( String comid, String memid, long first, long last )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.POST );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        bq.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.SEQNUM ),
                                         first, last, true, true );
        bq.add ( nq, BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getHasFiles ( String comid, String memid, long first, long last )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        bq.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.SEQNUM ),
                                         first, last, true, true );
        bq.add ( nq, BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getHasFiles ( String comid, String wdig, String pdig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wdigterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        bq.add ( new TermQuery ( wdigterm ), BooleanClause.Occur.MUST );

        Term pdigterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), pdig );
        bq.add ( new TermQuery ( pdigterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), "true" );
        bq.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getMyHasFiles ( String comid, String wdig, String pdig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wdigterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        bq.add ( new TermQuery ( wdigterm ), BooleanClause.Occur.MUST );

        Term pdigterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), pdig );
        bq.add ( new TermQuery ( pdigterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), "true" );
        bq.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        Term myterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( myterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getAllMyHasFiles ( )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), "true" );
        bq.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        Term myterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( myterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getAllHasFiles ( )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getMyHasFiles ( String wdig, String pdig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term wdigterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        bq.add ( new TermQuery ( wdigterm ), BooleanClause.Occur.MUST );

        Term pdigterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), pdig );
        bq.add ( new TermQuery ( pdigterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), "true" );
        bq.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        Term myterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( myterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getLocalHasFiles ( String comid, String memid, String localfile )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        bq.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term lfterm = new Term ( CObj.docPrivate ( CObj.LOCALFILE ), localfile );
        bq.add ( new TermQuery ( lfterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), "true" );
        bq.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObj getIdentHasFile ( String comid, String uid, String wdig, String pdig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wdigterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        bq.add ( new TermQuery ( wdigterm ), BooleanClause.Occur.MUST );

        Term pdigterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), pdig );
        bq.add ( new TermQuery ( pdigterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docString ( CObj.CREATOR ), uid );
        bq.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), "true" );
        bq.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList cl = search ( bq, Integer.MAX_VALUE );

        if ( cl.size() > 0 )
        {
            try
            {
                r = cl.get ( 0 );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        cl.close();
        return r;
    }

    public CObj getIdentity ( String id )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term idterm = new Term ( CObj.PARAM_ID, id );
        bq.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( bq, 1 );

        if ( l.size() > 0 )
        {
            try
            {
                r = l.get ( 0 );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        l.close();
        return r;
    }

    public CObj getMyIdentity ( String id )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term idterm = new Term ( CObj.PARAM_ID, id );
        bq.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( bq, 1 );

        if ( l.size() > 0 )
        {
            try
            {
                r = l.get ( 0 );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        l.close();
        return r;
    }

    public CObjList getMyIdentities()
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        bq.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObj getCommunity ( String comid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term idterm = new Term ( CObj.PARAM_DIG, comid );
        bq.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( bq, 1 );

        if ( l.size() > 0 )
        {
            try
            {
                r = l.get ( 0 );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        l.close();
        return r;
    }

    public CObjList getUnDecodedMemberships ( long lastdec )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term nondec = new Term ( CObj.docPrivate ( CObj.DECODED ), "false" );
        bq.add ( new TermQuery ( nondec ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docPrivateNumber ( CObj.LASTUPDATE ),
                                         lastdec, Long.MAX_VALUE, true, true );
        bq.add ( nq, BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getMembership ( String comid, String memid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docPrivate ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term midterm = new Term ( CObj.docPrivate ( CObj.MEMBERID ), memid );
        bq.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "true" );
        bq.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getInvalidMembership ( String comid, String memid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docPrivate ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term midterm = new Term ( CObj.docPrivate ( CObj.MEMBERID ), memid );
        bq.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "false" );
        bq.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        Term decterm = new Term ( CObj.docPrivate ( CObj.DECODED ), "true" );
        bq.add ( new TermQuery ( decterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObjList getPushesToSend()
    {
        BooleanQuery bq = new BooleanQuery();

        //For a BooleanQuery with no MUST clauses one or more SHOULD clauses
        //must match a document for the BooleanQuery to match.
        Term decterm = new Term ( CObj.docPrivate ( CObj.PRV_PUSH_REQ ), "true" );
        bq.add ( new TermQuery ( decterm ), BooleanClause.Occur.SHOULD );

        Term nocterm = new Term ( CObj.docPrivate ( CObj.PRV_PUSH_REQ ), "nocon" );
        bq.add ( new TermQuery ( nocterm ), BooleanClause.Occur.SHOULD );

        Sort s = new Sort();
        s.setSort ( new SortField ( CObj.docPrivateNumber ( CObj.PRV_PUSH_TIME ), SortField.Type.LONG, false ) );

        return search ( bq, Integer.MAX_VALUE, s );
    }

    public CObjList getPushesToConnect()
    {
        BooleanQuery bq = new BooleanQuery();

        Term decterm = new Term ( CObj.docPrivate ( CObj.PRV_PUSH_REQ ), "true" );
        bq.add ( new TermQuery ( decterm ), BooleanClause.Occur.MUST );

        Sort s = new Sort();
        s.setSort ( new SortField ( CObj.docPrivateNumber ( CObj.PRV_PUSH_TIME ), SortField.Type.LONG, false ) );

        return search ( bq, Integer.MAX_VALUE, s );
    }

    public CObjList getInvalidMemberships()
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "false" );
        bq.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        Term decterm = new Term ( CObj.docPrivate ( CObj.DECODED ), "true" );
        bq.add ( new TermQuery ( decterm ), BooleanClause.Occur.MUST );

        return search ( bq, Integer.MAX_VALUE );
    }

    public CObj getSubscription ( String comid, String memid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term midterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        bq.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        bq.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( bq, 1 );

        if ( l.size() > 0 )
        {
            try
            {
                r = l.get ( 0 );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        l.close();
        return r;
    }

    public CObj getSubscriptionUnsub ( String comid, String memid )
    {
        BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        bq.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        bq.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term midterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        bq.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( bq, 1 );

        if ( l.size() > 0 )
        {
            try
            {
                r = l.get ( 0 );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        l.close();
        return r;
    }

    public CObj getByDig ( String dig )
    {
        BooleanQuery bq = new BooleanQuery();
        Term midterm = new Term ( CObj.PARAM_DIG, dig );
        bq.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( bq, 1 );

        if ( l.size() > 0 )
        {
            try
            {
                r = l.get ( 0 );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        l.close();
        return r;
    }

    private void indexNoCommit ( CObj o, boolean onlynew ) throws IOException
    {
        if ( o.getDig() == null && o.getId() == null )
        {
            throw new IOException ( "Digest or id required!" );
        }

        boolean indexit = true;
        Term updateterm = null;

        if ( o.getId() != null )
        {
            updateterm = new Term ( "id", o.getId() );
            Query id0 = new TermQuery ( updateterm );

            if ( onlynew )
            {
                CObjList cl = search ( id0, 1 );
                indexit = ( cl.size() == 0 );
                cl.close();
            }

        }

        if ( o.getDig() != null && o.getId() == null )
        {
            updateterm = new Term ( "dig", o.getDig() );
            Query id0 = new TermQuery ( updateterm );

            if ( onlynew )
            {
                CObjList cl = search ( id0, 1 );
                indexit = ( cl.size() == 0 );
                cl.close();
            }

        }

        if ( indexit )
        {
            Document d = o.getDocument();
            writer.updateDocument ( updateterm, d );
        }

    }

    public void delete ( CObj o ) throws IOException
    {
        if ( o.getDig() == null && o.getId() == null )
        {
            throw new IOException ( "Digest or id required!" );
        }

        Term updateterm = null;

        if ( o.getId() != null )
        {
            updateterm = new Term ( "id", o.getId() );
        }

        if ( o.getDig() != null && o.getId() == null )
        {
            updateterm = new Term ( "dig", o.getDig() );
        }

        if ( updateterm != null )
        {
            writer.deleteDocuments ( updateterm );
            writer.commit();
        }

    }

    public void index ( CObj o, boolean onlynew ) throws IOException
    {
        indexNoCommit ( o, onlynew );
        writer.commit();
    }

    public void index ( List<CObj> l, boolean onlynew ) throws IOException
    {
        for ( CObj o : l )
        {
            indexNoCommit ( o, onlynew );
        }

        writer.commit();
    }

    public void index ( CObj o ) throws IOException
    {
        indexNoCommit ( o, false );
        writer.commit();
    }

    public void index ( List<CObj> l ) throws IOException
    {
        for ( CObj o : l )
        {
            indexNoCommit ( o, false );
        }

        writer.commit();
    }

}
