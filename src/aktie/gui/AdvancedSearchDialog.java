package aktie.gui;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Text;

import aktie.data.CObj;
import aktie.gui.table.AktieTable;
import aktie.gui.table.AktieTableViewerColumn;
import aktie.gui.table.CObjListTableCellLabelProviderTypeAdvSearchFieldDescription;
import aktie.gui.table.CObjListTableCellLabelProviderTypeString;
import aktie.index.CObjList;
import aktie.index.Index;
import org.eclipse.swt.widgets.Group;
import org.eclipse.jface.viewers.TableViewer;

public class AdvancedSearchDialog extends Dialog implements AddFieldInterface
{
    private Text text;
    private Text minUserRank;
    private CDateTime earliest;
    private CDateTime latest;
    private Text searchText;
    private AdvancedSearchTable table;
    private ComboViewer comboViewer;
    private SWTApp app;
    private CObj community;
    private CObj identity;
    private AddFieldDialog addFieldDialog;
    private Shell shell;
    private CObjContentProvider fieldProvider;
    private CObjListContentProvider queryProvider;
    private Button btnAutodownload;
    private CObj lastQuery;

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

    public void open ( CObj comid, CObj id )
    {
        community = comid;
        identity = id;
        setCommunity ( comid );
        super.open();
    }


    private void loadDefFields()
    {
        if ( community != null )
        {
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
            table.getTableViewer().setInput ( lf );
        }

    }

    private Button btnEarliest;
    private Button btnLatest;
    private Button btnNewWithinDays;
    private Text maxFileSize;
    private Text minFileSize;
    private Text daysNew;

    public void setCommunity ( CObj c )
    {
        community = c;

        if ( app != null && community != null )
        {
            if ( table != null && !table.isDisposed() &&
                    searchText != null && !searchText.isDisposed() )
            {

                boolean loaddefs = true;

                if ( lastQuery != null )
                {
                    String comid = lastQuery.getString ( CObj.COMMUNITYID );

                    if ( community.getDig().equals ( comid ) )
                    {
                        setQuery ( lastQuery );
                        loaddefs = false;
                    }

                }

                if ( loaddefs )
                {
                    loadDefFields();
                }

                updateQueries();

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
        composite.setLayout ( new GridLayout ( 7, false ) );
        composite.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        comboViewer = new ComboViewer ( composite, SWT.NONE );
        queryProvider = new CObjListContentProvider();
        comboViewer.setContentProvider ( queryProvider );
        comboViewer.setLabelProvider ( new ILabelProvider()
        {
            @Override
            public void addListener ( ILabelProviderListener arg0 )
            {
            }

            @Override
            public void dispose()
            {
            }

            @Override
            public boolean isLabelProperty ( Object arg0, String arg1 )
            {
                return false;
            }

            @Override
            public void removeListener ( ILabelProviderListener arg0 )
            {
            }

            @Override
            public Image getImage ( Object arg0 )
            {
                return null;
            }

            @Override
            public String getText ( Object c )
            {
                CObjListArrayElement e = ( CObjListArrayElement ) c;

                if ( e != null )
                {
                    return e.getCObj().getString ( CObj.NAME );
                }

                return "";
            }

        } );

        Combo combo = comboViewer.getCombo();
        GridData gd_combo = new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 );
        gd_combo.widthHint = 80;
        combo.setLayoutData ( gd_combo );
        combo.setToolTipText ( "Select a previously saved query." );

        Button btnLoadQuery = new Button ( composite, SWT.NONE );
        btnLoadQuery.setText ( "Load Query" );
        btnLoadQuery.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) comboViewer.getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof CObjListArrayElement )
                    {
                        CObjListArrayElement ce = ( CObjListArrayElement ) selo;
                        setQuery ( ce.getCObj() );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Button btnDelQuery = new Button ( composite, SWT.NONE );
        btnDelQuery.setText ( "Delete Query" );
        btnDelQuery.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) comboViewer.getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                String id = null;

