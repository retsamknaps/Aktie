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

    @Override
    public int hashCode()
    {
        return o.hashCode();
    }

    @Override
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
