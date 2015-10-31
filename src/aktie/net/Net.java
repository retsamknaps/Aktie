package aktie.net;

import java.io.File;

public interface Net
{

    public Destination getExistingDestination ( File privateinfo );

    public Destination getNewDestination();

    public String getStatus();

}
