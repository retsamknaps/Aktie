package aktie.gui.launchers;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aktie.data.Launcher;

public class LauncherContentProvider implements IStructuredContentProvider
{

    public LauncherContentProvider()
    {
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
    {
    }

    @Override
    public Object[] getElements ( Object a )
    {
        @SuppressWarnings ( "unchecked" )
        List<Launcher> l = ( List<Launcher> ) a;
        Object o[] = new Object[l.size()];
        int r = 0;

        for ( Launcher c : l )
        {
            o[r] = c;
            r++;
        }

        return o;
    }

}
