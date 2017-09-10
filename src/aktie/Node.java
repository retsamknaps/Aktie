package aktie;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.DeveloperIdentity;
import aktie.data.HH2Session;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.index.IndexInterface;
import aktie.net.ConnectionListener;
import aktie.net.ConnectionManager2;
import aktie.net.ConnectionValidatorProcessor;
import aktie.net.DownloadFileProcessor;
import aktie.net.EnqueueRequestProcessor;
import aktie.net.GetSendData2;
import aktie.net.InCheckDigProcessor;
import aktie.net.InCheckMemSubProcessor;
import aktie.net.InComProcessor;
import aktie.net.InDeveloperProcessor;
import aktie.net.InDigProcessor;
import aktie.net.InFileModeProcessor;
import aktie.net.InFileProcessor;
import aktie.net.InFragProcessor;
import aktie.net.InGlbSeqProcessor;
import aktie.net.InHasFileProcessor;
import aktie.net.InIdentityProcessor;
import aktie.net.InMemProcessor;
import aktie.net.InPostProcessor;
import aktie.net.InPrvIdentProcessor;
import aktie.net.InPrvMsgProcessor;
import aktie.net.InSpamExProcessor;
import aktie.net.InSubProcessor;
import aktie.net.Net;
import aktie.net.ReqComProcessor;
import aktie.net.ReqDigProcessor;
import aktie.net.ReqFragListProcessor;
import aktie.net.ReqFragProcessor;
import aktie.net.ReqGlobalSeq;
import aktie.net.ReqHasFileProcessor;
import aktie.net.ReqIdentProcessor;
import aktie.net.ReqMemProcessor;
import aktie.net.ReqPostsProcessor;
import aktie.net.ReqPrvIdentProcessor;
import aktie.net.ReqPrvMsgProcessor;
import aktie.net.ReqSpamExProcessor;
import aktie.net.ReqSubProcessor;
import aktie.spam.SpamTool;
import aktie.upgrade.NodeUpgrader;
import aktie.upgrade.UpgradeControllerCallback;
import aktie.user.IdentityManager;
import aktie.user.NewCommunityProcessor;
import aktie.user.NewDeveloperProcessor;
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
import aktie.user.RequestFileHandlerInterface;
import aktie.user.ShareManager;
import aktie.user.ShareManagerInterface;
import aktie.user.UsrCancelFileProcessor;
import aktie.user.UsrReqComProcessor;
import aktie.user.UsrReqFileProcessor;
import aktie.user.UsrReqSetRankProcessor;
import aktie.user.UsrReqShareProcessor;
import aktie.user.UsrReqSpamExProcessor;
import aktie.user.UsrSeed;
import aktie.user.UsrSeedCommunity;
import aktie.user.UsrStartDestinationProcessor;
import aktie.utils.HasFileCreator;

public class Node implements NodeInterface
{

    private Net network;
    private Index index;
    private HH2Session session;
    private ProcessQueue userQueue;
    private ProcessQueue dlQueue;
    private ProcessQueue preprocProcessor;
    private ProcessQueue inProcessor;
    private IdentityManager identManager;
    private UpdateDispatcher usrCallback;
    private UpdateDispatcher netCallback;
    private ConnectionListener conCallback;
    private ConnectionManager2 conMan;
    private RequestFileHandler requestHandler;
    private ShareManager shareManager;
    private Settings settings;
    private HasFileCreator hasFileCreator;
    private SpamTool spamtool;
    private File tmpDir;
    private NodeUpgrader upgrader;

