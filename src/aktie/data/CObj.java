package aktie.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.json.JSONObject;

import aktie.crypto.Utils;

public class CObj
{

    //Types - indexed
    public static String IDENTITY = "identity";
    public static String COMMUNITY = "community";
    public static String TEMPLATE = "template";
    public static String MEMBERSHIP = "membership";
    public static String SUBSCRIPTION = "subscription";
    public static String POST = "post";
    public static String FILE = "filetype";
    public static String HASFILE = "hasfile";
    public static String FRAGMENT = "fragment";
    public static String FILEF = "filef";

    //Temporary types - requests to other nodes.
    public static String CON_CHALLENGE = "con_challenge";
    public static String CON_REPLY = "con_reply";
    public static String CON_REQ_IDENTITIES = "con_ident";
    public static String CON_REQ_COMMUNITIES = "con_com";
    public static String CON_REQ_MEMBERSHIPS = "con_mem";
    public static String CON_REQ_SUBS = "con_subs";
    public static String CON_REQ_POSTS = "con_posts";
    public static String CON_REQ_HASFILE = "con_has";
    public static String CON_REQ_FRAGLIST = "con_flist";
    public static String CON_REQ_FRAG = "con_frag";
    public static String CON_LIST = "lsttype";
    public static String CON_FILEMODE = "filemode";

    //Temporary types - user requests.
    public static String USR_DOWNLOAD_FILE = "usr_downloadfile";
    public static String USR_HASFILE_UPDATE = "usr_hasfileupdate";
    public static String USR_POST_UPDATE = "usr_postupdate";
    public static String USR_SUB_UPDATE = "usr_subupdate";
    public static String USR_IDENTITY_UPDATE = "usr_identupdate";
    public static String USR_COMMUNITY_UPDATE = "usr_comupdate";
    public static String USR_MEMBER_UPDATE = "usr_memupdate";
    public static String USR_SEED = "usr_seed";
    public static String USR_COMMUNITY = "usr_com";
    public static String USR_START_DEST = "start_dest";

    public static String PRV_LOCALFILE   = "prv_localfile";
    public static String PRV_NAME        = "prv_name";
    public static String PRV_FILESIZE    = "prv_filesize";
    public static String PRV_FRAGDIGEST  = "prv_fragdig";
    public static String PRV_FRAGSIZE    = "prv_fragsize";
    public static String PRV_FRAGNUMBER  = "prv_fragnum";
    public static String PRV_FILEDIGEST  = "prv_filedig";
    public static String PRV_DEST_OPEN   = "prv_dest_open";
    public static String PRV_CLEAR_ERR   = "prv_clear_err";
    public static String PRV_PUSH_REQ    = "prv_push_req";
    public static String PRV_PUSH_TIME   = "prv_push_time";
    public static String PRV_DISPLAY_NAME = "prv_display_name";

    public static String PRV_TEMP_NEWPOSTS = "newposts";

    public static String NAME = "name";
    public static String TXTNAME = "txtname";
    public static String DESCRIPTION = "desc";
    public static String ERROR = "error";
    public static String PRIVATEKEY = "privkey";
    public static String DEST = "dest";
    public static String KEY = "key";
    public static String PAYLOAD = "payload";
    public static String PAYLOAD2 = "payload2";
    public static String SCOPE = "scope";
    public static String CREATOR = "creator";
    public static String CREATOR_NAME = "creator_name";
    public static String COMMUNITY_NAME = "community_name";
    public static String CREATEDON = "createdon";
    public static String SEQNUM = "seqnum";
    public static String COMMUNITYID = "comid";
    public static String MEMBERID = "memid";
    public static String ENCKEY = "enckey";
    public static String SUBSCRIBED = "subscribed";
    public static String LOCALFILE = "localfile";
    public static String NUMBER_HAS  = "prv_numhas";
    public static String FRAGSIZE = "fragsize";
    public static String FILESIZE = "filesize";
    public static String SCOPE_PRIVATE = "private";
    public static String SCOPE_PUBLIC = "public";
    public static String NAME_IS_PUBLIC = "namepub";
    public static String FILEDIGEST = "fdig";
    public static String FRAGDIGEST = "gdig"; //digest of digests
    public static String FRAGDIG = "frgdig"; //digest of single fragment
    public static String FRAGNUMBER = "fragnum"; //The number of fragments
    public static String FRAGOFFSET = "foffset";
    public static String STILLHASFILE = "stillhas";
    public static String FIRSTNUM = "firstn";
    public static String LASTNUM = "lastn";
    public static String COMPLETE = "complete";
    public static String COUNT = "count";
    public static String DECODED = "decoded";
    public static String LASTUPDATE = "lastupdate";
    public static String PRIORITY = "priority";
    public static String SUBJECT = "subject";
    public static String BODY = "body";
    public static String FILENAME = "fname";
    public static String UPGRADEFLAG = "upgrade";
    public static String SHARE_NAME = "sharename";
    public static String STATUS = "fstatus";

