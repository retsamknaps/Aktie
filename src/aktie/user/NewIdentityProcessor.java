package aktie.user;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.net.ConnectionListener;
import aktie.net.Destination;
import aktie.net.DestinationListener;
import aktie.net.DestinationThread;
import aktie.net.GetSendData;
import aktie.net.Net;

public class NewIdentityProcessor extends GenericProcessor
{

    private Net net;
    private GuiCallback guicallback;
    private GuiCallback netcallback;
    private Index index;
    private HH2Session session;
    private GetSendData sendData;
    private ConnectionListener conListener;
    private DestinationListener connectionMan;
    private RequestFileHandler fileHandler;

    public NewIdentityProcessor ( Net n, GetSendData sd, HH2Session s, Index i, GuiCallback g, GuiCallback nc, ConnectionListener cl, DestinationListener cm, RequestFileHandler rf )
    {
        fileHandler = rf;
        connectionMan = cm;
        netcallback = nc;
        conListener = cl;
        sendData = sd;
        net = n;
        session = s;
        index = i;
        guicallback = g;
    }

    /**
        Must set
        type: identity
        string: name
    */
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.IDENTITY.equals ( type ) )
        {
            if ( o.getStrings() == null || o.getStrings().get ( CObj.NAME ) == null ||
                    o.getStrings().get ( CObj.NAME ).equals ( "" ) )
            {
                o.pushString ( CObj.ERROR, "name must be set to create a new identity" );
                guicallback.update ( o );
                return true;
            }

            else
            {
                CObj updatemsg = new CObj();
                updatemsg.pushString ( CObj.ERROR, "Creating new identity.." );
                updatemsg.pushPrivate ( CObj.PRV_CLEAR_ERR, "false" );
                guicallback.update ( updatemsg );

                AsymmetricCipherKeyPair pair = Utils.generateKeyPair();
                o.pushPrivate ( CObj.PRIVATEKEY, Utils.stringFromPrivateKey (
                                    ( RSAPrivateCrtKeyParameters ) pair.getPrivate() ) );
                o.pushString ( CObj.KEY, Utils.stringFromPublicKey (
                                   ( RSAKeyParameters ) pair.getPublic() ) );
                Destination d = net.getNewDestination();
                DestinationThread dt = new DestinationThread ( d, sendData, session, index, netcallback, conListener, fileHandler );
                File df = d.savePrivateDestinationInfo();
                o.pushPrivate ( CObj.DEST, df.getPath() );
                o.pushString ( CObj.DEST, d.getPublicDestinationInfo() );
                byte [] id = Utils.digString ( ( byte[] ) null, o.getString ( CObj.KEY ) );
                o.setId ( Utils.toString ( id ) );
                o.sign ( ( RSAPrivateCrtKeyParameters ) pair.getPrivate() );
                o.pushPrivate ( CObj.MINE, "true" );

                o.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
                o.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

                try
                {
                    dt.setIdentity ( o );
                    index.index ( o );
                    connectionMan.addDestination ( dt );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                    o.pushString ( CObj.ERROR, "failed to index new identity" );
                    guicallback.update ( o );
                    return true;
                }

                Session s = null;

                try
                {
                    s = session.getSession();
                    s.getTransaction().begin();
                    IdentityData idat = new IdentityData();
                    idat.setId ( o.getId() );
                    idat.setFirstSeen ( ( new Date() ).getTime() );
                    idat.setMine ( true );
                    s.merge ( idat );
                    s.getTransaction().commit();
                    s.close();
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                    o.pushString ( CObj.ERROR, "failed to save identity data" );
                    guicallback.update ( o );

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

            }

            guicallback.update ( o );
        }

        return false;
    }


}
