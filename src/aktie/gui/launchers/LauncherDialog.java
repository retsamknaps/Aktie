package aktie.gui.launchers;

import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import swing2swt.layout.BorderLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;

import aktie.data.Launcher;
import aktie.gui.SWTApp;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;

public class LauncherDialog extends Dialog
{
    private LauncherTable table;
    private LauncherContentModel model;
    private SWTApp app;
    private Shell shell;
    private Text extensions;
    private Text program;

    /**
        Create the dialog.
        @param parentShell
    */
    public LauncherDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        shell = parentShell;
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
        container.setLayout ( new BorderLayout ( 0, 0 ) );

        Composite composite = new Composite ( container, SWT.NONE );
        composite.setLayoutData ( BorderLayout.NORTH );
        composite.setLayout ( new GridLayout ( 2, false ) );

        Label lblExtenions = new Label ( composite, SWT.NONE );
        lblExtenions.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblExtenions.setText ( "File Extenions" );

        extensions = new Text ( composite, SWT.BORDER );
        extensions.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Button btnSelectProgram = new Button ( composite, SWT.NONE );
        btnSelectProgram.setText ( "Select Program" );
        btnSelectProgram.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                FileDialog fd = new FileDialog ( shell, SWT.OPEN );
                fd.setText ( "Select" );
                //fd.setFilterPath();
                String[] filterExt = { "*", "*.*" };

                fd.setFilterExtensions ( filterExt );
                String selected = fd.open();

                if ( selected != null )
                {
                    program.setText ( selected );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {

            }

        } );


        program = new Text ( composite, SWT.BORDER );
        program.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        new Label ( composite, SWT.NONE );

        Button btnAddProgramLauncher = new Button ( composite, SWT.NONE );
        btnAddProgramLauncher.setText ( "Add Program Launcher for File Extensions" );
        btnAddProgramLauncher.addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                model.addLauncher ( program.getText(), extensions.getText() );
                table.getTableViewer().setInput ( model.getLaunchers() );
                table.getTableViewer().refresh();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {

            }

        } );

        table = new LauncherTable ( container, app );
        table.setLayoutData ( BorderLayout.CENTER );

        model = new LauncherContentModel ( app.getNode().getSession() );

        Menu menu_2 = new Menu ( table.getTable() );
        table.setMenu ( menu_2 );

        MenuItem remove = new MenuItem ( menu_2, SWT.NONE );
        remove.setText ( "Remove Selected" );
        remove.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = table.getTableViewer().getSelection();
                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                while ( i.hasNext() )
                {
                    Launcher l = ( Launcher ) i.next();
                    model.removeLauncher ( l.getExtension() );
                }

                table.getTableViewer().setInput ( model.getLaunchers() );
                table.getTableViewer().refresh();

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        table.getTableViewer().setInput ( model.getLaunchers() );

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

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 300 );
    }

    public boolean open ( String localfile )
    {
        return model.open ( localfile );
    }

}
