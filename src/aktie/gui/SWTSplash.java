package aktie.gui;

import java.io.InputStream;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

public class SWTSplash extends Dialog
{

    private Image splashImg;

    /**
        Create the dialog.
        @param parentShell
    */
    public SWTSplash ( Shell parentShell )
    {
        super ( parentShell );

        try
        {
            InputStream is  = SWTSplash.class.getClassLoader().getResourceAsStream ( "images/aktie.png" );
            splashImg = new Image ( Display.getDefault(), is );
            is.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
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

        Label lblNewLabel = new Label ( container, SWT.NONE );
        lblNewLabel.setLayoutData ( new GridData ( SWT.CENTER, SWT.CENTER, true, false, 1, 1 ) );
        lblNewLabel.setText ( "Please wait while Aktie starts..." );

        Label lblItCanTake = new Label ( container, SWT.NONE );
        lblItCanTake.setLayoutData ( new GridData ( SWT.CENTER, SWT.CENTER, false, false, 1, 1 ) );
        lblItCanTake.setText ( "It can take over 24 hours the first time you start it." );

        Label lblIfYouSelected = new Label ( container, SWT.NONE );
        lblIfYouSelected.setLayoutData ( new GridData ( SWT.CENTER, SWT.CENTER, false, false, 1, 1 ) );
        lblIfYouSelected.setText ( "If you selected to use and external router make sure it is running." );

        if ( splashImg != null )
        {
            Label lblImg = new Label ( container, SWT.NONE );
            lblImg.setLayoutData ( new GridData ( SWT.CENTER, SWT.CENTER, false, false, 1, 1 ) );
            lblImg.setImage ( splashImg );
        }

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

    private boolean opened = false;
    public int open()
    {
        opened = true;
        //NOTE: Open will block, so we have to set opened true first
        return super.open();
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

            while ( !opened );

            if ( splashImg != null && !splashImg.isDisposed() )
            {
                splashImg.dispose();
            }

            super.close();
        }

    }

    /**
        Return the initial location of the dialog.
    */
    @Override
    protected Point getInitialLocation ( Point initialSize )
    {
        Shell parent = getParentShell();
        int width = parent.getLocation().x + Math.abs ( parent.getSize().x - initialSize.x ) / 2;
        int height = parent.getLocation().y + Math.abs ( parent.getSize().y - initialSize.y ) / 2;
        return new Point ( width, height );
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 400 );
    }

}
