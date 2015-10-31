package aktie;

import java.util.List;

import aktie.data.CObj;

public abstract class GenericProcessor implements CObjProcessor
{

    @SuppressWarnings ( "rawtypes" )
    @Override
    public boolean processObj ( Object o )
    {
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
