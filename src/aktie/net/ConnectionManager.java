package aktie.net;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.CommunityMyMember;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.data.RequestFile;
import aktie.gui.GuiCallback;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.user.IdentityManager;
import aktie.user.PushInterface;
import aktie.user.RequestFileHandler;
import aktie.utils.MembershipValidator;
import aktie.utils.SymDecoder;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bouncycastle.crypto.params.KeyParameter;

public class ConnectionManager implements GetSendData, DestinationListener, PushInterface, Runnable
{

    Logger log = Logger.getLogger ( "aktie" );

    private Map<String, DestinationThread> destinations;
    private Index index;
    private RequestFileHandler fileHandler;
    private IdentityManager identManager;
    private SymDecoder symdec;
    private MembershipValidator memvalid;
    private boolean stop;
    private GuiCallback callback;

    public static int MAX_SERVICING_CONNECTIONS = 10;
    public static int MAX_TOTAL_DEST_CONNECTIONS = 100;
    public static long MIN_TIME_BETWEEN_CONNECTIONS = 5L * 60L * 1000L;
    //This value must be longer than the update period so we keep connections open
    //long enough to make requests.
    //!!!!!!!!!! DO NOT MAKE LESS THAN 10 MINUTES OR YOU BROKE THE UPDATE PERIOD NEGATIVE
    public static long MAX_TIME_WITH_NO_REQUESTS = 60L * 60L * 1000L;
    public static long MAX_CONNECTION_TIME  = 2L * 60L * 60L * 1000L; //Only keep connections for 2 hours
    public static long DECODE_AND_NEW_CONNECTION_DELAY = 5L * 60L * 1000L;
    //Don't send the same request to the same node within this period.
    public static int NO_REREQUEST_CYCLES = 2;

    public static int MAX_PUSH_LOOPS = 6; //The number of times we attempt to connect to a node to push to
    public static int PUSH_NODES = 5;  //The number of nodes we'd like to push to
    public static int PUSH_CONNECTS = 5; //The number of nodes we attempt to connect to for a push at one time

    public Map<String, Set<String>> pushMap;
    public Map<String, Integer> pushLoops;

    public ConnectionManager ( HH2Session s, Index i, RequestFileHandler r, IdentityManager id,
                               GuiCallback cb )
    {
        callback = cb;
        identManager = id;
        index = i;
        pushMap = new HashMap<String, Set<String>>();
        pushLoops = new HashMap<String, Integer>();
        destinations = new HashMap<String, DestinationThread>();
        fileHandler = r;
        symdec = new SymDecoder();
        memvalid = new MembershipValidator ( index );
        Thread t = new Thread ( this );
        t.setDaemon ( true );
        t.start();
    }

    public List<DestinationThread> getDestList()
    {
        List<DestinationThread> r = new LinkedList<DestinationThread>();

        synchronized ( destinations )
        {
            r.addAll ( destinations.values() );
        }

        return r;
    }

    public void addDestination ( DestinationThread d )
    {
        synchronized ( destinations )
        {
            destinations.put ( d.getDest().getPublicDestinationInfo(), d );
        }

    }

    @Override
    public boolean isDestinationOpen ( String dest )
    {
        boolean opn = true;

        synchronized ( destinations )
        {
            opn = destinations.containsKey ( dest );
        }

        return opn;
    }

    @Override
    public void closeDestination ( CObj myid )
    {
        String dest = myid.getString ( CObj.DEST );

        if ( dest != null )
        {
            synchronized ( destinations )
            {
                DestinationThread dt = destinations.remove ( dest );

                if ( dt != null )
                {
                    dt.stop();
                }

            }

        }

    }

    public void sendRequestsNow()
    {

        List<DestinationThread> dlst = new LinkedList<DestinationThread>();

        synchronized ( destinations )
        {
            dlst.addAll ( destinations.values() );
        }

        for ( DestinationThread dt : dlst )
        {
            dt.poke();
        }

    }

    @Override
    public void push ( CObj fromid, CObj o )
    {
        String dest = fromid.getString ( CObj.DEST );
        DestinationThread dt = null;

        if ( dest != null )
        {
            synchronized ( destinations )
            {
                dt = destinations.get ( dest );
            }

        }

        if ( dt != null )
        {
            dt.send ( o );
        }

    }

    @Override
    public List<String> getConnectedIds ( CObj fromid )
    {
        List<String> conids = null;

        if ( fromid != null )
        {
            String dest = fromid.getString ( CObj.DEST );

            synchronized ( destinations )
            {
                DestinationThread d = destinations.get ( dest );

                if ( d != null )
                {
                    conids = d.getConnectedIds();
                }

            }

        }

        if ( conids == null )
        {
            conids = new LinkedList<String>();
        }

        return conids;
    }

    @Override
    public void push ( CObj fromid, String to, CObj o )
    {
        String dest = fromid.getString ( CObj.DEST );
        DestinationThread dt = null;

        synchronized ( destinations )
        {
            dt = destinations.get ( dest );
        }

        if ( dt != null )
        {
            dt.send ( to, o );
        }

    }

    public void closeDestinationConnections ( CObj id )
    {
        synchronized ( destinations )
        {
            DestinationThread dt = destinations.get ( id.getString ( CObj.DEST ) );
            dt.closeConnections();
        }

    }

