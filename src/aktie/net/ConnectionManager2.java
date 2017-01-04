package aktie.net;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import org.bouncycastle.crypto.params.KeyParameter;

import aktie.crypto.Utils;
import aktie.data.CObj;
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

public class ConnectionManager2 implements GetSendData2, DestinationListener, PushInterface, Runnable
{

    Logger log = Logger.getLogger ( "aktie" );

    public static int MAX_TOTAL_DEST_CONNECTIONS = 100;
    public static long REQUEST_UPDATE_DELAY = 60L * 1000L;
    public static long DECODE_AND_NEW_CONNECTION_DELAY = 2L * 60L * 1000L;
    public static long UPDATE_CACHE_PERIOD = 60L * 1000L;
    public static long MAX_TIME_IN_QUEUE = 24L * 60L * 60L * 1000L;
    public static long MIN_TIME_TO_NEW_CONNECTION = 10L * 60L * 1000L;
    public static int QUEUE_DEPTH_FILE = 100;
    public static int ATTEMPT_CONNECTIONS = 10;
    public static long MAX_TIME_COMMUNITY_ACTIVE_UNTIL_NEW_CONNECTION = 2L * 60L * 1000L;
    public static long MAX_TIME_WITH_NO_REQUESTS = 60L * 60L * 1000L;
    public static long MAX_CONNECTION_TIME  = 2L * 60L * 60L * 1000L; //Only keep connections for 2 hours
    public static int MAX_PUSH_LOOPS = 20; //The number of times we attempt to connect to a node to push to
    public static int PUSH_NODES = 5;  //The number of nodes we'd like to push to

    private Index index;
    private RequestFileHandler fileHandler;
    private IdentityManager identityManager;
    private MembershipValidator memvalid;
    private boolean stop;
    private GuiCallback callback;
    private ConnectionFileManager fileManager;

    //Digest -> Identities pushed to already.
    private ConcurrentMap<String, Set<String>> pushedToCache;
    //Digest -> Number of connection attempts for the push
    private ConcurrentMap<String, Integer> pushConAttempts;
    //Community -> List of objects to push to members
    public static int MAX_MEM_PUSHES = 10;
    private ConcurrentMap<String, ConcurrentLinkedQueue<CObj>> membershipPushes;
    //Community -> List of objects to push to subscribers
    public static int MAX_SUB_PUSHES = 10;
    private ConcurrentMap<String, ConcurrentLinkedQueue<CObj>> subPushes;
    //List of objects to push to anyone
    public static int MAX_PUB_PUSHES = 10;
    private ConcurrentLinkedQueue<CObj> pubPushes;

    //key: remotedest
    private ConcurrentMap<String, Long> recentAttempts;


    private Map<String, DestinationThread> destinations;

    private Map<String, SoftReference<CObj>> identityCache;
    //Use weak reference because we want these to expire at
    //a non-indenfinate interval so we get new subs for peers
    private Map<String, WeakReference<Set<String>>> subCache;

    public ConnectionManager2 ( HH2Session s, Index i, RequestFileHandler r, IdentityManager id,
                                GuiCallback cb )
    {
        destinations = new HashMap<String, DestinationThread>();
        recentAttempts = new ConcurrentHashMap<String, Long>();
        identityCache = new HashMap<String, SoftReference<CObj>>();
        pushedToCache = new ConcurrentHashMap<String, Set<String>>() ;
        pushConAttempts = new ConcurrentHashMap<String, Integer>() ;
        membershipPushes = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CObj>>() ;
        subPushes = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CObj>>() ;
        pubPushes = new ConcurrentLinkedQueue<CObj>();
        index = i;
        fileHandler = r;
        identityManager = id;
        callback = cb;
        fileManager = new ConnectionFileManager ( s, i, r );
        memvalid = new MembershipValidator ( index );
        Thread t = new Thread ( this );
        t.setDaemon ( true );
        t.start();

    }

    public void clearRecentConnections()
    {
        long bt = System.currentTimeMillis() - MIN_TIME_TO_NEW_CONNECTION;
        Iterator<Entry<String, Long>> e = recentAttempts.entrySet().iterator();

        while ( e.hasNext() )
        {
            Entry<String, Long> t = e.next();

            if ( t.getValue() < bt )
            {
                e.remove();
            }

        }

    }

    public long getLastFileUpdate()
    {
        return fileManager.getLastFileUpdate();
    }

