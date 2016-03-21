package aktie.headless;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;

import aktie.data.CObj;
import aktie.json.CleanParser;

public class ClientThread implements Runnable
{

    private HeadlessMain hmain;
    private Socket socket;
    private OutputStream outstream;
    private boolean stop;
    private ConcurrentLinkedQueue<CObj> outqueue;
    private ClientReqIndexProcessor cliReqProc;

    public ClientThread ( Socket s, HeadlessMain m )
    {
        socket = s;
        hmain = m;
        cliReqProc = new ClientReqIndexProcessor ( this, hmain.getNode().getIndex() );
        outqueue = new ConcurrentLinkedQueue<CObj>();
        Thread t = new Thread ( this );
        t.start();
    }

    public HeadlessMain getMain()
    {
        return hmain;
    }

    public synchronized void stop()
    {
        stop = true;
        notifyAll();

        if ( socket != null )
        {
            try
            {
                if ( !socket.isClosed() )
                {
                    socket.close();
                }

            }

            catch ( Exception e )
            {
            }

        }

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

    class InputThread implements Runnable
    {

        public InputStream instream;

        @Override
        public void run()
        {
            CleanParser parser = new CleanParser ( instream );
            parser.setPermissive ( true );

            try
            {
                while ( !stop )
                {
                    JSONObject o = parser.next();
                    CObj co = new CObj();
                    co.loadJSON ( o );

                    if ( !cliReqProc.process ( co ) )
                    {
                        hmain.getNode().enqueue ( co );
                    }

                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

            stop();
        }

    }

    @Override
    public void run()
    {
        try
        {
            outstream = socket.getOutputStream();
            InputThread it = new InputThread();
            it.instream = socket.getInputStream();
            Thread t = new Thread ( it );
            t.start();

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

        stop();
    }



}
