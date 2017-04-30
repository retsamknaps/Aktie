package aktie.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.crypto.digests.RIPEMD256Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.utils.HasFileCreator;

public class NewFileProcessor extends GenericProcessor
{

    private GuiCallback guicallback;
    private Index index;
    private HasFileCreator hsc;

    public NewFileProcessor ( HH2Session s, Index i, SpamTool st, GuiCallback cb )
    {
        index = i;
        guicallback = cb;
        hsc = new HasFileCreator ( s, i, st, cb );
    }

    /**
        Must set:
        string: creator, community
        number: fragsize
        private: localfile
    */
    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.HASFILE.equals ( type ) )
        {
            String communityID = o.getString ( CObj.COMMUNITYID );
            String creatorID = o.getString ( CObj.CREATOR );
            String shareName = o.getString ( CObj.SHARE_NAME );

            if ( communityID == null || creatorID == null )
            {
                o.pushString ( CObj.ERROR, "No community or creator specified" );
                guicallback.update ( o );
                return true;
            }

            //Digest whole file.
            String localFile = o.getPrivate ( CObj.LOCALFILE );

            if ( localFile == null )
            {
                o.pushString ( CObj.ERROR, "No local file specified" );
                guicallback.update ( o );
                return true;
            }

            File file = new File ( localFile );

            if ( !file.exists() || !file.isFile() )
            {
                o.pushString ( CObj.ERROR, "Local file does not exist: " + localFile );
                guicallback.update ( o );
                return true;
            }

            //Set the file name if not set
            String name = o.getString ( CObj.NAME );

            if ( name == null )
            {
                name = file.getName();
                o.pushString ( CObj.NAME, name );
            }

            //Get the fragment size, set if necessary
            Long fragSize = o.getNumber ( CObj.FRAGSIZE );

            if ( fragSize == null )
            {
                fragSize = 512L * 1024L; //500K
                o.pushNumber ( CObj.FRAGSIZE, fragSize );
            }

            if ( fragSize > 20L * 1024L * 1024L ) //20MB
            {
                o.pushString ( CObj.ERROR, "Fragment size is too large " + fragSize );
                guicallback.update ( o );
                return true;
            }


            // Whether we make a full copy of all non-private data.
            // This is the case if only the name or path of the file changed, but community and creator are the same.
            boolean fullCopy = false;
            // Whether we will need to compute the full dig (otherwise, we copy)
            boolean doFullDig = true;
            // Whether we will need to compute the dig of digs (otherwise, we copy)
            boolean doDigOfDigs = true;

            // Check, if we already know a file by the same path or filename and size.
            // For files that are not very small, it is rather likely that
            // a file by the same name and size is the very same file that
            // we want to add.
            long fileSize =  file.length();

            CObjList sameSizeFiles = index.getHasFilesBySize ( fileSize );
            System.out.println ( "NewFileProcess.process(): Found " + sameSizeFiles.size() + " hasfiles with the same size " + fileSize + " B as " + file.getPath() );

            // If there is just one known file by the same name and size
            if ( sameSizeFiles.size() > 0 )
            {
                try
                {
                    CObj samePathHasFile = null;
                    CObj samePathCommunityCreatorShareHasFile = null;
                    LinkedList<CObj> sameCommunityMemberShareHasFiles = new LinkedList<CObj>();
                    LinkedList<CObj> maybeSameHasFiles = new LinkedList<CObj>();

                    for ( int i = 0; i < sameSizeFiles.size(); i++  )
                    {
                        CObj compareHasFile = sameSizeFiles.get ( i );
                        String comparePath = compareHasFile.getPrivate ( CObj.LOCALFILE );

                        if ( comparePath != null && comparePath.equals ( file.getPath() ) )
                        {
                            // A file with the same full path and size
                            // will extremely likely be the same file as the one we are processing.
                            // If we could not trust the file to be the same, we could not trust integrity
                            // of local hasfiles at all.
                            String compareCreatorID = compareHasFile.getString ( CObj.CREATOR );
                            String compareCommunityID = compareHasFile.getString ( CObj.COMMUNITYID );
                            String compareShareName = compareHasFile.getString ( CObj.SHARE_NAME );

                            if ( compareCreatorID != null && compareCommunityID != null && compareShareName != null )
                            {
                                if ( compareCreatorID.equals ( creatorID ) && compareCommunityID.equals ( communityID ) && compareShareName.equals ( shareName  ) )
                                {
                                    // Full hit! We already should have known this file!
                                    samePathCommunityCreatorShareHasFile = compareHasFile;
                                    System.out.println ( "NewFileProcess.process(): samePathCommunityCreatorHasFile = " + comparePath );
                                    break;
                                }

                                else
                                {
                                    samePathHasFile = compareHasFile;
                                    System.out.println ( "NewFileProcess.process(): samePathHasFile = " + comparePath );
                                }

                            }

                            else
                            {
                                System.err.println ( "NewFileProcess.process(): Saw hasfile without creator ID, community ID or share name assigned!" );
                            }

                            // If the path was the same, we do not need to check for file name equality
                            continue;
                        }

                        String compareFilename = null;

                        if ( comparePath != null )
                        {
                            compareFilename = new File ( comparePath ).getName();
                        }

                        else
                        {
                            compareFilename = compareHasFile.getString ( CObj.FILENAME );
                        }

                        String compareCreatorID = compareHasFile.getString ( CObj.CREATOR );
                        String compareCommunityID = compareHasFile.getString ( CObj.COMMUNITYID );
                        String compareShareName = compareHasFile.getString ( CObj.SHARE_NAME );

                        if ( compareCreatorID != null && compareCommunityID != null && compareShareName != null )
                        {
                            LinkedList<CObj> hasFileList;

                            if ( compareCreatorID.equals ( creatorID ) && compareCommunityID.equals ( communityID ) && compareShareName.equals ( shareName  ) )
                            {
                                hasFileList = sameCommunityMemberShareHasFiles;
                            }

                            else
                            {
                                hasFileList = maybeSameHasFiles;
                            }

                            if ( compareFilename != null && compareFilename.equals ( file.getName() ) )
                            {
                                // Files with the same name and size
                                // are likely candidates to be the same file as the one we are processing.
                                // Add them in the front of the list.
                                hasFileList.add ( 0, compareHasFile );
                                System.out.println ( "NewFileProcess.process(): sameNameHasFiles += " + comparePath );
                            }

                            else
                            {
                                // Files which just have the same size are less likely candidates.
                                // Add them in the back of the list.
                                hasFileList.add ( compareHasFile );
                                System.out.println ( "NewFileProcess.process(): notSameNameHasFiles += " + comparePath );
                            }

                        }

                        else
                        {
                            System.err.println ( "NewFileProcess.process(): Saw hasfile without creator ID, community ID or share name assigned!" );
                        }

                    }

                    if ( samePathCommunityCreatorShareHasFile != null )
                    {
                        // clone the hasfile
                        o = samePathCommunityCreatorShareHasFile.clone();
                        //o.pushInternal ( CObj.INTERNAL_HAS_FILE_COPY, CObj.TRUE );
                        // Yes, this is a full copy of all non-private data!
                        fullCopy = true;
                        doFullDig = false;
                        doDigOfDigs = false;
                        System.out.println ( "NewFileProcessor.process(): File alreay known with path: " + file.getPath() );
                    }

                    else if ( sameCommunityMemberShareHasFiles.size() > 0 )
                    {
                        // For the first, only compute the full dig of the hasfile we are processing
                        if ( !digest ( o, true, false ) )
                        {
                            return true;
                        }

                        // if digest returned successfull, we have the full dig pushed into CObj o
                        // keep this in mind, no matter what we decide further on
                        doFullDig = false;

                        String fullDig = o.getString ( CObj.FILEDIGEST );

                        if ( fullDig == null )
                        {
                            // this generally should not happen
                            return true;
                        }

                        for ( CObj compareHasFile : sameCommunityMemberShareHasFiles )
                        {
                            String maybeSameFullDig = compareHasFile.getString ( CObj.FILEDIGEST );

                            // If the full dig is the same as that of the known file
                            if ( maybeSameFullDig != null && fullDig.equals ( maybeSameFullDig ) )
                            {
                                // clone the has file
                                o = compareHasFile.clone();
                                //o.pushInternal ( CObj.INTERNAL_HAS_FILE_COPY, CObj.TRUE );
                                // Yes, this is a full copy of all non-private data!
                                fullCopy = true;
                                // We only have to push the private local filename
                                o.pushPrivate ( CObj.LOCALFILE, localFile );
                                // at least we saved doing the dig of digs compute job.
                                doDigOfDigs = false;
                                System.out.println ( "NewFileProcessor.process(): File '" + file.getName() + "' already known with dig " + fullDig );
                                break;
                            }

                        }

                    }

                    // If the path and the size are the same, we can assume
                    // that this file is already known (for another identity).
                    // For the already known file, we also have the trust that
                    // it didn't change in a single byte since we processed it once.
                    if ( !fullCopy && samePathHasFile != null )
                    {
                        // clone the hasfile
                        o = samePathHasFile.clone();
                        // and set the new creator and community
                        o.pushString ( CObj.COMMUNITYID, communityID );
                        o.pushString ( CObj.CREATOR, creatorID );

                        if ( shareName != null )
                        {
                            o.pushString ( CObj.SHARE_NAME, shareName );
                        }

                        String fileDigest = o.getString ( CObj.FILEDIGEST );
                        String fragDigest = o.getString ( CObj.FRAGDIGEST );

                        if ( fileDigest != null && fragDigest != null )
                        {
                            String hasFileID = Utils.mergeIds ( communityID, fragDigest, fileDigest );
                            System.out.println ( "NewFileProcessor.process(): Setting ID for file: " + file.getPath() );
                            o.setId ( hasFileID );
                        }

                        doFullDig = false;
                        doDigOfDigs = false;
                        System.out.println ( "NewFileProcessor.process(): File already known with path: " + file.getPath() );
                    }

                    else if ( !fullCopy )
                    {
                        // For the first, only compute the full dig of the hasfile we are processing
                        // and only do this, if it was not yet done
                        if ( !digest ( o, doFullDig, false ) )
                        {
                            return true;
                        }

                        // if digest returned successfull, we have the full dig pushed into CObj o
                        // keep this in mind, no matter what we decide further on
                        doFullDig = false;

                        String fullDig = o.getString ( CObj.FILEDIGEST );

                        if ( fullDig == null )
                        {
                            // this generally should not happen
                            return true;
                        }

                        for ( CObj compareHasFile : maybeSameHasFiles )
                        {
                            String maybeSameFullDig = compareHasFile.getString ( CObj.FILEDIGEST );

                            // If the full dig is the same as that of the known file
                            if ( maybeSameFullDig != null && fullDig.equals ( maybeSameFullDig ) )
                            {
                                // clone the has file
                                o = compareHasFile.clone();
                                // and set the new creator, community and path
                                o.pushString ( CObj.COMMUNITYID, communityID );
                                o.pushString ( CObj.CREATOR, creatorID );

                                if ( shareName != null )
                                {
                                    o.pushString ( CObj.SHARE_NAME, shareName );
                                }

                                o.pushPrivate ( CObj.LOCALFILE, localFile );
                                // at least we saved doing the dig of digs compute job.
                                doDigOfDigs = false;
                                System.out.println ( "NewFileProcessor.process(): File '" + file.getName() + "' already known with dig " + fullDig );
                                break;
                            }

                        }

                    }

                }

                catch ( IOException e )
                {
                    System.err.println ( "NewFileProcessor.process(): " + e.toString() );
                    sameSizeFiles.close();
                    return true;
                }

            }

            sameSizeFiles.close();

            // Compute digest, if required
            if ( !digest ( o, doFullDig, doDigOfDigs ) )
            {
                return true;
            }

            // In case that we did not compute the dig of digs, we need to get this from the other has file
            if ( !doDigOfDigs )
            {
                String wdig = o.getString ( CObj.FILEDIGEST );
                String pdig = o.getString ( CObj.FRAGDIGEST );
                System.out.println ( "NewFileProcessor.process(): Copying fragments for file with dig " + wdig );

                CObjList frags = index.getFragments ( communityID, wdig, pdig );

                for ( int i = 0; i < frags.size(); i++ )
                {
                    try
                    {
                        CObj frag = frags.get ( i );
                        // push the community ID and the local file to each copied fragment
                        frag.pushString ( CObj.COMMUNITY, communityID );
                        frag.pushPrivate ( CObj.LOCALFILE, file.getPath() );
                        frag.simpleDigest();
                        index.index ( frag );
                    }

                    catch ( IOException e )
                    {
                        System.out.println ( e.toString() );
                        frags.close();
                        return true;
                    }

                }

                frags.close();
            }

            if ( !fullCopy )
            {
                //Set the file size
                o.pushNumber ( CObj.FILESIZE, file.length() );
                //Indicate we (still) have the file
                o.pushString ( CObj.STILLHASFILE, CObj.TRUE );
            }

            o.pushPrivate ( CObj.PRV_PUSH_REQ, CObj.TRUE );
            o.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

            if ( !hsc.createHasFile ( o ) )
            {
                guicallback.update ( o );
                return true;
            }

            hsc.updateFileInfo ( o );
            guicallback.update ( o );
        }

        return false;
    }


    public GuiCallback getGuiCallback()
    {
        return guicallback;
    }


    private boolean digest ( CObj o, boolean doFullDig, boolean doDigOfDigs )
    {
        // if there is nothing asked to do, just return true
        if ( !doFullDig && !doDigOfDigs )
        {
            return true;
        }

        String communityID = o.getString ( CObj.COMMUNITYID );
        String localFile = o.getPrivate ( CObj.LOCALFILE );
        Long fragSize = o.getNumber ( CObj.FRAGSIZE );

        if ( communityID == null  || localFile == null || fragSize == null )
        {
            return false;
        }

        File file = new File ( localFile );

        try
        {
            CObj updateMsg = new CObj();
            updateMsg.pushString ( CObj.ERROR, "Adding new file: " + file.getName() );
            updateMsg.pushPrivate ( CObj.PRV_CLEAR_ERR, "false" );
            guicallback.update ( updateMsg );

            SHA256Digest fullDig = null;

            if ( doFullDig )
            {
                fullDig = new SHA256Digest();
            }

            List<byte[]> fragDigList = null;
            RIPEMD256Digest digOfDig = null;

            if ( doDigOfDigs )
            {
                fragDigList = new LinkedList<byte[]>();
                digOfDig = new RIPEMD256Digest();
            }

            long flvl = fragSize;
            int flv = ( int ) flvl;
            byte buffer[] = new byte[flv];

            FileInputStream fis = new FileInputStream ( file );
            long idx = 0;

            while ( idx < file.length() )
            {

                long mlenl = Math.min ( flvl, ( file.length() - idx ) );
                idx += mlenl;
                int mlen = ( int ) mlenl;
                int bidx = 0;

                while ( bidx < mlen )
                {
                    int rlen = fis.read ( buffer, bidx, mlen - bidx );

                    if ( rlen > 0 )
                    {
                        bidx += rlen;
                    }

                }

                if ( doFullDig )
                {
                    //Add to the complete digest
                    fullDig.update ( buffer, 0, mlen );
                }

                if ( doDigOfDigs )
                {
                    RIPEMD256Digest pdig = new RIPEMD256Digest();
                    pdig.update ( buffer, 0, mlen );
                    byte fdig[] = new byte[pdig.getDigestSize()];
                    pdig.doFinal ( fdig, 0 );
                    fragDigList.add ( fdig );
                    //Add to dig of digs
                    digOfDig.update ( fdig, 0, fdig.length );
                }

            }

            fis.close();

            String fulldigs;

            if ( doFullDig )
            {
                //Finalize full dig and dig of dig.
                byte fullDigBytes[] = new byte[fullDig.getDigestSize()];
                fullDig.doFinal ( fullDigBytes, 0 );
                //Add the full dig to the has file object
                fulldigs = Utils.toString ( fullDigBytes );
                o.pushString ( CObj.FILEDIGEST, fulldigs );
            }

            // if we only doDigOfDigs, we need to rely on getting the full digs from CObj o
            else
            {
                fulldigs = o.getString ( CObj.FILEDIGEST );

                if ( fulldigs == null )
                {
                    return false;
                }

            }

            if ( doDigOfDigs )
            {
                byte digDigBytes[] = new byte[digOfDig.getDigestSize()];
                digOfDig.doFinal ( digDigBytes, 0 );
                String digdigs = Utils.toString ( digDigBytes );

                //Add the dig of digs to the has file object
                o.pushString ( CObj.FRAGDIGEST, digdigs );
                o.pushNumber ( CObj.FRAGNUMBER, fragDigList.size() );

                //Create the actual fragment objects
                idx = 0;

                for ( byte fd[] : fragDigList )
                {
                    long mlenl = Math.min ( flvl, ( file.length() - idx ) );
                    //KEEP THESE AS SMALL AS POSSIBLE!
                    CObj fobj = new CObj();
                    fobj.setType ( CObj.FRAGMENT );
                    // TODO: Why tag a fragment with the community ID?
                    // Isn't it enough to just know the fragment,
                    // regardless of whether the file is shared in more than one community?
                    // Same for the local file. We can identify where the fragment belongs to by the file digest, can't we?
                    fobj.pushString ( CObj.COMMUNITYID, communityID );
                    fobj.pushString ( CObj.FRAGDIG, Utils.toString ( fd ) );
                    fobj.pushString ( CObj.FILEDIGEST, fulldigs );
                    fobj.pushString ( CObj.FRAGDIGEST, digdigs );
                    fobj.pushNumber ( CObj.FRAGOFFSET, idx );
                    fobj.pushNumber ( CObj.FRAGSIZE, mlenl );
                    fobj.pushPrivate ( CObj.LOCALFILE, file.getPath() );
                    fobj.pushPrivate ( CObj.COMPLETE, CObj.TRUE );
                    fobj.simpleDigest();
                    index.index ( fobj );
                    idx += mlenl;
                }

            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            o.pushString ( CObj.ERROR, "Failed to process file: " + localFile + " " + e.getMessage() );
            guicallback.update ( o );
            return false;
        }

        return true;
    }

}
