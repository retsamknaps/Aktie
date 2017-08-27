package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.data.CObj;

public class UsrReqHasFileProcessor extends GenericNoContextProcessor
{

    private IdentityManager identManager;

    public UsrReqHasFileProcessor ( IdentityManager i )
    {
        identManager = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_HASFILE_UPDATE.equals ( type ) )
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
                identManager.requestAllHasFile ( ( int ) priority );
            }

            else
            {
                identManager.requestHasFile ( comid, ( int ) priority );
            }

            return true;
        }

        return false;
    }

}
