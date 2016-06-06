package aktie.user;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.crypto.digests.RIPEMD256Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
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
        hsc = new HasFileCreator ( s, i, st );
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
            String comid = o.getString ( CObj.COMMUNITYID );
            String creator = o.getString ( CObj.CREATOR );

            if ( comid == null || creator == null )
            {
                o.pushString ( CObj.ERROR, "No community or creator specified" );
                guicallback.update ( o );
                return true;
            }

            //Digest whole file.
            String wfs = o.getPrivate ( CObj.LOCALFILE );

            if ( wfs == null )
            {
                o.pushString ( CObj.ERROR, "No local file specified" );
                guicallback.update ( o );
                return true;
            }

            File f = new File ( wfs );

            if ( !f.exists() || !f.isFile() )
            {
                o.pushString ( CObj.ERROR, "Local file does not exist: " + wfs );
                guicallback.update ( o );
                return true;
            }

            //Set the file name if not set
            String name = o.getString ( CObj.NAME );

            if ( name == null )
            {
                name = f.getName();
                o.pushString ( CObj.NAME, name );
            }

            //Get the fragment size, set if necessary
            Long fsize = o.getNumber ( CObj.FRAGSIZE );

            if ( fsize == null )
            {
                fsize = 512L * 1024L; //500K
                o.pushNumber ( CObj.FRAGSIZE, fsize );
            }

            if ( fsize > 20L * 1024L * 1024L ) //20MB
            {
                o.pushString ( CObj.ERROR, "Fragment size is too large " + fsize );
                guicallback.update ( o );
                return true;
            }

            //Set the file size
            o.pushNumber ( CObj.FILESIZE, f.length() );
            //Indicate we (still) have the file
            o.pushString ( CObj.STILLHASFILE, "true" );

            //Process file
            try
            {
                CObj updatemsg = new CObj();
                updatemsg.pushString ( CObj.ERROR, "Adding new file: " + f.getName() );
                updatemsg.pushPrivate ( CObj.PRV_CLEAR_ERR, "false" );
                guicallback.update ( updatemsg );

                List<byte[]> fragdiglst = new LinkedList<byte[]>();
                long flvl = fsize;
                int flv = ( int ) flvl;
                byte buf[] = new byte[flv];
                RIPEMD256Digest digofdig = new RIPEMD256Digest();
                SHA256Digest fulldig = new SHA256Digest();
                FileInputStream fis = new FileInputStream ( f );
                long idx = 0;

                while ( idx < f.length() )
                {

                    long mlenl = Math.min ( flvl, ( f.length() - idx ) );
                    idx += mlenl;
                    int mlen = ( int ) mlenl;
                    int bidx = 0;

                    while ( bidx < mlen )
                    {
                        int rlen = fis.read ( buf, bidx, mlen - bidx );

                        if ( rlen > 0 )
                        {
                            bidx += rlen;
                        }

                    }

                    RIPEMD256Digest pdig = new RIPEMD256Digest();
                    pdig.update ( buf, 0, mlen );
                    byte fdig[] = new byte[pdig.getDigestSize()];
                    pdig.doFinal ( fdig, 0 );
                    fragdiglst.add ( fdig );
                    //Add to the complete digest
                    fulldig.update ( buf, 0, mlen );
                    //Add to dig of digs
                    digofdig.update ( fdig, 0, fdig.length );
                }

                fis.close();
                //Finalize full dig and dig of dig.
                byte fulldigb[] = new byte[fulldig.getDigestSize()];
                fulldig.doFinal ( fulldigb, 0 );
                byte digdigb[] = new byte[digofdig.getDigestSize()];
                digofdig.doFinal ( digdigb, 0 );
                //Add the full dig and dig of digs to the has file object
                String fulldigs = Utils.toString ( fulldigb );
                String digdigs = Utils.toString ( digdigb );
                o.pushString ( CObj.FILEDIGEST, fulldigs );
                o.pushString ( CObj.FRAGDIGEST, digdigs );
                o.pushNumber ( CObj.FRAGNUMBER, fragdiglst.size() );

                //Create the actual fragment objects
                idx = 0;

                for ( byte fd[] : fragdiglst )
                {
                    long mlenl = Math.min ( flvl, ( f.length() - idx ) );
                    //KEEP THESE AS SMALL AS POSSIBLE!
                    CObj fobj = new CObj();
                    fobj.setType ( CObj.FRAGMENT );
                    fobj.pushString ( CObj.COMMUNITYID, comid );
                    fobj.pushString ( CObj.FRAGDIG, Utils.toString ( fd ) );
                    fobj.pushString ( CObj.FILEDIGEST, fulldigs );
                    fobj.pushString ( CObj.FRAGDIGEST, digdigs );
                    fobj.pushNumber ( CObj.FRAGOFFSET, idx );
                    fobj.pushNumber ( CObj.FRAGSIZE, mlenl );
                    fobj.pushPrivate ( CObj.LOCALFILE, f.getPath() );
                    fobj.pushPrivate ( CObj.COMPLETE, "true" );
                    fobj.simpleDigest();
                    index.index ( fobj );
                    idx += mlenl;
                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
                o.pushString ( CObj.ERROR, "Failed to process file: " + wfs + " " + e.getMessage() );
                guicallback.update ( o );
                return true;
            }

            o.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
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

}
