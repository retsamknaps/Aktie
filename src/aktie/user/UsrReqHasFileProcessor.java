package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class UsrReqHasFileProcessor extends GenericProcessor
{
    public static final long DEFAULT_PRIORITY = 5L;

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
            long priority = DEFAULT_PRIORITY;
            Long pril = b.getNumber ( CObj.PRIORITY );

            if ( pril != null )
            {
                priority = pril;
            }

            String comid = b.getString ( CObj.COMMUNITYID );

            if ( comid == null )
            {
                identManager.requestAllHasFiles ( ( int ) priority );
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
