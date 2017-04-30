package aktie.gui.table;

import aktie.gui.CObjListIdentityElement;
import aktie.index.Index;

import aktie.index.CObjList;

public class CObjListTableContentProviderTypeIdentityElement extends CObjListTableContentProvider<CObjListIdentityElement>
{
    private CObjList list = null;

    private Index index;
    private String identityKey;
    private boolean privateAttribute;

    public CObjListTableContentProviderTypeIdentityElement ( Index index, String identityKey, boolean privateAttribute )
    {
        this.index = index;
        this.identityKey = identityKey;
        this.privateAttribute = privateAttribute;
    }

    @Override
    public CObjListIdentityElement[] getElements ( Object a )
    {
        if ( a != null && a instanceof CObjList )
        {
            list = ( CObjList ) a;
            CObjListIdentityElement r[] = new CObjListIdentityElement[list.size()];

            for ( int i = 0; i < r.length; i++ )
            {
                r[i] = new CObjListIdentityElement ( index, list, i, identityKey, privateAttribute );
            }

            return r;
        }

        return new CObjListIdentityElement[] {};

    }

    @Override
    public void dispose()
    {
        if ( list != null )
        {
            list.close();
        }

    }

}