    private boolean checkRemovePush ( String d )
    {
        boolean rm = false;
        Set<String> pc = pushedToCache.get ( d );

        if ( pc != null )
        {
            if ( pc.size() >= PUSH_NODES )
            {
                rm = true;
                pushedToCache.remove ( d );
                pushConAttempts.remove ( d );
            }

        }

        Integer pa = pushConAttempts.get ( d );

        if ( pa != null )
        {
            if ( pa > MAX_PUSH_LOOPS )
            {
                rm = true;
                pushedToCache.remove ( d );
                pushConAttempts.remove ( d );
            }

        }

        return rm;
    }

    private void removeStale()
    {
        Iterator<Entry<String, ConcurrentLinkedQueue<CObj>>> ei = membershipPushes.entrySet().iterator();

        while ( ei.hasNext() )
        {
            Entry<String, ConcurrentLinkedQueue<CObj>> e = ei.next();
            Iterator<CObj> mi = e.getValue().iterator();

            while  ( mi.hasNext() )
            {
                CObj c = mi.next();
                String d = c.getDig();

                if ( checkRemovePush ( d ) )
                {
                    mi.remove();
                }

            }

        }

        ei = subPushes.entrySet().iterator();

        while ( ei.hasNext() )
        {
            Entry<String, ConcurrentLinkedQueue<CObj>> e = ei.next();
            Iterator<CObj> mi = e.getValue().iterator();

            while  ( mi.hasNext() )
            {
                CObj c = mi.next();
                String d = c.getDig();

                if ( checkRemovePush ( d ) )
                {
                    mi.remove();
                }

            }

        }

        Iterator<CObj> ii = pubPushes.iterator();

        while ( ii.hasNext() )
        {
            CObj c = ii.next();
            String d = c.getDig();

            if ( checkRemovePush ( d ) )
            {
                ii.remove();
            }

        }

        Iterator<Entry<String, SoftReference<CObj>>> i = identityCache.entrySet().iterator();

        while ( i.hasNext() )
        {
            Entry<String, SoftReference<CObj>> e = i.next();

            if ( e.getValue().get() == null )
            {
                i.remove();
            }

        }

    }

    /*
        Get a prioritized list of files to request from the
        remote destination - making sure the remote destination
        says it has the files
    */
    public Set<RequestFile> getHasFileForConnection ( String remotedest, Set<String> subs )
    {
        Set<RequestFile> r = new HashSet<RequestFile>();
        List<RequestFile> rl = fileHandler.listRequestFilesNE ( RequestFile.COMPLETE, Integer.MAX_VALUE );

        for ( RequestFile rf : rl )
        {
            if ( subs.contains ( rf.getCommunityId() ) )
            {
                CObj co = index.getIdentHasFile ( rf.getCommunityId(),
                                                  remotedest, rf.getWholeDigest(), rf.getFragmentDigest() );

                CObj so = index.getSubscription ( rf.getCommunityId(), remotedest );

                if ( co != null && so != null )
                {
                    r.add ( rf );
                }

            }

        }

        return r;
    }

    //  Communityid ->  File digest -> Requests
    public Object nextFile ( String localdest, String remotedest, Set<RequestFile> hasfiles )
    {
        //Connection was successful remove
        //from recent attempts so we know it's a good
        //one to connect to
        recentAttempts.remove ( remotedest + true );

        Object r = fileManager.nextFile ( localdest, remotedest, hasfiles );

        if ( r == null )
        {
            kickConnections();
        }

        return r;
    }

