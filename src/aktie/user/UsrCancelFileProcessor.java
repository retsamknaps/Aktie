package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.UpdateCallback;
import aktie.data.CObj;
import aktie.data.RequestFile;

public class UsrCancelFileProcessor extends GenericNoContextProcessor
{

    private RequestFileHandler fileHandler;
    private UpdateCallback callback;

    public UsrCancelFileProcessor ( RequestFileHandler rf, UpdateCallback cb )
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
