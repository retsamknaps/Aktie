package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.data.CObj;

public class UsrReqMemProcessor extends GenericNoContextProcessor
{

    private IdentityManager identManager;

    public UsrReqMemProcessor ( IdentityManager i )
    {
        identManager = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_MEMBER_UPDATE.equals ( type ) )
        {
            identManager.requestMembers();
            return true;
        }

        return false;
    }

}
