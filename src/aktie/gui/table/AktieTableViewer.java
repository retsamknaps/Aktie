package aktie.gui.table;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;

public class AktieTableViewer<L, E> extends TableViewer
{

    private AktieTable<L, E> aktieTable;

    public AktieTableViewer ( AktieTable<L, E> table )
    {
        super ( table.getTable() );
        this.aktieTable = table;
    }

    public AktieTable<L, E> getAktieTable()
    {
        return aktieTable;
    }

    public AktieTableViewerColumn<L, E> getSortColumn()
    {
        return aktieTable.getTableViewer().getSorter().getSortColumn();
    }

    public void setSortColumn ( AktieTableViewerColumn<L, E> c, boolean reverse )
    {
        if ( c.isSortable() )
        {
            c.setSortReverse ( reverse );
            aktieTable.getTableViewer().getSorter().setSortColumn ( c );
        }

    }

    @SuppressWarnings ( "unchecked" )
    @Override
    public AktieTableViewerSorter<L, E> getSorter()
    {
        try
        {
            return ( AktieTableViewerSorter<L, E> ) super.getSorter();
        }

        catch ( ClassCastException e )
        {
            return null;
        }

    }

    public void setSorter ( AktieTableViewerSorter<L, E> sorter )
    {
        super.setSorter ( sorter );
    }

    @Override
    public IStructuredSelection getSelection()
    {
        return ( IStructuredSelection ) super.getSelection();
    }

}
