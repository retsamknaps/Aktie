package aktie.gui;

import org.apache.lucene.search.Sort;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

import aktie.data.CObj;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDisplayName;
import aktie.gui.table.CObjListTableContentProviderTypeIdentityElement;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.index.CObjList;

public class ShowMembersDialog extends Dialog
{
    private ShowMembersMemberTable memberTable;
    private ShowMembersSubscriptionTable subscriptionTable;
    private Label lblMembersOfCommunity;
    private SWTApp app;
    private CObj CommunityId;

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

        memberTable = new ShowMembersMemberTable ( container, app, lblMembersOfCommunity );
        memberTable.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        lblSubscribers = new Label ( container, SWT.NONE );
        lblSubscribers.setText ( "Subscribers" );

        subscriptionTable = new ShowMembersSubscriptionTable ( container, app );
        subscriptionTable.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        setCommunity ( CommunityId );

        return container;
    }

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

    private Label lblSubscribers;

    public void open ( CObj comid )
    {
        CommunityId = comid;
        setCommunity ( CommunityId );
        super.open();
    }

    public void setCommunity ( CObj community )
    {
        if ( community != null &&
                memberTable != null && !memberTable.isDisposed() &&
                subscriptionTable != null && !subscriptionTable.isDisposed() )
        {

            memberTable.setSelectedCommunity ( community );
            subscriptionTable.setSelectedCommunity ( community );
            doMemberSearch();

            doSubscriptionSearch();

        }

    }

    private void doSubscriptionSearch()
    {
        subscriptionTable.searchAndSort();
    }

    private void doMemberSearch()
    {
        memberTable.searchAndSort();
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 402 );
    }

    public Label getLblMembersOfCommunity()
    {
        return lblMembersOfCommunity;
    }

    private class ShowMembersMemberTable extends CObjListTable<CObjListIdentityElement>
    {
        public ShowMembersMemberTable ( Composite composite, SWTApp app, Label labelMembersOfCommunity )
        {
            super ( composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL );

            setContentProvider ( new CObjListTableContentProviderTypeIdentityElement ( app.getNode().getIndex(), CObj.MEMBERID, true ) );

            setInputProvider ( new ShowMembersMemberTableInputProvider ( app, labelMembersOfCommunity ) );

            AktieTableViewerColumn<CObjList, CObjListGetter> column;

            column = addNonIndexSortedColumn ( "Identity", 300, new CObjListTableCellLabelProviderTypeDisplayName ( false, null ) );
            getTableViewer().setSortColumn ( column, false );
        }

        @Override
        public ShowMembersMemberTableInputProvider getInputProvider()
        {
            return ( ShowMembersMemberTableInputProvider ) super.getInputProvider();
        }

        public void setSelectedCommunity ( CObj co )
        {
            getInputProvider().setSelectedCommunity ( co );
        }

    }

    private class ShowMembersMemberTableInputProvider extends CObjListTableInputProvider
    {
        private SWTApp app;
        private Label labelMembersOfCommunity;
        private CObj selectedCommunity = null;

        public ShowMembersMemberTableInputProvider ( SWTApp app, Label labelMembersOfCommunity )
        {
            this.app = app;
            this.labelMembersOfCommunity = labelMembersOfCommunity;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            if ( selectedCommunity == null )
            {
                return null;
            }

            CObjList memberList;

            String name = selectedCommunity.getPrivate ( CObj.NAME );
            String labelText = "Members of Community: " + name;

            if ( CObj.SCOPE_PUBLIC.equals ( selectedCommunity.getString ( CObj.SCOPE ) ) )
            {
                labelText = labelText + " (PUBLIC)";
                memberList = new CObjList();
            }

            else
            {

                memberList = app.getNode().getIndex().getMemberships ( selectedCommunity.getDig(), sort );
                CObj ownerReference = new CObj();
                ownerReference.pushPrivate ( CObj.MEMBERID, selectedCommunity.getString ( CObj.CREATOR ) );
                memberList.add ( ownerReference );
            }

            if ( !labelMembersOfCommunity.isDisposed() )
            {
                labelMembersOfCommunity.setText ( labelText );
            }

            return memberList;
        }

        public void setSelectedCommunity ( CObj co )
        {
            selectedCommunity = co;
        }

    }

    private class ShowMembersSubscriptionTable extends CObjListTable<CObjListIdentityElement>
    {
        public ShowMembersSubscriptionTable ( Composite composite, SWTApp app )
        {
            super ( composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );

            setContentProvider ( new CObjListTableContentProviderTypeIdentityElement ( app.getNode().getIndex(), CObj.CREATOR, false ) );

            setInputProvider ( new ShowMembersSubscriptionTableInputProvider ( app ) );

            AktieTableViewerColumn<CObjList, CObjListGetter> column;

            column = addNonIndexSortedColumn ( "Identity", 300, new CObjListTableCellLabelProviderTypeDisplayName ( false, null ) );
            getTableViewer().setSortColumn ( column, false );
        }

        @Override
        public ShowMembersSubscriptionTableInputProvider getInputProvider()
        {
            return ( ShowMembersSubscriptionTableInputProvider ) super.getInputProvider();
        }

        public void setSelectedCommunity ( CObj co )
        {
            getInputProvider().setSelectedCommunity ( co );
        }

    }

    private class ShowMembersSubscriptionTableInputProvider extends CObjListTableInputProvider
    {
        private SWTApp app;
        private CObj selectedCommunity = null;

        public ShowMembersSubscriptionTableInputProvider ( SWTApp app )
        {
            this.app = app;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            if ( selectedCommunity == null )
            {
                return null;
            }

            return app.getNode().getIndex().getSubscriptions ( selectedCommunity.getDig(), sort );

        }

        public void setSelectedCommunity ( CObj co )
        {
            selectedCommunity = co;
        }

    }

}
