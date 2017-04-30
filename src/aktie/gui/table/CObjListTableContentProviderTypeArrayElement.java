package aktie.gui.table;

import aktie.gui.CObjListArrayElement;
import aktie.index.CObjList;

public class CObjListTableContentProviderTypeArrayElement extends CObjListTableContentProvider<CObjListArrayElement>
{
    private CObjList list = null;

    @Override
    public CObjListArrayElement[] getElements ( Object a )
    {
        if ( a != null && a instanceof CObjList )
        {
            list = ( CObjList ) a;
            CObjListArrayElement elements[] = new CObjListArrayElement[list.size()];

            for ( int i = 0; i < elements.length; i++ )
            {
                elements[i] = new CObjListArrayElement ( list, i );

            }

            return elements;
        }

        return new CObjListArrayElement[] {};

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
