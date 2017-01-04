package aktie.user;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.utils.SubscriptionValidator;

public class NewPostProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private GuiCallback guicallback;
    private Index index;
    private HH2Session session;
    private SubscriptionValidator validator;
    private SpamTool spamtool;
    private IdentityManager identManager;

    public NewPostProcessor ( HH2Session s, Index i, SpamTool st, GuiCallback cb )
    {
        session = s;
        index = i;
        guicallback = cb;
        spamtool = st;
        identManager = new IdentityManager ( s, i );
        validator = new SubscriptionValidator ( index );
    }

    /**
        must set:
        type: post
        string: creator, community

    */
    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.POST.equals ( type ) )
        {
            //Get the creator and make sure it is this user
            String creator = o.getString ( CObj.CREATOR );
            String comid = o.getString ( CObj.COMMUNITYID );
            String id = Utils.mergeIds ( creator, comid );
            CObj myid = validator.isMyUserSubscribed ( comid, creator );

            if ( myid == null )
            {
                o.pushString ( CObj.ERROR, "you must be subscribed to post" );
                guicallback.update ( o );
                return true;
            }

            //Set the sequence number
            Session s = null;
            long lastpostnum = 0;

            try
            {
                s = session.getSession();
                s.getTransaction().begin();
                CommunityMember m = ( CommunityMember ) s.get ( CommunityMember.class, id );

                if ( m == null )
                {
                    m = new CommunityMember();
                    m.setId ( id );
                    m.setCommunityId ( comid );
                    m.setMemberId ( creator );
                    s.persist ( m );
                }

                lastpostnum = m.getLastPostNumber();
                lastpostnum++;
                o.pushNumber ( CObj.SEQNUM, lastpostnum );
                m.setLastPostNumber ( lastpostnum );
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

            //Find the last post lastpostnum-1 and make sure the CREATEDON time for
            //this one is after that one.
            CObjList opl = index.getPosts ( comid, creator, lastpostnum - 1, lastpostnum - 1 );

            if ( opl.size() > 0 )
            {
                try
                {
                    CObj oldp = opl.get ( 0 );

                    if ( oldp != null )
                    {
                        Long oldt = oldp.getNumber ( CObj.CREATEDON );

                        if ( oldt != null )
                        {
                            Long nco = o.getNumber ( CObj.CREATEDON );

                            if ( nco != null && nco < oldt )
                            {
                                o.pushNumber ( CObj.CREATEDON, oldt + 1 );
                            }

                        }

                    }

                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            opl.close();

            CObj updatemsg = new CObj();
            updatemsg.pushString ( CObj.ERROR, "Creating new post. " );
            updatemsg.pushPrivate ( CObj.PRV_CLEAR_ERR, "false" );
            guicallback.update ( updatemsg );


            o.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
            o.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

            //Sign it.
            spamtool.finalize ( Utils.privateKeyFromString ( myid.getPrivate ( CObj.PRIVATEKEY ) ), o );

            log.info ( "NEW POST: " + o.getDig() );

            //Set the rank of the post based on the rank of the
            //user
            CObj idty = index.getIdentity ( creator );

            Long rnk = null;

            if ( idty != null )
            {
                rnk = idty.getPrivateNumber ( CObj.PRV_USER_RANK );

                if ( rnk != null )
                {
                    o.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
                }

            }

            long gseq = identManager.getGlobalSequenceNumber ( myid.getId() );
            o.pushPrivateNumber ( CObj.getGlobalSeq ( myid.getId() ), gseq );

            //List any new fields that were added by the post
            //save them.
            List<CObj> fldlist = o.listNewFields();

            try
            {
                index.index ( o );

                for ( CObj fld : fldlist )
                {
                    CObj ft = index.getByDig ( fld.getDig() );

                    if ( ft != null )
                    {
                        String deflt = ft.getPrivate ( CObj.PRV_DEF_FIELD );

                        if ( deflt != null )
                        {
                            fld.pushPrivate ( CObj.PRV_DEF_FIELD, deflt );
                        }

                    }

                    index.index ( fld );
                }

                index.forceNewSearcher();

            }

            catch ( Exception e )
            {
                e.printStackTrace();
                o.pushString ( CObj.ERROR, "Post could not be indexed" );
                guicallback.update ( o );
                return true;
            }

            guicallback.update ( o );
        }

        return false;
    }

}
