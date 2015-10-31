package aktie.net;

import java.io.File;

public interface Destination
{

    public File savePrivateDestinationInfo();

    public String getPublicDestinationInfo();

    public Connection connect ( String destination );

    public Connection accept();

    public void close();

}
