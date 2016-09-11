package aktie.net;

import java.util.Set;
import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class ReqSubProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private ConnectionThread connection;

    public ReqSubProcessor ( Index i, ConnectionThread c )
    {
        index = i;
        connection = c;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.CON_REQ_SUBS.equals ( type ) )
        {
            String creatorid = b.getString ( CObj.CREATOR );
            Long first = b.getNumber ( CObj.FIRSTNUM );
            Long last = b.getNumber ( CObj.LASTNUM );
            Set<String> memberships = connection.getMemberships();

            if ( creatorid != null && first != null && last != null && memberships != null )
            {

                CObjList cr = new CObjList();
                CObjList cl = index.getSubscriptions ( creatorid, first, last );

                for ( int c = 0; c < cl.size(); c++ )
                {
                    try
                    {
                        CObj sb = cl.get ( c );
                        String comid = sb.getString ( CObj.COMMUNITYID );

                        if ( comid != null )
                        {
                            if ( memberships.contains ( comid ) )
                            {
                            	log.info("Private subscription found. " + comid);
                                cr.add ( sb );
                            }

                            else
                            {
                                CObj com = index.getCommunity ( comid );

                               	log.info("Check if public com. " + com + " " + comid);
                                if ( com != null )
                                {
                                    if ( CObj.SCOPE_PUBLIC.equals ( com.getString ( CObj.SCOPE ) ) )
                                    {
                                    	log.info("Public community found. " + comid);
                                        cr.add ( sb );
                                    }
                                    else {
                                    	log.info("Community was not public and I am not member.");
                                    }

                                }

                            }

                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

                cl.close();

                log.info("Sending subscriptions: " + cr.size());
                if ( cr.size() > 0 )
                {
                    connection.enqueue ( cr );
                }

            }

            return true;
        }

        return false;
    }

}
