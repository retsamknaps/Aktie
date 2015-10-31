package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class InFileModeProcessor extends GenericProcessor
{

    private ConnectionThread connection;

    public InFileModeProcessor ( ConnectionThread c )
    {
        connection = c;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.CON_FILEMODE.equals ( type ) )
        {
            connection.setFileMode ( true );
            return true;
        }

        return false;
    }

}
