package aktie.gui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;

public class ConnectionDialog extends Dialog
{

    private SWTApp app;
    private Label lblFrom;
    private Label lblTo;

    /**
        Create the dialog.
        @param parentShell
    */
    public ConnectionDialog ( Shell parentShell, SWTApp a )
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
        container.setLayout ( new GridLayout ( 4, false ) );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );

        lblFrom = new Label ( container, SWT.NONE );
        lblFrom.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        lblFrom.setText ( "From" );

        Label label = new Label ( container, SWT.NONE );
        label.setText ( "<-->" );

        lblTo = new Label ( container, SWT.NONE );
        lblTo.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        lblTo.setText ( "To" );
        new Label ( container, SWT.NONE );

        Button btnDisconnect = new Button ( container, SWT.NONE );
        btnDisconnect.setText ( "Disconnect" );
        btnDisconnect.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                app.getNode().getConnectionManager().closeConnection ( fromId, toId );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {

            }

        } );

        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );


        Button btnToggleLogging = new Button ( container, SWT.NONE );
        btnToggleLogging.setText ( "Toggle Logging" );
        btnToggleLogging.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                app.getNode().getConnectionManager().toggleConnectionLogging ( fromId, toId );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {

            }

        } );

        new Label ( container, SWT.NONE );
        new Label ( container, SWT.NONE );


        setFromTo ( fromId, toId );

        return container;
    }

    private String fromId; //Local
    private String toId;   //Remote
    private void setFromTo ( String fromid, String toid )
    {
        fromId = fromid;
        toId = toid;

        if ( fromId != null && toId != null && lblFrom != null && !lblFrom.isDisposed() &&
                lblTo != null && !lblTo.isDisposed() )
        {
            String ln = app.getNode().getIndex().getDisplayNameForIdentity ( fromId );

            if ( ln != null )
            {
                lblFrom.setText ( ln );
            }

            String rm = app.getNode().getIndex().getDisplayNameForIdentity ( toId );

            if ( rm != null )
            {
                lblTo.setText ( rm );
            }

        }

    }

    public void open ( String from, String to )
    {
        setFromTo ( from, to );
        super.open();
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, "Dismiss", true );
        //createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 440, 202 );
    }

    public Label getLblFrom()
    {
        return lblFrom;
    }

    public Label getLblTo()
    {
        return lblTo;
    }

}
