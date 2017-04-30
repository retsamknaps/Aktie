package aktie.gui.table;

import org.eclipse.swt.widgets.TableColumn;

public class AktieTableColumn<L, E>
{

    // This class may not be subclassed, so it has to be wrapped
    private TableColumn tableColumn;

    public AktieTableColumn ( AktieTable<L, E> parent, int style )
    {
        this.tableColumn = new TableColumn ( parent.getTable(), style );
    }

    protected TableColumn getColumn()
    {
        return this.tableColumn;
    }

    protected void addSelectionListener ( AktieTableColumnSelectionListener<L, E> s )
    {
        this.tableColumn.addSelectionListener ( s );
    }

    public void setAlignment ( int alignment )
    {
        this.tableColumn.setAlignment ( alignment );
    }

    public void setText ( String name )
    {
        this.tableColumn.setText ( name );
    }

    public void setWidth ( int width )
    {
        this.tableColumn.setWidth ( width );
    }

}
