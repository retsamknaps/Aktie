package aktie.headless;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;

import aktie.data.CObj;

public class ClientThread implements Runnable
{

    private HeadlessMain hmain;
    private Socket socket;
    private OutputStream outstream;
    private boolean stop;
    private ConcurrentLinkedQueue<CObj> outqueue;

    public ClientThread ( Socket s, HeadlessMain m )
    {
        socket = s;
        hmain = m;
        outqueue = new ConcurrentLinkedQueue<CObj>();
    }

    public synchronized void stop()
    {
        stop = true;
        notifyAll();
    }

    public synchronized void enqueue ( CObj o )
    {
        outqueue.add ( o );
        notifyAll();
    }

    public synchronized CObj waitForOut()
    {
        if ( outqueue.size() == 0 && !stop )
        {
            try
            {
                wait();
            }

            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }

        }

        return outqueue.poll();
    }

    @Override
    public void run()
    {
        try
        {
            outstream = socket.getOutputStream();

            while ( !stop )
            {
                CObj o = waitForOut();

                if ( o != null && !stop )
                {
                    JSONObject jo = o.getJSON();
                    String os = jo.toString();

                    try
                    {
                        byte ob[] = os.getBytes ( "UTF-8" );
                        outstream.write ( ob );
                        outstream.flush();
                    }

                    catch ( UnsupportedEncodingException e )
                    {
                        e.printStackTrace();
                    }

                }

            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        stop = true;
    }



}