    public void closeAllConnections()
    {
        synchronized ( destinations )
        {
            for ( DestinationThread dt : destinations.values() )
            {
                dt.closeConnections();
            }

        }

    }

    private Map<String, CObj> getMyIdMap()
    {
        List<CObj> myidlst = Index.list ( index.getMyIdentities() );
        Map<String, CObj> myidmap = new HashMap<String, CObj>();

        for ( CObj co : myidlst )
        {
            myidmap.put ( co.getId(), co );
        }

        return myidmap;
    }

    private List<DestinationThread> findMyDestinationsForCommunity ( Map<String, CObj> myidmap, String comid )
    {
        List<CObj> mysubslist = Index.list ( index.getMySubscriptions ( comid ) );
        //Find my destinations for my subscriptions to this community
        List<DestinationThread> dlst = new LinkedList<DestinationThread>();

        synchronized ( destinations )
        {
            for ( CObj c : mysubslist )
            {
                CObj mid = myidmap.get ( c.getString ( CObj.CREATOR ) );

                if ( mid != null )
                {
                    String mydest = mid.getString ( CObj.DEST );

                    if ( mydest != null )
                    {
                        DestinationThread dt = destinations.get ( mydest );

                        if ( dt != null )
                        {
                            dlst.add ( dt );
                        }

                    }

                }

            }

        }

        return dlst;
    }

    private List<DestinationThread> findAllMyDestinationsForCommunity ( Map<String, CObj> myidmap, String comid )
    {
        List<DestinationThread> dlst = new LinkedList<DestinationThread>();
        CObj com = index.getCommunity ( comid );

        if ( com != null )
        {
            if ( CObj.SCOPE_PUBLIC.equals ( com.getString ( CObj.SCOPE ) ) )
            {
                synchronized ( destinations )
                {
                    dlst.addAll ( destinations.values() );
                }

            }

            else
            {
                List<CObj> mymemlist = Index.list ( index.getMyMemberships ( comid ) );

                //Find my destinations for my subscriptions to this community
                synchronized ( destinations )
                {
                    for ( CObj c : mymemlist )
                    {
                        CObj mid = myidmap.get ( c.getPrivate ( CObj.MEMBERID ) );

                        if ( mid != null )
                        {
                            String mydest = mid.getString ( CObj.DEST );

                            if ( mydest != null )
                            {
                                DestinationThread dt = destinations.get ( mydest );

                                if ( dt != null )
                                {
                                    dlst.add ( dt );
                                }

                            }

                        }

                    }

                    if ( "true".equals ( com.getPrivate ( CObj.MINE ) ) )
                    {
                        String creator = com.getString ( CObj.CREATOR );
                        CObj mid = myidmap.get ( creator );

                        if ( mid != null )
                        {
                            String mydest = mid.getString ( CObj.DEST );

                            if ( mydest != null )
                            {
                                DestinationThread dt = destinations.get ( mydest );

                                if ( dt != null && !dlst.contains ( dt ) )
                                {
                                    dlst.add ( dt );
                                }

                            }

                        }

                    }

                }

            }

        }

        return dlst;
    }

    private void attemptOneConnection ( DestinationThread dt, List<String> idlst, Map<String, CObj> myids, boolean filemode )
    {
        IdentityData idat = null;
        long curtime = ( new Date() ).getTime();
        long soonest = curtime - MIN_TIME_BETWEEN_CONNECTIONS;
        //Remove my ids from the list
        Iterator<String> i = idlst.iterator();

        while ( i.hasNext() )
        {
            String id = i.next();

            if ( myids.get ( id ) != null )
            {
                log.info ( "CONMAN: remove id that is mine from connect list." );
                i.remove();
            }

            else
            {
                IdentityData tid = identManager.getIdentity ( id );

                if ( tid != null )
                {
                    if ( tid.getLastConnectionAttempt() > soonest )
                    {
                        log.info ( "CONMAN: we tried too soon ago. " + id );
                        i.remove();
                    }

                }

            }

        }

        log.info ( "CONMAN: Number left to pick from: " + idlst.size() );

        if ( idlst.size() > 0 )
        {
            String pid = idlst.get ( Utils.Random.nextInt ( idlst.size() ) );
            idat = identManager.getIdentity ( pid );
        }

        log.info ( "CONMAN: selected node for new connection: " + idat );

        if ( idat != null )
        {
            CObj tid = index.getIdentity ( idat.getId() );

            if ( tid != null )
            {
                identManager.connectionAttempted ( idat.getId() );
                log.info ( "CONMAN: attempt new connection " + tid.getDisplayName() );
                dt.connect ( tid.getString ( CObj.DEST ), filemode );
            }

        }

    }

