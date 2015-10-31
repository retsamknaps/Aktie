package aktie.gui;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aktie.data.DirectoryShare;

public class DirectoryShareContentProvider implements IStructuredContentProvider
{

    @Override
    public void dispose()
    {

    }

    @Override
    public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
    {

    }

    @SuppressWarnings ( "unchecked" )
    @Override
    public Object[] getElements ( Object i )
    {
        if ( ! ( i instanceof List<?> ) )
        {
            throw new RuntimeException ( "You stupid" );
        }

        List<DirectoryShare> lst = ( List<DirectoryShare> ) i;
        Object[] o = new Object[lst.size()];
        int idx = 0;

        for ( DirectoryShare d : lst )
        {
            o[idx] = d;
            idx++;
        }

        return o;
    }

}
