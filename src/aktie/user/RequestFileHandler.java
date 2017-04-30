package aktie.user;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Query;
import org.hibernate.Session;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.DirectoryShare;
import aktie.data.HH2Session;
import aktie.data.RequestFile;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.utils.FUtils;

public class RequestFileHandler
{
    Logger log = Logger.getLogger ( "aktie" );

    public static final String AKTIEPART = ".aktiepart";

    private HH2Session session;
    private Index index;
    private File downloadDir;
    private NewFileProcessor nfp;
    private ShareManager shareMan;

    public RequestFileHandler ( HH2Session s, String downdir, NewFileProcessor n, Index i )
    {
        index = i;
        nfp = n;
        session = s;
        downloadDir = new File ( downdir );

        if ( !downloadDir.exists() )
        {
            downloadDir.mkdirs();
        }

    }

    public void cancelDownload ( RequestFile f )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();

            RequestFile rf = ( RequestFile ) s.get ( RequestFile.class, f.getId() );
            s.delete ( rf );

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        //Delete all the fragments
        CObjList cl = index.getFragments ( f.getWholeDigest(), f.getFragmentDigest() );

        for ( int c = 0; c < cl.size(); c++ )
        {
            try
            {
                CObj co = cl.get ( c );
                index.delete ( co );
            }

            catch ( Exception e )
            {
                //e.printStackTrace();
            }

        }

