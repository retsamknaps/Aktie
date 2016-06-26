package aktie.sequences;

import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.PrivateMsgIdentity;

public class PrivMsgSequence extends AbstractSequence<PrivateMsgIdentity>
{

    public PrivMsgSequence ( HH2Session s )
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
            return getObj().getLastMsgNumber();
        }

        return 0;
    }

    @Override
    public void setLastNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastMsgNumber ( ln );
        }

    }

    @Override
    public long getNextClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNextClosestMsgNumber();
        }

        return 0;
    }

    @Override
    public void setNextClosestNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setNextClosestMsgNumber ( ln );
        }

    }

    @Override
    public int getNumClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNumClosestMsgNumber();
        }

        return 0;
    }

    @Override
    public void setNumClosestNumber ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setNumClosestMsgNumber ( ln );
        }

    }

    @Override
    public long getLastUpdate()
    {
        if ( getObj() != null )
        {
            return getObj().getLastMsgUpdate();
        }

        return 0;
    }

    @Override
    public void setLastUpdate ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastMsgUpdate ( ln );
        }

    }

    @Override
    public int getStatus()
    {
        if ( getObj() != null )
        {
            return getObj().getMsgStatus();
        }

        return 0;
    }

    @Override
    public void setStatus ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setMsgStatus ( ln );
        }

    }

    @Override
    public int getUpdatePriority()
    {
        if ( getObj() != null )
        {
            getObj().getMsgUpdatePriority();
        }

        return 0;
    }

    @Override
    public void setUpdatePriority ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setMsgUpdatePriority ( ln );
        }

    }

    @Override
    public int getUpdateCycle()
    {
        if ( getObj() != null )
        {
            getObj().getMsgUpdateCycle();
        }

        return 0;
    }

    @Override
    public void setUpdateCycle ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setMsgUpdateCycle ( ln );
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
