package aktie.gui;

import java.io.IOException;

import aktie.data.CObj;
import aktie.index.CObjList;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;

public class NewMemberDialog extends Dialog
{
    private Table table;
    private Text searchText;
    private Label lblGrantMembershipFor;
    private Label lblGrantedByYour;
    private Label lblAuthority;
    private Button btnSearch;
    private TableViewer tableViewer;
    private Button btnMember;
    private Button btnGrantingMember;
    private Button btnSuperMember;
    private SWTApp app;

    /**
        Create the dialog.
        @param parentShell
    */
    public NewMemberDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
    }

    private String selectedIdentity;
    private String selectedCommunity;

    private void selectIdentity ( String selid, String comid )
    {
        selectedIdentity = selid;
        selectedCommunity = comid;

        if ( app != null && selectedIdentity != null && selectedCommunity != null &&
                lblGrantMembershipFor != null && !lblGrantMembershipFor.isDisposed() )
        {
            Button b = getButton ( IDialogConstants.OK_ID );
            CObj community = app.getNode().getIndex().getCommunity ( selectedCommunity );
            CObj identity = app.getNode().getIndex().getIdentity ( selectedIdentity );
            CObjList memlist = app.getNode().getIndex().getMembership ( comid, selid );

            if ( community != null && identity != null && b != null )
            {
                Long maxauth = null;

                if ( community.getString ( CObj.CREATOR ).equals ( selectedIdentity ) )
                {
                    maxauth = CObj.MEMBER_SUPER;
                }

                else if ( memlist != null )
                {
                    for ( int c = 0; c < memlist.size(); c++ )
                    {
                        try
                        {
                            CObj m = memlist.get ( c );
                            Long a = m.getPrivateNumber ( CObj.AUTHORITY );

                            if ( a != null )
                            {
                                if ( maxauth == null )
                                {
                                    maxauth = a;
                                }

                                else if ( a > maxauth )
                                {
                                    maxauth = a;
                                }

                            }

                        }

                        catch ( IOException e )
                        {
                            e.printStackTrace();
                        }

                    }

                }

                String comname = community.getPrivateDisplayName();
                String identname = identity.getDisplayName();

                if ( comname != null )
                {
                    lblGrantMembershipFor.setText ( "Grant membership for community: " + comname );
                }

                if ( identname != null )
                {
                    lblGrantedByYour.setText ( "Granted by your id: " + identname );
                }

                if ( maxauth != null )
                {
                    if ( CObj.MEMBER_SIMPLE == ( long ) maxauth )
                    {
                        b.setEnabled ( false );
                        lblAuthority.setText ( "Not Authorized. :(" );
                        btnMember.setEnabled ( false );
                        btnMember.setSelection ( false );
                        btnGrantingMember.setEnabled ( false );
                        btnGrantingMember.setSelection ( false );
                        btnSuperMember.setEnabled ( false );
                        btnSuperMember.setSelection ( false );
                    }

                    if ( CObj.MEMBER_CAN_GRANT == ( long ) maxauth )
                    {
                        b.setEnabled ( true );
                        lblAuthority.setText ( "Can Grant" );
                        btnMember.setEnabled ( true );
                        btnMember.setSelection ( true );
                        btnGrantingMember.setEnabled ( false );
                        btnGrantingMember.setSelection ( false );
                        btnSuperMember.setEnabled ( false );
                        btnSuperMember.setSelection ( false );
                    }

                    if ( CObj.MEMBER_SUPER == ( long ) maxauth )
                    {
                        b.setEnabled ( true );
                        lblAuthority.setText ( "Super Member" );
                        btnMember.setEnabled ( true );
                        btnMember.setSelection ( true );
                        btnGrantingMember.setEnabled ( true );
                        btnGrantingMember.setSelection ( false );
                        btnSuperMember.setEnabled ( true );
                        btnSuperMember.setSelection ( false );
                    }

                }

            }

        }

    }

    public void open ( String id, String comid )
    {
        selectIdentity ( id, comid );
        defaultSearch();
        super.open();
    }

    /**
        Create contents of the dialog.
        @param parent
    */
    @Override
    protected Control createDialogArea ( Composite parent )
    {
        Composite container = ( Composite ) super.createDialogArea ( parent );
        container.setLayout ( new GridLayout ( 2, false ) );

        lblGrantMembershipFor = new Label ( container, SWT.NONE );
        lblGrantMembershipFor.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblGrantMembershipFor.setText ( "Grant membership for community: " );
        new Label ( container, SWT.NONE );

        lblGrantedByYour = new Label ( container, SWT.NONE );
        lblGrantedByYour.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblGrantedByYour.setText ( "Granted by your id:" );

        lblAuthority = new Label ( container, SWT.NONE );
        lblAuthority.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblAuthority.setText ( "Authority" );

        searchText = new Text ( container, SWT.BORDER );
        searchText.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        btnSearch = new Button ( container, SWT.NONE );
        btnSearch.setText ( "Search" );
        btnSearch.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                doSearch();

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        tableViewer = new TableViewer ( container, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION );
        table = tableViewer.getTable();
        table.setHeaderVisible ( true );
        table.setLinesVisible ( true );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        CObjListContentProvider cont = new CObjListContentProvider();
        tableViewer.setContentProvider ( cont );

        TableViewerColumn col0 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col0.getColumn().setText ( "Name" );
        col0.getColumn().setWidth ( 150 );
        col0.setLabelProvider ( new CObjListDisplayNameColumnLabelProvider() );
        col0.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.NAME );

                if ( ns.equals ( sortPostField1 ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField1 = ns;
                    sortPostReverse = false;
                    sortPostType1 = SortField.Type.STRING;
                }

                doSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn col1 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col1.getColumn().setText ( "Description" );
        col1.getColumn().setWidth ( 150 );
        col1.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.DESCRIPTION ) );
        col1.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.DESCRIPTION );

                if ( ns.equals ( sortPostField1 ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField1 = ns;
                    sortPostReverse = false;
                    sortPostType1 = SortField.Type.STRING;
                }

                doSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Composite composite = new Composite ( container, SWT.NONE );
        composite.setLayout ( new RowLayout ( SWT.VERTICAL ) );
        composite.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, false, false, 1, 1 ) );

        btnMember = new Button ( composite, SWT.RADIO );
        btnMember.setText ( "Member" );

        btnGrantingMember = new Button ( composite, SWT.RADIO );
        btnGrantingMember.setText ( "Granting Member" );

        btnSuperMember = new Button ( composite, SWT.RADIO );
        btnSuperMember.setText ( "Super Member" );

        defaultSearch();

        selectIdentity ( selectedIdentity, selectedCommunity );

        return container;
    }

    private String sortPostField1;
    private boolean sortPostReverse;
    private SortField.Type sortPostType1;

    private void doSearch()
    {
        String squery = searchText.getText();
        doSearch ( squery );
    }

    private void doSearch ( String str )
    {
        Sort s = new Sort();

        if ( sortPostField1 != null )
        {
            s.setSort ( new SortField ( sortPostField1, sortPostType1, sortPostReverse ) );

        }

        else
        {
            s.setSort ( new SortField ( CObj.docNumber ( CObj.CREATEDON ), SortField.Type.LONG, true ) );
        }

        CObjList oldl = ( CObjList ) tableViewer.getInput();

        CObjList l = app.getNode().getIndex().searchIdenties ( str, s );

        if ( tableViewer != null )
        {
            tableViewer.setInput ( l );
        }

        if ( oldl != null )
        {
            oldl.close();
        }

    }

    private void defaultSearch()
    {
        if ( app != null )
        {
            if ( tableViewer != null && !table.isDisposed() )
            {
                doSearch ( "" );
            }

        }

    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, "Grant Membership",
                       true );
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );

        selectIdentity ( selectedIdentity, selectedCommunity );
    }

    @Override
    protected void cancelPressed()
    {
        CObjList clst = ( CObjList ) tableViewer.getInput();

        if ( clst != null )
        {
            clst.close();
        }

        super.cancelPressed();
    }

    @Override
    protected void okPressed()
    {
        CObjList clst = ( CObjList ) tableViewer.getInput();

        if ( selectedIdentity != null && selectedCommunity != null )
        {
            long auth = 0;

            if ( btnSuperMember.getSelection() )
            {
                auth = CObj.MEMBER_SUPER;
            }

            if ( btnGrantingMember.getSelection() )
            {
                auth = CObj.MEMBER_CAN_GRANT;
            }

            if ( btnMember.getSelection() )
            {
                auth = CObj.MEMBER_SIMPLE;
            }

            int idx[] = table.getSelectionIndices();

            for ( int c = 0; c < idx.length; c++ )
            {
                try
                {
                    CObj ident = clst.get ( idx[c] );
                    CObj co = new CObj();
                    co.setType ( CObj.MEMBERSHIP );
                    co.pushString ( CObj.CREATOR, selectedIdentity );
                    co.pushPrivateNumber ( CObj.AUTHORITY, auth );
                    co.pushPrivate ( CObj.COMMUNITYID, selectedCommunity );
                    co.pushPrivate ( CObj.MEMBERID, ident.getId() );

                    if ( app != null )
                    {
                        app.getNode().enqueue ( co );
                    }

                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

        }

        if ( clst != null )
        {
            clst.close();
        }

        super.okPressed();
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 300 );
    }

    public Label getLblGrantMembershipFor()
    {
        return lblGrantMembershipFor;
    }

    public Label getLblGrantedByYour()
    {
        return lblGrantedByYour;
    }

    public Label getLblAuthority()
    {
        return lblAuthority;
    }

    public Text getSearchText()
    {
        return searchText;
    }

    public Button getBtnSearch()
    {
        return btnSearch;
    }

    public Table getTable()
    {
        return table;
    }

    public TableViewer getTableViewer()
    {
        return tableViewer;
    }

    public Button getBtnMember()
    {
        return btnMember;
    }

    public Button getBtnGrantingMember()
    {
        return btnGrantingMember;
    }

    public Button getBtnSuperMember()
    {
        return btnSuperMember;
    }

}
