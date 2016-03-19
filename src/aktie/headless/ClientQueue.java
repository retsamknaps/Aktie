package aktie.headless;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import aktie.Node;
import aktie.gui.Wrapper;

public class ClientQueue implements Runnable
{

    private boolean stop;
    private Node node;

    @Override
    public void run()
    {
        try
        {
            String cli = Wrapper.getClientInterface();
            int port = Wrapper.getClientPort();
            InetSocketAddress a = new InetSocketAddress ( InetAddress.getByName ( cli ), port );
            ServerSocket srv = new ServerSocket();
            srv.bind ( a );

            try
            {
                while ( !stop )
                {
                    Socket s = srv.accept();
                    //TODO: Run socket
                }

            }

            catch ( Exception e )
            {
            }

            srv.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

}
