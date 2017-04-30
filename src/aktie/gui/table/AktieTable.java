package aktie.gui.table;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Composite;

public class AktieTable<L, E>
{

    // Table needs to be wrapped because it cannot be subclassed
    private Table table;
    private AktieTableViewer<L, E> tableViewer;

    public AktieTable ( Composite parent, int style )
    {
        table = new Table ( parent, style );
        table.setHeaderVisible ( true );
        table.setLinesVisible ( true );
        tableViewer = new AktieTableViewer<L, E> ( this );
        tableViewer.setSorter ( new AktieTableViewerSorter<L, E>() );
    }

    public void setLayoutData ( Object layoutData )
    {
        this.table.setLayoutData ( layoutData );
    }

    /**
        Returns the context menu of the wrapped table.
        If the table does not yet have a context menu,
        a new one is created before it is returned.
        @return The context menu of the wrapped table.
    */
    public Menu getMenu()
    {
        Menu menu = this.table.getMenu();

        if ( menu == null )
        {
            menu = new Menu ( this.table );
            this.table.setMenu ( menu );
        }

        return menu;
    }

    public AktieTableViewer<L, E> getTableViewer()
    {
        return tableViewer;
    }

    /**
        Due to the table is wrapped, this method should not be used for general purpose.
        Making direct use may break type conversion.
        @return The wrapped table.
    */
    public Table getTable()
    {
        return this.table;
    }

    public boolean isDisposed()
    {
        return this.table.isDisposed();
    }

    public int[] getSelectionIndices()
    {
        return this.table.getSelectionIndices();
    }

    public void setContentProvider ( AktieTableContentProvider<L, E> contentProvider )
    {
        this.tableViewer.setContentProvider ( contentProvider );
    }

    public void setMenu ( Menu menu )
    {
        this.table.setMenu ( menu );
    }

    /**
        Adds a column to the table.
        @param name The name of the column displayed in the table header
        @param width The width of the column
        @param p The cell label provider that knows what content to set for the table column's cells.
        @return The AktieTableViewerColumn<T> created by this add operation.
    */
    public AktieTableViewerColumn<L, E> addColumn ( String name, int width, AktieTableCellLabelProvider<E> p )
    {
        return this.addColumn ( name, width, SWT.LEFT, p );
    }

    /**
        Adds a column to the table.
        @param name The name of the column displayed in the table header
        @param width The width of the column
        @param alignment The alignment of the content in the column's cells.  The argument should be one of SWT.LEFT, SWT.RIGHT or SWT.CENTER.
        @param labelProvider The cell label provider that knows what content to set for the table column's cells.
        @return The AktieTableViewerColumn<T> created by this add operation.
    */
    public AktieTableViewerColumn<L, E> addColumn ( String name, int width, int alignment, AktieTableCellLabelProvider<E> labelProvider )
    {
        AktieTableColumn<L, E> tableColumn = new AktieTableColumn<L, E> ( this, SWT.NONE );
        tableColumn.setText ( name );
        tableColumn.setWidth ( width );
        tableColumn.setAlignment ( alignment );
        return new AktieTableViewerColumn<L, E> ( this.tableViewer, tableColumn, labelProvider );
    }

}
