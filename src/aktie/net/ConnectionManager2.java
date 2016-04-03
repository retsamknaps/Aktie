package aktie.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.IdentityData;
import aktie.data.RequestFile;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.user.IdentityManager;
import aktie.user.RequestFileHandler;

public class ConnectionManager2
{

    public static long MAX_TIME_IN_QUEUE = 60L * 60L * 1000L;
    public static int QUEUE_DEPTH_COM = 100;
    public static int QUEUE_DEPTH_FILE = 100;

    private Index index;
    private RequestFileHandler fileHandler;
    private IdentityManager identityManager;

    private long lastFileUpdate = Long.MIN_VALUE;
    private LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>> fileRequests;

    //Community data requests.  Key is community id
    //Requests only sent to subscribers of communities
    private ConcurrentMap<String, ConcurrentLinkedQueue<CObj>> communityRequests;

    //Membership update requests for private communites
    //Only members can be sent request.  For public communities membership
    //requests can be sent to anyone
    private ConcurrentMap<String, ConcurrentLinkedQueue<CObj>> subPrivRequests;

    //Requests that can be sent to any node
    private ConcurrentLinkedQueue<CObj> nonCommunityRequests;

    //The time the community requests were added to the queue
    private ConcurrentMap<String, Long> communityTime;

    //The time the file requests were added to the queue
    private ConcurrentMap<RequestFile, Long> fileTime;

    public ConnectionManager2()
    {
        fileRequests =
            new LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>>();
        communityRequests = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CObj>>();
        subPrivRequests = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CObj>>();
        nonCommunityRequests = new ConcurrentLinkedQueue<CObj>();
        communityTime = new ConcurrentHashMap<String, Long>();
        fileTime = new ConcurrentHashMap<RequestFile, Long>();
    }

    public long getLastFileUpdate()
    {
        return lastFileUpdate;
    }

    private int calculateNonFileQueueSize()
    {
        int sz = 0;
        List<String> kl = new LinkedList<String>();
        kl.addAll ( communityRequests.keySet() );

        for ( String k : kl )
        {
            ConcurrentLinkedQueue<CObj> l = communityRequests.get ( k );
            sz += l.size();
        }

        kl.clear();
        kl.addAll ( subPrivRequests.keySet() );

        for ( String k : kl )
        {
            ConcurrentLinkedQueue<CObj> l = subPrivRequests.get ( k );
            sz += l.size();
        }

        sz += nonCommunityRequests.size();
        return sz;
    }

    private void procComQueue()
    {
        int qsize = calculateNonFileQueueSize();
        int lastsize = 0;

        while ( qsize < QUEUE_DEPTH_COM && lastsize != qsize )
        {
            lastsize = qsize;

            //Identity update is always sent first to any node upon
            //initial connection.  No need to enqueue identity updates
            //here.

            //Community updates
            IdentityData id = identityManager.claimCommunityUpdate();

            if ( id != null )
            {
                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_COMMUNITIES );
                cr.pushString ( CObj.CREATOR, id.getId() );
                cr.pushNumber ( CObj.FIRSTNUM, id.getLastCommunityNumber() + 1 );
                cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                nonCommunityRequests.add ( cr );
                qsize++;
            }

            //Membership updates
            id = identityManager.claimMemberUpdate();

            if ( id != null )
            {
                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_MEMBERSHIPS );
                cr.pushString ( CObj.CREATOR, id.getId() );
                cr.pushNumber ( CObj.FIRSTNUM, id.getLastMembershipNumber() + 1 );
                cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                nonCommunityRequests.add ( cr );
                qsize++;
            }

            //Subscription updates
            CommunityMember cm = identityManager.claimSubUpdate();

            if ( cm != null )
            {
                CObj comi = index.getCommunity ( cm.getCommunityId() );

                if ( comi != null )
                {
                    CObj cr = new CObj();
                    cr.setType ( CObj.CON_REQ_SUBS );
                    cr.pushString ( CObj.COMMUNITYID, cm.getCommunityId() );
                    cr.pushNumber ( CObj.FIRSTNUM, cm.getLastSubscriptionNumber() + 1L );
                    cr.pushString ( CObj.CREATOR, cm.getMemberId() );
                    String pub = comi.getString ( CObj.SCOPE );

                    if ( CObj.SCOPE_PUBLIC.equals ( pub ) )
                    {
                        //If public it does on the nonCommunityRequests list
                        nonCommunityRequests.add ( cr );
                    }

                    else
                    {
                        //If private it goes on the subPrivRequests to go to members
                        ConcurrentLinkedQueue<CObj> clst = subPrivRequests.get ( cm.getCommunityId() );

                        if ( clst == null )
                        {
                            clst = new ConcurrentLinkedQueue<CObj>();
                            subPrivRequests.put ( cm.getCommunityId(), clst );
                        }

                        clst.add ( cr );
                    }

                    qsize++;

                }

            }

