package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.data.CObj;

public class UsrReqSubProcessor extends GenericNoContextProcessor
{

    private IdentityManager identManager;

    public UsrReqSubProcessor ( IdentityManager i )
    {
        identManager = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_SUB_UPDATE.equals ( type ) )
        {
            identManager.requestAllSubscriptions ( );
            return true;
        }

        return false;
    }

}
