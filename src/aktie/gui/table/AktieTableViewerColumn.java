package aktie.gui.table;

import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewerColumn;

public class AktieTableViewerColumn<L, E>
{

    // Must be wrapped due to class TableViewerColumn being final
    // It is not accessible from outside in order to avoid a wrong subclass type
    private TableViewerColumn tableViewerColumn;

    // A reference to the label provider for class internal use
    private AktieTableCellLabelProvider<E> labelProvider = null;

    private boolean sortable = true;

    private boolean reverse = false;

    private boolean indexSorted = false;

    private boolean permitReverseToggling = false;

    public AktieTableViewerColumn ( AktieTableViewer<L, E> viewer, AktieTableColumn<L, E> column, AktieTableCellLabelProvider<E> labelProvider )
    {
        this.tableViewerColumn = new TableViewerColumn ( viewer, column.getColumn() );
        this.tableViewerColumn.setLabelProvider ( labelProvider );
        this.labelProvider = labelProvider;
        column.addSelectionListener ( new AktieTableColumnSelectionListener<L, E> ( this ) );
    }

    public AktieTableCellLabelProvider<E> getLabelProvider()
    {
        return this.labelProvider;
    }

    @SuppressWarnings ( "unchecked" )
    public AktieTableViewer<L, E> getViewer()
    {
        return ( AktieTableViewer<L, E> ) tableViewerColumn.getViewer();
    }

    public void setMoveable ( boolean b )
    {
        tableViewerColumn.getColumn().setMoveable ( b );
    }

    public void setEditingSupport ( EditingSupport editingSupport )
    {
        tableViewerColumn.setEditingSupport ( editingSupport );
    }

    /**
        Whether this column is sorted in reversed order.
        @return If true, this column is sorted in reverse order.
    */
    public boolean isSortedReverse()
    {
        return reverse;
    }

    /**
        Toggle reverse order of sorting.
        This method only has an effect if toggling is permitted
        by setPermitSortReverseToggling
    */
    public void toogleSortReverse()
    {
        if ( permitReverseToggling )
        {
            reverse = !reverse;
        }

    }

    public void setSortReverse ( boolean b )
    {
        reverse = b;
    }

    /**
        Permit toggling of sort reverse. This method is used by AktieTableColumnSelectionListener to enable sort reverse toggling.
        In case that we do an automated sort, e.g. when applying sort filters, we usually would not want the reverse toggling to be on.
        @param b If true, sort reverse toggling is enabled. If false, it is disabled, i.e. toggleSortReverse() has no effect.
    */
    public void setPermitSortReverseToggling ( boolean b )
    {
        permitReverseToggling = b;
    }

    public boolean isSortable()
    {
        return sortable;
    }

    public void setSortable ( boolean b )
    {
        sortable = b;
    }

    /**
        Whether this table viewer column is sorted relying on the lucene index or
        on the sort method of the swt table.
        AktieTableViewerColumn is always sorted based on the swt table, no matter what this method returns.
        This method is only
        @return true if sorted by the lucene index, false if sorted by the swt table.
    */
    public boolean isIndexSorted()
    {
        return indexSorted;
    }

    /**
        Set whether this table viewer column is sorted relying on the lucene index or
        on the sort method of the swt table.
        AktieTableViewerColumn is always sorted based on the swt table.
        @param b  If set to true (default), the column will be sorted based on the lucene index
                 in case that it is an instance of the subclass CObjListTableViewerColumn
    */
    public void setIndexSorted ( boolean b )
    {
        indexSorted = b;
    }

    public void sort()
    {
        //System.out.println ( "AktieTableViewerColumn.sort()" );
        if ( sortable )
        {
            getViewer().setSortColumn ( this, reverse );
            getViewer().getSorter().sort ( this );
            getViewer().refresh();
        }

    }

    public int compare ( Object o1, Object o2, boolean reverse )
    {
        if ( labelProvider != null )
        {
            return labelProvider.compare ( o1, o2, reverse );
        }

        return 0;
    }

}
