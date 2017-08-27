package aktie.net;

import java.util.Date;

import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.UpdateCallback;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.index.Index;
import aktie.user.IdentityManager;

public class InIdentityProcessor extends GenericProcessor
{

    public static long DEF_USER_RANK = 5;

    private UpdateCallback guicallback;
    private Index index;
    private HH2Session session;
    private CObj ConId;
    private IdentityManager identManager;

    public InIdentityProcessor ( HH2Session s, Index i, IdentityManager im )
    {
        index = i;
        session = s;
        identManager = im;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.IDENTITY.equals ( type ) )
        {
            Session s = null;

            try
            {
                String id = b.getId();
                String dig = b.getDig();
                String name = b.getString ( CObj.NAME );
                String key = b.getString ( CObj.KEY );

                if ( key != null && id != null && dig != null && name != null )
                {
                    s = session.getSession();
                    s.getTransaction().begin();
                    IdentityData idat = ( IdentityData ) s.get ( IdentityData.class, id );
                    boolean insert = false;

                    if ( idat == null )
                    {
                        RSAKeyParameters pk = Utils.publicKeyFromString ( key );
                        byte [] cid = Utils.digString ( ( byte[] ) null, key );
                        String cids = Utils.toString ( cid );

                        if ( cids.equals ( id ) )
                        {
                            if ( b.checkSignatureX ( pk, 0 ) )
                            {
                                idat = new IdentityData();
                                idat.setFirstSeen ( ( new Date() ).getTime() );
                                idat.setId ( id );
                                s.merge ( idat );
                                insert = true;
                            }

                        }

                    }

                    s.getTransaction().commit();
                    s.close();

                    if ( insert )
                    {
                        b.pushPrivateNumber ( CObj.PRV_USER_RANK, DEF_USER_RANK );

                        if ( identManager != null && ConId != null )
                        {
                            long seq = identManager.getGlobalSequenceNumber ( ConId.getId(), false );
                            b.pushPrivateNumber ( CObj.getGlobalSeq ( ConId.getId() ), seq );
                        }

                        index.index ( b, true );

                        if ( guicallback != null )
                        {
                            guicallback.update ( b );
                        }

                    }

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
                        e2.printStackTrace();
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

    public void setCallback ( UpdateCallback cb )
    {
        guicallback = cb;
    }

    @Override
    public void setContext ( Object c )
    {
        ConnectionThread ct = ( ConnectionThread ) c;
        ConId = ct.getLocalDestination().getIdentity();
        guicallback = ct;
    }

}
