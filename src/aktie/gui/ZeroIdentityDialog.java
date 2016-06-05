package aktie.gui;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import swing2swt.layout.BorderLayout;
import org.eclipse.swt.widgets.Table;

import aktie.data.CObj;
import aktie.index.CObjList;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;

public class ZeroIdentityDialog extends Dialog
{

    private SWTApp app;
    private Table table;
    private TableViewer tableViewer;
    private SetUserRankDialog usrRankDialog;

    /**
        Create the dialog.
        @param parentShell
    */
    public ZeroIdentityDialog ( Shell parentShell, SetUserRankDialog ur, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        usrRankDialog = ur;
        app = a;
    }

    private void updateTable()
    {
        if ( table != null && tableViewer != null && !table.isDisposed() )
        {
            CObjList oldcl = ( CObjList ) tableViewer.getInput();
            CObjList nlst = app.getNode().getIndex().getZeroIdentities();
            tableViewer.setInput ( nlst );

            if ( oldcl != null )
            {
                oldcl.close();
            }

        }

    }

    public int open()
    {
        updateTable();
        return super.open();
    }

    /**
        Create contents of the dialog.
        @param parent
    */
    @Override
    protected Control createDialogArea ( Composite parent )
    {
        Composite container = ( Composite ) super.createDialogArea ( parent );
        container.setLayout ( new BorderLayout ( 0, 0 ) );

        tableViewer = new TableViewer ( container, SWT.MULTI | SWT.BORDER |
                                        SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
        tableViewer.setContentProvider ( new CObjListContentProvider() );
        table = tableViewer.getTable();
        table.setHeaderVisible ( true );
        table.setLinesVisible ( true );
        table.setLayoutData ( BorderLayout.CENTER );

        Menu menu = new Menu ( table );
        table.setMenu ( menu );

        MenuItem mntmSetRank = new MenuItem ( menu, SWT.NONE );
        mntmSetRank.setText ( "Set Selected User(s) Rank" );
        mntmSetRank.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) tableViewer.getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                Set<CObj> users = new HashSet<CObj>();

                while ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof CObjListArrayElement )
                    {
                        CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                        CObj fr = ae.getCObj();
                        users.add ( fr );

                    }

                }

                if ( users.size() > 0 )
                {
                    usrRankDialog.open ( users );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn col0 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col0.getColumn().setText ( "Identity" );
        col0.getColumn().setWidth ( 300 );
        col0.setLabelProvider ( new CObjListDisplayNameColumnLabelProvider() );

        updateTable();

        return container;
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true );
        createButton ( parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false );
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 300 );
    }

    public Table getTable()
    {
        return table;
    }

    public TableViewer getTableViewer()
    {
        return tableViewer;
    }

}
