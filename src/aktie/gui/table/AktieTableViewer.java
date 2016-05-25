package aktie.gui.table;

import org.eclipse.jface.viewers.TableViewer;

public class AktieTableViewer<T> extends TableViewer
{

    private AktieTable<T> aktieTable;

    public AktieTableViewer ( AktieTable<T> table )
    {
        super ( table.getTable() );
        this.aktieTable = table;
    }

    public AktieTable<T> getAktieTable()
    {
        return this.aktieTable;
    }

    @SuppressWarnings ( "unchecked" )
    public AktieTableViewerSorter<T> getSorter()
    {
        try
        {
            return ( AktieTableViewerSorter<T> ) super.getSorter();
        }

        catch ( ClassCastException e )
        {
            return null;
        }

    }

    public void setSorter ( AktieTableViewerSorter<T> sorter )
    {
        super.setSorter ( sorter );
    }

}
