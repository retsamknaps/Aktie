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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;

public class SpamRankDialog extends Dialog
{
    private Text newRank;
    private Text text_1;

    /**
        Create the dialog.
        @param parentShell
    */
    public SpamRankDialog ( Shell parentShell )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
    }

    private void setTextRank()
    {
        if ( newRank != null && ( !newRank.isDisposed() ) )
        {
            newRank.setText ( Integer.toString ( Wrapper.getPaymentRank() ) );
        }

    }

    @Override
    public int open()
    {
        setTextRank();
        return super.open();
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
        new Label ( container, SWT.NONE );

        text_1 = new Text ( container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL );
        text_1.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, true, 1, 1 ) );
        text_1.setEditable ( false );
        text_1.setText ( "Here you set the minimum rank that a user must have "
                         + "for you, in order to not require an anti-spam payment. For users "
                         + "that you trust, you can set their rank above this value, "
                         + "then they will not have to generate anti-spam for you to "
                         + "see their files and posts." );

        Label lblNewRank = new Label ( container, SWT.NONE );
        lblNewRank.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblNewRank.setText ( "New Rank" );

        newRank = new Text ( container, SWT.BORDER );
        newRank.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        setTextRank();

        return container;
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

    @Override
    protected void okPressed()
    {
        try
        {
            int nv = Integer.valueOf ( newRank.getText() );
            Wrapper.savePaymentRank ( nv );
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 192 );
    }

    public Text getNewRank()
    {
        return newRank;
    }

}
