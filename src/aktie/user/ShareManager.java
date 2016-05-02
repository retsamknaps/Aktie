package aktie.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.crypto.digests.RIPEMD256Digest;
import org.hibernate.Query;
import org.hibernate.Session;

import aktie.ProcessQueue;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.DirectoryShare;
import aktie.data.HH2Session;
import aktie.data.RequestFile;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.utils.FUtils;
import aktie.utils.HasFileCreator;

public class ShareManager implements Runnable
{

    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private HasFileCreator hfc;
    private ProcessQueue userQueue;
    private NewFileProcessor fileProc;
    private HH2Session session;
    private RequestFileHandler rfh;
    private boolean running;
    private boolean enabled;

    public ShareManager ( HH2Session s, RequestFileHandler rf, Index i, HasFileCreator h, NewFileProcessor n, ProcessQueue pq )
    {
        session = s;
        index = i;
        hfc = h;
        fileProc = n;
        userQueue = pq;
        enabled = true;
        running = false;
        rfh = rf;
        rfh.setShareMan ( this );
        Thread t = new Thread ( this, "Share Manager Thread" );
        t.setDaemon ( true );
        t.start();
    }

    private void autoDownload()
    {
        try
        {
            CObjList autodl = index.getAutodownloadQueries();
            log.info ( "autoDownload: found " + autodl.size() + " queries for auto download" );

            for ( int c = 0; c < autodl.size(); c++ )
            {
                try
                {
                    CObj co = autodl.get ( c );
                    List<CObj> ql = new LinkedList<CObj>();
                    ql.add ( co );
                    CObjList psts = index.searchPostsQuery ( ql, null );
                    log.info ( "autoDownload: found " + psts.size() + " matching posts" );

                    for ( int c0 = 0; c0 < psts.size(); c0++ )
                    {
                        try
                        {
                            CObj pst = psts.get ( c0 );

                            String name = pst.getString ( CObj.NAME );

                            if ( name != null )
                            {

                                log.info ( "autoDownload: Downloading: " + name );

                                CObj p = new CObj();
                                p.setType ( CObj.USR_DOWNLOAD_FILE );
                                p.pushString ( CObj.CREATOR, co.getString ( CObj.CREATOR ) );
                                p.pushString ( CObj.NAME, name );
                                p.pushString ( CObj.COMMUNITYID, pst.getString ( CObj.COMMUNITYID ) );
                                p.pushNumber ( CObj.FILESIZE, pst.getNumber ( CObj.FILESIZE ) );
                                p.pushString ( CObj.FRAGDIGEST, pst.getString ( CObj.FRAGDIGEST ) );
                                p.pushNumber ( CObj.FRAGSIZE, pst.getNumber ( CObj.FRAGSIZE ) );
                                p.pushNumber ( CObj.FRAGNUMBER, pst.getNumber ( CObj.FRAGNUMBER ) );
                                p.pushString ( CObj.FILEDIGEST, pst.getString ( CObj.FILEDIGEST ) );
                                p.pushString ( CObj.SHARE_NAME, pst.getString ( CObj.SHARE_NAME ) );

                                userQueue.enqueue ( p );
                            }

                            String lrgfile = pst.getString ( CObj.PRV_NAME );

                            if ( lrgfile != null )
                            {

                                log.info ( "autoDownload: Downloading: " + lrgfile );

                                CObj p = new CObj();
                                p.setType ( CObj.USR_DOWNLOAD_FILE );
                                p.pushString ( CObj.CREATOR, co.getString ( CObj.CREATOR ) );
                                p.pushString ( CObj.COMMUNITYID, pst.getString ( CObj.COMMUNITYID ) );
                                p.pushString ( CObj.NAME, pst.getString ( CObj.PRV_NAME ) );
                                p.pushNumber ( CObj.FILESIZE, pst.getNumber ( CObj.PRV_FILESIZE ) );
                                p.pushString ( CObj.FRAGDIGEST, pst.getString ( CObj.PRV_FRAGDIGEST ) );
                                p.pushNumber ( CObj.FRAGSIZE, pst.getNumber ( CObj.PRV_FRAGSIZE ) );
                                p.pushNumber ( CObj.FRAGNUMBER, pst.getNumber ( CObj.PRV_FRAGNUMBER ) );
                                p.pushString ( CObj.FILEDIGEST, pst.getString ( CObj.PRV_FILEDIGEST ) );
                                p.pushString ( CObj.SHARE_NAME, pst.getString ( CObj.SHARE_NAME ) );

                                userQueue.enqueue ( p );
                            }

                        }

                        catch ( Exception e )
                        {
                            e.printStackTrace();
                        }

                    }

                    psts.close();
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            autodl.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    private void addFile ( DirectoryShare s, File f )
    {
        CObj hf = new CObj();
        hf.setType ( CObj.HASFILE );
        hf.pushString ( CObj.CREATOR, s.getMemberId() );
        hf.pushString ( CObj.COMMUNITYID, s.getCommunityId() );
        hf.pushString ( CObj.SHARE_NAME, s.getShareName() );
        hf.pushPrivate ( CObj.LOCALFILE, f.getPath() ); //Canonical name gotten during processing
        fileProc.process ( hf );
    }

    private void checkFoundFile ( DirectoryShare s, File f )
    {
        if ( enabled )
        {
            String fp = f.getAbsolutePath();

            try
            {
                fp = f.getCanonicalPath();
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

            if ( !fp.endsWith ( ".aktiepart" ) && !fp.endsWith ( ".aktiebackup" ) )
            {

                if ( null == rfh.findFileByName ( fp ) )
                {

                    CObjList mlst = index.getLocalHasFiles ( s.getCommunityId(), s.getMemberId(), fp );

                    if ( mlst.size() == 0 )
                    {
                        mlst.close();

                        //Check if it's a duplicate
                        CObjList dlst = index.getDuplicate ( s.getCommunityId(), s.getMemberId(), fp );

                        if ( dlst.size() == 0 )
                        {
                            dlst.close();
                            addFile ( s, f );
                        }

                        else
                        {
                            CObj dlp = null;

                            try
                            {
                                //Check if file referenced by the duplicate exists
                                dlp = dlst.get ( 0 );
                            }

                            catch ( Exception e )
                            {
                                e.printStackTrace();
                            }

                            dlst.close();

                            boolean add = true;

                            if ( dlp != null )
                            {
                                String rfid = dlp.getString ( CObj.HASFILE );

                                if ( rfid != null )
                                {
                                    CObj hf = index.getById ( rfid );

                                    if ( hf != null )
                                    {
                                        //There is a hasfile, check if the file
                                        //still exists
                                        String phf = hf.getPrivate ( CObj.LOCALFILE );
                                        String shf = hf.getString ( CObj.STILLHASFILE );

                                        if ( phf != null && "true".equals ( shf ) )
                                        {
                                            File pf = new File ( phf );

                                            if ( pf.exists() )
                                            {
                                                add = false;
                                            }

                                        }

                                    }

                                }

                            }

                            if ( add )
                            {
                                //There is no hasfile for it.  So add it.
                                addFile ( s, f );
                            }

                        }

                    }

                    else
                    {
                        CObj mhf = null;

                        try
                        {
                            mhf = mlst.get ( 0 );
                        }

                        catch ( IOException e )
                        {
                            e.printStackTrace();
                        }

                        mlst.close();

                        if ( mhf != null )
                        {
                            String shr = mhf.getString ( CObj.SHARE_NAME );

                            if ( !s.getShareName().equals ( shr ) )
                            {
                                addFile ( s, f );
                            }

                        }

                    }

                }

            }

        }

    }

    /*
        If we search for all share files first.  Files renamed or moved
        in s share will already have thie hasfile record updated with the
        new filename before this is run.  When this is run for all hasfile
        records, it should only find files that have been deleted from a
        share, so we set them as stillhas false.
    */
    private void checkHasFile ( CObj hf )
    {
        String lf = hf.getPrivate ( CObj.LOCALFILE );
        String wd = hf.getString ( CObj.FILEDIGEST );
        Long ut = hf.getNumber ( CObj.CREATEDON );
        Long ln = hf.getNumber ( CObj.FILESIZE );

        if ( lf != null && wd != null && ut != null && ln != null )
        {
            File f = new File ( lf );
            boolean remove = true;

            if ( f.exists() )
            {
                if ( f.lastModified() <= ut && f.length() == ln )
                {
                    remove = false;
                }

                else
                {
                    String rdig = FUtils.digWholeFile ( lf );

                    if ( wd.equals ( rdig ) )
                    {
                        remove = false;
                    }

                }

            }

            if ( remove )
            {
                hf.pushString ( CObj.STILLHASFILE, "false" );
                hfc.createHasFile ( hf );
                hfc.updateFileInfo ( hf );
            }

        }

    }

    private void crawlDirectory ( DirectoryShare s, File df )
    {
        if ( df != null && df.exists() && df.isDirectory() && enabled )
        {
            File lsd[] = df.listFiles();

            for ( int c = 0; c < lsd.length; c++ )
            {
                File f = lsd[c];

                if ( f.exists() )
                {
                    if ( f.isDirectory() )
                    {
                        s.setNumberSubFolders ( s.getNumberSubFolders() + 1 );
                        crawlDirectory ( s, f );
                    }

                    else if ( f.isFile() )
                    {
                        s.setNumberFiles ( s.getNumberFiles() + 1 );
                        checkFoundFile ( s, f );
                    }

                }

            }

        }

        else
        {
            s.setMessage ( "Not a directory: " + df );
        }

    }

    private void crawlShare ( DirectoryShare s )
    {
        if ( enabled )
        {
            String ds = s.getDirectory();

            if ( ds != null )
            {
                File df = new File ( ds );
                crawlDirectory ( s, df );
            }

            else
            {
                s.setMessage ( "Directory not set." );
            }

        }

    }

    @SuppressWarnings ( "unchecked" )
    private void processShares()
    {
        if ( enabled )
        {
            Session s = null;

            try
            {
                s = session.getSession();
                List<DirectoryShare> l = s.createCriteria ( DirectoryShare.class ).list();

                for ( DirectoryShare ds : l )
                {
                    if ( enabled )
                    {
                        ds.setNumberSubFolders ( 0 );
                        ds.setNumberFiles ( 0 );
                        crawlShare ( ds );
                        saveShare ( s, ds );
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

                    }

                    catch ( Exception e2 )
                    {
                    }

                    try
                    {
                        s.close();
                    }

                    catch ( Exception e2 )
                    {
                    }

                }

            }

        }

    }

    //We are lazy and just remove all duplicates that don't exist
    //or don't have a hasfile that exists and has a file that exists.
    //they will be recrawled and new hasfiles will be created as needed.
    private void checkDuplicate ( CObj d )
    {
        if ( d != null )
        {
            String comid = d.getString ( CObj.COMMUNITYID );
            String memid = d.getString ( CObj.CREATOR );
            String lf = d.getString ( CObj.LOCALFILE );
            String hf = d.getString ( CObj.HASFILE );
            boolean remove = true;

            if ( comid != null && memid != null && lf != null && hf != null )
            {
                File f = new File ( lf );

                if ( f.exists() )
                {
                    try
                    {
                        CObj thf = index.getById ( hf );

                        if ( thf != null )
                        {
                            String olf = thf.getPrivate ( CObj.LOCALFILE );
                            String shf = thf.getString ( CObj.STILLHASFILE );

                            if ( olf != null && "true".equals ( shf ) )
                            {
                                File of = new File ( olf );

                                if ( of.exists() )
                                {
                                    remove = false;
                                }

                            }

                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

            }

            if ( remove )
            {
                try
                {
                    index.delete ( d );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

        }

    }

    private void checkAllHasFile()
    {
        if ( enabled )
        {

            CObjList myhf = index.getAllMyHasFiles();

            try
            {
                for ( int c = 0; c < myhf.size() && enabled; c++ )
                {
                    CObj hf = myhf.get ( c );
                    checkHasFile ( hf );
                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

            myhf.close();
        }

    }

    private void checkAllDuplicates()
    {
        if ( enabled )
        {

            CObjList myhf = index.getAllMyDuplicates();

            try
            {
                for ( int c = 0; c < myhf.size() && enabled; c++ )
                {
                    CObj hf = myhf.get ( c );
                    checkDuplicate ( hf );
                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

            myhf.close();
        }

    }

    private void saveShare ( Session s, DirectoryShare d )
    {
        try
        {
            s.getTransaction().begin();
            s.merge ( d );
            s.getTransaction().commit();
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

                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    /*
        If a file has been requested and not done, but yet
        all fragments say they are done.
    */
    private void checkFragments()
    {
        if ( enabled )
        {
            List<RequestFile> rl = rfh.listRequestFilesNE ( RequestFile.COMPLETE, Integer.MAX_VALUE );

            for ( RequestFile rf : rl )
            {
                File rlp = new File ( rf.getLocalFile() + RequestFileHandler.AKTIEPART );

                if ( rlp.exists() && enabled )
                {
                    //Find the fragments that haven't been requested yet.
                    CObjList cl = index.getFragmentsToRequest ( rf.getCommunityId(),
                                  rf.getWholeDigest(), rf.getFragmentDigest() );

                    if ( cl.size() == 0 )
                    {
                        //If there are no fragments that have not be requested yet,
                        //then let's reset the ones that in the req status, and not
                        //complete, in case we just failed to get it back after
                        //requesting.
                        cl.close();
                        cl = index.getFragmentsToReset ( rf.getCommunityId(),
                                                         rf.getWholeDigest(), rf.getFragmentDigest() );

                        if ( cl.size() == 0 )
                        {
                            //Welp, that sucks.  Let's make sure they're really done.
                            cl.close();
                            cl = index.getFragments ( rf.getWholeDigest(), rf.getFragmentDigest() );
                            log.warning ( "FOUND FILE TO CHECK FRAGMENTS: " + rf.getLocalFile() + " checking: " + cl.size() + " vs " + rf.getFragsTotal() );

                            if ( rf.getFragsTotal() != cl.size() &&
                                    rf.getLastRequest() <= ( System.currentTimeMillis() - 60L * 60L * 1000L ) )
                            {
                                log.warning ( "REREQUESTING FRAGMENT LIST" );
                                rfh.setReRequestList ( rf );

                            }

                            else
                            {
                                for ( int c = 0; c < cl.size() && enabled; c++ )
                                {
                                    try
                                    {
                                        CObj fg = cl.get ( c );
                                        log.warning ( "CHECKING FRAGMENT: " + c );
                                        String fdig = fg.getString ( CObj.FRAGDIG );
                                        Long fidx = fg.getNumber ( CObj.FRAGOFFSET );
                                        Long flen = fg.getNumber ( CObj.FRAGSIZE );
                                        FileInputStream fis = new FileInputStream ( rlp );
                                        fis.skip ( fidx );
                                        byte buf[] = new byte[4096];
                                        RIPEMD256Digest pdig = new RIPEMD256Digest();
                                        long lflen = flen;
                                        int iflen = ( int ) lflen;
                                        int nr = 0;

                                        while ( iflen > 0 && nr >= 0 )
                                        {
                                            int len = Math.min ( iflen, buf.length );
                                            nr = fis.read ( buf, 0, len );

                                            if ( nr > 0 )
                                            {
                                                pdig.update ( buf, 0, nr );
                                                iflen -= nr;
                                            }

                                        }

                                        fis.close();
                                        byte digb[] = new byte[pdig.getDigestSize()];
                                        pdig.doFinal ( digb, 0 );
                                        String cdig =  Utils.toString ( digb );

                                        if ( !cdig.equals ( fdig ) )
                                        {
                                            log.warning ( "FRAGMENT INCORRECT: " + cdig + " != " + fdig );
                                            fg.pushPrivate ( CObj.COMPLETE, "false" );
                                            index.index ( fg );
                                        }

                                    }

                                    catch ( IOException e )
                                    {
                                        e.printStackTrace();
                                    }

                                }

                            }

                        }

                    }

                    cl.close();

                }

            }

        }

    }

    @SuppressWarnings ( "unchecked" )
    public DirectoryShare getShare ( String comid, String memid, String name )
    {
        DirectoryShare r = null;
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM DirectoryShare x WHERE "
                                      + "x.shareName = :name AND "
                                      + "x.communityId = :comid AND "
                                      + "x.memberId = :memid" );
            q.setParameter ( "name", name );
            q.setParameter ( "comid", comid );
            q.setParameter ( "memid", memid );
            List<DirectoryShare> dlst = q.list();

            if ( dlst.size() > 0 )
            {
                r = dlst.get ( 0 );
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
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return r;

    }

    @SuppressWarnings ( "unchecked" )
    public DirectoryShare getDefaultShare ( String comid, String memid )
    {
        DirectoryShare r = null;
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM DirectoryShare x WHERE "
                                      + "x.defaultDownload = :def AND "
                                      + "x.communityId = :comid AND "
                                      + "x.memberId = :memid" );
            q.setParameter ( "def", true );
            q.setParameter ( "comid", comid );
            q.setParameter ( "memid", memid );
            List<DirectoryShare> dlst = q.list();

            if ( dlst.size() > 0 )
            {
                r = dlst.get ( 0 );
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
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return r;

    }

    public DirectoryShare getShare ( long id )
    {
        DirectoryShare r = null;
        Session s = null;

        try
        {
            s = session.getSession();
            r = ( DirectoryShare ) s.get ( DirectoryShare.class, id );
            s.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();

            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return r;
    }

    @SuppressWarnings ( "unchecked" )
    public List<DirectoryShare> listShares ( String comid, String memid )
    {
        Session s = null;
        List<DirectoryShare> r = new LinkedList<DirectoryShare>();

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM DirectoryShare x WHERE "
                                      + "x.communityId = :comid AND "
                                      + "x.memberId = :memid" );
            q.setParameter ( "comid", comid );
            q.setParameter ( "memid", memid );
            r = q.list();

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

                }

                catch ( Exception e2 )
                {
                }

                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return r;

    }

    @SuppressWarnings ( "unchecked" )
    public void deleteShare ( String comid, String memid, String name )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();

            Query q = s.createQuery ( "SELECT x FROM DirectoryShare x WHERE "
                                      + "x.shareName = :name AND "
                                      + "x.communityId = :comid AND x.memberId = :memid" );
            q.setParameter ( "name", name );
            q.setParameter ( "comid", comid );
            q.setParameter ( "memid", memid );
            List<DirectoryShare> sl = q.list();

            for ( int c = 0; c < sl.size(); c++ )
            {
                DirectoryShare d = sl.get ( c );
                s.delete ( d );
            }

            s.getTransaction().commit();
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

                }

                catch ( Exception e2 )
                {
                }

                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

    }

    @SuppressWarnings ( "unchecked" )
    public void addShare ( String comid, String memid, String name, String dir, boolean def )
    {
        boolean hassometing = false;

        if ( name != null )
        {
            Matcher m = Pattern.compile ( "(\\S+)" ).matcher ( name );
            hassometing = m.find();
        }

        if ( !hassometing )
        {
            return;
        }

        String conn = null;
        File sd = new File ( dir );

        if ( sd.exists() )
        {
            try
            {
                conn = sd.getCanonicalPath();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        if ( conn != null )
        {
            Session s = null;

            try
            {
                s = session.getSession();
                s.getTransaction().begin();

                DirectoryShare d = null;
                Query q = s.createQuery ( "SELECT x FROM DirectoryShare x WHERE "
                                          + "( x.directory = :path OR x.shareName = :name ) AND "
                                          + "x.communityId = :comid AND x.memberId = :memid" );
                q.setParameter ( "name", name );
                q.setParameter ( "path", conn );
                q.setParameter ( "comid", comid );
                q.setParameter ( "memid", memid );
                List<DirectoryShare> sl = q.list();

                if ( sl.size() > 0 )
                {
                    d = sl.get ( 0 );
                }

                if ( d == null )
                {
                    d = new DirectoryShare();
                }

                if ( def )
                {
                    q = s.createQuery ( "SELECT x FROM DirectoryShare x WHERE "
                                        + "x.defaultDownload = :def AND "
                                        + "x.communityId = :comid AND x.memberId = :memid" );
                    q.setParameter ( "def", true );
                    q.setParameter ( "comid", comid );
                    q.setParameter ( "memid", memid );
                    sl = q.list();

                    for ( DirectoryShare ds : sl )
                    {
                        ds.setDefaultDownload ( false );
                        s.merge ( ds );
                    }

                }

                d.setCommunityId ( comid );
                d.setDirectory ( conn );
                d.setMemberId ( memid );
                d.setShareName ( name );
                d.setDefaultDownload ( def );

                s.merge ( d );

                s.getTransaction().commit();
                s.close();

                newshare = true;
                go();
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

                    }

                    catch ( Exception e2 )
                    {
                    }

                    try
                    {
                        s.close();
                    }

                    catch ( Exception e2 )
                    {
                    }

                }

            }

        }

    }

    private boolean stop = false;
    private boolean newshare = false;

    public synchronized void go()
    {
        notifyAll();
    }

    public synchronized void stop()
    {
        stop = true;
        notifyAll();
    }

    public static long SHARE_DELAY = 60L * 1000L;
    public static long CHECKHASFILE_DELAY = 8L * 60L * 60L * 1000L;

    private long nextcheckhasfile = 0;

    public synchronized void delay()
    {
        try
        {
            wait ( SHARE_DELAY );
        }

        catch ( Exception e )
        {
        }

    }

    private ShareListener listener;
    public void setShareListener ( ShareListener l )
    {
        listener = l;
    }

    private void setRunning ( boolean t )
    {
        running = t;

        if ( listener != null )
        {
            listener.shareManagerRunning ( t );
        }

    }

    public void run()
    {
        while ( !stop )
        {
            newshare = false;

            if ( enabled )
            {
                setRunning ( true );
                processShares();
                setRunning ( false );
            }

            if ( !newshare )
            {
                delay();
            }

            long curtime = System.currentTimeMillis();

            if ( curtime >= nextcheckhasfile )
            {
                if ( enabled )
                {
                    setRunning ( true );
                    checkAllHasFile();
                    checkAllDuplicates();
                    checkFragments();
                    nextcheckhasfile = curtime + CHECKHASFILE_DELAY;
                    setRunning ( false );
                }

                autoDownload();

            }

        }

    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled ( boolean enabled )
    {
        this.enabled = enabled;
    }

    public boolean isRunning()
    {
        return running;
    }


}
