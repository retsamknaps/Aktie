package aktie.gui;

import java.util.Iterator;

import org.apache.lucene.search.Sort;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import aktie.data.CObj;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDisplayName;
import aktie.gui.table.CObjListTableCellLabelProviderTypeString;
import aktie.gui.table.CObjListTableContentProviderTypeArrayElement;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.index.CObjList;

public class SelectIdentityDialog extends Dialog
{
    private SelectIdentityTable table;
    private Text searchText;
    private Button btnSearch;
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

        table = new SelectIdentityTable ( container, app );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        defaultSearch();

        return container;
    }

    private void doSearch()
    {
        String squery = searchText.getText();
        doSearch ( squery );
    }

    private void doSearch ( String str )
    {
        table.setSearchString ( str );
        table.searchAndSort();
    }

    private void defaultSearch()
    {
        if ( app != null )
        {
            if ( table != null && !table.isDisposed() )
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
        IStructuredSelection sel = table.getTableViewer().getSelection();
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
        // Table viewer will close the currently open list,
        // when we set a 'new' one.
        table.getTableViewer().setInput ( null );

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

    private class SelectIdentityTable extends CObjListTable<CObjListArrayElement>
    {
        public SelectIdentityTable ( Composite composite, SWTApp app )
        {
            super ( composite, SWT.MULTI | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION );

            setContentProvider ( new CObjListTableContentProviderTypeArrayElement() );

            setInputProvider ( new SelectIdentityTableInputProvider ( app ) );

            AktieTableViewerColumn<CObjList, CObjListGetter> column;

            // FIXME: How to sort by lucene?
            column = addColumn ( "Name", 150, new CObjListTableCellLabelProviderTypeDisplayName ( true, null ) );
            getTableViewer().setSortColumn ( column, false );
            addColumn ( "Description", 150, new CObjListTableCellLabelProviderTypeString ( CObj.DESCRIPTION, false, null ) );
            // FIXME: Seems to be empty.
            //addColumn ( "Date Created", 150, new CObjListTableCellLabelProviderTypeDate( CObj.CREATEDON, false, null ) );
        }

        @Override
        public SelectIdentityTableInputProvider getInputProvider()
        {
            return ( SelectIdentityTableInputProvider ) super.getInputProvider();
        }

        public void setSearchString ( String s )
        {
            getInputProvider().setSearchString ( s );
        }

    }

    private class SelectIdentityTableInputProvider extends CObjListTableInputProvider
    {
        private SWTApp app;
        private String searchString = "";

        public SelectIdentityTableInputProvider ( SWTApp app )
        {
            this.app = app;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            return app.getNode().getIndex().searchIdentities ( searchString, sort );
        }

        public void setSearchString ( String s )
        {
            searchString = s;
        }

    }

}
