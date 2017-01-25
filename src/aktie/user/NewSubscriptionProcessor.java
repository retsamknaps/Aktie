package aktie.user;

import java.util.logging.Logger;

import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.gui.GuiCallback;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.net.ConnectionManager2;
import aktie.spam.SpamTool;
import aktie.utils.SubscriptionValidator;

public class NewSubscriptionProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private GuiCallback guicallback;
    private Index index;
    private HH2Session session;
    private SubscriptionValidator validator;
    private SpamTool spamtool;
    private IdentityManager identManager;
    private ConnectionManager2 conMan;

    public NewSubscriptionProcessor ( HH2Session s, ConnectionManager2 cm, Index i, SpamTool st, GuiCallback cb )
    {
        session = s;
        conMan = cm;
        index = i;
        guicallback = cb;
        spamtool = st;
        identManager = new IdentityManager ( s, i );
        validator = new SubscriptionValidator ( index );
    }

    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.SUBSCRIPTION.equals ( type ) )
        {
            //Get the creator and make sure it is this user
            String creator = o.getString ( CObj.CREATOR );

            if ( creator == null )
            {
                o.pushString ( CObj.ERROR, "creator must be defined" );
                guicallback.update ( o );
                return true;
            }

            CObj myid = index.getMyIdentity ( creator );

            if ( myid == null )
            {
                o.pushString ( CObj.ERROR, "you may only use your own identity" );
                guicallback.update ( o );
                return true;
            }

            //See if we can subscribe
            String comid = o.getString ( CObj.COMMUNITYID );

            if ( comid == null )
            {
                o.pushString ( CObj.ERROR, "community must be defined" );
                guicallback.update ( o );
                return true;
            }

            if ( !validator.canSubscribe ( comid, creator ) )
            {
                o.pushString ( CObj.ERROR, "may not subscribe" );
                guicallback.update ( o );
                return true;
            }

            CObj updatemsg = new CObj();
            updatemsg.pushString ( CObj.ERROR, "Creating new subscription. " );
            updatemsg.pushPrivate ( CObj.PRV_CLEAR_ERR, "false" );
            guicallback.update ( updatemsg );

            //Create the id value.
            String id = Utils.mergeIds ( creator, comid );
            o.setId ( id );
            //get the sequence number
            Session s = null;

            try
            {
                s = session.getSession();
                s.getTransaction().begin();
                IdentityData m = ( IdentityData ) s.get ( IdentityData.class, creator );

                if ( m == null )
                {
                    throw new Exception ( "Identity missing" );
                }

                long num = m.getLastSubNumber();
                num++;
                o.pushNumber ( CObj.SEQNUM, num );
                m.setLastSubNumber ( num );
                s.merge ( m );
                s.getTransaction().commit();
                s.close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
                o.pushString ( CObj.ERROR, "Bad error: " + e.getMessage() );
                guicallback.update ( o );

                if ( s != null )
                {
                    try
                    {
                        if ( s.getTransaction().isActive() )
                        {
                            s.getTransaction().rollback();
                        }

                    }

                    catch ( Exception e2 )
                    {
                    }

                    try
                    {
                        s.close();
                    }

                    catch ( Exception e2 )
                    {
                    }

                }

                return true;
            }

            //Sign it.
            o.pushPrivate ( CObj.MINE, "true" );
            o.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
            o.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

            spamtool.finalize ( Utils.privateKeyFromString ( myid.getPrivate ( CObj.PRIVATEKEY ) ),
                                o );

            //Set the rank of the post based on the rank of the
            //user
            Long rnk = myid.getPrivateNumber ( CObj.PRV_USER_RANK );

            if ( rnk != null )
            {
                o.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
            }

            long gseq = identManager.getGlobalSequenceNumber ( myid.getId(), true );
            o.pushPrivateNumber ( CObj.getGlobalSeq ( myid.getId() ), gseq );

            try
            {
                //Set the ID, so we always overwrite the last subscription
                log.info ( "SUBSCRIPTION DIG: " + o.getDig() + "CREATOR: " + creator + " SEQ: " + gseq );
                index.index ( o );
                index.forceNewSearcher();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
                o.pushString ( CObj.ERROR, "subscription could not be indexed" );
                guicallback.update ( o );
                return true;
            }

            //Force update of connections
            if ( conMan != null )
            {
                conMan.resetAllConnections();
            }

            if ( "true".equals ( o.getString ( CObj.SUBSCRIBED ) ) )
            {
                CObjList clst = index.getSubscriptions ( comid, null );

                for ( int c = 0; c < clst.size(); c++ )
                {
                    try
                    {
                        CObj sb = clst.get ( c );
                        String sc = sb.getString ( CObj.CREATOR );

                        if ( sc != null )
                        {
                            identManager.updateGlobalSequenceNumber ( sc,
                                    false, 0,
                                    false, 0,
                                    true, 0 );
                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

                clst.close();
            }

            guicallback.update ( o );
        }

        return false;
    }

}
