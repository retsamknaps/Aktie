package aktie.net;

import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class InCheckMemSubProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private ConnectionThread conThread;

    public InCheckMemSubProcessor ( ConnectionThread ct )
    {
        conThread = ct;
    }

    private void log ( String msg )
    {
        if ( conThread.getEndDestination() != null )
        {
            log.info ( "ME: " + conThread.getLocalDestination().getIdentity().getId() +
                       " FROM: " + conThread.getEndDestination().getId() + " " + msg );
        }

    }

    @Override
    public boolean process ( CObj b )
    {
        if ( CObj.CHECKSUB.equals ( b.getType() ) )
        {
            String d = b.getDig();

            if ( d != null )
            {
                log ( "CHECK SUB: " + d );
                conThread.addChkSub ( d );
            }

            return true;
        }

        else if ( CObj.CHECKMEM.equals ( b.getType() ) )
        {
            String d = b.getDig();

            if ( d != null )
            {
                log ( "CHECK MEM: " + d );
                conThread.addChkMem ( d );
            }

            return true;
        }

        else if ( CObj.CHECKCOMP.equals ( b.getType() ) )
        {
            log ( "CHECK DONE" );
            conThread.checkDone();
        }

        return false;
    }

}
