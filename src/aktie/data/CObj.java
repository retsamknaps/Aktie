package aktie.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
import org.apache.lucene.index.IndexableFieldType;
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

    public static int MAXSTRING = 50000;

    //Types - indexed
    public static String IDENTITY = "identity";
    public static String COMMUNITY = "community";
    public static String MEMBERSHIP = "membership";
    public static String SUBSCRIPTION = "subscription";
    public static String INV_SUBSCRIPTION = "invalidsub";
    public static String POST = "post";
    public static String FILE = "filetype";
    public static String HASFILE = "hasfile";
    public static String FRAGMENT = "fragment";
    public static String FILEF = "filef";
    public static String FRAGFAILED = "oops";
    public static String PRIVIDENTIFIER = "privident";
    public static String PRIVMESSAGE = "privmsg";
    public static String QUERY = "query";
    public static String FIELD = "field";
    public static String DUPFILE = "dupfile";
    public static String SPAMEXCEPTION = "spamex";
    public static String OBJDIG = "d";
    public static String SEQCOMP = "seqcomp";
    public static String CHECKMEM = "chkmem";
    public static String CHECKSUB = "chksub";
    public static String CHECKCOMP = "chkcomp";

    //Types - no indexed.
    public static String INDEX_QUERY = "index_query";
    public static String NODE_CMD = "node_cmd";

    //Node command types
    public static String NODE_CMD_TYPE = "node_cmd_type";
    public static String NODE_CMD_SHUTDOWN = "node_cmd_stop";

    //Index query types
    public static String INDEX_Q_TYPE = "index_q_type";
    public static String INDEX_Q_PUBCOM = "index_q_pubcom";
    public static String INDEX_Q_SUBS = "index_q_subs";
    public static String INDEX_Q_MEMS = "index_q_mems";
    public static String INDEX_Q_IDENT = "index_q_ident";

    //Temporary types - requests to other nodes.
    public static String CON_CHALLENGE = "con_challenge";
    public static String CON_REPLY = "con_reply";
    public static String CON_REQ_IDENTITIES = "con_ident";
    public static String CON_REQ_PRVIDENT = "con_prv_ident";
    public static String CON_REQ_PRVMSG = "con_prv_msg";
    public static String CON_REQ_SPAMEX = "con_spamex";
    public static String CON_REQ_COMMUNITIES = "con_com";
    public static String CON_REQ_MEMBERSHIPS = "con_mem";
    public static String CON_REQ_SUBS = "con_subs";
    public static String CON_REQ_POSTS = "con_posts";
    public static String CON_REQ_HASFILE = "con_has";
    public static String CON_REQ_FRAGLIST = "con_flist";
    public static String CON_REQ_FRAG = "con_frag";
    public static String CON_REQ_GLOBAL = "con_glb";
    public static String CON_REQ_DIG = "con_dig";
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
    public static String USR_PRVMSG_UPDATE = "usr_prvmsgupdate";
    public static String USR_SPAMEX_UPDATE = "usr_spamexupdate";
    public static String USR_SEED = "usr_seed";
    public static String USR_SPAMEX = "usr_spamex";
    public static String USR_COMMUNITY = "usr_com";
    public static String USR_START_DEST = "start_dest";
    public static String USR_SET_RANK = "set_rank";
    public static String USR_SHARE_MGR = "share_mgr";
    public static String USR_CANCEL_DL = "cancel_dl";
    public static String USR_FORCE_SEARCHER = "usr_force_searcher";

    //Private fields
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
    public static String PRV_USER_RANK   = "prv_user_rank";
    public static String PRV_DEF_FIELD   = "prv_def_field";
    public static String PRV_FLD_NEW     = "prv_fld_new";
    public static String PRV_QRY_AUTODOWNLOAD = "prv_auto_dl";
    public static String PRV_RECIPIENT   = "prv_to";
    public static String PRV_MSG_ID      = "prv_msg_id";
    public static String PRV_SKIP_PAYMENT = "prv_skip_payment";
    public static String PRV_GLOBAL_SEQ  = "prv_glbseq";

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
    public static String MEMSEQNUM = "mseqnum";
    public static String SUBSEQNUM = "sseqnum";
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
    public static String ENABLED = "enabled";
    public static String MSGIDENT = "msgid";
    public static String PAYMENT = "hashpayment";
    public static String BLOGMODE = "blogmode";

    //Field prefixes
    //Fields are added to posts.  They are specific to communities.
    //Any user can define fields for a community.
    //A special post defining a field or fields is created.
    //The field definition post contains these fields:
    //
    //  fld_id_<subid>      :  full string id of the field
    //  fld_type_<subid>    :  the field type
    //  fld_name_<subid>    :  short name of the field shown in tables
    //  fld_desc_<subid>    :  longer description of the field
    //  fld_val_<subid>_<x> :  allowable value x for type opt
    //  fld_def_<subid>     :  default value for field
    //  fld_max_<subid>     :  maximum value for number or decimal types
    //  fld_min_<subid>     :  minimum value for number or decimal types
    //
    // type is either: string, text, number, decimal, bool, opt
    //   string type is not parsed.  String must match exactly in query
    //   text type is parsed, and wild card queries can be used
    //   number is an integer
    //   decimal is a floating point number
    //   bool is either true or false string value
    //   opt can only have one value of specified _val_ strings
    //
    // When a field definition is encountered in a post, a field CObj is
    // created.  It is associated with the community and the creating user.
    // so fields can be searched by creator or by creator rank.
    //
    // In posts a field is added by doing;
    //    fld_id_<subid> : <full field id>
    //    fld_<subid> : <value>
    //
    // So information about the field can only be shown if the post defining the field
    // has been received.
    //
    //

    public static int SUBID_LEN = 12;
    public static String FLD = "fld_";
    public static String FLD_ID = FLD + "id_";
    public static String FLD_TYPE = FLD + "type_";
    public static String FLD_NAME = FLD + "name_";
    public static String FLD_DESC = FLD + "desc_";
    public static String FLD_VAL = FLD + "val_";
    public static String FLD_DEF = FLD + "def_";
    public static String FLD_MAX = FLD + "max_";
    public static String FLD_MIN = FLD + "min_";

    //string, text, number, decimal, bool, opt
    public static String FLD_TYPE_STRING = "string";
    public static String FLD_TYPE_TEXT = "text";
    public static String FLD_TYPE_NUMBER = "number";
    public static String FLD_TYPE_DECIMAL = "decimal";
    public static String FLD_TYPE_BOOL = "bool";
    public static String FLD_TYPE_OPT = "opt";

    public static String QRY_MIN_USER_RANK = "q_min_ur";
    public static String QRY_MAX_USER_RANK = "q_max_ur";
    public static String QRY_MIN_FILE_SIZE = "q_min_fs";
    public static String QRY_MAX_FILE_SIZE = "q_max_fs";
    public static String QRY_MIN_DATE = "q_min_dt";
    public static String QRY_MAX_DATE = "q_max_dt";
    public static String QRY_DAYS_BACK = "q_days_old";

    public static String getGlobalSeq ( String id )
    {
        return CObj.PRV_GLOBAL_SEQ + id;
    }

    public static String getSubid ( String id )
    {
        if ( id != null )
        {
            return id.substring ( 0, Math.min ( SUBID_LEN, id.length() ) );
        }

        return "";
    }

    public List<CObj> listNewFields()
    {
        List<CObj> r = new LinkedList<CObj>();
        Set<String> sublst = new HashSet<String>();
        Map<String, String> fidmap = new HashMap<String, String>();

        if ( strings != null )
        {
            for ( Entry<String, String> e : strings.entrySet() )
            {
                String k = e.getKey();

                if ( k.startsWith ( FLD_TYPE ) )
                {
                    String sbi = k.substring ( FLD_TYPE.length() );
                    sublst.add ( sbi );
                    String fid = getString ( CObj.FLD_ID + sbi );

                    if ( fid != null )
                    {
                        fidmap.put ( sbi, fid );
                    }

                }

            }

            for ( String sid : sublst )
            {
                CObj no = new CObj();
                no.setType ( CObj.FIELD );

                for ( Entry<String, String> e : strings.entrySet() )
                {
                    String k = e.getKey();
                    String v = e.getValue();
                    int sidx = k.indexOf ( sid );

                    //Do not save fld_id
                    if ( sidx > 0 && !k.startsWith ( FLD_ID ) )
                    {
                        String fldk = k.substring ( 0, sidx );

                        if ( sidx + sid.length() < k.length() )
                        {
                            fldk = fldk + k.substring ( sidx + sid.length() );
                        }

                        if ( !FLD.equals ( fldk ) ) //Do not save value
                        {
                            no.pushString ( fldk, v );
                        }

                    }

                }

                if ( text != null )
                {
                    for ( Entry<String, String> e : text.entrySet() )
                    {
                        String k = e.getKey();
                        String v = e.getValue();
                        int sidx = k.indexOf ( sid );

                        //Do not save fld_id
                        if ( sidx > 0 && !k.startsWith ( FLD_ID ) )
                        {
                            String fldk = k.substring ( 0, sidx );

                            if ( !FLD.equals ( fldk ) ) //Do not save value
                            {
                                no.pushText ( fldk, v );
                            }

                        }

                    }

                }

                if ( numbers != null )
                {
                    for ( Entry<String, Long> e : numbers.entrySet() )
                    {
                        String k = e.getKey();
                        Long v = e.getValue();
                        int sidx = k.indexOf ( sid );

                        //Do not save fld_id
                        if ( sidx > 0 && !k.startsWith ( FLD_ID ) )
                        {
                            String fldk = k.substring ( 0, sidx );

                            if ( !FLD.equals ( fldk ) ) //Do not save value
                            {
                                no.pushNumber ( fldk, v );
                            }

                        }

                    }

                }

                if ( decimals != null )
                {
                    for ( Entry<String, Double> e : decimals.entrySet() )
                    {
                        String k = e.getKey();
                        Double v = e.getValue();
                        int sidx = k.indexOf ( sid );

                        //Do not save fld_id
                        if ( sidx > 0 && !k.startsWith ( FLD_ID ) )
                        {
                            String fldk = k.substring ( 0, sidx );

                            if ( !FLD.equals ( fldk ) ) //Do not save value
                            {
                                no.pushDecimal ( fldk, v );
                            }

                        }

                    }

                }

                no.pushString ( COMMUNITYID, getString ( COMMUNITYID ) );
                //Creator is private so that we allow two different
                //people define the same field and don't list them twice
                no.pushPrivate ( CREATOR, getString ( CREATOR ) );
                no.pushPrivateNumber ( PRV_USER_RANK, getPrivateNumber ( PRV_USER_RANK ) );
                String fid = fidmap.get ( sid );

                if ( fid != null )
                {
                    no.setDig ( fid );
                    r.add ( no );
                }

            }

        }

        return r;
    }

    private String setNewFields ( CObj fo )
    {
        String subid = getSubid ( fo.getDig() );

        Map<String, String> ov = fo.getStrings();

        if ( ov != null )
        {
            for ( Entry<String, String> e : ov.entrySet() )
            {
                String ky = e.getKey();
                String vl = e.getValue();

                if ( ky.startsWith ( FLD_VAL ) )
                {
                    String ev = ky.substring ( FLD_VAL.length() );
                    pushString ( FLD_VAL + subid + ev, vl );
                }

                else if ( ky.startsWith ( FLD ) )
                {
                    pushString ( ky + subid, vl );
                }

            }

        }

        Map<String, String> ot = fo.getText();

        if ( ot != null )
        {
            for ( Entry<String, String> e : ot.entrySet() )
            {
                String ky = e.getKey();
                String vl = e.getValue();

                if ( ky.startsWith ( FLD ) )
                {
                    pushText ( ky + subid, vl );
                }

            }

        }

        Map<String, Long> lv = fo.getNumbers();

        if ( lv != null )
        {
            for ( Entry<String, Long> e : lv.entrySet() )
            {
                String ky = e.getKey();
                Long vl = e.getValue();

                if ( ky.startsWith ( FLD ) )
                {
                    pushNumber ( ky + subid, vl );
                }

            }

        }

        Map<String, Double> dv = fo.getDecimals();

        if ( dv != null )
        {
            for ( Entry<String, Double> e : dv.entrySet() )
            {
                String ky = e.getKey();
                Double vl = e.getValue();

                if ( ky.startsWith ( FLD ) )
                {
                    pushDecimal ( ky + subid, vl );
                }

            }

        }

        return fo.getDig();
    }

    public void setNewFieldString ( CObj fo, String v )
    {
        String id = setNewFields ( fo );
        setFieldString ( id, v );
    }

    public void setNewFieldText ( CObj fo, String v )
    {
        String id = setNewFields ( fo );
        setFieldText ( id, v );
    }

    public void setNewFieldBool ( CObj fo, boolean v )
    {
        String id = setNewFields ( fo );
        setFieldBool ( id, v );
    }

    public void setNewFieldNumber ( CObj fo, long v )
    {
        String id = setNewFields ( fo );
        setFieldNumber ( id, v );
    }

    public void setNewFieldDecimal ( CObj fo, double v )
    {
        String id = setNewFields ( fo );
        setFieldDecimal ( id, v );
    }

    public void setFieldString ( String id, String value )
    {
        String subid = getSubid ( id );
        pushString ( FLD_ID + subid, id );
        pushString ( FLD + subid, value );
    }

    public void setFieldText ( String id, String value )
    {
        String subid = getSubid ( id );
        pushString ( FLD_ID + subid, id );
        pushText ( FLD + subid, value );
    }

    public void setFieldBool ( String id, boolean value )
    {
        String subid = getSubid ( id );
        pushString ( FLD_ID + subid, id );
        pushString ( FLD + subid, Boolean.toString ( value ) );
    }

    public void setFieldNumber ( String id, long value )
    {
        String subid = getSubid ( id );
        pushString ( FLD_ID + subid, id );
        pushNumber ( FLD + subid, value );
    }

    public void setFieldNumberMax ( String id, long value )
    {
        String subid = getSubid ( id );
        pushString ( FLD_ID + subid, id );
        pushNumber ( FLD_MAX + subid, value );
    }

    public void setFieldNumberMin ( String id, long value )
    {
        String subid = getSubid ( id );
        pushString ( FLD_ID + subid, id );
        pushNumber ( FLD_MIN + subid, value );
    }

    public void setFieldDecimal ( String id, double value )
    {
        String subid = getSubid ( id );
        pushString ( FLD_ID + subid, id );
        pushDecimal ( FLD + subid, value );
    }

    public void setFieldDecimalMax ( String id, double value )
    {
        String subid = getSubid ( id );
        pushString ( FLD_ID + subid, id );
        pushDecimal ( FLD_MAX + subid, value );
    }

    public void setFieldDecimalMin ( String id, double value )
    {
        String subid = getSubid ( id );
        pushString ( FLD_ID + subid, id );
        pushDecimal ( FLD_MIN + subid, value );
    }

    public Long getFieldNumberMax ( String id )
    {
        String kv = FLD_MAX + getSubid ( id );
        return getNumber ( kv );
    }

    public Long getFieldNumberMin ( String id )
    {
        String kv = FLD_MIN + getSubid ( id );
        return getNumber ( kv );
    }

    public Double getFieldDecimalMax ( String id )
    {
        String kv = FLD_MAX + getSubid ( id );
        return getDecimal ( kv );
    }

    public Double getFieldDecimalMin ( String id )
    {
        String kv = FLD_MIN + getSubid ( id );
        return getDecimal ( kv );
    }

    public Set<String> getFieldOptVals ( String id )
    {
        Set<String> r = new HashSet<String>();

        if ( strings != null )
        {
            String kv = FLD_VAL + getSubid ( id );

            for ( Entry<String, String> e : strings.entrySet() )
            {
                String k = e.getKey();
                String v = e.getValue();

                if ( k.startsWith ( kv ) )
                {
                    r.add ( v );
                }

            }

        }

        return r;
    }

    public String getFieldDesc ( String id )
    {
        String kv = FLD_DESC + getSubid ( id );
        return getString ( kv );
    }

    public String getFieldName ( String id )
    {
        String kv = FLD_NAME + getSubid ( id );
        return getString ( kv );
    }

    public String getFieldType ( String id )
    {
        String kv = FLD_TYPE + getSubid ( id );
        return getString ( kv );
    }

    public Long getFieldNumberDef ( String id )
    {
        String kv = FLD_DEF + getSubid ( id );
        return getNumber ( kv );
    }

    public Double getFieldDecimalDef ( String id )
    {
        String kv = FLD_DEF + getSubid ( id );
        return getDecimal ( kv );
    }

    public String getFieldStringDef ( String id )
    {
        String kv = FLD_DEF + getSubid ( id );
        return getString ( kv );
    }

    public String getFieldTextDef ( String id )
    {
        String kv = FLD_DEF + getSubid ( id );
        return getText ( kv );
    }

    public Long getFieldNumber ( String id )
    {
        String kv = FLD + getSubid ( id );
        return getNumber ( kv );
    }

    public Double getFieldDecimal ( String id )
    {
        String kv = FLD + getSubid ( id );
        return getDecimal ( kv );
    }

    public String getFieldString ( String id )
    {
        String kv = FLD + getSubid ( id );
        return getString ( kv );
    }

    public String getFieldText ( String id )
    {
        String kv = FLD + getSubid ( id );
        return getText ( kv );
    }

    public Boolean getFieldBoolean ( String id )
    {
        String kv = FLD + getSubid ( id );
        String v = getString ( kv );

        if ( v != null )
        {
            return Boolean.valueOf ( v );
        }

        return null;
    }

    public Set<String> listFields()
    {
        Set<String> r = new HashSet<String>();

        if ( strings != null )
        {
            for ( Entry<String, String> s : strings.entrySet() )
            {
                String k = s.getKey();
                String v = s.getValue();

                if ( k.startsWith ( FLD_ID ) )
                {
                    r.add ( v );
                }

            }

        }

        return r;
    }

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
    private Map<String, Double> privatedecimals;

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
        privatedecimals = null;
    }

    public void makeCopy ( CObj c )
    {
        c.clear();
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

        if ( privatedecimals != null )
        {
            for ( Entry<String, Double> e : privatedecimals.entrySet() )
            {
                c.pushPrivateDecimal ( e.getKey(), e.getValue() );
            }

        }

    }

    public CObj clone()
    {
        CObj c = new CObj();
        makeCopy ( c );

        return c;
    }

    public JSONObject GETPRIVATEJSON()
    {
        JSONObject r = getJSON();

        if ( privatedata != null )
        {
            if ( privatedata.size() > 0 )
            {
                JSONObject so = new JSONObject();

                for ( Entry<String, String> e : privatedata.entrySet() )
                {
                    so.put ( e.getKey(), e.getValue() );
                }

                r.put ( "PRIVATE_STRINGS", so );
            }

        }

        if ( privatenumbers != null )
        {
            if ( privatenumbers.size() > 0 )
            {
                JSONObject so = new JSONObject();

                for ( Entry<String, Long> e : privatenumbers.entrySet() )
                {
                    so.put ( e.getKey(), e.getValue() );
                }

                r.put ( "PRIVATE_NUMBERS", so );
            }

        }

        if ( privatedecimals != null )
        {
            if ( privatedecimals.size() > 0 )
            {
                JSONObject so = new JSONObject();

                for ( Entry<String, Double> e : privatedecimals.entrySet() )
                {
                    so.put ( e.getKey(), e.getValue() );
                }

                r.put ( "PRIVATE_DECIMALS", so );
            }

        }

        return r;
    }

    public void LOADPRIVATEJSON ( JSONObject jo )
    {
        loadJSON ( jo );

        if ( jo.has ( "PRIVATE_STRINGS" ) )
        {
            JSONObject so = jo.getJSONObject ( "PRIVATE_STRINGS" );

            if ( so.length() > 0 )
            {
                privatedata = new HashMap<String, String>();
                Iterator<String> i = so.keys();

                while ( i.hasNext() )
                {
                    String k = i.next();
                    String v = so.getString ( k );
                    privatedata.put ( k, v );
                }

            }

        }

        if ( jo.has ( "PRIVATE_NUMBERS" ) )
        {
            JSONObject so = jo.getJSONObject ( "PRIVATE_NUMBERS" );

            if ( so.length() > 0 )
            {
                privatenumbers = new HashMap<String, Long>();
                Iterator<String> i = so.keys();

                while ( i.hasNext() )
                {
                    String k = i.next();
                    long v = so.getLong ( k );
                    privatenumbers.put ( k, v );
                }

            }

        }

        if ( jo.has ( "PRIVATE_DECIMALS" ) )
        {
            JSONObject so = jo.getJSONObject ( "PRIVATE_DECIMALS" );

            if ( so.length() > 0 )
            {
                privatedecimals = new HashMap<String, Double>();
                Iterator<String> i = so.keys();

                while ( i.hasNext() )
                {
                    String k = i.next();
                    double v = so.getDouble ( k );
                    privatedecimals.put ( k, v );
                }

            }

        }

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

        if ( privatedecimals != null )
        {
            if ( privatedecimals.size() > 0 )
            {
                for ( Entry<String, Double> e : privatedecimals.entrySet() )
                {
                    d.add ( new DoubleField ( docPrivateDecimal ( e.getKey() ), e.getValue(), Store.YES ) );
                    d.add ( new SortedNumericDocValuesField ( docPrivateDecimal ( e.getKey() ), NumericUtils.doubleToSortableLong ( e.getValue() ) ) );
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

    public static String docPrivateDecimal ( String t )
    {
        return "PRIVDEC_" + t;
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
            IndexableFieldType ft = i.fieldType();

            if ( ft.stored() )
            {

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
                    Number nm = i.numericValue();

                    if ( nm == null )
                    {
                        String st = i.stringValue();
                        nm = Long.valueOf ( st );
                    }

                    Long v = nm.longValue();
                    numbers.put ( nk, v );
                }

                if ( k.startsWith ( "decimals_" ) )
                {
                    if ( decimals == null )
                    {
                        decimals = new HashMap<String, Double>();
                    }

                    String nk = k.substring ( "decimals_".length() );
                    Double dv = i.numericValue().doubleValue();
                    decimals.put ( nk, dv );
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
                    Number nm = i.numericValue();

                    if ( nm == null )
                    {
                        String st = i.stringValue();
                        nm = Long.valueOf ( st );
                    }

                    privatenumbers.put ( nk, nm.longValue() );
                }

                if ( k.startsWith ( "PRIVDEC_" ) )
                {
                    if ( privatedecimals == null )
                    {
                        privatedecimals = new HashMap<String, Double>();
                    }

                    String nk = k.substring ( "PRIVDEC_".length() );
                    privatedecimals.put ( nk, i.numericValue().doubleValue() );
                }

                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                // DO NOT SAVE/RESTORE privatedata in JSON!!  Document ONLY!!!
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            }

        }

    }

    private boolean giveup = false;
    public void GiveUp()
    {
        giveup = true;
    }

    public void signX ( RSAPrivateCrtKeyParameters key, long bm )
    {
        byte dg[] = genPayment ( bm );

        if ( giveup ) { return; }

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


    public boolean checkPayment ( long bm )
    {
        byte td[] = digest();
        return checkPayment ( bm, td );
    }

    public boolean checkPayment ( long bm, byte precalcdig[] )
    {

        if ( bm > 0 )
        {
            byte tstb[] = Utils.getTarget ( bm, precalcdig.length );

            if ( !Utils.checkDig ( precalcdig, tstb ) )
            {
                return false;
            }

            String payment = this.getString ( CObj.PAYMENT );

            if ( payment == null )
            {
                return false;
            }

            String pl[] = payment.split ( "," );

            if ( pl.length != 2 )
            {
                return false;
            }

            strings.remove ( CObj.PAYMENT );
            byte paydig[] = digest();
            byte basedig[] = Utils.toByteArray ( pl[0] );

            if ( !Arrays.equals ( paydig, basedig ) )
            {
                return false;
            }

            pushString ( CObj.PAYMENT, payment );
        }

        if ( !Arrays.equals ( precalcdig, Utils.toByteArray ( dig ) ) ) { return false; }

        return true;
    }

    public boolean checkSignatureX ( RSAKeyParameters key, long bm )
    {
        byte td[] = digest();

        if ( !checkPayment ( bm, td ) )
        {
            return false;
        }

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

    private byte[] genPayment ( long bm )
    {

        byte d[] = digest();

        byte rb[] = new byte[d.length];
        System.arraycopy ( d, 0, rb, 0, d.length );

        if ( bm > 0 )
        {
            byte tstb[] = Utils.getTarget ( bm, d.length );

            Map<String, String> cm = new HashMap<String, String>();
            byte tb[] = new byte[d.length];
            long payment = Utils.Random.nextLong();
            String paymentbase = Utils.toString ( d ) + ",";
            String paymentstr = paymentbase + Long.toString ( payment );
            cm.put ( CObj.PAYMENT, paymentstr );
            Utils.digStringMap ( rb, tb, d, cm );

            while ( !Utils.checkDig ( rb, tstb ) && !giveup )
            {
                payment++;
                paymentstr = paymentbase + Long.toString ( payment );
                cm.put ( CObj.PAYMENT, paymentstr );
                Utils.digStringMap ( rb, tb, d, cm );
            }

            pushString ( CObj.PAYMENT, paymentstr );
        }

        return rb;
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

    public void pushPrivateDecimal ( String key, Double v )
    {
        if ( v != null )
        {
            if ( privatedecimals == null )
            {
                privatedecimals = new HashMap<String, Double>();
            }

            privatedecimals.put ( key, v );
        }

    }

    public Double getPrivateDecimal ( String key )
    {
        if ( privatedecimals == null ) { return null; }

        return privatedecimals.get ( key );
    }

    public void pushString ( String key, String v )
    {
        if ( v != null )
        {
            if ( strings == null )
            {
                strings = new HashMap<String, String>();
            }

            if ( v.length() > MAXSTRING )
            {
                v = v.substring ( 0, MAXSTRING );
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

            if ( v.length() > MAXSTRING )
            {
                v = v.substring ( 0, MAXSTRING );
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

    public Map<String, Double> getPrivateDecimals()
    {
        return privatedecimals;
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
                String dig = getDig();
                name = name + " <" + dig.substring ( dig.length() - 6, dig.length() ) + ">";
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
                String dig = getDig();
                name = name + " <" + dig.substring ( dig.length() - 6, dig.length() ) + ">";
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

    @SuppressWarnings ( "rawtypes" )
    public boolean mapEq ( Map<?, ?> m, Map<?, ?> n )
    {
        Set<?> k0 = new HashSet();
        Set<?> k1 = new HashSet();

        if ( m != null )
        {
            k0 = m.keySet();
        }

        if ( n != null )
        {
            k1 = n.keySet();
        }

        Iterator<?> i = k0.iterator();

        while ( i.hasNext() )
        {
            Object o = i.next();

            if ( o instanceof String )
            {
                String ks = ( String ) o;

                if ( ks.startsWith ( CObj.PRV_GLOBAL_SEQ ) )
                {
                    i.remove();
                }

                else if ( ks.startsWith ( "prv_push" ) )
                {
                    i.remove();
                }

                else if ( CObj.LASTUPDATE.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.PRV_TEMP_NEWPOSTS.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.PRV_SKIP_PAYMENT.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.PRV_LOCALFILE.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.LOCALFILE.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.MINE.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.UPGRADEFLAG.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.STATUS.equals ( ks ) )
                {
                    i.remove();
                }

            }

        }

        i = k1.iterator();

        while ( i.hasNext() )
        {
            Object o = i.next();

            if ( o instanceof String )
            {
                String ks = ( String ) o;

                if ( ks.startsWith ( CObj.PRV_GLOBAL_SEQ ) )
                {
                    i.remove();
                }

                else if ( ks.startsWith ( "prv_push" ) )
                {
                    i.remove();
                }

                else if ( CObj.LASTUPDATE.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.PRV_TEMP_NEWPOSTS.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.PRV_SKIP_PAYMENT.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.PRV_LOCALFILE.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.LOCALFILE.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.MINE.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.UPGRADEFLAG.equals ( ks ) )
                {
                    i.remove();
                }

                else if ( CObj.STATUS.equals ( ks ) )
                {
                    i.remove();
                }

            }

        }

        if ( k0.size() != k1.size() ) { return false; }

        if ( !k0.containsAll ( k1 ) ) { return false; }

        if ( !k1.containsAll ( k0 ) ) { return false; }

        if ( k0.size() > 0 )
        {
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

        if ( !mapEq ( privatedecimals, b.getPrivateDecimals() ) ) { return false; }

        return true;
    }

    public boolean whoopyPubEquals ( Object o )
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

    @Override
    public String toString()
    {
        JSONObject js = this.GETPRIVATEJSON();
        return js.toString();
    }

}
