package aktie.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class RequestSequencedData
{

    @Id
    @GeneratedValue
    private long id;
    private String contextId;
    private String userId;
    private String type;
    private long firstNumber;
    private long lastNumber;
    private int priority;
    private long lastRequest;

    public long getId()
    {
        return id;
    }

    public void setId ( long id )
    {
        this.id = id;
    }

    public String getContextId()
    {
        return contextId;
    }

    public void setContextId ( String contextId )
    {
        this.contextId = contextId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId ( String userId )
    {
        this.userId = userId;
    }

    public String getType()
    {
        return type;
    }

    public void setType ( String type )
    {
        this.type = type;
    }

    public long getFirstNumber()
    {
        return firstNumber;
    }

    public void setFirstNumber ( long firstNumber )
    {
        this.firstNumber = firstNumber;
    }

    public long getLastNumber()
    {
        return lastNumber;
    }

    public void setLastNumber ( long lastNumber )
    {
        this.lastNumber = lastNumber;
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
