package aktie.index;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aktie.Node;
import aktie.data.CObj;
import aktie.gui.GuiCallback;
import aktie.user.NewSubscriptionProcessor;

public class Upgrade0506 implements GuiCallback, Runnable
{

    private Node node;

    public Upgrade0506 ( Node n )
    {
        node = n;
    }

    @Override
    public void run()
    {
        //HH2Session s, Index i, SpamTool st, GuiCallback cb
        NewSubscriptionProcessor newsub = new NewSubscriptionProcessor (
            node.getSession(),
            node.getConnectionManager(),
            node.getIndex(),
            node.getSpamTool(),
            this
        );
        Map<String, Map<Long, Long>> cnums = new HashMap<String, Map<Long, Long>>();
        CObjList sublst = node.getIndex().getMySubscriptions();

        for ( int c = 0; c < sublst.size(); c++ )
        {
            try
            {
                CObj sb = sublst.get ( c );
                Long seq = sb.getNumber ( CObj.SEQNUM );
                String creator = sb.getString ( CObj.CREATOR );

                if ( seq != null && creator != null )
                {
                    Map<Long, Long> nums = cnums.get ( creator );

                    if ( nums == null )
                    {
                        nums = new HashMap<Long, Long>();
                        cnums.put ( creator, nums );
                    }

                    Long v = nums.get ( seq );

                    if ( v == null )
                    {
                        v = 0L;
                    }

                    long nv = v + 1;
                    nums.put ( seq, nv );
                }

            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        sublst.close();


        for ( Entry<String, Map<Long, Long>> enums : cnums.entrySet() )
        {
            Map<Long, Long> nums = enums.getValue();
            List<Long> seq = new LinkedList<Long>();
            seq.addAll ( nums.keySet() );
            long sl[] = new long[seq.size()];
            Iterator<Long> i = seq.iterator();

            for ( int c0 = 0; c0 < sl.length; c0++ )
            {
                sl[c0] = i.next();
            }

            Arrays.sort ( sl );

            for ( int c = 0; c < nums.size(); c++ )
            {
                long numsubs = nums.get ( sl[c] );
                System.out.println ( "Number of subs at seqnum: " + sl[c] + " is " + numsubs );

                if ( numsubs > 1 )
                {
                    CObjList cl = node.getIndex().getMySeqSubscriptions ( enums.getKey(), sl[c] );
                    System.out.println ( "Updating: " + cl.size() );

                    if ( cl.size() <= 1 )
                    {
                        System.out.println ( "WTF.  This should be more!" );
                    }

                    else
                    {
                        for ( int c1 = 0; c1 < cl.size(); c1++ )
                        {
                            try
                            {
                                CObj sb = cl.get ( c1 );
                                newsub.process ( sb );
                            }

                            catch ( Exception e )
                            {
                                e.printStackTrace();
                            }

                        }

                    }

                    cl.close();
                }

            }

        }

    }

    @Override
    public void update ( Object o )
    {

    }

}
