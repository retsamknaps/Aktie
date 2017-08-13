package aktie;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.Test;

public class WebSocketTest
{

    public class MClient extends WebSocketClient
    {

        boolean isopen = false;

        public MClient ( URI serverUri )
        {
            super ( serverUri );
        }

        @Override
        public void onClose ( int i, String s, boolean b )
        {
            System.out.println ( "MClient onClose!-------------------------------" );
            Thread.dumpStack();
        }

        @Override
        public void onError ( Exception e )
        {
            System.out.println ( "MClient onError!-------------------------------" );
            e.printStackTrace();
        }

        @Override

        public synchronized void onOpen ( ServerHandshake s )
        {
            System.out.println ( "Client onOpen" );
            isopen = true;
            notifyAll();
        }

        public synchronized boolean waitUntilOpen()
        {
            int timeout = 120;

            while ( !isopen && timeout > 0 )
            {
                try
                {
                    wait ( 1000L );
                }

                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }

                timeout--;
            }

            return timeout > 0;
        }

        public ByteBuffer lastbuf;
        public String lastmsg;
        public Framedata lastframe;

        @Override
        public void onMessage ( String m )
        {
            lastmsg = m;
        }

        @Override
        public void onMessage ( ByteBuffer buf )
        {
            lastbuf = buf;
        }

        @Override
        public void onFragment ( Framedata fragment )
        {
            lastframe = fragment;
        }

    }

    public class MServer extends WebSocketServer
    {

        public WebSocket socket;

        public MServer ( int port ) throws UnknownHostException
        {
            super ( new InetSocketAddress ( port ) );
        }

        public MServer ( InetSocketAddress uri ) throws UnknownHostException
        {
            super ( uri );
        }

        @Override
        public void onClose ( WebSocket ws, int i, String s, boolean b )
        {
            System.out.println ( "MServer onClose!--------------------------" );
            Thread.dumpStack();
        }

        @Override
        public void onError ( WebSocket ws, Exception e )
        {
            System.out.println ( "MServer onError!--------------------------" );
            e.printStackTrace();
        }

        @Override
        public void onOpen ( WebSocket ws, ClientHandshake c )
        {
            System.out.println ( "Server onOpen" );
            socket = ws;
        }

        @Override
        public void onStart()
        {
            System.out.println ( "WServer onStart!----------------------" );
        }

        @Override
        public void onMessage ( WebSocket ws, String s )
        {
            ws.send ( s );
        }

        @Override
        public void onMessage ( WebSocket ws, ByteBuffer buf )
        {
            ws.send ( buf );

        }

        @Override
        public void onFragment ( WebSocket ws, Framedata fragment )
        {
            ws.sendFrame ( fragment );
        }

    }

    @Test
    public void testIt()
    {
        try
        {
            MServer ms = new MServer ( new InetSocketAddress ( "127.0.0.1" , 8923 ) );
            ms.start();

            MClient mc = new MClient ( new URI ( "ws://127.0.0.1:8923" ) );
            mc.connect();

            assertTrue ( mc.waitUntilOpen() );

            String msg = "This is a test!";
            mc.send ( msg );

            Thread.sleep ( 1000 );

            assertEquals ( msg, mc.lastmsg );

            msg = "Yet another message..";
            ms.socket.send ( msg );

            Thread.sleep ( 1000 );

            assertEquals ( msg, mc.lastmsg );

            mc.close();
            ms.stop();

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }

}
