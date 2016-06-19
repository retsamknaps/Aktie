package aktie.user;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortedNumericSortField;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
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

public class NewPrivateMessageProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private HH2Session session;
    private GuiCallback guicallback;
    private NewPushProcessor push;
    private SpamTool spamtool;

    public NewPrivateMessageProcessor ( HH2Session s, Index i, NewPushProcessor p, SpamTool st, GuiCallback gc )
    {
        session = s;
        index = i;
        guicallback = gc;
        spamtool = st;
        push = p;
    }

    @Override
    public boolean process ( CObj b )
    {
        String tp = b.getType();

        if ( CObj.PRIVMESSAGE.equals ( tp ) )
        {
            String creator = b.getString ( CObj.CREATOR );
            String recipient = b.getPrivate ( CObj.PRV_RECIPIENT );

            if ( creator == null || recipient == null )
            {
                b.pushString ( CObj.ERROR, "Creator and recipient isn't set" );
                guicallback.update ( b );
                return true;
            }

            CObj recid = index.getIdentity ( recipient );

            if ( recid == null )
            {
                b.pushString ( CObj.ERROR, "Recipient identity not found" );
                guicallback.update ( b );
                return true;
            }

            CObj myid = index.getMyIdentity ( creator );

            if ( myid == null )
            {
                b.pushString ( CObj.ERROR, "You may only use your own identity" );
                guicallback.update ( b );
                return true;
            }

            CObj updatemsg = new CObj();
            updatemsg.pushString ( CObj.ERROR, "Creating new private message. " );
            updatemsg.pushPrivate ( CObj.PRV_CLEAR_ERR, "false" );
            guicallback.update ( updatemsg );

            //Get the creator's random identity for the recipient.
            String pid = Utils.mergeIds ( creator, recipient );

            Sort srt = new Sort();
            srt.setSort ( new SortedNumericSortField ( CObj.docNumber ( CObj.SEQNUM ),
                          SortedNumericSortField.Type.LONG, true ) );
            //Add private message index
            CObjList idlst = index.getPrivateMyMsgIdentity ( pid, srt );
            CObj pident = null;

            if ( idlst.size() > 0 )
            {
                try
                {
                    pident = idlst.get ( 0 );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            idlst.close();

            long idseqnum = 0;
            long msgnum = 0;

            Session s = null;

            try
            {
                s = session.getSession();
                s.getTransaction().begin();
                PrivateMsgIdentity pm = ( PrivateMsgIdentity ) s.get ( PrivateMsgIdentity.class, creator );

                if ( pm == null )
                {
                    pm = new PrivateMsgIdentity();
                    pm.setId ( creator );
                    pm.setMine ( true );
                }

                if ( pident == null )
                {
                    idseqnum = pm.getLastIdentNumber();
                    idseqnum++;
                    pm.setLastIdentNumber ( idseqnum );
                }

                msgnum = pm.getLastMsgNumber();
                msgnum++;
                pm.setLastMsgNumber ( msgnum );

                s.merge ( pm );

                s.getTransaction().commit();
                s.close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
                b.pushString ( CObj.ERROR, "failed to save identity data" );
                guicallback.update ( b );

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

            //k create ident if needed
            if ( pident == null )
            {
                pident = new CObj();
                pident.setType ( CObj.PRIVIDENTIFIER );
                pident.pushString ( CObj.CREATOR, creator );
                pident.pushNumber ( CObj.SEQNUM, idseqnum );
                pident.pushString ( CObj.MSGIDENT, Long.toString ( Utils.Random.nextLong() ) );

                KeyParameter kp = Utils.generateKey();
                RSAKeyParameters mpk = Utils.publicKeyFromString ( recid.getString ( CObj.KEY ) );
                byte enckey[] = Utils.anonymousAsymEncode ( mpk, Utils.CID0, Utils.CID1, kp.getKey() );
                pident.pushString ( CObj.ENCKEY, Utils.toString ( enckey ) );

                pident.pushPrivate ( CObj.KEY, Utils.toString ( kp.getKey() ) );
                pident.pushPrivate ( CObj.PRV_MSG_ID, pid );
                pident.pushPrivate ( CObj.PRV_RECIPIENT, recipient );
                pident.pushPrivate ( CObj.MINE, "true" );
                pident.pushPrivate ( CObj.DECODED, "true" );
                pident.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
                pident.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

                spamtool.finalize ( Utils.privateKeyFromString ( myid.getPrivate ( CObj.PRIVATEKEY ) ),
                                    pident );

                //Set the rank of the post based on the rank of the
                //user
                Long rnk = myid.getPrivateNumber ( CObj.PRV_USER_RANK );

                if ( rnk != null )
                {
                    pident.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
                }

                try
                {
                    index.index ( pident );
                    index.forceNewSearcher();
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                    b.pushString ( CObj.ERROR, "failed to save identity data" );
                    guicallback.update ( b );
                    return true;
                }

                guicallback.update ( pident );

                if ( push != null )
                {
                    push.process ( pident );
                }

            }

            b.pushNumber ( CObj.SEQNUM, msgnum );
            b.pushString ( CObj.MSGIDENT, pident.getString ( CObj.MSGIDENT ) );

            StringBuilder sb = new StringBuilder();
            sb.append ( CObj.SUBJECT );
            sb.append ( "=" );
            sb.append ( b.getPrivate ( CObj.SUBJECT ) );
            sb.append ( "," );
            sb.append ( CObj.BODY );
            sb.append ( "=" );
            sb.append ( b.getPrivate ( CObj.BODY ) );
            String rawstr = sb.toString();
            byte raw[] = Utils.stringToByteArray ( rawstr );
            //encrypt the community key with the identity public key
            byte symkey[] = Utils.toByteArray ( pident.getPrivate ( CObj.KEY ) );
            KeyParameter kp = new KeyParameter ( symkey );
            byte enc[] = Utils.anonymousSymEncode ( kp, Utils.CID0,
                                                    Utils.CID1, raw );
            b.pushString ( CObj.PAYLOAD, Utils.toString ( enc ) );
            b.pushPrivateNumber ( CObj.CREATEDON, ( new Date() ).getTime() );

            sb = new StringBuilder();
            sb.append ( CObj.CREATEDON );
            sb.append ( "=" );
            sb.append ( b.getPrivateNumber ( CObj.CREATEDON ) );
            rawstr = sb.toString();
            raw = Utils.stringToByteArray ( rawstr );
            //encrypt the community key with the identity public key
            enc = Utils.anonymousSymEncode ( kp, Utils.CID0,
                                             Utils.CID1, raw );
            b.pushString ( CObj.PAYLOAD2, Utils.toString ( enc ) );

            b.pushString ( CObj.DECODED, "true" );
            b.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
            b.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );
            b.pushPrivate ( CObj.PRV_MSG_ID, pid );
            b.pushPrivate ( CObj.PRV_RECIPIENT, recipient );
            b.pushPrivate ( CObj.MINE, "true" );
            b.pushPrivate ( CObj.NAME, recid.getDisplayName() );

            spamtool.finalize ( Utils.privateKeyFromString ( myid.getPrivate ( CObj.PRIVATEKEY ) ),
                                b );

            //Set the rank of the post based on the rank of the
            //user
            Long rnk = myid.getPrivateNumber ( CObj.PRV_USER_RANK );

            if ( rnk != null )
            {
                b.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
            }

            try
            {
                index.index ( b );
                index.forceNewSearcher();
            }

            catch ( IOException e )
            {
                e.printStackTrace();
                b.pushString ( CObj.ERROR, "Bad error: " + e.getMessage() );
                guicallback.update ( b );
                return true;
            }

            guicallback.update ( b );

        }

        return false;
    }

}
