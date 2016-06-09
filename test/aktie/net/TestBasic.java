package aktie.net;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import aktie.data.CObj;
import aktie.data.RequestFile;
import aktie.gui.Wrapper;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.utils.FUtils;

import org.hibernate.Session;
import org.junit.Test;

public class TestBasic
{

    public TestNode newId ( String id )
    {
        TestNode n0 = new TestNode ( id );

        CObj id0 = new CObj();
        id0.setType ( CObj.IDENTITY );
        id0.pushString ( CObj.NAME, id );

        n0.newUserData ( id0 );
        int trys = 100;
        Object tn0 = null;

        while ( tn0 == null && trys > 0 )
        {
            tn0 = n0.pollGuiQueue();
            trys--;

            try
            {
                Thread.sleep ( 100L );
            }

            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }

        }

        assertNotNull ( tn0 );
        assertTrue ( tn0 instanceof CObj );
        CObj cn0 = ( CObj ) tn0;
        n0.setNodeData ( cn0 );
        assertNotNull ( cn0.getId() );

        return n0;
    }

    public int getPort ( String dest )
    {
        String sp[] = dest.split ( ":" );
        return Integer.valueOf ( sp[1] );
    }


    private Object pollForData ( TestNode n )
    {
        int trys = 1000;
        Object o0 = null;

        while ( trys > 0 && o0 == null )
        {
            try
            {
                Thread.sleep ( 100 );
            }

            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }

            o0 = n.pollGuiQueue();
            trys--;
        }

        return o0;
    }

    @Test
    public void testIt()
    {
    	Wrapper.OLDPAYMENT = 0;
    	Wrapper.NEWPAYMENT = 5;
        Index.MIN_TIME_BETWEEN_SEARCHERS = 0;

        TestNode Tn0 = newId ( "n0" );
        TestNode Tn1 = newId ( "n1" );

        CObj n0 = Tn0.getNodeData();
        CObj n1 = Tn1.getNodeData();

        String d0 = n0.getString ( CObj.DEST );
        String d1 = n1.getString ( CObj.DEST );

        System.out.println ( "D0: " + d0 );
        System.out.println ( "D1: " + d1 );

        assertNotNull ( d0 );
        assertNotNull ( d1 );

        //int p0 = getPort(d0);
        int p1 = getPort ( d1 );
        String ns1 = "127.0.0.1:" + p1;

        Object o0 = Tn0.pollGuiQueue();
        assertNull ( o0 );

        Object o1 = Tn0.pollGuiQueue();
        assertNull ( o1 );

        DestinationThread dt0 = DestinationThread.threadlist.get ( d0 );

        dt0.connect ( ns1, true );  //Open a file only connection

        o0 = pollForData ( Tn1 );
        System.out.println ( "o0: " + o0 );
        assertTrue ( o0 instanceof CObj );
        CObj oc0 = ( CObj ) o0;
        assertEquals ( n0.getId(), oc0.getId() );
        assertEquals ( n0.getDig(), oc0.getDig() );

        o1 = pollForData ( Tn0 );
        System.out.println ( "o1: " + o1 );
        assertTrue ( o1 instanceof CObj );
        CObj oc1 = ( CObj ) o1;
        assertEquals ( n1.getId(), oc1.getId() );
        assertEquals ( n1.getDig(), oc1.getDig() );

        try
        {
            Thread.sleep ( 2000L );
        }

        catch ( InterruptedException e2 )
        {
            e2.printStackTrace();
        }

        dt0.connect ( ns1, false ); //Open a normal connection

        //Create a new public community.
        CObj com0 = new CObj();
        com0.setType ( CObj.COMMUNITY );
        com0.pushString ( CObj.CREATOR, n0.getId() );
        com0.pushPrivate ( CObj.NAME, "name_com0" );
        com0.pushPrivate ( CObj.DESCRIPTION, "desc_com0" );
        com0.pushString ( CObj.SCOPE, CObj.SCOPE_PUBLIC );
        Tn0.newUserData ( com0 );

        o0 = pollForData ( Tn0 );

        System.out.println ( "Class: " + o0.getClass().getName() );
        assertTrue ( o0 instanceof CObj );
        oc0 = ( CObj ) o0;
        System.out.println ( "COM0: ERROR: " + oc0.getString ( CObj.ERROR ) );
        System.out.println ( "COM0: TYPE:  " + oc0.getType() );
        assertNull ( oc0.getString ( CObj.ERROR ) );
        assertNotNull ( oc0.getDig() );
        assertEquals ( CObj.COMMUNITY, oc0.getType() );
        assertEquals ( com0.getPrivate ( CObj.NAME ), oc0.getPrivate ( CObj.NAME ) );
        assertEquals ( com0.getPrivate ( CObj.DESCRIPTION ), oc0.getPrivate ( CObj.DESCRIPTION ) );

        //Create a new private community.
        CObj com1 = new CObj();
        com1.setType ( CObj.COMMUNITY );
        com1.pushString ( CObj.CREATOR, n1.getId() );
        com1.pushPrivate ( CObj.NAME, "name_com1" );
        com1.pushPrivate ( CObj.DESCRIPTION, "desc_com1" );
        com1.pushString ( CObj.SCOPE, CObj.SCOPE_PRIVATE );
        Tn1.newUserData ( com1 );

        o1 = pollForData ( Tn1 );

        assertTrue ( o1 instanceof CObj );
        oc1 = ( CObj ) o1;
        System.out.println ( "COM0: ERROR: " + oc1.getString ( CObj.ERROR ) );
        System.out.println ( "COM0: TYPE:  " + oc1.getType() );
        System.out.println ( " NAME: " + oc1.getString ( CObj.NAME ) );
        assertNull ( oc1.getString ( CObj.ERROR ) );
        assertNotNull ( oc1.getDig() );
        assertEquals ( CObj.COMMUNITY, oc1.getType() );
        assertEquals ( com1.getPrivate ( CObj.NAME ), oc1.getPrivate ( CObj.NAME ) );
        assertEquals ( com1.getPrivate ( CObj.DESCRIPTION ), oc1.getPrivate ( CObj.DESCRIPTION ) );

        //Node 1 request new community from node0
        CObj reqcom = new CObj();
        reqcom.setType ( CObj.CON_REQ_COMMUNITIES );
        reqcom.pushString ( CObj.CREATOR, n0.getId() );
        reqcom.pushNumber ( CObj.FIRSTNUM, 0 );
        reqcom.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );

        Tn1.getTestReq().enqueue ( reqcom );

        o1 = pollForData ( Tn1 );
        assertNotNull ( o1 );
        assertTrue ( o1 instanceof CObj );
        oc1 = ( CObj ) o1;
        assertNotNull ( oc1.getDig() );
        assertEquals ( CObj.COMMUNITY, oc1.getType() );
        assertNotNull ( oc1.getString ( CObj.PAYLOAD ) );
        System.out.println ( "COM0 PAYLOAD: " + oc1.getString ( CObj.PAYLOAD ) );
        assertEquals ( com0.getString ( CObj.PAYLOAD ), oc1.getString ( CObj.PAYLOAD ) );

        //Node 1 request new community from node0
        reqcom = new CObj();
        reqcom.setType ( CObj.CON_REQ_COMMUNITIES );
        reqcom.pushString ( CObj.CREATOR, n1.getId() );
        reqcom.pushNumber ( CObj.FIRSTNUM, 0 );
        reqcom.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );

        Tn0.getTestReq().enqueue ( reqcom );

        o0 = pollForData ( Tn0 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        oc0 = ( CObj ) o0;
        assertNotNull ( oc0.getDig() );
        assertEquals ( CObj.COMMUNITY, oc0.getType() );
        assertNotNull ( oc0.getString ( CObj.PAYLOAD ) );
        System.out.println ( "COM1 PAYLOAD: " + oc0.getString ( CObj.PAYLOAD ) );
        assertEquals ( com1.getString ( CObj.PAYLOAD ), oc0.getString ( CObj.PAYLOAD ) );

        //Create membership
        System.out.println ( " ========================== PRIVATE TEST ==============================" );
        CObj prv0 = new CObj();
        prv0.setType ( CObj.PRIVMESSAGE );
        prv0.pushString ( CObj.CREATOR, n1.getId() );
        prv0.pushPrivate ( CObj.SUBJECT, "This is a test private message - subj" );
        prv0.pushPrivate ( CObj.BODY, "This is a test private message - body." );
        prv0.pushPrivate ( CObj.PRV_RECIPIENT, n0.getId() );
        Tn1.newUserData ( prv0 );

        o1 = pollForData ( Tn1 );
        assertNotNull ( o1 );
        assertTrue ( o1 instanceof CObj );
        oc1 = ( CObj ) o1;
        assertNotNull ( oc1.getDig() );
        assertEquals ( CObj.PRIVMESSAGE, oc1.getType() );
        assertNotNull ( oc1.getString ( CObj.PAYLOAD ) );
        assertNotNull ( oc1.getString ( CObj.PAYLOAD2 ) );
        System.out.println ( "PRIVATE IDENT:    " + oc1.getString ( CObj.MSGIDENT ) );
        System.out.println ( "PRIVATE SEQ:      " + oc1.getNumber ( CObj.SEQNUM ) );
        System.out.println ( "PRIVATE PAYLOAD:  " + oc1.getString ( CObj.PAYLOAD ) );
        System.out.println ( "PRIVATE PAYLOAD2: " + oc1.getString ( CObj.PAYLOAD2 ) );

        //Ok, now update private identifiers
        CObj reqprvident = new CObj();
        reqprvident.setType ( CObj.CON_REQ_PRVIDENT );
        reqprvident.pushString ( CObj.CREATOR, n1.getId() );
        reqprvident.pushNumber ( CObj.FIRSTNUM, 0 );
        reqprvident.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
        Tn0.getTestReq().enqueue ( reqprvident );

        o0 = pollForData ( Tn0 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        oc0 = ( CObj ) o0;
        assertNotNull ( oc0.getDig() );
        assertEquals ( CObj.PRIVIDENTIFIER, oc0.getType() );
        assertNotNull ( oc0.getString ( CObj.MSGIDENT ) );
        assertNotNull ( oc0.getPrivate ( CObj.KEY ) );
        assertNotNull ( oc0.getPrivate ( CObj.PRV_MSG_ID ) );
        assertNotNull ( oc0.getPrivate ( CObj.PRV_RECIPIENT ) );
        System.out.println ( "PRV IDENT: " + oc0.getString ( CObj.MSGIDENT ) );
        System.out.println ( "PRV KEY:   " + oc0.getPrivate ( CObj.PRV_MSG_ID ) );
        System.out.println ( "PRV RCP:   " + oc0.getPrivate ( CObj.PRV_RECIPIENT ) );

        //Ok, now update private messages
        CObj reqprvmsg = new CObj();
        reqprvmsg.setType ( CObj.CON_REQ_PRVMSG );
        reqprvmsg.pushString ( CObj.CREATOR, n1.getId() );
        reqprvmsg.pushNumber ( CObj.FIRSTNUM, 0 );
        reqprvmsg.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
        Tn0.getTestReq().enqueue ( reqprvmsg );

        o0 = pollForData ( Tn0 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        oc0 = ( CObj ) o0;
        assertNotNull ( oc0.getDig() );
        assertEquals ( CObj.PRIVMESSAGE, oc0.getType() );
        assertNotNull ( oc0.getString ( CObj.MSGIDENT ) );
        assertNotNull ( oc0.getPrivate ( CObj.SUBJECT ) );
        assertNotNull ( oc0.getPrivate ( CObj.BODY ) );
        assertEquals ( "true", oc0.getPrivate ( CObj.DECODED ) );
        System.out.println ( "PRV RCP:   " + oc0.getPrivate ( CObj.DECODED ) );
        System.out.println ( "PRV SUBJ:  " + oc0.getPrivate ( CObj.SUBJECT ) );
        System.out.println ( "PRV KEY:   " + oc0.getPrivate ( CObj.BODY ) );

        //Create membership
        System.out.println ( " ========================== PRIVATE TEST 1 ==============================" );
        CObj prv1 = new CObj();
        prv1.setType ( CObj.PRIVMESSAGE );
        prv1.pushString ( CObj.CREATOR, n0.getId() );
        prv1.pushPrivate ( CObj.SUBJECT, "This is a test private message - subj 22" );
        prv1.pushPrivate ( CObj.BODY, "This is a test private message - body 22" );
        prv1.pushPrivate ( CObj.PRV_RECIPIENT, n1.getId() );
        Tn0.newUserData ( prv1 );

        o1 = pollForData ( Tn0 );
        assertNotNull ( o1 );
        assertTrue ( o1 instanceof CObj );
        oc1 = ( CObj ) o1;
        assertNotNull ( oc1.getDig() );
        assertNull ( oc1.getString ( CObj.ERROR ) );
        assertEquals ( CObj.PRIVMESSAGE, oc1.getType() );
        assertNotNull ( oc1.getString ( CObj.PAYLOAD ) );
        assertNotNull ( oc1.getString ( CObj.PAYLOAD2 ) );
        System.out.println ( "2PRIVATE IDENT:    " + oc1.getString ( CObj.MSGIDENT ) );
        System.out.println ( "2PRIVATE SEQ:      " + oc1.getNumber ( CObj.SEQNUM ) );
        System.out.println ( "2PRIVATE PAYLOAD:  " + oc1.getString ( CObj.PAYLOAD ) );
        System.out.println ( "2PRIVATE PAYLOAD2: " + oc1.getString ( CObj.PAYLOAD2 ) );

        //Ok, now update private messages
        CObj reqprvmsg0 = new CObj();
        reqprvmsg0.setType ( CObj.CON_REQ_PRVMSG );
        reqprvmsg0.pushString ( CObj.CREATOR, n0.getId() );
        reqprvmsg0.pushNumber ( CObj.FIRSTNUM, 0 );
        reqprvmsg0.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
        Tn1.getTestReq().enqueue ( reqprvmsg0 );

        try
        {
            Thread.sleep ( 60000 );
        }

        catch ( InterruptedException e2 )
        {
            e2.printStackTrace();
        }

        assertNull ( Tn1.pollGuiQueue() );
        System.out.println ( "2MSG REQ DONE--------------------" );


        //Ok, now update private identifiers
        CObj reqprvident0 = new CObj();
        reqprvident0.setType ( CObj.CON_REQ_PRVIDENT );
        reqprvident0.pushString ( CObj.CREATOR, n0.getId() );
        reqprvident0.pushNumber ( CObj.FIRSTNUM, 0 );
        reqprvident0.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
        Tn1.getTestReq().enqueue ( reqprvident0 );

        o0 = pollForData ( Tn1 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        oc0 = ( CObj ) o0;
        assertNotNull ( oc0.getDig() );
        assertEquals ( CObj.PRIVIDENTIFIER, oc0.getType() );
        assertNotNull ( oc0.getString ( CObj.MSGIDENT ) );
        assertNotNull ( oc0.getPrivate ( CObj.KEY ) );
        assertNotNull ( oc0.getPrivate ( CObj.PRV_MSG_ID ) );
        assertNotNull ( oc0.getPrivate ( CObj.PRV_RECIPIENT ) );
        System.out.println ( "PRV IDENT: " + oc0.getString ( CObj.MSGIDENT ) );
        System.out.println ( "PRV KEY:   " + oc0.getPrivate ( CObj.PRV_MSG_ID ) );
        System.out.println ( "PRV RCP:   " + oc0.getPrivate ( CObj.PRV_RECIPIENT ) );

        o0 = pollForData ( Tn1 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        oc0 = ( CObj ) o0;
        assertNotNull ( oc0.getDig() );
        assertEquals ( CObj.PRIVMESSAGE, oc0.getType() );
        assertNotNull ( oc0.getString ( CObj.MSGIDENT ) );
        assertNotNull ( oc0.getPrivate ( CObj.SUBJECT ) );
        assertNotNull ( oc0.getPrivate ( CObj.BODY ) );
        assertEquals ( "true", oc0.getPrivate ( CObj.DECODED ) );
        System.out.println ( "PRV RCP:   " + oc0.getPrivate ( CObj.DECODED ) );
        System.out.println ( "PRV SUBJ:  " + oc0.getPrivate ( CObj.SUBJECT ) );
        System.out.println ( "PRV KEY:   " + oc0.getPrivate ( CObj.BODY ) );

        //Create membership
        System.out.println ( " ========================== MEMBERSHIP TEST ==============================" );
        CObj mem1 = new CObj();
        mem1.setType ( CObj.MEMBERSHIP );
        mem1.pushString ( CObj.CREATOR, n1.getId() );
        mem1.pushPrivate ( CObj.COMMUNITYID, com1.getDig() );
        mem1.pushPrivate ( CObj.MEMBERID, n0.getId() );
        mem1.pushPrivateNumber ( CObj.AUTHORITY, CObj.MEMBER_CAN_GRANT );
        Tn1.newUserData ( mem1 );

        o1 = pollForData ( Tn1 );
        assertNotNull ( o1 );
        assertTrue ( o1 instanceof CObj );
        oc1 = ( CObj ) o1;
        assertNotNull ( oc1.getDig() );
        assertEquals ( CObj.MEMBERSHIP, oc1.getType() );
        assertNotNull ( oc1.getString ( CObj.PAYLOAD ) );
        assertNotNull ( oc1.getString ( CObj.ENCKEY ) );
        System.out.println ( "MEMBERSHIP PAYLOAD: " + oc1.getString ( CObj.PAYLOAD ) );
        System.out.println ( "MEMBERSHIP ENCKEY:  " + oc1.getString ( CObj.ENCKEY ) );

        //Request new membership data.
        CObj reqmem = new CObj();
        reqmem.setType ( CObj.CON_REQ_MEMBERSHIPS );
        reqmem.pushString ( CObj.CREATOR, n1.getId() );
        reqmem.pushNumber ( CObj.FIRSTNUM, 0 );
        reqmem.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );

        Tn0.getTestReq().enqueue ( reqmem );

        o0 = pollForData ( Tn0 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        oc0 = ( CObj ) o0;
        assertNotNull ( oc0.getDig() );
        assertEquals ( CObj.MEMBERSHIP, oc0.getType() );
        assertNotNull ( oc0.getString ( CObj.PAYLOAD ) );
        assertNotNull ( oc0.getString ( CObj.ENCKEY ) );
        System.out.println ( "MEMBERSHIP PAYLOAD: " + oc0.getString ( CObj.PAYLOAD ) );
        System.out.println ( "MEMBERSHIP ENCKEY:  " + oc0.getString ( CObj.ENCKEY ) );
        assertEquals ( com1.getDig(), oc0.getPrivate ( CObj.COMMUNITYID ) );
        assertEquals ( n0.getId(), oc0.getPrivate ( CObj.MEMBERID ) );
        assertEquals ( CObj.MEMBER_CAN_GRANT, ( long ) oc0.getPrivateNumber ( CObj.AUTHORITY ) );

        //Create subscription
        System.out.println ( " ========================== SUB TEST ==============================" );
        CObj sub0 = new CObj();
        sub0.setType ( CObj.SUBSCRIPTION );
        sub0.pushString ( CObj.CREATOR, n0.getId() );
        sub0.pushString ( CObj.COMMUNITYID, com0.getDig() );
        sub0.pushString ( CObj.SUBSCRIBED, "true" );
        Tn0.newUserData ( sub0 );

        o0 = pollForData ( Tn0 );

        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        sub0 = ( CObj ) o0;
        assertNull ( sub0.getString ( CObj.ERROR ) );
        assertNotNull ( sub0.getDig() );

        //Create subscription
        CObj sub1 = new CObj();
        sub1.setType ( CObj.SUBSCRIPTION );
        sub1.pushString ( CObj.CREATOR, n1.getId() );
        sub1.pushString ( CObj.COMMUNITYID, com0.getDig() );
        sub1.pushString ( CObj.SUBSCRIBED, "true" );
        Tn1.newUserData ( sub1 );

        o1 = pollForData ( Tn1 );

        assertNotNull ( o1 );
        assertTrue ( o1 instanceof CObj );
        sub1 = ( CObj ) o1;
        assertNull ( sub1.getString ( CObj.ERROR ) );
        assertNotNull ( sub1.getDig() );

        //Rrequest subscriptions from each other.
        CObj subreq0 = new CObj();
        subreq0.setType ( CObj.CON_REQ_SUBS );
        subreq0.pushString ( CObj.COMMUNITYID, com0.getDig() );

        Tn0.getTestReq().enqueue ( subreq0 );

        o0 = pollForData ( Tn0 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        oc0 = ( CObj ) o0;
        assertEquals ( sub1.getDig(), oc0.getDig() );

        CObj subreq1 = new CObj();
        subreq1.setType ( CObj.CON_REQ_SUBS );
        subreq1.pushString ( CObj.COMMUNITYID, com0.getDig() );

        Tn1.getTestReq().enqueue ( subreq1 );

        o1 = pollForData ( Tn1 );
        assertNotNull ( o1 );
        assertTrue ( o1 instanceof CObj );
        oc1 = ( CObj ) o1;
        assertEquals ( sub0.getDig(), oc1.getDig() );

        //-------------------------------------------------------------
        //Create a post
        CObj p0 = new  CObj();
        p0.setType ( CObj.POST );
        p0.pushString ( CObj.CREATOR, n0.getId() );
        p0.pushString ( CObj.COMMUNITYID, com0.getDig() );
        p0.pushString ( "title", "HERE IS A TITLE! }} \\}" );

        p0.pushString ( "desc", "HERE IS A DESCRIPTION!" );
        Tn0.newUserData ( p0 );

        o0 = pollForData ( Tn0 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        p0 = ( CObj ) o0;
        assertNotNull ( p0.getDig() );

        //--------------------------------------------------------------
        //request posts
        CObj rp1 = new CObj();
        rp1.setType ( CObj.CON_REQ_POSTS );
        rp1.pushString ( CObj.COMMUNITYID, com0.getDig() );
        rp1.pushString ( CObj.CREATOR, n0.getId() );
        rp1.pushNumber ( CObj.FIRSTNUM, 0 );
        rp1.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );

        Tn1.getTestReq().enqueue ( rp1 );

        o1 = pollForData ( Tn1 );
        assertNotNull ( o1 );
        assertTrue ( o1 instanceof CObj );
        oc1 = ( CObj ) o1;
        assertEquals ( p0.getDig(), oc1.getDig() );

        //-------------------------------------------------------------------
        //Add a file
        File tf = null;

        try
        {
            tf = FUtils.createTestFile ( null, 5L * 1024L * 1024L + 1024L );
        }

        catch ( IOException e )
        {
            e.printStackTrace();
            fail();
        }

        CObj hf0 = new CObj();
        hf0.setType ( CObj.HASFILE );
        hf0.pushString ( CObj.CREATOR, n0.getId() );
        hf0.pushString ( CObj.COMMUNITYID, com0.getDig() );
        hf0.pushPrivate ( CObj.LOCALFILE, tf.getPath() );

        Tn0.newUserData ( hf0 );
        o0 = pollForData ( Tn0 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        hf0 = ( CObj ) o0;
        assertEquals ( CObj.HASFILE, hf0.getType() );
        assertNotNull ( hf0.getDig() );
        assertNotNull ( hf0.getString ( CObj.FILEDIGEST ) );
        assertNotNull ( hf0.getString ( CObj.FRAGDIGEST ) );

        //----------------------------------------------------
        //Test file rename
        File nf = null;

        try
        {
            nf = File.createTempFile ( "newfile", ".dat" );
            assertTrue ( tf.renameTo ( nf ) );
        }

        catch ( IOException e1 )
        {
            e1.printStackTrace();
            fail();
        }

        hf0 = new CObj();
        hf0.setType ( CObj.HASFILE );
        hf0.pushString ( CObj.CREATOR, n0.getId() );
        hf0.pushString ( CObj.COMMUNITYID, com0.getDig() );
        hf0.pushPrivate ( CObj.LOCALFILE, nf.getPath() );

        Tn0.newUserData ( hf0 );
        o0 = pollForData ( Tn0 );
        assertNotNull ( o0 );
        assertTrue ( o0 instanceof CObj );
        hf0 = ( CObj ) o0;
        assertEquals ( CObj.HASFILE, hf0.getType() );
        assertNotNull ( hf0.getDig() );
        assertNotNull ( hf0.getString ( CObj.FILEDIGEST ) );
        assertNotNull ( hf0.getString ( CObj.FRAGDIGEST ) );

        CObjList numf = Tn0.getIndex().getHasFiles ( com0.getDig(), hf0.getString ( CObj.FILEDIGEST ),
                        hf0.getString ( CObj.FRAGDIGEST ) );
        assertEquals ( 1, numf.size() );
        numf.close();

        //----------------------------------------------------
        //Request HasFile
        System.out.println ( " ========================== REQUEST HAS FILE ==============================" );
        CObj rhf1 = new CObj();
        rhf1.setType ( CObj.CON_REQ_HASFILE );
        rhf1.pushString ( CObj.COMMUNITYID, com0.getDig() );
        rhf1.pushString ( CObj.CREATOR, n0.getId() );
        rhf1.pushNumber ( CObj.FIRSTNUM, 0 );
        rhf1.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
        Tn1.getTestReq().enqueue ( rhf1 );

        o1 = pollForData ( Tn1 );
        assertNotNull ( o1 );
        assertTrue ( o1 instanceof CObj );
        oc1 = ( CObj ) o1;
        assertEquals ( hf0.getDig(), oc1.getDig() );

        System.out.println ( " ========================== REQUEST FRAGMENT LIST ==============================" );
        hf0.setType ( CObj.USR_DOWNLOAD_FILE );
        hf0.getPrivatedata().clear();
        assertNotNull ( Tn1.getRequestFile().createRequestFile ( hf0 ) );
        List<RequestFile> rflst = Tn1.getRequestFile().listRequestFiles ( RequestFile.REQUEST_FRAG_LIST, 10 );
        assertEquals ( 1, rflst.size() );
        RequestFile trf = rflst.get ( 0 );
        trf.setState ( RequestFile.REQUEST_FRAG_LIST_SNT ); //we have to indicate we already sent the request
        Session s = Tn1.getSession().getSession();
        s.getTransaction().begin();
        s.merge ( trf );
        s.getTransaction().commit();
        s.close();
        //Tn1.getRequestFile().updateRequestFile(trf);

        CObj rfl1 = new CObj();
        rfl1.setType ( CObj.CON_REQ_FRAGLIST );
        rfl1.pushString ( CObj.COMMUNITYID, com0.getDig() );
        rfl1.pushString ( CObj.FILEDIGEST, hf0.getString ( CObj.FILEDIGEST ) );
        rfl1.pushString ( CObj.FRAGDIGEST, hf0.getString ( CObj.FRAGDIGEST ) );
        Tn1.getTestReq().enqueue ( rfl1 );

        o1 = pollForData ( Tn1 );
        assertNotNull ( o1 );

        assertTrue ( o1 instanceof CObj );
        CObj to = ( CObj ) o1;
        assertEquals ( CObj.FILE, to.getType() );

        o1 = pollForData ( Tn1 );
        assertNotNull ( o1 );

        RequestFile nrf = ( RequestFile ) o1;
        assertEquals ( RequestFile.REQUEST_FRAG, nrf.getState() );

        System.out.println ( " ========================== REQUEST FRAGMENTS ==============================" );
        CObjList flst = Tn1.getIndex().getFragments ( hf0.getString ( CObj.FILEDIGEST ),
                        hf0.getString ( CObj.FRAGDIGEST ) );
        assertEquals ( ( long ) hf0.getNumber ( CObj.FRAGNUMBER ), flst.size() );

        try
        {
            for ( int c = flst.size() - 1; c >= 0 ; c-- )
            {
                CObj fg = flst.get ( c );
                CObj rg = new CObj();
                rg.setType ( CObj.CON_REQ_FRAG );
                rg.pushString ( CObj.COMMUNITYID, com0.getDig() );
                rg.pushString ( CObj.FILEDIGEST, hf0.getString ( CObj.FILEDIGEST ) );
                rg.pushString ( CObj.FRAGDIGEST, hf0.getString ( CObj.FRAGDIGEST ) );
                rg.pushString ( CObj.FRAGDIG, fg.getString ( CObj.FRAGDIG ) );
                Tn1.getTestReq().enqueue ( rg );
            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

        flst.close();

        RequestFile df0 = null;

        while ( df0 == null || df0.getFragsComplete() < df0.getFragsTotal() )
        {
            o0 = pollForData ( Tn1 );

            while ( ! ( o0 instanceof RequestFile ) )
            {
                o0 = pollForData ( Tn1 );
            }

            assertNotNull ( o0 );
            assertTrue ( o0 instanceof RequestFile );
            df0 = ( RequestFile ) o0;
            System.out.println ( "DF: " + df0.getFragsComplete() + " from: " + df0.getFragsTotal() );
        }

        assertNotEquals ( df0.getLocalFile(), nf );
        System.out.println ( "DF0: " + df0.getLocalFile() + " Exp: " + nf );

        try
        {
            assertTrue ( FUtils.diff ( new File ( df0.getLocalFile() ), nf ) );
        }

        catch ( IOException e )
        {
            e.printStackTrace();
            fail();
        }

        Tn0.stop();
        Tn1.stop();
        DestinationThread.stopAll();

    }

}
