package aktie.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class DeveloperIdentity
{

    @Id
    private String id;

    private long lastSpamExNumber;
    private long nextClosestSpamExNumber;
    private int  numClosestSpamExNumber;

    private long lastSpamExUpdate;
    private int spamExStatus;
    private int spamExUpdatePriority;

    @Column ( columnDefinition = "INTEGER(10) default 0" )
    private int spamExUpdateCycle;

    public DeveloperIdentity()
    {
    }

    public String getId()
    {
        return id;
    }

    public void setId ( String id )
    {
        this.id = id;
    }

    public long getLastSpamExNumber()
    {
        return lastSpamExNumber;
    }

    public void setLastSpamExNumber ( long lastSpamExNumber )
    {
        this.lastSpamExNumber = lastSpamExNumber;
    }

    public long getNextClosestSpamExNumber()
    {
        return nextClosestSpamExNumber;
    }

    public void setNextClosestSpamExNumber ( long nextClosestSpamExNumber )
    {
        this.nextClosestSpamExNumber = nextClosestSpamExNumber;
    }

    public int getNumClosestSpamExNumber()
    {
        return numClosestSpamExNumber;
    }

    public void setNumClosestSpamExNumber ( int numClosestSpamExNumber )
    {
        this.numClosestSpamExNumber = numClosestSpamExNumber;
    }

    public long getLastSpamExUpdate()
    {
        return lastSpamExUpdate;
    }

    public void setLastSpamExUpdate ( long lastSpamExUpdate )
    {
        this.lastSpamExUpdate = lastSpamExUpdate;
    }

    public int getSpamExStatus()
    {
        return spamExStatus;
    }

    public void setSpamExStatus ( int spamExStatus )
    {
        this.spamExStatus = spamExStatus;
    }

    public int getSpamExUpdatePriority()
    {
        return spamExUpdatePriority;
    }

    public void setSpamExUpdatePriority ( int spamExUpdatePriority )
    {
        this.spamExUpdatePriority = spamExUpdatePriority;
    }

    public int getSpamExUpdateCycle()
    {
        return spamExUpdateCycle;
    }

    public void setSpamExUpdateCycle ( int spamExUpdateCycle )
    {
        this.spamExUpdateCycle = spamExUpdateCycle;
    }


}
