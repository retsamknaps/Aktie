package aktie.gui.table;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

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
        //System.out.println ( "AktieTableViewerSorter.compare()" );

        if ( sortColumn == null || !sortColumn.isSortable() )
        {
            //System.out.println ( "AktieTableViewerSorter.compare(): sort column is null or not sortable" );
            return 0;
        }

        //System.out.println ( "AktieTableViewerSorter.compare(): sort column not null, comparing" );

        return sortColumn.compare ( o1, o2, sortColumn.isSortedReverse() );
    }

}
