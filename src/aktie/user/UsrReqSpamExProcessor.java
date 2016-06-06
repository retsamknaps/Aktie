package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class UsrReqSpamExProcessor extends GenericProcessor
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
