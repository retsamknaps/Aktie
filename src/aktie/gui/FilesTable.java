package aktie.gui;

import org.apache.lucene.search.Sort;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import aktie.data.CObj;
import aktie.data.DirectoryShare;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDate;
import aktie.gui.table.CObjListTableCellLabelProviderTypeHex;
import aktie.gui.table.CObjListTableCellLabelProviderTypeLong;
import aktie.gui.table.CObjListTableCellLabelProviderTypeString;
import aktie.gui.table.CObjListTableContentProviderTypeArrayElement;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.index.CObjList;

public class FilesTable extends CObjListTable<CObjListArrayElement>
{

    public FilesTable ( Composite composite, SWTApp app )
    {
        super ( composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI );

        setContentProvider ( new CObjListTableContentProviderTypeArrayElement() );

        setInputProvider ( new FilesTableInputProvider ( app ) );

        AktieTableViewerColumn<CObjList, CObjListGetter> sortColumn;

        addColumn ( "File", 200, new CObjListTableCellLabelProviderTypeString ( CObj.NAME, false, null ) );
        addColumn ( "Size", 100, new CObjListTableCellLabelProviderTypeLong ( CObj.FILESIZE, false, null ) );
        addColumn ( "SHA256", 100, new CObjListTableCellLabelProviderTypeHex ( CObj.FILEDIGEST, false, null ) );
        addColumn ( "Status", 70, new CObjListTableCellLabelProviderTypeString ( CObj.STATUS, false, null ) );
        addColumn ( "Local File", 100, new CObjListTableCellLabelProviderTypeString ( CObj.LOCALFILE, false, null ) );
        addColumn ( "Number Has", 50, new CObjListTableCellLabelProviderTypeLong ( CObj.NUMBER_HAS, false, null ) );
        addColumn ( "First Seen", 120, new CObjListTableCellLabelProviderTypeDate ( CObj.CREATEDON, false, null ) );
        sortColumn = addColumn ( "Last Seen", 120, new CObjListTableCellLabelProviderTypeDate ( CObj.LASTUPDATE, false, null ) );

        getTableViewer().setSortColumn ( sortColumn, true );
    }

    private class FilesTableInputProvider extends CObjListTableInputProvider
    {
        private final SWTApp app;

        public FilesTableInputProvider ( SWTApp app )
        {
            this.app = app;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            CObj selectedCommunity = app.getSelectedCommunity();

            CObjList list = null;

            if ( selectedCommunity != null )
            {

                String directoryShare = null;

                DirectoryShare selectedDirectoryShare = app.getSelecedDirectoryShare();

                if ( selectedDirectoryShare != null && selectedDirectoryShare.getId() != -1 )
                {
                    directoryShare = selectedDirectoryShare.getShareName();
                }

                String search = app.getFilesSearchText().getText();

                return app.getNode().getIndex().searchFiles ( selectedCommunity.getDig(), directoryShare, search, sort );

            }

            return list;
        }

    }

}
