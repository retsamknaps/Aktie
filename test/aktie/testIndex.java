package aktie;

import static org.junit.Assert.*;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.utils.FUtils;

public class testIndex
{

    @Test
    public void testSearcher()
    {
        File id = new File ( "testindex" );
        FUtils.deleteDir ( id );
        Index i = new Index();
        i.setIndexdir ( id );

        try
        {
            i.init();

            //comid: community0000
            //subj: Test subject zork
            //body: Body of zork dang
            //date: 23 Jan 2015
            //creator: 1
            //rank: 5
            //keyword: red
            //number: 300
            //decimal: 2.1

            //comid: community0000
            //subj: Test subject umpa zork
            //body: Body of umpa
            //date: 24 Jan 2015
            //creator: 2
            //rank: 8
            //keyword: blue
            //number: 200
            //decimal: 7.6

            //comid: community0001
            //subj: Test subject umpa zork
            //body: Body of umpa
            //date: 24 Jan 2015
            //creator: 2
            //rank: 8
            //keyword: blue
            //number: 200
            //decimal: 7.6

            CObj fldkeyword = new CObj();
            fldkeyword.setType ( CObj.FIELD );
            fldkeyword.pushString ( CObj.COMMUNITYID, "community0000" );
            fldkeyword.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_STRING );
            fldkeyword.pushString ( CObj.FLD_NAME, "keyword" );
            fldkeyword.pushString ( CObj.FLD_DESC, "keyword field" );
            fldkeyword.simpleDigest();

            CObj fldnum = new CObj();
            fldnum.setType ( CObj.FIELD );
            fldnum.pushString ( CObj.COMMUNITYID, "community0000" );
            fldnum.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_NUMBER );
            fldnum.pushString ( CObj.FLD_NAME, "number" );
            fldnum.pushString ( CObj.FLD_DESC, "number field" );
            fldnum.simpleDigest();

