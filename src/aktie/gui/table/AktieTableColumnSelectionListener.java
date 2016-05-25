package aktie.gui.table;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

public class AktieTableColumnSelectionListener<T> implements SelectionListener
{

    private AktieTableViewerColumn<T> tableViewerColumn;

    public AktieTableColumnSelectionListener ( AktieTableViewerColumn<T> tableViewerColumn )
    {
        this.tableViewerColumn = tableViewerColumn;
    }

    @Override
    public void widgetSelected ( SelectionEvent e )
    {
        this.tableViewerColumn.sort();
    }

    @Override
    public void widgetDefaultSelected ( SelectionEvent e )
    {

    }

}
