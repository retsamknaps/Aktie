package aktie.gui.pm;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;

import aktie.data.CObj;
import aktie.gui.SWTApp;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;

public class PrivateMessageDialog extends Dialog
{
    private Text SubjectText;
    private Label lblFrm;
    private Label lblMsgTo;
    private StyledText styledText;
    private Button btnSkipAntispam;
    private SWTApp app;

    /**
        Create the dialog.
        @param parentShell
    */
    public PrivateMessageDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
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

        Label lblFrom = new Label ( container, SWT.NONE );
        FontData fontData = lblFrom.getFont().getFontData() [0];
        Font font = new Font ( Display.getDefault(), new FontData ( fontData.getName(), fontData
                               .getHeight(), SWT.BOLD ) );
        lblFrom.setFont ( font );
        lblFrom.setText ( "From" );

        lblFrm = new Label ( container, SWT.NONE );
        lblFrm.setFont ( font );
        lblFrm.setText ( "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" );

        Label lblTo = new Label ( container, SWT.NONE );
        lblTo.setText ( "To" );

        lblMsgTo = new Label ( container, SWT.NONE );
        lblMsgTo.setText ( "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" );

        Label lblSubject = new Label ( container, SWT.NONE );
        lblSubject.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblSubject.setText ( "Subject" );

        SubjectText = new Text ( container, SWT.BORDER );
        SubjectText.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblBody = new Label ( container, SWT.NONE );
        lblBody.setText ( "Body" );

        styledText = new StyledText ( container, SWT.WRAP | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );
        styledText.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
        new Label ( container, SWT.NONE );

        btnSkipAntispam = new Button ( container, SWT.CHECK );
        btnSkipAntispam.setText ( "Skip Anti-Spam" );

        updateToFrom ( fromIdent, toIdent );

        return container;
    }

    private String fromIdent;
    private String toIdent;
    public void updateToFrom ( String from, String to )
    {
        fromIdent = from;
        toIdent = to;

        if ( fromIdent != null && toIdent != null )
        {
            if ( lblFrm != null && !lblFrm.isDisposed() )
            {
                lblFrm.setText ( app.getIdCache().getName ( fromIdent ) );
                lblMsgTo.setText ( app.getIdCache().getName ( toIdent ) );
            }

        }

    }

    public void open ( String from, String to )
    {
        updateToFrom ( from, to );
        super.open();
    }

    @Override
    protected void okPressed()
    {
        if ( fromIdent != null && toIdent != null )
        {
            CObj np = new CObj();
            np.setType ( CObj.PRIVMESSAGE );
            np.pushString ( CObj.CREATOR, fromIdent );
            np.pushPrivate ( CObj.PRV_RECIPIENT, toIdent );
            np.pushPrivate ( CObj.SUBJECT, SubjectText.getText() );
            np.pushPrivate ( CObj.BODY, styledText.getText() );

            if ( btnSkipAntispam.getSelection() )
            {
                np.pushPrivate ( CObj.PRV_SKIP_PAYMENT, "true" );
            }

            app.getNode().enqueue ( np );
        }

        super.okPressed();
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, "Send", true );
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

    public Label getLblFrm()
    {
        return lblFrm;
    }

    public Label getLblMsgTo()
    {
        return lblMsgTo;
    }

    public Text getSubjectText()
    {
        return SubjectText;
    }

    public StyledText getStyledText()
    {
        return styledText;
    }

    public Button getBtnSkipAntispam()
    {
        return btnSkipAntispam;
    }

}
