package aktie;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.junit.Test;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.DirectoryShare;
import aktie.data.HH2Session;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.net.ConnectionListener;
import aktie.net.ConnectionThread;
import aktie.net.RawNet;
import aktie.spam.SpamTool;
import aktie.upgrade.UpgradeControllerCallback;
import aktie.user.NewFileProcessor;
import aktie.user.RequestFileHandler;
import aktie.user.ShareManager;
import aktie.utils.FUtils;
import aktie.utils.HasFileCreator;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ShareManagerTest implements UpdateCallback, UpgradeControllerCallback
{

    public class CallbackIntr implements UpdateCallback
    {

        public ConcurrentLinkedQueue<Object> oqueue = new ConcurrentLinkedQueue<Object>();

        @Override

        public synchronized void update ( Object o )
        {
            if ( o instanceof CObj )
            {
                CObj co = ( CObj ) o;

                if ( co.getType() != null && co.getString ( CObj.ERROR ) == null )
                {
                    oqueue.add ( o );
                }

            }

            else
            {
                oqueue.add ( o );
            }

            notifyAll();
        }

        public synchronized void waitForUpdate()
        {
            int trys = 60;

            while ( oqueue.size() == 0 && trys > 0 )
            {
                trys--;

                try
                {
                    wait ( 1000 );
                }

                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }

            }

        }

    }

    public class ConCallbackIntr implements ConnectionListener
    {

        public ConcurrentLinkedQueue<ConnectionThread> oqueue = new ConcurrentLinkedQueue<ConnectionThread>();

        @Override
        public void update ( ConnectionThread ct )
        {
            oqueue.add ( ct );
        }

        @Override
        public void closed ( ConnectionThread ct )
        {
            oqueue.add ( ct );
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

    }

    private CObj createIdentity ( Node n, String name )
    {
        CObj nn = new CObj();
        nn.setType ( CObj.IDENTITY );
        nn.pushString ( CObj.NAME, name );
        n.enqueue ( nn );
        return nn;
    }

    @Test
    public void testNodeShareManager()
    {
        TestNode.setTestConsts();

        CallbackIntr cb0 = new CallbackIntr();
        CallbackIntr cb1 = new CallbackIntr();

        ConCallbackIntr cn0 = new ConCallbackIntr();
        ConCallbackIntr cn1 = new ConCallbackIntr();

        File tmpdir = new File ( "tstnode" );
        FUtils.deleteDir ( tmpdir );
        tmpdir.mkdirs();

        RawNet net0 = new RawNet ( tmpdir );
        RawNet net1 = new RawNet ( tmpdir );

        try
        {
            FUtils.deleteDir ( new File ( "testnode0" ) );
            FUtils.deleteDir ( new File ( "testnode1" ) );

            Node n0 = new Node ( "testnode0", net0, cb0, cb0, cn0, this );
            Node n1 = new Node ( "testnode1", net1, cb1, cb1, cn1, this );

            System.out.println ( "CREATE IDENTIES.............................." );
            //CObj node0a =
            CObj node0a = createIdentity ( n0, "node0a" );

            CObj node1a = createIdentity ( n1, "node1a" );

            cb0.waitForUpdate();
            Object n0dat = cb0.oqueue.poll();
            assertEquals ( "node0a", ( ( CObj ) n0dat ).getString ( CObj.NAME ) );

            cb1.waitForUpdate();
            Object n1dat = cb1.oqueue.poll();
            assertEquals ( "node1a", ( ( CObj ) n1dat ).getString ( CObj.NAME ) );

            CObj seed = node0a.clone();
            seed.getPrivatedata().clear();
            seed.setType ( CObj.USR_SEED );
            n1.enqueue ( seed );

            Thread.sleep ( 30000L );

            CObjList lst = n0.getIndex().getIdentities();
            assertEquals ( 2, lst.size() );
            lst.close();

            lst = n1.getIndex().getIdentities();
            assertEquals ( 2, lst.size() );
            lst.close();

            CObj pubcom = new CObj();
            pubcom.setType ( CObj.COMMUNITY );
            pubcom.pushPrivate ( CObj.NAME, "pubcom" );
            pubcom.pushPrivate ( CObj.DESCRIPTION, "description pubcom" );
            pubcom.pushString ( CObj.CREATOR, seed.getId() );
            pubcom.pushString ( CObj.SCOPE, CObj.SCOPE_PUBLIC );
            n0.enqueue ( pubcom );
            cb0.waitForUpdate();
            cb0.oqueue.poll();

            Thread.sleep ( 30000L );

            lst = n0.getIndex().getCommunities ( seed.getId(), 0, Integer.MAX_VALUE );
            CObj community = lst.get ( 0 );
            assertEquals ( 1, lst.size() );
            lst.close();

            lst = n1.getIndex().getCommunities ( seed.getId(), 0, Integer.MAX_VALUE );
            assertEquals ( 1, lst.size() );
            lst.close();

            CObj sub1 = new CObj();
            sub1.setType ( CObj.SUBSCRIPTION );
            sub1.pushString ( CObj.CREATOR, node0a.getId() );
            sub1.pushString ( CObj.COMMUNITYID, community.getDig() );
            sub1.pushString ( CObj.SUBSCRIBED, "true" );
            n0.enqueue ( sub1 );

            CObj sub2 = new CObj();
            sub2.setType ( CObj.SUBSCRIPTION );
            sub2.pushString ( CObj.CREATOR, node1a.getId() );
            sub2.pushString ( CObj.COMMUNITYID, community.getDig() );
            sub2.pushString ( CObj.SUBSCRIBED, "true" );
            n1.enqueue ( sub2 );

            Thread.sleep ( 30000 );

            lst = n0.getIndex().getSubscriptions ( community.getDig(), null );
            assertEquals ( 2, lst.size() );
            lst.close();

            lst = n1.getIndex().getSubscriptions ( community.getDig(), null );
            assertEquals ( 2, lst.size() );
            lst.close();

            File tmp = new File ( "testshare" );
            FUtils.deleteDir ( tmp );
            tmp.mkdirs();

            File nf = FUtils.createTestFile ( tmp, 10L * 1024L * 1024L + 263L );

            n0.getShareManager().addShare ( community.getDig(), node0a.getId(),
                                            "testshare", tmp.getPath(), false, false );

            List<DirectoryShare> slst = n0.getShareManager().listShares ( community.getDig(), node0a.getId() );

            for ( int c = 0; c < slst.size(); c++ )
            {
                DirectoryShare ds = slst.get ( 0 );
                System.out.println ( "DS name: " + ds.getShareName() );
            }

            assertEquals ( 1, slst.size() );

            Thread.sleep ( 20000L );

            lst = n0.getIndex().getAllHasFiles();
            assertEquals ( 1, lst.size() );
            CObj co = lst.get ( 0 );
            assertEquals ( "true", co.getString ( CObj.STILLHASFILE ) );
            lst.close();

            lst = n0.getIndex().getAllHasFiles();
            assertEquals ( 1, lst.size() );
            co = lst.get ( 0 );
            assertEquals ( "true", co.getString ( CObj.STILLHASFILE ) );
            lst.close();

            nf.delete();

            Thread.sleep ( 20000 );

            lst = n0.getIndex().getAllHasFiles();
            assertEquals ( 1, lst.size() );
            co = lst.get ( 0 );
            assertEquals ( "false", co.getString ( CObj.STILLHASFILE ) );
            lst.close();

            lst = n0.getIndex().getAllHasFiles();
            assertEquals ( 1, lst.size() );
            co = lst.get ( 0 );
            assertEquals ( "false", co.getString ( CObj.STILLHASFILE ) );
            lst.close();

            n0.close();
            n1.close();
        }

        catch ( Exception e )
        {
            fail();
            e.printStackTrace();
        }

    }

    @Test
    public void testShareManager()
    {

        Wrapper.OLDPAYMENT = 0;
        Wrapper.NEWPAYMENT = 0x0400004000000000L;
        ShareManager.CHECKHASFILE_DELAY = 2000L;
        ShareManager.SHARE_DELAY = 2000L;

        File id = new File ( "testindex1" );
        FUtils.deleteDir ( id );

        File sd = new File ( "testh2" );
        FUtils.deleteDir ( sd );

        Index i = new Index();
        i.setIndexdir ( id );

        try
        {
            i.init();

            HH2Session s = new HH2Session();
            s = new HH2Session();
            s.init ( sd.getPath() );

            SpamTool spamtool = new SpamTool ( i );
            NewFileProcessor nfp = new NewFileProcessor ( s, i, spamtool, this ) ;
            RequestFileHandler rfh = new RequestFileHandler ( s, "testdls", nfp, i );
            HasFileCreator hfc = new HasFileCreator ( s, i, spamtool );
            ProcessQueue pq = new ProcessQueue ( "testUserQueue" );

            ShareManager sm = new ShareManager ( s, rfh, i, hfc, nfp, pq );

            //Fake the subscription
            CObj myident = new CObj();
            myident.setType ( CObj.IDENTITY );
            myident.pushPrivate ( CObj.MINE, "true" );
            myident.setId ( "MEM0" );
            AsymmetricCipherKeyPair pair = Utils.generateKeyPair();
            myident.pushPrivate ( CObj.PRIVATEKEY, Utils.stringFromPrivateKey (
                                      ( RSAPrivateCrtKeyParameters ) pair.getPrivate() ) );
            myident.pushString ( CObj.KEY, Utils.stringFromPublicKey (
                                     ( RSAKeyParameters ) pair.getPublic() ) );
            myident.pushPrivateNumber ( CObj.PRV_USER_RANK, 5L );
            i.index ( myident );

            CObj sub = new CObj();
            sub.setType ( CObj.SUBSCRIPTION );
            sub.pushString ( CObj.SUBSCRIBED, "true" );
            sub.pushString ( CObj.CREATOR, "MEM0" );
            sub.pushString ( CObj.COMMUNITYID, "COM0" );
            sub.pushPrivateNumber ( CObj.PRV_USER_RANK, 1L );
            sub.setDig ( "subdig00000" );
            i.index ( sub );

            //Add a share.
            File shrdir = new File ( "sharedir0" );
            FUtils.deleteDir ( shrdir );
            assertTrue ( shrdir.mkdirs() );
            sm.addShare ( "COM0", "MEM0", "TestShare0", shrdir.getPath(), false, false );
            //Add a file.
            File nf = FUtils.createTestFile ( shrdir, 10L * 1024L + 263L );
            assertTrue ( nf.exists() );
            //Wait for spam payment
            Thread.sleep ( 10000L );
            //Check for HasFile
            CObjList cl = i.getAllHasFiles();
            assertEquals ( 1, cl.size() );
            CObj hf = cl.get ( 0 );
            assertEquals ( nf.getCanonicalFile().getPath(), hf.getPrivate ( CObj.LOCALFILE ) );
            cl.close();

            //Delete file.
            assertTrue ( nf.delete() );

            Thread.sleep ( 20000 );

            cl = i.getAllHasFiles();
            assertEquals ( 1, cl.size() );
            hf = cl.get ( 0 );
            assertEquals ( "false", hf.getString ( CObj.STILLHASFILE ) );
            cl.close();

            sm.stop();

            i.close();

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail ( "Oops" );
        }

    }

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
    public void update ( Object o )
    {
        if ( o instanceof CObj )
        {
            CObj co = ( CObj ) o;
            System.out.println ( "Update called: " + co.getType() );
        }

    }

}