            //Has file updates
            cm = identityManager.claimHasFileUpdate();

            if ( cm != null )
            {
                ConcurrentLinkedQueue<CObj> clst = communityRequests.get ( cm.getCommunityId() );

                if ( clst == null )
                {
                    clst = new ConcurrentLinkedQueue<CObj>();
                    communityRequests.put ( cm.getCommunityId(), clst );
                }

                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_HASFILE );
                cr.pushString ( CObj.COMMUNITYID, cm.getCommunityId() );
                cr.pushString ( CObj.CREATOR, cm.getMemberId() );
                cr.pushNumber ( CObj.FIRSTNUM, cm.getLastFileNumber() + 1 );
                cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                clst.add ( cr );
                qsize++;

            }

            //Post updates
            cm = identityManager.claimPostUpdate();

            if ( cm != null )
            {
                ConcurrentLinkedQueue<CObj> clst = communityRequests.get ( cm.getCommunityId() );

                if ( clst == null )
                {
                    clst = new ConcurrentLinkedQueue<CObj>();
                    communityRequests.put ( cm.getCommunityId(), clst );
                }

                CObj cr = new CObj();
                cr.setType ( CObj.CON_REQ_POSTS );
                cr.pushString ( CObj.COMMUNITYID, cm.getCommunityId() );
                cr.pushString ( CObj.CREATOR, cm.getMemberId() );
                cr.pushNumber ( CObj.FIRSTNUM, cm.getLastPostNumber() + 1 );
                cr.pushNumber ( CObj.LASTNUM, Long.MAX_VALUE );
                clst.add ( cr );
                qsize++;

            }

        }

    }

    private void procFileQueue()
    {
        //Get the prioritized list of files
        LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>> nlst =
            new LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>>();
        ConcurrentMap<RequestFile, Long> nt = new ConcurrentHashMap<RequestFile, Long>();

        List<RequestFile> flst = fileHandler.listRequestFilesNE ( RequestFile.COMPLETE, Integer.MAX_VALUE );

        for ( RequestFile rf : flst )
        {
            ConcurrentLinkedQueue<CObj> fl = null;

            synchronized ( fileRequests )
            {
                fl = fileRequests.get ( rf );
            }

            if ( fl == null )
            {
                fl = new ConcurrentLinkedQueue<CObj>();
            }

            Long tm = fileTime.get ( rf );

            if ( tm == null )
            {
                tm = System.currentTimeMillis();
            }

            nlst.put ( rf, fl );
            nt.put ( rf, tm );

            if ( fl.size() < QUEUE_DEPTH_FILE )
            {
                if ( rf.getState() == RequestFile.REQUEST_FRAG_LIST )
                {
                    if ( fileHandler.claimFileListClaim ( rf ) )
                    {
                        CObj cr = new CObj();
                        cr.setType ( CObj.CON_REQ_FRAGLIST );
                        cr.pushString ( CObj.COMMUNITYID, rf.getCommunityId() );
                        cr.pushString ( CObj.FILEDIGEST, rf.getWholeDigest() );
                        cr.pushString ( CObj.FRAGDIGEST, rf.getFragmentDigest() );
                        fl.add ( cr );
                    }

                }

                if ( rf.getState() == RequestFile.REQUEST_FRAG_LIST_SNT &&
                        rf.getLastRequest() <= ( System.currentTimeMillis() - 60L * 60L * 1000L ) )
                {
                    fileHandler.setReRequestList ( rf );
                }

                if ( rf.getState() == RequestFile.REQUEST_FRAG )
                {

                    //Find the fragments that haven't been requested yet.
                    CObjList cl = index.getFragmentsToRequest ( rf.getCommunityId(),
                                  rf.getWholeDigest(), rf.getFragmentDigest() );

                    //There are none.. reset those requested some time ago.
                    if ( cl.size() == 0 )
                    {
                        cl.close();
                        cl = index.getFragmentsToReset ( rf.getCommunityId(),
                                                         rf.getWholeDigest(), rf.getFragmentDigest() );

                        long backtime = System.currentTimeMillis() - 20L * 60L * 1000L;

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
                        index.forceNewSearcher();
                        //Get the new list of fragments to request after resetting
                        cl = index.getFragmentsToRequest ( rf.getCommunityId(),
                                                           rf.getWholeDigest(), rf.getFragmentDigest() );

                    }

                    for ( int c = 0; c < cl.size() && fl.size() < QUEUE_DEPTH_FILE; c++ )
                    {
                        try
                        {
                            CObj co = cl.get ( c );
                            co.pushPrivate ( CObj.COMPLETE, "req" );
                            co.pushPrivateNumber ( CObj.LASTUPDATE, System.currentTimeMillis() );
                            index.index ( co );
                            index.forceNewSearcher();
                            CObj sr = new CObj();
                            sr.setType ( CObj.CON_REQ_FRAG );
                            sr.pushString ( CObj.COMMUNITYID, co.getString ( CObj.COMMUNITYID ) );
                            sr.pushString ( CObj.FILEDIGEST, co.getString ( CObj.FILEDIGEST ) );
                            sr.pushString ( CObj.FRAGDIGEST, co.getString ( CObj.FRAGDIGEST ) );
                            sr.pushString ( CObj.FRAGDIG, co.getString ( CObj.FRAGDIG ) );
                            fl.add ( sr );
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

        fileRequests = nlst;
        fileTime = nt;
        lastFileUpdate++;
    }

    private void removeStale()
    {
        long cutoff = System.currentTimeMillis() - MAX_TIME_IN_QUEUE;

        for ( Entry<String, Long> e : communityTime.entrySet() )
        {
            if ( e.getValue() <= cutoff )
            {
                communityRequests.remove ( e.getKey() );
                subPrivRequests.remove ( e.getKey() );
            }

        }

        for ( Entry<RequestFile, Long> e : fileTime.entrySet() )
        {
            if ( e.getValue() <= cutoff )
            {
                fileRequests.remove ( e.getKey() );
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

                if ( co != null )
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
        if ( hasfiles == null )
        {
            return null;
        }

        Object n = null;

        List<RequestFile> rls = new LinkedList<RequestFile>();

        synchronized ( fileRequests )
        {
            rls.addAll ( fileRequests.keySet() );
        }

        Iterator<RequestFile> i = rls.iterator();

        while ( i.hasNext() && n == null )
        {
            RequestFile r = i.next();

            if ( hasfiles.contains ( r ) )
            {
                ConcurrentLinkedQueue<CObj> rl = null;

                synchronized ( fileRequests )
                {
                    rl = fileRequests.get ( r );
                }

                if ( rl != null )
                {
                    n = rl.poll();

                    if ( n != null )
                    {
                        fileTime.put ( r, System.currentTimeMillis() );
                    }

                }

            }

        }

        return n;
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

    private Object nextPrivSubRequest ( Set<String> mems )
    {
        Object n = null;
        List<String> sl = new ArrayList<String>();
        sl.addAll ( mems );
        int r[] = randomList ( mems.size() );

        for ( int i = 0; i < r.length && n == null; i++ )
        {
            int ix = r[i];
            String sbs = sl.get ( ix );
            ConcurrentLinkedQueue<CObj> requests = subPrivRequests.get ( sbs );

            if ( requests != null )
            {
                if ( requests.size() == 0 )
                {
                    subPrivRequests.remove ( sbs );
                }

                else
                {
                    n = requests.poll();

                    if ( n != null )
                    {
                        communityTime.put ( sbs, System.currentTimeMillis() );
                    }

                }

            }

        }

        return n;
    }


    private Object nextCommunityRequest ( Set<String> subs )
    {
        Object n = null;
        List<String> sl = new ArrayList<String>();
        sl.addAll ( subs );
        int r[] = randomList ( subs.size() );

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
                        communityTime.put ( sbs, System.currentTimeMillis() );
                    }

                }

            }

        }

        return n;
    }

    public Object nextNonFile ( String localdest, String remotedest, Set<String> members, Set<String> subs )
    {
        Object n = null;

        int tnd = Utils.Random.nextInt ( 3 );

        if ( tnd == 0 )
        {
            n = nextCommunityRequest ( subs );
        }

        if ( n == null || tnd == 1 )
        {
            n = nextPrivSubRequest ( members );
        }

        if ( n == null || tnd == 2 )
        {
            n = nonCommunityRequests.poll();
        }

        if ( n == null )
        {
            n = nextCommunityRequest ( subs );
        }

        if ( n == null )
        {
            n = nextPrivSubRequest ( members );
        }

        if ( n == null )
        {
            n = nonCommunityRequests.poll();
        }

        return n;
    }

}
