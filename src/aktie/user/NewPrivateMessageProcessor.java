package aktie.user;

import java.util.Date;
import java.util.logging.Logger;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.data.PrivateMsgIdentity;
import aktie.gui.GuiCallback;
import aktie.index.Index;

public class NewPrivateMessageProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private HH2Session session;
    private GuiCallback guicallback;

    public NewPrivateMessageProcessor ( HH2Session s, Index i, GuiCallback gc )
    {
        session = s;
        index = i;
        guicallback = gc;
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

            //Get the creator's random identity for the recipient.
            String pid = Utils.mergeIds ( creator, recipient );

            Sort srt = new Sort();

            srt.setSort ( new SortField ( CObj.docPrivate ( CObj.PRV_DISPLAY_NAME ), SortField.Type.STRING, true ) );
            //Add private message index
            index.getPrivateMsgIdentity ( pid, srt );
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

            }


            return true;
        }

        return false;
    }

}