    private int[] randomList ( int i )
    {
        int r[] = new int[i];

        for ( int n = 0; n < i; n++ )
        {
            r[n] = n;
        }

        for ( int n = 0; n < i - 1; n++ )
        {
            int ps = i - n;
            int pf = Utils.Random.nextInt ( ps );

            if ( pf > 0 )
            {
                int ix = n + pf;
                int v = r[ix];
                r[ix] = r[n];
                r[n] = v;
            }

        }

        return r;
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

    private CObj getIdentity ( String id )
    {
        CObj identObj = null;
        SoftReference<CObj> identity = identityCache.get ( id );

        if ( identity != null )
        {
            identObj = identity.get();
        }

        if ( identObj == null )
        {
            identObj = index.getIdentity ( id );

            if ( identObj != null )
            {
                identityCache.put ( id, new SoftReference<CObj> ( identObj ) );
            }

        }

        return identObj;
    }

    private DestinationThread getMyDestinationThread ( Map<String, CObj> myidmap, String myid )
    {
        DestinationThread dt = null;

        if ( myid != null )
        {
            CObj mid = myidmap.get ( myid );

            if ( mid != null )
            {
                String mydest = mid.getString ( CObj.DEST );

                if ( mydest != null )
                {
                    synchronized ( destinations )
                    {
                        dt = destinations.get ( mydest );
                    }

                }

            }

        }

        return dt;
    }

    private List<DestinationThread> findMyDestinationsForCommunity ( Map<String, CObj> myidmap, String comid )
    {
        List<CObj> mysubslist = Index.list ( index.getMySubscriptions ( comid ) );
        //Find my destinations for my subscriptions to this community
        List<DestinationThread> dlst = new LinkedList<DestinationThread>();

        for ( CObj c : mysubslist )
        {

            DestinationThread dt = getMyDestinationThread ( myidmap, c.getString ( CObj.CREATOR ) );

            if ( dt != null )
            {
                dlst.add ( dt );
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

                        DestinationThread dt = getMyDestinationThread ( myidmap, c.getPrivate ( CObj.MEMBERID ) );

                        if ( dt != null && !dlst.contains ( dt ) )
                        {
                            dlst.add ( dt );
                        }

                    }

                    if ( "true".equals ( com.getPrivate ( CObj.MINE ) ) )
                    {
                        DestinationThread dt = getMyDestinationThread ( myidmap, com.getString ( CObj.CREATOR ) );

                        if ( dt != null && !dlst.contains ( dt ) )
                        {
                            dlst.add ( dt );
                        }

                    }

                }

            }

        }

