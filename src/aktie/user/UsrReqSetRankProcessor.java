package aktie.user;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class UsrReqSetRankProcessor extends GenericProcessor
{

    private Index index;

    public UsrReqSetRankProcessor ( Index i )
    {
        index = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.USR_SET_RANK.equals ( type ) )
        {
            String id = b.getString ( CObj.CREATOR );
            Long rnk = b.getNumber ( CObj.PRV_USER_RANK );

            if ( id != null && rnk != null )
            {
                CObj user = index.getIdentity ( id );

                if ( user != null )
                {
                    user.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );

                    try
                    {
                        index.index ( user );
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                        //TODO: Don't be lazy and push error
                    }

                }

                CObjList cl = index.getCreatedBy ( id );

                for ( int c = 0; c < cl.size(); c++ )
                {
                    try
                    {
                        CObj co = cl.get ( c );
                        Long or = co.getPrivateNumber ( CObj.PRV_USER_RANK );

                        if ( !rnk.equals ( or ) )
                        {
                            co.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
                            index.index ( co );
                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                        //TODO: Don't be lazy and push error
                    }

                }

                cl.close();
            }

            return true;
        }

        return false;
    }

}
