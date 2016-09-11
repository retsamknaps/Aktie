package aktie.sequences;

import java.util.Date;

import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;

public class SubSequence extends AbstractSequence<IdentityData>
{

    public SubSequence ( HH2Session s )
    {
        super ( IdentityData.class, s );
    }

    @Override
    public String getId()
    {
        if ( getObj() != null )
        {
            return getObj().getId();
        }

        return staticid;
    }

    private String staticid;
    @Override
    public void setId ( String id )
    {
        if ( getObj() != null )
        {
            getObj().setId ( id );
        }

        staticid = id;
    }

    @Override
    public long getLastNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getLastSubNumber();
        }

        return 0;
    }

    @Override
    public void setLastNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastSubNumber ( ln );
        }

    }

    @Override
    public long getNextClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNextClosestSubNumber();
        }

        return 0;
    }

    @Override
    public void setNextClosestNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setNextClosestSubNumber ( ln );
        }

    }

    @Override
    public int getNumClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNumClosestSubNumber();
        }

        return 0;
    }

    @Override
    public void setNumClosestNumber ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setNumClosestSubNumber ( ln );
        }

    }

    @Override
    public long getLastUpdate()
    {
        if ( getObj() != null )
        {
            return getObj().getLastSubUpdate();
        }

        return 0;
    }

    @Override
    public void setLastUpdate ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastSubUpdate ( ln );
        }

    }

    @Override
    public int getStatus()
    {
        if ( getObj() != null )
        {
            return getObj().getSubStatus();
        }

        return 0;
    }

    @Override
    public void setStatus ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setSubStatus ( ln );
        }

    }

    @Override
    public int getUpdatePriority()
    {
        if ( getObj() != null )
        {
            return getObj().getSubUpdatePriority();
        }

        return 0;
    }

    @Override
    public void setUpdatePriority ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setSubUpdatePriority ( ln );
        }

    }

    @Override
    public int getUpdateCycle()
    {
        if ( getObj() != null )
        {
            return getObj().getSubUpdateCycle();
        }

        return 0;
    }

    @Override
    public void setUpdateCycle ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setSubUpdateCycle ( ln );
        }

    }

    @Override
    public IdentityData createNewObj ( CObj c )
    {
        if ( c != null )
        {
            IdentityData idat = new IdentityData();
            idat.setFirstSeen ( ( new Date() ).getTime() );
            idat.setId ( c.getId() );
            setObj ( idat );
            return idat;
        }

        return null;
    }



    @Override
    public IdentityData createNewObj ( String... id )
    {
        if ( id != null && id.length == 1 )
        {
            staticid = id[0];
            IdentityData idat = new IdentityData();
            idat.setFirstSeen ( ( new Date() ).getTime() );
            idat.setId ( staticid );
            setObj ( idat );
            return idat;
        }

        return null;
    }

}
