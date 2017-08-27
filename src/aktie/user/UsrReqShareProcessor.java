package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.data.CObj;

public class UsrReqShareProcessor extends GenericNoContextProcessor
{

    private ShareManager manager;

    public UsrReqShareProcessor ( ShareManager m )
    {
        manager = m;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_SHARE_MGR.equals ( type ) )
        {
            boolean on = true;
            String of = b.getString ( CObj.ENABLED );

            if ( "false".equals ( of ) )
            {
                on = false;
            }

            manager.setEnabled ( on );

            return true;
        }

        return false;
    }


}
