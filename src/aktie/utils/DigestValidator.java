package aktie.utils;

import org.bouncycastle.crypto.params.RSAKeyParameters;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.gui.Wrapper;
import aktie.index.Index;

public class DigestValidator
{

    private Index index;

    public DigestValidator ( Index i )
    {
        index = i;
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

                int payment = Wrapper.getCheckPayment();

                //No payment required for posts and files in private
                //communities
                if ( CObj.POST.equals ( b.getType() ) ||
                        CObj.HASFILE.equals ( b.getType() ) )
                {
                    String comid = b.getString ( CObj.COMMUNITYID );

                    if ( comid == null )
                    {
                        return false;
                    }

                    CObj com = index.getCommunity ( comid );

                    if ( com == null )
                    {
                        return false;
                    }

                    if ( CObj.SCOPE_PRIVATE.equals ( com.getString ( CObj.SCOPE ) ) )
                    {
                        payment = 0;
                    }

                }

                //Update the community sequence number if greater
                RSAKeyParameters pubk = Utils.publicKeyFromString ( pubkey );

                if ( b.checkSignature ( pubk, payment ) )
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

                        if ( chk.checkSignature ( pubk, payment ) )
                        {
                            return true;
                        }

                    }

                }

            }

        }

        return false;

    }

    public boolean newAndValid ( CObj b )
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
