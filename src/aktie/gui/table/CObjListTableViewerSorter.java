package aktie.gui.table;

import org.eclipse.jface.viewers.Viewer;

import aktie.gui.CObjListGetter;
import aktie.index.CObjList;

public class CObjListTableViewerSorter extends AktieTableViewerSorter<CObjList, CObjListGetter>
{
    @Override
    public int compare ( Viewer viewer, Object o1, Object o2 )
    {
        //System.out.println ( "CObjListTableViewerSorter.compare()" );

        AktieTableViewerColumn<CObjList, CObjListGetter> aktieColumn = getSortColumn();

        if ( aktieColumn instanceof CObjListTableViewerColumn )
        {
            //System.out.println ( "CObjListTableViewerSorter.compare(): column instanceof CObjListTableViewerColumn" );

            CObjListTableViewerColumn<CObjListGetter> coColumn = ( CObjListTableViewerColumn<CObjListGetter> ) aktieColumn;

            // In case that the column does not use lucene sort,
            // we rely on the super class method for sorting.
            if ( !coColumn.isIndexSorted() )
            {
                //System.out.println ( "CObjListTableViewerSorter.compare(): column not index sorted" );

                return super.compare ( viewer, o1, o2 );
            }

            return 0;
        }

        //System.out.println ( "CObjListTableViewerSorter.compare(): column not instanceof CObjListTableViewerColumn" );

        return 0;
    }

}
