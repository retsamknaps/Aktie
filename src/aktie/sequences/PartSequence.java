package aktie.sequences;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.HH2Session;

public class PartSequence extends AbstractSequence<CommunityMember>
{

    public PartSequence ( HH2Session s )
    {
        super ( CommunityMember.class, s );
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
            return getObj().getLastPartNumber();
        }

        return 0;
    }

    @Override
    public void setLastNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastPartNumber ( ln );
        }

    }

    @Override
    public long getNextClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNextClosestPartNumber();
        }

        return 0;
    }

    @Override
    public void setNextClosestNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setNextClosestPartNumber ( ln );
        }

    }

    @Override
    public int getNumClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNumClosestPartNumber();
        }

        return 0;
    }

    @Override
    public void setNumClosestNumber ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setNumClosestPartNumber ( ln );
        }

    }

    @Override
    public long getLastUpdate()
    {
        if ( getObj() != null )
        {
            return getObj().getLastPartUpdate();
        }

        return 0;
    }

    @Override
    public void setLastUpdate ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastPartUpdate ( ln );
        }

    }

    @Override
    public int getStatus()
    {
        if ( getObj() != null )
        {
            return getObj().getPartStatus();
        }

        return 0;
    }

    @Override
    public void setStatus ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setPartStatus ( ln );
        }

    }

    @Override
    public int getUpdatePriority()
    {
        if ( getObj() != null )
        {
            return getObj().getPartUpdatePriority();
        }

        return 0;
    }

    @Override
    public void setUpdatePriority ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setPartUpdatePriority ( ln );
        }

    }

    @Override
    public int getUpdateCycle()
    {
        if ( getObj() != null )
        {
            return getObj().getPartUpdateCycle();
        }

        return 0;
    }

    @Override
    public void setUpdateCycle ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setPartUpdateCycle ( ln );
        }

    }

    @Override
    public CommunityMember createNewObj ( CObj c )
    {
        if ( c != null )
        {
            String creatorID = c.getString ( CObj.CREATOR );
            String communityID = c.getString ( CObj.COMMUNITYID );

            if ( creatorID != null && communityID != null )
            {
                String id = Utils.mergeIds ( creatorID, communityID );
                CommunityMember idat = new CommunityMember();
                idat.setCommunityId ( communityID );
                idat.setId ( id );
                idat.setMemberId ( creatorID );
                setObj ( idat );
                return idat;
            }

        }

        return null;
    }



    @Override
    public CommunityMember createNewObj ( String... c )
    {
        if ( c != null )
        {
            if ( c.length == 3 )
            {
                CommunityMember idat = new CommunityMember();
                staticid = c[0];
                idat.setId ( staticid );
                idat.setCommunityId ( c[1] );
                idat.setMemberId ( c[2] );
                setObj ( idat );
                return idat;
            }

        }

        return null;
    }


}
