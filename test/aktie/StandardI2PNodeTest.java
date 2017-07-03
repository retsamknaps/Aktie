package aktie;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.net.ConnectionListener;
import aktie.net.ConnectionManager2;
import aktie.net.ConnectionThread;
import aktie.upgrade.UpgradeControllerCallback;
import aktie.utils.FUtils;

public class StandardI2PNodeTest
{

    class NodeListener  implements UpdateCallback, ConnectionListener, UpgradeControllerCallback
    {

        @Override
        public boolean doUpgrade()
        {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void updateMessage ( String msg )
        {
            // TODO Auto-generated method stub

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

    @Test
    public void startNode()
    {
        try
        {
            TestNode.setTestConsts();
            StandardI2PNode.TESTNODE = true;
            ConnectionManager2.ALLOWGLOBALAFTERSTARTUP = 30L * 1000L;

            String masterDir = "stdnode000";

            File tmpdir = new File ( masterDir );
            FUtils.deleteDir ( tmpdir );

            NodeListener masterlistener = new NodeListener();

            StandardI2PNode nd = new StandardI2PNode();
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

            File testseeds = new File ( masterDir + File.separator + "defseed.dat" );
            nd.saveSeeds ( testseeds );
            assertTrue ( testseeds.exists() );

            File devfile = new File ( masterDir + File.separator + "developerid.dat" );
            nd.saveSeeds ( devfile );
            assertTrue ( devfile.exists() );

            File testcom = new File ( masterDir + File.separator + "defcom.dat" );
            nd.saveValidCommunities ( testcom );
            assertTrue ( testcom.exists() );

            StandardI2PNode.TESTLOADDEFAULTS = true;

            StandardI2PNode sn00 = new StandardI2PNode();
            NodeListener snl00 = new NodeListener();
            prepareNode ( masterDir, "stdnode001", sn00, snl00 );

            pause ( 120 );

            CObjList nlst = sn00.getNode().getIndex().getIdentities();
            assertEquals ( 2, nlst.size() );
            nlst.close();

            //nlst = sn00.getNode().getIndex().getAllSubscriptions();
            //assertEquals(2, nlst.size());
            //nlst.close();

            nlst = nd.getNode().getIndex().getIdentities();
            assertEquals ( 2, nlst.size() );
            nlst.close();

            nlst = nd.getNode().getIndex().getAllSubscriptions();
            assertEquals ( 2, nlst.size() );
            nlst.close();

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }


}
