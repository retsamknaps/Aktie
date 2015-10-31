package aktie.gui;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aktie.index.CObjList;
import aktie.index.Index;

public class CObjListIdentPubContentProvider implements IStructuredContentProvider
{

    private CObjList list;
    private String idKey;
    private Index index;

    public CObjListIdentPubContentProvider ( Index i, String idk )
    {
        idKey = idk;
        index = i;
    }



    @Override
    public void dispose()
    {
        if ( list != null )
        {
            list.close();
        }

    }

    @Override
    public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
    {
    }

    @Override
    public Object[] getElements ( Object i )
    {
        if ( i instanceof CObjList && i != null )
        {
            list = ( CObjList ) i;
            Object r[] = new Object[list.size()];

            for ( int c = 0; c < r.length; c++ )
            {
                r[c] = new CObjListIdentPubElement ( index, list, idKey, c );
            }

            return r;
        }

        return new Object[] {};

    }


}
