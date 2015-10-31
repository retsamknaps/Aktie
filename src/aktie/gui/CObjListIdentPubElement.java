package aktie.gui;


import java.io.IOException;
import java.lang.ref.SoftReference;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class CObjListIdentPubElement implements CObjListGetter
{

    private int idx;
    private CObjList list;
    private SoftReference<CObj> softCObj;
    private Index index;
    private String idKey;

    public CObjListIdentPubElement ( Index i, CObjList l, String ik, int ix )
    {
        idx = ix;
        list = l;
        index = i;
        idKey = ik;
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
                CObj rr = list.get ( idx );

                if ( rr != null )
                {
                    String pk = rr.getString ( idKey );

                    if ( pk != null )
                    {
                        r = index.getIdentity ( pk );
                        softCObj = new SoftReference<CObj> ( r );
                    }

                }

            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        return r;
    }


}
