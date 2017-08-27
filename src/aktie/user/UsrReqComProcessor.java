package aktie.user;

import java.io.IOException;

import aktie.GenericNoContextProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class UsrReqComProcessor extends GenericNoContextProcessor
{

    private IdentityManager identManager;
    private Index index;

    public UsrReqComProcessor ( IdentityManager i, Index idx )
    {
        identManager = i;
        index = idx;
    }

    private void resetCommunity ( String comid )
    {
        CObjList clst = index.getSubscriptions ( comid, null );

        for ( int c = 0; c < clst.size(); c++ )
        {
            try
            {
                CObj sb = clst.get ( c );

                String mine = sb.getPrivate ( CObj.MINE );
                String sc = sb.getString ( CObj.CREATOR );

                if ( sc != null && ( !"true".equals ( mine ) ) )
                {
                    identManager.updateIdentityCommunitySeqNumber ( sc, comid, 0, true );
                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        clst.close();

        CObj community = index.getCommunity ( comid );

        if ( community != null )
        {
            if ( CObj.SCOPE_PRIVATE.equals ( community.getString ( CObj.SCOPE ) ) )
            {
                String creator = community.getString ( CObj.CREATOR );
                identManager.updateGlobalSequenceNumber ( creator, false, 0, true, 0, true, 0 );
                clst = index.getMemberships ( comid, null );

                for ( int c = 0; c < clst.size(); c++ )
                {
                    try
                    {
                        CObj m = clst.get ( c );
                        String memid = m.getPrivate ( CObj.MEMBERID );

                        if ( memid != null )
                        {
                            identManager.updateGlobalSequenceNumber ( memid, false, 0,
                                    true, 0, true, 0 );
                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

                clst.close();


            }

        }

    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_COMMUNITY_UPDATE.equals ( type ) )
        {
            String comid = b.getString ( CObj.COMMUNITYID );

            if ( comid != null )
            {
                resetCommunity ( comid );
            }

            else
            {
                CObjList clst = index.getMySubscriptions();

                for ( int c = 0; c < clst.size(); c++ )
                {
                    try
                    {
                        CObj sb = clst.get ( c );
                        comid = sb.getString ( CObj.COMMUNITYID );
                        resetCommunity ( comid );
                    }

                    catch ( IOException e )
                    {
                        e.printStackTrace();
                    }

                }

                clst.close();
            }

            return true;
        }

        return false;
    }

}
