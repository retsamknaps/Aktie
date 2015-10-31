package aktie.user;

import java.io.File;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.net.ConnectionListener;
import aktie.net.Destination;
import aktie.net.DestinationListener;
import aktie.net.DestinationThread;
import aktie.net.GetSendData;
import aktie.net.Net;

public class UsrStartDestinationProcessor extends GenericProcessor
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

    public UsrStartDestinationProcessor ( Net n, GetSendData sd, HH2Session s, Index i, GuiCallback g, GuiCallback nc, ConnectionListener cl, DestinationListener cm, RequestFileHandler rf )
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

        if ( CObj.USR_START_DEST.equals ( type ) )
        {

            o.setType ( CObj.IDENTITY );

            Long on = o.getPrivateNumber ( CObj.PRV_DEST_OPEN );

            if ( on == null )
            {
                on = 1L;
            }

            if ( on == 1L )
            {
                /*
                    we only have 1 user process thread.  So we can serially check
                    if a destination is already running, and then start it if not and
                    we do not have a problem starting it twice.
                */
                String destfile = o.getPrivate ( CObj.DEST );
                String deststr = o.getString ( CObj.DEST );

                if ( destfile != null && ( !connectionMan.isDestinationOpen ( deststr ) ) )
                {
                    CObj updatemsg = new CObj();
                    updatemsg.pushString ( CObj.ERROR, "Starting destination: " + o.getDisplayName() );
                    updatemsg.pushPrivate ( CObj.PRV_CLEAR_ERR, "false" );
                    guicallback.update ( updatemsg );

                    File f = new File ( destfile );
                    Destination d = net.getExistingDestination ( f );

                    if ( d != null )
                    {
                        DestinationThread dt = new DestinationThread ( d, sendData, session, index, netcallback, conListener, fileHandler );
                        dt.setIdentity ( o );
                        connectionMan.addDestination ( dt );
                    }

                    else
                    {
                        updatemsg.pushString ( CObj.ERROR, "Could not start destination" );
                        guicallback.update ( updatemsg );
                        return true;
                    }

                }

                else
                {
                    CObj updatemsg = new CObj();
                    updatemsg.pushString ( CObj.ERROR, "Destination already open or not found" );
                    guicallback.update ( updatemsg );
                    return true;
                }

            }

            else
            {
                connectionMan.closeDestination ( o );
            }

            try
            {
                index.index ( o );
                guicallback.update ( o );
            }

            catch ( Exception e )
            {
                CObj updatemsg = new CObj();
                updatemsg.pushString ( CObj.ERROR, "Bad problem: " + e.getMessage() );
                guicallback.update ( updatemsg );
            }

            return true;

        }

        return false;
    }


}
