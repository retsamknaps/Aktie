package aktie.net;

import static org.junit.Assert.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;
import org.junit.Test;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.CObjListClient;
import aktie.index.CObjListClientSelector;
import aktie.index.CObjListServer;
import aktie.index.CObjListServerSelector;
import aktie.index.Index;
import aktie.utils.FUtils;

public class CObjListClientServerTest
{

    Logger log = Logger.getLogger ( "aktie" );

    public static int SIZE = 10;
    public static int PORT = 9110;

    public static List<CObj> readList = new LinkedList<CObj>();

    class TestClientServerSocket implements Runnable
    {
        public void run()
        {
            try
            {
                FUtils.deleteDir ( new File ( "testcobjserverindex" ) );

                Index i = new Index();
                i.setIndexdir ( new File ( "testcobjserverindex" ) );
                i.init();

                for ( int ix = 0; ix < SIZE; ix++ )
                {
                    CObj co = new CObj();
                    co.setType ( CObj.IDENTITY );
                    co.setId ( Integer.toString ( ix ) );
                    co.setDig ( Integer.toString ( ix ) );
                    co.pushPrivate ( CObj.PRV_DISPLAY_NAME, Integer.toString ( ix ) );
                    i.index ( co );
                }

                CObjList lst = i.getAllCObj();

                CObjListServerSelector sel = new CObjListServerSelector();

                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.socket().bind ( new InetSocketAddress ( PORT ) );

                SocketChannel socketChannel =
                    serverSocketChannel.accept();
                log.info ( "Socket accepted" );

                CObjListServer srv = sel.getServer ( socketChannel, lst );

                Thread.sleep ( 4000L );

                srv.close();
                sel.close();
                serverSocketChannel.close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
                fail();
            }

        }

    }

    @Test
    public void testCObjServer()
    {
        try
        {
            TestClientServerSocket tcs = new TestClientServerSocket();
            Thread t = new Thread ( tcs );
            t.start();

            Thread.sleep ( 4000 );

            CObjListClientSelector ssel = new CObjListClientSelector();
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect ( new InetSocketAddress ( "127.0.0.1", PORT ) );
            socketChannel.configureBlocking ( false );

            CObjListClient c = ssel.getClient ( socketChannel );
            assertEquals ( SIZE, c.size() );

            for ( int ix = 0; ix < c.size(); ix++ )
            {
                CObj co = c.get ( ix );
                assertEquals ( CObj.IDENTITY, co.getType() );
                assertEquals ( Integer.toString ( ix ), co.getId() );
                assertEquals ( Integer.toString ( ix ), co.getDig() );
                assertEquals ( Integer.toString ( ix ), co.getPrivate ( CObj.PRV_DISPLAY_NAME ) );
            }

            for ( int ix = 0; ix < c.size(); ix++ )
            {
                CObj co = c.get ( ix );
                assertEquals ( CObj.IDENTITY, co.getType() );
                assertEquals ( Integer.toString ( ix ), co.getId() );
                assertEquals ( Integer.toString ( ix ), co.getDig() );
                assertEquals ( Integer.toString ( ix ), co.getPrivate ( CObj.PRV_DISPLAY_NAME ) );
            }

            c.close();
            ssel.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail ( "oops" );
        }

    }

}
