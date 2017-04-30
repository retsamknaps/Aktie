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
import aktie.utils.CObjHelper;

public class ConnectionFileManager implements Runnable
{
    public static long REREQUESTLISTAFTER = 60L * 60L * 1000L;
    public static long REREQUESTFRAGSAFTER = 2L * 60L * 60L * 1000L;

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
        fileRequests = new LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>>();
        index = i;
        fileHandler = r;
        Thread t = new Thread ( this );
        t.setDaemon ( true );
        t.start();
    }

    public List<RequestFile> getRequestFiles()
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

    /**
        Get the next file to request from a connected remote destination.
        @param localDestination The local destination that requests the files.
        @param remoteDestination The remote destination to which the local destination is connected
        @param remoteRequestFiles The files available at the remote destination from which to select the next file to request.
        @return The next file to request or null if there is no file of interest is available.
    */
    public CObj nextFile ( String localDestination, String remoteDestination, Set<RequestFile> remoteRequestFiles )
    {

        if ( Level.INFO.equals ( log.getLevel() ) )
        {
            //log.info("nextFile: " + );
        }

        if ( remoteRequestFiles == null )
        {
            return null;
        }

        CObj nextFile = null;

        // Retrieve the request files of the local destination (identity) from the global list
        List<RequestFile> requestFilesOfLocalDestination = new LinkedList<RequestFile>();

        synchronized ( fileRequests )
        {
            for ( RequestFile requestFile : fileRequests.keySet() )
            {
                if ( requestFile.getRequestId().equals ( localDestination ) )
                {
                    requestFilesOfLocalDestination.add ( requestFile );
                }

            }

        }

        // Filter out the request files that are available at the remote destination
        for ( RequestFile requestFile : requestFilesOfLocalDestination )
        {
            if ( remoteRequestFiles.contains ( requestFile ) )
            {
                ConcurrentLinkedQueue<CObj> remoteFilesOfInterest = null;

                synchronized ( fileRequests )
                {
                    remoteFilesOfInterest = fileRequests.get ( requestFile );
                }

                if ( remoteFilesOfInterest != null )
                {
                    nextFile = remoteFilesOfInterest.poll();

                    if ( nextFile != null )
                    {
                        fileTime.put ( requestFile, System.currentTimeMillis() );

                        // HasPart
                        // In case that we are requesting a fragment, check if we would to request it
                        // from a complete file or a part file. In case that we would request from a
                        // part file, figure out if the desired fragment is actually available at the
                        // remote destination. If not, proceed with the next item in the queue.
                        if ( nextFile.getType().equals ( CObj.CON_REQ_FRAG ) )
                        {
                            String fileDigest = nextFile.getString ( CObj.FILEDIGEST );
                            String fragDigest = nextFile.getString ( CObj.FRAGDIGEST );
                            String fragDig = nextFile.getString ( CObj.FRAGDIG );

                            if ( fileDigest != null && fragDigest != null && fragDig != null )
                            {
                                CObjList hasParts = index.getPartFiles ( remoteDestination, fileDigest, fragDigest );

                                // If there is no part file known, the remote destination has a complete file.
                                // Hence, we should be able to download an arbitrary fragment of this file.
                                if ( hasParts.size() > 0 )
                                {
                                    hasParts.close();
                                    break;
                                }

                                String hasPartPayload;

                                try
                                {
                                    hasPartPayload = hasParts.get ( 0 ).getString ( CObj.PAYLOAD );
                                }

                                catch ( IOException e )
                                {
                                    hasParts.close();
                                    continue;
                                }

                                hasParts.close();

                                if ( hasPartPayload == null )
                                {
                                    continue;
                                }

                                // Otherwise, the remote destination only has some fragments and we need to check
                                // whether we can get this particular fragment from the remote destination.
                                CObj fragment = index.getFragment ( fileDigest, fragDigest, fragDig );

                                if ( fragment != null )
                                {
                                    Long fragOffset = fragment.getNumber ( CObj.FRAGOFFSET );

                                    if ( fragOffset == null )
                                    {
                                        continue;
                                    }

                                    long fileSize = requestFile.getFileSize();
                                    long totalFragments = requestFile.getFragsTotal();
                                    long fragSize = requestFile.getFragSize();

                                    int fragmentIndex = CObjHelper.calculateFragmentIndex ( fileSize, totalFragments, fragOffset, fragSize );

                                    // If the fragment is know to be available, we should be able to get it.
                                    if ( CObjHelper.hasPartPayloadListsFragment ( hasPartPayload, fragmentIndex ) )
                                    {
                                        break;
                                    }

                                    continue;
                                }

                            }

                        }

                        // TODO: Correct me if wrong, but we should have breaked here in the code as it was before, shouldn't we?
                        // Because why should we walk through all the queue, emptying it, if we already found something that is not null?
                        break;
                    }

                }

            }

        }

        return nextFile;
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
        boolean goNext = false;
        //Get the prioritized list of files
        LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>> newFileRequests = new LinkedHashMap<RequestFile, ConcurrentLinkedQueue<CObj>>();
        ConcurrentMap<RequestFile, Long> nt = new ConcurrentHashMap<RequestFile, Long>();

        List<RequestFile> requestFileList = fileHandler.listRequestFilesNE ( RequestFile.COMPLETE, Integer.MAX_VALUE );

        if ( log.isLoggable ( Level.INFO ) )
        {
            log.info ( "procFileQueue: " + requestFileList.size() );
        }

        for ( RequestFile requestFile : requestFileList )
        {
            ConcurrentLinkedQueue<CObj> requests = null;

            synchronized ( fileRequests )
            {
                requests = fileRequests.get ( requestFile );
            }

            if ( requests == null )
            {
                requests = new ConcurrentLinkedQueue<CObj>();
            }

            if ( log.isLoggable ( Level.INFO ) )
            {
                log.info ( "procFileQueue: " + requestFile.getLocalFile() + " num in queue: " + requests.size() );
            }

            Long lastRequestTime = fileTime.get ( requestFile );

            if ( lastRequestTime == null )
            {
                lastRequestTime = System.currentTimeMillis();
            }

            newFileRequests.put ( requestFile, requests );
            nt.put ( requestFile, lastRequestTime );

            if ( requests.size() < ConnectionManager2.QUEUE_DEPTH_FILE )
            {

                if ( log.isLoggable ( Level.INFO ) )
                {
                    log.info ( "procFileQueue: state: " + requestFile.getState() );
                }

                if ( requestFile.getState() == RequestFile.REQUEST_FRAG_LIST )
                {
                    if ( fileHandler.claimFileListClaim ( requestFile ) )
                    {
                        if ( log.isLoggable ( Level.INFO ) )
                        {
                            log.info ( "procFileQueue: state: request file list: " + requestFile.getLocalFile() );
                        }

                        CObj cr = new CObj();
                        cr.setType ( CObj.CON_REQ_FRAGLIST );
                        cr.pushString ( CObj.COMMUNITYID, requestFile.getCommunityId() );
                        cr.pushString ( CObj.FILEDIGEST, requestFile.getWholeDigest() );
                        cr.pushString ( CObj.FRAGDIGEST, requestFile.getFragmentDigest() );
                        requests.add ( cr );
                        goNext = true;
                    }

                }

                if ( requestFile.getState() == RequestFile.REQUEST_FRAG_LIST_SNT &&
                        requestFile.getLastRequest() <= ( System.currentTimeMillis() - REREQUESTLISTAFTER ) )
                {
                    log.info ( "procFileQueue: re-request file list " + requestFile.getLocalFile() );
                    //Check if the fragment request is still in the queue
                    Iterator<CObj> fi = requests.iterator();
                    boolean fnd = false;

                    while ( fi.hasNext() && !fnd )
                    {
                        CObj c = fi.next();

                        if ( CObj.CON_REQ_FRAGLIST.equals ( c.getType() ) )
                        {
                            String comid = c.getString ( CObj.COMMUNITYID );
                            String wdig = c.getString ( CObj.FILEDIGEST );
                            String pdig = c.getString ( CObj.FRAGDIGEST );

                            if ( comid.equals ( requestFile.getCommunityId() ) &&
                                    wdig.equals ( requestFile.getWholeDigest() ) &&
                                    pdig.equals ( requestFile.getFragmentDigest() ) )
                            {
                                fnd = true;
                            }

                        }

                    }

                    if ( !fnd )
                    {
                        log.info ( "procFileQueue: really re-request file list " + requestFile.getLocalFile() );
                        fileHandler.setReRequestList ( requestFile );
                    }

                }

                if ( requestFile.getState() == RequestFile.REQUEST_FRAG )
                {

                    //Find the fragments that haven't been requested yet.
                    CObjList fragmentsToRequest = index.getFragmentsToRequest ( requestFile.getCommunityId(),
                                                  requestFile.getWholeDigest(), requestFile.getFragmentDigest() );

                    log.info ( "procFileQueue: request fragments " + fragmentsToRequest.size() );

                    //There are none.. reset those requested some time ago.
                    if ( fragmentsToRequest.size() == 0 )
                    {
                        CObjList fragmentsToReset = index.getFragmentsToReset ( requestFile.getCommunityId(),
                                                    requestFile.getWholeDigest(), requestFile.getFragmentDigest() );

                        long backtime = System.currentTimeMillis() -
                                        ( REREQUESTFRAGSAFTER );

                        log.info ( "procFileQueue: re-request fragments " + fragmentsToReset.size() );

                        for ( int i = 0; i < fragmentsToReset.size(); i++ )
                        {
                            try
                            {
                                //Set to false, so that we'll request again.
                                //Ones already complete won't be reset.
                                CObj co = fragmentsToReset.get ( i );
                                Long lt = co.getPrivateNumber ( CObj.LASTUPDATE );

                                //Check lastupdate so we don't request it back to
                                //back when there's only one fragment for a file.
                                if ( lt == null || lt < backtime )
                                {
                                    Iterator<CObj> fi = requests.iterator();
                                    boolean found = false;

                                    String comid = co.getString ( CObj.COMMUNITYID );
                                    String filed = co.getString ( CObj.FILEDIGEST );
                                    String fragt = co.getString ( CObj.FRAGDIGEST );
                                    String fragd = co.getString ( CObj.FRAGDIG );

                                    if ( comid != null && filed != null && fragt != null &&
                                            fragd != null )
                                    {

                                        while ( fi.hasNext() && !found )
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
                                                    found = true;
                                                }

                                            }

                                        }

                                        if ( !found )
                                        {
                                            co.pushPrivate ( CObj.COMPLETE, CObj.FALSE );
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

                        fragmentsToReset.close();

                        fragmentsToRequest.close();

                        index.forceNewSearcher();
                        //Get the new list of fragments to request after resetting
                        fragmentsToRequest = index.getFragmentsToRequest ( requestFile.getCommunityId(),
                                             requestFile.getWholeDigest(), requestFile.getFragmentDigest() );

                    }

                    boolean newsearcher = false;

                    log.info ( "procFileQueue: request fragments: " + fragmentsToRequest.size() + " queue size: " + requests.size() );

                    for ( int c = 0; c < fragmentsToRequest.size() &&
                            requests.size() < ConnectionManager2.QUEUE_DEPTH_FILE; c++ )
                    {
                        try
                        {

                            CObj fragment = fragmentsToRequest.get ( c );
                            log.info ( "procFileQueue: request fragment: " + c );

                            fragment.pushPrivate ( CObj.COMPLETE, CObj.REQUEST );
                            fragment.pushPrivateNumber ( CObj.LASTUPDATE, System.currentTimeMillis() );
                            index.index ( fragment );
                            newsearcher = true;
                            CObj sr = new CObj();
                            sr.setType ( CObj.CON_REQ_FRAG );
                            sr.pushString ( CObj.COMMUNITYID, fragment.getString ( CObj.COMMUNITYID ) );
                            sr.pushString ( CObj.FILEDIGEST, fragment.getString ( CObj.FILEDIGEST ) );
                            sr.pushString ( CObj.FRAGDIGEST, fragment.getString ( CObj.FRAGDIGEST ) );
                            sr.pushString ( CObj.FRAGDIG, fragment.getString ( CObj.FRAGDIG ) );
                            requests.add ( sr );
                            goNext = true;
                        }

                        catch ( IOException e )
                        {
                            e.printStackTrace();
                        }

                    }

                    fragmentsToRequest.close();

                    if ( newsearcher )
                    {
                        index.forceNewSearcher();
                    }

                }

            }

        }

        fileRequests = newFileRequests;
        fileTime = nt;
        lastFileUpdate++;
        return goNext;
    }

    public void bumpUpdate()
    {
        lastFileUpdate++;
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
