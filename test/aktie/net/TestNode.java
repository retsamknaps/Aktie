package aktie.net;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

import aktie.ProcessQueue;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.user.NewCommunityProcessor;
import aktie.user.NewFileProcessor;
import aktie.user.NewIdentityProcessor;
import aktie.user.NewMembershipProcessor;
import aktie.user.NewPostProcessor;
import aktie.user.NewPrivateMessageProcessor;
import aktie.user.NewSubscriptionProcessor;
import aktie.user.NewTemplateProcessor;
import aktie.user.RequestFileHandler;
import aktie.utils.FUtils;

public class TestNode implements GuiCallback, ConnectionListener, DestinationListener
{

    private CObj nodeData;
    private Index index;
    private HH2Session session;
    private RequestFileHandler requestFile;
    private TestReq req = new TestReq();
    private Net net;
    private ProcessQueue userQueue = new ProcessQueue ( "testQueue" );
    private ConcurrentLinkedQueue<Object> updateQueue = new ConcurrentLinkedQueue<Object>();

    public TestNode ( String wkdir )
    {
        try
        {
            File tmpdir = new File ( wkdir );
            FUtils.deleteDir ( tmpdir );
            assertTrue ( tmpdir.mkdirs() );
            net = new RawNet ( tmpdir );
            session = new HH2Session();
            session.init ( wkdir );

            File id = new File ( wkdir + "lucene" );
            FUtils.deleteDir ( id );
            index = new Index();
            index.setIndexdir ( id );
            index.init();

            SpamTool st = new SpamTool ( index );

            NewFileProcessor nfp = new NewFileProcessor ( session, index, st, this );
            requestFile = new RequestFileHandler ( session, wkdir + File.separator + "dl", nfp, index );

            RequestFileHandler fileHandler = new RequestFileHandler ( session, "tndl", null, null );

            userQueue.addProcessor ( new NewCommunityProcessor ( session, null, index, st, this ) );
            userQueue.addProcessor ( nfp );
            userQueue.addProcessor ( new NewIdentityProcessor ( net, req, session, index, this, this, this, this, fileHandler, st ) );
            userQueue.addProcessor ( new NewMembershipProcessor ( session, null, index, st, this ) );
            userQueue.addProcessor ( new NewPostProcessor ( session, index, st, this ) );
            userQueue.addProcessor ( new NewSubscriptionProcessor ( session, null, index, st, this ) );
            userQueue.addProcessor ( new NewTemplateProcessor ( session, index, this ) );
            userQueue.addProcessor ( new NewPrivateMessageProcessor ( session, index, null, st, this ) );

            //  index.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }

    public void stop()
    {
        index.close();
        userQueue.stop();
    }

    public void newUserData ( Object d )
    {
        userQueue.enqueue ( d );
    }

    @Override
    public void update ( Object o )
    {
        System.out.println ( "UPDATE (**): " + o );

        if ( o instanceof CObj )
        {
            CObj co = ( CObj ) o;

            if ( co.getString ( CObj.ERROR ) != null )
            {
                System.out.println ( "ERROR: " + co.getString ( CObj.ERROR ) );
            }

            if ( co.getType() != null )
            {
                updateQueue.add ( o );
            }

        }

        else
        {
            updateQueue.add ( o );
        }

    }

    public Object pollGuiQueue()
    {
        Object r = updateQueue.poll();
        return r ;
    }

    public CObj getNodeData()
    {
        return nodeData;
    }

    public void setNodeData ( CObj nodeData )
    {
        this.nodeData = nodeData;
    }

    public TestReq getTestReq()
    {
        return req;
    }

    public Index getIndex()
    {
        return index;
    }

    public HH2Session getSession()
    {
        return session;
    }

    public RequestFileHandler getRequestFile()
    {
        return requestFile;
    }

    @Override
    public void update ( ConnectionThread ct )
    {
        System.out.println ( "        NET \\/ " + ct.getInBytes() + "(" + ct.getInNonFileBytes() + ") /\\ " + ct.getOutBytes() );

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
