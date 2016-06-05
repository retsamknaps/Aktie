package aktie.gui;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Table;

import aktie.data.CObj;
import aktie.index.CObjList;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;

public class ShowHasFileDialog extends Dialog
{

    private SWTApp app;
    private TableViewer tableViewer;
    private Table table;
    private CObj fileo;
    private Label lblNodesHaveFile;
    private SetUserRankDialog usrRankDialog;

    /**
        Create the dialog.
        @param parentShell
    */
    public ShowHasFileDialog ( Shell parentShell, SetUserRankDialog d, SWTApp g )
    {
        super ( parentShell );
        usrRankDialog = d;
        app = g;
    }

    /**
        Create contents of the dialog.
        @param parent
    */
    @Override
    protected Control createDialogArea ( Composite parent )
    {
        Composite container = ( Composite ) super.createDialogArea ( parent );
        container.setLayout ( new GridLayout ( 1, false ) );

        lblNodesHaveFile = new Label ( container, SWT.NONE );
        lblNodesHaveFile.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        lblNodesHaveFile.setText ( "Nodes have file: <file>" );

        tableViewer = new TableViewer ( container, SWT.BORDER | SWT.FULL_SELECTION );
        tableViewer.setContentProvider ( new CObjListIdentPubContentProvider (
                                             app.getNode().getIndex(), CObj.CREATOR ) );
        table = tableViewer.getTable();
        table.setHeaderVisible ( true );
        table.setLinesVisible ( true );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

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

                    if ( selo instanceof CObjListIdentPubElement )
                    {
                        CObjListIdentPubElement ae = ( CObjListIdentPubElement ) selo;
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
        col0.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        doHasFileSearch ( fileo );

        return container;
    }

    public void open ( CObj f )
    {
        if ( doHasFileSearch ( f ) )
        {
            super.open();
        }

    }

    private boolean doHasFileSearch ( CObj f )
    {
        fileo = f;

        boolean showsf = false;

        if ( fileo != null )
        {
            String hdig = fileo.getString ( CObj.FILEDIGEST );
            String fdig = fileo.getString ( CObj.FRAGDIGEST );
            String comid = fileo.getString ( CObj.COMMUNITYID );

            showsf = ( hdig != null && fdig != null && comid != null );

            if ( !table.isDisposed() && !lblNodesHaveFile.isDisposed() && showsf )
            {
                String fname = fileo.getString ( CObj.NAME );
                lblNodesHaveFile.setText ( "Nodes have file: " + fname );

                CObjList ol = ( CObjList ) tableViewer.getInput();


                CObjList hlist =
                    app.getNode().getIndex().getHasFiles ( comid, hdig, fdig );
                tableViewer.setInput ( hlist );

                if ( ol != null )
                {
                    ol.close();
                }

            }

        }


        return showsf;

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

}
