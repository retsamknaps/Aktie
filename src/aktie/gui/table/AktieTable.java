package aktie.gui.table;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Composite;

public class AktieTable<T>
{

    // Table needs to be wrapped because it cannot be subclassed
    private Table table;
    private AktieTableViewer<T> tableViewer;

    public AktieTable ( Composite parent, int style )
    {
        this.table = new Table ( parent, style );
        this.table.setHeaderVisible ( true );
        this.table.setLinesVisible ( true );
        this.tableViewer = new AktieTableViewer<T> ( this );
        this.tableViewer.setSorter ( new AktieTableViewerSorter<T>() );
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

    public AktieTableViewer<T> getTableViewer()
    {
        return this.tableViewer;
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

    public void setContentProvider ( AktieTableContentProvider<T> contentProvider )
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
    */
    public void addColumn ( String name, int width, AktieTableCellLabelProvider<T> p )
    {
        this.addColumn ( name, width, SWT.LEFT, p );
    }

    /**
        Adds a column to the table.
        @param name The name of the column displayed in the table header
        @param width The width of the column
        @param alignment The alignment of the content in the column's cells.  The argument should be one of SWT.LEFT, SWT.RIGHT or SWT.CENTER.
        @param labelProvider The cell label provider that knows what content to set for the table column's cells.
    */
    public void addColumn ( String name, int width, int alignment, AktieTableCellLabelProvider<T> labelProvider )
    {
        AktieTableColumn<T> tableColumn = new AktieTableColumn<T> ( this, SWT.NONE );
        tableColumn.setText ( name );
        tableColumn.setWidth ( width );
        tableColumn.setAlignment ( alignment );
        new AktieTableViewerColumn<T> ( this.tableViewer, tableColumn, labelProvider );
    }

}
