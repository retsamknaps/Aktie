package aktie.gui.table;

import java.io.IOException;

import aktie.gui.CObjElement;
import aktie.index.CObjList;

public class CObjListTableContentProviderTypeElement extends CObjListTableContentProvider<CObjElement>
{
    private CObjList list = null;

    @Override
    public CObjElement[] getElements ( Object a )
    {
        CObjElement failureReturn[] = new CObjElement[] {};

        if ( a != null && a instanceof CObjList )
        {
            list = ( CObjList ) a;
            CObjElement elements[] = new CObjElement[list.size()];

            for ( int i = 0; i < elements.length; i++ )
            {
                try
                {
                    elements[i] = new CObjElement ( list.get ( i ) );
                }

                catch ( IOException e )
                {
                    return failureReturn;
                }

            }

            return elements;
        }

        return failureReturn;
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
