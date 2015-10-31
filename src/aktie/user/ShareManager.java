package aktie.user;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Query;
import org.hibernate.Session;

import aktie.ProcessQueue;
import aktie.data.CObj;
import aktie.data.DirectoryShare;
import aktie.data.HH2Session;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.utils.FUtils;
import aktie.utils.HasFileCreator;

public class ShareManager implements Runnable
{

    private Index index;
    private HasFileCreator hfc;
    private ProcessQueue userQueue;
    private HH2Session session;
    private RequestFileHandler rfh;

    public ShareManager ( HH2Session s, RequestFileHandler rf, Index i, HasFileCreator h, ProcessQueue pq )
    {
        session = s;
        index = i;
        hfc = h;
        userQueue = pq;
        rfh = rf;
        rfh.setShareMan ( this );
        Thread t = new Thread ( this );
        t.setDaemon ( true );
        t.start();
    }

    private void addFile ( DirectoryShare s, File f )
    {
        CObj hf = new CObj();
        hf.setType ( CObj.HASFILE );
        hf.pushString ( CObj.CREATOR, s.getMemberId() );
        hf.pushString ( CObj.COMMUNITYID, s.getCommunityId() );
        hf.pushString ( CObj.SHARE_NAME, s.getShareName() );
        hf.pushPrivate ( CObj.LOCALFILE, f.getPath() ); //Canonical name gotten during processing
        userQueue.enqueue ( hf );
    }

    private void checkFoundFile ( DirectoryShare s, File f )
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
                    addFile ( s, f );
                }

                else
                {
                    try
                    {
                        CObj mhf = mlst.get ( 0 );
                        String shr = mhf.getString ( CObj.SHARE_NAME );

                        if ( !s.getShareName().equals ( shr ) )
                        {
                            addFile ( s, f );
                        }

                    }

                    catch ( IOException e )
                    {
                        e.printStackTrace();
                    }

                }

                mlst.close();
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
        if ( df != null && df.exists() && df.isDirectory() )
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

    @SuppressWarnings ( "unchecked" )
    private void processShares()
    {
        Session s = null;

        try
        {
            s = session.getSession();
            List<DirectoryShare> l = s.createCriteria ( DirectoryShare.class ).list();

            for ( DirectoryShare ds : l )
            {
                ds.setNumberSubFolders ( 0 );
                ds.setNumberFiles ( 0 );
                crawlShare ( ds );
                saveShare ( s, ds );
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

    private void checkAllHasFile()
    {
        CObjList myhf = index.getAllMyHasFiles();

        try
        {
            for ( int c = 0; c < myhf.size(); c++ )
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

    public void run()
    {
        while ( !stop )
        {
            newshare = false;
            processShares();

            if ( !newshare )
            {
                delay();
            }

            long curtime = System.currentTimeMillis();

            if ( curtime >= nextcheckhasfile )
            {
                checkAllHasFile();
                nextcheckhasfile = curtime + CHECKHASFILE_DELAY;
            }

        }

    }


}
