package aktie.gui;

import aktie.data.CObj;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;

public class NewIdentityDialog extends Dialog
{
    private Text name;
    private Text description;
    private SWTApp app;

    /**
        Create the dialog.
        @param parentShell
    */
    public NewIdentityDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
    }

    @Override
    protected void configureShell ( Shell shell )
    {
        super.configureShell ( shell );
        shell.setText ( "New Identity" );
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

        Label lblName = new Label ( container, SWT.NONE );
        lblName.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblName.setText ( "Name" );

        name = new Text ( container, SWT.BORDER );
        name.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblDescription = new Label ( container, SWT.NONE );
        lblDescription.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblDescription.setText ( "Description" );

        description = new Text ( container, SWT.BORDER );
        description.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        return container;
    }

    @Override
    protected void okPressed()
    {
        String sname = name.getText();
        String desc = description.getText();
        CObj co = new CObj();
        co.setType ( CObj.IDENTITY );
        co.pushString ( CObj.NAME, sname );
        co.pushString ( CObj.DESCRIPTION, desc );

        if ( app != null )
        {
            app.getNode().enqueue ( co );
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
        createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                       true );
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 170 );
    }

    public Text getName()
    {
        return name;
    }

    public Text getDescription()
    {
        return description;
    }

}
