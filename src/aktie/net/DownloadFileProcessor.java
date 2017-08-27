package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.utils.HasFileCreator;

public class DownloadFileProcessor extends GenericProcessor
{

    private HasFileCreator hfc;

    public DownloadFileProcessor ( HasFileCreator h )
    {
        hfc = h;
    }

    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.HASFILE.equals ( type ) )
        {
            hfc.createHasFile ( o );
            hfc.updateFileInfo ( o );
            return true;
        }

        return false;
    }

    @Override
    public void setContext ( Object c )
    {

    }

}
