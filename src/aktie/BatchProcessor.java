package aktie;

import aktie.data.CObj;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class BatchProcessor
{
    Logger log = Logger.getLogger ( "aktie" );

    private List<CObjProcessor> processors;

    public BatchProcessor()
    {
        processors = new LinkedList<CObjProcessor>();
    }

    public void addProcessor ( CObjProcessor p )
    {
        synchronized ( processors )
        {
            processors.add ( p );
        }

    }

    public void processObj ( Object o )
    {
        if ( o != null )
        {
            List<CObjProcessor> n = new LinkedList<CObjProcessor>();

            synchronized ( processors )
            {
                n.addAll ( processors );
            }

            boolean done = false;
            Iterator<CObjProcessor> i = n.iterator();

            while ( !done && i.hasNext() )
            {
                CObjProcessor p = i.next();

                StringBuilder sb = new StringBuilder();
                sb.append ( "BATCH PROCESS: " );
                sb.append ( p.getClass().getName() );
                sb.append ( " processing: " );

                if ( o instanceof CObj )
                {
                    CObj co = ( CObj ) o;
                    sb.append ( co.getType() );
                }

                else
                {
                    sb.append ( o );
                }

                log.info ( sb.toString() );

                done = p.processObj ( o );
            }

        }

    }

    public void processCObj ( CObj o )
    {
        if ( o != null )
        {
            List<CObjProcessor> n = new LinkedList<CObjProcessor>();

            synchronized ( processors )
            {
                n.addAll ( processors );
            }

            boolean done = false;
            Iterator<CObjProcessor> i = n.iterator();

            while ( !done && i.hasNext() )
            {
                CObjProcessor p = i.next();
                done = p.process ( o );
            }

        }

    }

}
