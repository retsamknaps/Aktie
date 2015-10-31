package aktie.gui;

import java.util.Iterator;

import aktie.data.RequestFile;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Spinner;

public class DownloadPriorityDialog extends Dialog
{
    private Label lblFilename;
    private Spinner priority;

    private SWTApp app;

    private IStructuredSelection selected;
    private int setPriority = 5;

    /**
        Create the dialog.
        @param parentShell
    */
    public DownloadPriorityDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        app = a;
    }

    @SuppressWarnings ( "rawtypes" )
    public void open ( IStructuredSelection s )
    {
        selected = s;
        int lowpriority = -1;
        Iterator i = selected.iterator();

        while ( i.hasNext() )
        {
            RequestFile rf = ( RequestFile ) i.next();

            if ( lowpriority == -1 || rf.getPriority() < lowpriority )
            {
                lowpriority = rf.getPriority();
            }

        }

        if ( lowpriority == -1 ) { lowpriority = 5; }

        setPriority = lowpriority;

        if ( priority != null && !priority.isDisposed() )
        {
            priority.setSelection ( setPriority );
        }

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
        container.setLayout ( new GridLayout ( 2, false ) );
        new Label ( container, SWT.NONE );

        lblFilename = new Label ( container, SWT.NONE );
        lblFilename.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        lblFilename.setText ( "Filename" );

        Label lblNewPriority = new Label ( container, SWT.NONE );
        lblNewPriority.setText ( "New priority" );

        priority = new Spinner ( container, SWT.BORDER );
        priority.setMinimum ( 0 );
        priority.setMaximum ( 10 );
        priority.setIncrement ( 1 );
        priority.setSelection ( setPriority );

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

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 149 );
    }

    @SuppressWarnings ( "rawtypes" )
    @Override
    protected void okPressed()
    {
        if ( app != null && selected != null )
        {
            int pri = priority.getSelection();
            Iterator i = selected.iterator();

            while ( i.hasNext() )
            {
                RequestFile rf = ( RequestFile ) i.next();
                rf.setPriority ( pri );
                app.getNode().getFileHandler().setPriority ( rf, pri );
                app.getUserCallback().update ( rf );
            }

        }

        super.okPressed();
    }

    public Label getLblFilename()
    {
        return lblFilename;
    }

    public Spinner getPriority()
    {
        return priority;
    }

}
