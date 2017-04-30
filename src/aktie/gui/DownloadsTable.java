package aktie.gui;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import aktie.user.RequestFileHandler;
import aktie.data.CObj;
import aktie.data.RequestFile;
import aktie.gui.table.AktieTable;
import aktie.gui.table.AktieTableCellLabelProvider;
import aktie.gui.table.AktieTableContentProvider;
import aktie.gui.table.AktieTableViewerColumn;

public class DownloadsTable extends AktieTable<RequestFileHandler, RequestFile>
{
    private SWTApp app;

    public DownloadsTable ( Composite composite, SWTApp app )
    {
        super ( composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
        this.app = app;

        setContentProvider ( new DownloadsContentProvider() );

        AktieTableViewerColumn<RequestFileHandler, RequestFile> column;

        addColumn ( "", 2, new DownloadsColumnDummy() );

        column = addColumn ( "File", 200, SWT.RIGHT, new DownloadsColumnFileName() );
        getTableViewer().setSortColumn ( column, false );

        addColumn ( "Priority", 50, new DownloadsColumnPriority() );

        addColumn ( "Downloaded Parts", 150, new DownloadsColumnCompletedFrags() );

        addColumn ( "Total Parts", 100, new DownloadsColumnTotalFrags() );

        addColumn ( "File Size", 100, new DownloadsColumnFileSize() );

        addColumn ( "State", 200, new DownloadsColumnState() );

        addColumn ( "Requested by", 200, new DownloadsColumnLocalRequestId() );

        Menu menu = this.getMenu();

        // TODO: this could be done more elegant
        MenuItem changepriority = new MenuItem ( menu, SWT.NONE );
        changepriority.setText ( "Change Priority" );
        changepriority.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = DownloadsTable.this.getTableViewer().getSelection();
                DownloadsTable.this.app.getDownloadPriorityDialog().open ( sel );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem canceldl = new MenuItem ( menu, SWT.NONE );
        canceldl.setText ( "Cancel download" );
        canceldl.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = DownloadsTable.this.getTableViewer().getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();
                RequestFile rf = null;

                while ( i.hasNext() )
                {
                    rf = ( RequestFile ) i.next();

                    CObj cf = new CObj();
                    cf.setType ( CObj.USR_CANCEL_DL );
                    cf.pushString ( CObj.LOCALFILE, rf.getLocalFile() );
                    DownloadsTable.this.app.getNode().enqueue ( cf );
                }

                if ( rf != null )
                {
                    DownloadsTable.this.app.getUserCallback().update ( rf );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem whohas = new MenuItem ( menu, SWT.NONE );
        whohas.setText ( "Who has file" );
        whohas.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = DownloadsTable.this.getTableViewer().getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();
                RequestFile rf = null;

                if ( i.hasNext() )
                {
                    rf = ( RequestFile ) i.next();

                    if ( rf != null )
                    {
                        CObj hfs = new CObj();
                        hfs.setType ( CObj.HASFILE );
                        hfs.pushString ( CObj.FILEDIGEST, rf.getWholeDigest() );
                        hfs.pushString ( CObj.FRAGDIGEST, rf.getFragmentDigest() );
                        hfs.pushString ( CObj.COMMUNITYID, rf.getCommunityId() );
                        File f = new File ( rf.getLocalFile() );
                        hfs.pushString ( CObj.NAME, f.getName() );
                        DownloadsTable.this.app.getShowHasFileDialog().open ( hfs );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

    }

    private class DownloadsColumnFileName extends AktieTableCellLabelProvider<RequestFile>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                RequestFile rf = RequestFile.class.cast ( cell.getElement() );
                cell.setText ( rf.getLocalFile() );
            }

            catch ( ClassCastException e )
            {
                cell.setText ( "" );
            }

        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            if ( e1 == null || e2 == null )
            {
                return 0;
            }

            try
            {
                RequestFile element1 = RequestFile.class.cast ( e1 );
                RequestFile element2 = RequestFile.class.cast ( e2 );
                int comp = element1.getLocalFile().compareToIgnoreCase ( element2.getLocalFile() );

                if ( reverse && comp != 0 )
                {
                    return -comp;
                }

                return comp;
            }

            catch ( ClassCastException e )
            {
                System.out.println ( e.toString() );
                return 0;
            }

        }

    }

    private class DownloadsColumnDummy extends AktieTableCellLabelProvider<RequestFile>
    {
        @Override
        public void update ( ViewerCell cell )
        {
        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            return 0;
        }

    }

    private class DownloadsColumnPriority extends AktieTableCellLabelProvider<RequestFile>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                RequestFile rf = RequestFile.class.cast ( cell.getElement() );
                cell.setText ( Integer.toString ( rf.getPriority() ) );
            }

            catch ( ClassCastException e )
            {
                cell.setText ( "" );
            }

        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            if ( e1 == null || e2 == null )
            {
                return 0;
            }

            try
            {
                RequestFile element1 = RequestFile.class.cast ( e1 );
                RequestFile element2 = RequestFile.class.cast ( e2 );
                int comp = Integer.compare ( element1.getPriority(), element2.getPriority() );

                if ( reverse && comp != 0 )
                {
                    return -comp;
                }

                return comp;
            }