        cl.close();

    }

    public void setPriority ( RequestFile f, int pri )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            RequestFile rf = ( RequestFile ) s.get ( RequestFile.class, f.getId() );
            rf.setPriority ( pri );
            s.merge ( rf );
            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

    public void setReRequestList ( RequestFile f )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            RequestFile rf = ( RequestFile ) s.get ( RequestFile.class, f.getId() );
            rf.setState ( RequestFile.REQUEST_FRAG_LIST );
            s.merge ( rf );
            s.getTransaction().commit();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        finally
        {
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

    }

    @SuppressWarnings ( "unchecked" )
    public void deleteOldRequests ( long oldest )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE x.requestedOn < :old OR "
                                      + " x.state = :st " );
            q.setParameter ( "old", System.currentTimeMillis() - oldest );
            q.setParameter ( "st", RequestFile.COMPLETE );
            List<RequestFile> l = q.list();

            for ( RequestFile rf : l )
            {
                s.delete ( rf );
            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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
    public List<RequestFile> findFileToGetFrags ( String myid )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE "
                                      + "x.state = :st AND x.requestId = :rid ORDER BY "
                                      + "x.priority DESC, x.lastRequest ASC" );
            q.setParameter ( "st", RequestFile.REQUEST_FRAG );
            q.setParameter ( "rid", myid );
            q.setMaxResults ( 100 );
            List<RequestFile> l = q.list();//LOCKED HERE
            s.close();
            return l;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        return new LinkedList<RequestFile>();
    }

    @SuppressWarnings ( "unchecked" )
    public RequestFile findFileByName ( String name )
    {
        RequestFile requestFile = null;
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE "
                                      + "x.localFile = :fname" );
            q.setParameter ( "fname", name );
            q.setMaxResults ( 1 );
            List<RequestFile> l = q.list();//LOCKED HERE

            if ( l.size() > 0 )
            {
                requestFile = l.get ( 0 );
            }

            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        return requestFile;
    }

    public boolean claimFileListClaim ( RequestFile rf )
    {
        boolean claimed = false;
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            RequestFile r = ( RequestFile ) s.get ( RequestFile.class, rf.getId() );

            if ( RequestFile.REQUEST_FRAG_LIST == r.getState() ||
                    RequestFile.REQUEST_FRAG_LIST_SNT == r.getState() )
            {
                r.setState ( RequestFile.REQUEST_FRAG_LIST_SNT );
                r.setLastRequest ( System.currentTimeMillis() );
                s.merge ( r );
                claimed = true;
            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            claimed = false;
            //e.printStackTrace();

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

        return claimed;
    }

    @SuppressWarnings ( "unchecked" )
    public void setRequestedOn()
    {
        Session s = null;

        long today = System.currentTimeMillis();

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE x.requestedOn is null OR "
                                      + "x.requestedOn = 0" );
            q.setMaxResults ( 500 );
            List<RequestFile> l = q.list();

            for ( RequestFile rf : l )
            {
                rf.setRequestedOn ( today );
                s.merge ( rf );
            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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
    public List<RequestFile> findFileListFrags ( String rid, long backtime )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE x.requestId = :rid AND "
                                      + "(x.state = :st OR "
                                      + "   (x.state = :sts AND x.lastRequest < :rt) "
                                      + ") ORDER BY "
                                      + "x.priority DESC, x.lastRequest ASC" );
            q.setParameter ( "rid",  rid );
            q.setParameter ( "st", RequestFile.REQUEST_FRAG_LIST );
            q.setParameter ( "sts", RequestFile.REQUEST_FRAG_LIST_SNT );
            q.setParameter ( "rt", System.currentTimeMillis() - backtime );
            q.setMaxResults ( 500 );
            List<RequestFile> l = q.list();
            s.close();
            return l;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        return new LinkedList<RequestFile>();
    }

    @SuppressWarnings ( "unchecked" )
    public List<RequestFile> listRequestFiles ( int state, int max )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE "
                                      + "x.state = :st ORDER BY x.priority DESC" );
            q.setParameter ( "st", state );
            q.setMaxResults ( max );
            List<RequestFile> r = q.list();
            s.close();
            return r;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        return new LinkedList<RequestFile>();
    }

    @SuppressWarnings ( "unchecked" )
    public List<RequestFile> listRequestFilesNE ( int state, int max )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE "
                                      + "x.state != :st AND x.priority > 0 ORDER BY x.priority DESC, x.state DESC" );
            q.setParameter ( "st", state );
            q.setMaxResults ( max );
            List<RequestFile> r = q.list();
            s.close();
            return r;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        return new LinkedList<RequestFile>();
    }

    @SuppressWarnings ( "unchecked" )
    public List<RequestFile> listRequestFilesAll ( int state, int max )
    {
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE "
                                      + "x.state != :st ORDER BY x.priority DESC, x.state DESC" );
            q.setParameter ( "st", state );
            q.setMaxResults ( max );
            List<RequestFile> r = q.list();
            s.close();
            return r;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        return new LinkedList<RequestFile>();
    }

    public boolean claimFileComplete ( RequestFile rf )
    {
        boolean climaed = false;
        Session s = null;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            RequestFile nf = ( RequestFile ) s.get ( RequestFile.class, rf.getId() );

            if ( nf.getState() != RequestFile.COMPLETE )
            {
                nf.setState ( RequestFile.COMPLETE );
                nf.setFragsComplete ( nf.getFragsTotal() );
                s.merge ( nf );
                climaed = true;
            }

            s.getTransaction().commit();
            s.close();
        }

        catch ( Exception e )
        {
            climaed = false;
            //e.printStackTrace();

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

        return climaed;
    }

    private RequestFile createRF ( CObj hasfile, File lf, int state, int comp )
    {
        Session s = null;

        try
        {
            Long pri = hasfile.getNumber ( CObj.PRIORITY );
            int priority = 5;

            if ( pri != null )
            {
                long prl = pri;
                priority = ( int ) prl;
            }

            boolean upgrade = false;
            String upstr = hasfile.getPrivate ( CObj.UPGRADEFLAG );
            log.info ( "Download requested.  upgrade flag: " + upstr );

            if ( "true".equals ( upstr ) )
            {
                upgrade = true;
            }

            RequestFile rf = new RequestFile();
            rf.setRequestedOn ( System.currentTimeMillis() );
            rf.setUpgrade ( upgrade );
            rf.setCommunityId ( hasfile.getString ( CObj.COMMUNITYID ) );
            rf.setFileSize ( hasfile.getNumber ( CObj.FILESIZE ) );
            rf.setFragmentDigest ( hasfile.getString ( CObj.FRAGDIGEST ) );
            rf.setFragSize ( hasfile.getNumber ( CObj.FRAGSIZE ) );
            rf.setFragsTotal ( hasfile.getNumber ( CObj.FRAGNUMBER ) );
            rf.setWholeDigest ( hasfile.getString ( CObj.FILEDIGEST ) );
            rf.setRequestId ( hasfile.getString ( CObj.CREATOR ) );
            rf.setShareName ( hasfile.getString ( CObj.SHARE_NAME ) );
            rf.setPriority ( priority );
            rf.setLocalFile ( lf.getCanonicalPath() );
            rf.setState ( state );
            rf.setFragsComplete ( comp );
            s = session.getSession();
            s.getTransaction().begin();
            s.merge ( rf );
            s.getTransaction().commit();
            s.close();

            //NOTE: Make sure you create the RequestFile record first, so that we
            //don't create the file and have it added to a share prematurely before
            //the part values are checked.
            try
            {
                String id = Utils.mergeIds ( rf.getCommunityId(),
                                             rf.getFragmentDigest(), rf.getWholeDigest() );
                CObj fi = index.getFileInfo ( id );

                if ( fi != null )
                {
                    fi.pushString ( CObj.STATUS, "dnld" );
                    index.index ( fi );

                    if ( nfp != null && nfp.getGuiCallback() != null )
                    {
                        nfp.getGuiCallback().update ( fi );
                    }

                }

                if ( !lf.exists() )
                {
                    lf = new File ( lf.getPath() + AKTIEPART );
                    lf.createNewFile();
                }

            }

            catch ( Exception e )
            {
            }

            return rf;
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        return null;
    }

    @SuppressWarnings ( "unchecked" )
    private boolean alreadyRequested ( String comid, String wdig, String fdig, String creator )
    {
        boolean already = false;
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE x.communityId = :comid AND "
                                      + "x.wholeDigest = :wdig AND x.fragmentDigest = :fdig AND x.requestId = :creator AND "
                                      + "x.state != :state" );
            q.setParameter ( "comid", comid );
            q.setParameter ( "wdig", wdig );
            q.setParameter ( "fdig", fdig );
            q.setParameter ( "creator", creator );
            q.setParameter ( "state", RequestFile.COMPLETE );
            List<RequestFile> l = q.list();
            already = l.size() > 0;
            s.close();
        }

        catch ( Exception e )
        {
            //e.printStackTrace();

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

        return already;
    }

    private File makeSureNewFile ( File lf )
    {
        String fname = lf.getPath();

        int idx = 0;
        String ext = "";
        Matcher m = Pattern.compile ( "(.+)\\.(\\w+)$" ).matcher ( fname );

        if ( m.find() )
        {
            fname = m.group ( 1 );
            ext = "." + m.group ( 2 );
        }

        log.info ( "File name: " + fname + " ext: " + ext );

        File pf = new File ( lf.getPath() + AKTIEPART );

        while ( lf.exists() || pf.exists() )
        {
            lf = new File ( fname + "." + idx + ext );
            pf = new File ( lf.getPath() + AKTIEPART );
            log.info ( "Check file: " + lf );
            idx++;
        }

        return lf;
    }

    private DirectoryShare findShareForFile ( String comid, String memid, File f )
    {
        DirectoryShare r = null;

        try
        {
            String conn = f.getCanonicalPath();
            List<DirectoryShare> lst = shareMan.listShares ( comid, memid );

            for ( DirectoryShare d : lst )
            {
                if ( conn.startsWith ( d.getDirectory() ) )
                {
                    r = d;
                    break;
                }

            }

        }

        catch ( Exception e )
        {
            //e.printStackTrace();
        }

        return r;
    }

    /*
        First it check if we have a hasfile that matches the digests, community, and creator of the request
            - if one is found then we don't do anything
        If a matching file is found, but it's for the wrong, community/creator, localpath is set to the file
        Check the localfile set in the request.  If it points to an existing file, we check if the digest
            matches the requested digest.  If so we set localpath to the requested file.  If not,
            we backup the file localfile because it will be overwriten by the requested data.
        If localpath is set then we have a copy already of the file.  If the localfile value in the request
            is set, copy the localpath to the localfile (unless they are the same file, FUtils won't copy)
            If the localfile value is not set in the request
            then simply point localfile value to the existing file we already have.
            Then we simply process it as if we're adding a new file to the community.

    */
    public RequestFile createRequestFile ( CObj hasfile )
    {
        if ( CObj.USR_DOWNLOAD_FILE.equals ( hasfile.getType() ) )
        {

            String comid = hasfile.getString ( CObj.COMMUNITYID ) ;
            String creator = hasfile.getString ( CObj.CREATOR ) ;
            String pdig = hasfile.getString ( CObj.FRAGDIGEST ) ;
            String wdig = hasfile.getString ( CObj.FILEDIGEST ) ;
            String share = hasfile.getString ( CObj.SHARE_NAME );
            String lfs = hasfile.getPrivate ( CObj.LOCALFILE );
            String filename = hasfile.getString ( CObj.NAME );

            log.info ( "USR_DOWNLOAD_FILE: " + comid + " " + creator + " " + pdig + " " + wdig + " " + share + " " + lfs + " " + filename );

            //If share and localfile are both set make sure the localfile
            //is in the share path or else don't do it!
            if ( share != null && lfs != null && shareMan != null )
            {
                DirectoryShare s = shareMan.getShare ( comid, creator, share );

                log.info ( "SHARE: " + s );

                if ( s == null )
                {
                    hasfile.pushString ( CObj.SHARE_NAME, null );
                    share = null;
                    log.info ( "Clear the share as we don't have matching" );
                }

                else
                {
                    try
                    {
                        File tlf = new File ( lfs );
                        String conn = tlf.getCanonicalPath();

                        if ( !conn.startsWith ( s.getDirectory() ) )
                        {
                            log.info ( "Do not download.  The specified share does not match specified file location" );
                            return null;
                        }

                    }

                    catch ( Exception e )
                    {
                        ////e.printStackTrace();
                        log.info ( "Bad problem: " + e.getMessage() );
                        return null;
                    }

                }

            }

            if ( alreadyRequested ( comid, wdig, pdig, creator ) )
            {
                log.info ( "We already have the file downloading" );
                return null;
            }

            boolean okcreate = true;
            String localpath = null;

            try
            {
                //Check if we already have it
                CObjList myhlst = index.getMyHasFiles ( wdig, pdig );
                log.info ( "Number of matching files: " + myhlst.size() );

                for ( int i = 0; i < myhlst.size(); i++ )
                {
                    CObj hf = myhlst.get ( i );

                    if ( hf != null )
                    {
                        String tcr = hf.getString ( CObj.CREATOR );
                        String tcm = hf.getString ( CObj.COMMUNITYID );
                        String lp = hf.getPrivate ( CObj.LOCALFILE );
                        String cdig = FUtils.digWholeFile ( lp );
                        log.info ( "Checking if matches: " + cdig );

                        if ( cdig != null && cdig.equals ( wdig ) )
                        {
                            localpath = lp;
                            log.info ( "Digest matches: " + localpath );

                            if ( creator.equals ( tcr ) && comid.equals ( tcm ) )
                            {
                                log.info ( "We already have it!" );
                                okcreate = false;
                            }

                        }

                    }

                }

                myhlst.close();

            }

            catch ( Exception e )
            {
            }


            if ( okcreate )
            {
                //check if for some reason the existing localfile is actually
                //the correct file, if not and it exists anyway rename the current
                //file to backup.
                log.info ( "Existing localfile: " + lfs );

                if ( lfs != null )
                {
                    String dig = FUtils.digWholeFile ( lfs );

                    if ( wdig.equals ( dig ) )
                    {
                        //We already have the file defined in the localfile.  set localpath to
                        //the locafile value.  It won't copy over itself bellow.
                        //Note we would have found as a hasfile if the system knew about it
                        //keep going and simply add the file to the database.
                        localpath = lfs;
                    }

                    else
                    {
                        //Make a back-up file of the existing
                        File back = new File ( lfs + ".aktiebackup" );
                        File lf = new File ( lfs );

                        if ( lf.exists() )
                        {
                            lf.renameTo ( back );
                        }

                    }

                }

                if ( localpath != null )
                {
                    log.info ( "Localpath exists: " + localpath );

                    if ( nfp != null )
                    {
                        File lf = null;

                        //If localfile is set then we should copy the existing file. :(
                        //when localfile is set it means the user wants it saved to this
                        //specific file
                        if ( lfs != null )
                        {
                            lf = new File ( lfs );
                        }

                        else
                        {
                            //We have it in another group.  Create a new hasfile for this
                            if ( share != null && shareMan != null )
                            {
                                DirectoryShare ds = shareMan.getShare ( comid, creator, share );

                                if ( ds != null )
                                {
                                    lf = new File ( ds.getDirectory() + File.separator + filename );
                                }

                            }

                        }

                        if ( lf == null )
                        {
                            lf = new File ( localpath );
                        }

                        //See if the destination file is in a share, and set the share name
                        //if it is.
                        if ( share == null )
                        {
                            DirectoryShare shr = findShareForFile ( comid, creator, lf );

                            if ( shr != null )
                            {
                                share = shr.getShareName();
                                hasfile.pushString ( CObj.SHARE_NAME, share );
                            }

                        }

                        try
                        {
                            File s = new File ( localpath );
                            FUtils.copy ( s, lf );

                            CObj nhf = new CObj();
                            nhf.setType ( CObj.HASFILE );
                            nhf.pushString ( CObj.COMMUNITYID, comid );
                            nhf.pushString ( CObj.CREATOR, creator );
                            nhf.pushPrivate ( CObj.LOCALFILE, lf.getCanonicalPath() );
                            nhf.pushString ( CObj.SHARE_NAME, share );
                            nfp.process ( nhf );

                            long fn = hasfile.getNumber ( CObj.FRAGNUMBER );
                            log.info ( "SAVE requestfile: " + fn );
                            return createRF ( hasfile, lf, RequestFile.COMPLETE, ( int ) fn );
                        }

                        catch ( IOException e )
                        {
                            //e.printStackTrace();
                        }

                    }

                }

                else
                {
                    //We do not have a local copy of the file already, so actually create
                    //a request for the file

                    File lf = null;

                    //The localfile value is set.
                    if ( lfs != null )
                    {
                        lf = new File ( lfs );

                        if ( share == null )
                        {
                            DirectoryShare shr = findShareForFile ( comid, creator, lf );

                            if ( shr != null )
                            {
                                share = shr.getShareName();
                                hasfile.pushString ( CObj.SHARE_NAME, share );
                            }

                        }

                    }

                    log.info ( "LF: " + lf );

                    if ( lf == null )
                    {
                        if ( share != null && shareMan != null )
                        {
                            DirectoryShare ds = shareMan.getShare ( comid, creator, share );

                            if ( ds != null )
                            {
                                lf = new File ( ds.getDirectory() + File.separator + filename );
                            }

                        }

                        else
                        {
                            DirectoryShare ds = null;

                            if ( shareMan != null )
                            {
                                ds = shareMan.getDefaultShare ( comid, creator );
                            }

                            if ( ds != null )
                            {
                                lf = new File ( ds.getDirectory() + File.separator + filename );
                                hasfile.pushString ( CObj.SHARE_NAME, ds.getShareName() );
                            }

                            else
                            {
                                lf = new File ( downloadDir.getPath() + File.separator + filename );
                            }

                        }

                        lf = makeSureNewFile ( lf );

                    }

                    return createRF ( hasfile, lf, RequestFile.REQUEST_FRAG_LIST, 0 );
                }

            }

        }

        return null;
    }

    public void setShareMan ( ShareManager shareMan )
    {
        this.shareMan = shareMan;
    }

}
