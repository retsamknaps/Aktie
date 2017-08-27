package aktie;

import aktie.data.CObj;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
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

                if ( Level.INFO.equals ( log.getLevel() ) )
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append ( "BATCH PROCESS: " );
                    sb.append ( p.getClass().getName() );
                    sb.append ( " processing!>!*: " );

                    if ( o instanceof CObj )
                    {
                        CObj co = ( CObj ) o;
                        sb.append ( co.getType() );
                    }

                    else if ( ContextObject.class.isAssignableFrom ( o.getClass() ) )
                    {
                        ContextObject co = ( ContextObject ) o;
                        sb.append ( " [" );
                        sb.append ( co.obj );
                        sb.append ( "] " );
                    }

                    else
                    {
                        sb.append ( "FUCK! *< " );
                        sb.append ( o );
                        sb.append ( ">* FUCK! " );
                    }

                    log.info ( sb.toString() );
                }

                done = p.processObj ( o );
            }

            if ( !done && o instanceof ContextObject )
            {
                ContextObject co = ( ContextObject ) o;
                co.notifyProcessed();
            }

        }

    }

    public void processCObj ( CObj o )
    {
        processObj ( o );
    }

}
