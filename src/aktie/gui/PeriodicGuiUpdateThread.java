package aktie.gui;

import org.eclipse.swt.widgets.Display;

public class PeriodicGuiUpdateThread implements Runnable
{

    private SWTApp app;

    private boolean running = false;

    public PeriodicGuiUpdateThread ( SWTApp app )
    {
        this.app = app;

        Thread t = new Thread ( this, "Periodic GUI Update Thread" );
        t.start();
    }

    @Override
    public void run()
    {
        running = true;

        long lastUpdate = 0L;

        long lastTotalInBytes = 0L;
        long lastTotalOutBytes = 0L;

        while ( running )
        {
            long curtime = System.currentTimeMillis();
            final long dt = curtime - lastUpdate;

            if ( dt > SWTApp.UPDATE_INTERVAL )
            {
                long curTotalInBytes = app.getConnectionCallback().getTotalInBytes();
                long curTotalOutBytes = app.getConnectionCallback().getTotalOutBytes();

                final long deltaInBytes = curTotalInBytes - lastTotalInBytes;
                final long deltaOutBytes = curTotalOutBytes - lastTotalOutBytes;
                lastTotalInBytes = curTotalInBytes;
                lastTotalOutBytes = curTotalOutBytes;

                Display.getDefault().syncExec ( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        PeriodicGuiUpdateThread.this.app.updateConnections ( deltaInBytes, deltaOutBytes, dt );
                    }

                } );

                lastUpdate = curtime;
            }

            try
            {
                Thread.sleep ( 200 );
            }

            catch ( InterruptedException e )
            {
            }

        }

    }

    public void stop()
    {
        running = false;
    }

}
