package aktie.net;

import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import aktie.GenericProcessor;
import aktie.UpdateCallback;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.sequences.PrivIdentSequence;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;
import aktie.utils.DigestValidator;
import aktie.utils.SymDecoder;

public class InPrvIdentProcessor extends GenericProcessor
{

    private DigestValidator validator;
    private Index index;
    private HH2Session session;
    private UpdateCallback guicallback;
    private CObj ConId;
    private IdentityManager identManager;

    public InPrvIdentProcessor ( HH2Session s, Index i, SpamTool st, IdentityManager im, CObj mid, UpdateCallback cb )
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

        if ( CObj.PRIVIDENTIFIER.equals ( type ) )
        {
            if ( validator.valid ( b ) )
            {
                boolean isnew = ( null == index.getByDig ( b.getDig() ) );

                //Update creator's ident index
                String creator = b.getString ( CObj.CREATOR );
                Long seqnum = b.getNumber ( CObj.SEQNUM );
                String enckey = b.getString ( CObj.ENCKEY );
                String msgid = b.getString ( CObj.MSGIDENT );

                if ( creator != null && seqnum != null && enckey != null && msgid != null )
                {
                    try
                    {

                        PrivIdentSequence pseq = new PrivIdentSequence ( session );
                        pseq.setId ( creator );
                        pseq.updateSequence ( b );

                        if ( isnew )
                        {
                            //Ok, see if it is for me
                            byte encb[] = Utils.toByteArray ( enckey );

                            byte dec[] = null;
                            CObjList myids = index.getMyIdentities();

                            for ( int c = 0; c < myids.size() && dec == null; c++ )
                            {
                                CObj myid = myids.get ( c );
                                RSAPrivateCrtKeyParameters pk =
                                    Utils.privateKeyFromString ( myid.getPrivate ( CObj.PRIVATEKEY ) );
                                dec = Utils.attemptAsymDecode ( pk, Utils.CID0, Utils.CID1, encb );

                                if ( dec != null )
                                {
                                    String kstr = Utils.toString ( dec );
                                    String pid = Utils.mergeIds ( creator, myid.getId() );
                                    b.pushPrivate ( CObj.KEY, kstr );
                                    b.pushPrivate ( CObj.PRV_MSG_ID, pid );
                                    b.pushPrivate ( CObj.PRV_RECIPIENT, myid.getId() );
                                    b.pushPrivate ( CObj.DECODED, "true" );
                                }

                            }

                            myids.close();

                            //Set the rank of the post based on the rank of the
                            //user
                            CObj idty = index.getIdentity ( creator );

                            if ( idty != null )
                            {
                                Long rnk = idty.getPrivateNumber ( CObj.PRV_USER_RANK );

                                if ( rnk != null )
                                {
                                    b.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
                                }

                                b.pushPrivate ( CObj.NAME, idty.getDisplayName() );

                            }

                            long seq = identManager.getGlobalSequenceNumber ( ConId.getId(), false );
                            b.pushPrivateNumber ( CObj.getGlobalSeq ( ConId.getId() ), seq );

                            index.index ( b );
                            //Force new searcher so new private messages received after this
                            //are decoded
                            index.forceNewSearcher();

                            if ( dec != null )
                            {
                                guicallback.update ( b );
                                KeyParameter sk = new KeyParameter ( dec );
                                CObjList ndmsg = index.getPrivateMsgNotDecoded ( msgid );

                                for ( int c = 0; c < ndmsg.size(); c++ )
                                {
                                    CObj dm = ndmsg.get ( c );

                                    if ( SymDecoder.decode ( dm, sk ) )
                                    {
                                        dm.pushPrivate ( CObj.PRV_MSG_ID,
                                                         b.getPrivate ( CObj.PRV_MSG_ID ) );
                                        dm.pushPrivate ( CObj.PRV_RECIPIENT,
                                                         b.getPrivate ( CObj.PRV_RECIPIENT ) );
                                        dm.pushPrivate ( CObj.NAME, idty.getDisplayName() );
                                        dm.pushPrivate ( CObj.DECODED, "true" );
                                        index.index ( dm );
                                        guicallback.update ( dm );
                                    }

                                }

                                ndmsg.close();
                            }

                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();

                    }

                }

            }

            return true;
        }

        return false;
    }

}
