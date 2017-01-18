package aktie.net;

import org.bouncycastle.crypto.params.KeyParameter;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.sequences.PrivMsgSequence;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;
import aktie.utils.DigestValidator;
import aktie.utils.SymDecoder;

public class InPrvMsgProcessor extends GenericProcessor
{

    private DigestValidator validator;
    private Index index;
    private HH2Session session;
    private GuiCallback guicallback;
    private CObj ConId;
    private IdentityManager identManager;

    public InPrvMsgProcessor ( HH2Session s, Index i, SpamTool st, IdentityManager im, CObj mid, GuiCallback cb )
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

        if ( CObj.PRIVMESSAGE.equals ( type ) )
        {
            if ( validator.valid ( b ) )
            {
                boolean isnew = ( null == index.getByDig ( b.getDig() ) );
                //Update creator's ident index
                String creator = b.getString ( CObj.CREATOR );
                Long seqnum = b.getNumber ( CObj.SEQNUM );
                String msgid = b.getString ( CObj.MSGIDENT );

                if ( creator != null && seqnum != null && msgid != null )
                {

                    try
                    {
                        PrivMsgSequence mseq = new PrivMsgSequence ( session );
                        mseq.setId ( creator );
                        mseq.updateSequence ( b );

                        if ( isnew )
                        {
                            //Find identity for message
                            boolean decoded = false;
                            CObj mident = index.getPrivateMsgIdentity ( creator, msgid );

                            if ( mident != null )
                            {
                                String key = mident.getPrivate ( CObj.KEY );

                                if ( key != null )
                                {
                                    byte dec[] = Utils.toByteArray ( key );
                                    KeyParameter sk = new KeyParameter ( dec );

                                    if ( SymDecoder.decode ( b, sk ) )
                                    {
                                        b.pushPrivate ( CObj.DECODED, "true" );
                                        b.pushPrivate ( CObj.PRV_MSG_ID,
                                                        mident.getPrivate ( CObj.PRV_MSG_ID ) );
                                        b.pushPrivate ( CObj.PRV_RECIPIENT,
                                                        mident.getPrivate ( CObj.PRV_RECIPIENT ) );
                                        decoded = true;
                                    }

                                }

                            }

                            if ( !decoded )
                            {
                                b.pushPrivate ( CObj.DECODED, "false" );
                            }

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

                                if ( decoded )
                                {
                                    b.pushPrivate ( CObj.NAME, idty.getDisplayName() );
                                }

                            }

                            long seq = identManager.getGlobalSequenceNumber ( ConId.getId(), false );
                            b.pushPrivateNumber ( CObj.getGlobalSeq ( ConId.getId() ), seq );

                            index.index ( b );

                            if ( decoded )
                            {
                                guicallback.update ( b );
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