    public static String PARAM_ID = "id";
    public static String PARAM_TYPE = "type";
    public static String PARAM_DIG = "dig";
    public static String PARAM_SIG = "sig";

    public static String MINE = "MINE";

    public static String AUTHORITY = "auth";
    public static String VALIDMEMBER = "validmember";
    public static long MEMBER_SIMPLE = 0;
    public static long MEMBER_CAN_GRANT = 1;
    public static long MEMBER_SUPER = 2;

    private String id;
    private String type;
    private Map<String, String> strings;
    private Map<String, String> text;
    private Map<String, Long> numbers;
    private Map<String, Double> decimals;
    private String dig;
    private String signature;

    private Map<String, String> privatedata;
    private Map<String, Long> privatenumbers;

    public void clear()
    {
        id = null;
        type = null;
        dig = null;
        strings = null;
        text = null;
        numbers = null;
        decimals = null;
        signature = null;
        privatedata = null;
        privatenumbers = null;
    }

    public CObj clone()
    {
        CObj c = new CObj();
        c.setType ( getType() );
        c.setId ( getId() );
        c.setDig ( getDig() );
        c.setSignature ( getSignature() );

        if ( strings != null )
        {
            for ( Entry<String, String> e : strings.entrySet() )
            {
                c.pushString ( e.getKey(), e.getValue() );
            }

        }

        if ( text != null )
        {
            for ( Entry<String, String> e : text.entrySet() )
            {
                c.pushText ( e.getKey(), e.getValue() );
            }

        }

        if ( privatedata != null )
        {
            for ( Entry<String, String> e : privatedata.entrySet() )
            {
                c.pushPrivate ( e.getKey(), e.getValue() );
            }

        }

        if ( numbers != null )
        {
            for ( Entry<String, Long> e : numbers.entrySet() )
            {
                c.pushNumber ( e.getKey(), e.getValue() );
            }

        }

        if ( decimals != null )
        {
            for ( Entry<String, Double> e : decimals.entrySet() )
            {
                c.pushDecimal ( e.getKey(), e.getValue() );
            }

        }

        if ( privatenumbers != null )
        {
            for ( Entry<String, Long> e : privatenumbers.entrySet() )
            {
                c.pushPrivateNumber ( e.getKey(), e.getValue() );
            }

        }

        return c;
    }

    public JSONObject getJSON()
    {
        JSONObject r = new JSONObject();

        if ( id != null )
        {
            r.put ( PARAM_ID, id );
        }

        if ( type != null )
        {
            r.put ( PARAM_TYPE, type );
        }

        if ( dig != null )
        {
            r.put ( PARAM_DIG, dig );
        }

        if ( signature != null )
        {
            r.put ( PARAM_SIG, signature );
        }

        if ( strings != null )
        {
            if ( strings.size() > 0 )
            {
                JSONObject so = new JSONObject();

                for ( Entry<String, String> e : strings.entrySet() )
                {
                    so.put ( e.getKey(), e.getValue() );
                }

                r.put ( "strings", so );
            }

        }

        if ( text != null )
        {
            if ( text.size() > 0 )
            {
                JSONObject so = new JSONObject();

                for ( Entry<String, String> e : text.entrySet() )
                {
                    so.put ( e.getKey(), e.getValue() );
                }

                r.put ( "text", so );
            }

        }

        if ( numbers != null )
        {
            if ( numbers.size() > 0 )
            {
                JSONObject so = new JSONObject();

                for ( Entry<String, Long> e : numbers.entrySet() )
                {
                    so.put ( e.getKey(), e.getValue() );
                }

                r.put ( "numbers", so );
            }

        }

        if ( decimals != null )
        {
            if ( decimals.size() > 0 )
            {
                JSONObject so = new JSONObject();

                for ( Entry<String, Double> e : decimals.entrySet() )
                {
                    so.put ( e.getKey(), e.getValue() );
                }

                r.put ( "decimals", so );
            }

        }

        return r;
    }

