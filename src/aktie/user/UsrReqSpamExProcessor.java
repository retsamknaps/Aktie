package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.data.CObj;

public class UsrReqSpamExProcessor extends GenericNoContextProcessor
{

    private IdentityManager identManager;

    public UsrReqSpamExProcessor ( IdentityManager i )
    {
        identManager = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_SPAMEX_UPDATE.equals ( type ) )
        {
            identManager.requestSpamEx();
            return true;
        }

        return false;
    }

}
