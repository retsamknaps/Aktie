package aktie.data;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.json.JSONObject;

@Entity
public class PrivateMsgIdentity
{

    @Id
    private String id;

    private long lastMsgNumber;
    private long nextClosestMsgNumber;
    private int numClosestMsgNumber;
    private long lastMsgUpdate;
    private int msgStatus;
    private int msgUpdatePriority;
    private int msgUpdateCycle;

    private long lastIdentNumber;
    private long nextClosestIdentNumber;
    private int numClosestIdentNumber;
    private long lastIdentUpdate;
    private int identStatus;
    private int identUpdatePriority;
    private int identUpdateCycle;

    private boolean mine;

    public PrivateMsgIdentity()
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

    public long getLastMsgNumber()
    {
        return lastMsgNumber;
    }

    public void setLastMsgNumber ( long lastMsgNumber )
    {
        this.lastMsgNumber = lastMsgNumber;
    }

    public long getNextClosestMsgNumber()
    {
        return nextClosestMsgNumber;
    }

    public void setNextClosestMsgNumber ( long nextClosestMsgNumber )
    {
        this.nextClosestMsgNumber = nextClosestMsgNumber;
    }

    public int getNumClosestMsgNumber()
    {
        return numClosestMsgNumber;
    }

    public void setNumClosestMsgNumber ( int numClosestMsgNumber )
    {
        this.numClosestMsgNumber = numClosestMsgNumber;
    }

    public long getLastMsgUpdate()
    {
        return lastMsgUpdate;
    }

    public void setLastMsgUpdate ( long lastMsgUpdate )
    {
        this.lastMsgUpdate = lastMsgUpdate;
    }

    public int getMsgStatus()
    {
        return msgStatus;
    }

    public void setMsgStatus ( int msgStatus )
    {
        this.msgStatus = msgStatus;
    }

    public int getMsgUpdatePriority()
    {
        return msgUpdatePriority;
    }

    public void setMsgUpdatePriority ( int msgUpdatePriority )
    {
        this.msgUpdatePriority = msgUpdatePriority;
    }

    public boolean isMine()
    {
        return mine;
    }

    public void setMine ( boolean mine )
    {
        this.mine = mine;
    }

    public long getLastIdentNumber()
    {
        return lastIdentNumber;
    }

    public void setLastIdentNumber ( long lastIdentNumber )
    {
        this.lastIdentNumber = lastIdentNumber;
    }

    public long getNextClosestIdentNumber()
    {
        return nextClosestIdentNumber;
    }

    public void setNextClosestIdentNumber ( long nextClosestIdentNumber )
    {
        this.nextClosestIdentNumber = nextClosestIdentNumber;
    }

    public int getNumClosestIdentNumber()
    {
        return numClosestIdentNumber;
    }

    public void setNumClosestIdentNumber ( int numClosestIdentNumber )
    {
        this.numClosestIdentNumber = numClosestIdentNumber;
    }

    public long getLastIdentUpdate()
    {
        return lastIdentUpdate;
    }

    public void setLastIdentUpdate ( long lastIdentUpdate )
    {
        this.lastIdentUpdate = lastIdentUpdate;
    }

    public int getIdentStatus()
    {
        return identStatus;
    }

    public void setIdentStatus ( int identStatus )
    {
        this.identStatus = identStatus;
    }

    public int getIdentUpdatePriority()
    {
        return identUpdatePriority;
    }

    public void setIdentUpdatePriority ( int identUpdatePriority )
    {
        this.identUpdatePriority = identUpdatePriority;
    }

    public int getMsgUpdateCycle()
    {
        return msgUpdateCycle;
    }

    public void setMsgUpdateCycle ( int msgUpdateCycle )
    {
        this.msgUpdateCycle = msgUpdateCycle;
    }

    public int getIdentUpdateCycle()
    {
        return identUpdateCycle;
    }

    public void setIdentUpdateCycle ( int identUpdateCycle )
    {
        this.identUpdateCycle = identUpdateCycle;
    }

    public String toString() {
    	JSONObject o = new JSONObject(this);
    	return o.toString(4);
    }


}
