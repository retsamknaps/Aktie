package aktie.sequences;

import java.util.Date;

import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;

public class MemberSequence extends AbstractSequence<IdentityData>
{

    public MemberSequence ( HH2Session s )
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
            return getObj().getLastMembershipNumber();
        }

        return 0;
    }

    @Override
    public void setLastNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastMembershipNumber ( ln );
        }

    }

    @Override
    public long getNextClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNextClosestMembershipNumber();
        }

        return 0;
    }

    @Override
    public void setNextClosestNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setNextClosestMembershipNumber ( ln );
        }

    }

    @Override
    public int getNumClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNumClosestMembershipNumber();
        }

        return 0;
    }

    @Override
    public void setNumClosestNumber ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setNumClosestMembershipNumber ( ln );
        }

    }

    @Override
    public long getLastUpdate()
    {
        if ( getObj() != null )
        {
            return getObj().getLastMemberUpdate();
        }

        return 0;
    }

    @Override
    public void setLastUpdate ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastMemberUpdate ( ln );
        }

    }

    @Override
    public int getStatus()
    {
        if ( getObj() != null )
        {
            return getObj().getMemberStatus();
        }

        return 0;
    }

    @Override
    public void setStatus ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setMemberStatus ( ln );
        }

    }

    @Override
    public int getUpdatePriority()
    {
        if ( getObj() != null )
        {
            getObj().getMemberUpdatePriority();
        }

        return 0;
    }

    @Override
    public void setUpdatePriority ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setMemberUpdatePriority ( ln );
        }

    }

    @Override
    public int getUpdateCycle()
    {
        if ( getObj() != null )
        {
            getObj().getMemberUpdateCycle();
        }

        return 0;
    }

    @Override
    public void setUpdateCycle ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setMemberUpdateCycle ( ln );
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
