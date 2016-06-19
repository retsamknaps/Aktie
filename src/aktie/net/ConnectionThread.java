package aktie.net;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.crypto.digests.RIPEMD256Digest;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONObject;

import aktie.BatchProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.RequestFile;
import aktie.gui.GuiCallback;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.json.CleanParser;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;
import aktie.user.RequestFileHandler;
import aktie.utils.HasFileCreator;

public class ConnectionThread implements Runnable, GuiCallback
{
    Logger log = Logger.getLogger ( "aktie" );

    public static int MAXQUEUESIZE = 1000; //Long lists should be in CObjList each one could have open indexreader!
    public static long GUIUPDATEPERIOD = 10L * 1000L; // 10 seconds

    private boolean stop;
    private boolean fileOnly;
    private Connection con;
    private BatchProcessor preprocProcessor;
    private BatchProcessor inProcessor;
    private ConcurrentLinkedQueue<CObj> inQueue;
    private ConcurrentLinkedQueue<Object> outqueue;
    private GetSendData2 conMan;
    private OutputProcessor outproc;
    private CObj endDestination;
    private DestinationThread dest;
    private Index index;
    private HH2Session session;
    private GuiCallback guicallback;
    private OutputStream outstream;
    private HasFileCreator hfc;
    private RequestFileHandler fileHandler;
    private int listCount;
    private Set<String> accumulateTypes; //Types to combine in list before processing
    private List<CObj> currentList;
    private long inBytes;
    private long inNonFileBytes;
    private long outBytes;
    private ConnectionListener conListener;
    private ConnectionThread This;
    private IdentityManager IdentManager;
    private long lastMyRequest;
    private long startTime;
    private Set<String> memberships;
    private Set<String> subs;
    private Set<RequestFile> filesHasRequested;
    private long lastFileUpdate = Long.MIN_VALUE;
    private String fileUp;
    private String fileDown;

    public ConnectionThread ( DestinationThread d, HH2Session s, Index i, Connection c, GetSendData2 sd, GuiCallback cb, ConnectionListener cl, RequestFileHandler rf, boolean fo, SpamTool st )
    {
        This = this;
        fileOnly = fo;
        conListener = cl;
        guicallback = cb;
        conMan = sd;
        con = c;
        dest = d;
        index = i;
        session = s;
        fileHandler = rf;
        subs = new CopyOnWriteArraySet<String>();
        memberships = new CopyOnWriteArraySet<String>();
        filesHasRequested = new CopyOnWriteArraySet<RequestFile>();
        lastMyRequest = System.currentTimeMillis();
        startTime = lastMyRequest;
        IdentManager = new IdentityManager ( session, index );
        hfc = new HasFileCreator ( session, index, st );
        outqueue = new ConcurrentLinkedQueue<Object>();
        inQueue = new ConcurrentLinkedQueue<CObj>();
        accumulateTypes = new HashSet<String>();
        accumulateTypes.add ( CObj.FRAGMENT );
        preprocProcessor = new BatchProcessor();
        InIdentityProcessor ip = new InIdentityProcessor ( session, index, this );
        preprocProcessor.addProcessor ( new ConnectionValidatorProcessor ( ip, d, this ) );
        //!!!!!!!!!!!!!!!!!! InFragProcessor - should be first !!!!!!!!!!!!!!!!!!!!!!!
        //Otherwise the list of fragments will be interate though for preceding processors
        //that don't need to and time will be wasted.
        preprocProcessor.addProcessor ( new InFragProcessor ( session, index, this ) );
        preprocProcessor.addProcessor ( new InFileModeProcessor ( this ) );
        preprocProcessor.addProcessor ( ip );
        preprocProcessor.addProcessor ( new InFileProcessor ( this ) );
        preprocProcessor.addProcessor ( new InComProcessor ( session, index, st, this ) );
        preprocProcessor.addProcessor ( new InHasFileProcessor ( dest.getIdentity(), session, index, this, hfc, st ) );
        preprocProcessor.addProcessor ( new InPrvIdentProcessor ( session, index, st, this ) );
        preprocProcessor.addProcessor ( new InPrvMsgProcessor ( session, index, st, this ) );
        preprocProcessor.addProcessor ( new InMemProcessor ( session, index, st, this ) );
        preprocProcessor.addProcessor ( new InMemProcessor ( session, index, st, this ) );
        preprocProcessor.addProcessor ( new InPostProcessor ( dest.getIdentity(), session, index, st, this ) );
        preprocProcessor.addProcessor ( new InSubProcessor ( session, index, st, this ) );
        preprocProcessor.addProcessor ( new InSpamExProcessor ( session, index, st ) );
        //!!!!!!!!!!!!!!!!! EnqueueRequestProcessor - must be last !!!!!!!!!!!!!!!!!!!!
        //Otherwise requests from the other node will not be processed.
        preprocProcessor.addProcessor ( new EnqueueRequestProcessor ( this ) );
        //These process requests from the other node.
        inProcessor = new BatchProcessor();
        inProcessor.addProcessor ( new ReqIdentProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqFragListProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqFragProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqComProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqPrvIdentProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqPrvMsgProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqHasFileProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqMemProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqPostsProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqSubProcessor ( i, this ) );
        inProcessor.addProcessor ( new ReqSpamExProcessor ( i, this ) );
        outproc = new OutputProcessor();
        Thread t = new Thread ( this, "Input Connection Process Thread" );
        t.start();
    }

