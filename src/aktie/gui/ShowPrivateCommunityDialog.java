package aktie.gui;

import java.util.Iterator;

import org.apache.lucene.search.Sort;
//import org.apache.lucene.search.SortField;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
//import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.IStructuredSelection;
//import org.eclipse.jface.viewers.TableViewer;
//import org.eclipse.jface.viewers.TableViewerColumn;

import aktie.data.CObj;
import aktie.gui.pm.PrivateMessageDialog;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTable;
import aktie.gui.table.CObjListTableCellLabelProviderTypeDisplayName;
import aktie.gui.table.CObjListTableInputProvider;
import aktie.gui.table.CObjListTableCellLabelProviderTypeString;
import aktie.gui.table.CObjListTableContentProviderTypeArrayElement;
import aktie.index.CObjList;

public class ShowPrivateCommunityDialog extends Dialog
{
    private Text searchTxt;
    private ShowPrivComTable table;
    private SWTApp app;
    private PrivateMessageDialog msgDialog;

    /**
        Create the dialog.
        @param parentShell
    */
    public ShowPrivateCommunityDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
    }

    public void setMessageDialog ( PrivateMessageDialog m )
    {
        msgDialog = m;
    }

    @Override
    protected void configureShell ( Shell shell )
    {
        super.configureShell ( shell );
        shell.setText ( "Locked Communities" );
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

        Label lblCommunitiesYouAre = new Label ( container, SWT.NONE );
        lblCommunitiesYouAre.setText ( "Private communities you are not a member of (locked)." );
        new Label ( container, SWT.NONE );

        Label lblRequestAccessFrom = new Label ( container, SWT.NONE );
        lblRequestAccessFrom.setText ( "Request access from the creator. (Right click - Send Message)" );
        new Label ( container, SWT.NONE );

        searchTxt = new Text ( container, SWT.BORDER );
        searchTxt.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        searchTxt.addListener ( SWT.Traverse, new Listener()
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
                doSearch ( );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        table = new ShowPrivComTable ( container, app );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        new Label ( container, SWT.NONE );

        Menu menu_5 = new Menu ( table.getTable() );
        table.setMenu ( menu_5 );

        MenuItem newmsg = new MenuItem ( menu_5, SWT.NONE );
        newmsg.setText ( "Send Message" );
        newmsg.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( msgDialog != null )
                {
                    CObj selid = app.getSelectedIdentity();

                    if ( selid != null )
                    {
                        IStructuredSelection sel = table.getTableViewer().getSelection();
                        @SuppressWarnings ( "rawtypes" )
                        Iterator i = sel.iterator();

                        if ( i.hasNext() )
                        {
                            CObjListArrayElement ae = ( CObjListArrayElement ) i.next();
                            CObj com = ae.getCObj();

                            if ( com != null )
                            {
                                String creator = com.getString ( CObj.CREATOR );

                                if ( creator != null )
                                {
                                    CObj rply = new CObj();
                                    rply.pushPrivate ( CObj.SUBJECT, com.getDisplayName() );
                                    msgDialog.open ( selid.getId(), creator, rply );
                                }

                            }

                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        fillData();

        return container;
    }

    public int open()
    {
        fillData();
        return super.open();
    }

    private void doSearch ()
    {
        String str = searchTxt.getText();
        doSearch ( str );
    }

    private void doSearch ( String str )
    {
        table.setSearchString ( str );
        table.searchAndSort();
    }

    public void fillData()
    {
        if ( table != null && !table.isDisposed() )
        {
            doSearch ( null );
        }

    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );
        createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                       false );
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 300 );
    }

    public Text getSearchTxt()
    {
        return searchTxt;
    }

    private class ShowPrivComTable extends CObjListTable<CObjListArrayElement>
    {
        public ShowPrivComTable ( Composite composite, SWTApp app )
        {
            super ( composite, SWT.BORDER | SWT.FULL_SELECTION );

            setContentProvider ( new CObjListTableContentProviderTypeArrayElement() );

            setInputProvider ( new ShowPrivComTableInputProvider ( app ) );

            AktieTableViewerColumn<CObjList, CObjListGetter> column;

            column = addColumn ( "Community", 150, new CObjListTableCellLabelProviderTypeDisplayName ( false, null ) );
            getTableViewer().setSortColumn ( column, false );

            addColumn ( "Creator", 150, new CObjListTableCellLabelProviderTypeString ( CObj.CREATOR_NAME, false, null ) );
        }

        @Override
        public ShowPrivComTableInputProvider getInputProvider()
        {
            return ( ShowPrivComTableInputProvider ) super.getInputProvider();
        }

        public void setSearchString ( String s )
        {
            getInputProvider().setSearchString ( s );
        }

    }

    private class ShowPrivComTableInputProvider extends CObjListTableInputProvider
    {
        private SWTApp app;

        private String searchString = "";

        public ShowPrivComTableInputProvider ( SWTApp app )
        {
            this.app = app;
        }

        @Override
        public CObjList provideInput ( Sort sort )
        {
            return app.getNode().getIndex().searchSemiPrivateCommunities ( searchString, sort );
        }

        public void setSearchString ( String s )
        {
            searchString = s;
        }

    }

}
