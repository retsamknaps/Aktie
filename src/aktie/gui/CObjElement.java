package aktie.gui;

import aktie.data.CObj;

public class CObjElement implements CObjListGetter
{

    private CObj o;

    public CObjElement ( CObj t )
    {
        o = t;
    }

    @Override
    public CObj getCObj()
    {
        return o;
    }

    public int hashCode()
    {
        return o.hashCode();
    }

    public boolean equals ( Object c )
    {
        if ( c == null ) { return false; }

        if ( c instanceof CObjElement )
        {
            CObjElement e = ( CObjElement ) c;
            CObj ec = e.getCObj();

            if ( ec != null )
            {
                return ec.equals ( o );
            }

        }

        return false;
    }

}
