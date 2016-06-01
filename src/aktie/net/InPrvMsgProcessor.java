package aktie.net;

import org.bouncycastle.crypto.params.KeyParameter;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.PrivateMsgIdentity;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.utils.DigestValidator;
import aktie.utils.SymDecoder;

public class InPrvMsgProcessor extends GenericProcessor
{

    private DigestValidator validator;
    private Index index;
    private HH2Session session;
    private SymDecoder decoder;
    private GuiCallback guicallback;

    public InPrvMsgProcessor ( HH2Session s, Index i, GuiCallback cb )
    {
        index = i;
        session = s;
        guicallback = cb;
        decoder = new SymDecoder();
        validator = new DigestValidator ( index );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.PRIVMESSAGE.equals ( type ) )
        {
            if ( validator.newAndValid ( b ) )
            {
                //Update creator's ident index
                String creator = b.getString ( CObj.CREATOR );
                Long seqnum = b.getNumber ( CObj.SEQNUM );
                String msgid = b.getString ( CObj.MSGIDENT );

                if ( creator != null && seqnum != null && msgid != null )
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

                        if ( m.getLastMsgNumber() + 1 == ( long ) seqnum )
                        {
                            m.setLastMsgNumber ( seqnum );
                            m.setNextClosestMsgNumber ( seqnum );
                            m.setNumClosestMsgNumber ( 1 );
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
                            if ( seqnum > m.getLastMsgNumber() )
                            {
                                if ( m.getNextClosestMsgNumber() > seqnum ||
                                        m.getNextClosestMsgNumber() <= m.getLastMsgNumber() )
                                {
                                    m.setNextClosestMsgNumber ( seqnum );
                                    m.setNumClosestMsgNumber ( 1 );
                                    s.merge ( m );
                                }

                                else if ( m.getNextClosestMsgNumber() == seqnum )
                                {
                                    m.setNumClosestMsgNumber (
                                        m.getNumClosestMsgNumber() + 1 );
                                    s.merge ( m );
                                }

                            }

                        }

                        s.getTransaction().commit();

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

                                if ( decoder.decode ( b, sk ) )
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

                        index.index ( b );

                        if ( decoded )
                        {
                            guicallback.update ( b );
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
