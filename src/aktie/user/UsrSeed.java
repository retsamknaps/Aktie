package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.net.InIdentityProcessor;

public class UsrSeed extends GenericProcessor
{

    private InIdentityProcessor identProcessor;

    public UsrSeed ( HH2Session s, Index i, GuiCallback cb )
    {
        identProcessor = new InIdentityProcessor ( s, i, cb );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_SEED.equals ( type ) )
        {
            b.setType ( CObj.IDENTITY );
            identProcessor.process ( b );
        }

        return false;
    }

}