    public Node ( String nodedir, Net net, UpdateCallback uc,
                  UpdateCallback nc, ConnectionListener cc,
                  UpgradeControllerCallback ug ) throws IOException
    {
        usrCallback = new UpdateDispatcher(); // uc;
        netCallback = new UpdateDispatcher(); // nc;
        usrCallback.addUpdateListener ( uc );
        netCallback.addUpdateListener ( nc );
        upgrader = new NodeUpgrader ( this, nodedir, ug );
        usrCallback.addUpdateListener ( upgrader );
        netCallback.addUpdateListener ( upgrader );
        conCallback = cc;
        network = net;
        settings = new Settings ( nodedir );
        File idxdir = new File ( nodedir + File.separator + "index" );
        tmpDir = new File ( nodedir + File.separator + "tmp" );
        tmpDir.mkdirs();
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
        userQueue = new ProcessQueue ( "userQueue" );


        hasFileCreator = new HasFileCreator ( session, index, spamtool );
        //HH2Session s, Index i, HasFileCreator h, ProcessQueue pq

        shareManager = new ShareManager ( session, requestHandler, index,
                                          hasFileCreator, nfp, userQueue );

        dlQueue = new ProcessQueue ( "downloadQueue" );
        dlQueue.addProcessor ( new DownloadFileProcessor ( hasFileCreator ) );



        preprocProcessor = new ProcessQueue ( "Network preprocProcessor" );
        setupPreprocQueue ( preprocProcessor, session,
                            index, identManager, spamtool,
                            hasFileCreator, conMan );

        //These process requests from the other node.
        inProcessor = new ProcessQueue ( "Network inProcessor" );
        setupInputQueue ( inProcessor, index, identManager );

        NewPushProcessor pusher = new NewPushProcessor ( index, conMan );
        userQueue.addProcessor ( new NewQueryProcessor ( index ) );
        userQueue.addProcessor ( new NewCommunityProcessor ( session, conMan, index, spamtool, usrCallback ) );
        userQueue.addProcessor ( nfp );
        NewIdentityProcessor nip = new NewIdentityProcessor ( network, conMan, session,
                index, usrCallback, netCallback, conCallback, conMan, requestHandler, spamtool,
                preprocProcessor, inProcessor, dlQueue );
        nip.setTmpDir ( tmpDir );
        userQueue.addProcessor ( nip );
        userQueue.addProcessor ( new NewMembershipProcessor ( session, conMan, index, spamtool, usrCallback ) );
        userQueue.addProcessor ( new NewPostProcessor ( session, index, spamtool, usrCallback ) );

        userQueue.addProcessor ( new NewPrivateMessageProcessor ( session, index, pusher, spamtool, usrCallback ) );
        userQueue.addProcessor ( new NewSubscriptionProcessor ( session, conMan, index, spamtool, usrCallback ) );
        userQueue.addProcessor ( new NewSpamExProcessor ( session, index, identManager, usrCallback ) ) ;
        userQueue.addProcessor ( new NewDeveloperProcessor ( index, identManager, usrCallback ) );
        userQueue.addProcessor ( new InSpamExProcessor ( session, index, spamtool, null ) );
        userQueue.addProcessor ( new NewForceSearcher ( index ) );

        UsrStartDestinationProcessor usdp = new UsrStartDestinationProcessor ( network, conMan, session,
                index, usrCallback, netCallback, conCallback, conMan, requestHandler, spamtool,
                preprocProcessor, inProcessor, dlQueue );
        usdp.setTmpDir ( tmpDir );
        userQueue.addProcessor ( usdp );
        userQueue.addProcessor ( new UsrReqComProcessor ( identManager, index ) );
        userQueue.addProcessor ( new UsrReqFileProcessor ( requestHandler, usrCallback ) );
        //userQueue.addProcessor ( new UsrReqHasFileProcessor ( identManager ) );
        //userQueue.addProcessor ( new UsrReqIdentityProcessor ( identManager ) );
        //userQueue.addProcessor ( new UsrReqMemProcessor ( identManager ) );
        //userQueue.addProcessor ( new UsrReqPrvMsgProcessor ( session, index ) );
        //userQueue.addProcessor ( new UsrReqPostProcessor ( identManager ) );
        //userQueue.addProcessor ( new UsrReqSubProcessor ( identManager ) );
        userQueue.addProcessor ( new UsrReqSetRankProcessor ( index, hasFileCreator, usrCallback ) );
        userQueue.addProcessor ( new UsrReqShareProcessor ( shareManager ) );
        userQueue.addProcessor ( new UsrReqSpamExProcessor ( identManager ) );
        userQueue.addProcessor ( new UsrSeed ( session, index, netCallback ) );
        userQueue.addProcessor ( new UsrSeedCommunity ( session, index, spamtool, netCallback ) );
        userQueue.addProcessor ( new UsrCancelFileProcessor ( requestHandler, usrCallback ) );
        userQueue.addProcessor ( pusher );

        doUpdate();
    }

