package aktie.gui;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.layout.GridData;

import aktie.data.CObj;
import aktie.index.CObjList;

public class ShowMembersDialog extends Dialog
{
    private Table memberTable;
    private TableViewer memberTableViewer;
    private Label lblMembersOfCommunity;
    private SWTApp app;

    /**
        Create the dialog.
        @param parentShell
    */
    public ShowMembersDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
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

        lblMembersOfCommunity = new Label ( container, SWT.NONE );
        lblMembersOfCommunity.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        lblMembersOfCommunity.setText ( "Members of Community: " );

        memberTableViewer = new TableViewer ( container, SWT.BORDER |
                                              SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL );
        memberTableViewer.setContentProvider ( new CObjListIdentPrivContentProvider (
                app.getNode().getIndex(), CObj.MEMBERID ) );
        memberTable = memberTableViewer.getTable();
        memberTable.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
        memberTable.setHeaderVisible ( true );
        memberTable.setLinesVisible ( true );

        TableViewerColumn col0 = new TableViewerColumn ( memberTableViewer, SWT.NONE );
        col0.getColumn().setText ( "Identity" );
        col0.getColumn().setWidth ( 300 );
        col0.setLabelProvider ( new CObjListDisplayNameColumnLabelProvider() );
        col0.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                memberReverseSort = !memberReverseSort;
                doMemSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        lblSubscribers = new Label ( container, SWT.NONE );
        lblSubscribers.setText ( "Subscribers" );

        subTableViewer = new TableViewer ( container, SWT.BORDER | SWT.FULL_SELECTION |
                                           SWT.H_SCROLL | SWT.V_SCROLL );

        subTableViewer.setContentProvider ( new CObjListIdentPubContentProvider (
                                                app.getNode().getIndex(), CObj.CREATOR ) );

        subTable = subTableViewer.getTable();
        subTable.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
        subTable.setHeaderVisible ( true );
        subTable.setLinesVisible ( true );

        TableViewerColumn scol0 = new TableViewerColumn ( subTableViewer, SWT.NONE );
        scol0.getColumn().setText ( "Identity" );
        scol0.getColumn().setWidth ( 300 );
        scol0.setLabelProvider ( new CObjListDisplayNameColumnLabelProvider() );
        scol0.getColumn().addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                subReverseSort = !subReverseSort;
                doSubSearch();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        setCommunity ( selectedCommunity );

        return container;
    }

    private boolean memberReverseSort = false;
    private boolean subReverseSort = false;

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                       true );
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );
    }

    private CObj selectedCommunity;
    private Table subTable;
    private TableViewer subTableViewer;
    private Label lblSubscribers;

    public void open ( CObj comid )
    {
        setCommunity ( comid );
        super.open();
    }

    public void setCommunity ( CObj com )
    {
        selectedCommunity = com;

        if ( selectedCommunity != null && !memberTable.isDisposed() &&
                !subTable.isDisposed() && !lblMembersOfCommunity.isDisposed() )
        {
            doMemSearch();

            doSubSearch();

        }

    }

    private void doSubSearch()
    {
        Sort s = new Sort();

        s.setSort ( new SortField ( CObj.docString ( CObj.CREATOR ), SortField.Type.STRING, subReverseSort ) );

        CObjList ol = ( CObjList ) subTableViewer.getInput();
        CObjList sublst =
            app.getNode().getIndex().getSubscriptions ( selectedCommunity.getDig(), s );
        subTableViewer.setInput ( sublst );

        if ( ol != null )
        {
            ol.close();
        }

    }

    private void doMemSearch()
    {
        String name = selectedCommunity.getPrivate ( CObj.NAME );
        String lablestr = "Members of Community: " + name;

        CObjList ol = ( CObjList ) memberTableViewer.getInput();

        if ( CObj.SCOPE_PUBLIC.equals ( selectedCommunity.getString ( CObj.SCOPE ) ) )
        {
            lablestr = lablestr + " (PUBLIC)";
            CObjList tl = new CObjList();
            memberTableViewer.setInput ( tl );
        }

        else
        {
            Sort s = new Sort();

            s.setSort ( new SortField ( CObj.docString ( CObj.NAME ), SortField.Type.STRING, memberReverseSort ) );

            CObjList memlst =
                app.getNode().getIndex().getMemberships ( selectedCommunity.getDig(), s );
            CObj ownerref = new CObj();
            ownerref.pushPrivate ( CObj.MEMBERID, selectedCommunity.getString ( CObj.CREATOR ) );
            memlst.add ( ownerref );
            memberTableViewer.setInput ( memlst );
        }

        lblMembersOfCommunity.setText ( lablestr );

        if ( ol != null )
        {
            ol.close();
        }

    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 402 );
    }

    public Table getMemberTable()
    {
        return memberTable;
    }

    public TableViewer getMemberTableViewer()
    {
        return memberTableViewer;
    }

    public Label getLblMembersOfCommunity()
    {
        return lblMembersOfCommunity;
    }

    public Table getSubTable()
    {
        return subTable;
    }

    public TableViewer getSubTableViewer()
    {
        return subTableViewer;
    }

}
