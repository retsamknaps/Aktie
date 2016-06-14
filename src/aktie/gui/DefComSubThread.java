package aktie.gui;

import java.util.List;

import aktie.Node;
import aktie.data.CObj;
import aktie.index.CObjList;

public class DefComSubThread implements Runnable
{

    private Node node;
    private List<CObj> seedCommunity;

    public DefComSubThread ( Node n, List<CObj> defsub )
    {
        node = n;
        seedCommunity = defsub;
        Thread t = new Thread ( this, "Default Community Subscription Thread" );
        t.start();
    }

    @Override
    public void run()
    {
        for ( CObj seedcom : seedCommunity )
        {
            String creatorid = seedcom.getString ( CObj.CREATOR );
            CObj cid = node.getIndex().getIdentity ( creatorid );

            while ( cid == null )
            {
                try
                {
                    Thread.sleep ( 2000 );
                }

                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }

                cid = node.getIndex().getIdentity ( creatorid );
            }

            //node.enqueue ( seedcom );
            cid = node.getIndex().getCommunity ( seedcom.getDig() );

            while ( cid == null )
            {
                try
                {
                    Thread.sleep ( 2000 );
                }

                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }

                cid = node.getIndex().getCommunity ( seedcom.getDig() );
            }

            CObjList clst = node.getIndex().getMyIdentities();

            while ( clst.size() == 0 )
            {
                clst.close();

                try
                {
                    Thread.sleep ( 2000 );
                }

                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }

                clst = node.getIndex().getMyIdentities();
            }

            try
            {
                CObj myid = clst.get ( 0 );
                clst.close();
                CObj sub = new CObj();
                sub.setType ( CObj.SUBSCRIPTION );
                sub.pushString ( CObj.CREATOR, myid.getId() );
                sub.pushString ( CObj.COMMUNITYID, cid.getDig() );
                sub.pushString ( CObj.SUBSCRIBED, "true" );
                node.enqueue ( sub );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

    }



}
