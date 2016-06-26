package aktie.utils;

import org.bouncycastle.crypto.params.RSAKeyParameters;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.index.Index;
import aktie.spam.SpamTool;

public class DigestValidator
{

    private Index index;
    private SpamTool spamtool;

    public DigestValidator ( Index i, SpamTool st )
    {
        index = i;
        spamtool = st;
    }

    public boolean valid ( CObj b )
    {
        //We only care if we don't already have it.
        //Now find the creator.
        String creatorid = b.getString ( CObj.CREATOR );

        if ( creatorid == null )
        {
            return false;
        }

        CObj idty = index.getIdentity ( creatorid );

        if ( idty != null )
        {
            //Verify the signature using the creator's key
            String pubkey = idty.getString ( CObj.KEY );

            if ( pubkey != null )
            {

                //Update the community sequence number if greater
                RSAKeyParameters pubk = Utils.publicKeyFromString ( pubkey );

                if ( spamtool.check ( pubk, idty, b ) )
                {
                    return true;
                }

                else
                {
                    //Because we are nice and do not want to reset the network
                    //because we added id to has file, so that we only save
                    //one hasfile record per node per community per file.
                    if ( CObj.HASFILE.equals ( b.getType() ) )
                    {
                        CObj chk = b.clone();
                        chk.setId ( null );

                        if ( spamtool.check ( pubk, idty, chk ) )
                        {
                            return true;
                        }

                    }

                }

            }

        }

        return false;

    }

    public boolean newAndValidX ( CObj b )
    {
        String digid = b.getDig();

        if ( digid != null )
        {
            //See if we already have it.
            CObj co = index.getByDig ( digid );

            if ( co == null )
            {
                return valid ( b );
            }

        }

        return false;
    }

}