    public static void setupInputQueue ( ProcessQueue inProcessor, Index index, IdentityManager identManager )
    {
        inProcessor.addProcessor ( new ReqGlobalSeq ( index, identManager ) );
        inProcessor.addProcessor ( new ReqDigProcessor ( index ) );
        inProcessor.addProcessor ( new ReqIdentProcessor ( index ) );
        inProcessor.addProcessor ( new ReqFragListProcessor ( index ) );
        inProcessor.addProcessor ( new ReqFragProcessor ( index ) );
        inProcessor.addProcessor ( new ReqComProcessor ( index ) );
        inProcessor.addProcessor ( new ReqPrvIdentProcessor ( index ) );
        inProcessor.addProcessor ( new ReqPrvMsgProcessor ( index ) );
        inProcessor.addProcessor ( new ReqHasFileProcessor ( index ) );
        inProcessor.addProcessor ( new ReqMemProcessor ( index ) );
        inProcessor.addProcessor ( new ReqPostsProcessor ( index ) );
        inProcessor.addProcessor ( new ReqSubProcessor ( index ) );
        inProcessor.addProcessor ( new ReqSpamExProcessor ( index ) );

    }

    public static void setupPreprocQueue ( ProcessQueue preprocProcessor, HH2Session session,
                                           Index index, IdentityManager identManager, SpamTool spamtool,
                                           HasFileCreator hasFileCreator, GetSendData2 conMan )
    {
        InIdentityProcessor ip = new InIdentityProcessor ( session, index, identManager );
        preprocProcessor.addProcessor ( new ConnectionValidatorProcessor ( ip ) );
        //!!!!!!!!!!!!!!!!!! InFragProcessor - should be first !!!!!!!!!!!!!!!!!!!!!!!
        //Otherwise the list of fragments will be interate though for preceding processors
        //that don't need to and time will be wasted.
        preprocProcessor.addProcessor ( new InFragProcessor ( session, index ) );
        preprocProcessor.addProcessor ( new InCheckDigProcessor ( index ) );
        preprocProcessor.addProcessor ( new InFileModeProcessor ( ) );
        preprocProcessor.addProcessor ( ip );
        preprocProcessor.addProcessor ( new InFileProcessor ( ) );
        preprocProcessor.addProcessor ( new InDigProcessor ( index ) );
        preprocProcessor.addProcessor ( new InCheckMemSubProcessor ( ) );
        preprocProcessor.addProcessor ( new InGlbSeqProcessor ( ) );
        preprocProcessor.addProcessor ( new InComProcessor ( session, index, spamtool, identManager ) );
        preprocProcessor.addProcessor ( new InHasFileProcessor ( session, index, identManager, hasFileCreator, spamtool ) );
        preprocProcessor.addProcessor ( new InPrvIdentProcessor ( session, index, spamtool, identManager ) );
        preprocProcessor.addProcessor ( new InPrvMsgProcessor ( session, index, spamtool, identManager ) );
        preprocProcessor.addProcessor ( new InMemProcessor ( session, index, spamtool, identManager ) );
        preprocProcessor.addProcessor ( new InPostProcessor ( session, index, spamtool, identManager ) );
        preprocProcessor.addProcessor ( new InSubProcessor ( session, conMan, index, spamtool, identManager ) );
        preprocProcessor.addProcessor ( new InSpamExProcessor ( session, index, spamtool, identManager ) );
        preprocProcessor.addProcessor ( new InDeveloperProcessor ( index, spamtool, identManager ) );
        //!!!!!!!!!!!!!!!!! EnqueueRequestProcessor - must be last !!!!!!!!!!!!!!!!!!!!
        //Otherwise requests from the other node will not be processed.
        preprocProcessor.addProcessor ( new EnqueueRequestProcessor ( ) );

    }

    public NodeUpgrader getUpgrader()
    {
        return upgrader;
    }

    public UpdateCallback getUsrCallback()
    {
        return usrCallback;
    }

    public UpdateCallback getNetCallback()
    {
        return netCallback;
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

    public void close()
    {
        shareManager.stop();
        conMan.stop();
        dlQueue.stop();
        preprocProcessor.stop();
        inProcessor.stop();
        userQueue.stop();
        index.close();
        session.close();
    }

    public void newDeveloperIdentity ( String id )
    {
        identManager.newDeveloperIdentity ( id );
    }

    public DeveloperIdentity getDeveloper ( String id )
    {
        return identManager.getDeveloperIdentity ( id );
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

    public SpamTool getSpamTool()
    {
        return spamtool;
    }

    public IndexInterface getIndex()
    {
        return index;
    }

    public GetSendData2 getConnectionManager()
    {
        return conMan;
    }

    public RequestFileHandlerInterface getFileHandler()
    {
        return requestHandler;
    }

    public ShareManagerInterface getShareManager()
    {
        return shareManager;
    }

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