        return dlst;
    }

    private boolean attemptConnection ( DestinationThread dt, CObj id, boolean fm, Map<String, CObj> myids )
    {

        if ( dt != null && dt.numberConnection() < MAX_TOTAL_DEST_CONNECTIONS &&
                myids.get ( id.getId() ) == null )
        {
            long ct = System.currentTimeMillis();
            long bt = ct - MIN_TIME_TO_NEW_CONNECTION;
            Long ra = recentAttempts.get ( id.getId() + fm );

            if ( ra == null || ra <= bt )
            {
                String dest = id.getString ( CObj.DEST );

                if ( !dt.isConnected ( id.getId(), fm ) && dest != null )
                {
                    recentAttempts.put ( id.getId() + fm, ct );
                    dt.connect ( dest, fm );
                    return true;
                }

                else
                {
                    log.info ( "FAILED: Already connected! " + id.getId() );
                }

            }

            else
            {
                log.info ( "FAILED: Already attempted connection. " + id.getId() );
            }

        }

        else
        {
            log.info ( "FAILED:  Too many connections or self connection " + id.getId() );
        }

        return false;
    }


    private Set<String> getSubs ( String id )
    {
        Set<String> r = null;
        WeakReference<Set<String>> lst = subCache.get ( id );

        if ( lst != null )
        {
            r = lst.get();
        }

        if ( r == null )
        {
            r = new HashSet<String>();
            CObjList sl = index.getMemberSubscriptions ( id );

            for ( int c = 0; c < sl.size(); c++ )
            {
                try
                {
                    CObj sub = sl.get ( c );
                    r.add ( sub.getString ( CObj.COMMUNITYID ) );
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sl.close();
            subCache.put ( id, new WeakReference<Set<String>> ( r ) );
        }

        return r;
    }

    private void checkConnections()
    {
        int con = 0;

        //Increment push loops
        Iterator<Entry<String, ConcurrentLinkedQueue<CObj>>> ei = membershipPushes.entrySet().iterator();

        while ( ei.hasNext() )
        {
            Entry<String, ConcurrentLinkedQueue<CObj>> et = ei.next();
            Iterator<CObj> si = et.getValue().iterator();

            while ( si.hasNext() )
            {
                CObj co = si.next();
                Integer cnt = pushConAttempts.get ( co.getDig() );

                if ( cnt == null )
                {
                    cnt = 0;
                }

                cnt++;
                pushConAttempts.put ( co.getDig(), cnt );
            }

        }

        ei = subPushes.entrySet().iterator();

        while ( ei.hasNext() )
        {
            Entry<String, ConcurrentLinkedQueue<CObj>> et = ei.next();
            Iterator<CObj> si = et.getValue().iterator();

            while ( si.hasNext() )
            {
                CObj co = si.next();
                Integer cnt = pushConAttempts.get ( co.getDig() );

                if ( cnt == null )
                {
                    cnt = 0;
                }

                cnt++;
                pushConAttempts.put ( co.getDig(), cnt );
            }

        }

        Iterator<CObj> si = pubPushes.iterator();

        while ( si.hasNext() )
        {
            CObj co = si.next();
            Integer cnt = pushConAttempts.get ( co.getDig() );

            if ( cnt == null )
            {
                cnt = 0;
            }

            cnt++;
            pushConAttempts.put ( co.getDig(), cnt );
        }

        Map<String, CObj> myids = getMyIdMap();

        //======================================================
        //First connect to peers with files we requested
        //fileRequests is already in priority order, so just
        //try to connect to as many allowed that has the file!
        //user can change priorities if they don't like it.
        List<RequestFile> rls = fileManager.getRequestFile();
        Iterator<RequestFile> i = rls.iterator();

        while ( i.hasNext() && con < ATTEMPT_CONNECTIONS )
        {
            RequestFile rf = i.next();
            log.info ( "ConnectionManager2: attempt connection for file: " + rf.getLocalFile() );

            DestinationThread dt = null;

            CObj mydest = myids.get ( rf.getRequestId() );

            if ( mydest != null )
            {
                synchronized ( destinations )
                {
                    dt = destinations.get ( mydest.getString ( CObj.DEST ) );
                }

            }

            if ( dt != null )
            {
                if ( dt.numberConnection() < MAX_TOTAL_DEST_CONNECTIONS )
                {

                    CObjList clst = index.
                                    getHasFiles ( rf.getCommunityId(), rf.getWholeDigest(), rf.getFragmentDigest() );

                    log.info ( "ConnectionManager2: other nodes with file: " + clst.size() + " cons " + con );

                    int rl[] = randomList ( clst.size() );

                    for ( int c = 0; c < rl.length && con < ATTEMPT_CONNECTIONS; c++ )
                    {
                        try
                        {
                            CObj rd = clst.get ( rl[c] );
                            String id = rd.getString ( CObj.CREATOR );

                            Set<String> subs = getSubs ( id );

                            boolean contains = subs.contains ( rf.getCommunityId() );
                            log.info ( "ConnectionManager2: attempt connetion to: " + id + " subs: " + subs.size() + " contains: " + contains );

                            if ( contains )
                            {
                                CObj identity = getIdentity ( id );

                                if ( attemptConnection ( dt, identity, true, myids ) )
                                {
                                    con++;
                                }

                            }

                        }

                        catch ( Exception e )
                        {
                            e.printStackTrace();
                        }

                    }

                    clst.close();
                }

            }

        }

        //======================================================
        //Attempt connections to cover subscribed communities
        if ( con < ATTEMPT_CONNECTIONS )
        {
            CObjList clt = index.getMySubscriptions();
            Map<String, Integer> cl = new HashMap<String, Integer>();

            for ( int c = 0; c < clt.size(); c++ )
            {
                try
                {
                    CObj sub = clt.get ( c );
                    String comid = sub.getString ( CObj.COMMUNITYID );

                    if ( comid != null )
                    {
                        Integer cnt = cl.get ( comid );

                        if ( cnt == null )
                        {
                            cnt = 0;
                        }

                        cnt = cnt + 1;
                        cl.put ( comid, cnt );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            clt.close();
            List<String> comlst = new LinkedList<String>();
            comlst.addAll ( cl.keySet() );
            int ri[] = randomList ( comlst.size() );

            for ( int c = 0; c < ri.length && con < ATTEMPT_CONNECTIONS; c++ )
            {
                try
                {
                    String comid = comlst.get ( ri[c] );
                    Integer ccnt = cl.get ( comid );

                    if ( ccnt != null )
                    {
                        List<DestinationThread> dtl =
                            findMyDestinationsForCommunity ( myids, comid );

                        CObjList othersubs = index.getSubscriptions ( comid, null );
                        int ori[] = randomList ( othersubs.size() );
                        int cns = 0;

                        for ( int c0 = 0; c0 < ori.length &&
                                cns < ccnt &&
                                con < ATTEMPT_CONNECTIONS; c0++ )
                        {
                            CObj suber = othersubs.get ( ori[c0] );
                            String creator = suber.getString ( CObj.CREATOR );

                            if ( creator != null )
                            {
                                CObj identity = getIdentity ( creator );
                                boolean done = false;
                                Iterator<DestinationThread> dti = dtl.iterator();

                                while ( !done && dti.hasNext() && identity != null )
                                {
                                    DestinationThread dt = dti.next();

                                    if ( attemptConnection ( dt, identity, false, myids ) )
                                    {
                                        con++;
                                        cns++;
                                        done = true;
                                    }

                                }

                            }

                        }

                        othersubs.close();
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

        }

        //======================================================
        //Attempt connections to cover subscribed communities
        if ( con < ATTEMPT_CONNECTIONS )
        {
            CObjList clt = index.getMyValidMemberships ( null );
            Map<String, Integer> cl = new HashMap<String, Integer>();

            for ( int c = 0; c < clt.size(); c++ )
            {
                try
                {
                    CObj sub = clt.get ( c );
                    String comid = sub.getString ( CObj.COMMUNITYID );

                    if ( comid != null )
                    {
                        Integer cnt = cl.get ( comid );

                        if ( cnt == null )
                        {
                            cnt = 0;
                        }

                        cnt = cnt + 1;
                        cl.put ( comid, cnt );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            clt.close();
            List<String> comlst = new LinkedList<String>();
            comlst.addAll ( cl.keySet() );
            int ri[] = randomList ( comlst.size() );

            for ( int c = 0; c < ri.length && con < ATTEMPT_CONNECTIONS; c++ )
            {
                try
                {
                    String comid = comlst.get ( ri[c] );
                    Integer ccnt = cl.get ( comid );

                    if ( ccnt != null )
                    {
                        List<DestinationThread> dtl =
                            findAllMyDestinationsForCommunity ( myids, comid );

                        CObjList othersubs = index.getMemberships ( comid, null );
                        int ori[] = randomList ( othersubs.size() );
                        int cns = 0;

                        for ( int c0 = 0; c0 < ori.length &&
                                cns < ccnt &&
                                con < ATTEMPT_CONNECTIONS; c0++ )
                        {
                            CObj suber = othersubs.get ( ori[c0] );
                            String creator = suber.getString ( CObj.CREATOR );

                            if ( creator != null )
                            {
                                CObj identity = getIdentity ( creator );
                                boolean done = false;
                                Iterator<DestinationThread> dti = dtl.iterator();

                                while ( !done && dti.hasNext() && identity != null )
                                {
                                    DestinationThread dt = dti.next();

                                    if ( attemptConnection ( dt, identity, false, myids ) )
                                    {
                                        con++;
                                        cns++;
                                        done = true;
                                    }

                                }

                            }

                        }

                        othersubs.close();
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

        }

        //======================================================
        // Attempt Any if needed
        //if ( con == 0 )
        //{
        List<DestinationThread> alldest = new LinkedList<DestinationThread>();

        Iterator<CObj> pushlist = pubPushes.iterator();

        while ( pushlist.hasNext() )
        {
            CObj cobj = pushlist.next();
            String creator = cobj.getString ( CObj.CREATOR );

            if ( creator != null )
            {
                DestinationThread dt = getMyDestinationThread ( myids, creator );

                if ( dt != null && !alldest.contains ( dt ) )
                {
                    alldest.add ( dt );
                }

            }

        }

        if ( alldest.size() == 0 )
        {
            synchronized ( destinations )
            {
                alldest.addAll ( destinations.values() );
            }

        }

        CObjList idlst = index.getIdentities();
        int ridx[] = randomList ( idlst.size() );

        //log.info("ALL DESTINATIONS: " + alldest.size() + " ids: " + idlst.size() + " cons: " + con);

        for ( int c0 = 0; c0 < idlst.size() && con < ATTEMPT_CONNECTIONS; c0++ )
        {
            try
            {
                CObj identity = idlst.get ( ridx[c0] );
                Iterator<DestinationThread> it = alldest.iterator();

                while ( it.hasNext() && con < ATTEMPT_CONNECTIONS )
                {
                    DestinationThread dt = it.next();

                    if ( attemptConnection ( dt, identity, false, myids ) )
                    {
                        con++;
                    }

                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        idlst.close();
        //}

    }

    private Object nextMembershipPush ( String local, String ident, Set<String> mems )
    {
        Object n = null;
        List<String> sl = new ArrayList<String>();
        sl.addAll ( mems );
        int r[] = randomList ( sl.size() );

        for ( int i = 0; i < r.length && n == null; i++ )
        {
            int ix = r[i];
            ConcurrentLinkedQueue<CObj> cl = membershipPushes.get ( sl.get ( ix ) );

            if ( cl != null )
            {
                Iterator<CObj> ci = cl.iterator();

                while ( n == null && ci.hasNext() )
                {
                    CObj co = ci.next();

                    if ( local != null && local.equals ( co.getString ( CObj.CREATOR ) ) )
                    {
                        Set<String> sc = pushedToCache.get ( co.getDig() );

                        if ( sc == null )
                        {
                            sc = new CopyOnWriteArraySet<String>();
                            pushedToCache.put ( co.getDig(), sc );
                        }

                        if ( !sc.contains ( ident ) )
                        {
                            n = co;
                            sc.add ( ident );
                        }

                    }

                }

            }

        }

        return n;
    }

    private Object nextSubPush ( String local, String ident, Set<String> subs )
    {
        Object n = null;
        List<String> sl = new ArrayList<String>();
        sl.addAll ( subs );
        int r[] = randomList ( sl.size() );

        for ( int i = 0; i < r.length && n == null; i++ )
        {
            int ix = r[i];
            ConcurrentLinkedQueue<CObj> cl = subPushes.get ( sl.get ( ix ) );

            if ( cl != null )
            {
                Iterator<CObj> ci = cl.iterator();

                while ( n == null && ci.hasNext() )
                {
                    CObj co = ci.next();

                    if ( local != null && local.equals ( co.getString ( CObj.CREATOR ) ) )
                    {
                        Set<String> sc = pushedToCache.get ( co.getDig() );

                        if ( sc == null )
                        {
                            sc = new CopyOnWriteArraySet<String>();
                            pushedToCache.put ( co.getDig(), sc );
                        }

                        if ( !sc.contains ( ident ) )
                        {
                            n = co;
                            sc.add ( ident );
                        }

                    }

                }

            }

        }

        return n;
    }

    private Object nextPubPush ( String local, String ident )
    {
        Object n = null;
        Iterator<CObj> ci = pubPushes.iterator();

        while ( n == null && ci.hasNext() )
        {
            CObj co = ci.next();

            if ( local != null && local.equals ( co.getString ( CObj.CREATOR ) ) )
            {
                Set<String> sc = pushedToCache.get ( co.getDig() );

                if ( sc == null )
                {
                    sc = new CopyOnWriteArraySet<String>();
                    pushedToCache.put ( co.getDig(), sc );
                }

                if ( !sc.contains ( ident ) )
                {
                    n = co;
                    sc.add ( ident );
                }

            }

        }

        return n;
    }

    public Object nextNonFile ( String localdest, String remotedest, Set<String> members, Set<String> subs )
    {

        recentAttempts.remove ( remotedest + false );

        Object n = null;
        n = nextMembershipPush ( localdest, remotedest, members );

        if ( n == null )
        {
            n = nextSubPush ( localdest, remotedest, subs );
        }

        if ( n == null )
        {
            n = nextPubPush ( localdest, remotedest );
        }

        return n;
    }

    public List<CObj> getConnectedIdentities()
    {
        List<CObj> r = new LinkedList<CObj>();

        synchronized ( destinations )
        {
            for ( Entry<String, DestinationThread> e : destinations.entrySet() )
            {
                r.add ( e.getValue().getIdentity() );
            }

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

    public void closeConnection ( String localdest, String remotedest )
    {
        CObj myid = this.getIdentity ( localdest );

        if ( myid != null )
        {
            String dest = myid.getString ( CObj.DEST );

            if ( dest != null )
            {
                DestinationThread dt = null;

                synchronized ( destinations )
                {
                    dt = destinations.get ( dest );
                }

                if ( dt != null )
                {
                    dt.closeConnection ( remotedest );
                }

            }

        }

    }

    public void toggleConnectionLogging ( String localdest, String remotedest )
    {
        CObj myid = this.getIdentity ( localdest );

        if ( myid != null )
        {
            String dest = myid.getString ( CObj.DEST );

            if ( dest != null )
            {
                DestinationThread dt = null;

                synchronized ( destinations )
                {
                    dt = destinations.get ( dest );
                }

                if ( dt != null )
                {
                    dt.toggleConnectionLogging ( remotedest );
                }

            }

        }

    }

    private void checkGlobalSequences()
    {
        CObjList ilst = index.getMyIdentities();

        for ( int c = 0; c < ilst.size(); c++ )
        {
            try
            {
                CObj id = ilst.get ( c );
                CObjList misslst = index.getAllMissingSeqNumbers
                                   ( id.getId(), ( int ) IdentityData.MAXGLOBALSEQUENCECOUNT );

                for ( int c0 = 0; c0 < misslst.size(); c0++ )
                {
                    CObj ob = misslst.get ( c0 );
                    String sid = ob.getId();
                    long sn = identityManager.getGlobalSequenceNumber ( sid );
                    ob.pushPrivateNumber ( CObj.getGlobalSeq ( sid ), sn );
                    index.index ( ob );
                }

                misslst.close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        ilst.close();
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
            boolean newdecoded = false;

            //New memberships have a LastDecode time of zero.
            //So we should look at all undecoded memberships after
            //becoming a new member
            List<CommunityMyMember> mycoms = identityManager.getMyMemberships();

            for ( CommunityMyMember c : mycoms )
            {
                long lastdecode = c.getLastDecode();
                long newtime = System.currentTimeMillis();
                //Find all membership records we've received after this time.
                KeyParameter kp = new KeyParameter ( c.getKey() );
                CObjList unlst = index.getUnDecodedMemberships ( lastdecode -
                                 ( 2 * 5000 ) ) ; //Index.MIN_TIME_BETWEEN_SEARCHERS ) );

                for ( int cnt = 0; cnt < unlst.size(); cnt++ )
                {
                    CObj um = unlst.get ( cnt );

                    if ( SymDecoder.decode ( um, kp ) )
                    {
                        um.pushPrivate ( CObj.DECODED, "true" );
                        index.index ( um );
                        newdecoded = true;
                    }

                }

                unlst.close();

                //See if we've validated our own membership yet.
                //it could be we got a new membership, but we didn't decode
                //any.  we still want to attempt to validate it.

                c.setLastDecode ( newtime );
                identityManager.saveMyMembership ( c ); //if we get one after we start we'll try again

            }

            if ( newdecoded )
            {
                index.forceNewSearcher();
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
                            if ( "true".equals ( member.getPrivate ( CObj.MINE ) ) )
                            {
                                m.pushPrivate ( CObj.MINE, "true" );
                                com.pushPrivate ( memid, "true" );

                                //Test to make sure we've decoded the community.
                                //It's possible we haven't due to lucene commit delay
                                if ( !"true".equals ( com.getPrivate ( CObj.MINE ) ) )
                                {
                                    String key = m.getPrivate ( CObj.KEY );

                                    byte bk[] = Utils.toByteArray ( key );

                                    if ( bk != null )
                                    {
                                        KeyParameter sk = new KeyParameter ( bk );

                                        if ( !SymDecoder.decode ( com, sk ) )
                                        {
                                            log.severe ( "Community failed to decode! " + comid );
                                        }

                                    }

                                    else
                                    {
                                        log.severe ( "Membership key not found! " + memid );
                                    }

                                    com.pushPrivate ( CObj.MINE, "true" );
                                }

                                index.index ( com );
                            }

                            m.pushPrivate ( CObj.VALIDMEMBER, "true" );
                            m.pushPrivate ( CObj.NAME, com.getPrivate ( CObj.NAME ) );
                            m.pushPrivate ( CObj.DESCRIPTION, com.getPrivate ( CObj.DESCRIPTION ) );

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
            index.forceNewSearcher();
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

        fileManager.stop();

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
            wait ( REQUEST_UPDATE_DELAY );
        }

        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

    }

    private long nextdecode = 0;
    @Override
    public void run()
    {
        resetupLastUpdateToForceDecode();
        log.info ( "STARTING CONNECTION MANAGER2" );

        while ( !stop )
        {
            if ( !stop )
            {
                removeStale();

                if ( System.currentTimeMillis() >= nextdecode )
                {
                    decodeMemberships();
                    clearRecentConnections();
                    checkConnections();
                    checkGlobalSequences();
                    nextdecode = System.currentTimeMillis() +
                                 DECODE_AND_NEW_CONNECTION_DELAY;
                }

            }

            delay();

        }

        log.info ( "MANAGER2 EXIT" );

    }

}
