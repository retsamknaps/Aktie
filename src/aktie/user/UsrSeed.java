package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.UpdateCallback;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.Index;
import aktie.net.InIdentityProcessor;

public class UsrSeed extends GenericNoContextProcessor
{

    private InIdentityProcessor identProcessor;
    private UpdateCallback callback;

    public UsrSeed ( HH2Session s, Index i, UpdateCallback cb )
    {
        callback = cb;
        identProcessor = new InIdentityProcessor ( s, i, null );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_SEED.equals ( type ) )
        {
            b.setType ( CObj.IDENTITY );
            identProcessor.setCallback ( callback );
            identProcessor.process ( b );
        }

        return false;
    }

}
