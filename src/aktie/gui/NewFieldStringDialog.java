package aktie.gui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Text;

import aktie.data.CObj;

public class NewFieldStringDialog extends Dialog
{
    private Text text;
    private Text text_1;
    private NewPostDialog postDialog;

    /**
        Create the dialog.
        @param parentShell
    */
    public NewFieldStringDialog ( Shell parentShell, NewPostDialog p )
    {
        super ( parentShell );
        postDialog = p;
    }

    @Override
    protected void configureShell ( Shell newShell )
    {
        super.configureShell ( newShell );
        newShell.setText ( "New String Field" );
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

        text = new Text ( container, SWT.BORDER );
        text.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblDescription = new Label ( container, SWT.NONE );
        lblDescription.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblDescription.setText ( "Description" );

        text_1 = new Text ( container, SWT.BORDER );
        text_1.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        return container;
    }

    protected void okPressed()
    {
        CObjContentProvider cc = ( CObjContentProvider ) postDialog.getFieldTable().getContentProvider();
        CObj nf = new CObj();
        nf.setType ( CObj.FIELD );
        nf.pushString ( CObj.COMMUNITYID, postDialog.getCommunity().getDig() );
        nf.pushPrivate ( CObj.CREATOR, postDialog.getIdentity().getId() );
        nf.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_STRING );
        nf.pushString ( CObj.FLD_NAME, text.getText() );
        nf.pushString ( CObj.FLD_DESC, text_1.getText() );
        nf.simpleDigest();
        cc.addCObj ( nf );
        super.okPressed();
        //Just update the damn table with new data
        postDialog.getFieldTable().setInput ( nf );
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
        return new Point ( 500, 150 );
    }

}
