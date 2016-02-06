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

}
