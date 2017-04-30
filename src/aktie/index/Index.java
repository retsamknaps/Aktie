package aktie.index;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;

import aktie.data.CObj;
import aktie.data.IdentityData;

public class Index implements Runnable
{
    // HasPart
    /**
        Maximum age of a part file.
        Older files will not be accepted by InHasPartProcessor
        and get deleted by ShareManager.
    */
    public static final long HAS_PART_MAX_AGE = 365L * 24L * 60L * 60L * 1000L;

    // * NOTE *
    // This IS thread safe!
    private Analyzer analyzer;
    private IndexWriter writer;
    //private AktieSearcher searcher;

    private File indexdir;

    public Index()
    {
    }

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
        SimpleFSDirectory fsdir = new SimpleFSDirectory ( indexdir.toPath() );
        //analyzer = new StandardAnalyzer();
        analyzer = new GenenskapAnalyzer();
        IndexWriterConfig idxconf = new IndexWriterConfig ( analyzer );
        writer = new IndexWriter ( fsdir, idxconf );
        writer.commit();
        Thread t = new Thread ( this );
        t.setDaemon ( true );
        t.start();

    }

    public void forceNewSearcher()
    {
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
            AktieSearcher cs = AktieSearcher.newSearcher ( indexdir.getPath(), writer );
            CObjList l = new CObjList ( cs, q, srt );
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
            //DirectoryReader reader = DirectoryReader.open ( writer, true );
            //IndexSearcher searcher = new IndexSearcher ( reader );
            AktieSearcher cs = AktieSearcher.newSearcher ( indexdir.getPath(), writer );

            CObjList l = new CObjList ( cs, analyzer, qs, s );
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
            //DirectoryReader reader = DirectoryReader.open ( writer, true );
            //IndexSearcher searcher = new IndexSearcher ( reader );
            AktieSearcher cs = AktieSearcher.newSearcher ( indexdir.getPath(), writer );

            CObjList l = new CObjList ( bq, cs, analyzer, qs, s );
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
            //DirectoryReader reader = DirectoryReader.open ( writer, true );
            //IndexSearcher searcher = new IndexSearcher ( reader );
            AktieSearcher cs = AktieSearcher.newSearcher ( indexdir.getPath(), writer );

            CObjList l = new CObjList ( bq, cs, analyzer, qs, null );
            l.executeQuery ( max );
            return l;
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;

    }

    public CObjList getMemMissingSeqNumbers ( String ident, int max )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
                                          0L, Long.MAX_VALUE, true, true );

        builder.add ( nrq, BooleanClause.Occur.MUST_NOT );

        SortedNumericSortField field = new SortedNumericSortField (
            CObj.docPrivateNumber ( CObj.docNumber ( CObj.SEQNUM ) ),
            SortField.Type.LONG );
        Sort sort = new Sort ( field );

        return search ( builder.build(), max, sort );
    }

    public CObjList getSubMissingSeqNumbers ( String ident, int max )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        Term     typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.POST );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
                                          0L, Long.MAX_VALUE, true, true );

        builder.add ( nrq, BooleanClause.Occur.MUST_NOT );

        SortedNumericSortField field = new SortedNumericSortField (
            CObj.docPrivateNumber ( CObj.docNumber ( CObj.SEQNUM ) ),
            SortField.Type.LONG );
        Sort sort = new Sort ( field );

        return search ( builder.build(), max, sort );
    }

    public CObjList getPubMissingSeqNumbers ( String ident, int max )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVIDENTIFIER );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVMESSAGE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.SPAMEXCEPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
                                          0L, Long.MAX_VALUE, true, true );
        builder.add ( nrq, BooleanClause.Occur.MUST_NOT );

        SortedNumericSortField field = new SortedNumericSortField (
            CObj.docPrivateNumber ( CObj.docNumber ( CObj.SEQNUM ) ),
            SortField.Type.LONG );
        Sort sort = new Sort ( field );

        return search ( builder.build(), max, sort );
    }

    public CObjList getCommunitySeqNumbers ( String ident, String comid, long lastseq, long curseq )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        BooleanQuery.Builder tb = new BooleanQuery.Builder();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        tb.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        Term typterm2 = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        tb.add ( new TermQuery ( typterm2 ), BooleanClause.Occur.SHOULD );
        Term typterm3 = new Term ( CObj.PARAM_TYPE, CObj.HASPART );
        tb.add ( new TermQuery ( typterm3 ), BooleanClause.Occur.SHOULD );
        Term typterm4 = new Term ( CObj.PARAM_TYPE, CObj.POST );
        tb.add ( new TermQuery ( typterm4 ), BooleanClause.Occur.SHOULD );
        builder.add ( tb.build(), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
                                          lastseq, curseq, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        SortedNumericSortField field = new SortedNumericSortField (
            CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
            SortField.Type.LONG );
        Sort sort = new Sort ( field );

        return search ( builder.build(), ( int ) ( IdentityData.MAXGLOBALSEQUENCECOUNT * 2 ), sort );
    }

    public CObjList getGlobalMemSeqNumbers ( String ident, long lastseq, long curseq )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
                                          lastseq, curseq, false, true );

        builder.add ( nrq, BooleanClause.Occur.MUST );

        SortedNumericSortField field = new SortedNumericSortField (
            CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
            SortField.Type.LONG );

        Sort sort = new Sort ( field );

        return search ( builder.build(), ( int ) ( IdentityData.MAXGLOBALSEQUENCECOUNT * 2 ), sort );
    }

    public CObjList getGlobalSubSeqNumbers ( String ident, long lastseq, long curseq )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        BooleanQuery.Builder sb = new BooleanQuery.Builder();
        Term     typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        sb.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.POST );
        sb.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        builder.add ( sb.build(), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
                                          lastseq, curseq, false, true );

        builder.add ( nrq, BooleanClause.Occur.MUST );

        SortedNumericSortField field = new SortedNumericSortField (
            CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
            SortField.Type.LONG );

        Sort sort = new Sort ( field );

        return search ( builder.build(), ( int ) ( IdentityData.MAXGLOBALSEQUENCECOUNT * 2 ), sort );
    }

    public CObjList getGlobalPubSeqNumbers ( String ident, long lastseq, long curseq )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        BooleanQuery.Builder sb = new BooleanQuery.Builder();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVIDENTIFIER );
        sb.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVMESSAGE );
        sb.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        sb.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        sb.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        sb.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        typterm = new Term ( CObj.PARAM_TYPE, CObj.SPAMEXCEPTION );
        sb.add ( new TermQuery ( typterm ), BooleanClause.Occur.SHOULD );
        builder.add ( sb.build(), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
                                          lastseq, curseq, false, true );

        builder.add ( nrq, BooleanClause.Occur.MUST );

        SortedNumericSortField field = new SortedNumericSortField (
            CObj.docPrivateNumber ( CObj.getGlobalSeq ( ident ) ),
            SortField.Type.LONG );

        Sort sort = new Sort ( field );

        return search ( builder.build(), ( int ) ( IdentityData.MAXGLOBALSEQUENCECOUNT * 2 ), sort );
    }

    public CObjList getAllCObj()
    {
        MatchAllDocsQuery alld = new MatchAllDocsQuery();

        return search ( alld, ( int ) Integer.MAX_VALUE );
    }

    public CObjList getAllPrivIdents()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVIDENTIFIER );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getPrivateMsgIdentForIdentity ( String id )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVIDENTIFIER );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        BooleanQuery.Builder brcp = new BooleanQuery.Builder();
        Term memterm = new Term ( CObj.docPrivate ( CObj.PRV_RECIPIENT ), id );
        brcp.add ( new TermQuery ( memterm ), BooleanClause.Occur.SHOULD );

        Term cterm = new Term ( CObj.docString ( CObj.CREATOR ), id );
        brcp.add ( new TermQuery ( cterm ), BooleanClause.Occur.SHOULD );

        builder.add ( brcp.build(), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.DECODED ), "true" );
        builder.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getPrivateMyMsgIdentity ( String msgid, Sort s )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVIDENTIFIER );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docPrivate ( CObj.PRV_MSG_ID ), msgid );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObj getPrivateMsgIdentity ( String creator, String msgid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVIDENTIFIER );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.MSGIDENT ), msgid );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        Term cterm = new Term ( CObj.docString ( CObj.CREATOR ), creator );
        builder.add ( new TermQuery ( cterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( builder.build(), 1 );

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

    public CObjList getPrivateMsgNotDecoded ( String id )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVMESSAGE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.MSGIDENT ), id );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.DECODED ), "false" );
        builder.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getIdentities()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getZeroIdentities()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, 0L, true, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getCreatedBy ( String id )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term cterm = new Term ( CObj.docString ( CObj.CREATOR ), id );
        builder.add ( new TermQuery ( cterm ), BooleanClause.Occur.SHOULD );

        Term pcterm = new Term ( CObj.docPrivate ( CObj.CREATOR ), id );
        builder.add ( new TermQuery ( pcterm ), BooleanClause.Occur.SHOULD );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getDecodedPrvIdentifiers()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVIDENTIFIER );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docPrivate ( CObj.DECODED ), "true" );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getDecodedPrvMessages ( String mid, Sort s )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.PRIVMESSAGE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docPrivate ( CObj.PRV_MSG_ID ), mid );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList getSubscriptions ( String creator, long first, long last )
    {
        return getCreatorObjs ( CObj.SUBSCRIPTION, creator, first, last );
    }

    public CObjList getCommunities ( String creator, long first, long last )
    {
        return getCreatorObjs ( CObj.COMMUNITY, creator, first, last );
    }

    public CObjList getPrvIdent ( String creator, long first, long last )
    {
        return getCreatorObjs ( CObj.PRIVIDENTIFIER, creator, first, last );
    }

    public CObjList getAllSpamEx()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SPAMEXCEPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getSpamEx ( String creator, long first, long last )
    {
        int maxvals = ( int ) ( last - first );

        if ( maxvals <= 0 || maxvals > 1000 )
        {
            maxvals = 1000;
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SPAMEXCEPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), creator );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.SEQNUM ),
                                         first, Long.MAX_VALUE, true, true );
        builder.add ( nq, BooleanClause.Occur.MUST );

        return search ( builder.build(), maxvals );
    }

    public CObjList getPrvMsg ( String creator, long first, long last )
    {
        return getCreatorObjs ( CObj.PRIVMESSAGE, creator, first, last );
    }

    public CObjList getMemberships ( String creator, long first, long last )
    {
        return getCreatorObjs ( CObj.MEMBERSHIP, creator, first, last );
    }

    private CObjList getCreatorObjs ( String ctyp, String creator, long first, long last )
    {
        int maxvals = ( int ) ( last - first );

        if ( maxvals <= 0 || maxvals > 1000 )
        {
            maxvals = 1000;
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, ctyp );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), creator );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.SEQNUM ),
                                         first, Long.MAX_VALUE, true, true );
        builder.add ( nq, BooleanClause.Occur.MUST );

        return search ( builder.build(), maxvals );
    }

    //Could be based on creator and sequence number, but
    //not enough to worry about.  Just send all.
    public CObjList getIdentityPrivateCommunities ( String creator )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), creator );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term scopeterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PRIVATE );
        builder.add ( new TermQuery ( scopeterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getPublicCommunities()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        Term pubterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PUBLIC );
        builder.add ( new TermQuery ( pubterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getValidCommunities()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term valterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( valterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    //Could be based on creator and sequence number, but
    //not enough to worry about.  Just send all.
    //send ALL, even ones not maked decoded or valid
    public CObjList getIdentityMemberships ( String memid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docPrivate ( CObj.MEMBERID ), memid );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "true" );
        builder.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        CObjList r = search ( builder.build(), Integer.MAX_VALUE );
        return r;
    }

    public CObjList getMemberships ( String comid, Sort s )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docPrivate ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "true" );
        builder.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList getMyMemberships ( Sort s )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        Term privterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PRIVATE );
        builder.add ( new TermQuery ( privterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList getMyValidMemberships ( Sort s )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        BooleanQuery.Builder builder2 = new BooleanQuery.Builder();
        //BooleanQuery bq2 = new BooleanQuery();
        CObjList mlst = this.getMyIdentities();

        for ( int c = 0; c < mlst.size(); c++ )
        {
            try
            {
                CObj mi = mlst.get ( c );
                Term tt = new Term ( CObj.docPrivate ( mi.getId() ), "true" );
                builder2.add ( new TermQuery ( tt ), BooleanClause.Occur.SHOULD );
            }

            catch ( Exception e )
            {
            }

        }

        mlst.close();
        builder.add ( builder2.build(), BooleanClause.Occur.MUST );

        Term privterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PRIVATE );
        builder.add ( new TermQuery ( privterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList getPublicCommunities ( Sort s )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term privterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PUBLIC );
        builder.add ( new TermQuery ( privterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList getMyMemberships ( String comid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docPrivate ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term validmem = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "true" );
        builder.add ( new TermQuery ( validmem ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList searchIdenties ( String squery, Sort s )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery idq = new BooleanQuery();
        Term idterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        builder.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        Matcher m = Pattern.compile ( "\\S+" ).matcher ( squery );

        if ( m.find() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append ( CObj.docPrivateText ( CObj.PRV_DISPLAY_NAME ) );
            sb.append ( ":\"" );
            sb.append ( squery );
            sb.append ( "\" OR " );
            sb.append ( CObj.docStringText ( CObj.DESCRIPTION ) );
            sb.append ( ":\"" );
            sb.append ( squery );
            sb.append ( "\"" );
            return search ( builder.build(), sb.toString(), Integer.MAX_VALUE, s );
        }

        else
        {
            return search ( builder.build(), Integer.MAX_VALUE, s );
        }

    }

    public CObjList searchSemiPrivateCommunities ( String squery,  Sort s )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term mineterm = new Term ( CObj.docPrivate ( CObj.MINE ), "false" );
        builder.add ( new TermQuery ( mineterm ), BooleanClause.Occur.MUST );

        Term privterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PRIVATE );
        builder.add ( new TermQuery ( privterm ), BooleanClause.Occur.MUST );

        Term pnterm = new Term ( CObj.docString ( CObj.NAME_IS_PUBLIC ), "true" );
        builder.add ( new TermQuery ( pnterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        if ( squery != null )
        {
            Matcher m = Pattern.compile ( "\\S+" ).matcher ( squery );

            if ( m.find() )
            {
                StringBuilder sb = new StringBuilder();
                sb.append ( CObj.docPrivateText ( CObj.PRV_DISPLAY_NAME ) );
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
                return search ( builder.build(), sb.toString(), Integer.MAX_VALUE );
            }

        }

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList searchSubscribable ( String squery, String memid, boolean prv, boolean pub, Sort s )
    {
        if ( !prv && !pub )
        {
            prv = true;
            pub = true;
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery combined = new BooleanQuery();
        builder.setMinimumNumberShouldMatch ( 1 );

        Term comterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        if ( pub )
        {

            Term pubterm = new Term ( CObj.docString ( CObj.SCOPE ), CObj.SCOPE_PUBLIC );
            builder.add ( new TermQuery ( pubterm ), BooleanClause.Occur.SHOULD );

        }

        if ( prv && memid != null )
        {

            Term pubterm2 = new Term ( CObj.docPrivate ( memid ), "true" );
            builder.add ( new TermQuery ( pubterm2 ), BooleanClause.Occur.SHOULD );

        }

        if ( squery != null )
        {
            Matcher m = Pattern.compile ( "\\S+" ).matcher ( squery );

            if ( m.find() )
            {
                StringBuilder sb = new StringBuilder();
                sb.append ( CObj.docPrivateText ( CObj.PRV_DISPLAY_NAME ) );
                sb.append ( ":\"" );
                sb.append ( squery );
                sb.append ( "\" OR " );
                sb.append ( CObj.docPrivateText ( CObj.DESCRIPTION ) );
                sb.append ( ":\"" );
                sb.append ( squery );
                sb.append ( "\"" );
                return search ( builder.build(), sb.toString(), Integer.MAX_VALUE, s );
            }

        }

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList getQueries ( String comid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.QUERY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getAutodownloadQueries()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.QUERY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docPrivate ( CObj.PRV_QRY_AUTODOWNLOAD ), "true" );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList searchPostsQuery ( List<CObj> qlst, Sort srt )
    {
        Matcher sm = Pattern.compile ( "\\S+" ).matcher ( "" );

        BooleanQuery.Builder topbuild = new BooleanQuery.Builder();
        Iterator<CObj> qi = qlst.iterator();

        while ( qi.hasNext() )
        {
            CObj query = qi.next();

            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            //BooleanQuery query = new BooleanQuery();

            Term pstterm = new Term ( CObj.PARAM_TYPE, CObj.POST );
            builder.add ( new TermQuery ( pstterm ), BooleanClause.Occur.MUST );

            String comid = query.getString ( CObj.COMMUNITYID );

            if ( comid != null )
            {
                Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
                builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );
            }

            // Queries have to have a local creator identity for auto
            // downloads, so another field beside CREATOR will have to
            // be used if we want to actually do this.  No support
            // right now though.
            //String creator = query.getString ( CObj.CREATOR );
            //
            //if ( creator != null )
            //{
            //    Term crterm = new Term ( CObj.docString ( CObj.CREATOR ), creator );
            //    builder.add ( new TermQuery ( crterm ), BooleanClause.Occur.MUST );
            //}

            Long minusrrank = query.getNumber ( CObj.QRY_MIN_USER_RANK );
            Long maxusrrank = query.getNumber ( CObj.QRY_MAX_USER_RANK );

            if ( minusrrank != null || maxusrrank != null )
            {
                long min = 0L;
                long max = Long.MAX_VALUE;

                if ( minusrrank != null )
                {
                    min = Long.valueOf ( minusrrank );
                }

                if ( maxusrrank != null )
                {
                    max = Long.valueOf ( maxusrrank );
                }

                NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                                 CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                                 min, max, true, true );
                builder.add ( nq, BooleanClause.Occur.MUST );
            }

            Long mindate = query.getNumber ( CObj.QRY_MIN_DATE );
            Long maxdate = query.getNumber ( CObj.QRY_MAX_DATE );

            Long db = query.getNumber ( CObj.QRY_DAYS_BACK );

            if ( db != null && db > 0 )
            {
                maxdate = null;
                Date today = new Date();
                mindate = today.getTime() - ( db * 24L * 60L * 60L * 1000L );
            }

            if ( mindate != null || maxdate != null )
            {
                long min = 0L;
                long max = Long.MAX_VALUE;

                if ( mindate != null )
                {
                    min = Long.valueOf ( mindate );
                }

                if ( maxdate != null )
                {
                    max = Long.valueOf ( maxdate );
                }

                NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                                 CObj.docNumber ( CObj.CREATEDON ),
                                                 min, max, true, true );
                builder.add ( nq, BooleanClause.Occur.MUST );
            }

            Long minsize = query.getNumber ( CObj.QRY_MIN_FILE_SIZE );
            Long maxsize = query.getNumber ( CObj.QRY_MAX_FILE_SIZE );

            if ( minsize != null || maxsize != null )
            {
                long min = 0L;
                long max = Long.MAX_VALUE;

                if ( minsize != null )
                {
                    min = Long.valueOf ( minsize );
                }

                if ( maxsize != null )
                {
                    max = Long.valueOf ( maxsize );
                }

                NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                                 CObj.docNumber ( CObj.FILESIZE ),
                                                 min, max, true, true );
                builder.add ( nq, BooleanClause.Occur.MUST );
            }

            List<CObj> fq = query.listNewFields();
            Iterator<CObj> i = fq.iterator();

            while ( i.hasNext() )
            {
                CObj qf = i.next();

                String valuekey = CObj.FLD + CObj.getSubid ( qf.getDig() );

                String typ = qf.getString ( CObj.FLD_TYPE );

                if ( CObj.FLD_TYPE_BOOL.equals ( typ ) ||
                        CObj.FLD_TYPE_OPT.equals ( typ ) ||
                        CObj.FLD_TYPE_STRING.equals ( typ ) )
                {
                    String val = query.getString ( valuekey );

                    if ( val != null )
                    {
                        Term term = new Term ( CObj.docString ( valuekey ), val );
                        builder.add ( new TermQuery ( term ), BooleanClause.Occur.MUST );

                    }

                }

                if ( CObj.FLD_TYPE_DECIMAL.equals ( typ ) )
                {
                    Double max = qf.getDecimal ( CObj.FLD_MAX );
                    Double min = qf.getDecimal ( CObj.FLD_MIN );

                    if ( max != null && min != null )
                    {
                        NumericRangeQuery<Double> rq = NumericRangeQuery.newDoubleRange (
                                                           CObj.docDecimal ( valuekey ),
                                                           min, max, true, true );
                        builder.add ( rq, BooleanClause.Occur.MUST );
                    }

                }

                if ( CObj.FLD_TYPE_NUMBER.equals ( typ ) )
                {
                    Long max = qf.getNumber ( CObj.FLD_MAX );
                    Long min = qf.getNumber ( CObj.FLD_MIN );

                    if ( max != null && min != null )
                    {
                        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                                         CObj.docNumber ( valuekey ),
                                                         min, max, true, true );
                        builder.add ( nq, BooleanClause.Occur.MUST );
                    }

                }

            }

            String subj = query.getString ( CObj.SUBJECT );

            if ( subj != null )
            {
                sm.reset ( subj );

                if ( sm.find() )
                {
                    try
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append ( CObj.docStringText ( CObj.SUBJECT ) );
                        sb.append ( ":\"" );
                        sb.append ( subj );
                        sb.append ( "\" OR " );
                        sb.append ( CObj.docText ( CObj.BODY ) );
                        sb.append ( ":\"" );
                        sb.append ( subj );
                        sb.append ( "\"" );
                        QueryParser qp = new QueryParser ( "text_title", analyzer );
                        Query sq = qp.parse ( sb.toString() );
                        builder.add ( sq, BooleanClause.Occur.MUST );
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

            }

            topbuild.add ( builder.build(), BooleanClause.Occur.SHOULD );
        }

        return search ( topbuild.build(), Integer.MAX_VALUE, srt );
    }

    public CObjList searchPosts ( String comid, String qstr, Sort srt )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery query = new BooleanQuery();

        Term pstterm = new Term ( CObj.PARAM_TYPE, CObj.POST );
        builder.add ( new TermQuery ( pstterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

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
            sb.append ( CObj.docStringText ( CObj.NAME ) ); //File name
            sb.append ( ":\"" );
            sb.append ( qstr );
            sb.append ( "\"" );
            return search ( builder.build(), sb.toString(), Integer.MAX_VALUE, srt );
        }

        else
        {
            return search ( builder.build(), Integer.MAX_VALUE, srt );
        }

    }

    public CObj getField ( String id )
    {
        return null;
    }

    public CObjList searchFields ( String comid, String qstr, Sort srt )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery query = new BooleanQuery();

        Term pstterm = new Term ( CObj.PARAM_TYPE, CObj.FIELD );
        builder.add ( new TermQuery ( pstterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Matcher m = Pattern.compile ( "\\S+" ).matcher ( qstr );

        if ( m.find() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append ( CObj.docStringText ( CObj.FLD_NAME ) );
            sb.append ( ":\"" );
            sb.append ( qstr );
            sb.append ( "\" OR " );
            sb.append ( CObj.docStringText ( CObj.FLD_DESC ) );
            sb.append ( ":\"" );
            sb.append ( qstr );
            sb.append ( "\"" );
            return search ( builder.build(), sb.toString(), Integer.MAX_VALUE, srt );
        }

        else
        {
            return search ( builder.build(), Integer.MAX_VALUE, srt );
        }

    }

    public CObj getFileInfo ( String id )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery query = new BooleanQuery();

        Term pstterm = new Term ( CObj.PARAM_TYPE, CObj.FILE );
        builder.add ( new TermQuery ( pstterm ), BooleanClause.Occur.MUST );

        Term idterm = new Term ( CObj.PARAM_ID, id );
        builder.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( builder.build(), 1 );

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
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery query = new BooleanQuery();

        Term pstterm = new Term ( CObj.PARAM_TYPE, CObj.FILE );
        builder.add ( new TermQuery ( pstterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.NUMBER_HAS ),
                                         0L, Long.MAX_VALUE, false, true );
        builder.add ( nq, BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        if ( share != null )
        {
            Term shareterm = new Term ( CObj.docString ( CObj.SHARE_NAME ), share );
            builder.add ( new TermQuery ( shareterm ), BooleanClause.Occur.MUST );
        }

        Matcher m = Pattern.compile ( "\\S+" ).matcher ( qstr );

        if ( m.find() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append ( CObj.docStringText ( CObj.NAME ) );
            sb.append ( ":\"" );
            sb.append ( qstr );
            sb.append ( "\"" );
            return search ( builder.build(), sb.toString(), Integer.MAX_VALUE, srt );
        }

        else
        {
            return search ( builder.build(), Integer.MAX_VALUE, srt );
        }

    }

    public CObjList getSubscriptions ( String comid, Sort s )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        builder.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList getSubsUnsubs ( String comid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getMySubscriptions ( String comid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        builder.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getMySeqSubscriptions ( String creator, long num )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.CREATOR ), creator );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.SEQNUM ),
                                         num, num, true, true );
        builder.add ( nq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getDefFields ( String comid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FIELD );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.PRV_DEF_FIELD ), "true" );
        builder.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getMemberSubscriptions ( String memid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        builder.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getMySubscriptions()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        builder.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getFragments ( String wdig, String pdig )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term dig = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        builder.add ( new TermQuery ( dig ), BooleanClause.Occur.MUST );

        Term ddig = new Term ( CObj.docString ( CObj.FRAGDIGEST ), pdig );
        builder.add ( new TermQuery ( ddig ), BooleanClause.Occur.MUST );

        SortedNumericSortField field = new SortedNumericSortField ( CObj.docNumber ( CObj.FRAGOFFSET ), SortField.Type.LONG );
        Sort sort = new Sort ( field );

        return search ( builder.build(), Integer.MAX_VALUE, sort );
    }

    public CObj getFragment ( String comid, String wdig, String ddig, String dig )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term ct = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( ct ), BooleanClause.Occur.MUST );

        Term wdt = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        builder.add ( new TermQuery ( wdt ), BooleanClause.Occur.MUST );

        Term tddt = new Term ( CObj.docString ( CObj.FRAGDIGEST ), ddig );
        builder.add ( new TermQuery ( tddt ), BooleanClause.Occur.MUST );

        Term ddt = new Term ( CObj.docString ( CObj.FRAGDIG ), dig );
        builder.add ( new TermQuery ( ddt ), BooleanClause.Occur.MUST );

        Term shf = new Term ( CObj.docPrivate ( CObj.COMPLETE ), "true" );
        builder.add ( new TermQuery ( shf ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList cl = search ( builder.build(), Integer.MAX_VALUE );

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

    // HasPart
    public CObj getFragment ( String fileDigest, String fragDigest, String fragDig )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        Term fileDigestTerm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( fileDigestTerm ), BooleanClause.Occur.MUST );

        Term fragDigestTerm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( fragDigestTerm ), BooleanClause.Occur.MUST );

        Term fragDigTerm = new Term ( CObj.docString ( CObj.FRAGDIG ), fragDig );
        builder.add ( new TermQuery ( fragDigTerm ), BooleanClause.Occur.MUST );

        Term shf = new Term ( CObj.docPrivate ( CObj.COMPLETE ), CObj.TRUE );
        builder.add ( new TermQuery ( shf ), BooleanClause.Occur.MUST );

        CObj fragment = null;
        CObjList matchingFragments = search ( builder.build(), Integer.MAX_VALUE );

        if ( matchingFragments.size() > 0 )
        {
            try
            {
                fragment = matchingFragments.get ( 0 );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        matchingFragments.close();
        return fragment;
    }

    public CObjList getFragmentsComplete ( String fileDigest, String fragDigest )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term wterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( wterm ), BooleanClause.Occur.MUST );

        Term fterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( fterm ), BooleanClause.Occur.MUST );

        Term cpltterm = new Term ( CObj.docPrivate ( CObj.COMPLETE ), CObj.TRUE );
        builder.add ( new TermQuery ( cpltterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getFragmentsComplete ( String communityID, String fileDigest, String fragDigest )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), communityID );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( wterm ), BooleanClause.Occur.MUST );

        Term fterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( fterm ), BooleanClause.Occur.MUST );

        Term cpltterm = new Term ( CObj.docPrivate ( CObj.COMPLETE ), CObj.TRUE );
        builder.add ( new TermQuery ( cpltterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getFragmentsToRequest ( String communityID, String fileDigest, String fragDigest )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), communityID );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( wterm ), BooleanClause.Occur.MUST );

        Term fterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( fterm ), BooleanClause.Occur.MUST );

        Term cpltterm = new Term ( CObj.docPrivate ( CObj.COMPLETE ), CObj.FALSE );
        builder.add ( new TermQuery ( cpltterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getFragmentsToReset ( String comid, String wdig, String fdig )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        builder.add ( new TermQuery ( wterm ), BooleanClause.Occur.MUST );

        Term fterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fdig );
        builder.add ( new TermQuery ( fterm ), BooleanClause.Occur.MUST );

        Term cpltterm = new Term ( CObj.docPrivate ( CObj.COMPLETE ), CObj.REQUEST );
        builder.add ( new TermQuery ( cpltterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getFragments ( String digs )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.FRAGMENT );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term dig = new Term ( CObj.docString ( CObj.FRAGDIG ), digs );
        builder.add ( new TermQuery ( dig ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getPosts ( String comid, String memid, long first, long last )
    {
        int maxvals = ( int ) ( last - first );

        if ( maxvals <= 0 || maxvals > 1000 )
        {
            maxvals = 1000;
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.POST );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docNumber ( CObj.SEQNUM ),
                                         first, Long.MAX_VALUE, true, true );
        builder.add ( nq, BooleanClause.Occur.MUST );

        return search ( builder.build(), maxvals );
    }



    public CObjList getHasFiles ( String communityID, String memberID, long firstSeqNumber, long lastSeqNumber )
    {
        int maxValues = ( int ) ( lastSeqNumber - firstSeqNumber );

        if ( maxValues <= 0 || maxValues > 1000 )
        {
            maxValues = 1000;
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        Term communityTerm = new Term ( CObj.docString ( CObj.COMMUNITYID ), communityID );
        builder.add ( new TermQuery ( communityTerm ), BooleanClause.Occur.MUST );

        Term memberTerm = new Term ( CObj.docString ( CObj.CREATOR ), memberID );
        builder.add ( new TermQuery ( memberTerm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> rankTerm = NumericRangeQuery.newLongRange (
                                               CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                               0L, Long.MAX_VALUE, false, true );
        builder.add ( rankTerm, BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> seqTerm = NumericRangeQuery.newLongRange (
                                              CObj.docNumber ( CObj.SEQNUM ),
                                              firstSeqNumber, Long.MAX_VALUE, true, true );
        builder.add ( seqTerm, BooleanClause.Occur.MUST );

        return search ( builder.build(), maxValues );
    }

    // HasPart
    /*  public CObjList getHasFiles ( String fileDigest, String fragDigest )
        {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        Term fileDigestTerm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( fileDigestTerm ), BooleanClause.Occur.MUST );

        Term fragDigestTerm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( fragDigestTerm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), CObj.TRUE );
        builder.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> rankTerm = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( rankTerm, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
        }*/

    public CObjList getHasFiles ( String communityID, String fileDigest, String fragDigest )
    {
        return getHasFiles ( communityID, fileDigest, fragDigest, null );
    }

    public CObjList getHasFiles ( String communityID, String fileDigest, String fragDigest, Sort sort )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        Term communityTerm = new Term ( CObj.docString ( CObj.COMMUNITYID ), communityID );
        builder.add ( new TermQuery ( communityTerm ), BooleanClause.Occur.MUST );

        Term fileDigestTerm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( fileDigestTerm ), BooleanClause.Occur.MUST );

        Term fragDigestTerm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( fragDigestTerm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), CObj.TRUE );
        builder.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> rankTerm = NumericRangeQuery.newLongRange (
                                               CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                               0L, Long.MAX_VALUE, false, true );
        builder.add ( rankTerm, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, sort );
    }

    public CObjList getMyHasFiles ( String communityID, String fileDigest, String fragDigest )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), communityID );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wdigterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( wdigterm ), BooleanClause.Occur.MUST );

        Term pdigterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( pdigterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), CObj.TRUE );
        builder.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        Term myterm = new Term ( CObj.docPrivate ( CObj.MINE ), CObj.TRUE );
        builder.add ( new TermQuery ( myterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getAllMyHasFiles ( )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), "true" );
        builder.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        Term myterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( myterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getAllMyDuplicates ( )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.DUPFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getAllHasFiles ( )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getMyHasFiles ( String wdig, String pdig )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term wdigterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), wdig );
        builder.add ( new TermQuery ( wdigterm ), BooleanClause.Occur.MUST );

        Term pdigterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), pdig );
        builder.add ( new TermQuery ( pdigterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), "true" );
        builder.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        Term myterm = new Term ( CObj.docPrivate ( CObj.MINE ), CObj.TRUE );
        builder.add ( new TermQuery ( myterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getLocalHasFiles ( String comid, String memid, String localfile )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term lfterm = new Term ( CObj.docPrivate ( CObj.LOCALFILE ), localfile );
        builder.add ( new TermQuery ( lfterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), CObj.TRUE );
        builder.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getDuplicate ( String comid, String memid, String localfile )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.DUPFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term memterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        builder.add ( new TermQuery ( memterm ), BooleanClause.Occur.MUST );

        Term lfterm = new Term ( CObj.docString ( CObj.LOCALFILE ), localfile );
        builder.add ( new TermQuery ( lfterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObj getDuplicate ( String refid, String localfile )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.DUPFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.HASFILE ), refid );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term lfterm = new Term ( CObj.docString ( CObj.LOCALFILE ), localfile );
        builder.add ( new TermQuery ( lfterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList cl = search ( builder.build(), Integer.MAX_VALUE );

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

    public CObj getIdentHasFile ( String communityID, String memberID, String fileDigest, String fragDigest )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term comterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), communityID );
        builder.add ( new TermQuery ( comterm ), BooleanClause.Occur.MUST );

        Term wdigterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( wdigterm ), BooleanClause.Occur.MUST );

        Term pdigterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( pdigterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docString ( CObj.CREATOR ), memberID );
        builder.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), CObj.TRUE );
        builder.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList cl = search ( builder.build(), Integer.MAX_VALUE );

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

    // HasPart
    /*  public CObjList getIdentityHasFiles ( String memberID, String fileDigest, String fragDigest )
        {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASFILE );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docString ( CObj.CREATOR ), memberID );
        builder.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term wdigterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( wdigterm ), BooleanClause.Occur.MUST );

        Term pdigterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( pdigterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
        }*/

    // HasPart
    // Retrieve all part file sources for a defined file from the index.
    public CObjList getPartFiles ( String fileDigest, String fragDigest )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.HASPART );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term wdigterm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( wdigterm ), BooleanClause.Occur.MUST );

        Term pdigterm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( pdigterm ), BooleanClause.Occur.MUST );

        Term shterm = new Term ( CObj.docString ( CObj.STILLHASFILE ), CObj.TRUE );
        builder.add ( new TermQuery ( shterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> rankTerm = NumericRangeQuery.newLongRange (
                                               CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                               0L, Long.MAX_VALUE, false, true );
        builder.add ( rankTerm, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, null );
    }

    // HasPart
    public CObjList getPartFiles ( String memberID, long firstSeqNumber, long lastSeqNumber )
    {
        int maxValues = ( int ) ( lastSeqNumber - firstSeqNumber );

        if ( maxValues <= 0 || maxValues > 1000 )
        {
            maxValues = 1000;
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.HASPART );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        Term memberTerm = new Term ( CObj.docString ( CObj.CREATOR ), memberID );
        builder.add ( new TermQuery ( memberTerm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> rankTerm = NumericRangeQuery.newLongRange (
                                               CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                               0L, Long.MAX_VALUE, false, true );
        builder.add ( rankTerm, BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> seqTerm = NumericRangeQuery.newLongRange (
                                              CObj.docNumber ( CObj.SEQNUM ),
                                              firstSeqNumber, Long.MAX_VALUE, true, true );
        builder.add ( seqTerm, BooleanClause.Occur.MUST );

        return search ( builder.build(), maxValues );
    }

    // HasPart
    // Retrieve the part file source information for a defined file and a defined creator.
    public CObj getPartFile ( String creatorID, String communityID, String fileDigest, String fragDigest )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.HASPART );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        Term creatorTerm = new Term ( CObj.docString ( CObj.CREATOR ), creatorID );
        builder.add ( new TermQuery ( creatorTerm ), BooleanClause.Occur.MUST );

        Term communityTerm = new Term ( CObj.docString ( CObj.COMMUNITYID ), communityID );
        builder.add ( new TermQuery ( communityTerm ), BooleanClause.Occur.MUST );

        Term fileDigestTerm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( fileDigestTerm ), BooleanClause.Occur.MUST );

        Term fragDigestTerm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( fragDigestTerm ), BooleanClause.Occur.MUST );

        Term stillHasTerm = new Term ( CObj.docString ( CObj.STILLHASFILE ), CObj.TRUE );
        builder.add ( new TermQuery ( stillHasTerm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList cl = search ( builder.build(), Integer.MAX_VALUE, null );

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

    // HasPart
    // Retrieve the part file sources for a defined file and a defined creator.
    public CObjList getPartFiles ( String creatorID, String fileDigest, String fragDigest )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.HASPART );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        Term creatorTerm = new Term ( CObj.docString ( CObj.CREATOR ), creatorID );
        builder.add ( new TermQuery ( creatorTerm ), BooleanClause.Occur.MUST );

        Term fileDigestTerm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( fileDigestTerm ), BooleanClause.Occur.MUST );

        Term fragDigestTerm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( fragDigestTerm ), BooleanClause.Occur.MUST );

        Term stillHasTerm = new Term ( CObj.docString ( CObj.STILLHASFILE ), CObj.TRUE );
        builder.add ( new TermQuery ( stillHasTerm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, null );
    }

    // HasPart
    public CObjList getAllPartFiles ( )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.HASPART );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    // HasPart
    // Retrieve all own part file sources for a defined file from the index.
    public CObjList getAllMyPartFiles ( String fileDigest, String fragDigest )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typeTerm = new Term ( CObj.PARAM_TYPE, CObj.HASPART );
        builder.add ( new TermQuery ( typeTerm ), BooleanClause.Occur.MUST );

        Term fileDigestTerm = new Term ( CObj.docString ( CObj.FILEDIGEST ), fileDigest );
        builder.add ( new TermQuery ( fileDigestTerm ), BooleanClause.Occur.MUST );

        Term fragDigestTerm = new Term ( CObj.docString ( CObj.FRAGDIGEST ), fragDigest );
        builder.add ( new TermQuery ( fragDigestTerm ), BooleanClause.Occur.MUST );

        Term mineTerm = new Term ( CObj.docPrivate ( CObj.MINE ), CObj.TRUE );
        builder.add ( new TermQuery ( mineTerm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE, null );
    }

    public CObj getIdentity ( String id )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term idterm = new Term ( CObj.PARAM_ID, id );
        builder.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( builder.build(), 1 );

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
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term idterm = new Term ( CObj.PARAM_ID, id );
        builder.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( builder.build(), 1 );

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
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.IDENTITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term mustterm = new Term ( CObj.docPrivate ( CObj.MINE ), "true" );
        builder.add ( new TermQuery ( mustterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObj getCommunity ( String comid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.COMMUNITY );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term idterm = new Term ( CObj.PARAM_DIG, comid );
        builder.add ( new TermQuery ( idterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( builder.build(), 1 );

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
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term nondec = new Term ( CObj.docPrivate ( CObj.DECODED ), "false" );
        builder.add ( new TermQuery ( nondec ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nq = NumericRangeQuery.newLongRange (
                                         CObj.docPrivateNumber ( CObj.LASTUPDATE ),
                                         lastdec, Long.MAX_VALUE, true, true );
        builder.add ( nq, BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getMembership ( String comid, String memid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docPrivate ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term midterm = new Term ( CObj.docPrivate ( CObj.MEMBERID ), memid );
        builder.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "true" );
        builder.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getInvalidMembership ( String comid, String memid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docPrivate ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term midterm = new Term ( CObj.docPrivate ( CObj.MEMBERID ), memid );
        builder.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "false" );
        builder.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        Term decterm = new Term ( CObj.docPrivate ( CObj.DECODED ), "true" );
        builder.add ( new TermQuery ( decterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObjList getPushesToSend()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();

        //For a BooleanQuery with no MUST clauses one or more SHOULD clauses
        //must match a document for the BooleanQuery to match.
        Term decterm = new Term ( CObj.docPrivate ( CObj.PRV_PUSH_REQ ), "true" );
        builder.add ( new TermQuery ( decterm ), BooleanClause.Occur.SHOULD );

        Term nocterm = new Term ( CObj.docPrivate ( CObj.PRV_PUSH_REQ ), "nocon" );
        builder.add ( new TermQuery ( nocterm ), BooleanClause.Occur.SHOULD );

        Sort s = new Sort();
        s.setSort ( new SortedNumericSortField ( CObj.docPrivateNumber ( CObj.PRV_PUSH_TIME ), SortField.Type.LONG, false ) );

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList getPushesToConnect()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();

        Term decterm = new Term ( CObj.docPrivate ( CObj.PRV_PUSH_REQ ), "true" );
        builder.add ( new TermQuery ( decterm ), BooleanClause.Occur.MUST );

        Sort s = new Sort();
        s.setSort ( new SortedNumericSortField ( CObj.docPrivateNumber ( CObj.PRV_PUSH_TIME ), SortField.Type.LONG, false ) );

        return search ( builder.build(), Integer.MAX_VALUE, s );
    }

    public CObjList getInvalidMemberships()
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.MEMBERSHIP );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term validterm = new Term ( CObj.docPrivate ( CObj.VALIDMEMBER ), "false" );
        builder.add ( new TermQuery ( validterm ), BooleanClause.Occur.MUST );

        Term decterm = new Term ( CObj.docPrivate ( CObj.DECODED ), "true" );
        builder.add ( new TermQuery ( decterm ), BooleanClause.Occur.MUST );

        return search ( builder.build(), Integer.MAX_VALUE );
    }

    public CObj getSubscription ( String comid, String memid )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term midterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        builder.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        Term subterm = new Term ( CObj.docString ( CObj.SUBSCRIBED ), "true" );
        builder.add ( new TermQuery ( subterm ), BooleanClause.Occur.MUST );

        NumericRangeQuery<Long> nrq = NumericRangeQuery.newLongRange (
                                          CObj.docPrivateNumber ( CObj.PRV_USER_RANK ),
                                          0L, Long.MAX_VALUE, false, true );
        builder.add ( nrq, BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( builder.build(), 1 );

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
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term typterm = new Term ( CObj.PARAM_TYPE, CObj.SUBSCRIPTION );
        builder.add ( new TermQuery ( typterm ), BooleanClause.Occur.MUST );

        Term cidterm = new Term ( CObj.docString ( CObj.COMMUNITYID ), comid );
        builder.add ( new TermQuery ( cidterm ), BooleanClause.Occur.MUST );

        Term midterm = new Term ( CObj.docString ( CObj.CREATOR ), memid );
        builder.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( builder.build(), 1 );

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
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term midterm = new Term ( CObj.PARAM_DIG, dig );
        builder.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( builder.build(), 1 );

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

    public CObj getById ( String dig )
    {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        //BooleanQuery bq = new BooleanQuery();
        Term midterm = new Term ( CObj.PARAM_ID, dig );
        builder.add ( new TermQuery ( midterm ), BooleanClause.Occur.MUST );

        CObj r = null;
        CObjList l = search ( builder.build(), 1 );

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

    public void indexNoCommit ( IndexWriter idx, CObj o, boolean onlynew ) throws IOException
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
            idx.updateDocument ( updateterm, d );
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

    public void delete ( CObjList list ) throws IOException
    {
        for ( int i = 0; i < list.size(); i++ )
        {
            delete ( list.get ( i ) );
        }

    }

    public void index ( CObj o, boolean onlynew ) throws IOException
    {
        indexNoCommit ( writer, o, onlynew );
        writer.commit();
    }

    public void index ( List<CObj> l, boolean onlynew ) throws IOException
    {
        for ( CObj o : l )
        {
            indexNoCommit ( writer, o, onlynew );
        }

        writer.commit();
    }

    public void index ( CObj o ) throws IOException
    {
        indexNoCommit ( writer, o, false );
        writer.commit();
    }

    public void index ( List<CObj> l ) throws IOException
    {
        for ( CObj o : l )
        {
            indexNoCommit ( writer, o, false );
        }

        writer.commit();
    }

    @Override
    public void run()
    {
        // TODO Auto-generated method stub

    }

}
