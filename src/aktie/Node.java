package aktie;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.gui.Wrapper;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.net.ConnectionListener;
import aktie.net.ConnectionManager2;
import aktie.net.InSpamExProcessor;
import aktie.net.Net;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;
import aktie.user.NewCommunityProcessor;
import aktie.user.NewFileProcessor;
import aktie.user.NewForceSearcher;
import aktie.user.NewIdentityProcessor;
import aktie.user.NewMembershipProcessor;
import aktie.user.NewPostProcessor;
import aktie.user.NewPrivateMessageProcessor;
import aktie.user.NewPushProcessor;
import aktie.user.NewQueryProcessor;
import aktie.user.NewSpamExProcessor;
import aktie.user.NewSubscriptionProcessor;
import aktie.user.RequestFileHandler;
import aktie.user.ShareManager;
import aktie.user.UsrCancelFileProcessor;
import aktie.user.UsrReqComProcessor;
import aktie.user.UsrReqFileProcessor;
import aktie.user.UsrReqHasFileProcessor;
import aktie.user.UsrReqIdentityProcessor;
import aktie.user.UsrReqMemProcessor;
import aktie.user.UsrReqPostProcessor;
import aktie.user.UsrReqPrvMsgProcessor;
import aktie.user.UsrReqSetRankProcessor;
import aktie.user.UsrReqShareProcessor;
import aktie.user.UsrReqSpamExProcessor;
import aktie.user.UsrReqSubProcessor;
import aktie.user.UsrSeed;
import aktie.user.UsrSeedCommunity;
import aktie.user.UsrStartDestinationProcessor;
import aktie.utils.HasFileCreator;

public class Node
{

    private Net network;
    private Index index;
    private HH2Session session;
    private ProcessQueue userQueue;
    private IdentityManager identManager;
    private GuiCallback usrCallback;
    private GuiCallback netCallback;
    private ConnectionListener conCallback;
    private ConnectionManager2 conMan;
    private RequestFileHandler requestHandler;
    private ShareManager shareManager;
    private Settings settings;
    private HasFileCreator hasFileCreator;
    private SpamTool spamtool;


    public Node ( String nodedir, Net net, GuiCallback uc,
                  GuiCallback nc, ConnectionListener cc ) throws IOException
    {
        usrCallback = uc;
        netCallback = nc;
        conCallback = cc;
        network = net;
        settings = new Settings ( nodedir );
        File idxdir = new File ( nodedir + File.separator + "index" );
        index = new Index();
        index.setIndexdir ( idxdir );
        index.init();
        spamtool = new SpamTool ( index );

        session = new HH2Session();
        session.init ( nodedir + File.separator + "h2" );
        identManager = new IdentityManager ( session, index );
        NewFileProcessor nfp = new NewFileProcessor ( session, index, spamtool, usrCallback ) ;
        requestHandler = new RequestFileHandler ( session, Wrapper.DLDIR, nfp, index );
        conMan = new ConnectionManager2 ( session, index, requestHandler, identManager, usrCallback );
        userQueue = new ProcessQueue();


        hasFileCreator = new HasFileCreator ( session, index, spamtool );
        //HH2Session s, Index i, HasFileCreator h, ProcessQueue pq

        shareManager = new ShareManager ( session, requestHandler, index,
                                          hasFileCreator, nfp, userQueue );

        NewPushProcessor pusher = new NewPushProcessor ( index, conMan );
        userQueue.addProcessor ( new NewQueryProcessor ( index ) );
        userQueue.addProcessor ( new NewCommunityProcessor ( session, index, spamtool, usrCallback ) );
        userQueue.addProcessor ( nfp );
        userQueue.addProcessor ( new NewIdentityProcessor ( network, conMan, session,
                                 index, usrCallback, netCallback, conCallback, conMan, requestHandler, spamtool ) );
        userQueue.addProcessor ( new NewMembershipProcessor ( session, index, spamtool, usrCallback ) );
        userQueue.addProcessor ( new NewPostProcessor ( session, index, spamtool, usrCallback ) );

        userQueue.addProcessor ( new NewPrivateMessageProcessor ( session, index, pusher, spamtool, usrCallback ) );
        userQueue.addProcessor ( new NewSubscriptionProcessor ( session, index, spamtool, usrCallback ) );
        userQueue.addProcessor ( new NewSpamExProcessor ( session, index, identManager, usrCallback ) ) ;
        userQueue.addProcessor ( new InSpamExProcessor ( session, index, spamtool, null ) );
        userQueue.addProcessor ( new NewForceSearcher ( index ) );

        userQueue.addProcessor ( new UsrStartDestinationProcessor ( network, conMan, session,
                                 index, usrCallback, netCallback, conCallback, conMan, requestHandler, spamtool ) );
        userQueue.addProcessor ( new UsrReqComProcessor ( identManager ) );
        userQueue.addProcessor ( new UsrReqFileProcessor ( requestHandler, usrCallback ) );
        userQueue.addProcessor ( new UsrReqHasFileProcessor ( identManager ) );
        userQueue.addProcessor ( new UsrReqIdentityProcessor ( identManager ) );
        userQueue.addProcessor ( new UsrReqMemProcessor ( identManager ) );
        userQueue.addProcessor ( new UsrReqPrvMsgProcessor ( session, index ) );
        userQueue.addProcessor ( new UsrReqPostProcessor ( identManager ) );
        userQueue.addProcessor ( new UsrReqSubProcessor ( identManager ) );
        userQueue.addProcessor ( new UsrReqSetRankProcessor ( index, hasFileCreator, usrCallback ) );
        userQueue.addProcessor ( new UsrReqShareProcessor ( shareManager ) );
        userQueue.addProcessor ( new UsrReqSpamExProcessor ( identManager ) );
        userQueue.addProcessor ( new UsrSeed ( session, index, netCallback ) );
        userQueue.addProcessor ( new UsrSeedCommunity ( session, index, spamtool, netCallback ) );
        userQueue.addProcessor ( new UsrCancelFileProcessor ( requestHandler, usrCallback ) );
        userQueue.addProcessor ( pusher );

        doUpdate();
    }

