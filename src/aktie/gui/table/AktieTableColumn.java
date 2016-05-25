package aktie.gui.table;

import org.eclipse.swt.widgets.TableColumn;

public class AktieTableColumn<T>
{

    // This class may not be subclassed, so it has to be wrapped
    private TableColumn tableColumn;

    public AktieTableColumn ( AktieTable<T> parent, int style )
    {
        this.tableColumn = new TableColumn ( parent.getTable(), style );
    }

    protected TableColumn getColumn()
    {
        return this.tableColumn;
    }

    protected void addSelectionListener ( AktieTableColumnSelectionListener<T> s )
    {
        this.tableColumn.addSelectionListener ( s );
    }

    /*  public String getText() {
        return this.tableColumn.getText();
        }*/

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
