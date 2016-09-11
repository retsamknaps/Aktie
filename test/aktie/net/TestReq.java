package aktie.net;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import aktie.data.CObj;
import aktie.data.RequestFile;

import static org.junit.Assert.*;


public class TestReq implements GetSendData2
{

    private ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<Object>();
    private String expLocalDest;
    private String expRemoteDest;

    public void enqueue ( Object o )
    {
        queue.add ( o );
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

    @Override
    public Object nextNonFile ( String localdest, String remotedest, Set<String> members, Set<String> subs )
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

    @Override
    public Object nextFile ( String localdest, String remotedest, Set<RequestFile> hasfiles )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<RequestFile> getHasFileForConnection ( String remotedest, Set<String> subs )
    {
        // TODO Auto-generated method stub
        return null;
    }

    long lastupdate = 0;
    @Override
    public long getLastFileUpdate()
    {
        return ++lastupdate;
    }

	@Override
	public ConcurrentMap<String, ConcurrentLinkedQueue<CObj>> getPrivSubRequests() {
		// TODO Auto-generated method stub
		return null;
	}

}
