package aktie.gui.table;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public abstract class AktieTableContentProvider<L, E> implements IStructuredContentProvider
{

    @Override
    public void dispose() {}

    @Override
    public void inputChanged ( Viewer arg0, Object arg1, Object arg2 ) {}

    @Override
    public abstract E[] getElements ( Object a );

}
