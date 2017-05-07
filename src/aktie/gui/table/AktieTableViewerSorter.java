package aktie.gui.table;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import aktie.utils.NeverHappen;

public class AktieTableViewerSorter<L, E> extends ViewerSorter
{

    protected AktieTableViewerColumn<L, E> sortColumn = null;

    public AktieTableViewerColumn<L, E> getSortColumn()
    {
        return sortColumn;
    }

    public void setSortColumn ( AktieTableViewerColumn<L, E> c )
    {
        sortColumn = c;
    }

    public void sort ( AktieTableViewerColumn<L, E> p )
    {
        if ( sortColumn == null )
        {
            sortColumn = p;
            return;
        }

        if ( sortColumn.equals ( p ) )
        {
            sortColumn.toogleSortReverse();
            return;
        }

        sortColumn = p;
    }

    @Override
    public int compare ( Viewer viewer, Object o1, Object o2 )
    {
        if ( sortColumn == null || !sortColumn.isSortable() )
        {
            return 0;
        }

        if ( sortColumn.isIndexSorted() )
        {
            NeverHappen.never();
        }

        return sortColumn.compare ( o1, o2, sortColumn.isSortedReverse() );
    }

}