                if ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof CObjListArrayElement )
                    {
                        CObjListArrayElement ce = ( CObjListArrayElement ) selo;
                        id = ce.getCObj().getId();
                    }

                }

                if ( id == null )
                {
                    String qname = text.getText();
                    Matcher m = Pattern.compile ( "(\\S+)" ).matcher ( qname );

                    if ( m.find() )
                    {
                        id = "QUERY_ID_" + m.group ( 1 );
                    }

                }

                if ( id != null )
                {
                    CObj c = app.getNode().getIndex().getById ( id );

                    if ( c != null )
                    {
                        try
                        {
                            app.getNode().getIndex().delete ( c );
                            app.getNode().getIndex().forceNewSearcher();
                            updateQueries();
                        }

                        catch ( IOException e1 )
                        {
                            e1.printStackTrace();
                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Label lblQueryName = new Label ( composite, SWT.NONE );
        lblQueryName.setText ( "Query name" );

        text = new Text ( composite, SWT.BORDER );
        text.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        text.setSize ( 150, 32 );

        btnAutodownload = new Button ( composite, SWT.CHECK );
        btnAutodownload.setText ( "Auto-download" );

        Button btnSaveQuery = new Button ( composite, SWT.NONE );
        btnSaveQuery.setText ( "Save Query" );
        btnSaveQuery.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                saveQuery();
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
        composite_2.setLayout ( new GridLayout ( 6, false ) );
        composite_2.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        btnNewWithinDays = new Button ( composite_2, SWT.CHECK );
        btnNewWithinDays.setText ( "New within days" );
        btnNewWithinDays.setToolTipText ( "Only show posts that are new within "
                                          + "the last number of days specified." );
        btnNewWithinDays.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                btnEarliest.setSelection ( false );
                btnLatest.setSelection ( false );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        daysNew = new Text ( composite_2, SWT.BORDER );
        daysNew.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        daysNew.setText ( "30" );

        btnEarliest = new Button ( composite_2, SWT.CHECK );
        btnEarliest.setText ( "Set earliest post date" );
        btnEarliest.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                btnNewWithinDays.setSelection ( false );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        earliest = new CDateTime ( composite_2, CDT.BORDER | CDT.DROP_DOWN );
        earliest.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        earliest.setPattern ( "d MMM yyyy" );

        btnLatest = new Button ( composite_2, SWT.CHECK );
        btnLatest.setText ( "Set latest post date" );
        btnLatest.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                btnNewWithinDays.setSelection ( false );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        latest = new CDateTime ( composite_2, CDT.BORDER | CDT.DROP_DOWN );
        latest.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        latest.setPattern ( "d MMM yyyy" );

        Composite composite_5 = new Composite ( container, SWT.NONE );
        composite_5.setLayout ( new GridLayout ( 9, false ) );
        composite_5.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblMinUserRank = new Label ( composite_5, SWT.NONE );
        lblMinUserRank.setText ( "Min User Rank" );

        minUserRank = new Text ( composite_5, SWT.BORDER );
        minUserRank.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

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

        Button btnDefaultFields = new Button ( composite_5, SWT.NONE );
        btnDefaultFields.setText ( "Default Fields" );
        btnDefaultFields.setToolTipText ( "Load the default fields for this community." );
        btnDefaultFields.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                loadDefFields();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Button btnDeleteField = new Button ( composite_5, SWT.NONE );
        btnDeleteField.setText ( "Delete Field" );
        btnDeleteField.setToolTipText ( "Delete fields you do not wish to search by." );
        btnDeleteField.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = table.getTableViewer().getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                while ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof CObjElement )
                    {
                        CObjElement em = ( CObjElement ) selo;
                        fieldProvider.removeElement ( em );
                        table.getTableViewer().setInput ( em );
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Label lblFileSizeMin = new Label ( composite_5, SWT.NONE );
        lblFileSizeMin.setText ( "File size min" );

        minFileSize = new Text ( composite_5, SWT.BORDER );
        minFileSize.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblMax = new Label ( composite_5, SWT.NONE );
        lblMax.setText ( "max" );

        maxFileSize = new Text ( composite_5, SWT.BORDER );
        maxFileSize.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        fieldProvider = new CObjContentProvider();

        table = new AdvancedSearchTable ( container, app, fieldProvider );
        table.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        setCommunity ( community );

        return container;
    }

    private void setQuery ( CObj c )
    {
        if ( c != null )
        {
            lastQuery = c;

            if ( app != null && community != null )
            {
                if ( table != null && !table.isDisposed() &&
                        searchText != null && !searchText.isDisposed() )
                {
                    String nm = c.getString ( CObj.NAME );

                    if ( nm != null )
                    {
                        text.setText ( nm );
                    }

                    String ad = c.getPrivate ( CObj.PRV_QRY_AUTODOWNLOAD );

                    if ( ad == null || "false".equals ( ad ) )
                    {
                        btnAutodownload.setSelection ( false );
                    }

                    else
                    {
                        btnAutodownload.setSelection ( true );
                    }

                    Long rnk = c.getNumber ( CObj.QRY_MIN_USER_RANK );

                    if ( rnk != null )
                    {
                        minUserRank.setText ( rnk.toString() );
                    }

                    else
                    {
                        minUserRank.setText ( "" );
                    }

                    String sbj = c.getString ( CObj.SUBJECT );

                    if ( sbj != null )
                    {
                        searchText.setText ( sbj );
                    }

                    else
                    {
                        searchText.setText ( "" );
                    }

                    Long daysago = c.getNumber ( CObj.QRY_DAYS_BACK );

                    if ( daysago != null && daysago > 0 )
                    {
                        btnNewWithinDays.setSelection ( true );
                        daysNew.setText ( daysago.toString() );
                        btnEarliest.setSelection ( false );
                        btnLatest.setSelection ( false );
                    }

                    else
                    {
                        btnNewWithinDays.setSelection ( false );
                        Long erl = c.getNumber ( CObj.QRY_MIN_DATE );

                        if ( erl != null )
                        {
                            Date d = new Date ( erl );
                            earliest.setSelection ( d );
                            btnEarliest.setSelection ( true );
                        }

                        else
                        {
                            btnEarliest.setSelection ( false );
                        }

                        Long lts = c.getNumber ( CObj.QRY_MAX_DATE );

                        if ( lts != null )
                        {
                            Date d = new Date ( lts );
                            latest.setSelection ( d );
                            btnLatest.setSelection ( true );
                        }

                        else
                        {
                            btnLatest.setSelection ( false );
                        }

                    }


                    Long nfs = c.getNumber ( CObj.QRY_MIN_FILE_SIZE );

                    if ( nfs != null )
                    {
                        minFileSize.setText ( nfs.toString() );
                    }

                    else
                    {
                        minFileSize.setText ( "" );
                    }

                    Long mfs = c.getNumber ( CObj.QRY_MAX_FILE_SIZE );

                    if ( mfs != null )
                    {
                        maxFileSize.setText ( mfs.toString() );
                    }

                    else
                    {
                        maxFileSize.setText ( "" );
                    }


                    Object lt = new String[] {};

                    fieldProvider.clear();

                    List<CObj> fq = c.listNewFields();
                    Iterator<CObj> i = fq.iterator();

                    while ( i.hasNext() )
                    {
                        //Queries store the min/max of the query using the fields
                        //MIN and MAX, so we need to load the original field to get
                        //the MIN and MAX allowed by the field, while adding the
                        // MIN and MAX values for the query.
                        CObj ct = i.next();
                        CObj act = app.getNode().getIndex().getByDig ( ct.getDig() );

                        String val = null;
                        String max = null;

                        String ft = ct.getString ( CObj.FLD_TYPE );

                        if ( CObj.FLD_TYPE_BOOL.equals ( ft ) )
                        {
                            Boolean b = c.getFieldBoolean ( ct.getDig() );

                            if ( b != null )
                            {
                                val = b.toString();
                            }

                        }

                        if ( CObj.FLD_TYPE_DECIMAL.equals ( ft ) )
                        {
                            Double vd = ct.getDecimal ( CObj.FLD_MIN );

                            if ( vd != null )
                            {
                                val = vd.toString();
                            }

                            Double md = ct.getDecimal ( CObj.FLD_MAX );

                            if ( md != null )
                            {
                                max = md.toString();
                            }

                        }

                        if ( CObj.FLD_TYPE_NUMBER.equals ( ft ) )
                        {
                            Long vd = ct.getNumber ( CObj.FLD_MIN );

                            if ( vd != null )
                            {
                                val = vd.toString();
                            }

                            Long md = ct.getNumber ( CObj.FLD_MAX );

                            if ( md != null )
                            {
                                max = md.toString();
                            }

                        }

                        if ( CObj.FLD_TYPE_STRING.equals ( ft ) )
                        {
                            val = c.getFieldString ( ct.getDig() );
                        }

                        act.pushPrivate ( CObj.FLD_VAL, val );

                        if ( max != null )
                        {
                            act.pushPrivate ( CObj.FLD_MAX, max );
                        }

                        fieldProvider.addCObj ( act );
                        lt = ct;
                    }

                    table.getTableViewer().setInput ( lt );
                }

            }

        }

    }

    private void updateQueries()
    {
        if ( app != null && community != null )
        {
            if ( comboViewer != null )
            {
                CObjList oldl = ( CObjList ) comboViewer.getInput();
                CObjList l = app.getNode().getIndex().getQueries ( community.getDig() );
                comboViewer.setInput ( l );

                if ( oldl != null )
                {
                    oldl.close();
                }

            }

        }

    }

    private void saveQuery()
    {
        if ( app != null )
        {
            if ( text != null && !text.isDisposed() )
            {
                lastQuery = getQuery();
                String qname = lastQuery.getString ( CObj.NAME );
                Matcher m = Pattern.compile ( "(\\S+)" ).matcher ( qname );

                if ( !m.find() )
                {
                    MessageDialog.openWarning ( shell, "Set query name", "Please give the query a name." );
                }

                else
                {
                    try
                    {
                        app.getNode().getIndex().index ( lastQuery );
                        app.getNode().getIndex().forceNewSearcher();
                        updateQueries();
                    }

                    catch ( IOException e )
                    {
                        e.printStackTrace();
                    }

                }

            }

        }

    }

    private CObj getQuery()
    {
        CObj so = new CObj();
        so.setType ( CObj.QUERY );
        so.pushString ( CObj.COMMUNITYID, community.getDig() );
        so.pushString ( CObj.CREATOR, identity.getId() );

        if ( btnAutodownload.getSelection() )
        {
            so.pushPrivate ( CObj.PRV_QRY_AUTODOWNLOAD, "true" );
        }

        else
        {
            so.pushPrivate ( CObj.PRV_QRY_AUTODOWNLOAD, "false" );
        }

        String qname = text.getText();
        Matcher m = Pattern.compile ( "(\\S+)" ).matcher ( qname );

        if ( m.find() )
        {
            so.pushString ( CObj.NAME, qname );
            so.setId ( "QUERY_ID_" + qname );
        }

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

        long daysnew = 0L; //If zero, not used

        if ( btnNewWithinDays.getSelection() )
        {
            try
            {
                daysnew = Long.valueOf ( daysNew.getText() );
            }

            catch ( Exception e )
            {
            }

        }

        else
        {
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

        }

        so.pushNumber ( CObj.QRY_DAYS_BACK, daysnew );

        so.pushString ( CObj.SUBJECT, searchText.getText() );

        List<CObj> fl = fieldProvider.getCObjList();

        for ( CObj f : fl )
        {
            //Clone the field because we overwrite the min/max values
            //for numbers
            CObj cf = f.clone();
            String tp = cf.getString ( CObj.FLD_TYPE );

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

                    long minn = cf.getNumber ( CObj.FLD_MIN );
                    long maxn = cf.getNumber ( CObj.FLD_MAX );

                    if ( val != null )
                    {
                        try
                        {
                            Long tv = Long.valueOf ( val );
                            minn = Math.max ( minn, tv );
                        }

                        catch ( Exception e )
                        {
                        }

                    }

                    if ( max != null )
                    {
                        try
                        {
                            Long tv = Long.valueOf ( max );
                            maxn = Math.min ( maxn, tv );
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

                    double minn = cf.getDecimal ( CObj.FLD_MIN );
                    double maxn = cf.getDecimal ( CObj.FLD_MAX );

                    if ( val != null )
                    {
                        try
                        {
                            double tv = Double.valueOf ( val );
                            minn = Math.max ( minn, tv );
                        }

                        catch ( Exception e )
                        {
                        }

                    }

                    if ( max != null )
                    {
                        try
                        {
                            double tv = Double.valueOf ( max );
                            maxn = Math.min ( maxn, tv );
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
        lastQuery = getQuery();
        app.setAdvancedQuery ( lastQuery );
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
    public TableViewer getTableViewer()
    {
        return table.getTableViewer();
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

    public Text getDaysNew()
    {
        return daysNew;
    }

    /**
        Since the advanced search table is editable, we use the standard AktieTable instead of CObjListTable
        which does not a Lucene search upon each update of the table content.
        Otherwise, the editing in the table would be lost upon update of the table.
        However, to our best, we make use of CObjListTable related classes.

    */
    private class AdvancedSearchTable extends AktieTable<CObjList, CObjListGetter>
    {
        public AdvancedSearchTable ( Composite composite, SWTApp app, CObjContentProvider fieldProvider  )
        {
            super ( composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION );

            setContentProvider ( fieldProvider );

            AktieTableViewerColumn<CObjList, CObjListGetter> column;

            column = addColumn ( "Field", 100, new CObjListTableCellLabelProviderTypeString ( CObj.FLD_NAME, false, null ) );
            column.setMoveable ( false );
            getTableViewer().setSortColumn ( column, false );

            column = addColumn ( "Description", 450, new CObjListTableCellLabelProviderTypeAdvSearchFieldDescription() );
            column.setMoveable ( false );

            column = addColumn ( "Value/Minimum", 100, new CObjListTableCellLabelProviderTypeString ( CObj.FLD_VAL, true, null ) );
            column.setMoveable ( false );
            column.setEditingSupport ( new NewPostFieldEditorSupport ( getTableViewer() ) );

            column = addColumn ( "Maximum", 100, new CObjListTableCellLabelProviderTypeString ( CObj.FLD_MAX, true, null ) );
            column.setMoveable ( false );
            column.setEditingSupport (  new AdvSearchMaxEditorSupport ( getTableViewer() ) );
        }

    }

}
