package aktie.gui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;

import aktie.data.CObj;

public class NewDirectoryShareDialog extends Dialog
{
    private Text textShareName;
    private Text textSharePath;
    private Text text_2;
    private SWTApp app;
    private Label lblMemberid;
    private Label lblComid;

    private CObj memId;
    private CObj comId;
    private Button btnDefaultDownloadLocation;
    private Button btnDoNotGenerate;

    /**
        Create the dialog.
        @param parentShell
    */
    public NewDirectoryShareDialog ( Shell parentShell, SWTApp p )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = p;
    }

    public void setIdAndCom ( CObj memid, CObj comid )
    {
        comId = comid;
        memId = memid;

        if ( comId != null && memId != null )
        {

            if ( lblMemberid != null && !lblMemberid.isDisposed() )
            {
                lblMemberid.setText ( memId.getDisplayName() );
            }

            if ( lblComid != null && !lblComid.isDisposed() )
            {
                lblComid.setText ( comId.getPrivateDisplayName() );
            }

        }

    }

    public void open ( CObj memid, CObj comid )
    {
        setIdAndCom ( memid, comid );
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

        Label lblIdentity = new Label ( container, SWT.NONE );
        lblIdentity.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblIdentity.setText ( "Identity" );

        lblMemberid = new Label ( container, SWT.NONE );
        lblMemberid.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblMemberid.setText ( "id" );
        new Label ( container, SWT.NONE );

        Label lblCommunity = new Label ( container, SWT.NONE );
        lblCommunity.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblCommunity.setText ( "Community" );

        lblComid = new Label ( container, SWT.NONE );
        lblComid.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblComid.setText ( "com" );
        new Label ( container, SWT.NONE );

        Label lblShareName = new Label ( container, SWT.NONE );
        lblShareName.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblShareName.setText ( "Share Name (Publicly Visible)" );

        textShareName = new Text ( container, SWT.BORDER );
        textShareName.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        new Label ( container, SWT.NONE );

        Label lblDirectoryToShare = new Label ( container, SWT.NONE );
        lblDirectoryToShare.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblDirectoryToShare.setText ( "Directory to share" );

        textSharePath = new Text ( container, SWT.BORDER );
        textSharePath.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Button btnBrowse = new Button ( container, SWT.NONE );
        btnBrowse.setText ( "Browse" );
        new Label ( container, SWT.NONE );

        btnDefaultDownloadLocation = new Button ( container, SWT.CHECK );
        btnDefaultDownloadLocation.setText ( "Default Download Location" );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );

        btnDoNotGenerate = new Button ( container, SWT.CHECK );
        btnDoNotGenerate.setText ( "Do not generate anti-spam payment ( Expert )" );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );
        btnBrowse.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                DirectoryDialog dialog = new DirectoryDialog (
                    NewDirectoryShareDialog.this.getShell() );
                String sharedir = dialog.open();

                if ( sharedir != null )
                {
                    textSharePath.setText ( sharedir );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        text_2 = new Text ( container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );
        text_2.setEditable ( false );
        text_2.setText ( "All files in a share directory are shared with the community.\n" +
                         "Including all files in all subdirectories.  All files are\n" +
                         "tagged with the share name." );
        text_2.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        new Label ( container, SWT.NONE );

        setIdAndCom ( memId, comId );

        return container;
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                       true );
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );
    }


    @Override
    protected void okPressed()
    {
        app.getNode().getShareManager().addShare ( comId.getDig(),
                memId.getId(), textShareName.getText(), textSharePath.getText(),
                btnDefaultDownloadLocation.getSelection(),
                btnDoNotGenerate.getSelection() );
        super.okPressed();
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 550, 344 );
    }

    public Label getLblMemberid()
    {
        return lblMemberid;
    }

    public Label getLblComid()
    {
        return lblComid;
    }

    public Button getBtnDefaultDownloadLocation()
    {
        return btnDefaultDownloadLocation;
    }

    public Button getBtnDoNotGenerate()
    {
        return btnDoNotGenerate;
    }

}
