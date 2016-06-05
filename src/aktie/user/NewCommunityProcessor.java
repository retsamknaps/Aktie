package aktie.user;

import java.util.List;

import org.bouncycastle.crypto.params.KeyParameter;
import org.hibernate.Query;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMyMember;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.gui.GuiCallback;
import aktie.gui.Wrapper;
import aktie.index.Index;

public class NewCommunityProcessor extends GenericProcessor
{

    private GuiCallback guicallback;
    private Index index;
    private HH2Session session;

    public NewCommunityProcessor ( HH2Session s, Index i, GuiCallback cb )
    {
        session = s;
        index = i;
        guicallback = cb;
    }

    /**
        must set:
        type: community
        privatedata: name, description
        string: scope, creator

    */
    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.COMMUNITY.equals ( type ) )
        {
            String creator = o.getString ( CObj.CREATOR );

            if ( creator == null )
            {
                o.pushString ( CObj.ERROR, "Please select an identity for community" );
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

            o.pushString ( CObj.CREATOR_NAME, myid.getDisplayName() );

            String scope = o.getString ( CObj.SCOPE );

            if ( scope == null )
            {
                o.pushString ( CObj.ERROR, "Scope must be defined for a new community" );
                guicallback.update ( o );
                return true;
            }

            StringBuilder publicstring = new StringBuilder();
            String title = o.getPrivate ( CObj.NAME );
            String description = o.getPrivate ( CObj.DESCRIPTION );

            if ( title == null || description == null )
            {
                o.pushString ( CObj.ERROR, "Community must have name and description" );
                guicallback.update ( o );
                return true;
            }

            if ( title.contains ( "=" ) || title.contains ( "," ) ||
                    description.contains ( "=" ) || description.contains ( "," ) )
            {
                o.pushString ( CObj.ERROR, "Community name and description must not have , or =" );
                guicallback.update ( o );
                return true;
            }

            publicstring.append ( CObj.NAME );
            publicstring.append ( "=" );
            publicstring.append ( title );
            publicstring.append ( "," );
            publicstring.append ( CObj.DESCRIPTION );
            publicstring.append ( "=" );
            publicstring.append ( description );

            String comdata = publicstring.toString();

            KeyParameter kp = null;

            if ( CObj.SCOPE_PRIVATE.equals ( scope ) )
            {
                kp = Utils.generateKey();
                o.pushPrivate ( CObj.KEY, Utils.toString ( kp.getKey() ) );
                byte combytes[] = Utils.stringToByteArray ( comdata );
                byte encbytes[] = Utils.anonymousSymEncode ( kp, Utils.CID0, Utils.CID1, combytes );
                o.pushString ( CObj.PAYLOAD, Utils.toString ( encbytes ) );
                o.pushPrivate ( creator, "true" );
            }

            else if ( CObj.SCOPE_PUBLIC.equals ( scope ) )
            {
                o.pushString ( CObj.PAYLOAD, comdata );
            }

            else
            {
                o.pushString ( CObj.ERROR, "Scope must be public or private" );
                guicallback.update ( o );
                return true;
            }

            //Find the last sequence number to set.
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
                    o.pushString ( CObj.ERROR, "You must use one of your identities" );
                    guicallback.update ( o );
                    return true;
                }

                IdentityData idata = ld.get ( 0 );
                long num = idata.getLastCommunityNumber();
                num++;
                o.pushNumber ( CObj.SEQNUM, num );
                idata.setLastCommunityNumber ( num );
                s.merge ( idata );

                o.pushPrivate ( CObj.MINE, "true" );
                o.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
                o.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

                o.sign ( Utils.privateKeyFromString ( myid.getPrivate ( CObj.PRIVATEKEY ) ),
                         Wrapper.getGenPayment() );

                if ( CObj.SCOPE_PRIVATE.equals ( o.getString ( CObj.SCOPE ) ) )
                {
                    CommunityMyMember mm = new CommunityMyMember();
                    mm.setId ( o.getDig() );
                    mm.setCommunityId ( o.getDig() );
                    mm.setMemberId ( creator );
                    mm.setKey ( kp.getKey() );
                    s.merge ( mm );
                }

                s.getTransaction().commit();
                s.close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
                o.pushString ( CObj.ERROR, "Bad error: " + e.getMessage() );
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
                        e2.printStackTrace();
                    }

                    try
                    {
                        s.close();
                    }

                    catch ( Exception e2 )
                    {
                        e2.printStackTrace();
                    }

                }

                return true;
            }

            //Set the rank of the post based on the rank of the
            //user
            Long rnk = myid.getPrivateNumber ( CObj.PRV_USER_RANK );

            if ( rnk != null )
            {
                o.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
            }

            try
            {
                index.index ( o );
                index.forceNewSearcher();
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
