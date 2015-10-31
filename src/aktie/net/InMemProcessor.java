package aktie.net;

import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMyMember;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.gui.GuiCallback;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.utils.DigestValidator;
import aktie.utils.SymDecoder;

public class InMemProcessor extends GenericProcessor
{

    private GuiCallback guicallback;
    private Index index;
    private HH2Session session;
    private DigestValidator validator;
    private SymDecoder decoder;

    public InMemProcessor ( HH2Session s, Index i, GuiCallback cb )
    {
        index = i;
        session = s;
        guicallback = cb;
        validator = new DigestValidator ( index );
        decoder = new SymDecoder();
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.MEMBERSHIP.equals ( type ) )
        {
            //Check with enckey only!  Decode with sym key later.
            if ( validator.newAndValid ( b ) )
            {
                Long seqnum = b.getNumber ( CObj.SEQNUM );
                String creatorid = b.getString ( CObj.CREATOR );

                if ( seqnum != null && creatorid != null )
                {
                    //See if this is a membership for one of this node's identities
                    b.pushPrivate ( CObj.DECODED, "false" );
                    b.pushPrivate ( CObj.VALIDMEMBER, "false" );
                    byte dec[] = null;
                    String enckey = b.getString ( CObj.ENCKEY );

                    if ( enckey != null )
                    {
                        byte encb[] = Utils.toByteArray ( enckey );
                        CObjList myids = index.getMyIdentities();

                        try
                        {
                            //membership for this one?
                            boolean ismember = false;

                            for ( int c0 = 0; c0 < myids.size() && !ismember; c0++ )
                            {
                                CObj myIdent = myids.get ( c0 );
                                RSAPrivateCrtKeyParameters pk =
                                    Utils.privateKeyFromString ( myIdent.getPrivate ( CObj.PRIVATEKEY ) );
                                dec = Utils.attemptAsymDecode ( pk, Utils.CID0, Utils.CID1, encb );

                                if ( dec != null )
                                {
                                    //Use the decoded key to decode the payload.
                                    ismember = true;
                                    String kstr = Utils.toString ( dec );
                                    KeyParameter sk = new KeyParameter ( dec );
                                    b.pushPrivate ( CObj.KEY, kstr );
                                    decoder.decode ( b, sk );
                                    //Attempt to decode the community data.
                                    //Note we may not have the community data yet!
                                    //Be sure to attempt to decode communtiy data as it
                                    //arrives or later.
                                    String comid = b.getPrivate ( CObj.COMMUNITYID );

                                    if ( comid != null )
                                    {
                                        //it has been decoded, but we may not have
                                        //community yet
                                        b.pushPrivate ( CObj.DECODED, "true" );
                                        CObj com = index.getCommunity ( comid );

                                        if ( com != null )
                                        {
                                            //Add the key to the community data.
                                            if ( decoder.decode ( com, sk ) )
                                            {
                                                b.pushPrivate ( CObj.MINE, "true" );
                                                com.pushPrivate ( CObj.KEY, kstr );
                                                com.pushPrivate ( CObj.MINE, "true" );
                                                index.index ( com );
                                            }

                                            else
                                            {
                                                //Something is wrong, it should have decoded.
                                                //Don't save this membership as mine
                                                dec = null;
                                            }

                                        }

                                    }

                                    else
                                    {
                                        //Something is wrong, it should have decoded.
                                        //Don't save this membership as mine
                                        dec = null;
                                    }

                                }

                            }

                        }

                        catch ( Exception e )
                        {
                            e.printStackTrace();
                        }

                        myids.close();
                    }

                    Session s = null;

                    try
                    {
                        s = session.getSession();
                        s.getTransaction().begin();
                        IdentityData id = ( IdentityData )
                                          s.get ( IdentityData.class, creatorid );

                        if ( id != null )
                        {
                            //Do not update the last number unless it is in sequence
                            //keeping track of wholes in the sequence nubmers is stupid.
                            if ( seqnum == ( id.getLastMembershipNumber() + 1 ) )
                            {
                                id.setLastMembershipNumber ( seqnum );
                                id.setNextClosestMembershipNumber ( seqnum );
                                id.setNumClosestMembershipNumber ( 1 );
                                s.merge ( id );
                            }

                            else
                            {
                                /*
                                    if there is a permanent gap in a sequence number
                                    count how many times we see the next number, so
                                    if we see it too many times we just use it for last
                                    number instead
                                */
                                if ( seqnum > id.getLastMembershipNumber() )
                                {
                                    if ( id.getNextClosestMembershipNumber() > seqnum ||
                                            id.getNextClosestMembershipNumber() <= id.getLastMembershipNumber() )
                                    {
                                        id.setNextClosestMembershipNumber ( seqnum );
                                        id.setNumClosestMembershipNumber ( 1 );
                                        s.merge ( id );
                                    }

                                    else if ( id.getNextClosestMembershipNumber() == seqnum )
                                    {
                                        id.setNumClosestMembershipNumber (
                                            id.getNumClosestMembershipNumber() + 1 );
                                        s.merge ( id );
                                    }

                                }

                            }

                        }

                        String comid = b.getPrivate ( CObj.COMMUNITYID );
                        String myid = b.getPrivate ( CObj.MEMBERID );

                        if ( comid != null && myid != null )
                        {
                            //Everytime we get a new one we reset the CommunityMyMember
                            CommunityMyMember mm = new CommunityMyMember();
                            mm.setId ( b.getDig() );
                            mm.setCommunityId ( b.getPrivate ( CObj.COMMUNITYID ) );
                            mm.setMemberId ( b.getPrivate ( CObj.MEMBERID ) );
                            mm.setKey ( dec );
                            s.merge ( mm );
                        }

                        s.getTransaction().commit();
                        s.close();
                        b.pushPrivateNumber ( CObj.LASTUPDATE, System.currentTimeMillis() );
                        index.index ( b );
                        guicallback.update ( b );
                    }

                    catch ( Exception e )
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

            return true;
        }

        return false;
    }

}
