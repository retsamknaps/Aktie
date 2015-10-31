package aktie;

import static org.junit.Assert.*;

import java.io.File;

import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.junit.Test;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.net.ConnectionListener;
import aktie.net.ConnectionThread;
import aktie.net.DestinationListener;
import aktie.net.DestinationThread;
import aktie.net.GetSendData;
import aktie.net.RawNet;
import aktie.user.NewCommunityProcessor;
import aktie.user.NewFileProcessor;
import aktie.user.NewIdentityProcessor;
import aktie.user.NewMembershipProcessor;
import aktie.user.NewPostProcessor;
import aktie.user.NewSubscriptionProcessor;
import aktie.user.NewTemplateProcessor;
import aktie.user.RequestFileHandler;
import aktie.utils.FUtils;

public class UserTest implements GuiCallback, GetSendData, ConnectionListener, DestinationListener
{

    @Test
    public void dotest()
    {
        File tmpdir = new File ( "h2dbtest" );
        FUtils.deleteDir ( tmpdir );
        assertTrue ( tmpdir.mkdirs() );
        HH2Session sf = new HH2Session();
        sf.init ( "h2dbtest" );

        File id = new File ( "testindex" );
        FUtils.deleteDir ( id );
        Index i = new Index();
        i.setIndexdir ( id );
        RequestFileHandler fileHandler = new RequestFileHandler ( sf, "usertestdl", null, null );

        try
        {
            i.init();
            RawNet net = new RawNet ( id );
            ProcessQueue q = new ProcessQueue();
            q.addProcessor ( new NewCommunityProcessor ( sf, i, this ) );
            q.addProcessor ( new NewFileProcessor ( sf, i, this ) );
            q.addProcessor ( new NewIdentityProcessor ( net, this, sf, i, this, this, this, this, fileHandler ) );
            q.addProcessor ( new NewMembershipProcessor ( sf, i, this ) );
            q.addProcessor ( new NewPostProcessor ( sf, i, this ) );
            q.addProcessor ( new NewSubscriptionProcessor ( sf, i, this ) );
            q.addProcessor ( new NewTemplateProcessor ( sf, i, this ) );

            //New identity
            CObj i0 = new CObj();
            i0.setType ( CObj.IDENTITY );
            i0.pushString ( CObj.NAME, "aaaaa" );
            lastupdate = null;
            q.enqueue ( i0 );

            int loops = 120;

            while ( lastupdate == null && loops > 0 )
            {
                Thread.sleep ( 1000 );
                loops--;
            }

            assertNotNull ( lastupdate );
            assertNull ( lastupdate.getString ( CObj.ERROR ) );
            String privkey = lastupdate.getPrivate ( CObj.PRIVATEKEY );
            assertNotNull ( privkey );
            RSAPrivateCrtKeyParameters mykey = Utils.privateKeyFromString ( privkey );
            lastupdate.checkSignature ( mykey );
            assertNotNull ( lastupdate.getString ( CObj.DEST ) );
            System.out.println ( lastupdate.getString ( CObj.DEST ) );
            String id0 = lastupdate.getId();
            assertNotNull ( id0 );
            //CObj myid0 = lastupdate;

            //New identity
            CObj i1 = new CObj();
            i1.setType ( CObj.IDENTITY );
            i1.pushString ( CObj.NAME, "aaabb" );
            lastupdate = null;
            q.enqueue ( i1 );

            loops = 120;

            while ( lastupdate == null && loops > 0 )
            {
                Thread.sleep ( 1000 );
                loops--;
            }

            assertNotNull ( lastupdate );
            assertNull ( lastupdate.getString ( CObj.ERROR ) );
            privkey = lastupdate.getPrivate ( CObj.PRIVATEKEY );
            assertNotNull ( privkey );
            mykey = Utils.privateKeyFromString ( privkey );
            lastupdate.checkSignature ( mykey );
            assertNotNull ( lastupdate.getString ( CObj.DEST ) );
            System.out.println ( lastupdate.getString ( CObj.DEST ) );
            String id1 = lastupdate.getId();
            assertNotNull ( id1 );
            //CObj myid1 = lastupdate;

            //community - private
            lastupdate = null;
            CObj c0 = new CObj();
            c0.setType ( CObj.COMMUNITY );
            c0.pushPrivate ( CObj.NAME, "g000_name" );
            c0.pushPrivate ( CObj.DESCRIPTION, "g000_descr" );
            c0.pushString ( CObj.CREATOR, id0 );
            c0.pushString ( CObj.SCOPE, CObj.SCOPE_PRIVATE );
            lastupdate = null;
            q.enqueue ( c0 );

            loops = 120;

            while ( lastupdate == null && loops > 0 )
            {
                Thread.sleep ( 1000 );
                loops--;
            }

            assertNotNull ( lastupdate );
            assertEquals ( CObj.COMMUNITY, lastupdate.getType() );
            assertNull ( lastupdate.getString ( CObj.ERROR ) );
            assertNotNull ( lastupdate.getString ( CObj.PAYLOAD ) );
            assertNotNull ( lastupdate.getDig() );
            System.out.println ( lastupdate.getString ( CObj.PAYLOAD ) );
            c0 = lastupdate;

            //community - public
            lastupdate = null;
            CObj c1 = new CObj();
            c1.setType ( CObj.COMMUNITY );
            c1.pushPrivate ( CObj.NAME, "g001_name" );
            c1.pushPrivate ( CObj.DESCRIPTION, "g001_descr" );
            c1.pushString ( CObj.CREATOR, id0 );
            c1.pushString ( CObj.SCOPE, CObj.SCOPE_PUBLIC );
            lastupdate = null;
            q.enqueue ( c1 );

            loops = 120;

            while ( lastupdate == null && loops > 0 )
            {
                Thread.sleep ( 1000 );
                loops--;
            }

            assertNotNull ( lastupdate );
            assertNull ( lastupdate.getString ( CObj.ERROR ) );
            assertNotNull ( lastupdate.getString ( CObj.PAYLOAD ) );
            assertNotNull ( lastupdate.getDig() );
            System.out.println ( lastupdate.getString ( CObj.PAYLOAD ) );
            c1 = lastupdate;

            //subscribe
            lastupdate = null;
            CObj s0 = new CObj();
            s0.setType ( CObj.SUBSCRIPTION );
            s0.pushString ( CObj.CREATOR, id0 );
            s0.pushString ( CObj.COMMUNITYID, c0.getDig() );
            s0.pushString ( CObj.SUBSCRIBED, "true" );
            lastupdate = null;
            q.enqueue ( s0 );

            loops = 120;

            while ( lastupdate == null && loops > 0 )
            {
                Thread.sleep ( 1000 );
                loops--;
            }

            assertNotNull ( lastupdate );
            assertNull ( lastupdate.getString ( CObj.ERROR ) );
            assertNotNull ( lastupdate.getDig() );

            //post
            lastupdate = null;
            CObj p0 = new CObj();
            p0.setType ( CObj.POST );
            p0.pushString ( CObj.COMMUNITYID, c0.getDig() );
            p0.pushString ( CObj.CREATOR, id0 );
            p0.pushString ( CObj.PAYLOAD, "Something here" );
            lastupdate = null;
            q.enqueue ( p0 );

            loops = 120;

            while ( lastupdate == null && loops > 0 )
            {
                Thread.sleep ( 1000 );
                loops--;
            }

            assertNotNull ( lastupdate );
            assertNull ( lastupdate.getString ( CObj.ERROR ) );
            assertNotNull ( lastupdate.getDig() );

            //file
            lastupdate = null;
            CObj f0 = new CObj();
            f0.setType ( CObj.HASFILE );
            f0.pushString ( CObj.COMMUNITYID, c0.getDig() );
            f0.pushString ( CObj.CREATOR, id0 );
            File tf = FUtils.createTestFile ( null, 5L * 1024L * 1024L + 1024L );
            System.out.println ( "TF: " + tf.getPath() );
            f0.pushPrivate ( CObj.LOCALFILE, tf.getPath() );
            lastupdate = null;
            q.enqueue ( f0 );

            loops = 120;

            while ( lastupdate == null && loops > 0 )
            {
                Thread.sleep ( 1000 );
                loops--;
            }

            assertNotNull ( lastupdate );
            assertNull ( lastupdate.getString ( CObj.ERROR ) );
            assertNotNull ( lastupdate.getDig() );
            String wdig = lastupdate.getString ( CObj.FILEDIGEST );
            String ddig = lastupdate.getString ( CObj.FRAGDIGEST );
            Long fsize = lastupdate.getNumber ( CObj.FRAGSIZE );
            Long csize = lastupdate.getNumber ( CObj.FILESIZE );
            assertNotNull ( wdig );
            assertNotNull ( ddig );
            assertNotNull ( fsize );
            assertNotNull ( csize );
            assertNotNull ( lastupdate.getDig() );
            assertEquals ( tf.length(), ( long ) csize );
            System.out.println ( "WDIG: " + wdig );
            System.out.println ( "DDIG: " + ddig );
            System.out.println ( "FSIZ: " + fsize );
            System.out.println ( "CSIZ: " + csize );

            int numparts = ( int ) ( csize / fsize );
            numparts = csize % fsize == 0 ? numparts : numparts + 1;

            CObjList fl = i.getFragments ( wdig, ddig );
            assertEquals ( numparts, fl.size() );
            System.out.println ( "PARTS: " + numparts );

            for ( int c = 0; c < fl.size(); c++ )
            {
                CObj g = fl.get ( c );
                assertEquals ( wdig, g.getString ( CObj.FILEDIGEST ) );
                assertEquals ( ddig, g.getString ( CObj.FRAGDIGEST ) );
                assertNotNull ( g.getDig() );
                assertNotNull ( g.getNumber ( CObj.FRAGSIZE ) );
                assertNotNull ( g.getNumber ( CObj.FRAGOFFSET ) );
                System.out.println ( "OF: " + g.getNumber ( CObj.FRAGOFFSET ) + "        " + g.getDig() +
                                     "        " + g.getNumber ( CObj.FRAGSIZE ) );
            }

            fl.close();
            i.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }

    private CObj lastupdate;

    @Override
    public void update ( Object o )
    {
        if ( o instanceof CObj )
        {
            CObj co = ( CObj ) o;

            if ( co.getType() != null && co.getString ( CObj.ERROR ) == null )
            {
                lastupdate = co;
            }

        }

    }

    @Override
    public CObj next ( String localdest, String remotedest, boolean fmode )
    {
        //Called to get new data to send to remote destinations
        return null;
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
    public void addDestination ( DestinationThread d )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDestinationOpen ( String dest )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void closeDestination ( CObj myid )
    {
        // TODO Auto-generated method stub

    }

}
