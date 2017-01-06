package aktie.net;

import org.bouncycastle.crypto.params.RSAKeyParameters;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.index.Index;
import aktie.user.IdentityManager;

public class InCheckDigProcessor extends GenericProcessor
{

    private Index index;
    private ConnectionThread conThread;
    private IdentityManager identManager;

    public InCheckDigProcessor ( ConnectionThread ct, Index i, IdentityManager im )
    {
        conThread = ct;
        index = i;
        identManager = im;
    }

    @Override
    public boolean process ( CObj b )
    {
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
                RSAKeyParameters pubk = Utils.publicKeyFromString ( pubkey );

                if ( b.checkSignatureX ( pubk, 0 ) )
                {
                    System.out.println ( "PENDING DIGS: " + conThread.getLocalDestination().getIdentity().getId() + " from " +
                                         conThread.getEndDestination().getId() + " size: " +
                                         conThread.getDigReq().size() );

                    if ( conThread.getDigReq().remove ( b.getDig() ) )
                    {
                        if ( conThread.getDigReq().isEmpty() )
                        {
                            CObj rid = conThread.getEndDestination();

                            if ( rid != null )
                            {
                                String id = rid.getId();
                                identManager.updateGlobalSequenceNumber ( id,
                                        conThread.getLastSeq() );
                            }

                        }

                    }

                }

            }

        }

        return false;
    }

}
