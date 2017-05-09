package aktie.gui;

import java.util.Iterator;

import org.apache.lucene.search.Sort;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;

import org.eclipse.swt.widgets.Text;

import aktie.data.CObj;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeAdvSearchFieldDescription;
import aktie.gui.table.CObjListTableCellLabelProviderTypeIdentityName;
import aktie.gui.table.CObjListTableCellLabelProviderTypeString;
import aktie.gui.table.CObjListTableContentProviderTypeArrayElement;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.index.CObjList;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.jface.viewers.IStructuredSelection;

public class AddFieldDialog extends Dialog
{
    private Text text;

    private AddFieldInterface fieldAdder;
    private AddFieldTable table;

    /**
        Create the dialog.
        @param parentShell
    */
    public AddFieldDialog ( Shell parentShell, AddFieldInterface fv )
    {
        super ( parentShell );
        fieldAdder = fv;
        setShellStyle ( getShellStyle() | SWT.RESIZE );
    }

    private void doSearch()
    {
        if ( text != null && !text.isDisposed() && table != null && !table.isDisposed() )
        {
            table.setSearchString ( text.getText() );
            table.searchAndSort();
        }

    }

    public int open()
    {
        doSearch();
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
        container.setLayout ( new GridLayout ( 1, false ) );

        Composite composite = new Composite ( container, SWT.NONE );
        composite.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        composite.setLayout ( new GridLayout ( 2, false ) );

        text = new Text ( composite, SWT.BORDER );
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

        Button btnNewButton = new Button ( composite, SWT.NONE );
        btnNewButton.addSelectionListener ( new SelectionAdapter()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                doSearch();
            }

        } );

        btnNewButton.setText ( "Search" );

        table = new AddFieldTable ( container, fieldAdder );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        doSearch();

        return container;
    }

    @Override
    protected void okPressed()
    {
        IStructuredSelection sel = table.getTableViewer().getSelection();

        @SuppressWarnings ( "rawtypes" )
        Iterator i = sel.iterator();

        CObj lt = null;

        while ( i.hasNext() )
        {
            Object selo = i.next();

            if ( selo instanceof CObjListArrayElement )
            {
                CObjListArrayElement ce = ( CObjListArrayElement ) selo;
                lt = ce.getCObj();
                CObjContentProvider prv = ( CObjContentProvider ) fieldAdder.getTableViewer().getContentProvider();
                prv.addCObj ( lt );
            }

        }

        if ( lt != null )
        {
            fieldAdder.getTableViewer().setInput ( lt );
        }

    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, "Add Selected Fields", false );
        createButton ( parent, IDialogConstants.CANCEL_ID, "Close", false );
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 300 );
    }

    public Text getText()
    {
        return text;
    }

    private class AddFieldTable extends CObjListTable<CObjListArrayElement>
    {
        public AddFieldTable ( Composite composite, AddFieldInterface fv )
        {
            super ( composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );

            setContentProvider ( new CObjListTableContentProviderTypeArrayElement() );

            setInputProvider ( new AddFieldTableInputProvider ( fv ) );

            AktieTableViewerColumn<CObjList, CObjListGetter> column;

            column = addColumn ( "Field", 100, new CObjListTableCellLabelProviderTypeString ( CObj.FLD_NAME, false, null ) );
            column.setMoveable ( false );
            getTableViewer().setSortColumn ( column, false );

            column = addColumn ( "Description", 200, new CObjListTableCellLabelProviderTypeAdvSearchFieldDescription() );
            column.setMoveable ( false );
            column.setSortable ( false );

            column = addColumn ( "Creator", 100, new CObjListTableCellLabelProviderTypeIdentityName ( CObj.CREATOR, true, null, fieldAdder.getIndex() ) );
            column.setMoveable ( false );
        }

        @Override
        public AddFieldTableInputProvider getInputProvider()
        {
            return ( AddFieldTableInputProvider ) super.getInputProvider();
        }

        public void setSearchString ( String s )
        {
            getInputProvider().setSearchString ( s );
        }

    }

    private class AddFieldTableInputProvider extends CObjListTableInputProvider
    {
        private AddFieldInterface fieldAdder;
        private String searchString = "";

        public AddFieldTableInputProvider ( AddFieldInterface fv )
        {
            fieldAdder = fv;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            CObj community = fieldAdder.getCommunity();

            if ( community != null )
            {
                return fieldAdder.getIndex().searchFields ( community.getDig(), searchString, null );
            }

            return null;
        }

        public void setSearchString ( String s )
        {
            searchString = s;
        }

    }

}
