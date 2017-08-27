package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.data.CObj;

public class UsrReqPostProcessor extends GenericNoContextProcessor
{

    private IdentityManager ident;

    public UsrReqPostProcessor ( IdentityManager i )
    {
        ident = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_POST_UPDATE.equals ( type ) )
        {
            int priority = 5;
            Long plong = b.getNumber ( CObj.PRIORITY );

            if ( plong != null )
            {
                long pl = plong;
                priority = ( int ) pl;
            }

            String comid = b.getString ( CObj.COMMUNITYID );

            if ( comid == null )
            {
                ident.requestAllPosts ( priority );
            }

            else
            {
                ident.requestPosts ( comid, priority );
            }

            return true;
        }

        return false;
    }

}
