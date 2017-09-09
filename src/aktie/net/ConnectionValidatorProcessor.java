package aktie.net;

import java.util.Arrays;

import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.ident.Identity;

public class ConnectionValidatorProcessor extends GenericProcessor
{

    public static long CKEY0 = 0x12345678abcdef00L;
    public static long CKEY1 = 0x00fedcba87654321L;

    private DestinationThread dest;
    private ConnectionThread con;
    private InIdentityProcessor IdentProcessor;

    public ConnectionValidatorProcessor ( InIdentityProcessor ip )
    {
        IdentProcessor = ip;
    }

    @Override
    public boolean process ( CObj b )
    {
        if ( con.isConfirmed() )
        {
            return false;
        }

        if ( CObj.IDENTITY.equals ( b.getType() ) )
        {
            String pubkey = b.getString ( CObj.KEY );

            if ( !Identity.verifyIdentity ( b ) )
            {
                con.stop();
            }

            else
            {
                con.setPendingEndDest ( b );
                RSAKeyParameters pk = Utils.publicKeyFromString ( pubkey );
                byte challenge[] = new byte[32];
                Utils.Random.nextBytes ( challenge );
                con.setChallenge ( challenge );
                byte enc[] = Utils.anonymousAsymEncode ( pk, CKEY0, CKEY1, challenge );
                CObj clng = new CObj();
                clng.setType ( CObj.CON_CHALLENGE );
                clng.pushString ( CObj.PAYLOAD, Utils.toString ( enc ) );
                con.enqueue ( clng );
            }

        }

        else if ( CObj.CON_CHALLENGE.equals ( b.getType() ) )
        {
            String padlock = b.getString ( CObj.PAYLOAD );

            if ( padlock == null )
            {
                con.stop();
            }

            else
            {
                byte cb[] = Utils.toByteArray ( padlock );
                String privkey = dest.getIdentity().getPrivate ( CObj.PRIVATEKEY );
                RSAPrivateCrtKeyParameters pk = Utils.privateKeyFromString ( privkey );
                byte dec[] = Utils.attemptAsymDecode ( pk, CKEY0, CKEY1, cb );

                if ( dec == null )
                {
                    con.stop();
                }

                else
                {
                    CObj ro = new CObj();
                    ro.setType ( CObj.CON_REPLY );
                    ro.pushString ( CObj.PAYLOAD, Utils.toString ( dec ) );
                    con.enqueue ( ro );
                }

            }

        }

        else if ( CObj.CON_REPLY.equals ( b.getType() ) )
        {
            String rpy = b.getString ( CObj.PAYLOAD );

            if ( rpy == null )
            {
                con.stop();
            }

            else
            {
                byte [] crpy = Utils.toByteArray ( rpy );
                con.setConfirmed ( Arrays.equals ( crpy, con.getChallenge() ) );

                if ( !con.isConfirmed() )
                {
                    con.stop();
                }

                else
                {
                    con.setEndDestination ( con.getPendingEndDest() );

                    if ( con.isFileMode() )
                    {
                        CObj filemode = new CObj();
                        filemode.setType ( CObj.CON_FILEMODE );
                        con.enqueue ( filemode );
                    }

                    else
                    {
                        //First thing to do is always request identities.
                        //We STILL DO THIS even with global sequences.
                        //Because global sequences are held off for spam
                        //exception updates.
                        CObj cr = new CObj();
                        cr.setType ( CObj.CON_REQ_IDENTITIES );
                        con.enqueue ( cr );
                    }

                    dest.addEstablishedConnection ( con );
                    IdentProcessor.process ( con.getPendingEndDest() );
                    con.poke();
                }

            }

        }

        else
        {
            con.stop();
        }

        return true;
    }

    @Override
    public void setContext ( Object c )
    {
        con = ( ConnectionThread ) c;
        dest = con.getLocalDestination();

    }

}
