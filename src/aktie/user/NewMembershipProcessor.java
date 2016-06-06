package aktie.user;

import java.util.List;

import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.hibernate.Query;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.utils.MembershipValidator;

public class NewMembershipProcessor extends GenericProcessor
{

    private GuiCallback guicallback;
    private Index index;
    private MembershipValidator validator;
    private HH2Session session;
    private SpamTool spamtool;

    public NewMembershipProcessor ( HH2Session s, Index i, SpamTool st, GuiCallback cb )
    {
        session = s;
        index = i;
        guicallback = cb;
        spamtool = st;
        validator = new MembershipValidator ( index );
    }

    /**
        must set:
        type: membership
        private: communityid, memberid, authority
        string: creator
    */
    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.MEMBERSHIP.equals ( type ) )
        {
            //Check to make sure YOU are the creator (you cannot grant membership
            //in the name of others.
            String creator = o.getString ( CObj.CREATOR );

            if ( creator == null )
            {
                o.pushString ( CObj.ERROR, "Creator must be defined" );
                guicallback.update ( o );
                return true;
            }

            CObj myid = index.getMyIdentity ( creator );

            if ( myid == null )
            {
                o.pushString ( CObj.ERROR, "You may only use your own identity" );
                guicallback.update ( o );
                return true;
            }

            //Make sure the communityid, memberid, and authority are all set
            String comid = o.getPrivate ( CObj.COMMUNITYID );
            String memid = o.getPrivate ( CObj.MEMBERID );
            Long auth = o.getPrivateNumber ( CObj.AUTHORITY );

            if ( comid == null || memid == null || auth == null )
            {
                o.pushString ( CObj.ERROR, "community, member, and authority must be set" );
                guicallback.update ( o );
                return true;
            }

            //Only grant membership if it's a private community
            CObj com = index.getCommunity ( comid );

            if ( com == null )
            {
                o.pushString ( CObj.ERROR, "community must exist" );
                guicallback.update ( o );
                return true;
            }

            String scope = com.getString ( CObj.SCOPE );

            if ( !CObj.SCOPE_PRIVATE.equals ( scope ) )
            {
                o.pushString ( CObj.ERROR, "only add members to private or locked communities" );
                guicallback.update ( o );
                return true;
            }

            //Make sure not exceeding authority
            if ( validator.canGrantMemebership ( comid, creator, auth ) == null )
            {
                o.pushString ( CObj.ERROR, "you cannot grant membership" );
                guicallback.update ( o );
                return true;
            }

            //get the identity of the person
            CObj member = index.getIdentity ( memid );

            if ( member == null )
            {
                o.pushString ( CObj.ERROR, "member identity not found" );
                guicallback.update ( o );
                return true;
            }

            //encrypt the new membership data set payload
            StringBuilder sb = new StringBuilder();
            sb.append ( CObj.COMMUNITYID );
            sb.append ( "=" );
            sb.append ( comid );
            sb.append ( "," );
            sb.append ( CObj.MEMBERID );
            sb.append ( "=" );
            sb.append ( memid );
            String rawstr = sb.toString();
            byte raw[] = Utils.stringToByteArray ( rawstr );
            //encrypt the community key with the identity public key
            byte symkey[] = Utils.toByteArray ( com.getPrivate ( CObj.KEY ) );
            KeyParameter kp = new KeyParameter ( symkey );
            byte enc[] = Utils.anonymousSymEncode ( kp, Utils.CID0,
                                                    Utils.CID1, raw );
            o.pushString ( CObj.PAYLOAD, Utils.toString ( enc ) );

            sb = new StringBuilder();
            sb.append ( CObj.AUTHORITY );
            sb.append ( "=" );
            sb.append ( Long.toString ( auth ) );
            rawstr = sb.toString();
            raw = Utils.stringToByteArray ( rawstr );
            //encrypt the community key with the identity public key
            enc = Utils.anonymousSymEncode ( kp, Utils.CID0,
                                             Utils.CID1, raw );
            o.pushString ( CObj.PAYLOAD2, Utils.toString ( enc ) );

            RSAKeyParameters mpk = Utils.publicKeyFromString ( member.getString ( CObj.KEY ) );
            byte enckey[] = Utils.anonymousAsymEncode ( mpk, Utils.CID0, Utils.CID1, symkey );
            o.pushString ( CObj.ENCKEY, Utils.toString ( enckey ) );
            //Set the sequence number
            Session s = null;

            try
            {
                s = session.getSession();
                s.getTransaction().begin();
                Query q = s.createQuery ( "SELECT x FROM IdentityData x WHERE x.id = :uid" );
                q.setParameter ( "uid", creator );
                @SuppressWarnings ( "unchecked" )
                List<IdentityData> ld = q.list();

                if ( ld.size() != 1 )
                {
                    s.getTransaction().commit();
                    s.close();
                    o.pushString ( CObj.ERROR, "You can only add community one of your identities" );
                    guicallback.update ( o );
                    return true;
                }

                else
                {
                    IdentityData idata = ld.get ( 0 );
                    long num = idata.getLastMembershipNumber();
                    num++;
                    o.pushNumber ( CObj.SEQNUM, num );
                    idata.setLastMembershipNumber ( num );
                    s.merge ( idata );
                }

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

            spamtool.finalize ( Utils.privateKeyFromString ( myid.getPrivate ( CObj.PRIVATEKEY ) ), o );

            o.pushPrivate ( CObj.DECODED, "true" );
            o.pushPrivate ( CObj.VALIDMEMBER, "true" );
            o.pushPrivate ( CObj.NAME, com.getPrivate ( CObj.NAME ) );
            o.pushPrivate ( CObj.DESCRIPTION, com.getPrivate ( CObj.DESCRIPTION ) );

            o.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
            o.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

            //Set the rank of the post based on the rank of the
            //user
            Long rnk = myid.getPrivateNumber ( CObj.PRV_USER_RANK );

            if ( rnk != null )
            {
                o.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
            }

            if ( "true".equals ( member.getPrivate ( CObj.MINE ) ) )
            {
                o.pushPrivate ( CObj.MINE, "true" );
                com.pushPrivate ( memid, "true" );

                try
                {
                    index.index ( com );
                    index.forceNewSearcher();
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            try
            {
                index.index ( o );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
                o.pushString ( CObj.ERROR, "Community could not be indexed" );
                guicallback.update ( o );
                return true;
            }

            guicallback.update ( o );
        }

        return false;
    }

}
