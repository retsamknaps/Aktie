package aktie.user;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.hibernate.Session;

import aktie.GenericNoContextProcessor;
import aktie.ProcessQueue;
import aktie.UpdateCallback;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.index.Index;
import aktie.net.ConnectionListener;
import aktie.net.Destination;
import aktie.net.DestinationListener;
import aktie.net.DestinationThread;
import aktie.net.GetSendData2;
import aktie.net.Net;
import aktie.spam.SpamTool;

public class NewIdentityProcessor extends GenericNoContextProcessor
{
    Logger log = Logger.getLogger ( "aktie" );

    public static long DEF_USER_RANK = 4;

    private Net net;
    private UpdateCallback guicallback;
    private UpdateCallback netcallback;
    private Index index;
    private HH2Session session;
    private GetSendData2 conMan;
    private ConnectionListener conListener;
    private DestinationListener connectionMan;
    private RequestFileHandler fileHandler;
    private IdentityManager identManager;
    private File tmpDir;
    private ProcessQueue downloadQueue;
    private ProcessQueue preprocQueue;
    private ProcessQueue inputQueue;

    public NewIdentityProcessor ( Net n, GetSendData2 sd, HH2Session s, Index i, UpdateCallback g,
                                  UpdateCallback nc, ConnectionListener cl, DestinationListener cm, RequestFileHandler rf,
                                  SpamTool st, ProcessQueue preq, ProcessQueue inq, ProcessQueue dl )
    {
        fileHandler = rf;
        downloadQueue = dl;
        preprocQueue = preq;
        inputQueue = inq;
        connectionMan = cm;
        netcallback = nc;
        conListener = cl;
        conMan = sd;
        net = n;
        session = s;
        identManager = new IdentityManager ( s, i );
        index = i;
        guicallback = g;
    }

    public void setTmpDir ( File t )
    {
        tmpDir = t;
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
                DestinationThread dt = new DestinationThread ( d, conMan, session, index, netcallback,
                        conListener, fileHandler, preprocQueue, inputQueue, downloadQueue );
                dt.setTmpDir ( tmpDir );
                File df = d.savePrivateDestinationInfo();
                o.pushPrivate ( CObj.DEST, df.getPath() );
                o.pushString ( CObj.DEST, d.getPublicDestinationInfo() );
                byte [] id = Utils.digString ( ( byte[] ) null, o.getString ( CObj.KEY ) );
                o.setId ( Utils.toString ( id ) );
                o.signX ( ( RSAPrivateCrtKeyParameters ) pair.getPrivate(), 0 );
                o.pushPrivate ( CObj.MINE, "true" );

                o.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
                o.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

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

                try
                {
                    dt.setIdentity ( o );
                    o.pushPrivateNumber ( CObj.PRV_USER_RANK, DEF_USER_RANK );

                    long gseq = identManager.getGlobalSequenceNumber ( o.getId(), false );
                    o.pushPrivateNumber ( CObj.getGlobalSeq ( o.getId() ), gseq );

                    log.info ( "NEW IDENTITY: " + o.getId() + " name: " + o.getString ( CObj.NAME ) );

                    index.index ( o );
                    index.forceNewSearcher();
                    connectionMan.addDestination ( dt );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                    o.pushString ( CObj.ERROR, "failed to index new identity" );
                    guicallback.update ( o );
                    return true;
                }

            }

            guicallback.update ( o );
        }

        return false;
    }


}
