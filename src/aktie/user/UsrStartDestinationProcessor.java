package aktie.user;

import java.io.File;

import aktie.GenericProcessor;
import aktie.UpdateCallback;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.index.Index;
import aktie.net.ConnectionListener;
import aktie.net.ConnectionManager2;
import aktie.net.Destination;
import aktie.net.DestinationListener;
import aktie.net.DestinationThread;
import aktie.net.Net;
import aktie.spam.SpamTool;
import aktie.utils.FUtils;

public class UsrStartDestinationProcessor extends GenericProcessor
{

    private Net net;
    private UpdateCallback guicallback;
    private UpdateCallback netcallback;
    private Index index;
    private HH2Session session;
    private ConnectionManager2 conMan;
    private ConnectionListener conListener;
    private DestinationListener connectionMan;
    private RequestFileHandler fileHandler;
    private SpamTool spamtool;
    private File tmpDir;

    public UsrStartDestinationProcessor ( Net n, ConnectionManager2 sd, HH2Session s, Index i, UpdateCallback g, UpdateCallback nc, ConnectionListener cl, DestinationListener cm, RequestFileHandler rf, SpamTool st )
    {
        fileHandler = rf;
        connectionMan = cm;
        netcallback = nc;
        conListener = cl;
        conMan = sd;
        net = n;
        session = s;
        index = i;
        guicallback = g;
        spamtool = st;
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
                        DestinationThread dt = new DestinationThread ( d, conMan, session, index, netcallback, conListener, fileHandler, spamtool );
                        dt.setTmpDir ( tmpDir );
                        dt.setIdentity ( o );
                        connectionMan.addDestination ( dt );
                    }

                    else
                    {
                        updatemsg.pushString ( CObj.ERROR, "Could not start destination" );
                        guicallback.update ( updatemsg );
                        return true;
                    }


                    String pubdest = d.getPublicDestinationInfo();

                    if ( !pubdest.equals ( deststr ) )
                    {
                        System.out.println ( "OH NO!  THIS IS BROKEN! DEST WAS:" );
                        System.out.println ( deststr );
                        System.out.println ( "BUT IS NOW:" );
                        System.out.println ( pubdest );
                    }

                    File tf = d.savePrivateDestinationInfo();

                    try
                    {
                        if ( !FUtils.diff ( f, tf ) )
                        {
                            System.out.println ( "NEW DESTINATION FILE!: " + tf.getPath() + " was: " + f.getPath() );
                            o.pushPrivate ( CObj.DEST, tf.getPath() );
                            index.index ( o );
                        }

                        else
                        {
                            tf.delete();
                        }

                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
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
