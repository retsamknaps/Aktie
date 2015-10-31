package aktie.gui;

import java.io.IOException;
import java.lang.ref.SoftReference;

import aktie.index.CObjList;
import aktie.data.CObj;

public class CObjListArrayElement implements CObjListGetter
{

    private int index;
    private CObjList list;
    private SoftReference<CObj> softCObj;

    public CObjListArrayElement ( CObjList l, int idx )
    {
        index = idx;
        list = l;
    }

    public CObj getCObj()
    {
        CObj r = null;

        if ( softCObj != null )
        {
            r = softCObj.get();
        }

        if ( r == null )
        {
            try
            {
                r = list.get ( index );
                softCObj = new SoftReference<CObj> ( r );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        return r;
    }

    public boolean equals ( Object o )
    {
        if ( o instanceof CObjListArrayElement )
        {
            CObjListArrayElement ce = ( CObjListArrayElement ) o;
            CObj oco = ce.getCObj();

            if ( oco != null )
            {
                return oco.equals ( getCObj() );
            }

        }

        return false;
    }

    public int hashCode()
    {
        CObj co = getCObj();

        if ( co != null )
        {
            return co.hashCode();
        }

        return 0;
    }

}
