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
            long priority = 5;
            Long pril = b.getNumber ( CObj.PRIORITY );

            if ( pril != null )
            {
                priority = pril;
            }

            String comid = b.getString ( CObj.COMMUNITYID );

            if ( comid == null )
            {
                identManager.requestAllSubscriptions ( ( int ) priority );
            }

            else
            {
                identManager.requestSubscriptions ( comid, ( int ) priority );
            }

            return true;
        }

        return false;
    }

}
