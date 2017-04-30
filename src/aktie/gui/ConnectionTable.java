package aktie.gui;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import aktie.gui.SWTApp.ConnectionCallback;
import aktie.gui.table.AktieTable;
import aktie.gui.table.AktieTableCellLabelProvider;
import aktie.gui.table.AktieTableContentProvider;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.net.ConnectionElement;

public class ConnectionTable extends AktieTable<ConnectionCallback, ConnectionElement>
{

    private SWTApp app;

    public ConnectionTable ( Composite composite, SWTApp app )
    {
        super ( composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
        this.app = app;

        setContentProvider ( new ConnectionContentProvider() );

        AktieTableViewerColumn<ConnectionCallback, ConnectionElement> sortColumn;

        sortColumn = addColumn ( "Local", 200, new ConnectionColumnLocalDest() );
        addColumn ( "Remote", 200, new ConnectionColumnRemoteDest() );
        addColumn ( "Upload (B)", 100, new ConnectionColumnUpload() );
        addColumn ( "Download (B)", 100, new ConnectionColumnDownload() );
        addColumn ( "Time (s)", 90, new ConnectionColumnTime() );
        addColumn ( "Last sent", 100, new ConnectionColumnLastSent() );
        addColumn ( "Last read", 100, new ConnectionColumnLastRead() );
        addColumn ( "Pending", 100, new ConnectionColumnPending() );
        addColumn ( "Mode", 100, new ConnectionColumnMode() );
        addColumn ( "Down file", 200, SWT.RIGHT, new ConnectionColumnFileDown() );
        addColumn ( "Up file", 200, SWT.RIGHT, new ConnectionColumnFileUp() );

        getTableViewer().setSortColumn ( sortColumn, false );

        getTableViewer().addSelectionChangedListener ( new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged ( SelectionChangedEvent event )
            {
                IStructuredSelection selection = ConnectionTable.this.getTableViewer().getStructuredSelection();
                Object firstElement = selection.getFirstElement();

                if ( firstElement != null )
                {
                    ConnectionElement ct = ( ConnectionElement ) firstElement;

                    if ( ct.fulllocalid != null && ct.fullremoteid != null )
                    {
                        ConnectionTable.this.app.getConnectionDialog().open ( ct.fulllocalid, ct.fullremoteid );
                    }

                }

            }

        } );

    }


    private class ConnectionColumnFileUp extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                String text = element.upFile != null ? element.upFile : "";
                cell.setText ( text );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = element1.upFile.compareToIgnoreCase ( element2.upFile );

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

    private class ConnectionColumnFileDown extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                String text = element.downFile != null ? element.downFile : "";
                cell.setText ( text );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = element1.downFile.compareToIgnoreCase ( element2.downFile );

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

    private class ConnectionColumnRemoteDest extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                String text = element.remoteDest != null ? element.remoteDest : "";
                cell.setText ( text );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = element1.remoteDest.compareToIgnoreCase ( element2.remoteDest );

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

    private class ConnectionColumnLocalDest extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                String text = element.localId != null ? element.localId : "";
                cell.setText ( text );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = element1.localId.compareToIgnoreCase ( element2.localId );

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

    private class ConnectionColumnLastSent extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                cell.setText ( element.lastSent );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = element1.lastSent.compareToIgnoreCase ( element2.lastSent );

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

    private class ConnectionColumnLastRead extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                cell.setText ( element.lastRead );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = element1.lastRead.compareToIgnoreCase ( element2.lastRead );

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

    private class ConnectionColumnMode extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                cell.setText ( element.mode );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = element1.mode.compareToIgnoreCase ( element2.mode );

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

    private class ConnectionColumnPending extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                cell.setText ( Long.toString ( element.pending ) );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = Long.compare ( element1.pending, element2.pending );

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

    private class ConnectionColumnDownload extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                cell.setText ( Long.toString ( element.download ) );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = Long.compare ( element1.download, element2.download );

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

    private class ConnectionColumnTime extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                cell.setText ( Long.toString ( element.time ) );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = Long.compare ( element1.time, element2.time );

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

    private class ConnectionColumnUpload extends AktieTableCellLabelProvider<ConnectionElement>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                ConnectionElement element = ConnectionElement.class.cast ( cell.getElement() );
                cell.setText ( Long.toString ( element.upload ) );
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
                ConnectionElement element1 = ConnectionElement.class.cast ( e1 );
                ConnectionElement element2 = ConnectionElement.class.cast ( e2 );
                int comp = Long.compare ( element1.upload, element2.upload );

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

    private class ConnectionContentProvider extends AktieTableContentProvider<ConnectionCallback, ConnectionElement>
    {
        @Override
        public ConnectionElement[] getElements ( Object a )
        {
            if ( a instanceof ConnectionCallback )
            {
                ConnectionCallback cc = ( ConnectionCallback ) a;
                return cc.getElements();
            }

            return null;
        }

    }

}
