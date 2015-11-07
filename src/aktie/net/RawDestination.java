package aktie.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class RawDestination implements Destination
{

    private ServerSocket servsock;
    private File nodeDir;

    public RawDestination ( ServerSocket s, File nd )
    {
        nodeDir = nd;
        servsock = s;
    }

    @Override
    public File savePrivateDestinationInfo()
    {
        try
        {
            File lf = File.createTempFile ( "desttest", ".dat", nodeDir );
            FileOutputStream fos = new FileOutputStream ( lf );
            PrintWriter pw = new PrintWriter ( fos );
            pw.println ( servsock.getLocalPort() );
            pw.close();
            return lf;
        }

        catch ( Exception e )
        {
            throw new RuntimeException ( "oops. ", e );
        }

    }

    @Override
    public String getPublicDestinationInfo()
    {
        String addr = servsock.getInetAddress().getHostAddress();
        addr = addr + ":" + servsock.getLocalPort();
        return addr;
    }

    @Override
    public Connection connect ( String destination )
    {
        String p[] = destination.split ( ":" );
        int prt = Integer.valueOf ( p[1] );

        return new SocketConnection ( p[0], prt );
    }

    @Override
    public Connection accept()
    {
        try
        {
            Socket s = servsock.accept();
            return new SocketConnection ( s );
        }

        catch ( IOException e )
        {
        }

        return null;
    }

    @Override
    public void close()
    {
        try
        {
            servsock.close();
        }

        catch ( IOException e )
        {
        }

    }

}
