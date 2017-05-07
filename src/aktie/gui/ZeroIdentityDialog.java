package aktie.gui;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.search.Sort;
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

import aktie.data.CObj;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDisplayName;
import aktie.gui.table.CObjListTableContentProviderTypeArrayElement;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.index.CObjList;

import org.eclipse.jface.viewers.IStructuredSelection;

public class ZeroIdentityDialog extends Dialog
{

    private SWTApp app;
    private ZeroIdentityTable table;
    private SetUserRankDialog usrRankDialog;

    /**
        Create the dialog.
        @param parentShell
    */
    public ZeroIdentityDialog ( Shell parentShell, SetUserRankDialog ur, SWTApp app )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        usrRankDialog = ur;
        this.app = app;
    }

    private void updateTable()
    {
        if ( table != null && !table.isDisposed() )
        {
            table.searchAndSort();
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

        table = new ZeroIdentityTable ( container, app );
        table.setLayoutData ( BorderLayout.CENTER );

        Menu menu = new Menu ( table.getTable() );
        table.setMenu ( menu );

        MenuItem mntmSetRank = new MenuItem ( menu, SWT.NONE );
        mntmSetRank.setText ( "Set Selected User(s) Rank" );
        mntmSetRank.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = table.getTableViewer().getSelection();

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

    private class ZeroIdentityTable extends CObjListTable<CObjListArrayElement>
    {
        public ZeroIdentityTable ( Composite composite, SWTApp app )
        {
            super ( composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );

            setContentProvider ( new CObjListTableContentProviderTypeArrayElement() );

            setInputProvider ( new ZeroIdentityTableInputProvider ( app ) );

            AktieTableViewerColumn<CObjList, CObjListGetter> column;

            column = addColumn ( "Identity", 300, new CObjListTableCellLabelProviderTypeDisplayName ( false, null ) );

            getTableViewer().setSortColumn ( column, false );
        }

    }

    private class ZeroIdentityTableInputProvider extends CObjListTableInputProvider
    {
        private SWTApp app;

        public ZeroIdentityTableInputProvider ( SWTApp app )
        {
            this.app = app;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            return app.getNode().getIndex().getZeroIdentities();
        }

    }

}
