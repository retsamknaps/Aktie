package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.Index;

public class NewTemplateProcessor extends GenericProcessor
{

    //private GuiCallback guicallback;
    //private Index index;
    //private HH2Session session;

    public NewTemplateProcessor ( HH2Session s, Index i, GuiCallback cb )
    {
        //guicallback = cb;
        //index = i;
        //session = s;
    }

    @Override
    public boolean process ( CObj b )
    {
        //TODO: Not necessary.  Posts can be accepted with any data.
        //templates are just a convenience
        return false;
    }

}
