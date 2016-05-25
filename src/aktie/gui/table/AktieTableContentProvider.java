package aktie.gui.table;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class AktieTableContentProvider<T> implements IStructuredContentProvider
{

    @Override
    public void dispose() {}

    @Override
    public void inputChanged ( Viewer arg0, Object arg1, Object arg2 ) {}

    @Override
    public T[] getElements ( Object a )
    {
        return null;
    }

}
