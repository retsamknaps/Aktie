package aktie.headless;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.Sort;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;


public class ClientReqIndexProcessor extends GenericProcessor
{

    private Index index;
    private ClientThread cliThread;

    public ClientReqIndexProcessor ( ClientThread t, Index i )
    {
        index = i;
        cliThread = t;
    }

    private void sendCObjList ( CObjList l )
    {
        for ( int c = 0; c < l.size(); c++ )
        {
            try
            {
                CObj o = l.get ( c );
                cliThread.enqueue ( o );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        l.close();
    }

    @Override
    public boolean process ( CObj b )
    {
        String tp = b.getType();

        if ( CObj.INDEX_QUERY.equals ( tp ) )
        {
            String iq = b.getString ( CObj.INDEX_Q_TYPE );

            if ( CObj.INDEX_Q_IDENT.equals ( iq ) )
            {
                CObjList l = index.getMyIdentities();
                sendCObjList ( l );
            }

            if ( CObj.INDEX_Q_MEMS.equals ( iq ) )
            {
                CObjList l = index.getMyMemberships ( ( Sort ) null );
                sendCObjList ( l );
            }

            if ( CObj.INDEX_Q_PUBCOM.equals ( iq ) )
            {
                CObjList l = index.getPublicCommunities();
                sendCObjList ( l );
            }

            if ( CObj.INDEX_Q_SUBS.equals ( iq ) )
            {
                CObjList l = index.getMySubscriptions();
                sendCObjList ( l );
            }

            return true;
        }

        if ( CObj.NODE_CMD.equals ( tp ) )
        {
            String ct = b.getString ( CObj.NODE_CMD_TYPE );

            if ( CObj.NODE_CMD_SHUTDOWN.equals ( ct ) )
            {
                cliThread.getMain().shutdown();
            }

            return true;
        }

        if ( CObj.QUERY.equals ( tp ) )
        {
            List<CObj> q = new LinkedList<CObj>();
            q.add ( b );
            CObjList l = index.searchPostsQuery ( q, null );
            sendCObjList ( l );
            //Allow query to pass through so that
            //they can be saved.
            return false;
        }

        return false;
    }

}
