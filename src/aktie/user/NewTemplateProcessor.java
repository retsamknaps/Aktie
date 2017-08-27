package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.UpdateCallback;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.Index;

public class NewTemplateProcessor extends GenericNoContextProcessor
{

    //private GuiCallback guicallback;
    //private Index index;
    //private HH2Session session;

    public NewTemplateProcessor ( HH2Session s, Index i, UpdateCallback cb )
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
