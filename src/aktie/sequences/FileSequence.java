package aktie.sequences;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.HH2Session;

public class FileSequence extends AbstractSequence<CommunityMember>
{

    public FileSequence ( HH2Session s )
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
            return getObj().getLastFileNumber();
        }

        return 0;
    }

    @Override
    public void setLastNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastFileNumber ( ln );
        }

    }

    @Override
    public long getNextClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNextClosestFileNumber();
        }

        return 0;
    }

    @Override
    public void setNextClosestNumber ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setNextClosestFileNumber ( ln );
        }

    }

    @Override
    public int getNumClosestNumber()
    {
        if ( getObj() != null )
        {
            return getObj().getNumClosestFileNumber();
        }

        return 0;
    }

    @Override
    public void setNumClosestNumber ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setNumClosestFileNumber ( ln );
        }

    }

    @Override
    public long getLastUpdate()
    {
        if ( getObj() != null )
        {
            return getObj().getLastFileUpdate();
        }

        return 0;
    }

    @Override
    public void setLastUpdate ( long ln )
    {
        if ( getObj() != null )
        {
            getObj().setLastFileUpdate ( ln );
        }

    }

    @Override
    public int getStatus()
    {
        if ( getObj() != null )
        {
            return getObj().getFileStatus();
        }

        return 0;
    }

    @Override
    public void setStatus ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setFileStatus ( ln );
        }

    }

    @Override
    public int getUpdatePriority()
    {
        if ( getObj() != null )
        {
            getObj().getFileUpdatePriority();
        }

        return 0;
    }

    @Override
    public void setUpdatePriority ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setFileUpdatePriority ( ln );
        }

    }

    @Override
    public int getUpdateCycle()
    {
        if ( getObj() != null )
        {
            getObj().getFileUpdateCycle();
        }

        return 0;
    }

    @Override
    public void setUpdateCycle ( int ln )
    {
        if ( getObj() != null )
        {
            getObj().setFileUpdateCycle ( ln );
        }

    }

    @Override
    public CommunityMember createNewObj ( CObj c )
    {
        if ( c != null )
        {
            String comid = c.getString ( CObj.COMMUNITYID );
            String creatorid = c.getString ( CObj.CREATOR );

            if ( comid != null && creatorid != null )
            {
                String id = Utils.mergeIds ( creatorid, comid );
                CommunityMember idat = new CommunityMember();
                idat.setId ( id );
                idat.setCommunityId ( comid );
                idat.setMemberId ( creatorid );
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
