package aktie.gui.table;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class AktieTableViewerSorter<T> extends ViewerSorter
{

    private AktieTableViewerColumn<T> sortColumn = null;

    private boolean reverse = false;

    public boolean isReverse()
    {
        return this.reverse;
    }

    public void sort ( AktieTableViewerColumn<T> p )
    {
        if ( this.sortColumn == null )
        {
            this.sortColumn = p;
            this.reverse = false;
            return;
        }

        if ( this.sortColumn.equals ( p ) )
        {
            this.reverse = !this.reverse;
            return;
        }

        this.sortColumn = p;
        reverse = false;
    }

    @Override
    public int compare ( Viewer viewer, Object o1, Object o2 )
    {
        if ( this.sortColumn == null || !this.sortColumn.isSortable() )
        {
            return 0;
        }

        return this.sortColumn.compare ( o1, o2, this.reverse );
    }

}
