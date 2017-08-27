package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.UpdateCallback;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.Index;
import aktie.net.InComProcessor;
import aktie.spam.SpamTool;

public class UsrSeedCommunity extends GenericNoContextProcessor
{
    private InComProcessor comProcessor;
    private UpdateCallback callback;

    public UsrSeedCommunity ( HH2Session s, Index i, SpamTool st, UpdateCallback cb )
    {
        comProcessor = new InComProcessor ( s, i, st, null );
        callback = cb;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_COMMUNITY.equals ( type ) )
        {
            b.setType ( CObj.COMMUNITY );
            comProcessor.setCallback ( callback );
            comProcessor.process ( b );
        }

        return false;
    }


}