    public void loadJSON ( JSONObject jo )
    {
        clear();

        if ( jo.has ( PARAM_ID ) )
        {
            id = jo.getString ( PARAM_ID );
        }

        if ( jo.has ( PARAM_TYPE ) )
        {
            type = jo.getString ( PARAM_TYPE );
        }

        if ( jo.has ( PARAM_DIG ) )
        {
            dig = jo.getString ( PARAM_DIG );
        }

        if ( jo.has ( PARAM_SIG ) )
        {
            signature = jo.getString ( PARAM_SIG );
        }

        if ( jo.has ( "strings" ) )
        {
            JSONObject so = jo.getJSONObject ( "strings" );

            if ( so.length() > 0 )
            {
                strings = new HashMap<String, String>();
                Iterator<String> i = so.keys();

                while ( i.hasNext() )
                {
                    String k = i.next();
                    String v = so.getString ( k );
                    strings.put ( k, v );
                }

            }

        }

        if ( jo.has ( "text" ) )
        {
            JSONObject so = jo.getJSONObject ( "text" );

            if ( so.length() > 0 )
            {
                text = new HashMap<String, String>();
                Iterator<String> i = so.keys();

                while ( i.hasNext() )
                {
                    String k = i.next();
                    String v = so.getString ( k );
                    text.put ( k, v );
                }

            }

        }

        if ( jo.has ( "numbers" ) )
        {
            JSONObject so = jo.getJSONObject ( "numbers" );

            if ( so.length() > 0 )
            {
                numbers = new HashMap<String, Long>();
                Iterator<String> i = so.keys();

                while ( i.hasNext() )
                {
                    String k = i.next();
                    long v = so.getLong ( k );
                    numbers.put ( k, v );
                }

            }

        }

        if ( jo.has ( "decimals" ) )
        {
            JSONObject so = jo.getJSONObject ( "decimals" );

            if ( so.length() > 0 )
            {
                decimals = new HashMap<String, Double>();
                Iterator<String> i = so.keys();

                while ( i.hasNext() )
                {
                    String k = i.next();
                    double v = so.getDouble ( k );
                    decimals.put ( k, v );
                }

            }

        }

    }

    public Document getDocument()
    {
        Document d = new Document();

        if ( id != null )
        {
            d.add ( new StringField ( PARAM_ID, id, Store.YES ) );
        }

        if ( type != null )
        {
            d.add ( new StringField ( PARAM_TYPE, type, Store.YES ) );
        }

        if ( dig != null )
        {
            d.add ( new StringField ( PARAM_DIG, dig, Store.YES ) );
        }

        if ( signature != null )
        {
            d.add ( new StringField ( PARAM_SIG, signature, Store.YES ) );
        }

        if ( strings != null )
        {
            if ( strings.size() > 0 )
            {
                for ( Entry<String, String> e : strings.entrySet() )
                {
                    d.add ( new StringField ( docString ( e.getKey() ), e.getValue(), Store.YES ) );
                    d.add ( new SortedDocValuesField ( docString ( e.getKey() ), new BytesRef ( e.getValue() ) ) );
                    d.add ( new TextField ( docStringText ( e.getKey() ), e.getValue(), Store.NO ) );
                }

            }

        }

        if ( text != null )
        {
            if ( text.size() > 0 )
            {
                for ( Entry<String, String> e : text.entrySet() )
                {
                    d.add ( new TextField ( docText ( e.getKey() ), e.getValue(), Store.YES ) );
                }

            }

        }

        if ( numbers != null )
        {
            if ( numbers.size() > 0 )
            {
                for ( Entry<String, Long> e : numbers.entrySet() )
                {
                    d.add ( new LongField ( docNumber ( e.getKey() ), e.getValue(), Store.YES ) );
                    d.add ( new SortedNumericDocValuesField ( docNumber ( e.getKey() ), e.getValue() ) );
                }

            }

        }

        if ( decimals != null )
        {
            if ( decimals.size() > 0 )
            {
                for ( Entry<String, Double> e : decimals.entrySet() )
                {
                    d.add ( new DoubleField ( docDecimal ( e.getKey() ), e.getValue(), Store.YES ) );
                    d.add ( new SortedNumericDocValuesField ( docDecimal ( e.getKey() ), NumericUtils.doubleToSortableLong ( e.getValue() ) ) );
                }

            }

        }

        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // DO NOT SAVE/RESTORE privatedata in JSON!!  Document ONLY!!!
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        String dispname = getDisplayName();

        if ( dispname == null )
        {
            dispname = getPrivateDisplayName();
        }

        if ( dispname != null )
        {
            //d.add ( new StringField ( docPrivate ( PRV_DISPLAY_NAME ), dispname, Store.NO ) );
            d.add ( new SortedDocValuesField ( docPrivate ( PRV_DISPLAY_NAME ), new BytesRef ( dispname ) ) );
            d.add ( new TextField ( docPrivateText ( PRV_DISPLAY_NAME ), dispname, Store.NO ) );
        }

        if ( privatedata != null )
        {
            if ( privatedata.size() > 0 )
            {
                for ( Entry<String, String> e : privatedata.entrySet() )
                {
                    d.add ( new StringField ( docPrivate ( e.getKey() ), e.getValue(), Store.YES ) );
                    d.add ( new SortedDocValuesField ( docPrivate ( e.getKey() ), new BytesRef ( e.getValue() ) ) );
                    d.add ( new TextField ( docPrivateText ( e.getKey() ), e.getValue(), Store.NO ) );
                }

            }

        }

        if ( privatenumbers != null )
        {
            if ( privatenumbers.size() > 0 )
            {
                for ( Entry<String, Long> e : privatenumbers.entrySet() )
                {
                    d.add ( new LongField ( docPrivateNumber ( e.getKey() ), e.getValue(), Store.YES ) );
                    d.add ( new SortedNumericDocValuesField ( docPrivateNumber ( e.getKey() ), e.getValue() ) );
                }

            }

        }

        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // DO NOT SAVE/RESTORE privatedata in JSON!!  Document ONLY!!!
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        return d;
    }

