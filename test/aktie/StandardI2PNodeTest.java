package aktie;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Test;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.DumpIndexUtil;
import aktie.index.Index;
import aktie.net.ConnectionListener;
import aktie.net.ConnectionManager2;
import aktie.net.ConnectionThread;
import aktie.upgrade.UpgradeControllerCallback;
import aktie.utils.FUtils;

public class StandardI2PNodeTest
{

    class NodeListener  implements UpdateCallback, ConnectionListener, UpgradeControllerCallback
    {

        public boolean doupgrade = false;
        public boolean hascheckeddoupgrade = false;
        public String updateMessage = null;

        @Override
        public boolean doUpgrade()
        {
            hascheckeddoupgrade = true;
            return doupgrade;
        }

        @Override
        public void updateMessage ( String msg )
        {
            System.out.println ( "updateMessage: " + msg );
            updateMessage = msg;
        }

        @Override
        public void update ( ConnectionThread ct )
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void closed ( ConnectionThread ct )
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void bytesReceived ( long bytes )
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void bytesSent ( long bytes )
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void update ( Object o )
        {
            // TODO Auto-generated method stub

        }

    }

    public void prepareNode ( String masterDir, String nodedir, StandardI2PNode n, NodeListener l ) throws IOException
    {
        File nf = new File ( nodedir );
        FUtils.deleteDir ( nf );
        nf.mkdirs();

        File ss = new File ( masterDir + File.separator + "defseed.dat" );
        File sd = new File ( masterDir + File.separator + "developerid.dat" );
        File sc = new File ( masterDir + File.separator + "defcom.dat" );

        File ts = new File ( nodedir + File.separator + "defseed.dat" );
        File td = new File ( nodedir + File.separator + "developerid.dat" );
        File tc = new File ( nodedir + File.separator + "defcom.dat" );

        FUtils.copy ( ss, ts );
        FUtils.copy ( sd, td );
        FUtils.copy ( sc, tc );

        Properties p = new Properties();
        n.bootNode ( nodedir, p, l, l, l, l );
        n.nodeStarted();
    }

    public void pause ( long p )
    {
        try
        {
            Thread.sleep ( p * 1000L );
        }

        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

    }

    private static StandardI2PNode nd;
    private static StandardI2PNode sn00;

    @AfterClass
    public static void clearnup()
    {
        System.out.println ( "StandardI2PNode closing nodes" );

        if ( nd !=  null )
        {
            nd.closeNode();
        }

        if ( sn00 != null )
        {
            sn00.closeNode();
        }

        TestNode.restoreConsts();
    }

    @Test
    public void startNode()
    {
        try
        {
            TestNode.setTestConsts();
            Wrapper.TESTNODE = true;
            ConnectionManager2.ALLOWGLOBALAFTERSTARTUP = 10L * 1000L;
            ConnectionManager2.MAX_CONNECTION_TIME = 30L * 1000L;

            String masterDir = "stdnode000";

            File tmpdir = new File ( masterDir );
            FUtils.deleteDir ( tmpdir );

            NodeListener masterlistener = new NodeListener();
            masterlistener.doupgrade = false;

            nd = new StandardI2PNode();
            Properties p = new Properties();
            nd.bootNode ( masterDir, p, masterlistener, masterlistener, masterlistener, masterlistener );
            nd.nodeStarted();

            pause ( 5 );

            CObjList mylst = nd.getNode().getIndex().getMyIdentities();
            assertEquals ( 1, mylst.size() );
            CObj myid = mylst.get ( 0 );
            mylst.close();

            CObj com = new CObj();
            com.setType ( CObj.COMMUNITY );
            com.pushPrivate ( CObj.NAME, "master community" );
            com.pushPrivate ( CObj.DESCRIPTION, "master community used for test" );
            com.pushString ( CObj.CREATOR, myid.getId() );
            com.pushString ( CObj.SCOPE, CObj.SCOPE_PUBLIC );
            nd.getNode().enqueue ( com );

            pause ( 5 );

            CObjList nlst = nd.getNode().getIndex().getPublicCommunities();
            assertEquals ( 1, nlst.size() );
            CObj pubcom = nlst.get ( 0 );
            nlst.close();

            CObj sub0 = new CObj();
            sub0.setType ( CObj.SUBSCRIPTION );
            sub0.pushString ( CObj.CREATOR, myid.getId() );
            sub0.pushString ( CObj.COMMUNITYID, pubcom.getDig() );
            sub0.pushString ( CObj.SUBSCRIBED, "true" );
            nd.getNode().enqueue ( sub0 );

            pause ( 5 );

            File testseeds = new File ( masterDir + File.separator + "defseed.dat" );
            nd.saveSeeds ( testseeds );
            assertTrue ( testseeds.exists() );

            File devfile = new File ( masterDir + File.separator + "developerid.dat" );
            nd.saveSeeds ( devfile );
            assertTrue ( devfile.exists() );
            nd.loadDeveloperIdentity ( devfile );

            File testcom = new File ( masterDir + File.separator + "defcom.dat" );
            nd.saveValidCommunities ( testcom );
            assertTrue ( testcom.exists() );

            StandardI2PNode.TESTLOADDEFAULTS = true;

            sn00 = new StandardI2PNode();
            NodeListener snl00 = new NodeListener();
            snl00.doupgrade = true;
            prepareNode ( masterDir, "stdnode001", sn00, snl00 );

            pause ( 120 );

            nlst = sn00.getNode().getIndex().getIdentities();
            assertEquals ( 2, nlst.size() );
            nlst.close();

            nlst = sn00.getNode().getIndex().getAllSubscriptions();
            assertEquals ( 2, nlst.size() );
            nlst.close();

            nlst = nd.getNode().getIndex().getIdentities();
            assertEquals ( 2, nlst.size() );
            nlst.close();

            nlst = nd.getNode().getIndex().getAllSubscriptions();
            assertEquals ( 2, nlst.size() );
            nlst.close();

            //Post update file
            File f = new File ( "aktie.jar" );
            assertTrue ( f.exists() );

            CObj co = new CObj();
            co.setType ( CObj.HASFILE );
            co.pushString ( CObj.COMMUNITYID, pubcom.getDig() );
            co.pushString ( CObj.CREATOR, myid.getId() );
            co.pushPrivate ( CObj.LOCALFILE, f.getPath() );
            co.pushString ( CObj.UPGRADEFLAG, "true" );
            nd.getNode().enqueue ( co );

            pause ( 180 );

            System.out.println ( "ND INDEX---------------------------------------------------" );
            DumpIndexUtil.dumpIndex ( ( Index ) nd.getNode().getIndex() );
            System.out.println ( "SN00 INDEX-------------------------------------------------" );
            DumpIndexUtil.dumpIndex ( ( Index ) sn00.getNode().getIndex() );
            System.out.println ( "-----------------------------------------------------------" );

            File upfile = new File ( "stdnode001/upgrade/aktie.jar" );
            assertTrue ( FUtils.diff ( upfile, f ) );

            assertTrue ( masterlistener.hascheckeddoupgrade );
            assertTrue ( snl00.hascheckeddoupgrade );

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }


}
