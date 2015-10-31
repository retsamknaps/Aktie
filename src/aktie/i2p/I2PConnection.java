package aktie.i2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import aktie.net.Connection;

public class I2PConnection implements Connection
{

    private String destination;
    private I2PSocket socket;
    private I2PSocketManager manager;

    public I2PConnection ( I2PSocketManager man, String dest )
    {
        manager = man;
        destination = dest;
    }

    public I2PConnection ( I2PSocket s )
    {
        socket = s;
    }

    @Override
    public InputStream getInputStream()
    {
        try
        {
            return socket.getInputStream();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public OutputStream getOutputStream()
    {
        try
        {
            return socket.getOutputStream();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void close()
    {
        try
        {
            socket.close();
        }

        catch ( Exception e )
        {
        }

    }

    @Override
    public void connect() throws IOException
    {
        if ( socket == null && destination != null )
        {
            try
            {
                socket = manager.connect (
                             new net.i2p.data.Destination ( destination ) );
            }

            catch ( Exception e )
            {
                throw new IOException ( e );
            }

        }

    }

}