    public static String docPrivate ( String t )
    {
        return "PRIVATE_" + t;
    }

    public static String docPrivateText ( String t )
    {
        return "PRIVATE_TXT_" + t;
    }

    public static String docPrivateNumber ( String t )
    {
        return "PRIVNUM_" + t;
    }

    public static String docDecimal ( String t )
    {
        return "decimals_" + t;
    }

    public static String docNumber ( String t )
    {
        return "numbers_" + t;
    }

    public static String docText ( String t )
    {
        return "text_" + t;
    }

    public static String docString ( String t )
    {
        return "strings_" + t;
    }

    public static String docStringText ( String t )
    {
        return "str_txt" + t;
    }

    public void loadDocument ( Document d )
    {
        clear();
        List<IndexableField> l = d.getFields();

        for ( IndexableField i : l )
        {
            String k = i.name();

            if ( PARAM_ID.equals ( k ) )
            {
                id = i.stringValue();
            }

            if ( PARAM_TYPE.equals ( k ) )
            {
                type = i.stringValue();
            }

            if ( PARAM_DIG.equals ( k ) )
            {
                dig = i.stringValue();
            }

            if ( PARAM_SIG.equals ( k ) )
            {
                signature = i.stringValue();
            }

            if ( k.startsWith ( "strings_" ) )
            {
                if ( strings == null )
                {
                    strings = new HashMap<String, String>();
                }

                String nk = k.substring ( "strings_".length() );
                strings.put ( nk, i.stringValue() );
            }

            if ( k.startsWith ( "text_" ) )
            {
                if ( text == null )
                {
                    text = new HashMap<String, String>();
                }

                String nk = k.substring ( "text_".length() );
                text.put ( nk, i.stringValue() );
            }

            if ( k.startsWith ( "numbers_" ) )
            {
                if ( numbers == null )
                {
                    numbers = new HashMap<String, Long>();
                }

                String nk = k.substring ( "numbers_".length() );
                numbers.put ( nk, i.numericValue().longValue() );
            }

            if ( k.startsWith ( "decimals_" ) )
            {
                if ( decimals == null )
                {
                    decimals = new HashMap<String, Double>();
                }

                String nk = k.substring ( "decimals_".length() );
                decimals.put ( nk, i.numericValue().doubleValue() );
            }

            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // DO NOT SAVE/RESTORE privatedata in JSON!!  Document ONLY!!!
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if ( k.startsWith ( "PRIVATE_" ) )
            {
                if ( privatedata == null )
                {
                    privatedata = new HashMap<String, String>();
                }

                String nk = k.substring ( "PRIVATE_".length() );
                privatedata.put ( nk, i.stringValue() );
            }

            if ( k.startsWith ( "PRIVNUM_" ) )
            {
                if ( privatenumbers == null )
                {
                    privatenumbers = new HashMap<String, Long>();
                }

                String nk = k.substring ( "PRIVNUM_".length() );
                privatenumbers.put ( nk, i.numericValue().longValue() );
            }

            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // DO NOT SAVE/RESTORE privatedata in JSON!!  Document ONLY!!!
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        }

    }

