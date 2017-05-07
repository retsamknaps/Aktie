package aktie.gui.table;

import org.eclipse.jface.viewers.Viewer;

import aktie.gui.CObjListGetter;
import aktie.index.CObjList;

public class CObjListTableViewerSorter extends AktieTableViewerSorter<CObjList, CObjListGetter>
{

    @Override
    public int compare ( Viewer viewer, Object o1, Object o2 )
    {
        AktieTableViewerColumn<CObjList, CObjListGetter> aktieColumn = getSortColumn();

        if ( aktieColumn instanceof CObjListTableViewerColumn )
        {

            CObjListTableViewerColumn<CObjListGetter> coColumn = ( CObjListTableViewerColumn<CObjListGetter> ) aktieColumn;

            if ( !coColumn.isIndexSorted() )
            {
                return super.compare ( viewer, o1, o2 );
            }

            return 0;
        }

        return 0;
    }

}
