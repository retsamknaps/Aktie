package aktie.ident;

import org.bouncycastle.crypto.params.RSAKeyParameters;

import aktie.crypto.Utils;
import aktie.data.CObj;

public class Identity
{

    public static boolean verifyIdentity ( CObj b )
    {
        if ( CObj.IDENTITY.equals ( b.getType() ) )
        {
            String pubkey = b.getString ( CObj.KEY );

            if ( pubkey != null )
            {
                RSAKeyParameters pk = Utils.publicKeyFromString ( pubkey );
                byte dig[] = Utils.digString ( ( byte[] ) null, pubkey );
                String cid = Utils.toString ( dig );

                if ( cid.equals ( b.getId() ) )
                {
                    return b.checkSignatureX ( pk, 0 );
                }

            }

        }

        return false;
    }

}
