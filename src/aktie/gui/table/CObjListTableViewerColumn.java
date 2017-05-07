package aktie.gui.table;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;

import aktie.gui.CObjListGetter;
import aktie.index.CObjList;

public class CObjListTableViewerColumn<E extends CObjListGetter> extends AktieTableViewerColumn<CObjList, CObjListGetter>
{

    private CObjListTableInputProvider inputProvider;

    private final SortField[] sortFieldBuffer;
    private int numSortFields = 0;

    public CObjListTableViewerColumn ( AktieTableViewer<CObjList, CObjListGetter> viewer, CObjListTableColumn<E> column, AktieTableCellLabelProvider<CObjListGetter> labelProvider, CObjListTableInputProvider inputProvider, int maxSortFields )
    {
        super ( viewer, column, labelProvider );
        this.inputProvider = inputProvider;

        setIndexSorted ( true );

        if ( maxSortFields < 1 )
        {
            maxSortFields = 1;
        }

        else if ( maxSortFields > 5 )
        {
            maxSortFields = 5;
        }

        sortFieldBuffer = new SortField[maxSortFields];
        clearSortFields();
    }

    public void clearSortFields()
    {
        for ( int i = 0; i < sortFieldBuffer.length; i++ )
        {
            sortFieldBuffer[i] = null;
        }

        numSortFields = 0;
    }

    private void addNewSortField ( SortField sortField )
    {
        for ( int i = sortFieldBuffer.length - 1; i > 0; i-- )
        {
            sortFieldBuffer[i] = sortFieldBuffer[i - 1];
        }

        sortFieldBuffer[0] = sortField;

        if ( numSortFields < sortFieldBuffer.length )
        {
            numSortFields++;
        }

    }

    private void setFirstSortField ( SortField sortField )
    {
        sortFieldBuffer[0] = sortField;

        if ( numSortFields < 1 )
        {
            numSortFields = 1;
        }

    }

    private SortField[] getActiveSortFields()
    {
        SortField[] sortFields = new SortField[numSortFields];

        for ( int i = 0; i < sortFields.length; i++ )
        {
            sortFields[i] = sortFieldBuffer[i];
        }

        return sortFields;
    }

    @Override
    public CObjListTableCellLabelProvider getLabelProvider()
    {
        return ( CObjListTableCellLabelProvider ) super.getLabelProvider();
    }

    @Override
    public void sort()
    {

        if ( !isSortable() )
        {
            return;
        }

        if ( inputProvider == null )
        {
            return;
        }

        // If this column is not sorted using the lucene index,
        // we call the sort method of the super class which will initiate
        // a swt table based sort.
        if ( !isIndexSorted() )
        {
            //Still call get input because search strings could have changed.
            getViewer().setInput ( inputProvider.getInput ( null ) );

            super.sort();

            return;
        }

        // Remember this previous sort column as getViewer().getSorter().sort ( this ) will set us as sort column
        AktieTableViewerColumn<CObjList, CObjListGetter> previousSortColumn = getViewer().getSortColumn();

        // invocation needed for reversing sort if required
        getViewer().getSorter().sort ( this );

        CObjListTableCellLabelProvider sortProvider = getLabelProvider();

        boolean reverse = getViewer().getSorter().getSortColumn().isSortedReverse();

        String sortString = sortProvider.getSortString();
        SortField.Type sortFieldType = sortProvider.getSortFieldType();

        SortField sortField;

        if ( sortFieldType != null && sortFieldType.equals ( SortField.Type.LONG ) )
        {
            sortField = new SortedNumericSortField ( sortString, sortFieldType, reverse );
        }

        else
        {
            sortField = new SortField ( sortString, sortFieldType, reverse );
        }

        if ( this.equals ( previousSortColumn ) )
        {
            setFirstSortField ( sortField );
        }

        else
        {
            addNewSortField ( sortField );
        }

        Sort sort = new Sort();
        sort.setSort ( getActiveSortFields() );

        getViewer().setInput ( inputProvider.getInput ( sort ) );

        getViewer().setSortColumn ( this, reverse );
    }

    @Override
    public int compare ( Object o1, Object o2, boolean reverse )
    {
        if ( isIndexSorted() )
        {
            return 0;
        }

        return getLabelProvider().compare ( o1, o2, reverse );
    }

}
