package aktie.gui;

import java.util.Iterator;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import aktie.data.CObj;
import aktie.index.CObjList;

public class SelectIdentityDialog extends Dialog
{
    private Table table;
    private Text searchText;
    private Button btnSearch;
    private TableViewer tableViewer;
    private SWTApp app;
    private IdentitySelectedInterface selector;

    /**
        Create the dialog.
        @param parentShell
    */
    public SelectIdentityDialog ( Shell parentShell, SWTApp a, IdentitySelectedInterface s )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        selector = s;
        app = a;
    }

    @Override
    public int open ( )
    {
        defaultSearch();
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
        container.setLayout ( new GridLayout ( 2, false ) );

        searchText = new Text ( container, SWT.BORDER );
        searchText.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        searchText.addListener ( SWT.Traverse, new Listener()
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
                //String ns = CObj.docString ( CObj.NAME );
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

        TableViewerColumn col1 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col1.getColumn().setText ( "Description" );
        col1.getColumn().setWidth ( 150 );
        col1.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.DESCRIPTION ) );
        new Label ( container, SWT.NONE );
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

        defaultSearch();

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
            s.setSort ( new SortedNumericSortField ( CObj.docNumber ( CObj.CREATEDON ), SortedNumericSortField.Type.LONG, true ) );
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
        createButton ( parent, IDialogConstants.OK_ID, "Select Identity",
                       false );
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );

    }

    @Override
    protected void cancelPressed()
    {
        closeCObjList();
        super.cancelPressed();
    }

    @Override
    protected void okPressed()
    {
        IStructuredSelection sel = ( IStructuredSelection ) tableViewer.getSelection();
        @SuppressWarnings ( "rawtypes" )
        Iterator i = sel.iterator();

        if ( i.hasNext() )
        {
            Object o = i.next();

            if ( o instanceof CObjListArrayElement )
            {
                CObjListArrayElement ce = ( CObjListArrayElement ) o;

                if ( ce != null && selector != null )
                {
                    CObj co = ce.getCObj();
                    selector.selectedIdentity ( co );
                }

            }

        }

        closeCObjList();
        super.okPressed();
    }

    public void closeCObjList()
    {
        CObjList lst = ( CObjList ) tableViewer.getInput();

        if ( lst != null )
        {
            tableViewer.setInput ( null );
            lst.close();
        }

    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 300 );
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

}
