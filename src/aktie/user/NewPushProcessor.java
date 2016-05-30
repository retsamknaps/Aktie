package aktie.user;

import java.util.LinkedList;
import java.util.List;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class NewPushProcessor extends GenericProcessor
{

    private Index index;
    private PushInterface push;

    public NewPushProcessor ( Index i, PushInterface p )
    {
        index = i;
        push = p;
    }

    @Override
    public boolean process ( CObj b )
    {
        String err = b.getString ( CObj.ERROR );
        String creator = b.getString ( CObj.CREATOR );
        String type = b.getType();

        if ( err == null && creator != null )
        {
            CObj myid = index.getMyIdentity ( creator );

            if ( myid != null )
            {
                if ( CObj.IDENTITY.equals ( type ) ||
                        CObj.MEMBERSHIP.equals ( type ) ||
                        CObj.COMMUNITY.equals ( type ) ||
                        CObj.PRIVIDENTIFIER.equals ( type ) ||
                        CObj.PRIVMESSAGE.equals ( type )
                   )
                {
                    push.push ( myid, b );
                }

                if ( CObj.SUBSCRIPTION.equals ( type ) )
                {
                    String comid = b.getString ( CObj.COMMUNITYID );
                    CObj com = index.getCommunity ( comid );
                    List<String> conids = push.getConnectedIds ( myid );

                    if ( com != null && conids != null )
                    {
                        List<String> r = null;

                        if ( CObj.SCOPE_PRIVATE.equals ( com.getString ( CObj.SCOPE ) ) )
                        {
                            r = new LinkedList<String>();
                            String cid = com.getString ( CObj.CREATOR );

                            if ( cid != null )
                            {
                                r.add ( cid );
                            }

                            CObjList mlst = index.getMemberships ( comid, null );

                            for ( int ct = 0; ct < mlst.size(); ct++ )
                            {
                                try
                                {
                                    String mid = mlst.get ( ct ).getPrivate ( CObj.MEMBERID );

                                    if ( mid != null )
                                    {
                                        r.add ( mid );
                                    }

                                }

                                catch ( Exception e )
                                {
                                    e.printStackTrace();
                                }

                            }

                            mlst.close();
                        }

                        for ( String cid : conids )
                        {
                            if ( r == null )
                            {
                                push.push ( myid, cid, b );
                            }

                            else if ( r.contains ( conids ) )
                            {
                                push.push ( myid, cid, b );
                            }

                        }

                    }

                }

                if ( CObj.HASFILE.equals ( type ) || CObj.POST.equals ( type ) )
                {
                    String comid = b.getString ( CObj.COMMUNITYID );
                    List<String> conids = push.getConnectedIds ( myid );

                    for ( String cid : conids )
                    {
                        CObj sub = index.getSubscription ( comid, cid );

                        if ( sub != null )
                        {
                            push.push ( myid, cid, b );
                        }

                    }

                }

            }

        }

        return false;
    }

}
