package aktie.net;

import java.io.IOException;

import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.HH2Session;
import aktie.index.Index;
import aktie.utils.DigestValidator;
import aktie.utils.SubscriptionValidator;

public class InSubProcessor extends GenericProcessor
{

    private HH2Session session;
    private Index index;
    private DigestValidator validator;
    private SubscriptionValidator subvalidator;
    private ConnectionThread conThread;

    public InSubProcessor ( HH2Session s, Index i, ConnectionThread ct )
    {
        session = s;
        conThread = ct;
        index = i;
        validator = new DigestValidator ( index );
        subvalidator = new SubscriptionValidator ( index );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.SUBSCRIPTION.equals ( type ) )
        {
            //Check if it's valid and new
            if ( validator.newAndValid ( b ) )
            {
                Long seqnum = b.getNumber ( CObj.SEQNUM );
                String creatorid = b.getString ( CObj.CREATOR );
                String comid = b.getString ( CObj.COMMUNITYID );

                if ( seqnum != null && creatorid != null && comid != null )
                {
                    String id = Utils.mergeIds ( comid, creatorid );

                    //The merged id should already be set, so check to make sure,
                    //if not, then we can't just change it because the signature would
                    //be bad.
                    if ( id.equals ( b.getId() ) )
                    {
                        boolean update = false;
                        //We already made this getSubscription method before we
                        //decided to do the merged id thing, just go with it.
                        CObj co = index.getSubscription ( comid, creatorid );

                        //Note, if we already have a subscription object, then the
                        //identity must be ok to subscribe to the community and
                        //we can just check the new sequence number is greater
                        if ( co != null )
                        {
                            //If the sequence number is less than we have just
                            //discard.
                            if ( seqnum > co.getNumber ( CObj.SEQNUM ) )
                            {
                                update = true;
                            }

                        }

                        else
                        {
                            //We need to see if this user can subscribe.
                            if ( subvalidator.canSubscribe ( comid, creatorid ) )
                            {
                                update = true;
                            }

                        }

                        if ( update )
                        {
                            Session s = null;

                            try
                            {
                                s = session.getSession();
                                s.getTransaction().begin();
                                CommunityMember m = ( CommunityMember )
                                                    s.get ( CommunityMember.class, id );

                                if ( m == null )
                                {
                                    m = new CommunityMember();
                                    m.setId ( id );
                                    m.setCommunityId ( comid );
                                    m.setMemberId ( creatorid );
                                    s.persist ( m );
                                }

                                m.setLastSubscriptionNumber ( seqnum );
                                s.merge ( m );
                                s.getTransaction().commit();
                                s.close();
                                index.index ( b );
                                conThread.update ( b );
                            }

                            catch ( IOException e )
                            {
                                e.printStackTrace();

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

                            }

                        }

                    }

                }

            }

            return true;
        }

        return false;
    }

}
