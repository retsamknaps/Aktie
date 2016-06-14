package aktie.gui;

import java.util.Set;

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

import aktie.data.CObj;

import org.eclipse.swt.layout.GridData;

public class SetUserRankDialog extends Dialog
{
    private Text txtNewRank;
    private Label lblUserId;
    private Set<CObj> users;
    private SWTApp app;

    /**
        Create the dialog.
        @param parentShell
    */
    public SetUserRankDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        app = a;
    }

    public void open ( Set<CObj> u )
    {
        setUser ( u );
        super.open();
    }

    private void setUser ( Set<CObj> u )
    {
        users = u;

        if ( users != null && lblUserId != null && !lblUserId.isDisposed() )
        {
            lblUserId.setText ( Integer.toString ( users.size() ) );
            txtNewRank.setText ( "5" );
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
        container.setLayout ( new GridLayout ( 2, false ) );

        Label lblUser = new Label ( container, SWT.NONE );
        lblUser.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblUser.setText ( "Users selected" );

        lblUserId = new Label ( container, SWT.NONE );
        lblUserId.setText ( "0" );

        Label lblNewRank = new Label ( container, SWT.NONE );
        lblNewRank.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblNewRank.setText ( "New Rank" );

        txtNewRank = new Text ( container, SWT.BORDER );
        txtNewRank.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        new Label ( container, SWT.NONE );

        Label lblNoteItCan = new Label ( container, SWT.NONE );
        lblNoteItCan.setText ( "Note, it can take a minute" );
        new Label ( container, SWT.NONE );

        Label lblBeforeTheNew = new Label ( container, SWT.NONE );
        lblBeforeTheNew.setText ( "before the new rank is processed" );

        setUser ( users );

        return container;
    }

    @Override
    protected void okPressed()
    {
        try
        {

            long nm = Long.valueOf ( txtNewRank.getText() );

            for ( CObj c : users )
            {
                CObj nr = new CObj();
                nr.setType ( CObj.USR_SET_RANK );
                nr.pushString ( CObj.CREATOR, c.getId() );
                nr.pushNumber ( CObj.PRV_USER_RANK, nm );
                app.getNode().enqueue ( nr );
            }

        }

        catch ( Exception e )
        {

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
        createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true );
        createButton ( parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false );
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 337, 200 );
    }

    public Label getLblAnon()
    {
        return lblUserId;
    }

    public Text getTxtNewRank()
    {
        return txtNewRank;
    }

}