    public void sign ( RSAPrivateCrtKeyParameters key )
    {
        byte dg[] = digest();
        dig = Utils.toString ( dg );
        RSAEngine eng = new RSAEngine();
        PKCS1Encoding enc = new PKCS1Encoding ( eng );
        enc.init ( true, key );

        try
        {
            signature = Utils.toString ( enc.processBlock ( dg, 0, dg.length ) );
        }

        catch ( InvalidCipherTextException e )
        {
            e.printStackTrace();
            throw new RuntimeException ( e.getMessage() );
        }

    }

    public void simpleDigest()
    {
        byte dg[] = digest();
        dig = Utils.toString ( dg );
    }

    public boolean checkSignature ( RSAKeyParameters key )
    {
        byte td[] = digest();

        if ( !Arrays.equals ( td, Utils.toByteArray ( dig ) ) ) { return false; }

        RSAEngine eng = new RSAEngine();
        PKCS1Encoding enc = new PKCS1Encoding ( eng );
        enc.init ( false, key );

        try
        {
            byte sb[] = Utils.toByteArray ( signature );
            byte decsig[] = enc.processBlock ( sb, 0, sb.length );
            return Arrays.equals ( decsig, td );
        }

        catch ( Exception e )
        {
        }

        return false;
    }

    private byte[] digest()
    {
        //Digest key-value pairs in an order independent way
        byte d[] = null;

        if ( id != null )
        {
            d = Utils.digString ( d, id );
        }

        if ( type != null )
        {
            d = Utils.digString ( d, type );
        }

        if ( strings != null )
        {
            d = Utils.digStringMap ( d, strings );
        }

        if ( text != null )
        {
            d = Utils.digStringMap ( d, text );
        }

        if ( numbers != null )
        {
            d = Utils.digLongMap ( d, numbers );
        }

        if ( decimals != null )
        {
            d = Utils.digDoubleMap ( d, decimals );
        }

        return d;
    }

    public String getType()
    {
        return type;
    }

    public void setType ( String type )
    {
        this.type = type;
    }

    public String getDig()
    {
        return dig;
    }

    public void setDig ( String dig )
    {
        this.dig = dig;
    }

    public String getSignature()
    {
        return signature;
    }

    public void setSignature ( String signature )
    {
        this.signature = signature;
    }

    public void pushPrivate ( String key, String v )
    {
        if ( v != null )
        {
            if ( privatedata == null )
            {
                privatedata = new HashMap<String, String>();
            }

            privatedata.put ( key, v );
        }

    }

    public String getPrivate ( String key )
    {
        if ( privatedata == null ) { return null; }

        return privatedata.get ( key );
    }

    public void pushPrivateNumber ( String key, Long v )
    {
        if ( v != null )
        {
            if ( privatenumbers == null )
            {
                privatenumbers = new HashMap<String, Long>();
            }

            privatenumbers.put ( key, v );
        }

    }

    public Long getPrivateNumber ( String key )
    {
        if ( privatenumbers == null ) { return null; }

        return privatenumbers.get ( key );
    }

    public void pushString ( String key, String v )
    {
        if ( v != null )
        {
            if ( strings == null )
            {
                strings = new HashMap<String, String>();
            }

            strings.put ( key, v );
        }

    }

    public String getString ( String key )
    {
        if ( strings == null ) { return null; }

        return strings.get ( key );
    }

    public void pushText ( String key, String v )
    {
        if ( v != null )
        {
            if ( text == null )
            {
                text = new HashMap<String, String>();
            }

            text.put ( key, v );
        }

    }

    public String getText ( String key )
    {
        if ( text == null ) { return null; }

        return text.get ( key );
    }

    public void pushNumber ( String key, long v )
    {
        if ( numbers == null )
        {
            numbers = new HashMap<String, Long>();
        }

        numbers.put ( key, v );
    }

