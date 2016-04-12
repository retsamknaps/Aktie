package aktie.index;

public class WaitForNewSearcher
{

    private boolean started = false;
    private boolean complete = false;

    public synchronized void buildStarted()
    {
        started = true;
        notifyAll();
    }

    public synchronized void buildComplete()
    {
        complete = started;
        notifyAll();
    }

    public synchronized void waitForNewSearcher()
    {
        while ( !complete )
        {
            try
            {
                wait ( 1000L );
            }

            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }

        }

    }

    public synchronized boolean isComplete()
    {
        return complete;
    }


}