    /*
        hlst is a list of CObj's with CREATOR set to identies we
        could connect to
    */
    private void attemptDestinationConnection ( CObjList hlst, DestinationThread dt, Map<String, CObj> myids, boolean filemode )
    {
        //See how many of these nodes we're connected to
        List<String> idlst = new LinkedList<String>();
        int connected = 0;

        for ( int c = 0; c < hlst.size(); c++ )
        {
            try
            {
                CObj co = hlst.get ( c );
                String cr = null;

                if ( CObj.MEMBERSHIP.equals ( co.getType() ) )
                {
                    cr = co.getPrivate ( CObj.MEMBERID );
                }

                else if ( CObj.IDENTITY.equals ( co.getType() ) )
                {
                    cr = co.getId();
                }

                else
                {
                    cr = co.getString ( CObj.CREATOR );
                }

                log.info ( "CONMAN: attempt connection add node: " + cr );

                if ( dt.isConnected ( cr, filemode ) )
                {
                    log.info ( "CONMAN: alrady connected." );
                    connected++;
                }

                else
                {

                    if ( cr != null )
                    {
                        idlst.add ( cr );
                    }

                }

            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        hlst.close();
        log.info ( "CONMAN existing connections valid for request: " + connected  + " number to pick from: " + idlst.size() );

        if ( connected < MAX_SERVICING_CONNECTIONS && idlst.size() > 0 )
        {
            attemptOneConnection ( dt, idlst, myids, filemode );
        }

    }

    private void checkConnections()
    {
        //Remove pushes that are done.
        Set<String> digs = new HashSet<String>();

        synchronized ( pushMap )
        {
            digs.addAll ( pushMap.keySet() );
        }

        digs.addAll ( pushLoops.keySet() );

        for ( String d : digs )
        {
            CObj o = index.getByDig ( d );

            if ( o != null )
            {
                String pushstr = o.getPrivate ( CObj.PRV_PUSH_REQ );

                if ( pushstr == null || "false".equals ( pushstr ) )
                {
                    pushLoops.remove ( d );

                    synchronized ( pushMap )
                    {
                        pushMap.remove ( d );
                    }

                }

            }

        }

        Map<String, CObj> mymap = getMyIdMap();

        //Clean up push lists to free memory

        //Find things to push
        CObjList clst = index.getPushesToConnect();
        int pushcons = 0;
        log.info ( "CONMAN: Items ready to push: " + clst.size() );
        log.info ( "CONMAN BLARG00000" );

        for ( int c = 0; c < clst.size() && pushcons < PUSH_CONNECTS; c++ )
        {
            log.info ( "CONMAN BLARG00001" );

            try
            {
                CObj b = clst.get ( c );
                String err = b.getString ( CObj.ERROR );
                String creator = b.getString ( CObj.CREATOR );
                String type = b.getType();

                if ( err == null && creator != null )
                {
                    DestinationThread dt = null;

                    synchronized ( destinations )
                    {
                        dt = destinations.get ( mymap.get ( creator ).getString ( CObj.DEST ) );
                    }

                    if ( dt != null && dt.numberConnection() < MAX_TOTAL_DEST_CONNECTIONS )
                    {
                        if ( CObj.HASFILE.equals ( type ) || CObj.POST.equals ( type ) )
                        {
                            String comid = b.getString ( CObj.COMMUNITYID );
                            CObjList hlst = index.getSubscriptions ( comid, null );
                            log.info ( "CONMAN: Attempt connection for push hasfile/post " + hlst.size() );
                            attemptDestinationConnection ( hlst, dt, mymap, false );
                            pushcons++;
                        }

                        else if ( CObj.SUBSCRIPTION.equals ( type ) )
                        {
                            String comid = b.getString ( CObj.COMMUNITYID );

                            if ( comid != null )
                            {
                                CObj com = index.getCommunity ( comid );

                                if ( com != null )
                                {
                                    //if public it do not matter
                                    if ( CObj.SCOPE_PRIVATE.equals ( com.getString ( CObj.SCOPE ) ) )
                                    {
                                        CObjList mems = index.getMemberships ( comid, null );
                                        mems.add ( com ); //Add creator
                                        attemptDestinationConnection ( mems, dt, mymap, false );
                                        pushcons++;
                                    }

                                }

                            }

                        }

                        else
                        {
                            //Bellow we attempt to connect to most reliable anyway
                        }

                    }

                    Integer curloops = pushLoops.get ( b.getDig() );

                    if ( curloops == null )
                    {
                        curloops = 0;
                    }

                    pushLoops.put ( b.getDig(), curloops + 1 );

                    if ( curloops >= MAX_PUSH_LOOPS )
                    {
                        //No longer attempt to connect to a node to push to,
                        //but make pushable if we happen to get a connection we can push to
                        b.pushPrivate ( CObj.PRV_PUSH_REQ, "nocon" );
                        index.index ( b );
                        pushLoops.remove ( b.getDig() );
                    }

                }

                else
                {
                    b.pushPrivate ( CObj.PRV_PUSH_REQ, "false" );
                    index.index ( b );
                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        clst.close();

        //Find the current file requests we have
        List<RequestFile> rl = fileHandler.listRequestFilesNE ( RequestFile.COMPLETE, Integer.MAX_VALUE );
        log.info ( "CONMAN: Found files to request: " + rl.size() );
        log.info ( "CONMAN BLARG00002" );

        for ( RequestFile rf : rl )
        {

            log.info ( "CONMAN: Check for file: " + rf.getLocalFile() );
            //Get the DestinationThread that matches the requesting id.
            DestinationThread dt = null;

            synchronized ( destinations )
            {
                dt = destinations.get ( mymap.get ( rf.getRequestId() ).getString ( CObj.DEST ) );
            }

            log.info ( "CONMAN: Found destination " + dt );

            if ( dt != null )
            {
                CObj did = dt.getIdentity();

                if ( did != null )
                {
                    log.info ( "CONMAN: Dest id : " + did.getDisplayName() );
                }

                else
                {
                    log.info ( "CONMAN: Dest identity is null" );
                }

                log.info ( "CONMAN: connections: " + dt.numberConnection() );

                if ( dt.numberConnection() < MAX_TOTAL_DEST_CONNECTIONS )
                {
                    CObjList hlst = index.getHasFiles ( rf.getCommunityId(),
                                                        rf.getWholeDigest(), rf.getFragmentDigest() );
                    log.info ( "CONMAN: number has file: " + hlst.size() );
                    attemptDestinationConnection ( hlst, dt, mymap, true );
                }

            }

        }

        log.info ( "CONMAN BLARG000003" );
        //--- only members ---
        //Check hasfile requests (always initiate connections for highest prioty no
        //mater what.  If user is only working in one community don't waste connections
        //for other subscriptions.
        List<CommunityMember> reqfilelist = identManager.nextHasFileUpdate ( 20 );

        for ( CommunityMember cm : reqfilelist )
        {
            List<DestinationThread> destlist = findMyDestinationsForCommunity ( mymap, cm.getCommunityId() );

            log.info ( "CONMAN: My destinations for community: " + cm.getCommunityId() + " # " + destlist.size() );

            for ( DestinationThread dt : destlist )
            {
                if ( dt.numberConnection() < MAX_TOTAL_DEST_CONNECTIONS )
                {
                    CObjList hlst = index.getSubscriptions ( cm.getCommunityId(), null );
                    log.info ( "CONMAN: Attempt connection for has_file " + hlst.size() );
                    //See how many of these nodes we're connected to
                    attemptDestinationConnection ( hlst, dt, mymap, false );
                }

            }

        }

        log.info ( "CONMAN BLARG00004" );
        //Check post requests
        List<CommunityMember> reqpostlist = identManager.nextHasPostUpdate ( 20 );

        for ( CommunityMember cm : reqpostlist )
        {
            List<DestinationThread> destlist = findMyDestinationsForCommunity ( mymap, cm.getCommunityId() );

            for ( DestinationThread dt : destlist )
            {
                if ( dt.numberConnection() < MAX_TOTAL_DEST_CONNECTIONS )
                {
                    CObjList hlst = index.getSubscriptions ( cm.getCommunityId(), null );
                    //See how many of these nodes we're connected to
                    attemptDestinationConnection ( hlst, dt, mymap, false );
                }

            }

        }

        log.info ( "CONMAN BLARG000005" );
        //Check subscription requests
        List<CommunityMember> reqsublist = identManager.nextHasSubscriptionUpdate ( 20 );
        log.info ( "subscription updates: " + reqsublist.size() );

        for ( CommunityMember cm : reqsublist )
        {
            List<DestinationThread> destlist = findAllMyDestinationsForCommunity ( mymap, cm.getCommunityId() );
            log.info ( "subscription update destinations: " + destlist.size() );

            for ( DestinationThread dt : destlist )
            {
                if ( dt.numberConnection() < MAX_TOTAL_DEST_CONNECTIONS )
                {
                    CObj com = index.getCommunity ( cm.getCommunityId() );

                    if ( CObj.SCOPE_PRIVATE.equals ( com.getString ( CObj.SCOPE ) ) )
                    {
                        CObjList hlst = index.getMemberships ( cm.getCommunityId(), null );
                        hlst.add ( com ); //attempt to connect to the community creator too
                        log.info ( "subscription update nodes to try: " + hlst.size() );
                        //See how many of these nodes we're connected to
                        attemptDestinationConnection ( hlst, dt, mymap, false );
                    }

                    else
                    {
                        CObjList hlst = index.getIdentities();
                        attemptDestinationConnection ( hlst, dt, mymap, false );
                    }

                }

            }

        }

        log.info ( "CONMAN BLARG000006" );
        //Connect to the most reliable
        List<IdentityData> mrel = identManager.listMostReliable ( 100 );
        List<DestinationThread> dlst = getDestList();

        //Remove any node we're allready connected to from any destination
        for ( DestinationThread dt : dlst )
        {
            Iterator<IdentityData> i = mrel.iterator();

            while ( i.hasNext() )
            {
                IdentityData id = i.next();

                if ( dt.isConnected ( id.getId(), false ) )
                {
                    i.remove();
                }

            }

        }

        log.info ( "CONMAN BLARG000007" );
        List<String> ids = new LinkedList<String>();

        for ( IdentityData id : mrel )
        {
            ids.add ( id.getId() );
        }

        for ( DestinationThread dt : dlst )
        {
            log.info ( "CONMAN BLARG000008" );

            if ( dt.numberConnection() < MAX_TOTAL_DEST_CONNECTIONS )
            {
                attemptOneConnection ( dt, ids, mymap, false );
            }

        }

        log.info ( "CONMAN BLARG000009" );

    }

    private boolean alreadyPushedTo ( String remotedest, String dig )
    {
        synchronized ( pushMap )
        {
            Set<String> mset = pushMap.get ( dig );

            if ( mset == null )
            {
                mset = new HashSet<String>();
                pushMap.put ( dig, mset );
            }

            if ( mset.contains ( remotedest ) )
            {
                return true;
            }

            else
            {
                mset.add ( remotedest );
                return false;
            }

        }

    }

    private Object findNext ( String localdest, String remotedest, Map<String, Integer> comlist, long rdy, boolean filemode )
    {
        Object r = null;

        log.info ( "READY FOR NEXT: " + localdest + " to " + remotedest + " RDY: " + rdy + " match com: " + comlist.size() );

        //Find things we want to push
        if ( !filemode )
        {
            CObjList pushlst = index.getPushesToSend();
            log.info ( "CONTHREAD: Ready to send pushes: " + localdest + " to " + remotedest + " " + pushlst.size() );

            for ( int c = 0; c < pushlst.size() && r == null; c++ )
            {
                try
                {
                    CObj p = pushlst.get ( c );
                    int max = Integer.MAX_VALUE;
                    String type = p.getType();

                    if ( CObj.POST.equals ( type ) || CObj.HASFILE.equals ( type ) )
                    {
                        String comid = p.getString ( CObj.COMMUNITYID );

                        if ( comlist.containsKey ( comid ) )
                        {
                            if ( !alreadyPushedTo ( remotedest, p.getDig() ) )
                            {
                                r = p;
                                max = comlist.get ( comid );
                                log.info ( "CONTHREAD: 1 push ok, " + localdest + " to " + remotedest + " max: " + max );
                            }

                        }

                    }

                    else if ( CObj.SUBSCRIPTION.equals ( type ) )
                    {
                        String comid = p.getString ( CObj.COMMUNITYID );

                        if ( comid != null )
                        {
                            CObj com = index.getCommunity ( comid );

                            if ( com != null )
                            {
                                if ( CObj.SCOPE_PUBLIC.equals ( com.getString ( CObj.SCOPE ) ) )
                                {
                                    if ( !alreadyPushedTo ( remotedest, p.getDig() ) )
                                    {
                                        r = p;
                                        log.info ( "CONTHREAD: 2 push ok, " + localdest + " to " + remotedest + " max: " + max );
                                    }

                                }

                                else
                                {
                                    if ( remotedest.equals ( com.getString ( CObj.CREATOR ) ) )
                                    {
                                        if ( !alreadyPushedTo ( remotedest, p.getDig() ) )
                                        {
                                            r = p;
                                            log.info ( "CONTHREAD: 3 push ok, " + localdest + " to " + remotedest + " max: " + max );
                                        }

                                    }

                                    CObjList memlst = index.getMembership ( comid, remotedest );

                                    if ( memlst.size() > 0 )
                                    {
                                        if ( !alreadyPushedTo ( remotedest, p.getDig() ) )
                                        {
                                            max = memlst.size() + 1;
                                            r = p;
                                            log.info ( "CONTHREAD: 4 push ok, " + localdest + " to " + remotedest + " max: " + max );
                                        }

                                    }

                                    memlst.close();

                                }

                            }

                        }

                    }

                    else
                    {
                        //Anyone can do it.
                        if ( !alreadyPushedTo ( remotedest, p.getDig() ) )
                        {
                            r = p;
                        }

                    }

                    //See if we already pushed to as many as we can
                    if ( r != null )
                    {
                        boolean maxreached = false;

                        synchronized ( pushMap )
                        {
                            Set<String> pushset = pushMap.get ( p.getDig() );

                            if ( pushset != null )
                            {
                                if ( pushset.size() >= max || pushset.size() >= PUSH_NODES )
                                {
                                    maxreached = true;
                                }

                            }

                        }

                        if ( maxreached )
                        {
                            p.pushPrivate ( CObj.PRV_PUSH_REQ, "false" );
                            index.index ( p );
                        }

                    }

                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            pushlst.close();
        }

        //get files if we want them
        if ( ( rdy == 0 && r == null ) || filemode )
        {
            List<RequestFile> rflst = fileHandler.findFileListFrags ( localdest, 60L * 60L * 1000L );
            log.info ( "CONMAN: Requests for fragment list: " + rflst.size() );
            Iterator<RequestFile> it = rflst.iterator();

            while ( it.hasNext() && r == null )
            {
                RequestFile rf = it.next();
                CObj nhf = index.getIdentHasFile ( rf.getCommunityId(), remotedest,
                                                   rf.getWholeDigest(), rf.getFragmentDigest() );

                if ( nhf != null && "true".equals ( nhf.getString ( CObj.STILLHASFILE ) ) )
                {
                    if ( fileHandler.claimFileListClaim ( rf ) )
                    {
                        CObj cr = new CObj();
                        cr.setType ( CObj.CON_REQ_FRAGLIST );
                        cr.pushString ( CObj.COMMUNITYID, rf.getCommunityId() );
                        cr.pushString ( CObj.FILEDIGEST, rf.getWholeDigest() );
                        cr.pushString ( CObj.FRAGDIGEST, rf.getFragmentDigest() );
                        r = cr;
                    }

                }

            }

            if ( r == null )
            {
                //First find the highest priority file we're tryiing to get
                //from the communities this node is subscribed to.
                List<RequestFile> rlst = fileHandler.findFileToGetFrags ( localdest );

                log.info ( "CONMAN: files to request frag: " + rlst.size() );

                it = rlst.iterator();

                while ( it.hasNext() && r == null )
                {
                    //See if the remote dest has the file.
                    RequestFile rf = it.next();
                    CObj nhf = index.getIdentHasFile ( rf.getCommunityId(), remotedest,
                                                       rf.getWholeDigest(), rf.getFragmentDigest() );

                    if ( nhf != null && "true".equals ( nhf.getString ( CObj.STILLHASFILE ) ) )
                    {
                        //Find the fragments that haven't been requested yet.
                        CObjList cl = index.getFragmentsToRequest ( rf.getCommunityId(),
                                      rf.getWholeDigest(), rf.getFragmentDigest() );

                        log.info ( "CONMAN: fragments to request: " + cl.size() + " file: " + rf.getLocalFile() );

                        if ( cl.size() == 0 )
                        {
                            //If there are no fragments that have not be requested yet,
                            //then let's reset the ones that in the req status, and not
                            //complete, in case we just failed to get it back after
                            //requesting.
                            cl.close();
                            cl = index.getFragmentsToReset ( rf.getCommunityId(),
                                                             rf.getWholeDigest(), rf.getFragmentDigest() );

                            log.info ( "CONMAN: resetting fragments: " + cl.size() + " file: " + rf.getLocalFile() );

                            long backtime = System.currentTimeMillis() - 10L * 60L * 1000L;

                            for ( int c = 0; c < cl.size(); c++ )
                            {
                                try
                                {
                                    //Set to false, so that we'll request again.
                                    //Ones already complete won't be reset.
                                    CObj co = cl.get ( c );
                                    Long lt = co.getPrivateNumber ( CObj.LASTUPDATE );

                                    //Check lastupdate so we don't request it back to
                                    //back when there's only one fragment for a file.
                                    if ( lt == null || lt < backtime )
                                    {
                                        co.pushPrivate ( CObj.COMPLETE, "false" );
                                        index.index ( co );
                                    }

                                }

                                catch ( IOException e )
                                {
                                    e.printStackTrace();
                                }

                            }

                            cl.close();
                            //Get the new list of fragments to request after resetting
                            cl = index.getFragmentsToRequest ( rf.getCommunityId(),
                                                               rf.getWholeDigest(), rf.getFragmentDigest() );

                            log.info ( "CONMAN: fragments to request2: " + cl.size() + " file: " + rf.getLocalFile() );

                        }

                        if ( cl.size() > 0 )
                        {
                            int idx = Utils.Random.nextInt ( cl.size() );

                            try
                            {
                                CObj co = cl.get ( idx );
                                log.info ( "CONMAN: request fragment: offset " + co.getNumber ( CObj.FRAGOFFSET ) + " file: " + rf.getLocalFile() );
                                co.pushPrivate ( CObj.COMPLETE, "req" );
                                co.pushPrivateNumber ( CObj.LASTUPDATE, System.currentTimeMillis() );
                                index.index ( co );
                                CObj sr = new CObj();
                                sr.setType ( CObj.CON_REQ_FRAG );
                                sr.pushString ( CObj.COMMUNITYID, co.getString ( CObj.COMMUNITYID ) );
                                sr.pushString ( CObj.FILEDIGEST, co.getString ( CObj.FILEDIGEST ) );
                                sr.pushString ( CObj.FRAGDIGEST, co.getString ( CObj.FRAGDIGEST ) );
                                sr.pushString ( CObj.FRAGDIG, co.getString ( CObj.FRAGDIG ) );
                                r = sr;
                            }

                            catch ( IOException e )
                            {
                                e.printStackTrace();
                            }

                        }

                        cl.close();
                    }

                }

            }

        }

        if ( filemode )
        {
            return r;
        }

        //get has file information
        if ( rdy == 1 || ( rdy < 1 && r == null ) )
        {
            CommunityMember cm = identManager.claimHasFileUpdate ( remotedest, comlist, NO_REREQUEST_CYCLES );

            if ( cm != null )
            {
                log.info ( "CONMAN: send has file update request from " +
                           localdest + " to " +
                           remotedest + " community : " +
                           cm.getCommunityId() +
                           " member: " + cm.getMemberId() +
                           " last num: " + cm.getLastFileNumber() );
                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_HASFILE );
                cr.pushString ( CObj.COMMUNITYID, cm.getCommunityId() );
                cr.pushString ( CObj.CREATOR, cm.getMemberId() );
                cr.pushNumber ( CObj.FIRSTNUM, cm.getLastFileNumber() + 1 );
                cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                r = cr;
            }

        }

        //get posts
        if ( rdy == 2 || ( rdy < 2 && r == null ) )
        {
            CommunityMember cm = identManager.claimPostUpdate ( remotedest, comlist, NO_REREQUEST_CYCLES  );

            if ( cm != null )
            {
                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_POSTS );
                cr.pushString ( CObj.COMMUNITYID, cm.getCommunityId() );
                cr.pushString ( CObj.CREATOR, cm.getMemberId() );
                cr.pushNumber ( CObj.FIRSTNUM, cm.getLastPostNumber() + 1 );
                cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                r = cr;
            }

        }

        //get subscriptions
        if ( rdy == 3 || ( rdy < 3 && r == null ) )
        {

            Map<String, Integer> allmems = new HashMap<String, Integer>();
            //See which communities the remote identity is a member
            //does not have to be subscribed
            CObjList memlst = index.getIdentityMemberships ( remotedest );

            for ( int c = 0; c < memlst.size(); c++ )
            {
                try
                {
                    CObj m = memlst.get ( c );
                    String comid = m.getPrivate ( CObj.COMMUNITYID );

                    if ( comid != null )
                    {
                        CObjList numms = index.getMemberships ( comid, null );
                        allmems.put ( comid, numms.size() );
                        numms.close();
                    }

                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            memlst.close();
            //Get all the private communities created by remote node
            memlst = index.getIdentityPrivateCommunities ( remotedest );

            for ( int c = 0; c < memlst.size(); c++ )
            {
                try
                {
                    CObj m = memlst.get ( c );
                    CObjList numms = index.getMemberships ( m.getDig(), null );
                    allmems.put ( m.getDig(), numms.size() );
                    numms.close();
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            memlst.close();

            //Keep privates communities we are membership of
            Map<String, Integer> mymems = new HashMap<String, Integer>();
            memlst = index.getIdentityMemberships ( localdest );

            for ( int c = 0; c < memlst.size(); c++ )
            {
                try
                {
                    CObj m = memlst.get ( c );
                    String comid = m.getPrivate ( CObj.COMMUNITYID );

                    if ( comid != null )
                    {
                        Integer mnum = allmems.get ( comid );

                        if ( mnum != null )
                        {
                            mymems.put ( comid, mnum );
                        }

                    }

                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            memlst.close();
            //Keep private communities we created
            memlst = index.getIdentityPrivateCommunities ( localdest );

            for ( int c = 0; c < memlst.size(); c++ )
            {
                try
                {
                    CObj m = memlst.get ( c );
                    String comid = m.getDig();

                    if ( comid != null )
                    {
                        Integer mnum = allmems.get ( comid );

                        if ( mnum != null )
                        {
                            mymems.put ( comid, mnum );
                        }

                    }

                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            memlst.close();
            allmems = mymems;
            //Get all the public communities.  Everyone is a member of public communties.
            //Does not matter if not subscribed
            memlst = index.getPublicCommunities();

            for ( int c = 0; c < memlst.size(); c++ )
            {
                try
                {
                    CObj m = memlst.get ( c );
                    allmems.put ( m.getDig(), Integer.MAX_VALUE );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            memlst.close();
            log.info ( "memberships in common for requesting subscription update: " + allmems.size() );
            CommunityMember cm = identManager.claimSubUpdate ( remotedest, allmems, NO_REREQUEST_CYCLES );

            if ( cm != null )
            {
                log.info ( "send subscription update request" );
                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_SUBS );
                cr.pushString ( CObj.COMMUNITYID, cm.getCommunityId() );
                cr.pushNumber ( CObj.FIRSTNUM, cm.getLastSubscriptionNumber() + 1L );
                cr.pushString ( CObj.CREATOR, cm.getMemberId() );

                r = cr;
            }

        }

        //get memberships
        if ( rdy == 4 || ( rdy < 4 && r == null ) )
        {
            IdentityData id = identManager.claimMemberUpdate ( remotedest, NO_REREQUEST_CYCLES );

            if ( id != null )
            {
                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_MEMBERSHIPS );
                cr.pushString ( CObj.CREATOR, id.getId() );
                cr.pushNumber ( CObj.FIRSTNUM, id.getLastMembershipNumber() + 1 );
                cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                r = cr;
            }

        }

        //get communities
        if ( rdy == 5 || ( rdy < 5 && r == null ) )
        {
            IdentityData id = identManager.claimCommunityUpdate ( remotedest, NO_REREQUEST_CYCLES );

            if ( id != null )
            {
                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_COMMUNITIES );
                cr.pushString ( CObj.CREATOR, id.getId() );
                cr.pushNumber ( CObj.FIRSTNUM, id.getLastCommunityNumber() + 1 );
                cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                r = cr;
            }

        }

        //Check if we want to get identities
        if ( rdy == 6 || ( rdy < 6 && r == null ) )
        {
            IdentityData id = identManager.claimIdentityUpdate ( remotedest );

            if ( id != null )
            {
                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_IDENTITIES );
                r = cr;
            }

        }

        return r;
    }

    private Map<String, Integer> lastRequestType = new HashMap<String, Integer>();

    @Override
    public Object next ( String localdest, String remotedest, boolean filemode )
    {
        Object r = null;

        //Find all subscriptions the remotedest has, and see how
        //many subscribers that community has
        CObjList clst = index.getMemberSubscriptions ( remotedest );
        Map<String, Integer> comlist = new HashMap<String, Integer>();

        for ( int c = 0; c < clst.size(); c++ )
        {
            try
            {
                CObj co = clst.get ( c );
                String comid = co.getString ( CObj.COMMUNITYID );

                if ( comid != null )
                {
                    CObjList sublst = index.getSubscriptions ( comid, null );
                    log.info ( "CONMAN: " + remotedest + " subscribed to: " + comid + " num: " + sublst.size() );
                    comlist.put ( comid, sublst.size() );
                    sublst.close();
                }

            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        clst.close();
        //Remove subscriptions that this localdest does not have
        Map<String, Integer> ncomlst = new HashMap<String, Integer>();
        clst = index.getMemberSubscriptions ( localdest );

        for ( int c = 0; c < clst.size(); c++ )
        {
            try
            {
                CObj co = clst.get ( c );
                String comid = co.getString ( CObj.COMMUNITYID );

                if ( comid != null )
                {
                    Integer scnt = comlist.get ( comid );
                    log.info ( "CONMAN22: " + remotedest + " subscribed to: " + comid + " num: " + scnt );

                    if ( scnt != null )
                    {
                        ncomlst.put ( comid, scnt );
                    }

                }

            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        clst.close();

        Integer rdy = null;
        String rdykey = localdest + remotedest;

        synchronized ( lastRequestType )
        {
            rdy = lastRequestType.get ( rdykey );
        }

        if ( rdy == null )
        {
            rdy = 1; //Skip files.  Only download files with file only mode
        }

        r = findNext ( localdest, remotedest, ncomlst, rdy, filemode ) ;

        if ( r == null && !filemode )
        {
            r = findNext ( localdest, remotedest, ncomlst, 1, false ) ;
        }

        rdy++;

        if ( rdy > 6 )
        {
            rdy = 1; //Skip files.  Only download files with file only mode
        }

        synchronized ( lastRequestType )
        {
            lastRequestType.put ( rdykey, rdy );
        }

        return r;

    }

    private void deleteOldRequests()
    {
        //hardcode 10 days?
        fileHandler.deleteOldRequests ( 60L * 24L * 60L * 60L * 1000L );
    }

    private void resetupLastUpdateToForceDecode()
    {
        try
        {
            long curtime = System.currentTimeMillis();
            CObjList unlst = index.getUnDecodedMemberships ( 0 );

            for ( int c = 0; c < unlst.size(); c++ )
            {
                CObj co = unlst.get ( c );
                co.pushPrivateNumber ( CObj.LASTUPDATE, curtime );
                index.index ( co );
            }

            unlst.close();

        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    private void decodeMemberships()
    {
        //Find my memberships
        try
        {
            List<CommunityMyMember> mycoms = identManager.getMyMemberships();

            for ( CommunityMyMember c : mycoms )
            {
                long lastdecode = c.getLastDecode();
                long newtime = System.currentTimeMillis();
                //Find all membership records we've received after this time.
                KeyParameter kp = new KeyParameter ( c.getKey() );
                CObjList unlst = index.getUnDecodedMemberships ( lastdecode );

                for ( int cnt = 0; cnt < unlst.size(); cnt++ )
                {
                    CObj um = unlst.get ( cnt );

                    if ( symdec.decode ( um, kp ) )
                    {
                        um.pushPrivate ( CObj.DECODED, "true" );
                        index.index ( um );
                    }

                }

                unlst.close();

                //See if we've validated our own membership yet.
                //it could be we got a new membership, but we didn't decode
                //any.  we still want to attempt to validate it.

                c.setLastDecode ( newtime );
                identManager.saveMyMembership ( c ); //if we get one after we start we'll try again
            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        //Search for all decoded, invalid memberships, and check if valid
        //keep checking until no more validated.
        int lastdec = 0;
        CObjList invliddeclist = index.getInvalidMemberships();

        while ( invliddeclist.size() != lastdec )
        {
            lastdec = invliddeclist.size();

            for ( int c = 0; c < invliddeclist.size(); c++ )
            {
                try
                {
                    CObj m = invliddeclist.get ( c );
                    String creator = m.getString ( CObj.CREATOR );
                    String comid = m.getPrivate ( CObj.COMMUNITYID );
                    String memid = m.getPrivate ( CObj.MEMBERID );
                    Long auth = m.getPrivateNumber ( CObj.AUTHORITY );

                    if ( creator != null && comid != null && auth != null )
                    {
                        CObj com = memvalid.canGrantMemebership ( comid, creator, auth );
                        CObj member = index.getIdentity ( memid );

                        if ( com != null )
                        {
                            m.pushPrivate ( CObj.VALIDMEMBER, "true" );
                            m.pushPrivate ( CObj.NAME, com.getPrivate ( CObj.NAME ) );
                            m.pushPrivate ( CObj.DESCRIPTION, com.getPrivate ( CObj.DESCRIPTION ) );

                            if ( "true".equals ( member.getPrivate ( CObj.MINE ) ) )
                            {
                                m.pushPrivate ( CObj.MINE, "true" );
                                com.pushPrivate ( memid, "true" );
                                index.index ( com );
                            }

                            index.index ( m );

                            if ( callback != null )
                            {
                                callback.update ( m );
                            }

                        }

                    }

                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            invliddeclist.close();
            invliddeclist = index.getInvalidMemberships();
        }

        invliddeclist.close();

    }

    public synchronized void stop()
    {
        stop = true;

        synchronized ( destinations )
        {
            for ( DestinationThread dt : destinations.values() )
            {
                dt.stop();
            }

        }

        notifyAll();
    }

    public synchronized void kickConnections()
    {
        notifyAll();
    }


    private synchronized void delay()
    {
        try
        {
            wait ( DECODE_AND_NEW_CONNECTION_DELAY ); //5 minutes
        }

        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

    }

    @Override
    public void run()
    {
        log.info ( "CONMAN: START!" );
        resetupLastUpdateToForceDecode();

        while ( !stop )
        {
            if ( !stop )
            {
                log.info ( "CONMAN [[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ checkConnections ]]]]]]]]]]]]]]]]]]]]]]]]]]]]]" );
                checkConnections();
                log.info ( "CONMAN [[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ decodeMemberships ]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]" );
                decodeMemberships();
                log.info ( "CONMAN [[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ deleteOldRequests ]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]" );
                deleteOldRequests();
            }

            log.info ( "CONMAN [[[[[[[[[[[[[[[[[[[[[[[[[[[[[ STOP? " + stop + " ]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]" );
            delay();

            CObjList.displayAllStillOpen();

        }

    }

}

