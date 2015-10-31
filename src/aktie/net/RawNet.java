package aktie.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;

public class RawNet implements Net
{

    private File nodeDir;

    public RawNet ( File nd )
    {
        nodeDir = nd;
    }

    @Override
    public Destination getExistingDestination ( File privateinfo )
    {
        try
        {
            BufferedReader br = new BufferedReader ( new FileReader ( privateinfo ) );
            String p = br.readLine();
            br.close();
            int port = Integer.valueOf ( p );
            ServerSocket ss = new ServerSocket ( port );
            return new RawDestination ( ss, nodeDir );
        }

        catch ( IOException e )
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Destination getNewDestination()
    {
        try
        {
            ServerSocket ss = new ServerSocket ( 0 );
            RawDestination d = new RawDestination ( ss, nodeDir );
            return d;
        }

        catch ( IOException e )
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getStatus()
    {
        return "Running";
    }

}
