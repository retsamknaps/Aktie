package aktie.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
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
import aktie.gui.Wrapper;
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
    private RequestFileHandler requestFileHandler;
    private boolean running;
    private boolean enabled;

    private Thread shareManagerThread = null;

    public ShareManager ( HH2Session s, RequestFileHandler rf, Index i, HasFileCreator h, NewFileProcessor n, ProcessQueue pq )
    {
        session = s;
        index = i;
        hfc = h;
        fileProc = n;
        userQueue = pq;
        enabled = true;
        running = false;
        requestFileHandler = rf;
        requestFileHandler.setShareMan ( this );
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

    private void addFile ( DirectoryShare directoryShare, File file )
    {
        CObj hasFile = new CObj();
        hasFile.setType ( CObj.HASFILE );
        hasFile.pushString ( CObj.CREATOR, directoryShare.getMemberId() );
        hasFile.pushString ( CObj.COMMUNITYID, directoryShare.getCommunityId() );
        // Changing the share name will require re-signing the file!
        hasFile.pushString ( CObj.SHARE_NAME, directoryShare.getShareName() );
        hasFile.pushPrivate ( CObj.LOCALFILE, file.getPath() ); //Canonical name gotten during processing
        hasFile.pushPrivate ( CObj.PRV_SKIP_PAYMENT, directoryShare.isSkipSpam() ? CObj.TRUE : CObj.FALSE );

        fileProc.process ( hasFile );
    }

    private void checkFoundFile ( DirectoryShare directoryShare, File file )
    {
        if ( !enabled )
        {
            return;
        }

        System.out.println ( "ShareManager.checkFoundFile(): member = " + directoryShare.getMemberId() + ", community = " + directoryShare.getCommunityId() + ", share = " + directoryShare.getShareName() + ", file = " + file.getName() );

        String filePath = file.getAbsolutePath();

        try
        {
            filePath = file.getCanonicalPath();
        }

        catch ( IOException e )
        {
            e.printStackTrace();
        }

        if ( requestFileHandler.findFileByName ( filePath ) == null )
        {
            // TODO: Why does getLocalHasFiles use "new Term ( CObj.docPrivate ( CObj.LOCALFILE ), localfile )"
            CObjList knownHasFiles = index.getLocalHasFiles ( directoryShare.getCommunityId(), directoryShare.getMemberId(), filePath );

            // if the file is not yet known for this share
            if ( knownHasFiles.size() == 0 )
            {
                knownHasFiles.close();

                //Check if it's a duplicate
                // TODO: And why does getDuplicate use "new Term ( CObj.docString ( CObj.LOCALFILE ), localfile )"
                CObjList duplicateHasFiles = index.getDuplicate ( directoryShare.getCommunityId(), directoryShare.getMemberId(), filePath );

                // the file is not yet known as duplicate
                if ( duplicateHasFiles.size() == 0 )
                {
                    duplicateHasFiles.close();
                    // The file is not yet know and it is not a duplicate
                    System.out.println ( "ShareManager.checkFoundFile(): adding file " + file.getPath() + " which is not known for this community and identity and not a duplicate file" );
                    addFile ( directoryShare, file );
                }

                // the file is already known as duplicate
                else
                {
                    CObj duplicateHasFile = null;

                    try
                    {
                        //Check if file referenced by the duplicate exists
                        duplicateHasFile = duplicateHasFiles.get ( 0 );
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                    duplicateHasFiles.close();

                    boolean add = true;

                    if ( duplicateHasFile != null )
                    {
                        String hasFileID = duplicateHasFile.getString ( CObj.HASFILE );

                        if ( hasFileID != null )
                        {
                            CObj hasFile = index.getById ( hasFileID );

                            if ( hasFile != null )
                            {
                                //There is a hasfile, check if the file
                                //still exists
                                String privateLocalFile = hasFile.getPrivate ( CObj.LOCALFILE );
                                String stillHasFile = hasFile.getString ( CObj.STILLHASFILE );

                                if ( privateLocalFile != null && stillHasFile.equals ( CObj.TRUE ) )
                                {
                                    File privateFile = new File ( privateLocalFile );

                                    if ( privateFile.exists() )
                                    {
                                        // the file exists and is known as duplicate, so nothing to do
                                        add = false;
                                    }

                                }

                            }

                        }

                    }

                    if ( add )
                    {
                        System.out.println ( "ShareManager.checkFoundFile(): adding file " + file.getPath() + " because it is a duplicate, but no hasfile for it" );
                        //There is no hasfile for it.  So add it.
                        addFile ( directoryShare, file );
                    }

                }

            }

            // if the file is already known for this share
            else
            {
                CObj memberHasFile = null;

                try
                {
                    memberHasFile = knownHasFiles.get ( 0 );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

                knownHasFiles.close();

                if ( memberHasFile != null )
                {
                    String share = memberHasFile.getString ( CObj.SHARE_NAME );

                    // if the share name has changed
                    if ( !directoryShare.getShareName().equals ( share ) )
                    {
                        // TODO: We should just change the share name instead of re-adding which means re-hashing
                        // and re-creating signature, see below

                        System.out.println ( "ShareManager.checkFoundFile(): re-adding file " + file.getPath() + " because share name changed from " + share + " to " + directoryShare.getShareName() );
                        addFile ( directoryShare, file );

                        /*
                            // This could work instead, couldn't it? But with the current implementation, it would invalid the signature of the file!
                            memberHasFile.pushString ( CObj.SHARE_NAME, directoryShare.getShareName() );
                            try
                            {
                            index.index ( memberHasFile );
                            }

                            catch ( IOException e )
                            {
                            e.printStackTrace();
                            }

                            // TODO: call HasFileCreator.updateFileInfo() + GuiCallback is a good idea?
                            memberHasFile.pushInternal ( CObj.INTERNAL_SHARE_NAME_CHANGED, CObj.TRUE );
                            hfc.updateFileInfo( memberHasFile);*/
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
        String localFile = hf.getPrivate ( CObj.LOCALFILE );
        String fileDigest = hf.getString ( CObj.FILEDIGEST );
        Long createdOn = hf.getNumber ( CObj.CREATEDON );
        Long fileSize = hf.getNumber ( CObj.FILESIZE );

        if ( localFile != null && fileDigest != null && createdOn != null && fileSize != null )
        {
            System.out.println ( "ShareManager.checkHasFile(): " + localFile );
            File f = new File ( localFile );
            boolean remove = true;

            if ( f.exists() )
            {
                if ( f.lastModified() <= createdOn && f.length() == fileSize )
                {
                    remove = false;
                }

                else
                {
                    String rdig = FUtils.digWholeFile ( localFile );

                    if ( fileDigest.equals ( rdig ) )
                    {
                        remove = false;
                    }

                }

            }

            if ( remove )
            {
                hf.pushString ( CObj.STILLHASFILE, CObj.FALSE );
                hfc.createHasFile ( hf );
                hfc.updateFileInfo ( hf );
            }

        }

    }

    private void crawlDirectory ( Path dlPath, Path nodePath, DirectoryShare directoryShare, File shareDir )
    {
        if ( !enabled )
        {
            return;
        }

        if ( shareDir == null || !shareDir.exists() || !shareDir.isDirectory() )
        {
            System.err.println ( "ShareManager.crawlDirectory(): Not a directory: " + shareDir );
            directoryShare.setMessage ( "Not a directory: " + shareDir );
            return;
        }

        try
        {
            //Do not allow sharing of node dirs except
            //the download dir
            Path shareDirPath = shareDir.getCanonicalFile().toPath();

            if ( ( !shareDirPath.startsWith ( dlPath ) ) && shareDirPath.startsWith ( nodePath ) )
            {
                System.err.println ( "ShareManager.crawlDirectory(): Sharing of node directory not allowed: " + shareDir );
                return;
            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            return;
        }

        File filesInShare[] = shareDir.listFiles();
        System.err.println ( "ShareManager.crawlDirectory(): Share " + directoryShare.getShareName() + " has " + filesInShare.length + " files and directories" );

        continueHereWithNextFile:

        for ( File file : filesInShare )
        {
            if ( !file.exists() )
            {
                continue continueHereWithNextFile;
            }

            if ( file.isDirectory() )
            {
                // If we are supposed to also share hidden directories
                // or otherwise if the directory is not hidden, crawl it.
                if ( Wrapper.getShareHiddenDirs() || !file.isHidden() )
                {
                    directoryShare.setNumberSubFolders ( directoryShare.getNumberSubFolders() + 1 );
                    crawlDirectory ( dlPath, nodePath, directoryShare, file );
                }

            }

            else if ( file.isFile() )
            {
                // If file is hidden and not supposed to share
                // hidden files, do not proceed.
                if ( file.isHidden() && !Wrapper.getShareHiddenFiles() )
                {
                    continue continueHereWithNextFile;
                }

                String fileName = file.getName();
                String extension = FUtils.getFileExtension ( fileName );

                // If the file extension is contained in the list of extensions
                // not to be shared, do not proceed.
                if ( extension != null )
                {
                    List<String> doNotShareExts = Wrapper.getDoNotShareFileExtensions();

                    for ( String doNotShareExt : doNotShareExts )
                    {
                        // File extensions sometimes don't care about upper and lower case
                        // (e.g. ".pdf" = ".PDF"), so do a case-insensitive comparison.
                        if ( doNotShareExt.equalsIgnoreCase ( extension ) )
                        {
                            // The file is not supposed to be shared,
                            // so continue with the next file
                            continue continueHereWithNextFile;
                        }

                    }

                }

                // If the file name is contained in the list of file names
                // not to be shared, do not proceed.
                List<String> doNotShareFileNames = Wrapper.getDoNotShareFileNames();

                for ( String doNotShareFileName : doNotShareFileNames )
                {
                    // Windows does not care about upper and lower case
                    if ( Wrapper.osIsWindows() )
                    {
                        if ( doNotShareFileName.equalsIgnoreCase ( fileName ) )
                        {
                            continue continueHereWithNextFile;
                        }

                    }

                    // Other systems care about case in file names
                    else
                    {
                        if ( doNotShareFileName.equals ( fileName ) )
                        {
                            continue continueHereWithNextFile;
                        }

                    }

                }

                directoryShare.setNumberFiles ( directoryShare.getNumberFiles() + 1 );
                checkFoundFile ( directoryShare, file );
            }

        }

    }

    private void crawlShare ( Path dlPath, Path nodePath, DirectoryShare directoryShare )
    {
        if ( enabled )
        {
            String sharePath = directoryShare.getDirectory();

            if ( sharePath != null )
            {
                System.err.println ( "ShareManager.crawlShare(): share = " + directoryShare.getShareName() + ", crawling directory " + sharePath );
                File shareDir = new File ( sharePath );
                crawlDirectory ( dlPath, nodePath, directoryShare, shareDir );
            }

            else
            {
                System.err.println ( "ShareManager.crawlShare(): Directory not set for share " + directoryShare.getShareName() );
                directoryShare.setMessage ( "Directory not set." );
            }

        }

    }

    @SuppressWarnings ( "unchecked" )
    private void processShares()
    {
        if ( enabled )
        {
            System.out.println ( "ShareManager.processShares()" );

            Session s = null;

            try
            {
                File nodeRunDir = new File ( Wrapper.RUNDIR );
                Path nodePath = nodeRunDir.getCanonicalFile().toPath();

                File dlDir = new File ( Wrapper.DLDIR );
                Path dlPath = dlDir.getCanonicalFile().toPath();

                s = session.getSession();
                List<DirectoryShare> directoryShares = s.createCriteria ( DirectoryShare.class ).list();
                System.out.println ( "ShareManager.processShares() Found " + directoryShares.size() + " shares" );

                for ( DirectoryShare directoryShare : directoryShares )
                {
                    if ( enabled )
                    {
                        System.out.println ( "ShareManager.processShares(): Processing share = " + directoryShare.getShareName() + ", member " + directoryShare.getMemberId() + ", community = " + directoryShare.getCommunityId() );
                        directoryShare.setNumberSubFolders ( 0 );
                        directoryShare.setNumberFiles ( 0 );
                        crawlShare ( dlPath, nodePath, directoryShare );
                        saveShare ( s, directoryShare );
                        System.out.println ( "ShareManager.processShares(): Share = " + directoryShare.getShareName() + " has " + + directoryShare.getNumberFiles() + " files" );
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

            System.out.println ( "ShareManager.processShares() done." );
        }

    }

    //We are lazy and just remove all duplicates that don't exist
    //or don't have a hasfile that exists and has a file that exists.
    //they will be recrawled and new hasfiles will be created as needed.
    private void checkDuplicate ( CObj d )
    {
        if ( d != null )
        {
            String communityID = d.getString ( CObj.COMMUNITYID );
            String memberID = d.getString ( CObj.CREATOR );
            String localFile = d.getString ( CObj.LOCALFILE );
            String hasFile = d.getString ( CObj.HASFILE );
            boolean remove = true;

            if ( communityID != null && memberID != null && localFile != null && hasFile != null )
            {
                System.out.println ( "ShareManager.checkHasDuplicate(): " + localFile + " for hasfile " + hasFile );
                File f = new File ( localFile );

                if ( f.exists() )
                {
                    try
                    {
                        CObj thf = index.getById ( hasFile );

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
            List<RequestFile> rl = requestFileHandler.listRequestFilesNE ( RequestFile.COMPLETE, Integer.MAX_VALUE );

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
                                requestFileHandler.setReRequestList ( rf );

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
    public void addShare ( String communityID, String memberID, String shareName, String directory, boolean isDefaultShare, boolean skipSpam )
    {
        boolean fileNameNotOnlyWhitespace = false;

        if ( shareName != null )
        {
            Matcher m = Pattern.compile ( "(\\S+)" ).matcher ( shareName );
            fileNameNotOnlyWhitespace = m.find();
        }

        if ( !fileNameNotOnlyWhitespace )
        {
            return;
        }

        String shareDirectoryPath = null;
        File shareDirectory = new File ( directory );

        if ( shareDirectory.exists() )
        {
            try
            {
                shareDirectoryPath = shareDirectory.getCanonicalPath();
            }

            catch ( Exception e )
            {
                shareDirectoryPath = null;
                e.printStackTrace();
            }

        }

        if ( shareDirectoryPath != null )
        {
            Session s = null;

            try
            {
                s = session.getSession();
                s.getTransaction().begin();

                DirectoryShare directoryShare = null;
                Query q = s.createQuery ( "SELECT x FROM DirectoryShare x WHERE "
                                          + "( x.directory = :path OR x.shareName = :name ) AND "
                                          + "x.communityId = :comid AND x.memberId = :memid" );
                q.setParameter ( "name", shareName );
                q.setParameter ( "path", shareDirectoryPath );
                q.setParameter ( "comid", communityID );
                q.setParameter ( "memid", memberID );
                List<DirectoryShare> shareList = q.list();

                if ( shareList.size() > 0 )
                {
                    directoryShare = shareList.get ( 0 );
                }

                if ( directoryShare == null )
                {
                    directoryShare = new DirectoryShare();
                }

                if ( isDefaultShare )
                {
                    q = s.createQuery ( "SELECT x FROM DirectoryShare x WHERE "
                                        + "x.defaultDownload = :def AND "
                                        + "x.communityId = :comid AND x.memberId = :memid" );
                    q.setParameter ( "def", true );
                    q.setParameter ( "comid", communityID );
                    q.setParameter ( "memid", memberID );
                    shareList = q.list();

                    // set the other shares for this community and member to non-default
                    for ( DirectoryShare share : shareList )
                    {
                        share.setDefaultDownload ( false );
                        s.merge ( share );
                    }

                }

                directoryShare.setCommunityId ( communityID );
                directoryShare.setDirectory ( shareDirectoryPath );
                directoryShare.setMemberId ( memberID );
                directoryShare.setShareName ( shareName );
                directoryShare.setDefaultDownload ( isDefaultShare );
                directoryShare.setSkipSpam ( skipSpam );

                s.merge ( directoryShare );

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
        enabled = false;
        stop = true;
        notifyAll();
    }

    public static final long SHARE_DELAY = 60L * 1000L;
    public static final long CHECKHASFILE_DELAY = 1L * 60L * 60L * 1000L;

    private long nextcheckhasfile = 0;

    private boolean delay = false;

    private void delay() throws InterruptedException
    {
        try
        {
            delay = true;
            Thread.sleep ( SHARE_DELAY );
            delay = false;
        }

        catch ( InterruptedException e )
        {
            delay = false;
            System.out.println ( "ShareManager.delay() interrupted." );
            // We were woken up
            throw e;
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

    @Override
    public void run()
    {
        shareManagerThread = Thread.currentThread();

        while ( !stop )
        {
            newshare = false;
            boolean updateindex = false;

            if ( enabled )
            {
                setRunning ( true );
                processShares();
                setRunning ( false );
                updateindex = true;
            }

            if ( !newshare )
            {
                try
                {
                    delay();
                }

                catch ( InterruptedException e )
                {
                    // If we are interrupted (by setEnabled() which should be due to user interaction)
                    // continue with processing the shares instead of the sanity tasks below.
                    continue;
                }

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
                    updateindex = true;
                }

                autoDownload();

            }

            if ( updateindex )
            {
                index.forceNewSearcher();
            }

        }

        shareManagerThread = null;

    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled ( boolean enabled )
    {
        this.enabled = enabled;

        if ( enabled && delay && shareManagerThread != null )
        {
            shareManagerThread.interrupt();
        }

    }

    public boolean isRunning()
    {
        return running;
    }

}
