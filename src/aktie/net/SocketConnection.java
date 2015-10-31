package aktie.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketConnection implements Connection
{

    private String host;
    private int port;
    private Socket sock;

    public SocketConnection ( Socket s )
    {
        sock = s;
    }

    public SocketConnection ( String hst, int prt )
    {
        host = hst;
        port = prt;
    }

    @Override
    public InputStream getInputStream()
    {
        try
        {
            return sock.getInputStream();
        }

        catch ( IOException e )
        {
            e.printStackTrace();
            throw new RuntimeException ( "oops", e );
        }

    }

    @Override
    public OutputStream getOutputStream()
    {
        try
        {
            return sock.getOutputStream();
        }

        catch ( IOException e )
        {
            e.printStackTrace();
            throw new RuntimeException ( "oops", e );
        }

    }

    @Override
    public void close()
    {
        try
        {
            sock.close();
        }

        catch ( IOException e )
        {
            e.printStackTrace();
        }

    }

    @Override
    public void connect() throws IOException
    {
        if ( sock == null && host != null )
        {
            sock = new Socket ( host, port );
        }

    }

}
