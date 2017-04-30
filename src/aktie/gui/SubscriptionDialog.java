package aktie.gui;

import java.io.IOException;

import aktie.data.CObj;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDisplayName;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.gui.table.CObjListTableCellLabelProviderTypeString;
import aktie.gui.table.CObjListTableContentProviderTypeArrayElement;
import aktie.index.CObjList;

import org.apache.lucene.search.Sort;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.RowLayout;

public class SubscriptionDialog extends Dialog
{
    private Text text;
    private SubscriptionTable table;
    private SWTApp app;
    private Button btnShowPublic;
    private Button btnShowPrivate;


    /**
        Create the dialog.
        @param parentShell
    */
    public SubscriptionDialog ( Shell parentShell, SWTApp app )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        this.app = app;
    }


    private void selectIdentity ( String id )
    {
        if ( app != null && id != null && lblNewSubscriptionFor != null && !lblNewSubscriptionFor.isDisposed() )
        {
            CObj ido = app.getNode().getIndex().getIdentity ( id );
            lblNewSubscriptionFor.setText ( "New subscription for: " + ido.getDisplayName() );
        }

    }

    private String selectedid;
    private Label lblNewSubscriptionFor;

    public void open ( String selid )
    {
        selectedid = selid;
        selectIdentity ( selid );
        defaultSearch();
        super.open();
    }

    @Override
    protected void configureShell ( Shell shell )
    {
        super.configureShell ( shell );
        shell.setText ( "Subscription" );
    }

    private void doSearch ( String searchString, boolean searchPrivate, boolean searchPublic )
    {
        table.setSearch ( searchString, selectedid, searchPrivate, searchPublic );
        table.searchAndSort();
    }

    private void defaultSearch()
    {
        if ( table != null && !table.isDisposed() )
        {

            doSearch ( "", true, true );

        }

    }

    private void doSearch()
    {
        boolean prv = btnShowPrivate.getSelection();
        boolean pub = btnShowPublic.getSelection();

        doSearch ( text.getText(), prv, pub );
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

        lblNewSubscriptionFor = new Label ( container, SWT.NONE );
        lblNewSubscriptionFor.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblNewSubscriptionFor.setText ( "New Subscription for:" );
        new Label ( container, SWT.NONE );

        text = new Text ( container, SWT.BORDER );
        text.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        text.addListener ( SWT.Traverse, new Listener()
        {
            @Override
            public void handleEvent ( Event event )
            {
                if ( event.detail == SWT.TRAVERSE_RETURN )
                {
                    doSearch();
                }

            }

        } );

        Button btnSearch = new Button ( container, SWT.NONE );
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

        Composite composite = new Composite ( container, SWT.NONE );
        composite.setLayout ( new RowLayout ( SWT.HORIZONTAL ) );

        btnShowPublic = new Button ( composite, SWT.CHECK );
        btnShowPublic.setText ( "Show public" );

        btnShowPrivate = new Button ( composite, SWT.CHECK );
        btnShowPrivate.setText ( "Show private" );
        new Label ( container, SWT.NONE );

        table = new SubscriptionTable ( container, this );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        defaultSearch();

        if ( app != null )
        {
            if ( selectedid != null )
            {
                selectIdentity ( selectedid );
            }

        }

        return container;
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, "Subscribe",
                       false );
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );
    }

    @Override
    protected void cancelPressed()
    {
        CObjList clst = ( CObjList ) table.getTableViewer().getInput();

        if ( clst != null )
        {
            clst.close();
        }

        super.cancelPressed();
    }

    @Override
    protected void okPressed()
    {
        CObjList clst = ( CObjList ) table.getTableViewer().getInput();

        if ( selectedid != null )
        {
            String id = selectedid;

            if ( id != null )
            {
                int idx[] = table.getSelectionIndices();

                for ( int c = 0; c < idx.length; c++ )
                {
                    try
                    {
                        CObj com = clst.get ( idx[c] );
                        CObj co = new CObj();
                        co.setType ( CObj.SUBSCRIPTION );
                        co.pushString ( CObj.SUBSCRIBED, "true" );
                        co.pushString ( CObj.COMMUNITYID, com.getDig() );
                        co.pushString ( CObj.CREATOR, id );

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
        return new Point ( 611, 300 );
    }

    public Button getBtnShowPublic()
    {
        return btnShowPublic;
    }

    public Button getBtnShowPrivate()
    {
        return btnShowPrivate;
    }

    public Label getLblNewSubscriptionFor()
    {
        return lblNewSubscriptionFor;
    }

    private SWTApp getSWTApp()
    {
        return app;
    }

    private class SubscriptionTable extends CObjListTable<CObjListArrayElement>
    {
        public SubscriptionTable ( Composite composite, SubscriptionDialog dialog )
        {
            super ( composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );

            setContentProvider ( new CObjListTableContentProviderTypeArrayElement() );

            setInputProvider ( new SubscriptionTableInputProvider ( dialog ) );

            AktieTableViewerColumn<CObjList, CObjListGetter> column;

            column = addColumn ( "Community", 150, new CObjListTableCellLabelProviderTypeDisplayName ( true, null ) );
            getTableViewer().setSortColumn ( column, false );

            addColumn ( "Description", 300, new CObjListTableCellLabelProviderTypeString ( CObj.DESCRIPTION, true, null ) );

            addColumn ( "Scope", 50, new CObjListTableCellLabelProviderTypeString ( CObj.SCOPE, false, null ) );

            // This attribute seems not to exist.
            //addColumn ( "Date Created", 100, new CObjListTableCellLabelProviderTypeDate( CObj.CREATEDON, false, null ) );

        }

        @Override
        public SubscriptionTableInputProvider getInputProvider()
        {
            return ( SubscriptionTableInputProvider ) super.getInputProvider();
        }

        public void setSearch ( String searchString, String selectedIdentity, boolean searchPrivate, boolean searchPublic )
        {
            getInputProvider().setSearchString ( searchString );
            getInputProvider().setSelectedIdentity ( selectedIdentity );
            getInputProvider().setSearchPrivate ( searchPrivate );
            getInputProvider().setSearchPublic ( searchPublic );
        }

    }

    private class SubscriptionTableInputProvider extends CObjListTableInputProvider
    {
        private SubscriptionDialog dialog;

        private String searchString = "";
        private String selectedIdentity = "";
        private boolean searchPrivate = true;
        private boolean searchPublic = true;

        public SubscriptionTableInputProvider ( SubscriptionDialog dialog )
        {
            this.dialog = dialog;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            return dialog.getSWTApp().getNode().getIndex().searchSubscribable ( searchString, selectedIdentity, searchPrivate, searchPublic, sort );
        }

        public void setSearchString ( String s )
        {
            searchString = s;
        }

        public void setSelectedIdentity ( String id )
        {
            selectedIdentity = id;
        }

        public void setSearchPrivate ( boolean b )
        {
            searchPrivate = b;
        }

        public void setSearchPublic ( boolean b )
        {
            searchPublic = b;
        }

    }

}