            CObj flddec = new CObj();
            flddec.setType ( CObj.FIELD );
            flddec.pushString ( CObj.COMMUNITYID, "community0000" );
            flddec.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_DECIMAL );
            flddec.pushString ( CObj.FLD_NAME, "decimal" );
            flddec.pushString ( CObj.FLD_DESC, "decimal field" );
            flddec.simpleDigest();

            CObj postred = new CObj();
            postred.setType ( CObj.POST );
            postred.pushString ( CObj.COMMUNITYID, "community0000" );
            postred.pushString ( CObj.SUBJECT, "Test subject zork color" );
            postred.pushText ( CObj.BODY, "Body of zork dang" );
            postred.pushString ( CObj.CREATOR, "1" );
            postred.pushNumber ( CObj.CREATEDON, 1000000 );
            postred.pushPrivateNumber ( CObj.PRV_USER_RANK, 5L );
            postred.setFieldString ( fldkeyword.getDig(), "red" );
            postred.setFieldNumber ( fldnum.getDig(), 300L );
            postred.setFieldDecimal ( flddec.getDig(), 2.1D );
            postred.simpleDigest();

            i.index ( postred );

            CObj postblue = new CObj();
            postblue.setType ( CObj.POST );
            postblue.pushString ( CObj.COMMUNITYID, "community0000" );
            postblue.pushString ( CObj.SUBJECT, "Test subject umpa derg" );
            postblue.pushText ( CObj.BODY, "Body of umpa color" );
            postblue.pushString ( CObj.CREATOR, "2" );
            postblue.pushNumber ( CObj.CREATEDON, 2000000 );
            postblue.pushPrivateNumber ( CObj.PRV_USER_RANK, 8L );
            postblue.setFieldString ( fldkeyword.getDig(), "blue" );
            postblue.setFieldNumber ( fldnum.getDig(), 200L );
            postblue.setFieldDecimal ( flddec.getDig(), 7.6D );
            postblue.simpleDigest();

            i.index ( postblue );

            CObj postgreen = new CObj();
            postgreen.setType ( CObj.POST );
            postgreen.pushString ( CObj.COMMUNITYID, "community0001" );
            postgreen.pushString ( CObj.SUBJECT, "Test subject umpa derg" );
            postgreen.pushText ( CObj.BODY, "Body of umpa color" );
            postgreen.pushString ( CObj.CREATOR, "2" );
            postgreen.pushNumber ( CObj.CREATEDON, 2000000 );
            postgreen.pushPrivateNumber ( CObj.PRV_USER_RANK, 8L );
            postgreen.setFieldString ( fldkeyword.getDig(), "blue" );
            postgreen.setFieldNumber ( fldnum.getDig(), 200L );
            postgreen.setFieldDecimal ( flddec.getDig(), 7.6D );
            postgreen.simpleDigest();

            i.index ( postgreen );

            i.forceNewSearcher();

            CObj qryzork = new CObj();
            qryzork.pushString ( CObj.COMMUNITYID, "community0000" );
            qryzork.pushString ( CObj.SUBJECT, "zork" );
            List<CObj> ql = new LinkedList<CObj>();
            ql.add ( qryzork );

            CObjList cl = i.searchPostsQuery ( ql, null );

            assertEquals ( 1, cl.size() );
            CObj tc = cl.get ( 0 );
            assertEquals ( postred.getDig(), tc.getDig() );

            cl.close();

            CObj qrycolor = new CObj();
            qrycolor.pushString ( CObj.COMMUNITYID, "community0000" );
            qrycolor.pushString ( CObj.SUBJECT, "color" );
            ql = new LinkedList<CObj>();
            ql.add ( qrycolor );

            cl = i.searchPostsQuery ( ql, null );

            assertEquals ( 2, cl.size() );

            cl.close();

            CObj qrycreator = new CObj();
            qrycreator.pushString ( CObj.COMMUNITYID, "community0000" );
            qrycreator.pushString ( CObj.CREATOR, "1" );
            ql = new LinkedList<CObj>();
            ql.add ( qrycreator );

            cl = i.searchPostsQuery ( ql, null );

            assertEquals ( 1, cl.size() );
            tc = cl.get ( 0 );
            assertEquals ( postred.getDig(), tc.getDig() );

            cl.close();

            CObj qryearlier = new CObj();
            qryearlier.pushString ( CObj.COMMUNITYID, "community0000" );
            qryearlier.pushNumber ( CObj.QRY_MAX_DATE, 1003000 );
            ql = new LinkedList<CObj>();
            ql.add ( qryearlier );

            cl = i.searchPostsQuery ( ql, null );

            assertEquals ( 1, cl.size() );
            tc = cl.get ( 0 );
            assertEquals ( postred.getDig(), tc.getDig() );

            cl.close();

            CObj qrylater = new CObj();
            qrylater.pushString ( CObj.COMMUNITYID, "community0000" );
            qrylater.pushNumber ( CObj.QRY_MIN_DATE, 1003000 );
            ql = new LinkedList<CObj>();
            ql.add ( qrylater );

            cl = i.searchPostsQuery ( ql, null );

            assertEquals ( 1, cl.size() );
            tc = cl.get ( 0 );
            assertEquals ( postblue.getDig(), tc.getDig() );

            cl.close();

            CObj qrynum = new CObj();
            qrynum.pushString ( CObj.COMMUNITYID, "community0000" );
            CObj fldnumq = fldnum.clone();
            fldnumq.pushNumber ( CObj.FLD_MAX, Long.MAX_VALUE );
            fldnumq.pushNumber ( CObj.FLD_MIN, 295L );
            qrynum.setNewFieldNumber ( fldnumq, 295L );
            ql = new LinkedList<CObj>();
            ql.add ( qrynum );

            cl = i.searchPostsQuery ( ql, null );

            assertEquals ( 1, cl.size() );
            tc = cl.get ( 0 );
            assertEquals ( postred.getDig(), tc.getDig() );

            cl.close();

            CObj qrynummax = new CObj();
            qrynummax.pushString ( CObj.COMMUNITYID, "community0000" );
            CObj fldnumqmax = fldnum.clone();
            fldnumqmax.pushNumber ( CObj.FLD_MAX, 295L );
            fldnumqmax.pushNumber ( CObj.FLD_MIN, Long.MIN_VALUE );
            qrynummax.setNewFieldNumber ( fldnumqmax, 295L );
            ql = new LinkedList<CObj>();
            ql.add ( qrynummax );

            cl = i.searchPostsQuery ( ql, null );

            assertEquals ( 1, cl.size() );
            tc = cl.get ( 0 );
            assertEquals ( postblue.getDig(), tc.getDig() );

            cl.close();

            CObj qrydecmax = new CObj();
            qrydecmax.pushString ( CObj.COMMUNITYID, "community0000" );
            CObj flddecmaxq = flddec.clone();
            flddecmaxq.pushDecimal ( CObj.FLD_MAX, Double.MAX_VALUE );
            flddecmaxq.pushDecimal ( CObj.FLD_MIN, 5.4D );
            qrydecmax.setNewFieldDecimal ( flddecmaxq, 5.4D );
            ql = new LinkedList<CObj>();
            ql.add ( qrydecmax );

            cl = i.searchPostsQuery ( ql, null );

            assertEquals ( 1, cl.size() );
            tc = cl.get ( 0 );
            assertEquals ( postblue.getDig(), tc.getDig() );

            cl.close();

            CObj qrydecmin = new CObj();
            qrydecmin.pushString ( CObj.COMMUNITYID, "community0000" );
            CObj flddecminq = flddec.clone();
            flddecminq.pushDecimal ( CObj.FLD_MAX, 5.4D );
            flddecminq.pushDecimal ( CObj.FLD_MIN, Double.MIN_VALUE );
            qrydecmin.setNewFieldDecimal ( flddecminq, 5.4D );
            ql = new LinkedList<CObj>();
            ql.add ( qrydecmin );

            cl = i.searchPostsQuery ( ql, null );

            assertEquals ( 1, cl.size() );
            tc = cl.get ( 0 );
            assertEquals ( postred.getDig(), tc.getDig() );

            cl.close();

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void testIndexer()
    {
        File id = new File ( "testindex" );
        FUtils.deleteDir ( id );
        Index i = new Index();
        i.setIndexdir ( id );

        try
        {
            i.init();

            CObj b0 = new CObj();
            b0.setDig ( "dig0" );
            b0.setId ( "id0" );
            b0.setSignature ( "sig0" );
            b0.setType ( "type0" );
            b0.pushDecimal ( "dec", 0.54D );
            b0.pushDecimal ( "dec1", 1.44D );
            b0.pushNumber ( "num0", 123L );
            b0.pushPrivate ( "prv0", "private stuff" );
            b0.pushString ( "s0", "string0" );
            b0.pushString ( "s1", "string1" );
            b0.pushText ( "title", "This is a title" );
            i.index ( b0 );

            CObj b1 = new CObj();
            b1.setDig ( "dig1" );
            b1.setId ( "id1" );
            b1.setSignature ( "sig1" );
            b1.setType ( "type1" );
            b1.pushDecimal ( "dec", 0.54D );
            b1.pushDecimal ( "dec1", 1.44D );
            b1.pushNumber ( "num0", 123L );
            b1.pushPrivate ( "prv0", "private stuff" );
            b1.pushString ( "s0", "string0" );
            b1.pushString ( "s1", "string1" );
            b1.pushText ( "title", "This not not what you think!" );
            i.index ( b1 );

            CObjList l0 = i.search ( "text_title:what", 10000 );
            //List<CObj> l0 = i.searchId("id0");

            assertEquals ( 1, l0.size() );

            CObj c = l0.get ( 0 );
            l0.close();

            assertEquals ( c, b1 );

            l0 = i.search ( "text_title:title", 10000 );
            //List<CObj> l0 = i.searchId("id0");

            assertEquals ( 1, l0.size() );

            c = l0.get ( 0 );
            l0.close();

            assertEquals ( c, b0 );

            i.close();

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }

}