    public Long getNumber ( String key )
    {
        if ( numbers == null ) { return null; }

        return numbers.get ( key );
    }

    public void pushDecimal ( String key, double v )
    {
        if ( decimals == null )
        {
            decimals = new HashMap<String, Double>();
        }

        decimals.put ( key, v );
    }

    public Double getDecimal ( String key )
    {
        if ( decimals == null ) { return null; }

        return decimals.get ( key );
    }

    public Map<String, String> getStrings()
    {
        return strings;
    }

    public Map<String, String> getText()
    {
        return text;
    }

    public Map<String, Long> getNumbers()
    {
        return numbers;
    }

    public Map<String, Double> getDecimals()
    {
        return decimals;
    }

    public Map<String, String> getPrivatedata()
    {
        return privatedata;
    }

    public Map<String, Long> getPrivateNumbers()
    {
        return privatenumbers;
    }

    public String getId()
    {
        return id;
    }

    public void setId ( String id )
    {
        this.id = id;
    }

    public String getDisplayName()
    {
        String name = getString ( CObj.NAME );

        if ( name != null )
        {
            if ( getId() != null )
            {
                name = name + " <" + getId().substring ( 0, 6 ) + ">";
            }

            else
            {
                name = name + " <" + getDig().substring ( 0, 6 ) + ">";
            }

        }

        return name;
    }

    public String getPrivateDisplayName()
    {
        String name = getPrivate ( CObj.NAME );

        if ( name != null )
        {
            if ( getId() != null )
            {
                name = name + " <" + getId().substring ( 0, 6 ) + ">";
            }

            else
            {
                name = name + " <" + getDig().substring ( 0, 6 ) + ">";
            }

        }

        return name;
    }

    @Override
    public int hashCode()
    {
        if ( id != null )
        {
            return id.hashCode();
        }

        if ( dig != null )
        {
            return dig.hashCode();
        }

        return 1;
    }

    private boolean strEq ( String x, String y )
    {
        if ( x != null ) { return x.equals ( y ); }

        else { return y == null; }

    }

    public boolean mapEq ( Map<?, ?> m, Map<?, ?> n )
    {
        if ( m == null && n == null ) { return true; }

        if ( m == null || n == null ) { return false; }

        if ( m.size() != n.size() ) { return false; }

        Set<?> k0 = m.keySet();
        Set<?> k1 = n.keySet();

        if ( !k0.containsAll ( k1 ) ) { return false; }

        if ( !k1.containsAll ( k0 ) ) { return false; }

        for ( Object k : k0 )
        {
            Object o0 = m.get ( k );
            Object o1 = n.get ( k );

            if ( o0 instanceof byte[] )
            {
                if ( !Arrays.equals ( ( byte[] ) o0, ( byte[] ) o1 ) ) { return false; }

            }

            else
            {
                if ( !m.get ( k ).equals ( n.get ( k ) ) ) { return false; }

            }

        }

        return true;
    }

    public boolean whoopyEquals ( Object o )
    {
        if ( ! ( o instanceof CObj ) ) { return false; }

        CObj b = ( CObj ) o;

        if ( !strEq ( id, b.getId() ) ) { return false; }

        if ( !strEq ( dig, b.getDig() ) ) { return false; }

        if ( !strEq ( type, b.getType() ) ) { return false; }

        if ( !strEq ( signature, b.getSignature() ) ) { return false; }

        if ( !mapEq ( decimals, b.getDecimals() ) ) { return false; }

        if ( !mapEq ( numbers, b.getNumbers() ) ) { return false; }

        if ( !mapEq ( strings, b.getStrings() ) ) { return false; }

        if ( !mapEq ( text, b.getText() ) ) { return false; }

        if ( !mapEq ( privatedata, b.getPrivatedata() ) ) { return false; }

        if ( !mapEq ( privatenumbers, b.getPrivateNumbers() ) ) { return false; }

        return true;
    }


    private boolean productionEquals ( Object o )
    {
        if ( ! ( o instanceof CObj ) ) { return false; }

        CObj b = ( CObj ) o;

        if ( id != null )
        {
            return id.equals ( b.getId() );
        }

        if ( dig != null )
        {
            return dig.equals ( b.getDig() );
        }

        return false;
    }

    @Override
    public boolean equals ( Object o )
    {
        return productionEquals ( o );
        //return whoopyEquals(o);
    }

}
