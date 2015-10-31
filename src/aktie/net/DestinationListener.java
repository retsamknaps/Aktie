package aktie.net;

import aktie.data.CObj;

public interface DestinationListener
{

    public void addDestination ( DestinationThread d );

    public boolean isDestinationOpen ( String dest );

    public void closeDestination ( CObj myid );

}
