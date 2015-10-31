package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class UsrReqComProcessor extends GenericProcessor
{

    private IdentityManager identManager;

    public UsrReqComProcessor ( IdentityManager i )
    {
        identManager = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_COMMUNITY_UPDATE.equals ( type ) )
        {
            identManager.requestCommunities();
            return true;
        }

        return false;
    }

}