    public void setFileMode ( boolean filemode )
    {
        fileOnly = filemode;
    }

    public boolean isFileMode()
    {
        return fileOnly;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public long getInBytes()
    {
        return inBytes;
    }

    public long getOutBytes()
    {
        return outBytes;
    }

    public CObj getEndDestination()
    {
        return endDestination;
    }

    public DestinationThread getLocalDestination()
    {
        return dest;
    }

    private void updateMemberships()
    {
        if ( endDestination != null )
        {
            Set<String> nl0 = new HashSet<String>();
            Set<String> nl1 = new CopyOnWriteArraySet<String>();
            CObjList sl = index.getIdentityMemberships ( endDestination.getId() );

            for ( int c = 0; c < sl.size(); c++ )
            {
                try
                {
                    CObj sb = sl.get ( c );
                    String sbid = sb.getPrivate ( CObj.COMMUNITYID );

                    if ( sbid != null )
                    {
                        nl0.add ( sbid );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sl.close();

            sl = index.getIdentityPrivateCommunities ( endDestination.getId() );

            for ( int c = 0; c < sl.size(); c++ )
            {
                try
                {
                    CObj sb = sl.get ( c );
                    nl0.add ( sb.getDig() );
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sl.close();

            sl = index.getIdentityMemberships ( getLocalDestination().getIdentity().getId() );

            for ( int c = 0; c < sl.size(); c++ )
            {
                try
                {
                    CObj sb = sl.get ( c );
                    String sbid = sb.getPrivate ( CObj.COMMUNITYID );

                    if ( nl0.contains ( sbid ) )
                    {
                        nl1.add ( sbid );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sl.close();

            sl = index.getIdentityPrivateCommunities ( getLocalDestination().getIdentity().getId() );

            for ( int c = 0; c < sl.size(); c++ )
            {
                try
                {
                    CObj sb = sl.get ( c );

                    if ( nl0.contains ( sb.getDig() ) )
                    {
                        nl1.add ( sb.getDig() );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sl.close();

            memberships = nl1;

        }

    }

    private void updateSubs()
    {
        if ( endDestination != null )
        {
            Set<String> nl0 = new HashSet<String>();
            Set<String> nl1 = new CopyOnWriteArraySet<String>();
            CObjList sl = index.getMemberSubscriptions ( endDestination.getId() );

            for ( int c = 0; c < sl.size(); c++ )
            {
                try
                {
                    CObj sb = sl.get ( c );
                    String sbid = sb.getString ( CObj.COMMUNITYID );

                    if ( sbid != null )
                    {
                        nl0.add ( sbid );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sl.close();
            sl = index.getMemberSubscriptions ( getLocalDestination().getIdentity().getId() );

            for ( int c = 0; c < sl.size(); c++ )
            {
                try
                {
                    CObj sb = sl.get ( c );
                    String sbid = sb.getString ( CObj.COMMUNITYID );

                    if ( nl0.contains ( sbid ) )
                    {
                        nl1.add ( sbid );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sl.close();
            subs = nl1;

        }

    }

    private void updateSubsAndFiles()
    {
        long nu = conMan.getLastFileUpdate();

        //log.info("CON UPDATE SUBS AND FILES " + nu + " > " + lastFileUpdate);
        appendOutput ( "updateSubsAndFiles: nu: " + nu + " > " + lastFileUpdate );

        if ( nu > lastFileUpdate )
        {
            lastFileUpdate = nu;
            updateMemberships();
            updateSubs();
            lastFileUpdate = conMan.getLastFileUpdate();
            appendOutput ( "updateSubsAndFiles: subs: " + subs );

            if ( fileOnly && endDestination != null )
            {
                filesHasRequested = conMan.getHasFileForConnection ( endDestination.getId(), subs );
                appendOutput ( "updateSubsAndFiles: filesHasRequested: " + filesHasRequested );
            }

        }

    }

    public void setEndDestination ( CObj o )
    {
        endDestination = o;
        updateSubsAndFiles();
    }

    public void stop()
    {
        boolean wasstopped = stop;
        stop = true;
        outproc.go();
        dest.connectionClosed ( this );

        if ( con != null )
        {
            con.close();
        }

        if ( !wasstopped )
        {
            CObj endd = getEndDestination();

            if ( endd != null )
            {
                IdentManager.connectionClose ( endd.getId(),
                                               getInNonFileBytes(),
                                               getInBytes(), getOutBytes() );
            }

            if ( intrace != null )
            {
                try
                {
                    appendInput ( "stopping" );
                    intrace.close();
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            if ( outtrace != null )
            {
                try
                {
                    appendOutput ( "stopping" );
                    outtrace.close();
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            conListener.closed ( this );
        }

    }

    public boolean isStopped()
    {
        return stop;
    }

    public boolean enqueueRemoteRequest ( CObj o )
    {
        if ( inQueue.size() < MAXQUEUESIZE )
        {
            inQueue.add ( o );
            outproc.go();
            return true;
        }

        return false;
    }

    private boolean checkType ( Object o )
    {
        if ( fileOnly )
        {
            if ( o instanceof CObj )
            {
                CObj c = ( CObj ) o;
                String tt = c.getType();

                if ( ! ( CObj.CON_REQ_FRAG.equals ( tt ) ||
                         CObj.CON_REQ_FRAGLIST.equals ( tt ) ||
                         CObj.FILEF.equals ( tt ) ||
                         CObj.FRAGMENT.equals ( tt ) ||
                         CObj.IDENTITY.equals ( tt ) ||
                         CObj.CON_CHALLENGE.equals ( tt ) ||
                         CObj.CON_REPLY.equals ( tt ) ||
                         CObj.CON_FILEMODE.equals ( tt ) ||
                         CObj.FRAGFAILED.equals ( tt )
                       ) )
                {
                    log.severe ( "ERROR: file mode unacceptable type: " + tt );
                    return false;
                }

            }

        }

        return true;
    }

    public boolean enqueue ( Object o )
    {

        if ( outqueue.size() < MAXQUEUESIZE )
        {
            if ( checkType ( o ) )
            {
                outqueue.add ( o );
                outproc.go();
                return true;
            }

        }

        log.info ( "CONTHREAD: DROPPED! " + o );

        if ( o instanceof CObjList )
        {
            CObjList l = ( CObjList ) o;
            l.close();
        }

        return false;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength ( int length )
    {
        this.length = length;
    }

    public void poke()
    {
        if ( outproc != null )
        {
            outproc.go();
        }

    }

    public void setLoadFile ( boolean loadFile )
    {
        this.loadFile = loadFile;
    }

    public void decrFileRequest()
    {
        if ( outproc != null )
        {
            outproc.decrFileRequests();
        }

    }

    private void process()
    {
        CObj o = inQueue.poll();

        try
        {
            if ( o != null )
            {
                inProcessor.processCObj ( o );
            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            stop();
        }

    }

    private class OutputProcessor implements Runnable
    {
        private int tick = 0;

        public synchronized void go()
        {
            notifyAll();
        }

        private synchronized void doWait()
        {
            if ( inQueue.size() == 0 && outqueue.size() == 0 )
            {
                try
                {
                    wait ( 30000 );
                }

                catch ( InterruptedException e )
                {
                }

            }

        }

        private Object getOutQueueData()
        {
            Object r = outqueue.poll();

            if ( r == null )
            {
                //Ok, process some of the requests from the other node.
                //Do this as late as possible so we don't have a bunch
                //of request data piling up waiting to go back.
                process();
                r = outqueue.poll();
            }

            return r;
        }

        public synchronized void incrFileRequests()
        {
            pendingFileRequests++;
        }

        public synchronized void decrFileRequests()
        {
            if ( pendingFileRequests > 0 )
            {
                pendingFileRequests--;
            }

        }

        private Object getLocalRequests()
        {
            appendOutput ( "getLocalRequests: START" );

            if (
                dest != null && endDestination != null &&
                (   pendingFileRequests < MAX_PENDING_FILES
                )
            )
            {
                appendOutput ( "getLocalRequests: fileOnly? " + fileOnly + " filesHasRequested? " + filesHasRequested );

                if ( fileOnly && filesHasRequested != null )
                {
                    Object r = conMan.nextFile ( dest.getIdentity().getId(),
                                                 endDestination.getId(), filesHasRequested );

                    appendOutput ( "getLocalRequests: nextFile? " + fileOnly + " filesHasRequested? " + filesHasRequested + " R: " + r );

                    if ( r != null )
                    {
                        if ( r instanceof CObj )
                        {
                            CObj co = ( CObj ) r;

                            if ( CObj.CON_REQ_FRAG.equals ( co.getType() ) ||
                                    CObj.CON_REQ_FRAGLIST.equals ( co.getType() ) )
                            {
                                incrFileRequests();
                            }

                        }

                    }

                    return r;
                }

                else
                {
                    int memcnt = 0;

                    if ( memberships != null )
                    {
                        memcnt = memberships.size();
                    }

                    int subcnt = 0;

                    if ( subs != null )
                    {
                        subcnt = subs.size();
                    }

                    appendOutput ( "nextNonFile mem: " + memberships + " " +
                                   memcnt + " subs: " + subs + " " + subcnt );
                    Object r = conMan.nextNonFile ( dest.getIdentity().getId(),
                                                    endDestination.getId(), memberships, subs );
                    appendOutput ( "nextNonFile " + r );
                    return r;
                }

            }

            return null;
        }

        private Object getData()
        {
            Object r = null;

            //Try to make fair by alternating seeing if we have a request
            //to go out or if the other node has requested data to send
            //back first.
            if ( tick == 0 )
            {
                r = getOutQueueData();
                tick = 1;
            }

            else
            {
                r = getLocalRequests();
                tick = 0;
            }

            //Ok, now just do both in case the first one we picked above
            //had no data.
            if ( r == null )
            {
                r = getOutQueueData();
            }

            if ( r == null )
            {
                r = getLocalRequests();
            }

            return r;
        }

        private void sendCObj ( CObj c ) throws IOException
        {
            sendCObjNoFlush ( c );
            outstream.flush();
        }

        private void sendCObjNoFlush ( CObj c ) throws IOException
        {
            lastSent = c.getType();
            lastSentTime = System.currentTimeMillis();
            JSONObject ot = c.getJSON();
            String os = ot.toString();
            byte ob[] = os.getBytes ( "UTF-8" );
            outBytes += ob.length;
            conListener.bytesSent ( ob.length );
            outstream.write ( ob );
        }

        private void seeIfUseless()
        {
            long curtime = System.currentTimeMillis();

            if ( !fileOnly )
            {
                long cuttime = curtime - ConnectionManager2.MAX_TIME_WITH_NO_REQUESTS;

                if ( lastMyRequest < cuttime )
                {
                    stop();

                }

            }

            long maxtime = curtime - ConnectionManager2.MAX_CONNECTION_TIME;

            if ( startTime < maxtime )
            {
                stop();
            }

        }

        @Override
        public void run()
        {
            while ( !stop )
            {
                try
                {

                    seeIfUseless();

                    Object o = getData();
                    appendOutput ( "WAIT FOR DATA.. " + o );
                    updateSubsAndFiles();

                    if ( o == null )
                    {
                        outstream.flush();
                        doWait();
                    }

                    else
                    {
                        //checkType ( o ); //TODO: Remove

                        if ( o instanceof CObj )
                        {
                            CObj c = ( CObj ) o;

                            if ( log.getLevel() == Level.INFO )
                            {
                                appendOutput ( c.getType() + "=============" );
                                appendOutput ( "comid:   " + c.getString ( CObj.COMMUNITYID ) );
                                appendOutput ( "creator: " + c.getString ( CObj.CREATOR ) );
                                appendOutput ( "memid:   " + c.getString ( CObj.MEMBERID ) );
                                appendOutput ( "seqnum:  " + c.getNumber ( CObj.SEQNUM ) );
                                appendOutput ( "first:   " + c.getNumber ( CObj.FIRSTNUM ) );
                                appendOutput ( "wdig:    " + c.getString ( CObj.FILEDIGEST ) );
                                appendOutput ( "offset:  " + c.getNumber ( CObj.FRAGOFFSET ) );
                            }

                            sendCObjNoFlush ( c );

                            if ( CObj.FILEF.equals ( c.getType() ) )
                            {
                                String lfs = c.getPrivate ( CObj.LOCALFILE );
                                Long offset = c.getNumber ( CObj.FRAGOFFSET );
                                Long len = c.getNumber ( CObj.FRAGSIZE );

                                if ( lfs != null && offset != null && len != null )
                                {
                                    byte buf[] = new byte[4096];
                                    File lf = new File ( lfs );
                                    RandomAccessFile raf = new RandomAccessFile ( lf, "r" );
                                    raf.seek ( offset );
                                    long ridx = 0;

                                    while ( ridx < len )
                                    {
                                        int l = raf.read ( buf, 0, Math.min ( buf.length,
                                                                              ( int ) ( len - ridx ) ) );

                                        if ( l < 0 )
                                        {
                                            throw new IOException ( "Oops." );
                                        }

                                        if ( l > 0 )
                                        {
                                            outstream.write ( buf, 0, l );
                                            outBytes += l;
                                            ridx += l;
                                            conListener.bytesSent ( l );
                                        }

                                    }

                                    raf.close();
                                }

                            }

                        }

                        else if ( o instanceof CObjList )
                        {
                            CObjList cl = ( CObjList ) o;
                            int len = cl.size();
                            CObj lo = new CObj();
                            lo.setType ( CObj.CON_LIST );
                            lo.pushNumber ( CObj.COUNT, len );
                            sendCObj ( lo );

                            for ( int c = 0; c < len; c++ )
                            {
                                sendCObjNoFlush ( cl.get ( c ) );
                            }

                            cl.close();
                        }

                        else
                        {
                            throw new RuntimeException ( "wtf? " + o.getClass().getName() );
                        }

                        updateGui();
                    }

                }

                catch ( Exception e )
                {
                    if ( log.getLevel() == Level.INFO )
                    {
                        e.printStackTrace();
                    }

                    stop();
                }

            }

        }

    }

    public static int MAX_PENDING_FILES = 10;
    private int pendingFileRequests = 0;
    private boolean loadFile;
    private int length;

    public int getPendingFileRequests()
    {
        return pendingFileRequests;
    }

    private void readFileData ( InputStream i ) throws IOException
    {
        if ( loadFile )
        {
            appendInput ( "Start reading file" );
            byte buf[] = new byte[1024];

            RIPEMD256Digest fdig = new RIPEMD256Digest();
            File tmpf = File.createTempFile ( "rxfile", ".dat" );
            FileOutputStream fos = new FileOutputStream ( tmpf );
            int rl = 0;

            while ( rl < length )
            {
                int len = i.read ( buf, 0, Math.min ( buf.length, length - rl ) );

                if ( len < 0 )
                {
                    stop();
                    fos.close();
                    throw new IOException ( "End of socket." );
                }

                if ( len > 0 )
                {
                    inBytes += len;
                    fdig.update ( buf, 0, len );
                    fos.write ( buf, 0, len );
                    rl += len;
                    conListener.bytesReceived ( len );
                }

            }

            fos.close();
            byte expdig[] = new byte[fdig.getDigestSize()];
            fdig.doFinal ( expdig, 0 );
            String dstr = Utils.toString ( expdig );

            outproc.decrFileRequests();
            loadFile = false;
            lastMyRequest = System.currentTimeMillis();
            appendInput ( "OUTPUT THREAD GO! " + pendingFileRequests );
            outproc.go(); //it holds off on local requests until the file is read.

            appendInput ( "File read " + dstr );
            processFragment ( dstr, tmpf );
            tmpf.delete();
            //now we have it, tell outproc to go again.
        }

    }

    @SuppressWarnings ( "unchecked" )
    private void processFragment ( String dig, File fpart ) throws IOException
    {
        byte buf[] = new byte[1024];
        CObjList flist = index.getFragments ( dig );
        appendInput ( "matching frags: " + flist.size() );

        for ( int c = 0; c < flist.size(); c++ )
        {
            RandomAccessFile raf = null;
            FileInputStream fis = null;
            Session s = null;
            CObj fg = flist.get ( c );

            String wdig = fg.getString ( CObj.FILEDIGEST );
            String fdig = fg.getString ( CObj.FRAGDIGEST );
            Long fidx = fg.getNumber ( CObj.FRAGOFFSET );
            Long flen = fg.getNumber ( CObj.FRAGSIZE );
            String cplt = fg.getString ( CObj.COMPLETE );

            appendInput ( " offset: " + fidx + " wdig: " + wdig +
                          " fdig: " + fdig + " flen: " + flen + " state: " + cplt );

            if ( wdig != null && fdig != null && fidx != null &&
                    flen != null && ( !"true".equals ( cplt ) ) )
            {
                try
                {
                    s = session.getSession();
                    Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE x.wholeDigest = :wdig "
                                              + "AND x.fragmentDigest = :fdig AND x.state != :dstate" );
                    q.setParameter ( "wdig", wdig );
                    q.setParameter ( "fdig", fdig );
                    q.setParameter ( "dstate", RequestFile.COMPLETE );
                    List<RequestFile> lrf = q.list();
                    String lf = null;

                    appendInput ( "matches RequestFiles found: " + lrf.size() );

                    for ( RequestFile rf : lrf )
                    {
                        //This could change for every fragment, but someone
                        //might find it interesting information.
                        fileDown = rf.getLocalFile();

                        boolean exists = false;
                        lf = rf.getLocalFile();

                        appendInput ( "lf: " + lf );

                        if ( lf != null )
                        {
                            File f = new File ( lf + RequestFileHandler.AKTIEPART );
                            appendInput ( "Check part file: " + f.getPath() + " exists " + f.exists() );
                            exists = f.exists();
                        }

                        if ( !exists )
                        {
                            lf = null;
                            s.getTransaction().begin();
                            RequestFile rrf = ( RequestFile ) s.get ( RequestFile.class, rf.getId() );
                            s.delete ( rrf );
                            s.getTransaction().commit();
                        }

                        else
                        {
                            s.getTransaction().begin();
                            rf = ( RequestFile ) s.get ( RequestFile.class, rf.getId() );
                            rf.setFragsComplete ( rf.getFragsComplete() + 1 );
                            s.merge ( rf );
                            appendInput ( "Frags complete: " + rf.getFragsComplete()  );
                            //Copy the fragment to the whole file.
                            raf = new RandomAccessFile ( rf.getLocalFile() + RequestFileHandler.AKTIEPART, "rw" );
                            fis = new FileInputStream ( fpart );
                            raf.seek ( fidx );
                            int ridx = 0;

                            while ( ridx < flen )
                            {
                                int len = fis.read ( buf, 0, Math.min ( buf.length, ( int ) ( flen - ridx ) ) );

                                if ( len < 0 )
                                {
                                    fis.close();
                                    raf.close();
                                    throw new IOException ( "Oops." );
                                }

                                if ( len > 0 )
                                {
                                    raf.write ( buf, 0, len );
                                    ridx += len;
                                }

                            }

                            raf.close();
                            fis.close();
                            s.getTransaction().commit();
                        }

                        //If we're done, then create a new HasFile for us!
                    }

                    if ( lf != null )
                    {
                        fg.pushPrivate ( CObj.COMPLETE, "true" );
                        fg.pushPrivate ( CObj.LOCALFILE, lf );
                        index.index ( fg );
                        index.forceNewSearcher(); //So we see immediately if we have all frags.
                    }

                    //Refresh the list of RequestFiles in case we deleted any.
                    q = s.createQuery ( "SELECT x FROM RequestFile x WHERE x.wholeDigest = :wdig "
                                        + "AND x.fragmentDigest = :fdig AND x.state != :dstate" );
                    q.setParameter ( "wdig", wdig );
                    q.setParameter ( "fdig", fdig );
                    q.setParameter ( "dstate", RequestFile.COMPLETE );
                    lrf = q.list();

                    //Commit the transaction
                    //Ok now count how many fragments of each is done.
                    appendInput ( "Pending files: " + lrf.size()  );

                    for ( RequestFile rf : lrf )
                    {
                        s.getTransaction().begin();
                        rf = ( RequestFile ) s.get ( RequestFile.class, rf.getId() );

                        if ( rf != null )
                        {
                            CObjList fdone = index.getFragmentsComplete ( rf.getCommunityId(),
                                             rf.getWholeDigest(), rf.getFragmentDigest() );
                            int numdone = fdone.size();
                            fdone.close();
                            rf.setFragsComplete ( numdone );
                            s.merge ( rf );
                            s.getTransaction().commit();
                            appendInput ( "Fragments complete in index: " + numdone + " <> " + rf.getFragsTotal() );


                            if ( rf.getFragsComplete() >= rf.getFragsTotal() )
                            {
                                if ( !fileHandler.claimFileComplete ( rf ) )
                                {
                                    appendInput ( "Failed to claim complete.." );
                                }

                                else
                                {
                                    appendInput ( "CLAIM FILE COMPLETE!!!!!!" );
                                    //rename the aktiepart file to the real file name
                                    File lff = new File ( rf.getLocalFile() );
                                    File rlp = new File ( rf.getLocalFile() + RequestFileHandler.AKTIEPART );

                                    int lps = 120;

                                    while ( lff.exists() && lps > 0 )
                                    {
                                        lps--;

                                        if ( !lff.delete() )
                                        {
                                            log.info ( "Could not delete file: " + lff.getPath() );

                                            try
                                            {
                                                Thread.sleep ( 1000L );
                                            }

                                            catch ( InterruptedException e )
                                            {
                                                e.printStackTrace();
                                            }

                                        }

                                    }

                                    lps = 120;

                                    while ( rlp.exists() && lps > 0 )
                                    {
                                        lps--;

                                        if ( !rlp.renameTo ( lff ) )
                                        {
                                            log.info ( "Failed to rename: " + rlp.getPath() + " to " + lff.getPath() );

                                            try
                                            {
                                                Thread.sleep ( 1000L );
                                            }

                                            catch ( InterruptedException e )
                                            {
                                                e.printStackTrace();
                                            }

                                        }

                                    }

                                    CObj hf = new CObj();
                                    hf.setType ( CObj.HASFILE );
                                    hf.pushString ( CObj.CREATOR, rf.getRequestId() );
                                    hf.pushString ( CObj.COMMUNITYID, rf.getCommunityId() );
                                    hf.pushString ( CObj.NAME, ( new File ( rf.getLocalFile() ) ).getName() );
                                    hf.pushText ( CObj.NAME, hf.getString ( CObj.NAME ) );
                                    hf.pushNumber ( CObj.FRAGSIZE, rf.getFragSize() );
                                    hf.pushNumber ( CObj.FILESIZE, rf.getFileSize() );
                                    hf.pushNumber ( CObj.FRAGNUMBER, rf.getFragsTotal() );
                                    hf.pushString ( CObj.STILLHASFILE, "true" );
                                    hf.pushString ( CObj.FILEDIGEST, rf.getWholeDigest() );
                                    hf.pushString ( CObj.FRAGDIGEST, rf.getFragmentDigest() );
                                    hf.pushPrivate ( CObj.LOCALFILE, rf.getLocalFile() );
                                    hf.pushPrivate ( CObj.UPGRADEFLAG, rf.isUpgrade() ? "true" : "false" );
                                    hf.pushString ( CObj.SHARE_NAME, rf.getShareName() );
                                    hfc.createHasFile ( hf );
                                    hfc.updateFileInfo ( hf );
                                    update ( hf );

                                }

                            }

                            update ( rf );
                        }

                        else
                        {
                            s.getTransaction().commit();
                        }

                    }

                    s.close();
                }

                catch ( Exception e )
                {
                    e.printStackTrace();

                    if ( s != null )
                    {
                        try
                        {
                            if ( s.getTransaction().isActive() )
                            {
                                s.getTransaction().rollback();
                            }

                            s.close();
                        }

                        catch ( Exception e2 )
                        {
                            e2.printStackTrace();
                        }

                    }

                    if ( raf != null )
                    {
                        try
                        {
                            raf.close();
                        }

                        catch ( Exception e2 )
                        {
                        }

                    }

                    if ( fis != null )
                    {
                        try
                        {
                            fis.close();
                        }

                        catch ( Exception e2 )
                        {
                        }

                    }

                }

            }

            appendInput ( "DONE PROCESSING FRAGMENT " );

        }

        flist.close();
    }

    public static long LONGESTLIST = 100000000;

    private long lastReadTime;
    private String lastRead = "";
    public String getLastRead()
    {
        return lastRead;
    }

    public long getLastReadTime()
    {
        return lastReadTime;
    }

    private long lastSentTime;
    private String lastSent = "";
    public String getLastSent()
    {
        return lastSent;
    }

    public long getLastSentTime()
    {
        return lastSentTime;
    }

    public long getListCount()
    {
        return listCount;
    }

    @Override
    public void run()
    {
        try
        {
            con.connect();
            outstream = con.getOutputStream();
            outproc = new OutputProcessor();
            Thread t = new Thread ( outproc, "Output Connection Processor Thread" );
            t.start();

            InputStream is = con.getInputStream();
            CleanParser clnpar = new CleanParser ( is );

            while ( !stop )
            {
                JSONObject jo = clnpar.next();
                inBytes += clnpar.getBytesRead();
                inNonFileBytes += clnpar.getBytesRead();
                conListener.bytesReceived ( clnpar.getBytesRead() );
                CObj r = new CObj();
                r.loadJSON ( jo );
                lastRead = r.getType();
                lastReadTime = System.currentTimeMillis();

                if ( log.getLevel() == Level.INFO )
                {
                    appendInput ( r.getType() + "=============" );
                    appendInput ( "dig:     " + r.getDig() );
                    appendInput ( "comid:   " + r.getString ( CObj.COMMUNITYID ) );
                    appendInput ( "creator: " + r.getString ( CObj.CREATOR ) );
                    appendInput ( "memid:   " + r.getString ( CObj.MEMBERID ) );
                    appendInput ( "seqnum:  " + r.getNumber ( CObj.SEQNUM ) );
                    appendInput ( "first:   " + r.getNumber ( CObj.FIRSTNUM ) );
                    appendInput ( "wdig:    " + r.getString ( CObj.FILEDIGEST ) );
                    appendInput ( "offset:  " + r.getNumber ( CObj.FRAGOFFSET ) );
                }

                if ( CObj.CON_LIST.equals ( r.getType() ) )
                {
                    if ( currentList == null )
                    {
                        long lc = r.getNumber ( CObj.COUNT );

                        if ( lc > LONGESTLIST ) { stop(); }

                        listCount = ( int ) lc;
                        appendInput ( Integer.toString ( listCount ) );
                    }

                    else
                    {
                        //This means they sent a new list while one was still
                        //going, this is a bad thing on them.
                        stop();
                    }

                }

                else
                {
                    if ( listCount > 0 )
                    {
                        if ( accumulateTypes.contains ( r.getType() ) )
                        {
                            if ( currentList == null )
                            {
                                currentList = new LinkedList<CObj>();
                            }

                            currentList.add ( r );
                            listCount--;
                            appendInput ( Integer.toString ( listCount ) );
                        }

                        else
                        {
                            currentList = null;
                            listCount = 0;
                        }

                    }

                    //If we're populating a list, then don't process until
                    //we have the whole list, at which time listCount will be zero
                    //if we're not collecting a list listCount is always zero
                    if ( listCount == 0 )
                    {
                        if ( currentList == null )
                        {
                            //Not a list, just process it.
                            try
                            {
                                preprocProcessor.processCObj ( r );
                            }

                            catch ( Exception e )
                            {
                                //Make sure we can debug processing bugs
                                e.printStackTrace();
                                stop();
                            }

                        }

                        else
                        {
                            try
                            {
                                outproc.decrFileRequests();
                                preprocProcessor.processObj ( currentList );
                            }

                            catch ( Exception e )
                            {
                                //Make sure we can debug processing bugs
                                e.printStackTrace();
                                stop();
                            }

                            currentList = null;
                        }

                    }

                    readFileData ( is );
                }

                //updateGui();
                outproc.go();
            }

        }

        catch ( Exception e )
        {
            if ( log.getLevel() == Level.INFO )
            {
                e.printStackTrace();
            }

        }

        stop();
    }

    long nextupdate = 0;
    private void updateGui()
    {
        long t = System.currentTimeMillis();

        if ( t >= nextupdate )
        {
            nextupdate = t + GUIUPDATEPERIOD;
            conListener.update ( This );
        }

    }

    public long getInNonFileBytes()
    {
        return inNonFileBytes;
    }

    public long getLastMyRequest()
    {
        return lastMyRequest;
    }

    @Override
    public void update ( Object o )
    {
        guicallback.update ( o );
        lastMyRequest = System.currentTimeMillis();
    }

    private PrintWriter outtrace;
    private PrintWriter intrace;

    private void appendOutput ( String msg )
    {
        if ( log.getLevel() == Level.INFO )
        {
            if ( endDestination != null )
            {
                if ( outtrace == null )
                {
                    String myid = dest.getIdentity().getId().substring ( 0, 6 );
                    String oid = endDestination.getId().substring ( 0, 6 );
                    String n = "out_" + myid + "_to_" + oid + ".trace";
                    n = n.replaceAll ( "\\\\", "_" );
                    n = n.replaceAll ( "/", "_" );
                    n = n.replaceAll ( ":", "_" );
                    n = n.replaceAll ( ";", "_" );
                    File f = new File ( n );
                    int idx = 0;

                    while ( f.exists() )
                    {
                        f = new File ( n + idx );
                        idx++;
                    }

                    try
                    {
                        outtrace = new PrintWriter ( new BufferedWriter ( new FileWriter ( f.getPath(), true ) ) );
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

                if ( outtrace != null )
                {
                    appendLog ( outtrace, msg );
                }

            }

        }

    }

    private void appendInput ( String msg )
    {
        if ( log.getLevel() == Level.INFO )
        {
            if ( endDestination != null )
            {
                if ( intrace == null )
                {
                    String myid = dest.getIdentity().getId().substring ( 0, 6 );
                    String oid = endDestination.getId().substring ( 0, 6 );
                    String n = "in_" + myid + "_to_" + oid + ".trace";
                    n = n.replaceAll ( "\\\\", "_" );
                    n = n.replaceAll ( "/", "_" );
                    n = n.replaceAll ( ":", "_" );
                    n = n.replaceAll ( ";", "_" );
                    File f = new File ( n );
                    int idx = 0;

                    while ( f.exists() )
                    {
                        f = new File ( n + idx );
                        idx++;
                    }

                    try
                    {
                        intrace = new PrintWriter ( new BufferedWriter ( new FileWriter ( f.getPath(), true ) ) );
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

                if ( intrace != null )
                {
                    appendLog ( intrace, msg );
                }

            }

        }

    }

    private void appendLog ( PrintWriter pw, String s )
    {
        if ( pw != null )
        {
            try
            {
                s = System.currentTimeMillis() + ":: " + s;
                pw.println ( s );
                pw.flush();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

    }

    public String getFileUp()
    {
        return fileUp;
    }

    public void setFileUp ( String fileUp )
    {
        this.fileUp = fileUp;
    }

    public String getFileDown()
    {
        return fileDown;
    }

    public void setFileDown ( String fileDown )
    {
        this.fileDown = fileDown;
    }

}
