package aktie.gui;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Text;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;
import swing2swt.layout.FlowLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;

public class AdvancedSearchDialog extends Dialog implements AddFieldInterface
{
    private Text text;
    private Text minUserRank;
    private CDateTime earliest;
    private CDateTime latest;
    private Text searchText;
    private Table table;
    private ComboViewer comboViewer;
    private SWTApp app;
    private CObj community;
    private AddFieldDialog addFieldDialog;
    private Shell shell;
    private TableViewer fieldTableViewer;
    private CObjContentProvider fieldProvider;
    private NewPostFieldEditorSupport fieldEditor;
    private AdvSearchMaxEditorSupport fieldMaxEditor;

    /**
        Create the dialog.
        @param parentShell
    */
    public AdvancedSearchDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        shell = parentShell;
        app = a;
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        addFieldDialog = new AddFieldDialog ( shell, this );
        addFieldDialog.create();
    }

    public void open ( CObj comid )
    {
        community = comid;
        setCommunity ( comid );
        super.open();
    }

    private boolean newCommunity;
    private Button btnEarliest;
    private Button btnLatest;
    private Text maxFileSize;
    private Text minFileSize;
    public void setCommunity ( CObj c )
    {
        newCommunity = newCommunity || community == null || !community.equals ( c );
        community = c;

        if ( app != null && community != null )
        {
            if ( table != null && !table.isDisposed() &&
                    searchText != null && !searchText.isDisposed() )
            {
                if ( newCommunity )
                {
                    newCommunity = false;
                    fieldProvider.clear();
                    CObjList dlst = app.getNode().getIndex().getDefFields ( community.getDig() );
                    CObj lf = null;

                    for ( int i = 0; i < dlst.size(); i++ )
                    {
                        try
                        {
                            CObj d = dlst.get ( i );
                            lf = d;
                            fieldProvider.addCObj ( d );
                        }

                        catch ( Exception e )
                        {
                        }

                    }

                    dlst.close();
                    fieldTableViewer.setInput ( lf );

                }

            }

        }

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

        Group composite = new Group ( container, SWT.NONE );
        composite.setLayout ( new GridLayout ( 6, false ) );
        composite.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        comboViewer = new ComboViewer ( composite, SWT.NONE );
        comboViewer.setContentProvider ( ArrayContentProvider.getInstance() );
        comboViewer.setLabelProvider ( new LabelProvider() );
        Combo combo = comboViewer.getCombo();
        GridData gd_combo = new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 );
        gd_combo.widthHint = 80;
        combo.setLayoutData ( gd_combo );
        combo.setToolTipText ( "Select a previously saved query." );
        comboViewer.setInput ( new String[] {} );

        Button btnLoadQuery = new Button ( composite, SWT.NONE );
        btnLoadQuery.setText ( "Load Query" );

        Label lblQueryName = new Label ( composite, SWT.NONE );
        lblQueryName.setText ( "Query name" );

        text = new Text ( composite, SWT.BORDER );
        text.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        text.setSize ( 150, 32 );

        Button btnAutodownload = new Button ( composite, SWT.CHECK );
        btnAutodownload.setText ( "Auto-download" );

        Button btnSaveQuery = new Button ( composite, SWT.NONE );
        btnSaveQuery.setText ( "Save Query" );
        btnSaveQuery.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String v[] = ( String[] ) comboViewer.getInput();
                String na[] = new String[v.length + 1];

                for ( int c = 0; c < v.length; c++ )
                {
                    na[c] = v[c];
                }

                na[v.length] = text.getText();
                comboViewer.setInput ( na );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Composite composite_3 = new Composite ( container, SWT.NONE );
        composite_3.setLayout ( new GridLayout ( 2, false ) );
        composite_3.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblBodysubjectSearchFor = new Label ( composite_3, SWT.NONE );
        lblBodysubjectSearchFor.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblBodysubjectSearchFor.setText ( "Body/Subject search for:" );

        searchText = new Text ( composite_3, SWT.BORDER );
        searchText.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Composite composite_2 = new Composite ( container, SWT.NONE );
        composite_2.setLayout ( new FlowLayout ( FlowLayout.CENTER, 5, 5 ) );
        composite_2.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        btnEarliest = new Button ( composite_2, SWT.CHECK );
        btnEarliest.setText ( "Set earliest post date" );

        earliest = new CDateTime ( composite_2, CDT.BORDER | CDT.DROP_DOWN );

        btnLatest = new Button ( composite_2, SWT.CHECK );
        btnLatest.setText ( "Set latest post date" );

        latest = new CDateTime ( composite_2, CDT.BORDER | CDT.DROP_DOWN );

        Composite composite_5 = new Composite ( container, SWT.NONE );
        composite_5.setLayout ( new FlowLayout ( FlowLayout.CENTER, 5, 5 ) );
        composite_5.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblMinUserRank = new Label ( composite_5, SWT.NONE );
        lblMinUserRank.setText ( "Min User Rank" );

        minUserRank = new Text ( composite_5, SWT.BORDER );

        Button btnAddField = new Button ( composite_5, SWT.NONE );
        btnAddField.setText ( "Add Search Field" );
        btnAddField.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                addFieldDialog.open();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Button btnDeleteField = new Button ( composite_5, SWT.NONE );
        btnDeleteField.setText ( "Delete Field" );
        btnDeleteField.setToolTipText ( "Delete fields you do not wish to search by." );

        Label lblFileSizeMin = new Label ( composite_5, SWT.NONE );
        lblFileSizeMin.setText ( "File size min" );

        minFileSize = new Text ( composite_5, SWT.BORDER );

        Label lblMax = new Label ( composite_5, SWT.NONE );
        lblMax.setText ( "max" );

        maxFileSize = new Text ( composite_5, SWT.BORDER );

        btnDeleteField.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) fieldTableViewer.getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                while ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof CObjElement )
                    {
                        CObjElement em = ( CObjElement ) selo;
                        fieldProvider.removeElement ( em );
                        fieldTableViewer.setInput ( em );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        fieldProvider = new CObjContentProvider();
        fieldTableViewer = new TableViewer ( container, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION );
        fieldTableViewer.setContentProvider ( fieldProvider );
        table = fieldTableViewer.getTable();
        table.setHeaderVisible ( true );
        table.setLinesVisible ( true );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        TableViewerColumn col0 = new TableViewerColumn ( fieldTableViewer, SWT.NONE );
        col0.getColumn().setText ( "Field" );
        col0.getColumn().setWidth ( 100 );
        col0.getColumn().setMoveable ( false );
        col0.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.FLD_NAME ) );

        TableViewerColumn col1 = new TableViewerColumn ( fieldTableViewer, SWT.NONE );
        col1.getColumn().setText ( "Description" );
        col1.getColumn().setWidth ( 450 );
        col1.getColumn().setMoveable ( false );
        col1.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.FLD_DESC ) );

        TableViewerColumn col2 = new TableViewerColumn ( fieldTableViewer, SWT.NONE );
        col2.getColumn().setText ( "Value/Minimum" );
        col2.getColumn().setWidth ( 100 );
        col2.getColumn().setMoveable ( false );
        col2.setLabelProvider ( new CObjListPrivateColumnLabelProvider ( CObj.FLD_VAL ) );
        fieldEditor = new NewPostFieldEditorSupport ( fieldTableViewer );
        col2.setEditingSupport ( fieldEditor );

        TableViewerColumn col3 = new TableViewerColumn ( fieldTableViewer, SWT.NONE );
        col3.getColumn().setText ( "Value/Minimum" );
        col3.getColumn().setWidth ( 100 );
        col3.getColumn().setMoveable ( false );
        col3.setLabelProvider ( new CObjListPrivateColumnLabelProvider ( CObj.FLD_MAX ) );
        fieldMaxEditor = new AdvSearchMaxEditorSupport ( fieldTableViewer );
        col3.setEditingSupport ( fieldMaxEditor );

        setCommunity ( community );
        return container;
    }

    private CObj getQuery()
    {
        CObj so = new CObj();
        so.pushString ( CObj.COMMUNITYID, community.getDig() );

        long minrank = 0;

        try
        {
            minrank = Long.valueOf ( minUserRank.getText() );
        }

        catch ( Exception e )
        {
        }

        so.pushNumber ( CObj.QRY_MIN_USER_RANK, minrank );
        so.pushNumber ( CObj.QRY_MAX_USER_RANK, Long.MAX_VALUE );

        String minsize = minFileSize.getText();
        Matcher dig = Pattern.compile ( "(\\d+)" ).matcher ( minsize );

        if ( dig.find() )
        {
            String d = dig.group ( 1 );

            try
            {
                long mins = Long.valueOf ( d );
                so.pushNumber ( CObj.QRY_MIN_FILE_SIZE, mins );
            }

            catch ( Exception e )
            {
            }

        }

        String maxsize = maxFileSize.getText();
        dig.reset ( maxsize );

        if ( dig.find() )
        {
            String d = dig.group ( 1 );

            try
            {
                long maxs = Long.valueOf ( d );
                so.pushNumber ( CObj.QRY_MAX_FILE_SIZE, maxs );
            }

            catch ( Exception e )
            {
            }

        }

        if ( btnEarliest.getSelection() && earliest.hasSelection() )
        {
            Date e = earliest.getSelection();
            so.pushNumber ( CObj.QRY_MIN_DATE, e.getTime() );
        }

        if ( btnLatest.getSelection() && latest.hasSelection() )
        {
            Date e = latest.getSelection();
            so.pushNumber ( CObj.QRY_MAX_DATE, e.getTime() );
        }

        so.pushString ( CObj.SUBJECT, searchText.getText() );

        List<CObj> fl = fieldProvider.getCObjList();

        for ( CObj f : fl )
        {
            //Clone the field because we overwrite the min/max values
            //for numbers
            CObj cf = f.clone();
            String tp = cf.getType();

            String val = cf.getPrivate ( CObj.FLD_VAL );
            String max = cf.getPrivate ( CObj.FLD_MAX );

            if ( val != null )
            {

                if ( CObj.FLD_TYPE_BOOL.equals ( tp ) )
                {
                    so.setNewFieldBool ( cf, Boolean.valueOf ( val ) );
                }

                if ( CObj.FLD_TYPE_STRING.equals ( tp ) )
                {
                    so.setNewFieldString ( cf, val );
                }

                if ( CObj.FLD_TYPE_OPT.equals ( tp ) )
                {
                    so.setNewFieldString ( cf, val );
                }

                if ( CObj.FLD_TYPE_NUMBER.equals ( tp ) )
                {

                    long minn = Long.MIN_VALUE;
                    long maxn = Long.MAX_VALUE;

                    if ( val != null )
                    {
                        try
                        {
                            minn = Long.valueOf ( val );
                        }

                        catch ( Exception e )
                        {
                        }

                    }

                    if ( max != null )
                    {
                        try
                        {
                            maxn = Long.valueOf ( max );
                        }

                        catch ( Exception e )
                        {
                        }

                    }

                    cf.pushNumber ( CObj.FLD_MAX, maxn );
                    cf.pushNumber ( CObj.FLD_MIN, minn );
                    so.setNewFieldNumber ( cf, 0L );

                }

                if ( CObj.FLD_TYPE_DECIMAL.equals ( tp ) )
                {

                    double minn = Double.MIN_VALUE;
                    double maxn = Double.MAX_VALUE;

                    if ( val != null )
                    {
                        try
                        {
                            minn = Double.valueOf ( val );
                        }

                        catch ( Exception e )
                        {
                        }

                    }

                    if ( max != null )
                    {
                        try
                        {
                            maxn = Double.valueOf ( max );
                        }

                        catch ( Exception e )
                        {
                        }

                    }

                    cf.pushDecimal ( CObj.FLD_MAX, maxn );
                    cf.pushDecimal ( CObj.FLD_MIN, minn );
                    so.setNewFieldDecimal ( cf, 0D );

                }

            }

        }

        return so;
    }

    @Override
    protected void okPressed()
    {
        CObj q = getQuery();
        app.setAdvancedQuery ( q );
        super.okPressed();
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false );
        createButton ( parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false );
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 810, 500 );
    }

    @Override
    public Index getIndex()
    {
        return app.getNode().getIndex();
    }

    @Override
    public CObj getCommunity()
    {
        return community;
    }

    @Override
    public IdentityCache getIdCache()
    {
        return app.getIdCache();
    }

    @Override
    public TableViewer getTableViewer()
    {
        return fieldTableViewer;
    }

    public Button getBtnEarliest()
    {
        return btnEarliest;
    }

    public Button getBtnLatest()
    {
        return btnLatest;
    }

    public Text getMinUserRank()
    {
        return minUserRank;
    }

    public Text getMinFileSize()
    {
        return minFileSize;
    }

    public Text getMaxFileSize()
    {
        return maxFileSize;
    }

    public Text getSearchText()
    {
        return searchText;
    }

}