            catch ( ClassCastException e )
            {
                System.out.println ( e.toString() );
                return 0;
            }

        }

    }

    private class DownloadsColumnCompletedFrags extends AktieTableCellLabelProvider<RequestFile>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                RequestFile rf = RequestFile.class.cast ( cell.getElement() );
                cell.setText ( Long.toString ( rf.getFragsComplete() ) );
            }

            catch ( ClassCastException e )
            {
                cell.setText ( "" );
            }

        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            if ( e1 == null || e2 == null )
            {
                return 0;
            }

            try
            {
                RequestFile element1 = RequestFile.class.cast ( e1 );
                RequestFile element2 = RequestFile.class.cast ( e2 );
                int comp = Long.compare ( element1.getFragsComplete(), element2.getFragsComplete() );

                if ( reverse && comp != 0 )
                {
                    return -comp;
                }

                return comp;
            }

            catch ( ClassCastException e )
            {
                System.out.println ( e.toString() );
                return 0;
            }

        }

    }

    private class DownloadsColumnTotalFrags extends AktieTableCellLabelProvider<RequestFile>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                RequestFile rf = RequestFile.class.cast ( cell.getElement() );
                cell.setText ( Long.toString ( rf.getFragsTotal() ) );
            }

            catch ( ClassCastException e )
            {
                cell.setText ( "" );
            }

        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            if ( e1 == null || e2 == null )
            {
                return 0;
            }

            try
            {
                RequestFile element1 = RequestFile.class.cast ( e1 );
                RequestFile element2 = RequestFile.class.cast ( e2 );
                int comp = Long.compare ( element1.getFragsTotal(), element2.getFragsTotal() );

                if ( reverse && comp != 0 )
                {
                    return -comp;
                }

                return comp;
            }

            catch ( ClassCastException e )
            {
                System.out.println ( e.toString() );
                return 0;
            }

        }

    }

    private class DownloadsColumnFileSize extends AktieTableCellLabelProvider<RequestFile>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                RequestFile rf = RequestFile.class.cast ( cell.getElement() );
                cell.setText ( Long.toString ( rf.getFileSize() ) );
            }

            catch ( ClassCastException e )
            {
                cell.setText ( "" );
            }

        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            if ( e1 == null || e2 == null )
            {
                return 0;
            }

            try
            {
                RequestFile element1 = RequestFile.class.cast ( e1 );
                RequestFile element2 = RequestFile.class.cast ( e2 );
                int comp = Long.compare ( element1.getFileSize(), element2.getFileSize() );

                if ( reverse && comp != 0 )
                {
                    return -comp;
                }

                return comp;
            }

            catch ( ClassCastException e )
            {
                System.out.println ( e.toString() );
                return 0;
            }

        }

    }

    private class DownloadsColumnState extends AktieTableCellLabelProvider<RequestFile>
    {



        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                RequestFile rf = RequestFile.class.cast ( cell.getElement() );
                cell.setText ( rf.getStateText() );

            }

            catch ( ClassCastException e )
            {
                cell.setText ( "" );
            }

        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            if ( e1 == null || e2 == null )
            {
                return 0;
            }

            try
            {
                RequestFile element1 = RequestFile.class.cast ( e1 );
                RequestFile element2 = RequestFile.class.cast ( e2 );
                int comp = element1.getStateText().compareTo ( element2.getStateText() );

                if ( reverse && comp != 0 )
                {
                    return -comp;
                }

                return comp;
            }

            catch ( ClassCastException e )
            {
                System.out.println ( e.toString() );
                return 0;
            }

        }

    }


    private class DownloadsColumnLocalRequestId extends AktieTableCellLabelProvider<RequestFile>
    {
        // TODO: This information mapping should be prepared somewhere else in code
        // where local identities are handled
        // Could this be done in aktie.data.RequestFile?
        private Map<String, String> idMap = new HashMap<String, String>();

        public String getDisplayName ( RequestFile rf )
        {
            String rid = rf.getRequestId();
            String dn = this.idMap.get ( rid );

            if ( dn == null )
            {
                dn = "";

                if ( rid != null )
                {
                    CObj myid = DownloadsTable.this.app.getNode().getIndex().getMyIdentity ( rid );

                    if ( myid != null )
                    {
                        dn = myid.getDisplayName();
                    }

                    this.idMap.put ( rid, dn );
                }

            }

            return dn;
        }

        @Override
        public void update ( ViewerCell cell )
        {
            RequestFile rf = RequestFile.class.cast ( cell.getElement() );

            cell.setText ( this.getDisplayName ( rf ) );
        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            if ( e1 == null || e2 == null )
            {
                return 0;
            }

            try
            {
                RequestFile element1 = RequestFile.class.cast ( e1 );
                RequestFile element2 = RequestFile.class.cast ( e2 );
                int comp = this.getDisplayName ( element1 ).compareToIgnoreCase ( this.getDisplayName ( element2 ) );

                if ( reverse && comp != 0 )
                {
                    return -comp;
                }

                return comp;
            }

            catch ( ClassCastException e )
            {
                System.out.println ( e.toString() );
                return 0;
            }

        }

    }

    private class DownloadsContentProvider extends AktieTableContentProvider<RequestFileHandler, RequestFile>
    {
        @Override
        public RequestFile[] getElements ( Object a )
        {
            if ( a instanceof RequestFileHandler )
            {
                RequestFileHandler cc = ( RequestFileHandler ) a;
                List<RequestFile> rfl = cc.listRequestFilesAll ( RequestFile.COMPLETE, Integer.MAX_VALUE );
                RequestFile[] r = new RequestFile[rfl.size()];
                return rfl.toArray ( r );
            }

            return null;
        }

    }

}
