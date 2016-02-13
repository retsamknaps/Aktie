package aktie.gui;

import java.io.IOException;

import aktie.data.CObj;
import aktie.index.CObjList;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;

public class SubscriptionDialog extends Dialog
{
    private Text text;
    private TableViewer tableViewer;
    private Table table;
    private SWTApp app;
    private Button btnShowPublic;
    private Button btnShowPrivate;


    /**
        Create the dialog.
        @param parentShell
    */
    public SubscriptionDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
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

    private String sortPostField1;
    private boolean sortPostReverse;
    private SortField.Type sortPostType1;

    private void doSearch ( String ss, boolean prv, boolean pub )
    {
        Sort s = new Sort();

        if ( sortPostField1 != null )
        {
            s.setSort ( new SortField ( sortPostField1, sortPostType1, sortPostReverse ) );

        }

        else
        {
            s.setSort ( new SortedNumericSortField ( CObj.docNumber ( CObj.CREATEDON ), SortedNumericSortField.Type.LONG, true ) );
        }

        CObjList oldl = ( CObjList ) tableViewer.getInput();
        CObjList l = app.getNode().getIndex().searchSubscribable ( ss,
                     selectedid, prv, pub, s );

        if ( tableViewer != null )
        {
            tableViewer.setInput ( l );
        }

        if ( oldl != null )
        {
            oldl.close();
        }

        tableViewer.refresh();

    }

    private void defaultSearch()
    {
        if ( app != null && tableViewer != null && !table.isDisposed() )
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
                    System.out.println ( "Enter pressed" );
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

        tableViewer = new TableViewer ( container, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
        table = tableViewer.getTable();
        table.setLinesVisible ( true );
        table.setHeaderVisible ( true );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
        new Label ( container, SWT.NONE );

        CObjListContentProvider cont = new CObjListContentProvider();
        tableViewer.setContentProvider ( cont );
        //https://bugs.eclipse.org/bugs/show_bug.cgi?id=446534

        TableViewerColumn col0 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col0.getColumn().setText ( "Community" );
        col0.getColumn().setWidth ( 150 );
        col0.setLabelProvider ( new CObjListPrivDispNameColumnLabelProvider() );
        col0.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docPrivate ( CObj.PRV_DISPLAY_NAME );

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

        TableViewerColumn col2 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col2.getColumn().setText ( "Description" );
        col2.getColumn().setWidth ( 300 );
        col2.setLabelProvider ( new CObjListPrivateColumnLabelProvider ( CObj.DESCRIPTION ) );
        col2.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docPrivate ( CObj.DESCRIPTION );

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
        col1.getColumn().setText ( "Scope" );
        col1.getColumn().setWidth ( 50 );
        col1.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.SCOPE ) );
        col1.getColumn().addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docString ( CObj.SCOPE );

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

}