    public HasFileCreator getHasFileCreator()
    {
        return hasFileCreator;
    }

    private void doUpdate()
    {
        requestHandler.setRequestedOn();
    }

    public void startDestinations ( int delay )
    {

        CObjList myids = index.getMyIdentities();

        for ( int c = 0; c < myids.size(); c++ )
        {
            try
            {
                final CObj myid = myids.get ( c ).clone();
                myid.setType ( CObj.USR_START_DEST );
                Timer t = new Timer ( true );
                t.schedule ( new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        enqueue ( myid );
                    }

                }, ( long ) Utils.Random.nextInt ( delay ) );

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        myids.close();

    }

    public void sendRequestsNow()
    {
        conMan.sendRequestsNow();
    }

    public void close()
    {
        shareManager.stop();
        conMan.stop();
        userQueue.stop();
        index.close();
    }

    public void newDeveloperIdentity ( String id )
    {
        identManager.newDeveloperIdentity ( id );
    }

    public void priorityEnqueue ( CObj o )
    {
        userQueue.priorityEnqueue ( o );
    }

    public void enqueue ( CObj o )
    {
        if ( !userQueue.enqueue ( o ) )
        {
            System.out.println ( "WARNING: DATA DROPPED!" );
        }

    }

    public void enqueue ( CObjList o )
    {
        if ( ! userQueue.enqueue ( o ) )
        {
            System.out.println ( "WARNING: DATA DROPPED 2" );
        }

    }

    public Index getIndex()
    {
        return index;
    }

    public ConnectionManager2 getConnectionManager()
    {
        return conMan;
    }

    public HH2Session getSession()
    {
        return session;
    }

    public RequestFileHandler getFileHandler()
    {
        return requestHandler;
    }

    public ShareManager getShareManager()
    {
        return shareManager;
    }

    //public

    public void closeDestinationConnections ( CObj id )
    {
        conMan.closeDestinationConnections ( id );
    }

    public void closeAllConnections()
    {
        conMan.closeAllConnections();
    }

    public Settings getSettings()
    {
        return settings;
    }

    public Net getNet()
    {
        return network;
    }

}
