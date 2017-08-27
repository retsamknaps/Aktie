package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class InFileModeProcessor extends GenericProcessor
{

    private ConnectionThread connection;

    public InFileModeProcessor ( )
    {
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

    @Override
    public void setContext ( Object c )
    {
        connection = ( ConnectionThread ) c;

    }

}
