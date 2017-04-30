package aktie.gui.table;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

public class AktieTableColumnSelectionListener<L, E> implements SelectionListener
{
    private AktieTableViewerColumn<L, E> tableViewerColumn;

    public AktieTableColumnSelectionListener ( AktieTableViewerColumn<L, E> tableViewerColumn )
    {
        this.tableViewerColumn = tableViewerColumn;
    }

    @Override
    public void widgetSelected ( SelectionEvent e )
    {
        //System.out.println ( "AktieTableColumnSelectionListener.widgetSelected()" );

        tableViewerColumn.setPermitSortReverseToggling ( true );

        this.tableViewerColumn.sort();

        tableViewerColumn.setPermitSortReverseToggling ( false );
    }

    @Override
    public void widgetDefaultSelected ( SelectionEvent e )
    {

    }

}
