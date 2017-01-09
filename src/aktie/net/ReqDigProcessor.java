package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.Index;

public class ReqDigProcessor extends GenericProcessor
{

    private ConnectionThread conThread;
    private Index index;

    public ReqDigProcessor ( Index i, ConnectionThread ct )
    {
        conThread = ct;
        index = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        if ( CObj.CON_REQ_DIG.equals ( b.getType() ) )
        {
            String d = b.getDig();
            //Get the object with that digest.
            CObj rid = conThread.getEndDestination();

            if ( d != null && rid != null )
            {
                CObj o = index.getByDig ( d );

                if ( o != null )
                {
                    if ( CObj.POST.equals ( o.getType() )  ||
                            CObj.HASFILE.equals ( o.getType() ) )
                    {
                        String comid = o.getString ( CObj.COMMUNITYID );

                        if ( comid != null )
                        {
                            if ( conThread.getSubs().contains ( comid ) )
                            {
                                conThread.enqueue ( o );
                            }

                        }

                    }

                    if ( CObj.SUBSCRIPTION.equals ( o.getType() ) )
                    {
                        String comid = o.getString ( CObj.COMMUNITYID );

                        if ( conThread.getMemberships().contains ( comid ) )
                        {
                            //Just send it if they're already members
                            conThread.enqueue ( o );
                        }

                        else
                        {
                            CObj com = index.getCommunity ( comid );

                            if ( com != null )
                            {
                                String pp = com.getString ( CObj.SCOPE );

                                if ( CObj.SCOPE_PUBLIC.equals ( pp ) )
                                {
                                    conThread.enqueue ( o );
                                }

                            }

                        }

                    }

                    if ( CObj.MEMBERSHIP.equals ( o.getType() ) ||
                            CObj.PRIVIDENTIFIER.equals ( o.getType() ) ||
                            CObj.PRIVMESSAGE.equals ( o.getType() ) ||
                            CObj.COMMUNITY.equals ( o.getType() ) ||
                            CObj.IDENTITY.equals ( o.getType() ) ||
                            CObj.SPAMEXCEPTION.equals ( o.getType() ) )
                    {
                        conThread.enqueue ( o );
                    }

                }

            }

            return true;
        }

        return false;
    }

}
