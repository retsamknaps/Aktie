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
import aktie.data.CommunityMember;
import aktie.data.CommunityMyMember;
import aktie.data.DeveloperIdentity;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.data.PrivateMsgIdentity;
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
    //Community dig -> List of subscription requests from members
    private ConcurrentMap<String, ConcurrentLinkedQueue<CObj>> privSubRequests;

    //key: remotedest
    private ConcurrentMap<String, Long> recentAttempts;

    //Community data requests.  Key is community id
    //Requests only sent to subscribers of communities
    public int MAX_COM_REQUESTS = 100;
    private ConcurrentMap<String, ConcurrentLinkedQueue<CObj>> communityRequests;

    //Requests that can be sent to any node
    public int MAX_PUB_REQUESTS = 1000;
    private ConcurrentLinkedQueue<CObj> nonCommunityRequests;

    //The time the community requests were added to the queue
    private ConcurrentMap<String, Long> communityTime;

    private Map<String, DestinationThread> destinations;

    private Map<String, SoftReference<CObj>> identityCache;
    //Use weak reference because we want these to expire at
    //a non-indenfinate interval so we get new subs for peers
    private Map<String, WeakReference<Set<String>>> subCache;

    //A cache of recent private membership subscription updates
    //So we know which are updating ok and where we need new
    //connections
    private ConcurrentMap<String, Long> currentActiveSubPrivRequests;
    private ConcurrentMap<String, Long> currentActiveComRequests;

    public ConnectionManager2 ( HH2Session s, Index i, RequestFileHandler r, IdentityManager id,
                                GuiCallback cb )
    {
        privSubRequests = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CObj>>();
        communityRequests = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CObj>>();
        nonCommunityRequests = new ConcurrentLinkedQueue<CObj>();
        communityTime = new ConcurrentHashMap<String, Long>();
        destinations = new HashMap<String, DestinationThread>();
        recentAttempts = new ConcurrentHashMap<String, Long>();
        identityCache = new HashMap<String, SoftReference<CObj>>();
        subCache = new HashMap<String, WeakReference<Set<String>>>();
        currentActiveSubPrivRequests = new ConcurrentHashMap<String, Long>();
        currentActiveComRequests = new ConcurrentHashMap<String, Long>();
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

    public ConcurrentMap<String, ConcurrentLinkedQueue<CObj>> getPrivSubRequests()
    {
        return privSubRequests;
    }

    private void gatherPrivSubRequests()
    {
        Set<String> comids = new HashSet<String>();

        for ( String mid : getMyIdMap().keySet() )
        {
            CObjList sl = index.getIdentityMemberships ( mid );
            log.info ( "Memberships for " + mid + " " + sl.size() );

            for ( int c = 0; c < sl.size(); c++ )
            {
                try
                {
                    CObj sb = sl.get ( c );
                    String sbid = sb.getPrivate ( CObj.COMMUNITYID );

                    if ( sbid != null )
                    {
                        log.info ( "Adding comunity: " + sbid );
                        comids.add ( sbid );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sl.close();
            sl = index.getIdentityPrivateCommunities ( mid );
            log.info ( "Private communitys created by "  + mid + " " + sl.size() );

            for ( int c = 0; c < sl.size(); c++ )
            {
                try
                {
                    CObj sb = sl.get ( c );

                    if ( sb != null && sb.getDig() != null )
                    {
                        log.info ( "Adding comunity: " + sb.getDig() );
                        comids.add ( sb.getDig() );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sl.close();
        }

        ConcurrentMap<String, Set<String>> tempComMemberships =
            new ConcurrentHashMap<String, Set<String>>() ;

        for ( String comid : comids )
        {
            CObj comc = index.getCommunity ( comid );

            if ( comc != null )
            {
                pushMember ( tempComMemberships, comid,
                             comc.getString ( CObj.CREATOR ) );
            }

            CObjList mems = index.getMemberships ( comid, null );

            for ( int c = 0; c < mems.size(); c++ )
            {
                try
                {
                    CObj mem = mems.get ( c );

                    if ( mem != null )
                    {
                        String memid = mem.getPrivate ( CObj.MEMBERID );

                        if ( memid != null )
                        {
                            pushMember ( tempComMemberships, comid, memid );
                        }

                    }

                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            mems.close();
            log.info ( "Number of members for : " + comid + " " + tempComMemberships.get ( comid ).size() );

        }

        ConcurrentMap<String, ConcurrentLinkedQueue<CObj>> tmpPrivSubList = new
        ConcurrentHashMap<String, ConcurrentLinkedQueue<CObj>>();

        for ( Entry<String, Set<String>> e : tempComMemberships.entrySet() )
        {
            ConcurrentLinkedQueue<CObj> q = tmpPrivSubList.get ( e.getKey() );

            if ( q == null )
            {
                q = new ConcurrentLinkedQueue<CObj>();
                tmpPrivSubList.put ( e.getKey(), q );
            }

            for ( String memid : e.getValue() )
            {
                IdentityData id = identityManager.getIdentity ( memid );
                log.info ( "identityData for " + memid + " " + id );

                if ( id != null )
                {
                    CObj cr = new CObj();
                    cr.setType ( CObj.CON_REQ_SUBS );
                    cr.pushString ( CObj.CREATOR, id.getId() );
                    cr.pushNumber ( CObj.FIRSTNUM, id.getLastSubNumber() + 1 );
                    cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                    q.add ( cr );
                }

            }

        }

        privSubRequests = tmpPrivSubList;
    }

    private void pushMember ( Map<String, Set<String>> cm, String com, String mid )
    {
        if ( com != null && mid != null )
        {
            Set<String> ms = cm.get ( com );

            if ( ms == null )
            {
                ms = new HashSet<String>();
                cm.put ( com, ms );
            }

            ms.add ( mid );
        }

    }

    private boolean procComQueue()
    {
        int qsize = 0;
        int lastsize = -1;
        boolean gonext = false;

        //My subs
        CObjList mslist = index.getMySubscriptions();
        List<CObj> mysubs = new LinkedList<CObj>();

        for ( int c = 0; c < mslist.size(); c++ )
        {
            try
            {
                mysubs.add ( mslist.get ( c ) );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        mslist.close();

        int maxloops = 3;

        while ( lastsize != qsize && maxloops > 0 )
        {
            maxloops--;

            log.info ( "procComQueue loop... " + lastsize + " = " + qsize );
            lastsize = qsize;

            //Find pushes we haven't claimed
            CObjList plst = index.getPushesToSend();

            for ( int cnt = 0; cnt < 2; cnt++ )
            {
                try
                {
                    CObj co = plst.get ( cnt );

                    //public static String IDENTITY = "identity";
                    //public static String COMMUNITY = "community";
                    //public static String MEMBERSHIP = "membership";
                    String tp = co.getType();

                    if ( CObj.IDENTITY.equals ( tp ) ||
                            CObj.COMMUNITY.equals ( tp ) ||
                            CObj.MEMBERSHIP.equals ( tp ) ||
                            CObj.SPAMEXCEPTION.equals ( tp ) ||
                            CObj.PRIVIDENTIFIER.equals ( tp ) ||
                            CObj.PRIVMESSAGE.equals ( tp ) )
                    {
                        if ( pubPushes.size() < MAX_PUB_PUSHES )
                        {
                            co.pushPrivate ( CObj.PRV_PUSH_REQ, "false" );
                            index.index ( co );
                            qsize++;
                            gonext = true;
                            pubPushes.add ( co );
                        }

                    }

                    //public static String SUBSCRIPTION = "subscription";
                    if ( CObj.SUBSCRIPTION.equals ( tp ) )
                    {
                        String comid = co.getString ( CObj.COMMUNITYID );

                        if ( comid != null )
                        {
                            CObj com = index.getByDig ( comid );

                            if ( com != null )
                            {
                                String scope = com.getString ( CObj.SCOPE );

                                if ( CObj.SCOPE_PRIVATE.equals ( scope ) )
                                {
                                    ConcurrentLinkedQueue<CObj> mq = membershipPushes.get ( comid );

                                    if ( mq == null )
                                    {
                                        mq = new ConcurrentLinkedQueue<CObj>();
                                        membershipPushes.put ( comid, mq );
                                    }

                                    if ( mq.size() < MAX_MEM_PUSHES )
                                    {
                                        co.pushPrivate ( CObj.PRV_PUSH_REQ, "false" );
                                        index.index ( co );
                                        qsize++;
                                        gonext = true;
                                        mq.add ( co );
                                    }

                                }

                                else
                                {
                                    if ( pubPushes.size() < MAX_PUB_PUSHES )
                                    {
                                        co.pushPrivate ( CObj.PRV_PUSH_REQ, "false" );
                                        index.index ( co );
                                        qsize++;
                                        gonext = true;
                                        pubPushes.add ( co );

                                    }

                                }

                            }

                        }

                    }

                    //public static String POST = "post";
                    //public static String HASFILE = "hasfile";
                    if ( CObj.POST.equals ( tp ) ||
                            CObj.HASFILE.equals ( tp ) )
                    {
                        String comid = co.getString ( CObj.COMMUNITYID );

                        if ( comid != null )
                        {
                            ConcurrentLinkedQueue<CObj> mq = subPushes.get ( comid );

                            if ( mq == null )
                            {
                                mq = new ConcurrentLinkedQueue<CObj>();
                                subPushes.put ( comid, mq );
                            }

                            if ( mq.size() < MAX_SUB_PUSHES )
                            {
                                co.pushPrivate ( CObj.PRV_PUSH_REQ, "false" );
                                index.index ( co );
                                qsize++;
                                gonext = true;
                                mq.add ( co );
                            }

                        }

                    }

                }

                catch ( Exception e )
                {
                }

            }

            plst.close();

            //Identity update is always sent first to any node upon
            //initial connection.  No need to enqueue identity updates
            //here.

            //CObjList slist = index.getMySubscriptions();

            for ( int c = 0; c < mysubs.size(); c++ )
            {
                log.info ( "procComQueue loop AA " + c );
                CObj sb = mysubs.get ( c );
                String comid = sb.getString ( CObj.COMMUNITYID );

                if ( comid != null )
                {
                    ConcurrentLinkedQueue<CObj> clst = communityRequests.get ( comid );

                    if ( clst == null )
                    {
                        clst = new ConcurrentLinkedQueue<CObj>();
                        communityRequests.put ( comid, clst );
                    }

                    if ( clst.size() < MAX_COM_REQUESTS )
                    {
                        List<CommunityMember> cl = identityManager.claimHasFileUpdate ( comid, 5 );
                        Iterator<CommunityMember> il = cl.iterator();

                        while ( il.hasNext() )
                        {
                            CommunityMember cm = il.next();
                            CObj cr = new CObj();
                            cr.setType ( CObj.CON_REQ_HASFILE );
                            cr.pushString ( CObj.COMMUNITYID, cm.getCommunityId() );
                            cr.pushString ( CObj.CREATOR, cm.getMemberId() );
                            cr.pushNumber ( CObj.FIRSTNUM, cm.getLastFileNumber() + 1 );
                            cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                            clst.add ( cr );
                            gonext = true;
                            qsize++;
                            Long tm = communityTime.get ( comid );

                            if ( tm == null )
                            {
                                tm = System.currentTimeMillis();
                                communityTime.put ( comid, tm );
                            }

                        }

                        cl = identityManager.claimPostUpdate ( comid, 5 );
                        il = cl.iterator();

                        while ( il.hasNext() )
                        {
                            CommunityMember cm = il.next();
                            CObj cr = new CObj();
                            cr.setType ( CObj.CON_REQ_POSTS );
                            cr.pushString ( CObj.COMMUNITYID, cm.getCommunityId() );
                            cr.pushString ( CObj.CREATOR, cm.getMemberId() );
                            cr.pushNumber ( CObj.FIRSTNUM, cm.getLastPostNumber() + 1 );
                            cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                            clst.add ( cr );
                            gonext = true;
                            qsize++;
                            Long tm = communityTime.get ( comid );

                            if ( tm == null )
                            {
                                tm = System.currentTimeMillis();
                                communityTime.put ( comid, tm );
                            }

                        }

                    }

                }


            }

            if ( nonCommunityRequests.size() < MAX_PUB_REQUESTS )
            {
                List<DeveloperIdentity> cl = identityManager.claimSpamExUpdate ( 10 );
                Iterator<DeveloperIdentity> il = cl.iterator();

                while ( il.hasNext() )
                {
                    DeveloperIdentity id = il.next();
                    CObj cr = new CObj();
                    cr.setType ( CObj.CON_REQ_SPAMEX );
                    cr.pushString ( CObj.CREATOR, id.getId() );
                    cr.pushNumber ( CObj.FIRSTNUM, id.getLastSpamExNumber() + 1 );
                    cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                    nonCommunityRequests.add ( cr );
                    gonext = true;
                    qsize++;
                }

            }

            if ( nonCommunityRequests.size() < MAX_PUB_REQUESTS )
            {
                List<IdentityData> cl = identityManager.claimCommunityUpdate ( 10 );
                Iterator<IdentityData> il = cl.iterator();

                while ( il.hasNext() )
                {
                    IdentityData id = il.next();
                    CObj cr = new CObj();
                    cr.setType ( CObj.CON_REQ_COMMUNITIES );
                    cr.pushString ( CObj.CREATOR, id.getId() );
                    cr.pushNumber ( CObj.FIRSTNUM, id.getLastCommunityNumber() + 1 );
                    cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                    nonCommunityRequests.add ( cr );
                    gonext = true;
                    qsize++;
                }

            }

            //Membership updates
            if ( nonCommunityRequests.size() < MAX_PUB_REQUESTS )
            {
                List<IdentityData> ci = identityManager.claimMemberUpdate ( 10 );
                Iterator<IdentityData> il = ci.iterator();

                while ( il.hasNext() )
                {
                    IdentityData id = il.next();
                    CObj cr = new CObj();
                    cr.setType ( CObj.CON_REQ_MEMBERSHIPS );
                    cr.pushString ( CObj.CREATOR, id.getId() );
                    cr.pushNumber ( CObj.FIRSTNUM, id.getLastMembershipNumber() + 1 );
                    cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                    nonCommunityRequests.add ( cr );
                    qsize++;
                    gonext = true;
                }

            }

            //Membership updates
            if ( nonCommunityRequests.size() < MAX_PUB_REQUESTS )
            {
                List<IdentityData> ci = identityManager.claimSubUpdate ( 10 );
                Iterator<IdentityData> il = ci.iterator();

                while ( il.hasNext() )
                {
                    IdentityData id = il.next();
                    CObj cr = new CObj();
                    cr.setType ( CObj.CON_REQ_SUBS );
                    cr.pushString ( CObj.CREATOR, id.getId() );
                    cr.pushNumber ( CObj.FIRSTNUM, id.getLastSubNumber() + 1 );
                    cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                    nonCommunityRequests.add ( cr );
                    qsize++;
                    gonext = true;
                }

            }

            if ( nonCommunityRequests.size() < MAX_PUB_REQUESTS )
            {
                List<PrivateMsgIdentity> cl = identityManager.claimPrvtIdentUpdate ( 10 );
                Iterator<PrivateMsgIdentity> il = cl.iterator();

                while ( il.hasNext() )
                {
                    PrivateMsgIdentity id = il.next();
                    CObj cr = new CObj();
                    cr.setType ( CObj.CON_REQ_PRVIDENT );
                    cr.pushString ( CObj.CREATOR, id.getId() );
                    cr.pushNumber ( CObj.FIRSTNUM, id.getLastIdentNumber() + 1 );
                    cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                    nonCommunityRequests.add ( cr );
                    gonext = true;
                    qsize++;
                }

            }

            if ( nonCommunityRequests.size() < MAX_PUB_REQUESTS )
            {
                List<PrivateMsgIdentity> cl = identityManager.claimPrvtMsgUpdate ( 10 );
                Iterator<PrivateMsgIdentity> il = cl.iterator();

                while ( il.hasNext() )
                {
                    PrivateMsgIdentity id = il.next();
                    CObj cr = new CObj();
                    cr.setType ( CObj.CON_REQ_PRVMSG );
                    cr.pushString ( CObj.CREATOR, id.getId() );
                    cr.pushNumber ( CObj.FIRSTNUM, id.getLastMsgNumber() + 1 );
                    cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                    nonCommunityRequests.add ( cr );
                    gonext = true;
                    qsize++;
                }

            }

        }

        return gonext;

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
        long ct = System.currentTimeMillis();
        long cuttime = ct - MAX_TIME_COMMUNITY_ACTIVE_UNTIL_NEW_CONNECTION;

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

        Iterator<Entry<String, Long>> ai = currentActiveSubPrivRequests.entrySet().iterator();

        while ( ai.hasNext() )
        {
            Entry<String, Long> e = ai.next();

            if ( e.getValue() <= cuttime )
            {
                ai.remove();
            }

        }

        ai = currentActiveComRequests.entrySet().iterator();

        while ( ai.hasNext() )
        {
            Entry<String, Long> e = ai.next();

            if ( e.getValue() <= cuttime )
            {
                ai.remove();
            }

        }

        long cutoff = ct - MAX_TIME_IN_QUEUE;

        Iterator<Entry<String, Long>> ci = communityTime.entrySet().iterator();

        while ( ci.hasNext() )
        {
            Entry<String, Long> e = ci.next();

            if ( e.getValue() <= cutoff )
            {
                ci.remove();
                communityRequests.remove ( e.getKey() );
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

        Iterator<Entry<String, WeakReference<Set<String>>>> i2 = subCache.entrySet().iterator();

        while ( i2.hasNext() )
        {
            Entry<String, WeakReference<Set<String>>> e = i2.next();

            if ( e.getValue().get() == null )
            {
                i2.remove();
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
        // Attempt connections for subscribed communities
        long ct = System.currentTimeMillis();
        long cuttime = ct - MAX_TIME_COMMUNITY_ACTIVE_UNTIL_NEW_CONNECTION;
        Set<String> comids = new HashSet<String>();

        comids.addAll ( communityRequests.keySet() );
        comids.addAll ( subPushes.keySet() );

        Iterator<String> is = comids.iterator();

        while ( is.hasNext() && con < ATTEMPT_CONNECTIONS )
        {
            String comid = is.next();
            //First make sure we don't currently have a connection
            //actively servicing subscription updates for this community
            Long st = currentActiveComRequests.get ( comid );

            if ( st == null || st <= cuttime )
            {
                ConcurrentLinkedQueue<CObj> pushlist = subPushes.get ( comid );

                //Get destinations for pushes first.
                List<DestinationThread> dtlst = null;

                if ( pushlist != null )
                {
                    dtlst = new LinkedList<DestinationThread>();
                    Iterator<CObj> pi = pushlist.iterator();

                    while ( pi.hasNext() )
                    {
                        CObj cobj = pi.next();
                        String creator = cobj.getString ( CObj.CREATOR );

                        if ( creator != null )
                        {
                            DestinationThread dt = getMyDestinationThread ( myids, creator );

                            if ( dt != null && !dtlst.contains ( dt ) )
                            {
                                dtlst.add ( dt );
                            }

                        }

                    }

                }

                if ( dtlst == null || dtlst.size() == 0 )
                {
                    dtlst = findMyDestinationsForCommunity ( myids, comid );
                }

                CObjList subs = index.getSubscriptions ( comid, null );
                int ridx[] = randomList ( subs.size() );

                for ( int c0 = 0; c0 < subs.size() && con < ATTEMPT_CONNECTIONS; c0++ )
                {
                    try
                    {
                        CObj sub = subs.get ( ridx[c0] );
                        String creatorid = sub.getString ( CObj.CREATOR );

                        if ( creatorid != null )
                        {
                            CObj creator = index.getIdentity ( creatorid );

                            if ( creator != null )
                            {
                                Iterator<DestinationThread> dti = dtlst.iterator();

                                while ( dti.hasNext() && con < ATTEMPT_CONNECTIONS )
                                {
                                    DestinationThread dt = dti.next();

                                    if ( attemptConnection ( dt, creator, false, myids ) )
                                    {
                                        con++;
                                    }

                                }

                            }

                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

                subs.close();
            }

        }

        //======================================================
        //Attempt connections to private members of communities
        //First gather a list of all possible identities we might
        //want to connect to based for private communities based
        //on the privSubRequests.
        //attemptlist = identity > set of private communities
        Map<String, Set<String>> attemptlist = new HashMap<String, Set<String>>();
        //list of identities we can select from randomly
        List<String> memlst = new LinkedList<String>();

        for ( Entry<String, ConcurrentLinkedQueue<CObj>> e : privSubRequests.entrySet() )
        {
            for ( CObj co : e.getValue() )
            {
                String cr = co.getString ( CObj.CREATOR );

                if ( cr != null )
                {
                    memlst.add ( cr );
                    Set<String> cs = attemptlist.get ( cr );

                    if ( cs == null )
                    {
                        cs = new HashSet<String>();
                        attemptlist.put ( cr, cs );
                    }

                    cs.add ( e.getKey() );
                }

            }

        }

        //Now to randomly try to connect to an identity
        int rl[] = randomList ( memlst.size() );
        Map<String, List<DestinationThread>> comThreads = new HashMap<String, List<DestinationThread>>();

        for ( int c = 0; c < rl.length && con < ATTEMPT_CONNECTIONS ; c++ )
        {
            String memberid = memlst.get ( rl[c] );
            CObj identity = getIdentity ( memberid );
            Set<String> memcoms = attemptlist.get ( memberid );
            Iterator<String> it = memcoms.iterator();
            List<DestinationThread> dl = null;

            while ( it.hasNext() && dl == null )
            {
                String comid = it.next();
                List<DestinationThread> dlt = comThreads.get ( comid );

                if ( dlt == null )
                {
                    dlt = findAllMyDestinationsForCommunity ( myids, comid );
                    comThreads.put ( comid, dlt );
                }

                if ( dlt.size() > 0 )
                {
                    dl = dlt;
                }

            }

            if ( dl != null )
            {
                Iterator<DestinationThread> dit = dl.iterator();

                while ( dit.hasNext() && con < ATTEMPT_CONNECTIONS )
                {
                    DestinationThread dt = dit.next();

                    if ( attemptConnection ( dt, identity, false, myids ) )
                    {
                        con++;
                    }

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

    private Object nextCommunityRequest ( Set<String> subs )
    {
        Object n = null;
        long ct = System.currentTimeMillis();
        List<String> sl = new ArrayList<String>();
        sl.addAll ( subs );
        int r[] = randomList ( sl.size() );

        for ( int i = 0; i < r.length && n == null; i++ )
        {
            int ix = r[i];
            String sbs = sl.get ( ix );
            ConcurrentLinkedQueue<CObj> requests = communityRequests.get ( sbs );

            if ( requests != null )
            {
                if ( requests.size() == 0 )
                {
                    communityRequests.remove ( sbs );
                }

                else
                {
                    n = requests.poll();

                    if ( n != null )
                    {
                        communityTime.put ( sbs, ct );
                        currentActiveComRequests.put ( sbs, ct );

                    }

                }

            }

        }

        return n;
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

        if ( n == null )
        {
            int tnd = Utils.Random.nextInt ( 2 );

            if ( tnd == 0 )
            {
                n = nextCommunityRequest ( subs );
            }

            if ( n == null || tnd == 1 )
            {
                n = nonCommunityRequests.poll();
            }

        }

        if ( n == null )
        {
            n = nextCommunityRequest ( subs );
        }

        if ( n == null )
        {
            n = nonCommunityRequests.poll();
        }

        if ( n == null )
        {
            IdentityData id = identityManager.claimIdentityUpdate ( remotedest );

            if ( id != null )
            {
                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_IDENTITIES );
                n = cr;
            }

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
            log.info ( "MANAGER2 LOOP!!!!!!!!!!!!!!!!! 0" );

            if ( !stop )
            {
                removeStale();
                log.info ( "MANAGER2 LOOP!!!!!!!!!!!!!!!!! 1" );

                boolean g0 = procComQueue();
                log.info ( "MANAGER2 LOOP!!!!!!!!!!!!!!!!! 1.5" );

                if ( g0 )
                {
                    log.info ( "MANAGER2 LOOP!!!!!!!!!!!!!!!!! 3" );
                    sendRequestsNow();
                }

                if ( System.currentTimeMillis() >= nextdecode )
                {
                    log.info ( "MANAGER2 LOOP!!!!!!!!!!!!!!!!! 4" );
                    gatherPrivSubRequests();
                    decodeMemberships();
                    clearRecentConnections();
                    checkConnections();
                    nextdecode = System.currentTimeMillis() +
                                 DECODE_AND_NEW_CONNECTION_DELAY;
                }

            }

            log.info ( "MANAGER2 LOOP!!!!!!!!!!!!!!!!! 5" );
            delay();
            log.info ( "MANAGER2 LOOP!!!!!!!!!!!!!!!!! 6" );

        }

        log.info ( "MANAGER2 EXIT" );

    }

}
