package aktie.gui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.custom.StyledText;

public class SWTSplash extends Dialog
{

    /**
        Create the dialog.
        @param parentShell
    */
    public SWTSplash ( Shell parentShell )
    {
        super ( parentShell );
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

        Label lblNewLabel = new Label ( container, SWT.NONE );
        lblNewLabel.setLayoutData ( new GridData ( SWT.CENTER, SWT.CENTER, true, false, 1, 1 ) );
        lblNewLabel.setText ( "Please wait while Aktie starts.." );

        Label lblItCanTake = new Label ( container, SWT.NONE );
        lblItCanTake.setLayoutData ( new GridData ( SWT.CENTER, SWT.CENTER, false, false, 1, 1 ) );
        lblItCanTake.setText ( "It can take quite a while the first time you start it." );

        Label lblIfYouSelected = new Label ( container, SWT.NONE );
        lblIfYouSelected.setLayoutData ( new GridData ( SWT.CENTER, SWT.CENTER, false, false, 1, 1 ) );
        lblIfYouSelected.setText ( "If you selected to use and external router make sure it is running" );

        StyledText styledText = new StyledText ( container, SWT.BORDER );
        styledText.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );
        styledText.setEditable ( false );
        styledText.setWordWrap ( true );
        styledText.setText (
            "Here is a riddle while you wait: I end the race. I am "
            + "the beginning of the end. "
            + "The start of eternity and the end of space. There are "
            + "two of me in Heaven and one in hell. I am in water, fire, "
            + "sunshine and darkness. I am the beginning of earth and "
            + "the end of life. What am I?" );
        return container;
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        //createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        //createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    public boolean close()
    {
        return false;
    }

    private boolean openned = false;
    public int open()
    {
        openned = true;
        //NOTE: Open will block, so we have to set opened true first
        return super.open();
    }

    private void superClose()
    {
        super.close();
    }

    public boolean isClosed()
    {
        return isclosed;
    }

    boolean isclosed = false;
    public void reallyClose()
    {
        if ( !isclosed )
        {
            isclosed = true;

            //Ugly..
            do
            {

                try
                {
                    Thread.sleep ( 100 );
                }

                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }

            }

            while ( !openned );

            superClose();
        }

    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 220 );
    }

}
