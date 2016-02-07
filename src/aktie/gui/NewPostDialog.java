package aktie.gui;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aktie.crypto.Utils;
import aktie.data.CObj;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.layout.RowData;

public class NewPostDialog extends Dialog
{

    public static int REPLY_WRAP_LENGTH = 80;

    private Text subject;
    private Label lblPostingToCommunity;
    private Label lblNewLabel;
    private CObj postIdentity;
    private CObj community;
    private SWTApp app;
    private StyledText postBody;
    private CObj fileRef;
    private CObj prvFileRef;
    private CObj replyPost;
    private Text previewText;
    private Text fileText;
    private NewFieldNumberDialog newNumberDialog;
    private NewFieldDecimalDialog newDecimalDialog;
    private NewFieldStringDialog newStringDialog;
    private NewFieldBooleanDialog newBooleanDialog;
    private TableViewer fieldTableViewer;
    private CObjContentProvider fieldProvider;
    private Shell shell;

    public TableViewer getFieldTable()
    {
        return fieldTableViewer;
    }

    public CObj getCommunity()
    {
        return community;
    }

    /**
        Create the dialog.
        @param parentShell
    */
    public NewPostDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
        shell = parentShell;
        newStringDialog = new NewFieldStringDialog ( shell, this );
        newStringDialog.create();
        newBooleanDialog = new NewFieldBooleanDialog ( shell, this );
        newBooleanDialog.create();
        newNumberDialog = new NewFieldNumberDialog ( shell, this );
        newNumberDialog.create();
        newDecimalDialog = new NewFieldDecimalDialog ( shell, this );
        newDecimalDialog.create();
    }

    private File selectFile()
    {
        FileDialog fd = new FileDialog ( shell, SWT.OPEN | SWT.SINGLE );
        fd.setText ( "Add File" );
        //fd.setFilterPath();
        String[] filterExt =
        {
            "*",
            "*.txt",
            "*.pdf",
            "*.exe",
            "*.jpg",
            "*.jpeg",
            "*.png",
            "*.gif",
            "*.bmp",
            "*.mov",
            "*.mpg",
            "*.mpeg",
            "*.avi",
            "*.flv",
            "*.wmv",
            "*.webv",
            "*.rm"
        };

        fd.setFilterExtensions ( filterExt );
        fd.open();
        String selary[] = fd.getFileNames();
        String selpath = fd.getFilterPath();

        if ( selary != null && selpath != null && selary.length > 0 )
        {
            File f = new File ( selpath + File.separator + selary[0] );

            if ( f.exists() )
            {
                return f;
            }

        }

        return null;
    }

    public static String formatDisplay ( String body, boolean quote )
    {
        if ( body != null )
        {
            StringBuilder rb = new StringBuilder();
            rb.append ( "\n" );
            String lines[] = body.split ( "\r\n|\n" );

            Matcher mt = Pattern.compile ( "^> " ).matcher ( "" );

            for ( int c = 0; c < lines.length; c++ )
            {
                String bln = lines[c];
                mt.reset ( bln );

                if ( !mt.find() )
                {
                    if ( bln.length() == 0 )
                    {
                        if ( quote )
                        {
                            rb.append ( "> " );
                        }

                        rb.append ( "\n" );
                    }

                    Matcher whitemat = Pattern.compile ( "\\s+" ).matcher ( "" );
                    int cn = 0;

                    while ( cn < bln.length() )
                    {

                        if ( quote )
                        {
                            rb.append ( "> " );
                        }

                        int end = Math.min ( cn + REPLY_WRAP_LENGTH, bln.length() );
                        whitemat.reset ( bln.substring ( end - 1, end ) );
                        boolean wspc = whitemat.find();

                        while ( !wspc && end < bln.length() )
                        {
                            end++;
                            whitemat.reset ( bln.substring ( end - 1, end ) );
                            wspc = whitemat.find();
                        }

                        if ( wspc )
                        {
                            while ( wspc && end < bln.length() )
                            {
                                end++;
                                whitemat.reset ( bln.substring ( end - 1, end ) );
                                wspc = whitemat.find();
                            }

                            if ( !wspc ) { end--; }

                        }

                        rb.append ( bln.substring ( cn, end ) );
                        rb.append ( "\n" );
                        cn = end;
                    }

                }

                else
                {
                    if ( quote )
                    {
                        rb.append ( "> " );
                    }

                    rb.append ( bln );
                    rb.append ( "\n" );
                }

            }

            return rb.toString();

        }

        return "";

    }

    private void selectIdentity ( CObj id, CObj com )
    {
        postIdentity = id;
        community = com;

        if ( app != null )
        {
            if ( lblPostingToCommunity != null && !lblPostingToCommunity.isDisposed() &&
                    lblNewLabel != null && !lblNewLabel.isDisposed() &&
                    community != null && postIdentity != null )
            {
                lblPostingToCommunity.setText ( "Posting to community: " + community.getPrivateDisplayName() );
                lblNewLabel.setText ( "Posting as: " + postIdentity.getDisplayName() );

                if ( replyPost != null )
                {
                    String fdig = replyPost.getString ( CObj.FILEDIGEST );

                    if ( fdig != null )
                    {
                        fileRef = replyPost;
                    }

                    fdig = replyPost.getString ( CObj.PRV_FILEDIGEST );

                    if ( fdig != null )
                    {
                        prvFileRef = new CObj();
                        prvFileRef.pushString ( CObj.NAME, replyPost.getString       ( CObj.PRV_NAME ) );
                        prvFileRef.pushNumber ( CObj.FILESIZE, replyPost.getNumber   ( CObj.PRV_FILESIZE ) );
                        prvFileRef.pushString ( CObj.FRAGDIGEST, replyPost.getString ( CObj.PRV_FRAGDIGEST ) );
                        prvFileRef.pushNumber ( CObj.FRAGSIZE, replyPost.getNumber   ( CObj.PRV_FRAGSIZE ) );
                        prvFileRef.pushNumber ( CObj.FRAGNUMBER, replyPost.getNumber ( CObj.PRV_FRAGNUMBER ) );
                        prvFileRef.pushString ( CObj.FILEDIGEST, replyPost.getString ( CObj.PRV_FILEDIGEST ) );
                    }

                    String subj = replyPost.getString ( CObj.SUBJECT );
                    String body = replyPost.getText ( CObj.BODY );

                    if ( subj == null ) { subj = ""; }

                    Matcher m = Pattern.compile ( "^Re:" ).matcher ( subj );

                    if ( !m.find() )
                    {
                        subj = "Re: " + subj;

                    }

                    subject.setText ( subj );

                    postBody.setText ( formatDisplay ( body, true ) );
                    postBody.setFocus();

                }

                if ( prvFileRef != null )
                {
                    previewText.setText ( prvFileRef.getString ( CObj.NAME ) );
                }

                else
                {
                    previewText.setText ( "" );
                }

                if ( fileRef != null )
                {
                    fileText.setText ( fileRef.getString ( CObj.NAME ) );
                }

                else
                {
                    fileText.setText ( "" );
                }

            }

        }

    }

    public void reply ( CObj id, CObj comid, CObj rpst )
    {
        fileRef = null;
        prvFileRef = null;
        newAttachment = null;
        newPreview = null;
        replyPost = rpst;
        selectIdentity ( id, comid );
        super.open();
    }

    public void open ( CObj id, CObj comid, CObj prv, CObj lref )
    {
        replyPost = null;
        newAttachment = null;
        newPreview = null;
        prvFileRef = prv;
        fileRef = lref;
        selectIdentity ( id, comid );
        super.open();
    }

    /**
        Create contents of the dialog.
        @param parent
    */
    @Override
    protected Control createDialogArea ( Composite parent )
    {
        Composite container = ( Composite ) super.createDialogArea ( parent );
        container.setLayout ( new GridLayout ( 3, false ) );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );

        lblPostingToCommunity = new Label ( container, SWT.NONE );
        lblPostingToCommunity.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblPostingToCommunity.setText ( "Posting to community: " );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );

        lblNewLabel = new Label ( container, SWT.NONE );
        lblNewLabel.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblNewLabel.setText ( "Posting as:" );
        new Label ( container, SWT.NONE );

        Label lblSubject = new Label ( container, SWT.NONE );
        lblSubject.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblSubject.setText ( "Subject" );

        subject = new Text ( container, SWT.BORDER );
        subject.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        new Label ( container, SWT.NONE );

        Label lblBody = new Label ( container, SWT.NONE );
        lblBody.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, true, 1, 1 ) );
        lblBody.setText ( "Body" );

        postBody = new StyledText ( container, SWT.WRAP | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );
        postBody.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );

        Composite composite_1 = new Composite ( container, SWT.NONE );
        composite_1.setLayout ( new RowLayout ( SWT.HORIZONTAL ) );

        Button btnAddAllFields = new Button ( composite_1, SWT.NONE );
        btnAddAllFields.setText ( "Add All Fields" );
        btnAddAllFields.setToolTipText ( "Add all available fields to the post." );

        ComboViewer comboViewer = new ComboViewer ( composite_1, SWT.NONE );
        Combo combo = comboViewer.getCombo();
        combo.setLayoutData ( new RowData ( 85, SWT.DEFAULT ) );

        Button btnAddField = new Button ( composite_1, SWT.NONE );
        btnAddField.setText ( "Add Field" );
        btnAddField.setToolTipText ( "Add the selected field to the post." );

        Button btnAddDefaultField = new Button ( composite_1, SWT.NONE );
        btnAddDefaultField.setText ( "Set Default" );
        btnAddDefaultField.setToolTipText ( "Set the current set of fields as the default.\n"
                                            + "They will be added automatically to all new posts." );

        final ComboViewer comboViewer_1 = new ComboViewer ( composite_1, SWT.NONE );
        comboViewer_1.setContentProvider ( ArrayContentProvider.getInstance() );
        comboViewer_1.setLabelProvider ( new LabelProvider() );
        Combo combo_1 = comboViewer_1.getCombo();
        combo_1.setLayoutData ( new RowData ( 79, SWT.DEFAULT ) );
        combo_1.setToolTipText ( "Select the type of new field you want to add." );
        comboViewer_1.setInput ( new String[] {"String", "Number", "Decimal", "Checkbox"} );

        comboViewer_1.setSelection ( new StructuredSelection ( "String" ) );

        Button btnNewField = new Button ( composite_1, SWT.NONE );
        btnNewField.setText ( "New Field" );
        btnNewField.setToolTipText ( "Create a new field of the type selected." );
        btnNewField.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection selection = comboViewer_1.getStructuredSelection();
                String s = ( String ) selection.getFirstElement();

                if ( "Checkbox".equals ( s ) )
                {
                    newBooleanDialog.open();
                }

                if ( "Decimal".equals ( s ) )
                {
                    newDecimalDialog.open();
                }

                if ( "Number".equals ( s ) )
                {
                    newNumberDialog.open();
                }

                if ( "String".equals ( s ) )
                {
                    newStringDialog.open();
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        new Label ( container, SWT.NONE );

        Label lblFields = new Label ( container, SWT.NONE );
        lblFields.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblFields.setText ( "Fields" );
        fieldTableViewer = new TableViewer ( container, SWT.BORDER | SWT.FULL_SELECTION );
        fieldProvider = new CObjContentProvider();
        fieldTableViewer.setContentProvider ( fieldProvider );
        table = fieldTableViewer.getTable();
        GridData gd_table = new GridData ( SWT.FILL, SWT.FILL, true, false, 1, 1 );
        gd_table.heightHint = 100;
        table.setLayoutData ( gd_table );
        table.setHeaderVisible ( true );
        table.setLinesVisible ( true );

        TableViewerColumn col0 = new TableViewerColumn ( fieldTableViewer, SWT.NONE );
        col0.getColumn().setText ( "Field" );
        col0.getColumn().setWidth ( 100 );
        col0.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.FLD_NAME ) );

        TableViewerColumn col1 = new TableViewerColumn ( fieldTableViewer, SWT.NONE );
        col1.getColumn().setText ( "Description" );
        col1.getColumn().setWidth ( 100 );
        col1.setLabelProvider ( new CObjListStringColumnLabelProvider ( CObj.FLD_DESC ) );

        TableViewerColumn col2 = new TableViewerColumn ( fieldTableViewer, SWT.NONE );
        col2.getColumn().setText ( "Value" );
        col2.getColumn().setWidth ( 100 );
        col2.setLabelProvider ( new CObjListPrivateColumnLabelProvider ( CObj.FLD_VAL ) );

        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );

        Composite composite = new Composite ( container, SWT.NONE );
        composite.setLayout ( new FillLayout ( SWT.HORIZONTAL ) );

        Button btnAttachNewFile = new Button ( composite, SWT.NONE );
        btnAttachNewFile.setText ( "Attach File" );
        btnAttachNewFile.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                newAttachment = selectFile();

                if ( newAttachment != null )
                {
                    try
                    {
                        fileText.setText ( newAttachment.getCanonicalPath() );
                    }

                    catch ( Exception e2 )
                    {
                        e2.printStackTrace();
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Button btnAttachPreview = new Button ( composite, SWT.NONE );
        btnAttachPreview.setText ( "Attach Preview" );
        btnAttachPreview.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                newPreview = selectFile();

                if ( newPreview != null )
                {
                    try
                    {
                        previewText.setText ( newPreview.getCanonicalPath() );
                    }

                    catch ( Exception e2 )
                    {
                        e2.printStackTrace();
                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Button btnRemoveFile = new Button ( composite, SWT.NONE );
        btnRemoveFile.setText ( "Remove File" );
        btnRemoveFile.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                fileRef = null;
                newAttachment = null;
                fileText.setText ( "" );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Button btnRemovePreview = new Button ( composite, SWT.NONE );
        btnRemovePreview.setText ( "Remove Preview" );
        btnRemovePreview.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                prvFileRef = null;
                newPreview = null;
                previewText.setText ( "" );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        new Label ( container, SWT.NONE );

        Label lblFile = new Label ( container, SWT.NONE );
        lblFile.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblFile.setText ( "Preview File" );

        previewText = new Text ( container, SWT.BORDER );
        previewText.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        previewText.setEditable ( false );

        new Label ( container, SWT.NONE );

        Label lblFile_1 = new Label ( container, SWT.NONE );
        lblFile_1.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblFile_1.setText ( "Complete File" );

        fileText = new Text ( container, SWT.BORDER );
        fileText.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        fileText.setEditable ( false );
        //scrolledComposite.setContent(bodyText);
        //scrolledComposite.setMinSize(bodyText.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        selectIdentity ( postIdentity, community );

        return container;
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        Button button = createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                                       true );
        button.setText ( "Post" );
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );
    }

    private File newAttachment;
    private File newPreview;
    private Table table;

    @Override
    protected void okPressed()
    {
        if ( postIdentity != null && community != null && app != null )
        {
            CObj p = new CObj();
            p.setType ( CObj.POST );
            p.pushString ( CObj.CREATOR, postIdentity.getId() );
            p.pushString ( CObj.CREATOR_NAME, postIdentity.getDisplayName() );
            p.pushString ( CObj.COMMUNITYID, community.getDig() );
            p.pushString ( CObj.COMMUNITY_NAME, community.getPrivateDisplayName() );
            p.pushString ( CObj.SUBJECT, subject.getText() );
            p.pushNumber ( CObj.CREATEDON, Utils.fuzzTime ( fileRef, prvFileRef, replyPost ) );
            p.pushText ( CObj.BODY, postBody.getText() );

            if ( newAttachment == null )
            {
                if ( fileRef != null )
                {
                    p.pushString ( CObj.NAME, fileRef.getString ( CObj.NAME ) );
                    p.pushNumber ( CObj.FILESIZE, fileRef.getNumber ( CObj.FILESIZE ) );
                    p.pushString ( CObj.FRAGDIGEST, fileRef.getString ( CObj.FRAGDIGEST ) );
                    p.pushNumber ( CObj.FRAGSIZE, fileRef.getNumber ( CObj.FRAGSIZE ) );
                    p.pushNumber ( CObj.FRAGNUMBER, fileRef.getNumber ( CObj.FRAGNUMBER ) );
                    p.pushString ( CObj.FILEDIGEST, fileRef.getString ( CObj.FILEDIGEST ) );
                }

            }

            else
            {
                try
                {
                    p.pushPrivate ( CObj.LOCALFILE, newAttachment.getCanonicalPath() );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                    newAttachment = null;
                }

            }

            if ( newPreview == null )
            {
                if ( prvFileRef != null )
                {
                    p.pushString ( CObj.PRV_NAME,       prvFileRef.getString ( CObj.NAME ) );
                    p.pushNumber ( CObj.PRV_FILESIZE,   prvFileRef.getNumber ( CObj.FILESIZE ) );
                    p.pushString ( CObj.PRV_FRAGDIGEST, prvFileRef.getString ( CObj.FRAGDIGEST ) );
                    p.pushNumber ( CObj.PRV_FRAGSIZE,   prvFileRef.getNumber ( CObj.FRAGSIZE ) );
                    p.pushNumber ( CObj.PRV_FRAGNUMBER, prvFileRef.getNumber ( CObj.FRAGNUMBER ) );
                    p.pushString ( CObj.PRV_FILEDIGEST, prvFileRef.getString ( CObj.FILEDIGEST ) );
                }

            }

            else
            {
                try
                {
                    p.pushPrivate ( CObj.PRV_LOCALFILE, newPreview.getCanonicalPath() );
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                    newPreview = null;
                }

            }

            if ( newPreview == null && newAttachment == null )
            {
                app.getNode().priorityEnqueue ( p );
            }

            else
            {
                app.addPendingPost ( p );

                if ( newPreview != null )
                {
                    try
                    {
                        CObj nf = new CObj();
                        nf.setType ( CObj.HASFILE );
                        nf.pushString ( CObj.COMMUNITYID, community.getDig() );
                        nf.pushString ( CObj.CREATOR, postIdentity.getId() );
                        nf.pushPrivate ( CObj.LOCALFILE, newPreview.getCanonicalPath() );
                        app.getNode().priorityEnqueue ( nf );
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

                if ( newAttachment != null )
                {
                    try
                    {
                        CObj nf = new CObj();
                        nf.setType ( CObj.HASFILE );
                        nf.pushString ( CObj.COMMUNITYID, community.getDig() );
                        nf.pushString ( CObj.CREATOR, postIdentity.getId() );
                        nf.pushPrivate ( CObj.LOCALFILE, newAttachment.getCanonicalPath() );
                        app.getNode().priorityEnqueue ( nf );
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }

                }

            }

        }

        super.okPressed();
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 820, 700 );
    }

    public Label getLblPostingToCommunity()
    {
        return lblPostingToCommunity;
    }

    public Label getLblNewLabel()
    {
        return lblNewLabel;
    }

    public Text getSubject()
    {
        return subject;
    }

    public StyledText getPostBody()
    {
        return postBody;
    }

    public Text getFile1Text()
    {
        return previewText;
    }

    public Text getFile2Text()
    {
        return fileText;
    }

}
