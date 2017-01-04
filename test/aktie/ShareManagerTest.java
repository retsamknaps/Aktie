package aktie;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.junit.Test;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.gui.Wrapper;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.user.NewFileProcessor;
import aktie.user.RequestFileHandler;
import aktie.user.ShareManager;
import aktie.utils.FUtils;
import aktie.utils.HasFileCreator;

import static org.junit.Assert.*;

import java.io.File;

public class ShareManagerTest implements GuiCallback
{

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

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail ( "Oops" );
        }

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
