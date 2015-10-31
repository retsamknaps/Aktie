package aktie.net;

import java.io.IOException;

import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.utils.DigestValidator;
import aktie.utils.SubscriptionValidator;

public class InPostProcessor extends GenericProcessor
{

    private GuiCallback guicallback;
    private Index index;
    private HH2Session session;
    private DigestValidator validator;
    private SubscriptionValidator subvalidator;
    private CObj destIdent;

    public InPostProcessor ( CObj id, HH2Session s, Index i, GuiCallback cb )
    {
        destIdent = id;
        index = i;
        session = s;
        guicallback = cb;
        validator = new DigestValidator ( index );
        subvalidator = new SubscriptionValidator ( index );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.POST.equals ( type ) )
        {
            if ( validator.newAndValid ( b ) )
            {
                //Make sure this identity
                String comid = b.getString ( CObj.COMMUNITYID );
                String creatorid = b.getString ( CObj.CREATOR );
                Long seqnum = b.getNumber ( CObj.SEQNUM );
                CObj mysubid = subvalidator.isMyUserSubscribed ( comid, destIdent.getId() );

                if ( comid != null && creatorid != null && mysubid != null && seqnum != null )
                {
                    String id = Utils.mergeIds ( creatorid, comid );
                    CObj usub = subvalidator.isUserSubscribed ( comid, creatorid );

                    if ( usub != null )
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

                            if ( m.getLastPostNumber() + 1 == ( long ) seqnum )
                            {
                                m.setLastPostNumber ( seqnum );
                                m.setNextClosestPostNumber ( seqnum );
                                m.setNumClosestPostNumber ( 1 );
                                s.merge ( m );
                            }

                            else
                            {
                                /*
                                    if there is a permanent gap in a sequence number
                                    count how many times we see the next number, so
                                    if we see it too many times we just use it for last
                                    number instead
                                */
                                if ( seqnum > m.getLastPostNumber() )
                                {
                                    if ( m.getNextClosestPostNumber() > seqnum ||
                                            m.getNextClosestPostNumber() <= m.getLastPostNumber() )
                                    {
                                        m.setNextClosestPostNumber ( seqnum );
                                        m.setNumClosestPostNumber ( 1 );
                                        s.merge ( m );
                                    }

                                    else if ( m.getNextClosestPostNumber() == seqnum )
                                    {
                                        m.setNumClosestPostNumber (
                                            m.getNumClosestPostNumber() + 1 );
                                        s.merge ( m );
                                    }

                                }

                            }

                            s.getTransaction().commit();
                            s.close();
                            b.pushPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS, 1L );
                            index.index ( b );
                            guicallback.update ( b );
                        }

                        catch ( IOException e )
                        {
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

            return true;
        }

        return false;
    }

}
