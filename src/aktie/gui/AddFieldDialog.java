package aktie.gui;

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
import aktie.index.CObjList;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;

public class AddFieldDialog extends Dialog
{
    private Text text;

    private NewPostDialog postDialog;
    private Table table;
    private TableViewer tableViewer;

    /**
        Create the dialog.
        @param parentShell
    */
    public AddFieldDialog ( Shell parentShell, NewPostDialog pd )
    {
        super ( parentShell );
        postDialog = pd;
    }

    private void doSearch()
    {
        if ( text != null && tableViewer != null &&
                postDialog.getCommunity() != null &&
                !text.isDisposed() && !tableViewer.getTable().isDisposed() )
        {
            Object inp = tableViewer.getInput();

            String ss = text.getText();
            CObjList sl = postDialog.getApp().getNode().getIndex().searchFields (
                              postDialog.getCommunity().getDig(), ss, null );
            tableViewer.setInput ( sl );

            if ( inp != null && inp instanceof CObjList )
            {
                CObjList ls = ( CObjList ) inp;
                ls.close();
            }

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
                    System.out.println ( "Enter pressed" );
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

        tableViewer = new TableViewer ( container, SWT.BORDER | SWT.FULL_SELECTION );
        tableViewer.setContentProvider ( new CObjListContentProvider() );
        table = tableViewer.getTable();
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
        table.setHeaderVisible ( true );
        table.setLinesVisible ( true );

        TableViewerColumn col0 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col0.getColumn().setText ( "Field" );
        col0.getColumn().setWidth ( 100 );
        col0.getColumn().setMoveable ( false );
        col0.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.FLD_NAME ) );

        TableViewerColumn col1 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col1.getColumn().setText ( "Description" );
        col1.getColumn().setWidth ( 200 );
        col1.getColumn().setMoveable ( false );
        col1.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.FLD_DESC ) );

        TableViewerColumn col2 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col2.getColumn().setText ( "Creator" );
        col2.getColumn().setWidth ( 100 );
        col2.getColumn().setMoveable ( false );
        col2.setLabelProvider ( new CObjListCachePrivateIdentityLableProvider (
                                    postDialog.getApp().getIdCache(), CObj.CREATOR ) );

        doSearch();

        return container;
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, "Add Selected Fields", false );
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

    public Text getText()
    {
        return text;
    }

    public TableViewer getTableViewer()
    {
        return tableViewer;
    }

}
