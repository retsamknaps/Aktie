package aktie.user;

import aktie.GenericNoContextProcessor;
import aktie.UpdateCallback;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.utils.HasFileCreator;

public class UsrReqSetRankProcessor extends GenericNoContextProcessor
{

    private Index index;
    private HasFileCreator hfc;
    private UpdateCallback guicallback;

    public UsrReqSetRankProcessor ( Index i, HasFileCreator c, UpdateCallback cb )
    {
        index = i;
        hfc = c;
        guicallback = cb;
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

                rnk = Math.max ( 0L, rnk );
                CObj user = index.getIdentity ( id );

                if ( user != null )
                {
                    CObj updatemsg = new CObj();
                    updatemsg.pushString ( CObj.ERROR, "Setting user rank for: " + user.getDisplayName() );
                    updatemsg.pushPrivate ( CObj.PRV_CLEAR_ERR, "false" );
                    guicallback.update ( updatemsg );

                    user.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );

                    try
                    {
                        index.index ( user );
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

                CObjList cl = index.getCreatedBy ( id );

                for ( int c = 0; c < cl.size(); c++ )
                {
                    try
                    {
                        CObj co = cl.get ( c );
                        Long or = co.getPrivateNumber ( CObj.PRV_USER_RANK );

                        if ( or == null ) { or = 0L; }

                        if ( !rnk.equals ( or ) )
                        {
                            co.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
                            index.index ( co );

                            /*
                                If rank is zero reduce the hasfile count for the
                                file.
                            */
                            if ( ( or == 0 || rnk == 0 ) &&
                                    CObj.HASFILE.equals ( co.getType() ) )
                            {
                                hfc.updateFileInfo ( co );
                            }

                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                        //TODO: Don't be lazy and push error
                    }

                }

                cl.close();

                CObj updatemsg = new CObj();
                updatemsg.pushString ( CObj.ERROR, "" );
                guicallback.update ( updatemsg );
            }

            return true;
        }

        return false;
    }

}
