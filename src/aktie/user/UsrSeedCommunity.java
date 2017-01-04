package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.net.InComProcessor;
import aktie.spam.SpamTool;

public class UsrSeedCommunity extends GenericProcessor
{
    private InComProcessor comProcessor;

    public UsrSeedCommunity ( HH2Session s, Index i, SpamTool st, GuiCallback cb )
    {
        comProcessor = new InComProcessor ( s, i, st, null, null, cb );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_COMMUNITY.equals ( type ) )
        {
            b.setType ( CObj.COMMUNITY );
            comProcessor.process ( b );
        }

        return false;
    }


}
