package aktie.index;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import org.json.JSONObject;

import aktie.data.CObj;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class CObjListServer implements Closeable
{

    Logger log = Logger.getLogger ( "aktie" );

    private SocketChannel socket;
    private CObjList list;
    private Queue<ByteBuffer> writedata;
    private ByteBuffer readdata;

    public CObjListServer ( CObjList bl, SocketChannel s ) throws ClosedChannelException
    {
        list = bl;
        socket = s;
        readdata = ByteBuffer.allocate ( Integer.SIZE / Byte.SIZE );
        writedata = new LinkedList<ByteBuffer>();
    }

    public void doRegister ( Selector sel ) throws ClosedChannelException
    {
        SelectionKey selectKey = socket.register ( sel,
                                 SelectionKey.OP_READ | SelectionKey.OP_WRITE );
        selectKey.attach ( this );
        sendLength();
    }

    private void resetRead()
    {
        readdata.clear();
    }

    @Override
    public void close()
    {
        try
        {
            socket.close();
        }

        catch ( Exception e )
        {
        }

        list.close();
    }

    private void sendLength()
    {
        ByteBuffer buf = ByteBuffer.allocate ( Integer.SIZE / Byte.SIZE );
        buf.putInt ( list.size() );
        buf.clear();
        writedata.add ( buf );
    }

    private void sendObj ( int idx, SelectionKey key )
    {
        try
        {
            CObj obj = list.get ( idx );

            if ( obj != null )
            {
                JSONObject jo = obj.GETPRIVATEJSON();
                byte bl[] = jo.toString().getBytes ( "UTF8" );
                int blen = bl.length + ( 2 * ( Integer.SIZE / Byte.SIZE ) );
                ByteBuffer buf = ByteBuffer.allocate ( blen );
                buf.putInt ( idx );
                buf.putInt ( bl.length );
                buf.put ( bl );
                buf.clear();
                writedata.add ( buf );
                key.interestOps ( key.interestOps() | SelectionKey.OP_WRITE );
            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            close();
        }

    }

    public void ready ( SelectionKey key )
    {
        try
        {
            if ( ( key.readyOps() & SelectionKey.OP_READ ) != 0 )
            {
                log.info ( "read op ready " + readdata.remaining() );
                int len = socket.read ( readdata );

                if ( len < 0 )
                {
                    close();
                    return;
                }

                log.info ( "read op ready after read " + readdata.remaining() );

                if ( !readdata.hasRemaining() )
                {
                    readdata.flip();
                    int idx = readdata.getInt();
                    log.info ( "get index: " + idx );

                    if ( idx < 0 || idx >= list.size() )
                    {
                        close();
                        return;
                    }

                    else
                    {
                        sendObj ( idx, key );
                    }

                    resetRead();
                }

            }

            if ( ( key.readyOps() & SelectionKey.OP_WRITE ) != 0 )
            {
                log.info ( "write op ready" );
                ByteBuffer lst = writedata.peek();

                while ( lst != null )
                {
                    if ( lst.hasRemaining() )
                    {
                        log.info ( "bytes sent" );

                        Thread.sleep ( 100 );

                        socket.write ( lst );
                        break;
                    }

                    else
                    {
                        writedata.poll();
                        lst = writedata.peek();
                    }

                }

                if ( writedata.size() == 0 )
                {
                    key.interestOps (
                        key.interestOps() & ( ~SelectionKey.OP_WRITE ) );
                }

            }

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            close();
        }

    }

}
