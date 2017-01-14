package aktie.net;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.crypto.params.RSAKeyParameters;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.index.Index;
import aktie.user.IdentityManager;

public class InCheckDigProcessor extends GenericProcessor
{
    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private ConnectionThread conThread;
    private IdentityManager identManager;

    public InCheckDigProcessor ( ConnectionThread ct, Index i, IdentityManager im )
    {
        conThread = ct;
        index = i;
        identManager = im;
    }

    private void log ( String msg )
    {
        if ( conThread.getEndDestination() != null )
        {
            if ( log.isLoggable ( Level.INFO ) )
            {
                log.info ( "ME: " + conThread.getLocalDestination().getIdentity().getId() +
                           " FROM: " + conThread.getEndDestination().getId() + " " + msg );
            }

        }

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
                    if ( log.isLoggable ( Level.INFO ) )
                    {
                        log ( "CHECK: " + b.getDig() );
                    }

                    if ( conThread.getDigReq().remove ( b.getDig() ) )
                    {

                        if ( log.isLoggable ( Level.INFO ) )
                        {
                            log ( "FOUND: " + b.getDig() );
                        }

                        if ( conThread.getDigReq().isEmpty() )
                        {
                            CObj rid = conThread.getEndDestination();

                            if ( rid != null )
                            {
                                if ( log.isLoggable ( Level.INFO ) )
                                {
                                    log ( "COMPLETE: PUB" + conThread.getLastPubSeq() + " MEM " +
                                          conThread.isUpdatemembers() + " " + conThread.getLastMemSeq() +
                                          " SUB " + conThread.isUpdatesubs() + " " + conThread.getLastSubSeq() );
                                }

                                String id = rid.getId();
                                identManager.updateGlobalSequenceNumber ( id,
                                        true, conThread.getLastPubSeq(),
                                        conThread.isUpdatemembers(), conThread.getLastMemSeq(),
                                        conThread.isUpdatesubs(), conThread.getLastSubSeq() );
                            }

                        }

                    }

                }

            }

        }

        return false;
    }

}
