package aktie.net;

import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.UpdateCallback;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMyMember;
import aktie.data.HH2Session;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.sequences.MemberSequence;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;
import aktie.utils.DigestValidator;
import aktie.utils.SymDecoder;

public class InMemProcessor extends GenericProcessor
{

    private UpdateCallback guicallback;
    private Index index;
    private HH2Session session;
    private DigestValidator validator;
    private CObj ConId;
    private IdentityManager identManager;

    public InMemProcessor ( HH2Session s, Index i, SpamTool st, IdentityManager im, CObj mid, UpdateCallback cb )
    {
        index = i;
        session = s;
        guicallback = cb;
        ConId = mid;
        identManager = im;
        validator = new DigestValidator ( index, st );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.MEMBERSHIP.equals ( type ) )
        {
            //Check with enckey only!  Decode with sym key later.
            if ( validator.valid ( b ) )
            {
                boolean isnew = ( null == index.getByDig ( b.getDig() ) );

                Long seqnum = b.getNumber ( CObj.SEQNUM );
                String creatorid = b.getString ( CObj.CREATOR );

                if ( seqnum != null && creatorid != null )
                {
                    //See if this is a membership for one of this node's identities
                    b.pushPrivate ( CObj.DECODED, "false" );
                    b.pushPrivate ( CObj.VALIDMEMBER, "false" );
                    byte dec[] = null;
                    String enckey = b.getString ( CObj.ENCKEY );

                    if ( enckey != null && isnew )
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
                                    SymDecoder.decode ( b, sk );
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
                                            if ( SymDecoder.decode ( com, sk ) )
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
                        //Find the last sequence number to set.
                        MemberSequence memseq = new MemberSequence ( session );
                        memseq.setId ( creatorid );
                        memseq.updateSequence ( b );

                        s = session.getSession();
                        s.getTransaction().begin();

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

                        if ( isnew )
                        {
                            b.pushPrivateNumber ( CObj.LASTUPDATE, System.currentTimeMillis() );

                            //Set the rank of the post based on the rank of the
                            //user

                            CObj idty = index.getIdentity ( creatorid );

                            if ( idty != null )
                            {
                                Long rnk = idty.getPrivateNumber ( CObj.PRV_USER_RANK );

                                if ( rnk != null )
                                {
                                    b.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
                                }

                            }

                            long seq = identManager.getGlobalSequenceNumber ( ConId.getId(), true );
                            b.pushPrivateNumber ( CObj.getGlobalSeq ( ConId.getId() ), seq );

                            index.index ( b );
                            guicallback.update ( b );

                        }

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
