package aktie.user;

import java.io.IOException;
import java.util.logging.Logger;

import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import aktie.GenericNoContextProcessor;
import aktie.UpdateCallback;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.DeveloperIdentity;
import aktie.index.Index;

public class NewDeveloperProcessor extends GenericNoContextProcessor
{

    public static boolean TESTEVILDEV = false;

    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private IdentityManager identManager;
    private UpdateCallback callback;

    public NewDeveloperProcessor ( Index i, IdentityManager m, UpdateCallback cb )
    {
        identManager = m;
        index = i;
        callback = cb;
    }

    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.DEVELOPER.equals ( type ) )
        {
            String creator = o.getString ( CObj.CREATOR );

            if ( creator == null )
            {
                o.pushString ( CObj.ERROR, "Creator must be specified." );
                callback.update ( o );
                return true;
            }

            if ( !TESTEVILDEV )
            {

                DeveloperIdentity di = identManager.getDeveloperIdentity ( creator );

                if ( di == null )
                {
                    o.pushString ( CObj.ERROR, "Developer Identity not specified." );
                    callback.update ( o );
                    return true;
                }

            }

            CObj devid = index.getMyIdentity ( creator );

            if ( devid == null )
            {
                o.pushString ( CObj.ERROR, "Not your identity." );
                callback.update ( o );
                return true;
            }

            RSAPrivateCrtKeyParameters pkey = Utils.privateKeyFromString ( devid.getPrivate ( CObj.PRIVATEKEY ) );

            if ( pkey == null )
            {
                o.pushString ( CObj.ERROR, "Private key not found!" );
                callback.update ( o );
                return true;
            }

            String newdevid = o.getString ( CObj.IDENTITY );

            if ( newdevid == null )
            {
                o.pushString ( CObj.ERROR,  "Developer id was not specified!" );
                callback.update ( o );
                return true;
            }

            CObj co = index.getIdentity ( newdevid );

            if ( co == null )
            {
                o.pushString ( CObj.ERROR,  "Developer id does not seem valid!" );
                callback.update ( o );
                return true;
            }

            o.signX ( pkey, 0 );
            o.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
            o.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

            long gseq = identManager.getGlobalSequenceNumber ( creator, false );
            o.pushPrivateNumber ( CObj.getGlobalSeq ( creator ), gseq );

            log.info ( "CREATING DEVELOPER: creator: " + creator + " new dev: " + newdevid + " dig: " + o.getDig() );

            try
            {
                index.index ( o );
                identManager.newDeveloperIdentity ( co.getId() );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

            return true;
        }

        return false;
    }

}
