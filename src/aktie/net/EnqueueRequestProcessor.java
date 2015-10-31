package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;

public class EnqueueRequestProcessor  extends GenericProcessor
{

    private ConnectionThread connection;

    public EnqueueRequestProcessor ( ConnectionThread c )
    {
        connection = c;
    }

    @Override
    public boolean process ( CObj b )
    {
        //If it's still in the queue, then it wasn't data we want locally.
        //add it to the remote request queue.
        connection.enqueueRemoteRequest ( b );
        return true;
    }

}
