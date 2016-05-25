package aktie.gui.table;

import org.eclipse.jface.viewers.TableViewerColumn;

public class AktieTableViewerColumn<T>
{

    // Must be wrapped due to class TableViewerColumn being final
    // It is not accessible from outside in order to avoid a wrong subclass type
    private TableViewerColumn tableViewerColumn;

    // A reference to the label provider for class internal use
    private AktieTableCellLabelProvider<T> labelProvider = null;

    private boolean sortable = true;

    public AktieTableViewerColumn ( AktieTableViewer<T> viewer, AktieTableColumn<T> column, AktieTableCellLabelProvider<T> labelProvider )
    {
        this.tableViewerColumn = new TableViewerColumn ( viewer, column.getColumn() );
        this.tableViewerColumn.setLabelProvider ( labelProvider );
        this.labelProvider = labelProvider;
        column.addSelectionListener ( new AktieTableColumnSelectionListener<T> ( this ) );
    }

    @SuppressWarnings ( "unchecked" )
    public AktieTableViewer<T> getViewer()
    {
        return ( AktieTableViewer<T> ) this.tableViewerColumn.getViewer();
    }

    public boolean isSortable()
    {
        return this.sortable;
    }

    public void setSortable ( boolean b )
    {
        this.sortable = b;
    }

    public void sort()
    {
        this.getViewer().getSorter().sort ( this );
        this.getViewer().refresh();
    }

    public int compare ( Object o1, Object o2, boolean reverse )
    {
        if ( this.labelProvider != null )
        {
            return this.labelProvider.compare ( o1, o2, reverse );
        }

        return 0;
    }

}
