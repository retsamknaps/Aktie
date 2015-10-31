package aktie.data;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
    Request identity information from another node

*/
@Entity
public class RequestIdentities
{

    @Id
    private String id;
    private int priority;
    private long lastRequest;

    public String getId()
    {
        return id;
    }

    public void setId ( String id )
    {
        this.id = id;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority ( int priority )
    {
        this.priority = priority;
    }

    public long getLastRequest()
    {
        return lastRequest;
    }

    public void setLastRequest ( long lastRequest )
    {
        this.lastRequest = lastRequest;
    }

}
