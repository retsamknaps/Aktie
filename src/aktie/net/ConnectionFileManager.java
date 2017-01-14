package aktie.net;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.RequestFile;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.user.RequestFileHandler;

public class ConnectionFileManager implements Runnable
{

    Logger log = Logger.getLogger ( "aktie" );

    private boolean stop = false;

    private long lastFileUpdate = Long.MIN_VALUE + 1;
    private LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>> fileRequests;

    private Index index;
    private RequestFileHandler fileHandler;

    //The time the file requests were added to the queue
    private ConcurrentMap<RequestFile, Long> fileTime;

    public ConnectionFileManager ( HH2Session s, Index i, RequestFileHandler r )
    {
        fileTime = new ConcurrentHashMap<RequestFile, Long>();
        fileRequests =
            new LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>>();
        index = i;
        fileHandler = r;
        Thread t = new Thread ( this );
        t.setDaemon ( true );
        t.start();
    }

    public List<RequestFile> getRequestFile()
    {
        List<RequestFile> r = new LinkedList<RequestFile>();

        synchronized ( fileRequests )
        {
            r.addAll ( fileRequests.keySet() );
        }

        return r;
    }

    private void removeStale()
    {
        long ct = System.currentTimeMillis();
        long cutoff = ct - ConnectionManager2.MAX_TIME_IN_QUEUE;

        Iterator<Entry<RequestFile, Long>> fi = fileTime.entrySet().iterator();

        while ( fi.hasNext() )
        {
            Entry<RequestFile, Long> e = fi.next();

            if ( e.getValue() <= cutoff )
            {
                fi.remove();
                fileRequests.remove ( e.getKey() );
            }

        }

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
            for ( RequestFile rf : fileRequests.keySet() )
            {
                if ( rf.getRequestId().equals ( localdest ) )
                {
                    rls.add ( rf );
                }

            }

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

    public long getLastFileUpdate()
    {
        return lastFileUpdate;
    }

    public synchronized void stop()
    {
        stop = true;

        notifyAll();
    }

    private boolean procFileQueue()
    {
        boolean gonext = false;
        //Get the prioritized list of files
        LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>> nlst =
            new LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>>();
        ConcurrentMap<RequestFile, Long> nt = new ConcurrentHashMap<RequestFile, Long>();

        List<RequestFile> flst = fileHandler.listRequestFilesNE ( RequestFile.COMPLETE, Integer.MAX_VALUE );

        if ( log.isLoggable ( Level.INFO ) )
        {
            log.info ( "procFileQueue: " + flst.size() );
        }

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

            if ( log.isLoggable ( Level.INFO ) )
            {
                log.info ( "procFileQueue: " + rf.getLocalFile() + " num in queue: " + fl.size() );
            }

            Long tm = fileTime.get ( rf );

            if ( tm == null )
            {
                tm = System.currentTimeMillis();
            }

            nlst.put ( rf, fl );
            nt.put ( rf, tm );

            if ( fl.size() < ConnectionManager2.QUEUE_DEPTH_FILE )
            {

                if ( log.isLoggable ( Level.INFO ) )
                {
                    log.info ( "procFileQueue: state: " + rf.getState() );
                }

                if ( rf.getState() == RequestFile.REQUEST_FRAG_LIST )
                {
                    if ( fileHandler.claimFileListClaim ( rf ) )
                    {
                        if ( log.isLoggable ( Level.INFO ) )
                        {
                            log.info ( "procFileQueue: state: request file list: " + rf.getLocalFile() );
                        }

                        CObj cr = new CObj();
                        cr.setType ( CObj.CON_REQ_FRAGLIST );
                        cr.pushString ( CObj.COMMUNITYID, rf.getCommunityId() );
                        cr.pushString ( CObj.FILEDIGEST, rf.getWholeDigest() );
                        cr.pushString ( CObj.FRAGDIGEST, rf.getFragmentDigest() );
                        fl.add ( cr );
                        gonext = true;
                    }

                }

                if ( rf.getState() == RequestFile.REQUEST_FRAG_LIST_SNT &&
                        rf.getLastRequest() <= ( System.currentTimeMillis() - 60L * 60L * 1000L ) )
                {
                    log.info ( "procFileQueue: re-request file list " + rf.getLocalFile() );
                    //Check if the fragment request is still in the queue
                    Iterator<CObj> fi = fl.iterator();
                    boolean fnd = false;

                    while ( fi.hasNext() && !fnd )
                    {
                        CObj c = fi.next();

                        if ( CObj.CON_REQ_FRAGLIST.equals ( c.getType() ) )
                        {
                            String comid = c.getString ( CObj.COMMUNITYID );
                            String wdig = c.getString ( CObj.FILEDIGEST );
                            String pdig = c.getString ( CObj.FRAGDIGEST );

                            if ( comid.equals ( rf.getCommunityId() ) &&
                                    wdig.equals ( rf.getWholeDigest() ) &&
                                    pdig.equals ( rf.getFragmentDigest() ) )
                            {
                                fnd = true;
                            }

                        }

                    }

                    if ( !fnd )
                    {
                        log.info ( "procFileQueue: really re-request file list " + rf.getLocalFile() );
                        fileHandler.setReRequestList ( rf );
                    }

                }

                if ( rf.getState() == RequestFile.REQUEST_FRAG )
                {

                    //Find the fragments that haven't been requested yet.
                    CObjList cl = index.getFragmentsToRequest ( rf.getCommunityId(),
                                  rf.getWholeDigest(), rf.getFragmentDigest() );

                    log.info ( "procFileQueue: request fragments " + cl.size() );

                    //There are none.. reset those requested some time ago.
                    if ( cl.size() == 0 )
                    {
                        cl.close();
                        cl = index.getFragmentsToReset ( rf.getCommunityId(),
                                                         rf.getWholeDigest(), rf.getFragmentDigest() );

                        long backtime = System.currentTimeMillis() -
                                        ( 2L * 60L * 60L * 1000L );

                        log.info ( "procFileQueue: re-request fragments " + cl.size() );

                        for ( int ct = 0; ct < cl.size(); ct++ )
                        {
                            try
                            {
                                //Set to false, so that we'll request again.
                                //Ones already complete won't be reset.
                                CObj co = cl.get ( ct );
                                Long lt = co.getPrivateNumber ( CObj.LASTUPDATE );

                                //Check lastupdate so we don't request it back to
                                //back when there's only one fragment for a file.
                                if ( lt == null || lt < backtime )
                                {
                                    Iterator<CObj> fi = fl.iterator();
                                    boolean fnd = false;

                                    String comid = co.getString ( CObj.COMMUNITYID );
                                    String filed = co.getString ( CObj.FILEDIGEST );
                                    String fragt = co.getString ( CObj.FRAGDIGEST );
                                    String fragd = co.getString ( CObj.FRAGDIG );

                                    if ( comid != null && filed != null && fragt != null &&
                                            fragd != null )
                                    {

                                        while ( fi.hasNext() && !fnd )
                                        {
                                            CObj c = fi.next();

                                            if ( CObj.CON_REQ_FRAG.equals ( c.getType() ) )
                                            {
                                                String ccomid = c.getString ( CObj.COMMUNITYID );
                                                String cfiled = c.getString ( CObj.FILEDIGEST );
                                                String cfragt = c.getString ( CObj.FRAGDIGEST );
                                                String cfragd = c.getString ( CObj.FRAGDIG );

                                                if ( comid.equals ( ccomid ) &&
                                                        filed.equals ( cfiled ) &&
                                                        fragt.equals ( cfragt ) &&
                                                        fragd.equals ( cfragd ) )
                                                {
                                                    fnd = true;
                                                }

                                            }

                                        }

                                        if ( !fnd )
                                        {
                                            co.pushPrivate ( CObj.COMPLETE, "false" );
                                            index.index ( co );
                                        }

                                    }

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

                    boolean newsearcher = false;

                    log.info ( "procFileQueue: request fragments: " + cl.size() + " queue size: " + fl.size() );

                    for ( int c = 0; c < cl.size() &&
                            fl.size() < ConnectionManager2.QUEUE_DEPTH_FILE; c++ )
                    {
                        try
                        {

                            CObj co = cl.get ( c );
                            log.info ( "procFileQueue: request fragment: " + c );

                            co.pushPrivate ( CObj.COMPLETE, "req" );
                            co.pushPrivateNumber ( CObj.LASTUPDATE, System.currentTimeMillis() );
                            index.index ( co );
                            newsearcher = true;
                            CObj sr = new CObj();
                            sr.setType ( CObj.CON_REQ_FRAG );
                            sr.pushString ( CObj.COMMUNITYID, co.getString ( CObj.COMMUNITYID ) );
                            sr.pushString ( CObj.FILEDIGEST, co.getString ( CObj.FILEDIGEST ) );
                            sr.pushString ( CObj.FRAGDIGEST, co.getString ( CObj.FRAGDIGEST ) );
                            sr.pushString ( CObj.FRAGDIG, co.getString ( CObj.FRAGDIG ) );
                            fl.add ( sr );
                            gonext = true;
                        }

                        catch ( IOException e )
                        {
                            e.printStackTrace();
                        }

                    }

                    cl.close();

                    if ( newsearcher )
                    {
                        index.forceNewSearcher();
                    }

                }

            }

        }

        fileRequests = nlst;
        fileTime = nt;
        lastFileUpdate++;
        return gonext;
    }

    private synchronized void delay()
    {
        try
        {
            wait ( ConnectionManager2.REQUEST_UPDATE_DELAY );
        }

        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

    }

    @Override
    public void run()
    {
        if ( log.isLoggable ( Level.INFO ) )
        {
            log.info ( "STARTING CONNECTION FILEMANAGER" );
        }

        while ( !stop )
        {
            if ( log.isLoggable ( Level.INFO ) )
            {
                log.info ( "FILEMANAGER LOOP!!!!!!!!!!!!!!!!! 0" );
            }

            if ( !stop )
            {
                removeStale();

                if ( log.isLoggable ( Level.INFO ) )
                {
                    log.info ( "FILEMANAGER LOOP!!!!!!!!!!!!!!!!! 1" );
                }

                procFileQueue();

                if ( log.isLoggable ( Level.INFO ) )
                {
                    log.info ( "FILEMANAGER LOOP!!!!!!!!!!!!!!!!! 2" );
                }

                delay();

                if ( log.isLoggable ( Level.INFO ) )
                {
                    log.info ( "FILEMANAGER LOOP!!!!!!!!!!!!!!!!! 6" );
                }

            }

        }

        log.info ( "FILEMANAGER EXIT" );

    }


}
