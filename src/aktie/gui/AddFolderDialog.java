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

import aktie.gui.subtree.SubTreeEntity;

import org.eclipse.swt.layout.GridData;

public class AddFolderDialog extends Dialog
{
    private Text folderName;

    private SWTApp app;
    private SubTreeEntity entity;
    private Label parentName;

    public AddFolderDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
    }

    public void open ( SubTreeEntity s )
    {
        if ( s != null )
        {
            entity = s;

            if ( parentName != null && !parentName.isDisposed() )
            {
                parentName.setText ( entity.getText() );
            }

            super.open();
        }

    }

    @Override
    protected Control createDialogArea ( Composite parent )
    {
        Composite container = ( Composite ) super.createDialogArea ( parent );
        container.setLayout ( new GridLayout ( 2, false ) );

        Label lblParent = new Label ( container, SWT.NONE );
        lblParent.setText ( "Parent" );

        parentName = new Label ( container, SWT.NONE );
        parentName.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        parentName.setText ( "parent" );

        if ( entity != null )
        {
            parentName.setText ( entity.getText() );
        }

        Label lblNewFolderName = new Label ( container, SWT.NONE );
        lblNewFolderName.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblNewFolderName.setText ( "New Folder Name" );

        folderName = new Text ( container, SWT.BORDER );
        folderName.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        return container;
    }

    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true );
        createButton ( parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false );
    }

    @Override
    protected void okPressed()
    {
        if ( entity != null )
        {
            app.addFolder ( entity, folderName.getText() );
        }

        super.okPressed();
    }

    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 153 );
    }

    public Text getFolderName()
    {
        return folderName;
    }

    public Label getParentName()
    {
        return parentName;
    }

}
