package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class UsrReqSubProcessor extends GenericProcessor
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
