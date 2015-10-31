package aktie.gui;

import aktie.index.CObjList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class CObjListContentProvider implements IStructuredContentProvider
{

    private CObjList list;

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
                r[c] = new CObjListArrayElement ( list, c );
            }

            return r;
        }

        return new Object[] {};

    }

}
