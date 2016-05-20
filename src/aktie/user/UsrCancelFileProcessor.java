package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.RequestFile;
import aktie.gui.GuiCallback;

public class UsrCancelFileProcessor extends GenericProcessor
{

    private RequestFileHandler fileHandler;
    private GuiCallback callback;

    public UsrCancelFileProcessor ( RequestFileHandler rf, GuiCallback cb )
    {
        fileHandler = rf;
        callback = cb;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_CANCEL_DL.equals ( type ) )
        {
            String lf = b.getString ( CObj.LOCALFILE );

            if ( lf != null )
            {
                RequestFile rf = fileHandler.findFileByName ( lf );

                if ( rf != null )
                {
                    fileHandler.cancelDownload ( rf );
                    callback.update ( rf );
                }

            }

            return true;
        }

        return false;
    }

}
