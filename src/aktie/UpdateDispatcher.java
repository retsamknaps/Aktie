package aktie;

import java.util.LinkedList;
import java.util.List;

public class UpdateDispatcher implements UpdateCallback
{

    private List<UpdateCallback> list;

    public UpdateDispatcher()
    {
        list = new LinkedList<UpdateCallback>();
    }

    public void addUpdateListener ( UpdateCallback uc )
    {
        list.add ( uc );
    }

    @Override
    public void update ( Object o )
    {
        for ( UpdateCallback uc : list )
        {
            uc.update ( o );
        }

    }

}
