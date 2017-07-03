package aktie.gui;

import java.io.IOException;
import java.lang.ref.SoftReference;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.IndexInterface;

public class CObjListIdentityElement implements CObjListGetter
{

    private int listIndex;
    private CObjList list;
    private SoftReference<CObj> softCObj;
    private IndexInterface index;
    private String identityKey;
    private boolean privateAttribute;

    public CObjListIdentityElement ( IndexInterface index, CObjList list, int listIndex, String identityKey, boolean privateAttribute )
    {
        this.listIndex = listIndex;
        this.list = list;
        this.index = index;
        this.identityKey = identityKey;
        this.privateAttribute = privateAttribute;
    }

    @Override
    public CObj getCObj()
    {
        CObj identityElement = null;

        if ( softCObj != null )
        {
            identityElement = softCObj.get();
        }

        if ( identityElement == null )
        {
            try
            {
                CObj listElement = list.get ( listIndex );

                if ( listElement != null )
                {
                    String id;

                    if ( privateAttribute )
                    {
                        id = listElement.getPrivate ( identityKey );
                    }

                    else
                    {
                        id = listElement.getString ( identityKey );
                    }

                    if ( id != null )
                    {
                        identityElement = index.getIdentity ( id );
                        softCObj = new SoftReference<CObj> ( identityElement );
                    }

                }

            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        return identityElement;
    }


}
