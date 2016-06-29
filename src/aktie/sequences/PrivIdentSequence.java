package aktie.sequences;

import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.PrivateMsgIdentity;

public class PrivIdentSequence extends AbstractSequence<PrivateMsgIdentity>
{

    public PrivIdentSequence ( HH2Session s )
    {
        super ( PrivateMsgIdentity.class, s );
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
            return getObj().getLastIdentNumber();
        }

        return 0;
    }

    @Override
    public void setLastNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastIdentNumber ( ln );
        }

    }

    @Override
    public long getNextClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNextClosestIdentNumber();
        }

        return 0;
    }

    @Override
    public void setNextClosestNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setNextClosestIdentNumber ( ln );
        }

    }

    @Override
    public int getNumClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNumClosestIdentNumber();
        }

        return 0;
    }

    @Override
    public void setNumClosestNumber ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setNumClosestIdentNumber ( ln );
        }

    }

    @Override
    public long getLastUpdate()
    {
        if ( getObj() != null )
        {
            return getObj().getLastIdentUpdate();
        }

        return 0;
    }

    @Override
    public void setLastUpdate ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastIdentUpdate ( ln );
        }

    }

    @Override
    public int getStatus()
    {
        if ( getObj() != null )
        {
            return getObj().getIdentStatus();
        }

        return 0;
    }

    @Override
    public void setStatus ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setIdentStatus ( ln );
        }

    }

    @Override
    public int getUpdatePriority()
    {
        if ( getObj() != null )
        {
            return getObj().getIdentUpdatePriority();
        }

        return 0;
    }

    @Override
    public void setUpdatePriority ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setIdentUpdatePriority ( ln );
        }

    }

    @Override
    public int getUpdateCycle()
    {
        if ( getObj() != null )
        {
            return getObj().getIdentUpdateCycle();
        }

        return 0;
    }

    @Override
    public void setUpdateCycle ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setIdentUpdateCycle ( ln );
        }

    }

    @Override
    public PrivateMsgIdentity createNewObj ( CObj c )
    {
        if ( c != null )
        {
            String creatorid = c.getString ( CObj.CREATOR );

            if ( creatorid != null )
            {
                PrivateMsgIdentity idat = new PrivateMsgIdentity();
                idat.setId ( creatorid );
                setObj ( idat );
                return idat;
            }

        }

        return null;
    }



    @Override
    public PrivateMsgIdentity createNewObj ( String... c )
    {
        if ( c != null )
        {
            if ( c.length == 1 )
            {
                PrivateMsgIdentity idat = new PrivateMsgIdentity();
                staticid = c[0];
                idat.setId ( staticid );
                setObj ( idat );
                return idat;
            }

        }

        return null;
    }

}
