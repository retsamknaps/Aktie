package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class UsrReqPrvMsgProcessor extends GenericProcessor
{

    private IdentityManager identManager;

    public UsrReqPrvMsgProcessor ( IdentityManager i )
    {
        identManager = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_PRVMSG_UPDATE.equals ( type ) )
        {
            identManager.requestPrvIdentMsg();
            return true;
        }

        return false;
    }

}
