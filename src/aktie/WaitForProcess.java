package aktie;

public class WaitForProcess extends ContextObject
{

    public static void waitForProcess ( Object context, Object procobj, ProcessQueue q ) throws InterruptedException
    {
        WaitForProcess wt = new WaitForProcess ( context, procobj, q );
        wt.waitForProcess();
    }

    public WaitForProcess ( Object context, Object subobj, ProcessQueue q )
    {
        queue = q;
        this.context = context;
        this.obj = subobj;
    }

    private ProcessQueue queue;

    public synchronized void waitForProcess() throws InterruptedException
    {
        queue.enqueue ( this );
        wait();
    }

    @Override

    public synchronized void notifyProcessed()
    {
        notifyAll();
    }


}
