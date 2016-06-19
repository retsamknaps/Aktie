package aktie.net;

import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.PrivateMsgIdentity;
import aktie.gui.GuiCallback;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.utils.DigestValidator;
import aktie.utils.SymDecoder;

public class InPrvIdentProcessor extends GenericProcessor
{

    private DigestValidator validator;
    private Index index;
    private HH2Session session;
    private SymDecoder decoder;
    private GuiCallback guicallback;

    public InPrvIdentProcessor ( HH2Session s, Index i, SpamTool st, GuiCallback cb )
    {
        index = i;
        session = s;
        guicallback = cb;
        decoder = new SymDecoder();
        validator = new DigestValidator ( index, st );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.PRIVIDENTIFIER.equals ( type ) )
        {
            if ( validator.newAndValid ( b ) )
            {
                //Update creator's ident index
                String creator = b.getString ( CObj.CREATOR );
                Long seqnum = b.getNumber ( CObj.SEQNUM );
                String enckey = b.getString ( CObj.ENCKEY );
                String msgid = b.getString ( CObj.MSGIDENT );

                if ( creator != null && seqnum != null && enckey != null && msgid != null )
                {
                    Session s = null;

                    try
                    {
                        s = session.getSession();
                        s.getTransaction().begin();
                        PrivateMsgIdentity m = ( PrivateMsgIdentity )
                                               s.get ( PrivateMsgIdentity.class, creator );

                        if ( m == null )
                        {
                            m = new PrivateMsgIdentity();
                            m.setId ( creator );
                            s.persist ( m );
                        }

                        if ( m.getLastIdentNumber() + 1 == ( long ) seqnum )
                        {
                            m.setLastIdentNumber ( seqnum );
                            m.setNextClosestIdentNumber ( seqnum );
                            m.setNumClosestIdentNumber ( 1 );
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
                            if ( seqnum > m.getLastIdentNumber() )
                            {
                                if ( m.getNextClosestIdentNumber() > seqnum ||
                                        m.getNextClosestIdentNumber() <= m.getLastIdentNumber() )
                                {
                                    m.setNextClosestIdentNumber ( seqnum );
                                    m.setNumClosestIdentNumber ( 1 );
                                    s.merge ( m );
                                }

                                else if ( m.getNextClosestIdentNumber() == seqnum )
                                {
                                    m.setNumClosestIdentNumber (
                                        m.getNumClosestIdentNumber() + 1 );
                                    s.merge ( m );
                                }

                            }

                        }

                        s.getTransaction().commit();

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

                        }

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

                                if ( decoder.decode ( dm, sk ) )
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

                    catch ( Exception e )
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

                        }

                    }

                    finally
                    {
                        if ( s != null )
                        {
                            try
                            {
                                s.close();
                            }

                            catch ( Exception e )
                            {
                                e.printStackTrace();
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
