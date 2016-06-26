package aktie.sequences;

import java.util.Date;

import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;

public class CommunitySequence extends AbstractSequence<IdentityData>
{

    public CommunitySequence ( HH2Session s )
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
            return getObj().getLastCommunityNumber();
        }

        return 0;
    }

    @Override
    public void setLastNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastCommunityNumber ( ln );
        }

    }

    @Override
    public long getNextClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNextClosestCommunityNumber();
        }

        return 0;
    }

    @Override
    public void setNextClosestNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setNextClosestCommunityNumber ( ln );
        }

    }

    @Override
    public int getNumClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNumClosestCommunityNumber();
        }

        return 0;
    }

    @Override
    public void setNumClosestNumber ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setNumClosestCommunityNumber ( ln );
        }

    }

    @Override
    public long getLastUpdate()
    {
        if ( getObj() != null )
        {
            return getObj().getLastCommunityUpdate();
        }

        return 0;
    }

    @Override
    public void setLastUpdate ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastCommunityUpdate ( ln );
        }

    }

    @Override
    public int getStatus()
    {
        if ( getObj() != null )
        {
            return getObj().getCommunityStatus();
        }

        return 0;
    }

    @Override
    public void setStatus ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setCommunityStatus ( ln );
        }

    }

    @Override
    public int getUpdatePriority()
    {
        if ( getObj() != null )
        {
            getObj().getCommunityUpdatePriority();
        }

        return 0;
    }

    @Override
    public void setUpdatePriority ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setCommunityUpdatePriority ( ln );
        }

    }

    @Override
    public int getUpdateCycle()
    {
        if ( getObj() != null )
        {
            getObj().getCommunityUpdateCycle();
        }

        return 0;
    }

    @Override
    public void setUpdateCycle ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setCommunityUpdateCycle ( ln );
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
