package aktie.gui;

import org.apache.lucene.search.Sort;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDisplayName;
import aktie.gui.table.CObjListTableContentProviderTypeArrayElement;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.index.CObjList;

public class MembershipsTable extends CObjListTable<CObjListArrayElement>
{
    public MembershipsTable ( Composite composite, SWTApp app )
    {
        super ( composite, SWT.BORDER | SWT.FULL_SELECTION );

        setContentProvider ( new CObjListTableContentProviderTypeArrayElement() );

        setInputProvider ( new MembershipsTableInputProvider ( app ) );

        AktieTableViewerColumn<CObjList, CObjListGetter> sortColumn;

        sortColumn = this.addNonIndexSortedColumn ( "Memberships", 170, new CObjListTableCellLabelProviderTypeDisplayName ( true, null ) );
        getTableViewer().setSortColumn ( sortColumn, true );
    }

    private class MembershipsTableInputProvider extends CObjListTableInputProvider
    {
        private SWTApp app;

        public MembershipsTableInputProvider ( SWTApp app )
        {
            this.app = app;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            return app.getNode().getIndex().getMyValidMemberships ( sort );
        }

    }

}
