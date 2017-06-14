package aktie.net;

import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class ReqDigProcessor extends GenericProcessor
{
    Logger log = Logger.getLogger ( "aktie" );

    private ConnectionThread conThread;
    private Index index;

    public ReqDigProcessor ( Index i, ConnectionThread ct )
    {
        conThread = ct;
        index = i;
    }

    private void log ( String msg )
    {
        if ( conThread.getEndDestination() != null )
        {
            log.info ( "ME: " + conThread.getLocalDestination().getIdentity().getId() +
                       " FROM: " + conThread.getEndDestination().getId() + " " + msg );
        }

    }

    @Override
    public boolean process ( CObj b )
    {
        if ( CObj.CON_REQ_DIG.equals ( b.getType() ) )
        {
            String d = b.getDig();
            //Get the object with that digest.
            CObj rid = conThread.getEndDestination();
            CObj mid = null;

            if ( conThread.getLocalDestination() != null )
            {
                mid = conThread.getLocalDestination().getIdentity();
            }

            if ( d != null && rid != null && mid != null )
            {
                CObj o = index.getByDig ( d );
                log ( "GET DIG: " + d + " obj: " + o );

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
                                log ( "SND PST/HAS: " + d );
                                conThread.enqueue ( o );
                            }

                        }

                    }

                    if ( CObj.SUBSCRIPTION.equals ( o.getType() ) )
                    {
                        String comid = o.getString ( CObj.COMMUNITYID );

                        if ( comid != null )
                        {
                            CObj com = index.getCommunity ( comid );

                            if ( com != null )
                            {

                                String pp = com.getString ( CObj.SCOPE );

                                if ( CObj.SCOPE_PUBLIC.equals ( pp ) )
                                {
                                    log ( "SND PUB SUB: " + d );
                                    conThread.enqueue ( o );
                                }

                                else
                                {
                                    CObjList mlst = index.getMembership ( comid, rid.getId() );
                                    boolean ismem = mlst.size() > 0;
                                    mlst.close();

                                    if ( !ismem )
                                    {
                                        ismem = rid.getId().equals ( com.getString ( CObj.CREATOR ) );
                                    }

                                    if ( ismem )
                                    {
                                        mlst = index.getMembership ( comid, mid.getId() );

                                        if ( mlst.size() == 0 )
                                        {
                                            ismem = mid.getId().equals ( com.getString ( CObj.CREATOR ) );
                                        }

                                        mlst.close();
                                    }

                                    if ( ismem )
                                    {
                                        //Just send it if they're already members
                                        log ( "SND PRV SUB: " + d );
                                        conThread.enqueue ( o );
                                    }

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
                        log ( "SND PBLC: " + d );
                        conThread.enqueue ( o );
                    }

                }

            }

            return true;
        }

        return false;
    }

}
