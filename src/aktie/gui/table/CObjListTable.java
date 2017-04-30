package aktie.gui.table;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import aktie.gui.CObjListGetter;
import aktie.index.CObjList;

public class CObjListTable<E extends CObjListGetter> extends AktieTable<CObjList, CObjListGetter>
{

    public static final int MAX_SORT_FIELDS = 2;

    private CObjListTableInputProvider inputProvider = null;;

    public CObjListTable ( Composite parent, int style )
    {
        super ( parent, style );
        getTableViewer().setSorter ( new CObjListTableViewerSorter() );
    }

    public void searchAndSort()
    {
        AktieTableViewerColumn<CObjList, CObjListGetter> selectedColumn = getTableViewer().getSortColumn();

        if ( selectedColumn != null )
        {
            selectedColumn.sort();
        }

    }

    public CObjListTableInputProvider getInputProvider()
    {
        return inputProvider;
    }

    public void setInputProvider ( CObjListTableInputProvider p )
    {
        inputProvider = p;
    }

    /**
        Adds a column to the table.
        @param name The name of the column displayed in the table header
        @param width The width of the column
        @param alignment The alignment of the content in the column's cells.  The argument should be one of SWT.LEFT, SWT.RIGHT or SWT.CENTER.
        @param labelProvider The cell label provider that knows what content to set for the table column's cells.
        @return The AktieTableViewerColumn<CObjListGetter> created by this add operation.
    */
    @Override
    public CObjListTableViewerColumn<E> addColumn ( String name, int width, int alignment, AktieTableCellLabelProvider<CObjListGetter> labelProvider )
    {
        CObjListTableColumn<E> tableColumn = new CObjListTableColumn<E> ( this, SWT.NONE );
        tableColumn.setText ( name );
        tableColumn.setWidth ( width );
        tableColumn.setAlignment ( alignment );
        return new CObjListTableViewerColumn<E> ( getTableViewer(), tableColumn, labelProvider, inputProvider, MAX_SORT_FIELDS );
    }

    /**
        Adds a column to the table which is not sorted based on the lucene index, but on the methods of the swt table.
        @param name The name of the column displayed in the table header
        @param width The width of the column
        @param labelProvider The cell label provider that knows what content to set for the table column's cells.
        @return The AktieTableViewerColumn<CObjListGetter> created by this add operation.
    */
    public CObjListTableViewerColumn<E> addNonIndexSortedColumn ( String name, int width, AktieTableCellLabelProvider<CObjListGetter> labelProvider )
    {
        return this.addNonIndexSortedColumn ( name, width, SWT.LEFT, labelProvider );
    }

    /**
        Adds a column to the table which is not sorted based on the lucene index, but on the methods of the swt table.
        @param name The name of the column displayed in the table header
        @param width The width of the column
        @param alignment The alignment of the content in the column's cells.  The argument should be one of SWT.LEFT, SWT.RIGHT or SWT.CENTER.
        @param labelProvider The cell label provider that knows what content to set for the table column's cells.
        @return The AktieTableViewerColumn<CObjListGetter> created by this add operation.
    */
    public CObjListTableViewerColumn<E> addNonIndexSortedColumn ( String name, int width, int alignment, AktieTableCellLabelProvider<CObjListGetter> labelProvider )
    {
        CObjListTableColumn<E> tableColumn = new CObjListTableColumn<E> ( this, SWT.NONE );
        tableColumn.setText ( name );
        tableColumn.setWidth ( width );
        tableColumn.setAlignment ( alignment );
        CObjListTableViewerColumn<E> tableViewerColumn = new CObjListTableViewerColumn<E> ( getTableViewer(), tableColumn, labelProvider, inputProvider, MAX_SORT_FIELDS );
        tableViewerColumn.setIndexSorted ( false );
        return tableViewerColumn;
    }

}
