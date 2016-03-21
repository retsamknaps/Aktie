package aktie.headless;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import aktie.gui.Wrapper;

public class ClientServer implements Runnable
{

    private boolean stop;
    private HeadlessMain hmain;
    private ServerSocket server;

    public ClientServer ( HeadlessMain m )
    {
        hmain = m;
        Thread t = new Thread ( this );
        t.start();
    }

    public void stop()
    {
        stop = true;

        if ( server != null )
        {
            try
            {
                server.close();
            }

            catch ( Exception e )
            {
            }

        }

    }

    @Override
    public void run()
    {
        try
        {
            String cli = Wrapper.getClientInterface();
            int port = Wrapper.getClientPort();
            System.out.println ( "==================================================" );
            System.out.println ( "Listen for client on: " + cli + " : " + port );
            System.out.println ( "==================================================" );
            InetSocketAddress a = new InetSocketAddress ( InetAddress.getByName ( cli ), port );
            server = new ServerSocket();
            server.bind ( a );

            try
            {
                while ( !stop )
                {
                    Socket s = server.accept();
                    new ClientThread ( s, hmain );
                }

            }

            catch ( Exception e )
            {
            }

            server.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

}
