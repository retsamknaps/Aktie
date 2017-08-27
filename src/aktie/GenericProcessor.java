package aktie;

import java.util.List;

import aktie.data.CObj;

public abstract class GenericProcessor implements CObjProcessor
{

    @SuppressWarnings ( "rawtypes" )
    @Override
    public boolean processObj ( Object o )
    {
        if ( ContextObject.class.isAssignableFrom ( o.getClass() ) )
        {
            ContextObject co = ( ContextObject ) o;
            setContext ( co.context );

            if ( processObj ( co.obj ) )
            {
                co.notifyProcessed();
            }

        }

        if ( o instanceof CObj )
        {
            return process ( ( CObj ) o );
        }

        else if ( o instanceof List )
        {
            boolean prc = true;
            List ol = ( List ) o;

            for ( Object obj : ol )
            {
                if ( !processObj ( obj ) ) { prc = false; }

            }

            return prc;
        }

        return false;
    }

}
