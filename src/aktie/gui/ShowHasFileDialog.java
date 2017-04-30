package aktie.gui;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.search.Sort;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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

import aktie.data.CObj;
import aktie.gui.pm.PrivateMessageDialog;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDisplayName;
import aktie.gui.table.CObjListTableContentProviderTypeIdentityElement;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.index.CObjList;

import org.eclipse.jface.viewers.IStructuredSelection;

public class ShowHasFileDialog extends Dialog
{

    private SWTApp app;
    private ShowHasFileTable table;
    private CObj fileo;
    private Label lblNodesHaveFile;
    private SetUserRankDialog usrRankDialog;
    private PrivateMessageDialog prvMessageDialog;

    /**
        Create the dialog.
        @param parentShell
    */
    public ShowHasFileDialog ( Shell parentShell, SetUserRankDialog d, SWTApp app )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        usrRankDialog = d;
        this.app = app;
        prvMessageDialog = new PrivateMessageDialog ( app );
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

        table = new ShowHasFileTable ( container, app, lblNodesHaveFile );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

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

                    if ( selo instanceof CObjListIdentityElement )
                    {
                        CObjListIdentityElement element = ( CObjListIdentityElement ) selo;
                        users.add ( element.getCObj() );
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

        MenuItem sndPrivate = new MenuItem ( menu, SWT.NONE );
        sndPrivate.setText ( "Send Private Message" );
        sndPrivate.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = table.getTableViewer().getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof CObjListIdentityElement )
                    {
                        CObjListIdentityElement element = ( CObjListIdentityElement ) selo;
                        CObj fr = element.getCObj();

                        if ( app.getSelectedIdentity() == null )
                        {
                            MessageDialog.openWarning ( app.getShell(),
                                                        "Select an identity.", "Sorry, select an identity in the Communities tab" );
                        }

                        else if ( fr != null )
                        {
                            prvMessageDialog.open ( app.getSelectedIdentity().getId(), fr.getId() );
                        }

                    }

                }

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
        doHasFileSearch ( f );

        if ( table.getTableViewer().getInput() != null )
        {
            super.open();
        }

    }

    private void doHasFileSearch ( CObj f )
    {
        table.setHasFile ( f );
        table.searchAndSort();
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

    private class ShowHasFileTable extends CObjListTable<CObjListIdentityElement>
    {
        public ShowHasFileTable ( Composite composite, SWTApp app, Label lblNodesHaveFile )
        {
            super ( composite,  SWT.BORDER | SWT.FULL_SELECTION );

            setContentProvider ( new CObjListTableContentProviderTypeIdentityElement ( app.getNode().getIndex(), CObj.CREATOR, false ) );

            setInputProvider ( new ShowHasFileTableInputProvider ( app, lblNodesHaveFile ) );

            AktieTableViewerColumn<CObjList, CObjListGetter> column;

            column = addColumn ( "Identity", 300, new CObjListTableCellLabelProviderTypeDisplayName ( false, null ) );
            getTableViewer().setSortColumn ( column, false );
            /// Seems to be empty
            //addColumn ( "Date Created", 150, new CObjListTableCellLabelProviderTypeDate( CObj.CREATEDON, false, null ) );
        }

        @Override
        public ShowHasFileTableInputProvider getInputProvider()
        {
            return ( ShowHasFileTableInputProvider ) super.getInputProvider();
        }

        public void setHasFile ( CObj co )
        {
            getInputProvider().setHasFile ( co );
        }

    }

    private class ShowHasFileTableInputProvider extends CObjListTableInputProvider
    {
        private SWTApp app;
        private Label lblNodesHaveFile;
        private CObj hasFile = null;

        public ShowHasFileTableInputProvider ( SWTApp app, Label lblNodesHaveFile  )
        {
            this.app = app;
            this.lblNodesHaveFile = lblNodesHaveFile;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            if ( hasFile != null )
            {
                String fileDigest = hasFile.getString ( CObj.FILEDIGEST );
                String fragDigest = hasFile.getString ( CObj.FRAGDIGEST );
                String communityID = hasFile.getString ( CObj.COMMUNITYID );

                if ( fileDigest != null && fragDigest != null && communityID != null )
                {

                    String fname = hasFile.getString ( CObj.NAME );

                    if ( !lblNodesHaveFile.isDisposed() )
                    {
                        lblNodesHaveFile.setText ( "Identities have file: " + fname );
                    }

                    return app.getNode().getIndex().getHasFiles ( communityID, fileDigest, fragDigest );
                }

            }

            return null;
        }

        public void setHasFile ( CObj co )
        {
            hasFile = co;
        }

    }

}
