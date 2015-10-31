package aktie.net;

import java.util.concurrent.ConcurrentLinkedQueue;
import static org.junit.Assert.*;


public class TestReq implements GetSendData
{

    private ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<Object>();
    private String expLocalDest;
    private String expRemoteDest;

    public void enqueue ( Object o )
    {
        queue.add ( o );
    }

    @Override
    public Object next ( String localdest, String remotedest, boolean filemode )
    {
        System.out.println ( "CALLING NEXT: " + localdest + " -> " + remotedest );

        if ( expLocalDest != null )
        {
            assertEquals ( expLocalDest, localdest );
        }

        if ( expRemoteDest != null )
        {
            assertEquals ( expRemoteDest, remotedest );
        }

        Object so = queue.poll();
        System.out.println ( "SO: " + so );
        return so;
    }

    public String getExpLocalDest()
    {
        return expLocalDest;
    }

    public void setExpLocalDest ( String expLocalDest )
    {
        this.expLocalDest = expLocalDest;
    }

    public String getExpRemoteDest()
    {
        return expRemoteDest;
    }

    public void setExpRemoteDest ( String expRemoteDest )
    {
        this.expRemoteDest = expRemoteDest;
    }

}
